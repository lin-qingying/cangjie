package org.cangnova.cangjie.cfir.serialization.provider

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.deserialization.ModuleDataProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoLoadedPackage
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.implicitSystemAnnotationsProvider
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import java.util.concurrent.ConcurrentHashMap

/**
 * 原 named metadata producer 的不可变装配身份。
 * 同一 manager 可服务多个模块图，owner、配置与导入 authority 绝不登记为可覆盖的 manager 默认值。
 */
class CjoDeserializationBinding(
    val session: CfirSession,
    val ownManager: CjoManager,
    val scopeProvider: CfirCangJieScopeProvider,
    val libraryModuleData: CfirModuleData,
    val moduleDataProvider: ModuleDataProvider,
    val dependencyManager: CjoManager = ownManager,
    private val importedPackageContextProvider: CjoPackageContextProvider? = null,
) : CjoPackageContextProvider {
    internal val languageVersionSettings = session.languageVersionSettings
    private val systemAnnotationsProvider = session.implicitSystemAnnotationsProvider
    internal val implicitSystemAnnotations = systemAnnotationsProvider.annotations.toSet()

    /** 原 binding 的导入边在本代固定；后续 FullId、类型、注解不能按同名包重新选择另一目标。 */
    private data class TargetKey(
        val packageName: String,
        val mode: CjoDeserializationMode,
        val requestingModule: CfirModuleData?,
    )
    private val importedContexts = ConcurrentHashMap<TargetKey, CfirDeserializationContext>()

    init {
        require(ownManager.declarationMaterialization === dependencyManager.declarationMaterialization) {
            "CJO binding requires one metadata graph materialization transaction"
        }
        require(libraryModuleData in moduleDataProvider.allModuleData) {
            "CJO binding does not contain its declared library module"
        }
    }

    fun getOwnPackageContext(name: String, mode: CjoDeserializationMode): CfirDeserializationContext? =
        selectOwnSnapshot(name, null)?.let { contextForSnapshot(it, mode) }

    /** CJMP 专有父来源并列进入普通名称发现，每个来源仍保有自己的装配 context。 */
    internal fun getOwnPackageContexts(name: String, mode: CjoDeserializationMode): List<CfirDeserializationContext> =
        ownManager.loadPackageSnapshots(name).map { contextForSnapshot(it, mode) }

    internal fun selectOwnSnapshot(name: String, requestingModule: CfirModuleData?): CjoLoadedPackage? =
        selectSnapshot(ownManager, name, requestingModule)

    /** 同包多快照按原 requesting module 的依赖闭包选择；没有明确权威时直接拒绝歧义。 */
    private fun selectSnapshot(manager: CjoManager, name: String, requestingModule: CfirModuleData?): CjoLoadedPackage? {
        val candidates = manager.loadPackageSnapshots(name)
        if (candidates.size <= 1) return candidates.singleOrNull()
        checkNotNull(requestingModule) { "Multiple CJO sources for $name require an original requesting module" }
        val reachable = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirModuleData, Boolean>())
        val queue = ArrayDeque<CfirModuleData>()
        queue.add(requestingModule)
        while (queue.isNotEmpty()) {
            val module = queue.removeFirst()
            if (!reachable.add(module)) continue
            queue.addAll(module.dependencies)
            queue.addAll(module.refinementDependencies)
        }
        val selected = candidates.filter { moduleDataProvider.getModuleData(it.sourcePath) in reachable }
        check(selected.size == 1) {
            "Ambiguous imported CJO snapshots for '$name' in module '${requestingModule.name}'"
        }
        return selected.single()
    }

    /** 来源已确定的 reader 只消费本 manager 发布的实际 snapshot，不借 name-selected lookup。 */
    fun contextForSnapshot(snapshot: CjoLoadedPackage, mode: CjoDeserializationMode): CfirDeserializationContext {
        require(publishedSnapshot(snapshot.header, snapshot.sourcePath) === snapshot) {
            "CJO snapshot was not published by its original own manager"
        }
        val owner = checkNotNull(moduleDataProvider.getModuleData(snapshot.sourcePath)) {
            "No module owner for CJO " + snapshot.sourcePath
        }
        val authority = dependencyManager.deserializedSymbolLookups.forBinding(this, owner, mode)
        return CfirDeserializationContext(
            pkg = snapshot.pkg, header = snapshot.header, moduleData = owner,
            cjoManager = dependencyManager, classSymbolLookup = authority,
            sourcePath = snapshot.sourcePath, mode = mode,
            packageContextProvider = this, binding = this, snapshotIdentity = snapshot,
        ).canonical()
    }

    /** 两个原装配 manager 的已发布来源均按 header identity 查；没有名称或默认 owner 推断。 */
    internal fun publishedSnapshot(
        header: org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader, sourcePath: java.nio.file.Path?,
    ): CjoLoadedPackage? {
        val own = ownManager.loadedSnapshotIdentity(header, sourcePath)
        val dependency = dependencyManager.loadedSnapshotIdentity(header, sourcePath)
        require(own == null || dependency == null || own === dependency) { "CJO header has two source publishers" }
        return if (own != null) own else dependency
    }

    /**
     * 旧 constructor 可传同一正式 provider 的 router 入口，它是本 binding 的明确转发。
     * 任意外部 router 必须在初始 producer 中装配；不能由先到 context 覆盖原服务。
     */
    internal fun contextRouter(router: CjoPackageContextProvider): CjoPackageContextProvider {
        require(router === this || router is CfirDeserializedSymbolProvider && router.deserializationBinding === this) {
            "CJO context router is not authorized by its original producer"
        }
        return this
    }

    /** constructor 的 lookup 参数证明原 producer identity；实际查询一律由当前 snapshot 的 context 承担。 */
    internal fun requireLookupAuthority(lookup: CfirClassLikeSymbolLookup) {
        require(lookup is CfirDeserializedSymbolProvider && lookup.deserializationBinding === this) {
            "CJO context lookup is not authorized by its original producer"
        }
    }

    /** 派生 provider 可以继承或重复原 import 服务；不能携带一个被 binding 悄悄忽略的替换参数。 */
    internal fun requireImportedRouterAuthority(router: CjoPackageContextProvider?) {
        require(router == null || router === importedPackageContextProvider) {
            "Derived CJO provider cannot replace its original import router"
        }
    }

    internal fun requireSessionAuthority(moduleData: CfirModuleData) {
        require(moduleData.session === session && session.languageVersionSettings === languageVersionSettings &&
            session.implicitSystemAnnotationsProvider === systemAnnotationsProvider &&
            systemAnnotationsProvider.annotations == implicitSystemAnnotations) {
            "CJO producer session services changed inside a metadata generation"
        }
    }

    override fun getPackageContext(name: String, mode: CjoDeserializationMode): CfirDeserializationContext? =
        getPackageContext(name, mode, null)

    internal fun getPackageContext(name: String, mode: CjoDeserializationMode, requestingModule: CfirModuleData?): CfirDeserializationContext? {
        selectOwnSnapshot(name, requestingModule)?.let { return contextForSnapshot(it, mode) }
        val key = TargetKey(name, mode, requestingModule)
        importedContexts[key]?.let { return it.validatedTarget(this, name, mode) }
        val target = if (importedPackageContextProvider != null) {
            importedPackageContextProvider.getPackageContext(name, mode)
        } else {
            selectSnapshot(dependencyManager, name, requestingModule)?.let { contextForSnapshot(it, mode) }
        } ?: return null
        val accepted = target.validatedTarget(this, name, mode)
        return (importedContexts.putIfAbsent(key, accepted) ?: accepted).validatedTarget(this, name, mode)
    }
}
