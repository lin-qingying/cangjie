package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.Package
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import java.util.concurrent.ConcurrentHashMap

/**
 * 反序列化上下文。
 *
 * 该对象承载当前包数据、跨包加载能力以及面向 `FullId` 的统一解析入口。
 */
class CfirDeserializationContext(
    /** 当前反序列化的 FlatBuffers Package。 */
    val pkg: Package,
    /** 当前包头。 */
    val header: CjoPackageHeader,
    /** 库模块元数据。 */
    val moduleData: CfirModuleData,
    /** `.cjo` 包管理器，用于跨包声明索引和包头装载。 */
    val cjoManager: CjoManager,
) {
    /** `allTypes` 索引 -> 已反序列化类型。 */
    val typeCache = ConcurrentHashMap<Int, ConeCangJieType>()

    /** `allDecls` 索引 -> 已反序列化声明。 */
    val declCache = ConcurrentHashMap<Int, CfirDeclaration>()

    /**
     * 声明/类型反序列化器本身持有递归检测与 owner 栈，不能跨线程共享。
     *
     * 这里把“最终结果缓存”留在 context 上，再按索引串行化实际 materialization，
     * 以保证同一个 `.cjo` 条目只会有一个共享结果进入缓存。
     */
    private val declMaterializationLocks = ConcurrentHashMap<Int, Any>()
    private val typeMaterializationLocks = ConcurrentHashMap<Int, Any>()

    /** 导入包索引 -> 包级声明索引。 */
    internal val importedPackageIndices = ConcurrentHashMap<Int, CjoPackageIndex>()

    /** FullId 统一解析器。 */
    internal val fullIdResolver: CjoFullIdResolver by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CjoFullIdResolver(this)
    }

    /**
     * `CfirDeclDeserializer` / `CfirTypeDeserializer` 都带有调用栈态，
     * 必须按次创建，不能像 Kotlin 的无栈 member deserializer 那样直接复用实例。
     */
    internal fun createTypeDeserializer(): CfirTypeDeserializer = CfirTypeDeserializer(this)

    internal fun createDeclDeserializer(): CfirDeclDeserializer =
        CfirDeclDeserializer(this, createTypeDeserializer())

    internal fun declMaterializationLock(index: Int): Any =
        declMaterializationLocks.computeIfAbsent(index) { Any() }

    internal fun releaseDeclMaterializationLock(index: Int, lock: Any) {
        declMaterializationLocks.remove(index, lock)
    }

    internal fun typeMaterializationLock(index: Int): Any =
        typeMaterializationLocks.computeIfAbsent(index) { Any() }

    internal fun releaseTypeMaterializationLock(index: Int, lock: Any) {
        typeMaterializationLocks.remove(index, lock)
    }
}
