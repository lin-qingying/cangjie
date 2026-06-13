package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirControlFlowGraphOwner
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolveState
import org.cangnova.cangjie.cfir.declarations.ResolveStateAccess
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirPatternVariableImpl
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirMatchBranch
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.impl.CfirMatchBranchImpl
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.CfirPatternMutableState
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirImplicitBodyResolveComputationSessionSnapshot
import org.cangnova.cangjie.cfir.resolve.body.ReturnTypeCalculatorWithJump
import org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ConeContextSensitiveAlternativeForQualifierAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeLambdaWithTypeVariableAsExpectedTypeAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConePostponedResolvedAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtomWithPostponedChild
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtomWithSingleChild
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedCallableReferenceAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedLambdaAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeSimpleLeafResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeSimpleNameForContextSensitiveResolution
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.inference.PostponedArgumentsAnalyzer
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.resolve.initialTypeOfCandidate
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import java.util.IdentityHashMap

/**
 * 按候选目标函数类型重检 lambda body，并用真实 body 约束收敛重载候选。
 *
 * Kotlin FIR 的同层职责是 `FirOverloadByLambdaReturnTypeResolver`。仓颉官方
 * `CheckFunctionMatch` 会在候选函数类型下 `ReInferCallArgs`，lambda 检查路径也会
 * `ClearLambdaBodyForReCheck` 后按目标函数类型重检参数和返回体，所以这里保留 overload
 * resolver 的框架位置，但按仓颉语义检查完整 lambda body，而不只检查返回类型。
 */
class CfirOverloadByLambdaBodyResolver(
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val conflictResolver: ConeCallConflictResolver,
) {
    fun <T> reduceCandidates(
        call: T,
        candidates: Set<Candidate>,
    ): Set<Candidate> where T : CfirExpression, T : CfirResolvable {
        if (candidates.size <= 1) return candidates

        val lambdas = collectSingleLambdaByCandidate(candidates) ?: return candidates
        val snapshot = CfirResolutionSnapshot.capture(call)
        val atomStates = candidates.capturePostponedAtomStates()
        val analyzer = components.callCompleter.createPostponedArgumentsAnalyzer(
            components.transformer.resolutionContext,
        )

        val originalCalleeReference = call.calleeReference
        val successfulCandidates = linkedSetOf<Candidate>()
        val successfulCandidateStates = IdentityHashMap<Candidate, SuccessfulCandidateAnalysis>()
        try {
            for ((candidate, lambda) in lambdas) {
                snapshot.restore()
                atomStates.restore()
                val analysis = analyzeCandidateLambda(call, candidate, lambda, analyzer, rollback = true)
                if (analysis.successful) {
                    successfulCandidates += candidate
                    analysis.successfulState?.let { state ->
                        successfulCandidateStates[candidate] = state
                    }
                }
            }

            val selected = when {
                successfulCandidates.isEmpty() -> return candidates.also {
                    snapshot.restore()
                    atomStates.restore()
                }

                successfulCandidates.size == candidates.size -> return candidates.also {
                    snapshot.restore()
                    atomStates.restore()
                }

                else -> conflictResolver.chooseMaximallySpecificCandidates(successfulCandidates)
            }

            snapshot.restore()
            atomStates.restore()
            if (selected.size == 1) {
                val selectedCandidate = selected.single()
                val selectedLambda = lambdas[selectedCandidate] ?: return selected
                val selectedState = successfulCandidateStates[selectedCandidate]
                if (selectedState != null) {
                    val implicitBodyResolveComputationSession =
                        (components.returnTypeCalculator as? ReturnTypeCalculatorWithJump)
                            ?.implicitBodyResolveComputationSession
                    selectedState.restore(selectedCandidate, implicitBodyResolveComputationSession)
                } else {
                    analyzeCandidateLambda(call, selectedCandidate, selectedLambda, analyzer, rollback = false)
                }
            }

            return selected
        } finally {
            call.replaceCalleeReference(originalCalleeReference)
        }
    }

    private fun collectSingleLambdaByCandidate(
        candidates: Set<Candidate>,
    ): Map<Candidate, ConeResolvedLambdaAtom>? {
        if (candidates.any { candidate -> !candidate.isSuccessful }) return null

        val lambdaPairs = candidates.flatMap { candidate ->
            candidate.postponedAtoms
                .filterIsInstance<ConeResolvedLambdaAtom>()
                .filter { atom -> !atom.analyzed }
                .map { atom -> candidate to atom }
        }
        if (lambdaPairs.isEmpty()) return null

        val groupedByLambda = lambdaPairs.groupBy { (_, atom) -> atom.anonymousFunction }
        val singleLambdaGroup = groupedByLambda.values.singleOrNull() ?: return null
        if (singleLambdaGroup.size != candidates.size) return null

        val result = linkedMapOf<Candidate, ConeResolvedLambdaAtom>()
        for ((candidate, atom) in singleLambdaGroup) {
            if (result.put(candidate, atom) != null) return null
            if (atom.expectedType?.fullyExpandedType(components.session) !is ConeFunctionType) return null
        }

        return result.takeIf { it.size == candidates.size }
    }

    @OptIn(ConstraintSystemCompletionMode.ExclusiveForOverloadResolutionByLambdaReturnType::class)
    private fun <T> analyzeCandidateLambda(
        call: T,
        candidate: Candidate,
        lambda: ConeResolvedLambdaAtom,
        analyzer: PostponedArgumentsAnalyzer,
        rollback: Boolean,
    ): CandidateLambdaAnalysis where T : CfirExpression, T : CfirResolvable {
        val mutableState = CandidateMutableState.capture(candidate)
        val postponedContextSnapshot = if (rollback) {
            components.context.capturePostponedAtomsResolutionContexts()
        } else {
            null
        }
        val implicitBodyResolveComputationSession =
            (components.returnTypeCalculator as? ReturnTypeCalculatorWithJump)?.implicitBodyResolveComputationSession
        val implicitBodyResolveSnapshot = if (rollback) {
            implicitBodyResolveComputationSession?.capture()
        } else {
            null
        }
        val transaction = if (rollback) candidate.system.prepareTransaction() else null
        try {
            val analyze = {
                call.replaceCalleeReference(CfirNamedReferenceWithCandidate(null, candidate.callInfo.name, candidate))
                components.callCompleter.runCompletionForCall(
                    candidate = candidate,
                    completionMode = ConstraintSystemCompletionMode.UNTIL_FIRST_LAMBDA,
                    call = call,
                    initialType = components.initialTypeOfCandidate(candidate),
                )
                components.context.withOverloadByLambdaCandidate(candidate) {
                    analyzer.analyzeLambda(
                        csImpl = candidate.system,
                        atom = lambda,
                        candidate = candidate,
                        withPCLASession = false,
                        forOverloadByLambdaReturnType = true,
                    )
                }
            }
            if (rollback) {
                components.context.dataFlowAnalyzerContext.withIsolatedContext {
                    analyze()
                }
            } else {
                analyze()
            }
            val successful = candidate.isSuccessful
            val successfulState = if (rollback && successful) {
                SuccessfulCandidateAnalysis(
                    resolutionSnapshot = CfirResolutionSnapshot.capture(call),
                    postponedAtomStates = setOf(candidate).capturePostponedAtomStates(),
                    candidateState = CandidatePostState.capture(candidate, components),
                    implicitBodyResolveSnapshot = implicitBodyResolveComputationSession?.capture(),
                )
            } else {
                null
            }
            return CandidateLambdaAnalysis(successful, successfulState)
        } finally {
            if (transaction != null) {
                transaction.rollbackTransaction()
                mutableState.restore(candidate)
                components.context.restorePostponedAtomsResolutionContexts(postponedContextSnapshot!!)
                if (implicitBodyResolveSnapshot != null) {
                    implicitBodyResolveComputationSession!!.restore(implicitBodyResolveSnapshot)
                }
            }
        }
    }

    private fun Set<Candidate>.capturePostponedAtomStates(): PostponedAtomStates {
        val states = IdentityHashMap<ConePostponedResolvedAtom, PostponedAtomState>()
        val postponedChildStates = IdentityHashMap<ConeResolutionAtomWithPostponedChild, ConeResolutionAtom?>()
        val visited = IdentityHashMap<ConeResolutionAtom, Unit>()

        fun visit(atom: ConeResolutionAtom?) {
            if (atom == null || visited.put(atom, Unit) != null) return

            when (atom) {
                is ConeAtomWithCandidate -> {
                    for (argument in atom.candidate.arguments) {
                        visit(argument)
                    }
                    for (pclaCall in atom.candidate.postponedPCLACalls) {
                        visit(pclaCall)
                    }
                }

                is ConeResolvedLambdaAtom -> {
                    states[atom] = PostponedAtomState(
                        analyzed = atom.analyzed,
                        returnStatements = atom.returnStatements,
                    )
                    if (atom.analyzed) {
                        for (statement in atom.returnStatements) {
                            visit(statement)
                        }
                    }
                }

                is ConeLambdaWithTypeVariableAsExpectedTypeAtom -> {
                    states[atom] = PostponedAtomState(
                        analyzed = atom.analyzed,
                        returnStatements = null,
                    )
                    visit(atom.subAtom)
                }

                is ConeResolvedCallableReferenceAtom,
                is ConeSimpleNameForContextSensitiveResolution,
                is ConeContextSensitiveAlternativeForQualifierAtom,
                -> {
                    states[atom] = PostponedAtomState(
                        analyzed = atom.analyzed,
                        returnStatements = null,
                    )
                }

                is ConeResolutionAtomWithPostponedChild -> {
                    postponedChildStates[atom] = atom.subAtom
                    visit(atom.subAtom)
                }

                is ConeResolutionAtomWithSingleChild -> visit(atom.subAtom)
                is ConeSimpleLeafResolutionAtom -> Unit
            }
        }

        for (candidate in this) {
            for (argument in candidate.arguments) {
                visit(argument)
            }
            for (pclaCall in candidate.postponedPCLACalls) {
                visit(pclaCall)
            }
            for (atom in candidate.postponedAtoms) {
                visit(atom)
            }
        }
        return PostponedAtomStates(states, postponedChildStates)
    }

    private class CandidateLambdaAnalysis(
        val successful: Boolean,
        val successfulState: SuccessfulCandidateAnalysis?,
    )

    private class SuccessfulCandidateAnalysis(
        private val resolutionSnapshot: CfirResolutionSnapshot,
        private val postponedAtomStates: PostponedAtomStates,
        private val candidateState: CandidatePostState,
        private val implicitBodyResolveSnapshot: CfirImplicitBodyResolveComputationSessionSnapshot?,
    ) {
        fun restore(
            candidate: Candidate,
            implicitBodyResolveComputationSession: org.cangnova.cangjie.cfir.resolve.body.CfirImplicitBodyResolveComputationSession?,
        ) {
            resolutionSnapshot.restore()
            postponedAtomStates.restore()
            candidateState.restore(candidate)
            if (implicitBodyResolveSnapshot != null) {
                implicitBodyResolveComputationSession?.restore(implicitBodyResolveSnapshot)
            }
        }
    }

    private class CandidatePostState private constructor(
        private val systemStorage: ConstraintStorage,
        private val diagnostics: List<ResolutionDiagnostic>,
        private val lowestApplicability: CandidateApplicability,
        private val postponedAtoms: List<ConePostponedResolvedAtom>,
        private val postponedPCLACalls: List<ConeResolutionAtom>,
        private val lambdasAnalyzedWithPCLA: List<CfirDeclaration>,
        private val completionCallbacks: List<(ConeSubstitutor) -> Unit>,
    ) {
        fun restore(candidate: Candidate) {
            candidate.system.replaceContentWith(systemStorage)
            candidate.diagnostics.clear()
            candidate.diagnostics.addAll(diagnostics)
            candidate.restoreLowestApplicability(lowestApplicability)
            candidate.postponedAtoms.clear()
            candidate.postponedAtoms.addAll(postponedAtoms)
            candidate.postponedPCLACalls.clear()
            candidate.postponedPCLACalls.addAll(postponedPCLACalls)
            candidate.lambdasAnalyzedWithPCLA.clear()
            candidate.lambdasAnalyzedWithPCLA.addAll(lambdasAnalyzedWithPCLA)
            candidate.onPCLACompletionResultsWritingCallbacks.clear()
            candidate.onPCLACompletionResultsWritingCallbacks.addAll(completionCallbacks)
        }

        companion object {
            fun capture(
                candidate: Candidate,
                components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
            ): CandidatePostState {
                val systemCopy = components.session.inferenceComponents.createConstraintSystem().apply {
                    setBaseSystem(candidate.system.currentStorage())
                }.currentStorage()
                return CandidatePostState(
                    systemStorage = systemCopy,
                    diagnostics = candidate.diagnostics.toList(),
                    lowestApplicability = candidate.lowestApplicability,
                    postponedAtoms = candidate.postponedAtoms.toList(),
                    postponedPCLACalls = candidate.postponedPCLACalls.toList(),
                    lambdasAnalyzedWithPCLA = candidate.lambdasAnalyzedWithPCLA.toList(),
                    completionCallbacks = candidate.onPCLACompletionResultsWritingCallbacks.toList(),
                )
            }
        }
    }

    private data class PostponedAtomState(
        val analyzed: Boolean,
        val returnStatements: Collection<ConeResolutionAtom>?,
    )

    private class PostponedAtomStates(
        private val states: IdentityHashMap<ConePostponedResolvedAtom, PostponedAtomState>,
        private val postponedChildStates: IdentityHashMap<ConeResolutionAtomWithPostponedChild, ConeResolutionAtom?>,
    ) {
        fun restore() {
            for ((atom, subAtom) in postponedChildStates) {
                atom.subAtom = subAtom
            }
            for ((atom, state) in states) {
                atom.analyzed = state.analyzed
                if (atom is ConeResolvedLambdaAtom && state.returnStatements != null) {
                    atom.returnStatements = state.returnStatements
                }
            }
        }
    }

    private class CandidateMutableState private constructor(
        private val diagnosticsSize: Int,
        private val lowestApplicability: CandidateApplicability,
        private val postponedAtomsSize: Int,
        private val postponedPCLACallsSize: Int,
        private val lambdasAnalyzedWithPCLASize: Int,
        private val completionCallbacksSize: Int,
    ) {
        fun restore(candidate: Candidate) {
            candidate.diagnostics.removeTail(diagnosticsSize)
            candidate.restoreLowestApplicability(lowestApplicability)
            candidate.postponedAtoms.removeTail(postponedAtomsSize)
            candidate.postponedPCLACalls.removeTail(postponedPCLACallsSize)
            candidate.lambdasAnalyzedWithPCLA.removeTail(lambdasAnalyzedWithPCLASize)
            candidate.onPCLACompletionResultsWritingCallbacks.removeTail(completionCallbacksSize)
        }

        companion object {
            fun capture(candidate: Candidate): CandidateMutableState = CandidateMutableState(
                diagnosticsSize = candidate.diagnostics.size,
                lowestApplicability = candidate.lowestApplicability,
                postponedAtomsSize = candidate.postponedAtoms.size,
                postponedPCLACallsSize = candidate.postponedPCLACalls.size,
                lambdasAnalyzedWithPCLASize = candidate.lambdasAnalyzedWithPCLA.size,
                completionCallbacksSize = candidate.onPCLACompletionResultsWritingCallbacks.size,
            )
        }
    }

    private class CfirResolutionSnapshot private constructor(
        private val resolveStates: IdentityHashMap<CfirElementWithResolveState, CfirResolveState>,
        private val expressionTypes: IdentityHashMap<CfirExpression, ConeCangJieType?>,
        private val calleeReferences: IdentityHashMap<CfirResolvable, CfirReference>,
        private val anonymousFunctionReturnTypes: IdentityHashMap<CfirAnonymousFunction, CfirTypeRef>,
        private val anonymousFunctionTypes: IdentityHashMap<CfirAnonymousFunction, CfirTypeRef>,
        private val anonymousFunctionMatchingTypes: IdentityHashMap<CfirAnonymousFunction, ConeCangJieType?>,
        private val anonymousFunctionBodies: IdentityHashMap<CfirAnonymousFunction, CfirBlock?>,
        private val variableTypes: IdentityHashMap<CfirVariable, CfirTypeRef>,
        private val blockStatements: IdentityHashMap<CfirBlock, List<CfirStatement>>,
        private val patternVariablePatterns: IdentityHashMap<CfirPatternVariable, CfirPattern>,
        private val matchBranchPatterns: IdentityHashMap<CfirMatchBranch, CfirPattern>,
        private val patternStates: IdentityHashMap<CfirPattern, CfirPatternMutableState>,
        private val qualifiedAccessStates: IdentityHashMap<CfirQualifiedAccessExpression, QualifiedAccessState>,
        private val functionCallArgumentLists: IdentityHashMap<CfirFunctionCall, CfirArgumentList>,
        private val controlFlowGraphReferences: IdentityHashMap<CfirControlFlowGraphOwner, CfirControlFlowGraphReference?>,
    ) {
        @OptIn(ResolveStateAccess::class)
        fun restore() {
            for ((element, state) in resolveStates) {
                element.resolveState = state
            }
            for ((owner, reference) in controlFlowGraphReferences) {
                owner.replaceControlFlowGraphReference(reference)
            }
            for ((variable, typeRef) in variableTypes) {
                variable.replaceReturnTypeRef(typeRef)
            }
            for ((function, typeRef) in anonymousFunctionReturnTypes) {
                function.replaceReturnTypeRef(typeRef)
            }
            for ((function, typeRef) in anonymousFunctionTypes) {
                function.replaceTypeRef(typeRef)
            }
            for ((function, matchingType) in anonymousFunctionMatchingTypes) {
                function.replaceMatchingParameterFunctionType(matchingType)
            }
            for ((function, body) in anonymousFunctionBodies) {
                function.replaceBody(body)
            }
            for ((patternVariable, pattern) in patternVariablePatterns) {
                val impl = patternVariable as? CfirPatternVariableImpl
                    ?: error("CfirPatternVariable must be backed by generated implementation")
                impl.pattern = pattern
            }
            for ((branch, pattern) in matchBranchPatterns) {
                val impl = branch as? CfirMatchBranchImpl
                    ?: error("CfirMatchBranch must be backed by generated implementation")
                impl.pattern = pattern
            }
            for ((_, state) in patternStates) {
                state.restore()
            }
            for ((block, statements) in blockStatements) {
                val mutableStatements = block.statements as? MutableList<CfirStatement>
                    ?: error("CfirBlock statements must be mutable during overload-by-lambda rollback")
                mutableStatements.clear()
                mutableStatements.addAll(statements)
            }
            for ((call, argumentList) in functionCallArgumentLists) {
                call.replaceArgumentList(argumentList)
            }
            for ((access, state) in qualifiedAccessStates) {
                access.replaceDispatchReceiver(state.dispatchReceiver)
                access.replaceTypeArguments(state.typeArguments)
            }
            for ((resolvable, reference) in calleeReferences) {
                resolvable.replaceCalleeReference(reference)
            }
            for ((expression, type) in expressionTypes) {
                expression.replaceConeTypeOrNull(type)
            }
        }

        companion object {
            fun capture(root: CfirElement): CfirResolutionSnapshot {
                val resolveStates = IdentityHashMap<CfirElementWithResolveState, CfirResolveState>()
                val expressionTypes = IdentityHashMap<CfirExpression, ConeCangJieType?>()
                val calleeReferences = IdentityHashMap<CfirResolvable, CfirReference>()
                val anonymousFunctionReturnTypes = IdentityHashMap<CfirAnonymousFunction, CfirTypeRef>()
                val anonymousFunctionTypes = IdentityHashMap<CfirAnonymousFunction, CfirTypeRef>()
                val anonymousFunctionMatchingTypes = IdentityHashMap<CfirAnonymousFunction, ConeCangJieType?>()
                val anonymousFunctionBodies = IdentityHashMap<CfirAnonymousFunction, CfirBlock?>()
                val variableTypes = IdentityHashMap<CfirVariable, CfirTypeRef>()
                val blockStatements = IdentityHashMap<CfirBlock, List<CfirStatement>>()
                val patternVariablePatterns = IdentityHashMap<CfirPatternVariable, CfirPattern>()
                val matchBranchPatterns = IdentityHashMap<CfirMatchBranch, CfirPattern>()
                val patternStates = IdentityHashMap<CfirPattern, CfirPatternMutableState>()
                val qualifiedAccessStates = IdentityHashMap<CfirQualifiedAccessExpression, QualifiedAccessState>()
                val functionCallArgumentLists = IdentityHashMap<CfirFunctionCall, CfirArgumentList>()
                val controlFlowGraphReferences =
                    IdentityHashMap<CfirControlFlowGraphOwner, CfirControlFlowGraphReference?>()

                root.accept(
                    object : CfirVisitorVoid() {
                        @OptIn(ResolveStateAccess::class)
                        override fun visitElement(element: CfirElement) {
                            if (element is CfirElementWithResolveState) {
                                resolveStates[element] = element.resolveState
                            }
                            if (element is CfirExpression && element !is CfirAnonymousFunctionExpression) {
                                expressionTypes[element] = element.coneTypeOrNull
                            }
                            if (element is CfirResolvable) {
                                calleeReferences[element] = element.calleeReference
                            }
                            if (element is CfirAnonymousFunction) {
                                anonymousFunctionReturnTypes[element] = element.returnTypeRef
                                anonymousFunctionTypes[element] = element.typeRef
                                anonymousFunctionMatchingTypes[element] = element.matchingParameterFunctionType
                                anonymousFunctionBodies[element] = element.body
                            }
                            if (element is CfirVariable) {
                                variableTypes[element] = element.returnTypeRef
                            }
                            if (element is CfirBlock) {
                                blockStatements[element] = element.statements.toList()
                            }
                            if (element is CfirPatternVariable) {
                                patternVariablePatterns[element] = element.pattern
                            }
                            if (element is CfirMatchBranch) {
                                matchBranchPatterns[element] = element.pattern
                            }
                            if (element is CfirPattern) {
                                CfirPatternMutableState.capture(element)?.let { patternState ->
                                    patternStates[element] = patternState
                                }
                            }
                            if (element is CfirQualifiedAccessExpression) {
                                qualifiedAccessStates[element] = QualifiedAccessState(
                                    dispatchReceiver = element.dispatchReceiver,
                                    typeArguments = element.typeArguments.toList(),
                                )
                            }
                            if (element is CfirFunctionCall) {
                                functionCallArgumentLists[element] = element.argumentList
                            }
                            if (element is CfirControlFlowGraphOwner) {
                                controlFlowGraphReferences[element] = element.controlFlowGraphReference
                            }
                            element.acceptChildren(this, null)
                        }
                    },
                    null,
                )

                return CfirResolutionSnapshot(
                    resolveStates,
                    expressionTypes,
                    calleeReferences,
                    anonymousFunctionReturnTypes,
                    anonymousFunctionTypes,
                    anonymousFunctionMatchingTypes,
                    anonymousFunctionBodies,
                    variableTypes,
                    blockStatements,
                    patternVariablePatterns,
                    matchBranchPatterns,
                    patternStates,
                    qualifiedAccessStates,
                    functionCallArgumentLists,
                    controlFlowGraphReferences,
                )
            }
        }

        private data class QualifiedAccessState(
            val dispatchReceiver: CfirExpression?,
            val typeArguments: List<CfirTypeRef>,
        )
    }
}

private fun <T> MutableList<T>.removeTail(size: Int) {
    if (this.size > size) {
        subList(size, this.size).clear()
    }
}
