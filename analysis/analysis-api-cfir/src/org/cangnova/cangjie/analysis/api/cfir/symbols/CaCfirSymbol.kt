package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaModifierExplicitness
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.descriptors.Visibilities


/**
 * 所有 CFIR 后端公开符号的基础协议。
 */
internal interface CaCfirSymbol<out S : CfirBasedSymbol<*>> : CaSymbol, CaLifetimeOwner {
    /**
     * 承载公开符号语义的底层 CFIR 符号。
     */
    val cfirSymbol: S

    /**
     * 底层 CFIR 符号；符号不可恢复时返回 `null`。
     *
     * 这是符号层**唯一**的降级入口。Kotlin `KaFirSymbol` 只有一个非空 `firSymbol`，
     * 因为 FIR 由 PSI 构建、每个声明必有符号；CFIR 不同——库声明可能尚未进入索引、
     * 反编译 PSI 可能失效，恢复会真的失败。
     *
     * 默认实现返回 [cfirSymbol]：只持有符号、不会恢复失败的实现（合成/局部符号）无需覆写。
     * PSI 承载的符号在 [CaCfirPsiSymbol] 覆写为带记忆化的真实实现。
     */
    val cfirSymbolOrNull: S? get() = cfirSymbol

    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    val analysisSession: CaCfirSession
    /**
     * 当前 session 里的公开符号构建器。
     */
    val builder: CaSymbolByCfirBuilder get() = analysisSession.cfirSymbolBuilder

    /**
     * 当前符号的生命周期 token。
     */
    override val token: CaLifetimeToken get() = analysisSession.token
    /**
     * 当前符号在公开 API 中呈现的来源。
     */
    override val origin: CaSymbolOrigin get() = withValidityAssertion { symbolOrigin() }
}

/**
 * 根据底层 CFIR declaration origin 推导公开符号 origin。
 */
internal fun CaCfirSymbol<*>.symbolOrigin(): CaSymbolOrigin = cfirSymbol.cfir.cjSymbolOrigin()

/**
 * 将 CFIR 声明 origin 转换为公开 Analysis API 符号 origin。
 */
internal fun CfirDeclaration.cjSymbolOrigin(): CaSymbolOrigin = origin.asPublicOrigin()

/**
 * 推导符号在公开 API 中的声明位置类别。
 */
internal fun CaCfirSymbol<*>.getSymbolKind(): CaSymbolLocation {
    val cfirSymbol = cfirSymbol
    return when {
        cfirSymbol is CfirClassLikeSymbol<*> -> CaSymbolLocation.TOP_LEVEL
        else -> CaSymbolLocation.LOCAL
    }
}

/**
 * 将 CFIR 声明 origin 枚举映射为公开符号 origin 枚举。
 */
internal fun CfirDeclarationOrigin.asPublicOrigin(): CaSymbolOrigin = when (this) {
    CfirDeclarationOrigin.Source -> CaSymbolOrigin.SOURCE
    CfirDeclarationOrigin.Library -> CaSymbolOrigin.LIBRARY
    CfirDeclarationOrigin.ImplicitDefault -> CaSymbolOrigin.IMPLICIT_DEFAULT
    CfirDeclarationOrigin.GenericInstantiation -> CaSymbolOrigin.GENERIC_INSTANTIATION
    CfirDeclarationOrigin.Extension -> CaSymbolOrigin.EXTENSION
    CfirDeclarationOrigin.SamConstructor -> CaSymbolOrigin.SAM_CONSTRUCTOR
    CfirDeclarationOrigin.IntersectionOverride -> CaSymbolOrigin.SYNTHETIC
    CfirDeclarationOrigin.SubstitutionOverride.DeclarationSite -> CaSymbolOrigin.SUBSTITUTION_OVERRIDE_DECLARATION_SITE
    CfirDeclarationOrigin.SubstitutionOverride.CallSite -> CaSymbolOrigin.SUBSTITUTION_OVERRIDE_CALL_SITE
    is CfirDeclarationOrigin.Synthetic -> CaSymbolOrigin.SYNTHETIC
}

/**
 * 将编译器 visibility 映射为公开符号 visibility。
 */
internal fun Visibility.asPublicVisibility() = when (this) {
    Visibilities.Private -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility.PRIVATE
    Visibilities.PrivateToThis -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility.PRIVATE_TO_THIS
    Visibilities.Protected -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility.PROTECTED
    Visibilities.Internal -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility.INTERNAL
    Visibilities.Public -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility.PUBLIC
    Visibilities.Local -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility.LOCAL
    else -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility.UNKNOWN
}

/**
 * 将编译器 modality 映射为公开符号 modality。
 */
internal fun Modality.asPublicModality() = when (this) {
    Modality.FINAL -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality.FINAL
    Modality.SEALED -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality.SEALED
    Modality.OPEN -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality.OPEN
    Modality.ABSTRACT -> org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality.ABSTRACT
}

/** 将 CFIR 显式性投影到公开 Analysis API 类型。 */
internal fun CfirModifierExplicitness.asPublicExplicitness(): CaModifierExplicitness = when (this) {
    CfirModifierExplicitness.EXPLICIT -> CaModifierExplicitness.EXPLICIT
    CfirModifierExplicitness.IMPLICIT -> CaModifierExplicitness.IMPLICIT
    CfirModifierExplicitness.UNKNOWN -> CaModifierExplicitness.UNKNOWN
}

/** 将 CFIR 声明状态中的可见性显式性映射到公开 Analysis API。 */
internal fun CfirDeclarationStatus?.asPublicVisibilityExplicitness(): CaModifierExplicitness =
    this?.visibilityExplicitness?.asPublicExplicitness() ?: CaModifierExplicitness.UNKNOWN

/** 没有有效 modality 的声明不公开 modality 显式性。 */
internal fun CfirDeclarationStatus?.asPublicModalityExplicitness(): CaModifierExplicitness = when {
    this?.modality == null -> CaModifierExplicitness.UNKNOWN
    else -> modalityExplicitness.asPublicExplicitness()
}
