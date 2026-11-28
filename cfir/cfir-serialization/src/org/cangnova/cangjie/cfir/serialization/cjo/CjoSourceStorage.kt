package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.source.CfirSourceGraph
import org.cangnova.cangjie.cfir.source.CfirSourceGraphPublicationOwner
import org.cangnova.cangjie.cfir.source.CfirSourceImportedFunctionHeader
import org.cangnova.cangjie.cfir.source.CfirSourcePositionIdentity
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.session.CfirSession

/** 原 context 首次读取时保存的位置标量；null 宿主槽与存在的无效坐标不合并。 */
internal data class CjoSavedSourcePosition(
    val file: UInt,
    val pkgId: UInt,
    val line: Int,
    val column: Int,
    val ignore: Boolean,
)

/** 原 union 的确定表示；子值只保留实际 typed identity，不能保存 raw 递归 reader。 */
internal sealed interface CjoSavedConstantStorage {
    data class Scalar(val value: CjoConstValueInfo) : CjoSavedConstantStorage
    data object Array : CjoSavedConstantStorage
    data class Composite(val fields: List<CjoSavedConstantField>) : CjoSavedConstantStorage
}

internal data class CjoSavedConstantField(
    val name: String,
    val type: ConeCangJieType,
    val value: CfirConstantValue,
    /** Tuple 使用实际 ordinal，没有虚构字段 symbol。 */
    val symbol: CfirCallableSymbol<*>?,
)

/** inline 根以宿主槽/ordinal 定义身份；只有 CompositeReference 使用实际 v 池共享。 */
internal sealed interface CjoConstantRootSlot {
    data class Variable(val tablePosition: Int) : CjoConstantRootSlot
    data class Pattern(val tablePosition: Int, val ordinal: Int) : CjoConstantRootSlot
}

/** 每个真实宿主 use 保留自己的保存类型；它不参与值对象或宿主身份合并。 */
internal data class CjoSavedConstantRoot(val typeReference: UInt, val type: ConeCangJieType, val value: CfirConstantValue)

/**
 * 正式库 context 的完成产品及原存储事实。构造只允许该 context 发布后使用；后续 export
 * freeze 将来源字符串/header 标量复制成格式输入，编码器不读取这里的 source.pkg。
 */
class CjoSourceExportSnapshot internal constructor(
    val source: CjoLoadedPackage,
    internal val binderIdentity: Any,
    internal val domain: CfirSession,
    val graph: CfirSourceGraph?,
    internal val declarations: Map<Int, CfirDeclaration>,
    internal val types: Map<Int, ConeCangJieType>,
    internal val headers: Map<CfirCallableDeclaration, CfirSourceImportedFunctionHeader>,
    internal val positions: Map<CfirSourcePositionIdentity, CjoSavedSourcePosition>,
    internal val constants: Map<CfirConstantValue, CjoSavedConstantStorage>,
    internal val constantRoots: Map<CjoConstantRootSlot, CjoSavedConstantRoot>,
    internal val publicationIdentity: Any,
    internal val graphPublications: Map<CfirSourceGraph, CfirSourceGraph>,
    internal val positionFileBindings: Map<CjoSavedFileReference, CjoPositionFileBinding>,
) {
    /** 仅查询本次捕获的成功发布目录，不向原 owner 请求新代或装载内容。 */
    internal fun finalPublishedGraph(original: CfirSourceGraph): CfirSourceGraph {
        val result = checkNotNull(graphPublications[original]) { "SOURCE graph has no successful publication in its actual context" }
        check(graphPublications[result] === result) { "SOURCE publication must resolve directly to its completed final product" }
        return result
    }
}

/** 原 graph 的准备路由；全部方法被动，不给出 header/body/包内容生产权限。 */
internal interface CjoSourceGraphPublicationOwner : CfirSourceGraphPublicationOwner {
    val binderIdentity: Any
    fun completedSnapshotOf(original: CfirSourceGraph): CjoSourceExportSnapshot
    fun currentPublicationIdentity(): Any
    fun requirePublicationUnchanged(publicationIdentity: Any)
    fun <T> withReadOnlyContent(action: () -> T): T
}

/** 格式来源能力沿正式 deserialized provider 暴露；manager 本身没有语义 binder。 */
interface CjoSourceExportSnapshotProvider : CjoLoadedPackageSnapshotProvider {
    fun sourceExportSnapshotOf(declaration: CfirDeclaration, includeContent: Boolean): CjoSourceExportSnapshot?
    /** 只消费既有 context 已完成产品；不得解码、冷装载或委托上面的生产入口。 */
    fun completedSourceExportSnapshotOf(declaration: CfirDeclaration, includeContent: Boolean): CjoSourceExportSnapshot?
    /** 仅完成显式定义/header/content 选择；匹配请求的解码错误保持原异常传播。 */
    fun definitionExportSnapshotOf(request: CjoDefinitionSnapshotRequest): CjoDefinitionSnapshotResult
    /** 只查该原 request 的成功目录，不冷装载或补 scope。 */
    fun completedDefinitionExportSnapshotOf(request: CjoDefinitionSnapshotRequest): CjoDefinitionSnapshotResult
}
