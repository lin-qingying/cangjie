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
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMatchEntry
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.CjVarOrEnumPattern
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
import org.cangnova.cangjie.psi.stubs.elements.getAllBindings

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
        val matchBranchBindings = restoreMatchBranchPatternBindings(this@resolveToSymbols).distinctSymbols()
        if (matchBranchBindings.isNotEmpty()) {
            return@withValidityAssertion matchBranchBindings
        }

        buildList {
            addAll(
                analysisSession.resolveSymbols(this@resolveToSymbols)
                    .map(analysisSession::getPublicSymbol),
            )
            addAll(restoreCallBackedSymbols(this@resolveToSymbols))
        }.distinctSymbols()
    }

    override fun CjElement.resolveToCall() = withValidityAssertion {
        analysisSession.queryCallInfo(this@resolveToCall)?.asCaCallInfo(analysisSession, token)
    }

    /**
     * `match` 分支中的模式绑定属于源码局部声明。
     *
     * 它们在当前仓库里还没有完全通过 low-level reference 索引稳定暴露，
     * 但其语义边界在 PSI 上是明确的：只能解析到当前分支条件侧声明的具名绑定。
     * 因此这里直接基于 `CjMatchEntry.conditions` 恢复同分支 binding symbol，
     * 保证不同分支的同名绑定不会混淆。
     */
    private fun restoreMatchBranchPatternBindings(reference: CjReferenceExpression): Collection<CaSymbol> {
        val simpleName = reference as? CjSimpleNameExpression ?: return emptyList()
        val matchEntry = simpleName.getStrictParentOfType<CjMatchEntry>() ?: return emptyList()
        val arrow = matchEntry.arrow ?: return emptyList()
        if (simpleName.textOffset <= arrow.textOffset) {
            return emptyList()
        }

        val resolved = matchEntry.conditions.asSequence()
            .flatMap { condition ->
                sequence {
                    yieldAll(condition.getAllBindings().asSequence())
                    yieldAll(com.intellij.psi.util.PsiTreeUtil.findChildrenOfType(condition, CjVarOrEnumPattern::class.java).asSequence())
                }
            }
            .filter { declaration -> declaration.name == simpleName.referencedName }
            .mapNotNull { declaration ->
                resolvePatternBindingSymbolByPsi(declaration)
                    ?: (declaration as? CjVarOrEnumPattern)?.reference?.let(::resolvePatternBindingSymbolByPsi)
            }
            .toList()
        return resolved
    }

    private fun Collection<CaSymbol>.distinctSymbols(): List<CaSymbol> {
        return distinctBy { symbol ->
            symbol.publicSymbolCacheKeyOrNull() ?: "${symbol::class.qualifiedName}@${System.identityHashCode(symbol)}"
        }
    }

    /**
     * `resolveToSymbol()` 不能只盯住“当前 PSI 节点恰好被 low-level 语义索引命中”这一种形态。
     *
     * Kotlin Analysis 在调用入口上会把 call-shaped PSI 也稳定映射回目标 callable；
     * 仓颉这里同样需要把 `call info` 作为正式语义来源之一，而不是让 `CjCallExpression`
     * 因为索引锚点落在父节点/子节点就直接解析失败。
     */
    private fun restoreCallBackedSymbols(reference: CjReferenceExpression): Collection<CaSymbol> {
        val snapshot = generateSequence(reference as com.intellij.psi.PsiElement?) { current -> current.parent }
            .mapNotNull(analysisSession::queryCallInfo)
            .firstOrNull { callInfo ->
                callInfo.successfulCall?.target != null || callInfo.calls.any { call -> call.target != null }
            }
            ?: return emptyList()

        val lowLevelTargets = buildList {
            snapshot.successfulCall?.target?.let(::add)
            snapshot.calls.mapNotNullTo(this) { call -> call.target }
        }

        return lowLevelTargets.map(analysisSession::getPublicSymbol)
    }

    private fun resolvePatternBindingSymbolByPsi(psi: com.intellij.psi.PsiElement): CaPatternBindingSymbol? {
        return analysisSession.lookupSymbolsByPsi(psi)
            .map(analysisSession::getPublicSymbol)
            .filterIsInstance<CaPatternBindingSymbol>()
            .firstOrNull()
    }
}

/**
 * 符号关系组件。
 */
internal class CaCfirSymbolRelationProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), org.cangnova.cangjie.analysis.api.components.CaSymbolRelationProvider {
    override fun CaSymbol.isEquivalentTo(other: CaSymbol): Boolean = withValidityAssertion {
        this@isEquivalentTo === other ||
            (this@isEquivalentTo.publicSymbolCacheKeyOrNull() != null &&
                this@isEquivalentTo.publicSymbolCacheKeyOrNull() == other.publicSymbolCacheKeyOrNull())
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
            extraSymbols = listOf(with(analysisSession) { this@getFileScope.symbol }),
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
            val classId = classSymbol.classId ?: error("Local/anonymous class-like symbols do not expose declared-member scope.")
            analysisSession.queryDeclaredMemberScope(classId)?.asAnalysisScope()
                ?: error("无法为 `${classId.asString()}` 构建 declared-member 作用域快照。")
        }

    override val org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol.declaredMemberScope: CaScope
        get() = withValidityAssertion {
            val extendSymbol = this@declaredMemberScope as? CaCfirExtendSymbolImpl
                ?: error("Only CFIR extend symbols can expose declared-member scope: ${this@declaredMemberScope::class.simpleName}")
            extendSymbol.backingSymbol.cfir.declarations.asDeclarationListScope()
        }

    override val CaClassLikeSymbol.memberScope: CaScope
        get() = withValidityAssertion {
            val classSymbol = requireClassLikeSymbol(this@memberScope)
            val classId = classSymbol.classId ?: error("Local/anonymous class-like symbols do not expose use-site member scope.")
            analysisSession.queryMemberScope(classId)?.asAnalysisScope()
                ?: error("无法为 `${classId.asString()}` 构建 use-site member 作用域快照。")
        }

    override val org.cangnova.cangjie.analysis.api.types.CaType.scope: CaScope?
        get() = withValidityAssertion {
            analysisSession.queryTypeScope(this@scope.requireCfirConeType("成员作用域查询"))?.asAnalysisScope()
        }

    private fun requireClassLikeSymbol(symbol: CaClassLikeSymbol): CaCfirClassLikeSymbolBase<*> {
        return symbol as? CaCfirClassLikeSymbolBase<*>
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
                    analysisSession.getPublicSymbol(symbol) as CaClassifierSymbol
                }
            },
        )
    }

    private fun List<org.cangnova.cangjie.cfir.declarations.CfirDeclaration>.asDeclarationListScope(): CaScope {
        val publicSymbols = mapNotNull { declaration ->
            when (declaration) {
                is org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration ->
                    analysisSession.getPublicSymbol(declaration.symbol) as? CaDeclarationSymbol
                is org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration ->
                    analysisSession.getPublicSymbol(declaration.symbol) as? CaDeclarationSymbol
                is org.cangnova.cangjie.cfir.declarations.CfirTypeParameter ->
                    analysisSession.getPublicSymbol(declaration.symbol) as? CaDeclarationSymbol
                is org.cangnova.cangjie.cfir.declarations.CfirExtend ->
                    analysisSession.getPublicSymbol(declaration.symbol) as? CaDeclarationSymbol
                else -> null
            }
        }
        val symbolsByName = publicSymbols.groupBy { symbol -> symbol.name ?: Name.special("<anonymous>") }
        return CaCfirScopeImpl(
            indexedNames = symbolsByName.keys,
            eagerSymbols = publicSymbols,
            token = token,
            symbolLookup = { name -> symbolsByName[name].orEmpty() },
            callableLookup = { name -> symbolsByName[name].orEmpty().filterIsInstance<CaCallableSymbol>() },
            classifierLookup = { name -> symbolsByName[name].orEmpty().filterIsInstance<CaClassifierSymbol>() },
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
