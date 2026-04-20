package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendSemanticModel
import org.cangnova.cangjie.cfir.session.extendIndexStore
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
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
    val packageFqName: FqName,
    val fileName: String,
    val declarationIndexInFile: Int,
    val targetClassId: ClassId?,
    val inheritedInterfaceSemanticKeys: List<String>,
)

internal data class CaCfirResolvedExtendIdentity(
    val stableIdentity: CaCfirExtendSymbolIdentity,
    val extendId: String,
    val extendPsi: CjExtend?,
    val packageFqName: FqName,
)

/**
 * 为 extend 声明统一解析稳定身份。
 *
 * 规则分成两层：
 * 1. cache key / pointer restore 使用 `extendIndexStore` 提供的正式语义身份；
 * 2. public `extendId` 继续保留现有文本投影，作为公开 API 的兼容视图。
 */
internal fun CaCfirSession.resolveExtendIdentity(symbol: CfirExtendSymbol): CaCfirResolvedExtendIdentity {
    val semanticModel = cfirSession.extendIndexStore.modelForDeclaration(symbol.cfir)
        ?: error("Extend `${symbol}` is missing semantic model in extendIndexStore")
    val sourceExtendPsi = symbolQueries.lookupSourcePsi(symbol) as? CjExtend
    return CaCfirResolvedExtendIdentity(
        stableIdentity = semanticModel.toPublicSymbolIdentity(),
        extendId = buildPublicExtendId(semanticModel, sourceExtendPsi),
        extendPsi = sourceExtendPsi,
        packageFqName = semanticModel.packageFqName,
    )
}

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

private fun normalizeExtendTypeText(rendered: String): String {
    return rendered.removePrefix("R|").removeSuffix("|").trim()
}
