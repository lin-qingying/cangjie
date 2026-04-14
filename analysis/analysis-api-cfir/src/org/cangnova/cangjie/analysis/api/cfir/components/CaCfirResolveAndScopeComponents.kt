package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.completion.CaCompletionCandidateDecision
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
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
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
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

        val callBackedSymbols = restoreCallBackedSymbols(this@resolveToSymbols).distinctSymbols()
        if (callBackedSymbols.isNotEmpty()) {
            return@withValidityAssertion callBackedSymbols
        }

        analysisSession.resolveSymbols(this@resolveToSymbols)
            .map(analysisSession::getPublicSymbol)
            .distinctSymbols()
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

        val extendDispatchTargets = restoreExtendDispatchTargets(reference, snapshot)
        if (extendDispatchTargets.isNotEmpty()) {
            return extendDispatchTargets
        }

        return lowLevelTargets.map(analysisSession::getPublicSymbol)
    }

    /**
     * extend 成员调用在 low-level `call target` 上可能先落到被实现的接口成员。
     *
     * 为了让引用、导航、查找用法统一指向真正承载实现体的 extend 成员，
     * 这里基于接收者类型把目标回收到对应的 extend declared-member scope。
     */
    private fun restoreExtendDispatchTargets(
        reference: CjReferenceExpression,
        snapshot: org.cangnova.cangjie.analysis.api.cfir.resolve.CaCfirCallInfoSnapshot,
    ): Collection<CaSymbol> {
        val memberName = (reference as? CjSimpleNameExpression)?.referencedNameAsName
            ?: snapshot.successfulCall?.calleeName
            ?: return emptyList()

        val receiverClassId = snapshot.successfulCall?.explicitReceiverType?.classIdOrPrimitiveClassId
            ?: snapshot.calls.asSequence()
                .mapNotNull { call -> call.explicitReceiverType?.classIdOrPrimitiveClassId }
                .firstOrNull()
            ?: return emptyList()

        return analysisSession.getExtendPublicSymbols(receiverClassId)
            .flatMap { extendSymbol ->
                with(analysisSession) {
                    extendSymbol.declaredMemberScope.getCallableSymbols(memberName)
                }
            }
            .onEach { symbol ->
                (symbol as? CaCfirBackedSymbol<*>)?.backingSymbol?.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
            }
            .distinctSymbols()
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

    override val CaCallableSymbol.directlyOverriddenSymbols: Sequence<CaCallableSymbol>
        get() = withValidityAssertion {
            if (!mayHaveOverriddenSymbols()) {
                return@withValidityAssertion emptySequence()
            }

            val backingSymbol = (this@directlyOverriddenSymbols as? CaCfirBackedSymbol<*>)
                ?.backingSymbol as? org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol<*>
                ?: return@withValidityAssertion emptySequence()

            analysisSession.resolutionFacade.getDirectlyOverriddenCallableSymbols(backingSymbol)
                .map(analysisSession::getPublicSymbol)
                .filterIsInstance<CaCallableSymbol>()
                .distinctStableCallables()
                .asSequence()
        }

    override val CaCallableSymbol.allOverriddenSymbols: Sequence<CaCallableSymbol>
        get() = withValidityAssertion {
            if (!mayHaveOverriddenSymbols()) {
                return@withValidityAssertion emptySequence()
            }

            val visited = linkedSetOf<String>()
            val result = mutableListOf<CaCallableSymbol>()
            val queue = ArrayDeque(this@allOverriddenSymbols.directlyOverriddenSymbols.toList())

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val key = current.stableCallableIdentity() ?: continue
                if (!visited.add(key)) continue
                result += current
                queue.addAll(current.directlyOverriddenSymbols)
            }

            result.asSequence()
        }

    override fun CaClassSymbol.isSubClassOf(superClass: CaClassSymbol): Boolean = withValidityAssertion {
        isSubclassOf(superClass, allowIndirect = true)
    }

    override fun CaClassSymbol.isDirectSubClassOf(superClass: CaClassSymbol): Boolean = withValidityAssertion {
        isSubclassOf(superClass, allowIndirect = false)
    }

    private fun CaCallableSymbol.mayHaveOverriddenSymbols(): Boolean {
        return this is org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol ||
            this is org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
    }

    private fun List<CaCallableSymbol>.distinctStableCallables(): List<CaCallableSymbol> {
        return distinctBy { callable ->
            callable.stableCallableIdentity() ?: "${callable::class.qualifiedName}@${System.identityHashCode(callable)}"
        }
    }

    private fun CaCallableSymbol.stableCallableIdentity(): String? {
        return publicSymbolCacheKeyOrNull()?.toString() ?: callableId?.toString()
    }

    private fun CaClassSymbol.isSubclassOf(
        superClass: CaClassSymbol,
        allowIndirect: Boolean,
    ): Boolean {
        if (isSameClassAs(superClass)) {
            return false
        }

        val directSuperSymbols = superTypes.mapNotNull { type ->
            with(analysisSession) { type.classLikeSymbol as? CaClassSymbol }
        }
        if (directSuperSymbols.any { symbol -> symbol.isSameClassAs(superClass) }) {
            return true
        }
        if (!allowIndirect) {
            return false
        }

        val visited = linkedSetOf<String>()
        val queue = ArrayDeque(directSuperSymbols)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current.classRelationIdentity())) continue
            if (current.isSameClassAs(superClass)) {
                return true
            }
            queue.addAll(current.superTypes.mapNotNull { type ->
                with(analysisSession) { type.classLikeSymbol as? CaClassSymbol }
            })
        }
        return false
    }

    /**
     * subclass relation 既要覆盖带 `ClassId` 的稳定声明，也要覆盖当前 session 中仅由源码承载的局部类。
     *
     * 因此这里统一按“ClassId 优先，其次源码 PSI 身份”来比较两个 class symbol，
     * 避免把 local class 关系硬退化成一律 `false`。
     */
    private fun CaClassSymbol.isSameClassAs(other: CaClassSymbol): Boolean {
        val thisClassId = classId
        val otherClassId = other.classId
        if (thisClassId != null && otherClassId != null) {
            return thisClassId == otherClassId
        }

        val thisPsi = psi
        val otherPsi = other.psi
        return thisPsi != null && otherPsi != null && thisPsi == otherPsi
    }

    private fun CaClassSymbol.classRelationIdentity(): String {
        classId?.let { return "classId:${it.asString()}" }

        val declarationPsi = psi
        if (declarationPsi != null) {
            val filePath = declarationPsi.containingFile?.virtualFile?.path ?: declarationPsi.containingFile?.name.orEmpty()
            return "psi:$filePath:${declarationPsi.textOffset}"
        }

        return "${this::class.qualifiedName}@${System.identityHashCode(this)}"
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
            analysisSession.queryDiagnostics(this@diagnostics, filter.asCfilter())
                .map { diagnostic -> diagnostic.asPublicDiagnostic() }
        }

    override fun CjFile.collectDiagnostics(filter: CaDiagnosticCheckerFilter): Collection<CaDiagnosticWithPsi<*>> =
        this@CaCfirDiagnosticProvider.withValidityAssertion {
            analysisSession.queryFileDiagnostics(this@collectDiagnostics, filter.asCfilter())
                .map { diagnostic -> diagnostic.asPublicDiagnostic() }
        }

    private fun CaDiagnosticCheckerFilter.asCfilter(): DiagnosticCheckerFilter = when (this) {
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
