package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.source.CjSourceLocation

/** 从 CJO 分别还原声明起止和名称的源码位置，保留每个位置自身的文件归属。 */
data class CfirCjoDeclarationPosition(
    /** 声明起点；缺失、无效或其源码包不可用时为空。 */
    val declarationLocation: CjSourceLocation?,
    /** 声明终点，独立保留其源码文件。 */
    val endLocation: CjSourceLocation?,
    /** CJO identifierPos；旧 CJO 缺少该字段时为空。 */
    val identifierLocation: CjSourceLocation?,
) {
    /** 相关诊断说明的名称锚点；旧 CJO 缺少 identifierPos 时不伪造该位置。 */
    val relatedInformationLocation: CjSourceLocation?
        get() = identifierLocation
}

/**
 * 单个反序列化声明的二进制 source element。
 *
 * 同时保留声明起点和 identifierPos；诊断锚点策略由反序列化位置 owner 统一提供，调用方不再重复解码 CJO。
 */
class CfirCjoDeclarationSourceElement(
    /** 该声明从 CJO 恢复的完整位置元数据。 */
    val declarationPosition: CfirCjoDeclarationPosition?,
    /** 与当前 CFIR 声明对应的稳定符号身份。 */
    stableIdentity: Any,
) : CjBinarySourceElement(
    debugText = declarationPosition?.declarationLocation?.let { "CJO declaration at ${it.filePath}:${it.line}:${it.column}" }
        ?: "CJO declaration without source position",
    binaryFilePath = declarationPosition?.declarationLocation?.filePath,
    stableIdentity = stableIdentity,
    sourceLocation = declarationPosition?.declarationLocation,
) {
    /** 相关诊断说明仅使用 CJO identifierPos；缺少名称位置时不构造替代位置。 */
    val relatedInformationLocation: CjSourceLocation?
        get() = declarationPosition?.relatedInformationLocation
}

/** 为 CJO 声明标识符提供可用于主诊断的二进制 source。 */
fun CfirCjoDeclarationSourceElement.identifierSourceElementOrNull(): CjBinarySourceElement? =
    relatedInformationLocation?.let { identifierLocation ->
        CfirCjoDeclarationIdentifierSourceElement(this, identifierLocation)
    }

private class CfirCjoDeclarationIdentifierSourceElement(
    declarationSource: CfirCjoDeclarationSourceElement,
    identifierLocation: CjSourceLocation,
) : CjBinarySourceElement(
    debugText = "CJO declaration identifier at ${identifierLocation.filePath}:${identifierLocation.line}:${identifierLocation.column}",
    binaryFilePath = identifierLocation.filePath,
    stableIdentity = declarationSource to "identifier",
    sourceLocation = identifierLocation,
)

/** CJO 声明的不可变 wire 身份，供导出阶段按原始快照定位其序列化信息。 */
data class CfirCjoDeclarationOrigin(
    /** 承载声明的原始 CJO 文件路径。 */
    val packagePath: String,
    /** 原始 `Package.allDecls` 的 0-based 索引。 */
    val declarationIndex: Int,
    /** 原始声明表明确保存的物理顶层位；不能用 ClassId 或发现组的排列推断。 */
    val isTopLevel: Boolean,
) {
    init {
        require(packagePath.isNotBlank())
        require(declarationIndex >= 0)
    }
}

private object CjoDeclarationPositionKey : CfirDeclarationDataKey() {
    override val rawCopyPolicy get() = org.cangnova.cangjie.cfir.CfirRawDeclarationDataPolicies.cjoPosition
}

/** CJO 携带的原始声明位置，供二进制诊断锚点与反序列化候选 note 共用。 */
var CfirDeclaration.cjoDeclarationPosition: CfirCjoDeclarationPosition?
    by CfirDeclarationDataRegistry.data(CjoDeclarationPositionKey)

private object CjoDeclarationSourceElementKey : CfirDeclarationDataKey()

/** 反序列化声明的统一二进制 source element。 */
var CfirDeclaration.cjoDeclarationSourceElement: CfirCjoDeclarationSourceElement?
    by CfirDeclarationDataRegistry.data(CjoDeclarationSourceElementKey)

private object CjoDeclarationOriginKey : CfirDeclarationDataKey()

/** 原始 CJO 包路径与声明索引；不从显示名称、源码位置或模块名反推。 */
var CfirDeclaration.cjoDeclarationOrigin: CfirCjoDeclarationOrigin?
    by CfirDeclarationDataRegistry.data(CjoDeclarationOriginKey)

private object CjoDeclarationIndexKey : CfirDeclarationDataKey() {
    override val rawCopyPolicy get() = org.cangnova.cangjie.cfir.CfirRawDeclarationDataPolicies.cjoDeclarationIndex
}

/** 当前 CJO 的零基 declaration pool 下标，compiled header 用它定位同一份只读 signature。 */
var CfirDeclaration.cjoDeclarationIndex: Int?
    by CfirDeclarationDataRegistry.data(CjoDeclarationIndexKey)
