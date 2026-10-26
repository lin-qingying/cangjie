package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendSemanticModel
import org.cangnova.cangjie.cfir.session.extendIndexStoreOrNull
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.buildExtendId

/**
 * `extend` 的稳定语义身份。
 *
 * 这里对齐 Kotlin pointer/cache 体系的思路：内部恢复依赖结构化身份对象，
 * 不再把公开 `extendId` 文本反向当作唯一身份来源。
 */
internal data class CaCfirExtendSymbolIdentity(
    /**
     * extend 声明所在包名。
     */
    val packageFqName: FqName,
    /**
     * extend 声明所在文件名。
     */
    val fileName: String,
    /**
     * extend 声明在文件中的稳定声明序号。
     */
    val declarationIndexInFile: Int,
    /**
     * 被扩展目标类型的 classId，无法稳定解析时为 null。
     */
    val targetClassId: ClassId?,
    /**
     * extend 继承接口的稳定语义 key 列表。
     */
    val inheritedInterfaceSemanticKeys: List<String>,
)

/**
 * 同时包含内部稳定身份和公开 extendId 的解析结果。
 */
internal data class CaCfirResolvedExtendIdentity(
    /**
     * 供缓存和 pointer 恢复使用的稳定身份。
     */
    val stableIdentity: CaCfirExtendSymbolIdentity,
    /**
     * 公开 API 暴露的 extendId 文本。
     */
    val extendId: String,
    /**
     * extend 声明对应的源码 PSI。
     */
    val extendPsi: CjExtend?,
    /**
     * extend 声明所在包名。
     */
    val packageFqName: FqName,
)

/**
 * 为 extend 声明统一解析稳定身份。
 *
 * 规则分成两层：
 * 1. cache key / pointer restore 使用 `extendIndexStore` 提供的正式语义身份；
 * 2. public `extendId` 继续保留现有文本投影，作为公开 API 的兼容视图。
 *
 * 语义模型按**声明所属 session** 的 extend 索引解析：基本类型目标的查询天然命中
 * 标准库的大量 `extend Int64 <: Comparable` 等库声明，库 extend 的模型不在 use-site
 * session 的 `CfirExtendIndexStore` 里（该索引只随源码 EXTENSIONS 阶段建立），
 * 固定查 use-site store 会让所有库 extend 抛 `missing semantic model`。
 *
 * 库 extend 没有 EXTENSIONS 阶段语义模型（源码管线的产物），按结构信息构造
 * **降级身份**：包名取 provider 的 `getPackageFqName`，目标 ClassId 取
 * `extendedTypeRef` 的展开结果，文件名与声明序号用稳定占位。降级身份只服务
 * 符号缓存与展示；pointer restore 对库 extend 仍走 PSI/符号身份路径。
 */
internal fun CaCfirSession.resolveExtendIdentity(symbol: CfirExtendSymbol): CaCfirResolvedExtendIdentity {
    // 仓颉 `extend` 的稳定身份建立在 EXTENSIONS 阶段后的语义模型上。
    // 因此统一在 identity 入口推进到该阶段，再读取 extendIndexStore。
    symbol.lazyResolveToPhase(CfirResolvePhase.EXTENSIONS)
    val declaration = symbol.cfir
    val declarationSession = declaration.moduleData.session
    val semanticModel = declarationSession.extendIndexStoreOrNull?.modelForDeclaration(declaration)
    val sourceExtendPsi = symbol.backingPsiIfApplicable as? CjExtend
    if (semanticModel != null) {
        return CaCfirResolvedExtendIdentity(
            stableIdentity = semanticModel.toPublicSymbolIdentity(),
            extendId = buildPublicExtendId(semanticModel, sourceExtendPsi),
            extendPsi = sourceExtendPsi,
            packageFqName = semanticModel.packageFqName,
        )
    }

    // 库 extend：无源码语义模型，按结构信息构造降级身份。
    val packageFqName = declarationSession.extendProviderOrNull
        ?.getPackageFqName(declaration)
        ?: declaration.moduleData.name.let { FqName(it.asString()) }
    val targetClassId = declaration.extendedTypeRef.coneTypeOrNull
        ?.fullyExpandedType(declarationSession)
        ?.classIdOrPrimitiveClassId
    val stableIdentity = CaCfirExtendSymbolIdentity(
        packageFqName = packageFqName,
        fileName = LIBRARY_EXTEND_FILE_NAME,
        declarationIndexInFile = -1,
        targetClassId = targetClassId,
        inheritedInterfaceSemanticKeys = emptyList(),
    )
    return CaCfirResolvedExtendIdentity(
        stableIdentity = stableIdentity,
        extendId = buildPublicExtendId(semanticModel = null, extend = declaration, sourceExtendPsi),
        extendPsi = sourceExtendPsi,
        packageFqName = packageFqName,
    )
}

/**
 * 库 extend 身份使用的稳定文件名占位：反序列化声明没有源码文件身份。
 */
private const val LIBRARY_EXTEND_FILE_NAME: String = "<library>"

/**
 * 无语义模型时按声明结构生成公开 `extendId`（对齐 [buildPublicExtendId] 的文本投影）。
 */
private fun buildPublicExtendId(
    semanticModel: CfirExtendSemanticModel?,
    extend: CfirExtend,
    sourceExtendPsi: CjExtend?,
): String {
    if (semanticModel != null) return buildPublicExtendId(semanticModel, sourceExtendPsi)
    sourceExtendPsi?.let { return it.getExtendId() }

    val readableRenderer = CfirRenderer.withReadability()
    val extendedTypeText = normalizeExtendTypeText(readableRenderer.renderElementAsString(extend.extendedTypeRef))
    val superTypeTexts = extend.superTypeRefs
        .map { superTypeRef -> normalizeExtendTypeText(readableRenderer.renderElementAsString(superTypeRef)) }
        .filter(String::isNotBlank)
    return buildExtendId(
        packageFqName = extend.moduleData.session.extendProviderOrNull
            ?.getPackageFqName(extend)
            ?: FqName(extend.moduleData.name.asString()),
        receiverTypeText = extendedTypeText,
        superTypeTexts = superTypeTexts,
    )
}

/**
 * 将 low-level extend 语义模型转换为公开符号缓存使用的稳定身份。
 */
internal fun CfirExtendSemanticModel.toPublicSymbolIdentity(): CaCfirExtendSymbolIdentity =
    CaCfirExtendSymbolIdentity(
        packageFqName = packageFqName,
        fileName = fileName,
        declarationIndexInFile = declarationIndexInFile,
        targetClassId = targetClassId,
        inheritedInterfaceSemanticKeys = inheritedInterfaceSemanticKeys,
    )

/**
 * public `extendId` 是兼容视图，不承担内部稳定身份职责。
 *
 * source PSI 存在时沿用 PSI/stub 已固化的投影；否则退回到同一套公开字符串算法，
 * 仅用于 `CaExtendSymbol.extendId` 暴露，而非 cache/pointer 恢复。
 */
private fun buildPublicExtendId(
    semanticModel: CfirExtendSemanticModel,
    extendPsi: CjExtend?,
): String {
    extendPsi?.let { return it.getExtendId() }

    val extend = semanticModel.declaration
    val readableRenderer = CfirRenderer.withReadability()
    val extendedTypeText = normalizeExtendTypeText(readableRenderer.renderElementAsString(extend.extendedTypeRef))
    val superTypeTexts = extend.superTypeRefs
        .map { superTypeRef -> normalizeExtendTypeText(readableRenderer.renderElementAsString(superTypeRef)) }
        .filter(String::isNotBlank)
    return buildExtendId(
        packageFqName = semanticModel.packageFqName,
        receiverTypeText = extendedTypeText,
        superTypeTexts = superTypeTexts,
    )
}

/**
 * 规范化 renderer 产出的 extend 类型文本。
 */
private fun normalizeExtendTypeText(rendered: String): String {
    return rendered.removePrefix("R|").removeSuffix("|").trim()
}
