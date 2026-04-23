package org.cangnova.cangjie.analysis.api.signatures

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor

/**
 * 函数族 use-site 签名。
 *
 * 该层对齐 Kotlin `CaFunctionSignature`：
 * 它不新增额外状态，只是把底层 symbol 族约束收窄到 `CaFunctionSymbol`，
 * 以便 `asSignature()` / `substitute()` 在公开 API 上保留精确返回类型。
 */
@SubclassOptInRequired(CaImplementationDetail::class)
public interface CaFunctionSignature<out S : CaFunctionSymbol> : CaCallableSignature<S> {
    /**
     * The use-site-substituted [value parameters][CaFunctionSymbol.valueParameters].
     */
    public val valueParameters: List<CaVariableSignature<CaValueParameterSymbol>>

    @CaExperimentalApi
    abstract override fun substitute(substitutor: CaSubstitutor): CaFunctionSignature<S>
}
