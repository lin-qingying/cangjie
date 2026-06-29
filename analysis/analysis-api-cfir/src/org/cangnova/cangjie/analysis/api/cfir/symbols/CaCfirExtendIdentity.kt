package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendSemanticModel
import org.cangnova.cangjie.cfir.session.extendIndexStore
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
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
 */
internal fun CaCfirSession.resolveExtendIdentity(symbol: CfirExtendSymbol): CaCfirResolvedExtendIdentity {
    // 仓颉 `extend` 的稳定身份建立在 EXTENSIONS 阶段后的语义模型上。
    // 因此统一在 identity 入口推进到该阶段，再读取 extendIndexStore。
    symbol.lazyResolveToPhase(CfirResolvePhase.EXTENSIONS)
    val semanticModel = cfirSession.extendIndexStore.modelForDeclaration(symbol.cfir)
        ?: error("Extend `${symbol}` is missing semantic model in extendIndexStore")
    val sourceExtendPsi = symbol.backingPsiIfApplicable as? CjExtend
    return CaCfirResolvedExtendIdentity(
        stableIdentity = semanticModel.toPublicSymbolIdentity(),
        extendId = buildPublicExtendId(semanticModel, sourceExtendPsi),
        extendPsi = sourceExtendPsi,
        packageFqName = semanticModel.packageFqName,
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
