package org.cangnova.cangjie.analysis.api.signatures

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor

/**
 * 函数族 use-site 签名。
 *
 * - 在 [CaCallableSignature] 的基础上,把符号族约束收窄到 [CaFunctionSymbol];
 * - 暴露值参数列表的 use-site 视图([valueParameters]);
 * - `substitute()` 在公开 API 上返回精确的 [CaFunctionSignature] 类型,
 *   而不是退回到通用 [CaCallableSignature]。
 *
 * 对齐 Kotlin Analysis API 的 `KaFunctionSignature`。
 *
 * @param S 实际函数符号类型(协变)。
 */
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaFunctionSignature<out S : CaFunctionSymbol> : CaCallableSignature<S> {
    /**
     * 经过 use-site 替换的值参数签名列表,对齐 [CaFunctionSymbol.valueParameters]。
     */
    val valueParameters: List<CaVariableSignature<CaValueParameterSymbol>>

    /**
     * 函数签名特化的替换入口,返回 [CaFunctionSignature] 以保留精确类型。
     */
    @CaExperimentalApi
    abstract override fun substitute(substitutor: CaSubstitutor): CaFunctionSignature<S>
}
