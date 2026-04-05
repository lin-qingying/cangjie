package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.asCaDiagnostic
import org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirScopeSnapshot
import org.cangnova.cangjie.analysis.api.cfir.resolve.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.components.CaCompletionCandidateChecker
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.api.components.CaDiagnosticProvider
import org.cangnova.cangjie.analysis.api.components.CaResolver
import org.cangnova.cangjie.analysis.api.components.CaScopeProvider
import org.cangnova.cangjie.analysis.api.diagnostics.CaDiagnosticWithPsi
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjReferenceExpression

/**
 * CFIR resolver 组件。
 *
 * 该组件只负责把公开 Analysis API 的解析请求映射到 session 内部协议，
 * 不再直接接触 low-level facade。
 */
internal class CaCfirResolver(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaResolver, CaCfirSessionComponent {
    override fun CjReferenceExpression.resolveToSymbols(): Collection<CaSymbol> = withValidityAssertion {
        analysisSession.resolveSymbols(this@resolveToSymbols)
            .map(analysisSession::getPublicSymbol)
    }

    override fun CjElement.resolveToCall() = withValidityAssertion {
        analysisSession.queryCallInfo(this@resolveToCall)?.asCaCallInfo(analysisSession, token)
    }
}

/**
 * 符号关系组件。
 */
internal class CaCfirSymbolRelationProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), org.cangnova.cangjie.analysis.api.components.CaSymbolRelationProvider {
    override fun CaSymbol.isEquivalentTo(other: CaSymbol): Boolean = withValidityAssertion {
        when {
            this@isEquivalentTo === other -> true
            this@isEquivalentTo is CaPackageSymbol && other is CaPackageSymbol ->
                this@isEquivalentTo.fqName == other.fqName

            this@isEquivalentTo is org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol &&
                other is org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol ->
                this@isEquivalentTo.file == other.file

            this@isEquivalentTo is CaClassLikeSymbol && other is CaClassLikeSymbol ->
                this@isEquivalentTo.classId == other.classId

            this@isEquivalentTo is org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol &&
                other is org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol -> {
                val leftCallableId = this@isEquivalentTo.callableId
                val rightCallableId = other.callableId
                when {
                    leftCallableId != null && rightCallableId != null -> leftCallableId == rightCallableId
                    else -> this@isEquivalentTo === other
                }
            }

            else -> false
        }
    }
}

/**
 * 诊断组件。
 */
internal class CaCfirDiagnosticProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaDiagnosticProvider, CaCfirSessionComponent {
    override fun CjElement.diagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>> =
        this@CaCfirDiagnosticProvider.withValidityAssertion {
            analysisSession.queryDiagnostics(this@diagnostics, filter.asLLFilter())
                .map { diagnostic -> diagnostic.asPublicDiagnostic() }
        }

    override fun CjFile.collectDiagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>> =
        this@CaCfirDiagnosticProvider.withValidityAssertion {
            analysisSession.queryFileDiagnostics(this@collectDiagnostics, filter.asLLFilter())
                .map { diagnostic -> diagnostic.asPublicDiagnostic() }
        }

    private fun CaDiagnosticCheckerFilter.asLLFilter(): DiagnosticCheckerFilter = when (this) {
        CaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS -> DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS
        CaDiagnosticCheckerFilter.ONLY_EXTENDED_CHECKERS -> DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS
        CaDiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS -> DiagnosticCheckerFilter.ONLY_EXPERIMENTAL_CHECKERS
        CaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS ->
            DiagnosticCheckerFilter.ONLY_DEFAULT_CHECKERS + DiagnosticCheckerFilter.ONLY_EXTRA_CHECKERS
    }
}

/**
 * 作用域组件。
 *
 * 所有 low-level scope snapshot 都统一通过 session 查询并映射为公开 `CaScope`。
 */
internal class CaCfirScopeProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaScopeProvider, CaCfirSessionComponent {
    override fun CjFile.getFileScope(): CaScope = withValidityAssertion {
        analysisSession.queryFileScope(this@getFileScope).asAnalysisScope(
            extraSymbols = listOf(with(analysisSession) { this@getFileScope.fileSymbol() }),
        )
    }

    override fun getPackageScope(packageFqName: FqName): CaScope? = withValidityAssertion {
        val packageSymbol = with(analysisSession) { getPackageSymbol(packageFqName) } ?: return@withValidityAssertion null
        analysisSession.queryPackageScope(packageFqName)?.asAnalysisScope(
            extraSymbols = listOf(packageSymbol),
        ) ?: error("无法为包 `${packageFqName.asString()}` 构建 low-level 作用域快照。")
    }

    override val CaPackageSymbol.packageScope: CaScope
        get() = withValidityAssertion {
            val packageSymbol = this@packageScope as? CaCfirPackageSymbolImpl
                ?: error("仅 CFIR 包符号支持包级作用域查询：${this@packageScope::class.simpleName}")
            analysisSession.queryPackageScope(packageSymbol.fqName)?.asAnalysisScope(
                extraSymbols = listOf(packageSymbol),
            ) ?: error("无法为包 `${packageSymbol.fqName.asString()}` 构建 low-level 作用域快照。")
        }

    override val CaClassLikeSymbol.declaredMemberScope: CaScope
        get() = withValidityAssertion {
            val classSymbol = requireClassLikeSymbol(this@declaredMemberScope)
            analysisSession.queryDeclaredMemberScope(classSymbol.classId)?.asAnalysisScope()
                ?: error("无法为 `${classSymbol.classId.asString()}` 构建 declared-member 作用域快照。")
        }

    override val CaClassLikeSymbol.memberScope: CaScope
        get() = withValidityAssertion {
            val classSymbol = requireClassLikeSymbol(this@memberScope)
            analysisSession.queryMemberScope(classSymbol.classId)?.asAnalysisScope()
                ?: error("无法为 `${classSymbol.classId.asString()}` 构建 use-site member 作用域快照。")
        }

    override val org.cangnova.cangjie.analysis.api.types.CaType.scope: CaScope?
        get() = withValidityAssertion {
            analysisSession.queryTypeScope(this@scope.requireCfirConeType("成员作用域查询"))?.asAnalysisScope()
        }

    private fun requireClassLikeSymbol(symbol: CaClassLikeSymbol): CaCfirClassLikeSymbolImpl {
        return symbol as? CaCfirClassLikeSymbolImpl
            ?: error("仅 CFIR class-like 符号支持成员作用域查询：${symbol::class.simpleName}")
    }

    /**
     * 把 low-level scope snapshot 适配为公开 Analysis API scope。
     */
    private fun CaCfirScopeSnapshot.asAnalysisScope(
        extraSymbols: List<CaSymbol> = emptyList(),
    ): CaScope {
        return CaCfirScopeImpl(
            indexedNames = availableNames,
            eagerSymbols = extraSymbols,
            token = token,
            symbolLookup = { name ->
                getSymbols(name).map { symbol ->
                    analysisSession.getPublicSymbol(symbol)
                }
            },
            callableLookup = { name ->
                getCallableSymbols(name).map { symbol ->
                    analysisSession.getPublicSymbol(symbol) as CaCallableSymbol
                }
            },
            classifierLookup = { name ->
                getClassifierSymbols(name).map { symbol ->
                    analysisSession.getPublicSymbol(symbol) as CaClassLikeSymbol
                }
            },
        )
    }
}

/**
 * 补全候选判定组件。
 */
internal class CaCfirCompletionCandidateChecker(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaCompletionCandidateChecker {
    override fun CaSymbol.checkCompletionCandidate(position: CjElement): CaCompletionCandidateDecision = withValidityAssertion {
        analysisSession.checkCompletionCandidate(this@checkCompletionCandidate, position)
    }
}
