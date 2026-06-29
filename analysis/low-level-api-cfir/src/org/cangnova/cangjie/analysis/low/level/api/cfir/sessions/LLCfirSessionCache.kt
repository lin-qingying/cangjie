/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.CaCachedService
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieModuleInformationProvider
import org.cangnova.cangjie.analysis.api.projectStructure.*
import org.cangnova.cangjie.analysis.api.util.withCaModuleEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirBuiltinsSessionFactory
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkCanceled
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.PrivateSessionConstructor
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.session.registerModuleData
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment

/**
 * 工程级 low-level CFIR session 缓存。
 *
 * 缓存按模块类型拆分存储，负责复用有效 session、创建新 session，并在返回前校验 session 有效性。
 */
@LLCfirInternals
class LLCfirSessionCache(
    /**
     * 当前缓存所属工程。
     */
    private val project: Project,

    /**
     * session 缓存底层存储集合。
     */
    val storage: LLCfirSessionCacheStorage,
) : Disposable {
    constructor(project: Project) : this(
        project,
        LLCfirSessionCacheStorage.createEmpty { LLCfirSessionCleaner(it.requestedDisposableOrNull) }
    )

    @OptIn(CaPlatformInterface::class)
    @CaCachedService
    /**
     * 模块信息提供器，用于跳过空模块依赖 session 创建。
     */
    private val moduleInformationProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CangJieModuleInformationProvider.getInstance(project)
    }

    /**
     * Returns the existing session if found, or creates a new session and caches it.
     * Analyzable session will be returned for a library module.
     *
     * Must be called from a read action.
     */
    fun getSession(module: CaModule, preferBinary: Boolean = false): LLCfirSession =
        when (module) {
            is CaBuiltinsModule if preferBinary ->
                LLCfirBuiltinsSessionFactory.getInstance(project).getBuiltinsSession(module.targetPlatform)

            is CaLibraryModule if preferBinary -> getBinaryLibraryCachedSession(module, storage.binaryCache)

            // Fallback dependencies aren't resolvable and thus always binary, regardless of `preferBinary`.
            is CaLibraryFallbackDependenciesModule -> getBinaryLibraryCachedSession(module, storage.libraryFallbackDependenciesCache)

            is CaDanglingFileModule -> getDanglingFileCachedSession(module)
            else -> getCachedSession(module, storage.sourceCache, factory = ::createSession)
        }

    /**
     * Returns the [LLCfirSession] for [module], to be used as a *dependency*, or `null` if it doesn't make sense to create such a session as
     * a dependency. This is an optimization for [CaModule]s of certain kinds, like empty modules.
     *
     * Dependency sessions are implicitly binary-preferred because sessions used as dependencies do not need to be resolvable.
     */
    @OptIn(CaPlatformInterface::class)
    fun getDependencySession(module: CaModule): LLCfirSession? {
        if (moduleInformationProvider?.isEmpty(module) == true) return null
        return getSession(module, preferBinary = true)
    }

    /**
     * 从 [storage] 中取得或创建二进制库 session。
     */
    private fun getBinaryLibraryCachedSession(module: CaModule, storage: SessionStorage): LLCfirSession =
        getCachedSession(module, storage) {
            createPlatformAwareSessionFactory(module).createBinaryLibrarySession(module)
        }

    /**
     * 取得 dangling file session。
     *
     * stable dangling module 使用普通缓存；unstable dangling module 需要检查文件修改戳，内容变化时重新创建 session。
     */
    private fun getDanglingFileCachedSession(module: CaDanglingFileModule): LLCfirSession {
        if (module.isStable) {
            return getCachedSession(module, storage.danglingFileSessionCache, ::createSession)
        }

        checkCanceled()

        val session = storage.unstableDanglingFileSessionCache.compute(module) { _, existingSession ->
            if (existingSession is LLCfirDanglingFileSession && !existingSession.hasFileModifications) {
                existingSession
            } else {
                createSession(module)
            }
        }

        requireNotNull(session)
        checkSessionValidity(session)

        return session
    }

    /**
     * 从指定 [storage] 中取得 [module] 的 session，不存在时通过 [factory] 创建。
     *
     * 对不能隔离创建的模块，先在 compute 外部创建 session，避免递归访问同一缓存导致更新异常。
     */
    private fun <T : CaModule> getCachedSession(module: T, storage: SessionStorage, factory: (T) -> LLCfirSession): LLCfirSession {
        checkCanceled()

        val session = if (module.supportsIsolatedSessionCreation) {
            storage.computeIfAbsent(module) { factory(module) }
        } else {
            // Non-isolated session creation may need to access other sessions, so we should create the session outside `computeIfAbsent` to
            // avoid recursive update exceptions.
            storage[module] ?: run {
                val newSession = factory(module)
                storage.computeIfAbsent(module) { newSession }
            }
        }

        checkSessionValidity(session)
        return session
    }

    /**
     * 校验缓存返回的 [session] 仍处于有效状态。
     */
    private fun checkSessionValidity(session: LLCfirSession) {
        requireWithAttachment(session.isValid, { "A session acquired via `getSession` should always be valid." }) {
            withCaModuleEntry("module", session.caModule)
            withEntry("invalidationInformation", session.invalidationInformation)
        }
    }

    /**
     * Whether the session for this [CaModule] can be created without getting other sessions from the cache. Should be kept in sync with
     * [createSession].
     */
    private val CaModule.supportsIsolatedSessionCreation: Boolean
        get() = this !is CaDanglingFileModule

    /**
     * 根据 [module] 类型创建对应 low-level CFIR session。
     */
    private fun createSession(module: CaModule): LLCfirSession {
        val sessionFactory = createPlatformAwareSessionFactory(module)
        return when (module) {
            is CaDanglingFileModule -> {
                //  Dangling file context must have an analyzable session, so we can properly compile code against it.
                val contextSession = getSession(module.contextModule, preferBinary = false)
                sessionFactory.createDanglingFileSession(module, contextSession)
            }
            is CaSourceModule -> sessionFactory.createSourcesSession(module)
            is CaBuiltinsModule -> sessionFactory.createResolvableLibrarySession(module)
            is CaLibraryModule -> sessionFactory.createResolvableLibrarySession(module)
            is CaLibrarySourceModule -> sessionFactory.createResolvableLibrarySession(module)
            is CaLibraryFallbackDependenciesModule -> sessionFactory.createBinaryLibrarySession(module)
            is CaNotUnderContentRootModule -> sessionFactory.createNotUnderContentRootResolvableSession(module)
            else -> error("Unexpected module kind: ${module::class.simpleName}")
        }
    }

    /**
     * 创建适用于 [module] 的平台感知 session 工厂。
     */
    private fun createPlatformAwareSessionFactory(module: CaModule): LLCfirAbstractSessionFactory {
        return LLCfirCommonSessionFactory(project)
    }

    /**
     * 释放 session cache 服务。
     *
     * 实际 session 清理由缓存存储和 invalidator 驱动，这里没有额外状态需要处理。
     */
    override fun dispose() {
    }

    companion object {
        /**
         * 取得工程级 session cache 服务。
         */
        fun getInstance(project: Project): LLCfirSessionCache = project.service()
    }
}

/**
 * 应用所有通过 extension point 注册的 session 配置器。
 */
internal fun LLCfirSessionConfigurator.Companion.configure(session: LLCfirSession) {
    val project = session.project
    for (extension in extensionPointName.getExtensionList(project)) {
        extension.configure(session)
    }
}

@Deprecated(
    "This is a dirty hack used only for one usage (building fir for psi from stubs) and it should be removed after fix of that usage",
    level = DeprecationLevel.ERROR
)
@OptIn(PrivateSessionConstructor::class)
/**
 * 创建只用于 stub-based PSI 构建场景的空 CFIR session。
 */
fun createEmptySession(): CfirSession {
    return object : CfirSession(Kind.Source) {}.apply {
        val moduleData = CfirSourceModuleData(
            Name.identifier("<stub module>"),
            dependencies = emptyList(),
            refinementDependencies = emptyList(),
            targetPlatform = CangJiePlatforms.defaultCangJiePlatform,
            platform = CfirPlatform.DEFAULT,
        )
        registerModuleData(moduleData)
        moduleData.bindSession(this)
    }
}
