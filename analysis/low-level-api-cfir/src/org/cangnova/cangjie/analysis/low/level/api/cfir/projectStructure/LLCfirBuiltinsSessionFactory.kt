/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.impl.base.projectStructure.CaBuiltinsModuleImpl
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.analysis.low.level.api.cfir.providers.LLCfirBuiltinsSessionProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirBuiltinsSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories.LLLibrarySymbolProviderFactory
import org.cangnova.cangjie.cfir.PrivateSessionConstructor
import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.platform.TargetPlatform
import java.util.concurrent.ConcurrentHashMap

/**
 * 为不同 [TargetPlatform] 创建并缓存低阶 CFIR builtins module 与 builtins session 的工程级工厂。
 *
 * Builtins module 可以在项目结构创建期间提前暴露为依赖；builtins session 则延迟到分析服务可用后创建，
 * 避免项目结构初始化阶段过早触发 session 组件注册。
 *
 * @param project 当前工厂所属工程。
 */
@OptIn(PrivateSessionConstructor::class, SessionConfiguration::class, CaPlatformInterface::class)
@LLCfirInternals
class LLCfirBuiltinsSessionFactory(private val project: Project) {
    /**
     * 所有 builtins session 共享的 CFIR 内建类型集合。
     */
    private val builtInTypes = CfirBuiltinTypes()

    /**
     * 按目标平台缓存的 builtins analysis API module。
     */
    private val builtinsModules = ConcurrentHashMap<TargetPlatform, CaBuiltinsModule>()

    /**
     * 按目标平台缓存的 builtins session。
     *
     * session 包装在 IntelliJ [CachedValue] 中，使其能跟随 session validity tracker 自动失效。
     */
    private val builtinsSessions = ConcurrentHashMap<TargetPlatform, CachedValue<LLCfirBuiltinsSession>>()

    /**
     * 当前工程的项目结构提供器，用于读取 builtins session 创建所需的语言版本设置。
     */
    private val projectStructureProvider: CangJieProjectStructureProvider =
        CangJieProjectStructureProvider.getInstance(project)

    /**
     * Returns the [targetPlatform]'s builtins [CaBuiltinsModule]. [getBuiltinsModule] should be used instead of [getBuiltinsSession] when a
     * [CaBuiltinsModule] is needed as a dependency for other [CaModule][org.cangnova.cangjie.analysis.api.projectStructure.CaModule]s. This
     * is because during project structure creation, we have to avoid the creation of the builtins *session*, as not all services might have
     * been registered at that point.
     */
    fun getBuiltinsModule(targetPlatform: TargetPlatform): CaBuiltinsModule =
        builtinsModules.getOrPut(targetPlatform) { CaBuiltinsModuleImpl(targetPlatform, project) }

    /**
     * 返回 [targetPlatform] 对应的 builtins session。
     *
     * session 按平台懒创建，并通过其 validity tracker 绑定缓存生命周期。
     */
    fun getBuiltinsSession(targetPlatform: TargetPlatform): LLCfirBuiltinsSession =
        builtinsSessions.getOrPut(targetPlatform) {
            CachedValuesManager.getManager(project).createCachedValue {
                val session = createBuiltinsSession(targetPlatform)
                CachedValueProvider.Result(session, session.createValidityTracker())
            }
        }.value

    /**
     * Invalidates all builtins modules and sessions.
     *
     * [invalidateAll] should be called after [global module state modification][org.cangnova.cangjie.analysis.api.platform.modification.KotlinGlobalModuleStateModificationEvent],
     * as well as after [module state modification][org.cangnova.cangjie.analysis.api.platform.modification.KotlinModuleStateModificationEvent]
     * of a [CaBuiltinsModule]. Modification of builtins might affect any session, so in addition to the builtins sessions, all other
     * sessions should also be invalidated.
     *
     * Builtins cannot be affected by out-of-block modification.
     *
     * The method must be called in a write action, or alternatively when the caller can guarantee that no other threads can perform
     * invalidation or code analysis until the invalidation is complete.
     */
    internal fun invalidateAll() {
        builtinsModules.clear()
        builtinsSessions.clear()
    }

    /**
     * 创建完整配置的 [LLCfirBuiltinsSession]。
     *
     * 创建流程会注册 IDE 公共组件、builtins symbol provider、CFIR provider 和模块数据；
     * builtins 使用 dummy lazy resolver，因为 builtins 声明来自库符号提供器而非源码 lazy resolve。
     */
    private fun createBuiltinsSession(targetPlatform: TargetPlatform): LLCfirBuiltinsSession {
        val builtinsModule = getBuiltinsModule(targetPlatform)
        val session = LLCfirBuiltinsSession(builtinsModule, builtInTypes)
        val moduleData = LLCfirModuleData(session)

        return session.apply {
            val languageVersionSettings = projectStructureProvider.libraryLanguageVersionSettings
            registerIdeComponents(project, languageVersionSettings, builtinsModule.contentScope)
            register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
            registerCommonComponents(languageVersionSettings)
            registerCommonComponentsAfterExtensionsAreConfigured()
            registerModuleData(moduleData)
            val cangjieScopeProvider = CfirCangJieScopeProvider()
            register(CfirCangJieScopeProvider::class, cangjieScopeProvider)

            val symbolProvider = createCompositeSymbolProvider(this) {
                addAll(
                    LLLibrarySymbolProviderFactory
                        .fromSettings(project)
                        .createBuiltinsSymbolProvider(session)
                )
            }

            register(CfirSymbolProvider::class, symbolProvider)
            register(CfirProvider::class, LLCfirBuiltinsSessionProvider(symbolProvider))
        }
    }

    companion object {
        /**
         * 取得工程级 builtins session 工厂服务。
         */
        fun getInstance(project: Project): LLCfirBuiltinsSessionFactory = project.service()
    }
}
