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
    /** body resolve 组件集合，提供 call completer、context、session 和返回类型计算器。 */
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    /** 完整重检后用于在成功候选集合中继续选择最具体候选的冲突解析器。 */
    private val conflictResolver: ConeCallConflictResolver,
) {
    /**
     * 使用 lambda body 真实约束收敛重载候选集合。
     *
     * 该流程会对每个候选在对应函数类型下重检同一个 lambda，借助可回滚快照隔离副作用；
     * 若只剩唯一最具体成功候选，则恢复该候选成功分析后的状态并返回收敛后的候选集合。
     */
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

    /**
     * 收集“每个候选都对应同一个未分析 lambda atom”的重载收敛场景。
     */
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

    /**
     * 在指定候选的期望函数类型下分析 lambda。
     *
     * `rollback=true` 时会捕获并回滚约束系统、postponed context、隐式 body resolve session 与 CFIR 树状态；
     * `rollback=false` 时用于最终唯一候选的真实落地分析。
     */
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

    /**
     * 捕获候选集合中所有 postponed atom 的可变状态。
     */
    private fun Set<Candidate>.capturePostponedAtomStates(): PostponedAtomStates {
        val states = IdentityHashMap<ConePostponedResolvedAtom, PostponedAtomState>()
        val postponedChildStates = IdentityHashMap<ConeResolutionAtomWithPostponedChild, ConeResolutionAtom?>()
        val visited = IdentityHashMap<ConeResolutionAtom, Unit>()

        /**
         * 递归访问 atom 图，记录 postponed atom 和 postponed child 的当前状态。
         */
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

    /**
     * 单个候选 lambda 重检的分析结果。
     */
    private class CandidateLambdaAnalysis(
        /** 候选在重检 lambda body 后是否仍然成功。 */
        val successful: Boolean,
        /** 成功且需要回滚时捕获的最终可恢复状态。 */
        val successfulState: SuccessfulCandidateAnalysis?,
    )

    /**
     * 成功候选在回滚模式下捕获的最终状态。
     */
    private class SuccessfulCandidateAnalysis(
        /** CFIR 树与解析状态快照。 */
        private val resolutionSnapshot: CfirResolutionSnapshot,
        /** postponed atom 图的分析状态快照。 */
        private val postponedAtomStates: PostponedAtomStates,
        /** 候选约束系统和诊断等后置状态快照。 */
        private val candidateState: CandidatePostState,
        /** 隐式 body resolve session 的快照。 */
        private val implicitBodyResolveSnapshot: CfirImplicitBodyResolveComputationSessionSnapshot?,
    ) {
        /**
         * 把成功分析后的状态恢复到当前候选和隐式 body resolve session 上。
         */
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

    /**
     * 候选在成功分析完成后的完整可恢复状态。
     */
    private class CandidatePostState private constructor(
        /** 候选约束系统存储副本。 */
        private val systemStorage: ConstraintStorage,
        /** 候选诊断列表副本。 */
        private val diagnostics: List<ResolutionDiagnostic>,
        /** 候选最低适用性副本。 */
        private val lowestApplicability: CandidateApplicability,
        /** 候选 postponed atom 列表副本。 */
        private val postponedAtoms: List<ConePostponedResolvedAtom>,
        /** 候选 postponed PCLA 调用列表副本。 */
        private val postponedPCLACalls: List<ConeResolutionAtom>,
        /** 已经通过 PCLA 分析过的 lambda 列表副本。 */
        private val lambdasAnalyzedWithPCLA: List<CfirDeclaration>,
        /** PCLA 完成结果写回回调列表副本。 */
        private val completionCallbacks: List<(ConeSubstitutor) -> Unit>,
    ) {
        /**
         * 将捕获的候选后置状态写回候选。
         */
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

        /**
         * 候选后置状态工厂。
         */
        companion object {
            /**
             * 捕获候选当前约束系统、诊断、适用性和 postponed 状态。
             */
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

    /**
     * 单个 postponed atom 的可回滚状态。
     */
    private data class PostponedAtomState(
        /** atom 是否已经完成分析。 */
        val analyzed: Boolean,
        /** lambda atom 已收集的返回语句 atom；非 lambda atom 为空。 */
        val returnStatements: Collection<ConeResolutionAtom>?,
    )

    /**
     * postponed atom 图的可回滚状态集合。
     */
    private class PostponedAtomStates(
        /** postponed atom 到自身状态的快照。 */
        private val states: IdentityHashMap<ConePostponedResolvedAtom, PostponedAtomState>,
        /** postponed child 容器到当前子 atom 的快照。 */
        private val postponedChildStates: IdentityHashMap<ConeResolutionAtomWithPostponedChild, ConeResolutionAtom?>,
    ) {
        /**
         * 恢复所有 postponed atom 与 postponed child 状态。
         */
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

    /**
     * 候选分析过程中的可变增量状态。
     */
    private class CandidateMutableState private constructor(
        /** 捕获时诊断列表长度。 */
        private val diagnosticsSize: Int,
        /** 捕获时最低适用性。 */
        private val lowestApplicability: CandidateApplicability,
        /** 捕获时 postponed atom 列表长度。 */
        private val postponedAtomsSize: Int,
        /** 捕获时 postponed PCLA 调用列表长度。 */
        private val postponedPCLACallsSize: Int,
        /** 捕获时 PCLA lambda 列表长度。 */
        private val lambdasAnalyzedWithPCLASize: Int,
        /** 捕获时 PCLA completion 回调列表长度。 */
        private val completionCallbacksSize: Int,
    ) {
        /**
         * 回滚候选分析过程中新增的增量状态。
         */
        fun restore(candidate: Candidate) {
            candidate.diagnostics.removeTail(diagnosticsSize)
            candidate.restoreLowestApplicability(lowestApplicability)
            candidate.postponedAtoms.removeTail(postponedAtomsSize)
            candidate.postponedPCLACalls.removeTail(postponedPCLACallsSize)
            candidate.lambdasAnalyzedWithPCLA.removeTail(lambdasAnalyzedWithPCLASize)
            candidate.onPCLACompletionResultsWritingCallbacks.removeTail(completionCallbacksSize)
        }

        /**
         * 候选增量状态工厂。
         */
        companion object {
            /**
             * 捕获候选当前各个可变集合的长度和最低适用性。
             */
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

    /**
     * CFIR 树可变解析状态快照。
     *
     * overload-by-lambda 会反复在同一个调用树上试探候选，因此必须记录所有可能被 lambda body resolve
     * 改写的类型、引用、body、pattern、CFG 和 resolve state。
     */
    private class CfirResolutionSnapshot private constructor(
        /** 带 resolve state 元素的阶段状态快照。 */
        private val resolveStates: IdentityHashMap<CfirElementWithResolveState, CfirResolveState>,
        /** 表达式到其 cone type 的快照。 */
        private val expressionTypes: IdentityHashMap<CfirExpression, ConeCangJieType?>,
        /** 可解析表达式到 callee reference 的快照。 */
        private val calleeReferences: IdentityHashMap<CfirResolvable, CfirReference>,
        /** 匿名函数返回类型引用快照。 */
        private val anonymousFunctionReturnTypes: IdentityHashMap<CfirAnonymousFunction, CfirTypeRef>,
        /** 匿名函数整体函数类型引用快照。 */
        private val anonymousFunctionTypes: IdentityHashMap<CfirAnonymousFunction, CfirTypeRef>,
        /** 匿名函数匹配参数函数类型快照。 */
        private val anonymousFunctionMatchingTypes: IdentityHashMap<CfirAnonymousFunction, ConeCangJieType?>,
        /** 匿名函数 body 快照。 */
        private val anonymousFunctionBodies: IdentityHashMap<CfirAnonymousFunction, CfirBlock?>,
        /** 变量返回类型引用快照。 */
        private val variableTypes: IdentityHashMap<CfirVariable, CfirTypeRef>,
        /** block 语句列表快照。 */
        private val blockStatements: IdentityHashMap<CfirBlock, List<CfirStatement>>,
        /** 模式变量当前 pattern 快照。 */
        private val patternVariablePatterns: IdentityHashMap<CfirPatternVariable, CfirPattern>,
        /** match branch 当前 pattern 快照。 */
        private val matchBranchPatterns: IdentityHashMap<CfirMatchBranch, CfirPattern>,
        /** pattern 内部可变状态快照。 */
        private val patternStates: IdentityHashMap<CfirPattern, CfirPatternMutableState>,
        /** qualified access 接收者与类型实参快照。 */
        private val qualifiedAccessStates: IdentityHashMap<CfirQualifiedAccessExpression, QualifiedAccessState>,
        /** 函数调用实参列表快照。 */
        private val functionCallArgumentLists: IdentityHashMap<CfirFunctionCall, CfirArgumentList>,
        /** CFG owner 到 CFG reference 的快照。 */
        private val controlFlowGraphReferences: IdentityHashMap<CfirControlFlowGraphOwner, CfirControlFlowGraphReference?>,
    ) {
        /**
         * 把快照中的全部 CFIR 可变状态恢复到原对象上。
         */
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

        /**
         * CFIR 解析快照工厂。
         */
        companion object {
            /**
             * 从根元素遍历并捕获 overload-by-lambda 回滚所需的全部状态。
             */
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
                    /**
                     * 快照捕获访问器。
                     */
                    object : CfirVisitorVoid() {
                        /**
                         * 捕获当前元素及其子树中所有可回滚状态。
                         */
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

        /**
         * qualified access 的可变状态。
         */
        private data class QualifiedAccessState(
            /** 当前 dispatch receiver。 */
            val dispatchReceiver: CfirExpression?,
            /** 当前类型实参列表。 */
            val typeArguments: List<CfirTypeRef>,
        )
    }
}

/**
 * 把列表截断回捕获时的长度。
 */
private fun <T> MutableList<T>.removeTail(size: Int) {
    if (this.size > size) {
        subList(size, this.size).clear()
    }
}
