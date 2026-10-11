package org.cangnova.cangjie.analysis.api.impl.base.resolution

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnostic
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionError
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionSuccess
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol

/**
 * 元素级符号解析成功的基类实现。
 *
 * 对齐 Kotlin `KaBaseSymbolResolutionSuccess`：符号集合在构造时固定，
 * 生命周期 token 默认取自首个符号。
 */
@CaImplementationDetail
class CaBaseSymbolResolutionSuccess(
    private val backingSymbols: List<CaSymbol>,
    override val token: CaLifetimeToken,
) : CaSymbolResolutionSuccess {
    /**
     * 单符号快捷构造，token 取自该符号。
     */
    constructor(backingSymbol: CaSymbol) : this(
        backingSymbols = listOf(backingSymbol),
        token = backingSymbol.token,
    )

    override val symbols: List<CaSymbol>
        get() = withValidityAssertion { backingSymbols }
}

/**
 * 元素级符号解析失败的基类实现。
 *
 * 对齐 Kotlin `KaBaseSymbolResolutionError`：生命周期 token 取自诊断。
 */
@CaImplementationDetail
class CaBaseSymbolResolutionError(
    private val backingDiagnostic: CaDiagnostic,
    private val backingCandidateSymbols: List<CaSymbol>,
) : CaSymbolResolutionError {
    override val token: CaLifetimeToken
        get() = backingDiagnostic.token

    override val diagnostic: CaDiagnostic
        get() = withValidityAssertion { backingDiagnostic }

    override val candidateSymbols: List<CaSymbol>
        get() = withValidityAssertion { backingCandidateSymbols }
}