package org.cangnova.cangjie.analysis.api.cfir.resolve

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithCandidates
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.patterns.CfirBindingPattern
import org.cangnova.cangjie.cfir.patterns.CfirConstPattern
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirExpressionPattern
import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirTuplePattern
import org.cangnova.cangjie.cfir.patterns.CfirTypePattern
import org.cangnova.cangjie.cfir.patterns.CfirVarOrEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirErrorReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjCollectionLiteralExpression
import org.cangnova.cangjie.psi.CjConstructorDelegationReferenceExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjArrayAccessExpression
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjRangeExpression
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.psi.CjSuperTypeCallEntry
import org.cangnova.cangjie.psi.psiUtil.callExpression
import org.cangnova.cangjie.psi.psiUtil.getQualifiedExpressionForSelector
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.source.psi
import java.util.LinkedHashMap
import java.util.LinkedHashSet

private data class CaCfirPsiAnchorKey(
    val fileIdentity: String,
    val elementClassName: String,
    val startOffset: Int,
    val endOffset: Int,
) {
    companion object {
        fun create(psi: PsiElement): CaCfirPsiAnchorKey? {
            val file = psi.containingFile ?: return null
            val range = psi.textRange ?: return null
            val fileIdentity = file.virtualFile?.path ?: "${file.name}#${file.text.hashCode()}"
            return CaCfirPsiAnchorKey(
                fileIdentity = fileIdentity,
                elementClassName = psi::class.java.name,
                startOffset = range.startOffset,
                endOffset = range.endOffset,
            )
        }
    }
}

/**
 * low-level 语义查询提供器。
 *
 * 它把 PSI -> CFIR 的稳定语义映射集中在一处：
 * 1. 表达式类型；
 * 2. 声明返回类型；
 * 3. 参数类型；
 * 4. 类默认类型；
 * 5. 引用目标；
 * 6. 调用结果快照。
 *
 * facade 只组合这些查询能力，不再自己维护大型 visitor 逻辑。
 */
internal class CaCfirSemanticQueryProvider(
    cfirFiles: List<CfirFile>,
) {
    private val semanticIndex: SemanticIndex by lazy(LazyThreadSafetyMode.NONE) {
        SemanticIndex.build(cfirFiles)
    }

    fun getExpressionType(expression: CjExpression): ConeCangJieType? =
        semanticIndex.expressionTypes[expression]

    fun getDeclarationReturnType(declaration: CjCallableDeclaration): ConeCangJieType? =
        semanticIndex.callableReturnTypes[declaration]

    fun getValueParameterType(parameter: CjParameter): ConeCangJieType? =
        semanticIndex.valueParameterTypes[parameter]

    fun getClassDefaultType(declaration: CjClassLikeDeclaration): ConeCangJieType? =
        semanticIndex.classDefaultTypes[declaration]

    fun getDeclarationSymbols(psi: PsiElement): List<CfirSymbol<*>> =
        semanticIndex.lookupDeclarationSymbols(psi)

    fun resolveReference(reference: CjReferenceExpression): Collection<CfirSymbol<*>> =
        semanticIndex.lookupReferenceTargets(reference)

    fun getCallInfo(element: PsiElement): CaCfirCallInfoSnapshot? =
        semanticIndex.lookupCallInfo(element)

    /**
     * 只保留当前公开 Analysis API 需要的稳定语义索引。
     */
    private class SemanticIndex(
        val expressionTypes: Map<CjExpression, ConeCangJieType>,
        val callableReturnTypes: Map<CjCallableDeclaration, ConeCangJieType>,
        val valueParameterTypes: Map<CjParameter, ConeCangJieType>,
        val classDefaultTypes: Map<CjClassLikeDeclaration, ConeCangJieType>,
        val declarationSymbols: Map<PsiElement, List<CfirSymbol<*>>>,
        val declarationSymbolsByAnchor: Map<CaCfirPsiAnchorKey, List<CfirSymbol<*>>>,
        val referenceTargets: Map<CjReferenceExpression, Set<CfirSymbol<*>>>,
        val referenceTargetsByAnchor: Map<CaCfirPsiAnchorKey, Set<CfirSymbol<*>>>,
        val callInfos: Map<PsiElement, CaCfirCallInfoSnapshot>,
        val callInfosByAnchor: Map<CaCfirPsiAnchorKey, CaCfirCallInfoSnapshot>,
    ) {
        /**
         * generated / standalone 测试里经常会出现“语义索引建在一份 PSI 上，断言拿到的是另一份等价 PSI”的情况。
         *
         * 只用 `PsiElement` 对象身份做 key，会让 `resolveToSymbol` / `resolveToCall` / `lookupSymbolsByPsi`
         * 在框架级场景里整体失效；这里补上一层稳定锚点键，让语义查询既支持同实例命中，也支持等价 PSI 命中。
         */
        fun lookupDeclarationSymbols(psi: PsiElement): List<CfirSymbol<*>> {
            return declarationSymbols[psi]
                ?: CaCfirPsiAnchorKey.create(psi)?.let(declarationSymbolsByAnchor::get)
                ?: emptyList()
        }

        fun lookupReferenceTargets(reference: CjReferenceExpression): Set<CfirSymbol<*>> {
            return referenceTargets[reference]
                ?: CaCfirPsiAnchorKey.create(reference)?.let(referenceTargetsByAnchor::get)
                ?: emptySet()
        }

        fun lookupCallInfo(element: PsiElement): CaCfirCallInfoSnapshot? {
            return callInfos[element]
                ?: CaCfirPsiAnchorKey.create(element)?.let(callInfosByAnchor::get)
        }

        companion object {
            fun build(cfirFiles: List<CfirFile>): SemanticIndex {
                val expressionTypes = LinkedHashMap<CjExpression, ConeCangJieType>()
                val callableReturnTypes = LinkedHashMap<CjCallableDeclaration, ConeCangJieType>()
                val valueParameterTypes = LinkedHashMap<CjParameter, ConeCangJieType>()
                val classDefaultTypes = LinkedHashMap<CjClassLikeDeclaration, ConeCangJieType>()
                val declarationSymbols = LinkedHashMap<PsiElement, LinkedHashSet<CfirSymbol<*>>>()
                val declarationSymbolsByAnchor = LinkedHashMap<CaCfirPsiAnchorKey, LinkedHashSet<CfirSymbol<*>>>()
                val referenceTargets = LinkedHashMap<CjReferenceExpression, LinkedHashSet<CfirSymbol<*>>>()
                val referenceTargetsByAnchor = LinkedHashMap<CaCfirPsiAnchorKey, LinkedHashSet<CfirSymbol<*>>>()
                val callInfos = LinkedHashMap<PsiElement, CaCfirCallInfoSnapshot>()
                val callInfosByAnchor = LinkedHashMap<CaCfirPsiAnchorKey, CaCfirCallInfoSnapshot>()

                val visitor = object : CfirVisitorVoid() {
                    override fun visitElement(element: CfirElement) {
                        recordElement(element)
                        element.acceptChildren(this)
                    }

                    private fun recordElement(element: CfirElement) {
                        val psi = element.source?.psi
                        if (psi != null) {
                            element.sourceSymbolOrNull()?.let { symbol ->
                                declarationSymbols.recordPsiIdentity(psi, symbol)
                                declarationSymbolsByAnchor.recordPsiAnchor(psi, symbol)
                                if (element is org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction && psi is CjLambdaExpression) {
                                    declarationSymbols.recordPsiIdentity(psi.functionLiteral, symbol)
                                    declarationSymbolsByAnchor.recordPsiAnchor(psi.functionLiteral, symbol)
                                }
                            }
                        }

                        when (element) {
                            is org.cangnova.cangjie.cfir.declarations.CfirPatternVariable ->
                                collectPatternDeclarationSymbols(element.pattern, declarationSymbols, declarationSymbolsByAnchor)
                            is CfirMatchBranch ->
                                collectPatternDeclarationSymbols(element.pattern, declarationSymbols, declarationSymbolsByAnchor)
                        }

                        if (psi is CjExpression && element is CfirExpression) {
                            element.coneTypeOrNull?.let { expressionTypes[psi] = it }
                        }

                        if (psi is CjCallableDeclaration && element is CfirCallableDeclaration) {
                            element.returnTypeRef.coneTypeOrNull?.let { callableReturnTypes[psi] = it }
                        }

                        if (psi is CjParameter && element is CfirValueParameter) {
                            element.returnTypeRef.coneTypeOrNull?.let { valueParameterTypes[psi] = it }
                        }

                        if (psi is CjClassLikeDeclaration && element is CfirClassLikeDeclaration) {
                            classDefaultTypes[psi] = element.symbol.constructType()
                        }

                        if (psi is CjReferenceExpression && element is CfirResolvable) {
                            collectReferenceTargets(psi, element.calleeReference, referenceTargets, referenceTargetsByAnchor)
                        }

                        if (psi is CjReferenceExpression && element is CfirResolvedNamedReference) {
                            referenceTargets.recordPsiIdentity(psi, element.resolvedSymbol)
                            referenceTargetsByAnchor.recordPsiAnchor(psi, element.resolvedSymbol)
                        }

                        if (element is CfirFunctionCall) {
                            collectCallInfoAnchors(psi).forEach { anchor ->
                                val snapshot = buildCallInfo(element)
                                callInfos[anchor] = snapshot
                                CaCfirPsiAnchorKey.create(anchor)?.let { anchorKey ->
                                    callInfosByAnchor[anchorKey] = snapshot
                                }
                            }
                        }
                    }
                }

                cfirFiles.forEach { cfirFile ->
                    cfirFile.accept(visitor, null)
                }

                return SemanticIndex(
                    expressionTypes = expressionTypes,
                    callableReturnTypes = callableReturnTypes,
                    valueParameterTypes = valueParameterTypes,
                    classDefaultTypes = classDefaultTypes,
                    declarationSymbols = declarationSymbols.mapValues { (_, symbols) -> symbols.toList() },
                    declarationSymbolsByAnchor = declarationSymbolsByAnchor.mapValues { (_, symbols) -> symbols.toList() },
                    referenceTargets = referenceTargets.mapValues { (_, symbols) -> symbols.toSet() },
                    referenceTargetsByAnchor = referenceTargetsByAnchor.mapValues { (_, symbols) -> symbols.toSet() },
                    callInfos = callInfos,
                    callInfosByAnchor = callInfosByAnchor,
                )
            }

            private fun buildCallInfo(functionCall: CfirFunctionCall): CaCfirCallInfoSnapshot {
                val calleeReference = functionCall.calleeReference
                val errorCandidateCalls = buildDiagnosticCandidateSnapshots(functionCall, calleeReference)
                val directCandidateSnapshot = (calleeReference as? CfirNamedReferenceWithCandidate)
                    ?.candidate
                    ?.toCallSnapshot(functionCall, calleeReference)
                val referenceSnapshot = buildResolvedReferenceCallSnapshot(functionCall, calleeReference)
                val calls = when {
                    errorCandidateCalls.isNotEmpty() -> errorCandidateCalls
                    directCandidateSnapshot != null -> listOf(directCandidateSnapshot)
                    referenceSnapshot != null -> listOf(referenceSnapshot)
                    else -> emptyList()
                }

                return CaCfirCallInfoSnapshot(
                    successfulCall = calls.singleSuccessOrNull(),
                    calls = calls,
                )
            }

            private fun buildDiagnosticCandidateSnapshots(
                functionCall: CfirFunctionCall,
                calleeReference: CfirReference,
            ): List<CaCfirCallSnapshot> {
                val errorReference = calleeReference as? CfirErrorReferenceWithCandidate ?: return emptyList()
                val diagnostic = errorReference.diagnostic as? ConeDiagnosticWithCandidates ?: return emptyList()
                return diagnostic.candidates
                    .filterIsInstance<Candidate>()
                    .map { candidate -> candidate.toCallSnapshot(functionCall, calleeReference) }
                    .distinctBy { snapshot ->
                        snapshot.target?.callableId?.toString()
                            ?: "${snapshot.calleeName?.asString().orEmpty()}#${snapshot.applicability.name}"
                    }
            }

            private fun Candidate.toCallSnapshot(
                functionCall: CfirFunctionCall,
                calleeReference: CfirReference,
            ): CaCfirCallSnapshot {
                return CaCfirCallSnapshot(
                    kind = CaCfirCallKind.FUNCTION,
                    origin = functionCall.origin.asAnalysisOrigin(),
                    applicability = applicability.asAnalysisApplicability(),
                    isImplicitInvoke = callInfo.isImplicitInvoke,
                    calleeName = (calleeReference as? CfirNamedReference)?.name ?: callInfo.name,
                    target = symbol as? CfirCallableSymbol<*>,
                    explicitReceiverType = functionCall.explicitReceiver?.coneTypeOrNull,
                    dispatchReceiverType = dispatchReceiverExpression()?.coneTypeOrNull,
                    extensionReceiverType = chosenExtensionReceiverExpression()?.coneTypeOrNull,
                    contextArgumentTypes = contextArguments().map { argument -> argument.coneTypeOrNull },
                    argumentTypes = functionCall.argumentList.arguments.map { argument -> argument.coneTypeOrNull },
                    typeArguments = callInfo.typeArguments.map { typeArgument -> typeArgument.coneTypeOrNull },
                    argumentMapping = buildArgumentMappings(functionCall, this),
                )
            }

            private fun buildResolvedReferenceCallSnapshot(
                functionCall: CfirFunctionCall,
                calleeReference: CfirReference,
            ): CaCfirCallSnapshot? {
                val resolvedTarget = (calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol as? CfirCallableSymbol<*>
                    ?: return null
                return CaCfirCallSnapshot(
                    kind = CaCfirCallKind.FUNCTION,
                    origin = functionCall.origin.asAnalysisOrigin(),
                    applicability = CaCfirCallApplicability.RESOLVED,
                    isImplicitInvoke = false,
                    calleeName = (calleeReference as? CfirNamedReference)?.name,
                    target = resolvedTarget,
                    explicitReceiverType = functionCall.explicitReceiver?.coneTypeOrNull,
                    dispatchReceiverType = null,
                    extensionReceiverType = null,
                    contextArgumentTypes = emptyList(),
                    argumentTypes = functionCall.argumentList.arguments.map { argument -> argument.coneTypeOrNull },
                    typeArguments = functionCall.typeArguments.map { typeArgument -> typeArgument.coneTypeOrNull },
                    argumentMapping = functionCall.argumentList.arguments.mapIndexed { index, _ ->
                        CaCfirCallArgumentMappingSnapshot(
                            argumentIndex = index,
                            parameterName = null,
                            parameterType = null,
                        )
                    },
                )
            }

            private fun buildArgumentMappings(
                functionCall: CfirFunctionCall,
                candidate: Candidate,
            ): List<CaCfirCallArgumentMappingSnapshot> {
                val parameterByArgument = if (candidate.argumentMappingInitialized) {
                    candidate.argumentMapping.entries.associate { (argument, parameter) ->
                        argument.expression to parameter
                    }
                } else {
                    emptyMap()
                }

                return functionCall.argumentList.arguments.mapIndexed { index, argument ->
                    val mappedParameter = parameterByArgument[argument]
                        ?: parameterByArgument.entries.firstOrNull { (mappedArgument, _) ->
                            mappedArgument.source?.psi == argument.source?.psi
                        }?.value
                    CaCfirCallArgumentMappingSnapshot(
                        argumentIndex = index,
                        parameterName = mappedParameter?.name,
                        parameterType = mappedParameter?.returnTypeRef?.coneTypeOrNull,
                    )
                }
            }
        }
    }
}

private fun collectPatternDeclarationSymbols(
    pattern: CfirPattern,
    declarationSymbols: LinkedHashMap<PsiElement, LinkedHashSet<CfirSymbol<*>>>,
    declarationSymbolsByAnchor: LinkedHashMap<CaCfirPsiAnchorKey, LinkedHashSet<CfirSymbol<*>>>,
) {
    when (pattern) {
        is CfirBindingPattern -> {
            pattern.bindingVariable?.source?.psi?.let { psi ->
                declarationSymbols.recordPsiIdentity(psi, pattern.bindingVariable!!.symbol)
                declarationSymbolsByAnchor.recordPsiAnchor(psi, pattern.bindingVariable!!.symbol)
            }
            pattern.nestedPattern?.let { nestedPattern ->
                collectPatternDeclarationSymbols(nestedPattern, declarationSymbols, declarationSymbolsByAnchor)
            }
        }

        is CfirTypePattern -> {
            pattern.bindingVariable?.source?.psi?.let { psi ->
                declarationSymbols.recordPsiIdentity(psi, pattern.bindingVariable!!.symbol)
                declarationSymbolsByAnchor.recordPsiAnchor(psi, pattern.bindingVariable!!.symbol)
            }
        }

        is CfirVarOrEnumPattern -> {
            pattern.bindingVariable?.source?.psi?.let { psi ->
                declarationSymbols.recordPsiIdentity(psi, pattern.bindingVariable!!.symbol)
                declarationSymbolsByAnchor.recordPsiAnchor(psi, pattern.bindingVariable!!.symbol)
            }
        }

        is CfirTuplePattern -> pattern.elements.forEach { element ->
            collectPatternDeclarationSymbols(element, declarationSymbols, declarationSymbolsByAnchor)
        }

        is CfirEnumPattern -> pattern.arguments.forEach { argument ->
            collectPatternDeclarationSymbols(argument, declarationSymbols, declarationSymbolsByAnchor)
        }

        is CfirOrPattern -> pattern.alternatives.forEach { alternative ->
            collectPatternDeclarationSymbols(alternative, declarationSymbols, declarationSymbolsByAnchor)
        }

        is CfirWildcardPattern,
        is CfirConstPattern,
        is CfirExpressionPattern,
        -> Unit
    }
}

/**
 * 统一把一次底层 function call 绑定到公开 Analysis API 可见的调用锚点。
 */
private fun CfirElement.sourceSymbolOrNull(): CfirSymbol<*>? = when (this) {
    is CfirFile -> symbol
    is CfirClassLikeDeclaration -> symbol
    is CfirExtend -> symbol
    is CfirTypeParameter -> symbol
    is CfirCallableDeclaration -> symbol
    else -> null
}

private fun collectCallInfoAnchors(psi: PsiElement?): List<PsiElement> {
    return buildList {
        when (psi) {
            is CjCallExpression -> {
                add(psi)
                psi.getQualifiedExpressionForSelector()?.let(::add)
            }

            is CjArrayAccessExpression -> {
                add(psi)
                psi.getQualifiedExpressionForSelector()?.let(::add)
            }

            is CjCollectionLiteralExpression -> add(psi)

            is CjConstructorDelegationReferenceExpression -> add(psi)

            is CjAnnotation -> add(psi)

            is CjRangeExpression -> add(psi)

            is CjSuperTypeCallEntry -> add(psi)

            is org.cangnova.cangjie.psi.CjQualifiedExpression -> {
                add(psi)
                psi.callExpression?.let(::add)
            }

            else -> Unit
        }

        generateSequence(psi?.parent) { current -> current.parent }
            .forEach { current ->
                when (current) {
                    is CjCallExpression -> {
                        add(current)
                        current.getQualifiedExpressionForSelector()?.let(::add)
                    }

                    is CjArrayAccessExpression -> {
                        add(current)
                        current.getQualifiedExpressionForSelector()?.let(::add)
                    }

                    is CjCollectionLiteralExpression -> add(current)

                    is CjConstructorDelegationReferenceExpression -> add(current)

                    is CjAnnotation -> add(current)

                    is CjRangeExpression -> add(current)

                    is CjSuperTypeCallEntry -> add(current)

                    is org.cangnova.cangjie.psi.CjQualifiedExpression -> {
                        add(current)
                        current.callExpression?.let(::add)
                    }
                }
            }
    }.distinct()
}

private fun collectReferenceTargets(
    referenceExpression: CjReferenceExpression,
    reference: CfirReference,
    referenceTargets: LinkedHashMap<CjReferenceExpression, LinkedHashSet<CfirSymbol<*>>>,
    referenceTargetsByAnchor: LinkedHashMap<CaCfirPsiAnchorKey, LinkedHashSet<CfirSymbol<*>>>,
) {
    when (reference) {
        is CfirResolvedNamedReference -> {
            referenceTargets.recordPsiIdentity(referenceExpression, reference.resolvedSymbol)
            referenceTargetsByAnchor.recordPsiAnchor(referenceExpression, reference.resolvedSymbol)
        }

        is CfirNamedReferenceWithCandidate -> {
            referenceTargets.recordPsiIdentity(referenceExpression, reference.candidate.symbol)
            referenceTargetsByAnchor.recordPsiAnchor(referenceExpression, reference.candidate.symbol)
        }

        else -> Unit
    }
}

private fun <K : PsiElement, V> LinkedHashMap<K, LinkedHashSet<V>>.recordPsiIdentity(
    psi: K,
    value: V,
) {
    getOrPut(psi, ::LinkedHashSet).add(value)
}

private fun <V> LinkedHashMap<CaCfirPsiAnchorKey, LinkedHashSet<V>>.recordPsiAnchor(
    psi: PsiElement,
    value: V,
) {
    val anchor = CaCfirPsiAnchorKey.create(psi) ?: return
    getOrPut(anchor, ::LinkedHashSet).add(value)
}

private fun CfirFunctionCallOrigin.asAnalysisOrigin(): CaCfirCallOrigin = when (this) {
    CfirFunctionCallOrigin.Regular -> CaCfirCallOrigin.REGULAR
    CfirFunctionCallOrigin.Operator -> CaCfirCallOrigin.OPERATOR
    CfirFunctionCallOrigin.ConstructorDelegationThis -> CaCfirCallOrigin.CONSTRUCTOR_DELEGATION_THIS
    CfirFunctionCallOrigin.ConstructorDelegationSuper -> CaCfirCallOrigin.CONSTRUCTOR_DELEGATION_SUPER
}

private fun CandidateApplicability.asAnalysisApplicability(): CaCfirCallApplicability = when (this) {
    CandidateApplicability.HIDDEN -> CaCfirCallApplicability.HIDDEN
    CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER -> CaCfirCallApplicability.INAPPLICABLE_WRONG_RECEIVER
    CandidateApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR -> CaCfirCallApplicability.INAPPLICABLE_ARGUMENTS_MAPPING_ERROR
    CandidateApplicability.INAPPLICABLE -> CaCfirCallApplicability.INAPPLICABLE
    CandidateApplicability.VISIBILITY_ERROR -> CaCfirCallApplicability.VISIBILITY_ERROR
    CandidateApplicability.UNSAFE_CALL -> CaCfirCallApplicability.UNSAFE_CALL
    CandidateApplicability.UNSTABLE_SMARTCAST -> CaCfirCallApplicability.UNSTABLE_SMARTCAST
    CandidateApplicability.CONVENTION_ERROR -> CaCfirCallApplicability.CONVENTION_ERROR
    CandidateApplicability.RESOLVED_LOW_PRIORITY -> CaCfirCallApplicability.RESOLVED_LOW_PRIORITY
    CandidateApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY ->
        CaCfirCallApplicability.RESOLVED_NEED_PRESERVE_COMPATIBILITY
    CandidateApplicability.RESOLVED_WITH_ERROR -> CaCfirCallApplicability.RESOLVED_WITH_ERROR
    CandidateApplicability.RESOLVED -> CaCfirCallApplicability.RESOLVED
}

private fun CaCfirCallApplicability.isSuccess(): Boolean =
    this >= CaCfirCallApplicability.RESOLVED_LOW_PRIORITY &&
        this != CaCfirCallApplicability.RESOLVED_WITH_ERROR

private fun List<CaCfirCallSnapshot>.singleSuccessOrNull(): CaCfirCallSnapshot? {
    var successful: CaCfirCallSnapshot? = null
    for (call in this) {
        if (!call.applicability.isSuccess()) continue
        if (successful != null) return null
        successful = call
    }
    return successful
}
