package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.substitution.asCone

import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferTypeParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferGenericFunctionTypeParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferValueParameterType
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.semantics.BareEnumConstructorInsufficientInformation
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.*
import org.cangnova.cangjie.cfir.resolve.calls.ConePostponedResolvedAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.processCandidatesAndPostponedAtoms
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExpectedTypeConstraintPosition
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.semantics.isSuccess
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.firstErrorTypeOrNull
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.isPostponedInference
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaParameterType
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.session.inferenceLogger
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeVariable
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.components.ResultTypeResolver
import org.cangnova.cangjie.resolve.calls.inference.components.TypeVariableDependencyInformationProvider
import org.cangnova.cangjie.resolve.calls.inference.components.TypeVariableDirectionCalculator
import org.cangnova.cangjie.resolve.calls.inference.components.VariableFixationFinder
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.resolve.calls.inference.model.NotEnoughInformationForTypeParameter
import org.cangnova.cangjie.resolve.calls.inference.model.VariableWithConstraints
import org.cangnova.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.runIf

/**
 * 约束系统完成器。
 *
 * 负责驱动类型推断的完成阶段，通过多轮迭代依次执行以下策略：
 * 1. 分析参数类型已固定的延迟参数
 * 2. 为延迟参数构建新的函数期望类型
 * 3. 固定参数类型变量
 * 4. 将可修订期望类型的 atom 转换为带新函数期望类型的 atom
 * 5. 分析下一个就绪的延迟参数
 * 6. 固定下一个就绪的类型变量
 * 7. 通过 PCLA 分析输入类型尚未固定的 lambda
 * 8. 报告无法推断的类型变量错误
 * 9. 强制分析剩余未分析的延迟参数
 */
class ConstraintSystemCompleter(
    /**
     * 调用完成阶段依赖的 body resolve 组件。
     */
    components: BodyResolveComponents,
) {
    /**
     * 当前会话的推断组件集合。
     */
    private val inferenceComponents = components.session.inferenceComponents
    /** 已完成 lambda body 的实际返回表达式，供最终变量固定保留源码错误。 */
    private val dataFlowAnalyzer by lazy { components.dataFlowAnalyzer }
    /**
     * 类型变量固定选择器。
     */
    private val variableFixationFinder = inferenceComponents.variableFixationFinder
    /**
     * 延迟实参输入类型解析器。
     */
    private val postponedArgumentsInputTypesResolver = inferenceComponents.postponedArgumentInputTypesResolver
    /**
     * 当前语言版本设置。
     */
    private val languageVersionSettings = components.session.languageVersionSettings

    /**
     * 延迟 atom 分析器接口。
     *
     * 由调用方实现，负责对单个延迟 atom 执行完整的语义分析。
     * [withPCLASession] 为 true 时表示在 PCLA 模式下重新分析，
     * lambda 体内的约束需要反向传播到外层推断系统。
     */
    fun interface PostponedAtomAnalyzer {
        /**
         * 分析一个延迟解析 atom。
         */
        fun analyzeInternal(
            postponedResolvedAtom: ConePostponedResolvedAtom,
            withPCLASession: Boolean,
        )
    }

    /**
     * 调用延迟 atom 分析器，默认不启用 PCLA 会话。
     */
    private fun PostponedAtomAnalyzer.analyze(
        postponedResolvedAtom: ConePostponedResolvedAtom,
        withPCLASession: Boolean = false,
    ) {
        return analyzeInternal(postponedResolvedAtom, withPCLASession)
    }

    /**
     * 对外暴露的约束系统完成入口。
     */
    fun complete(
        c: ConstraintSystemCompletionContext,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ConeResolutionAtom>,
        candidateReturnType: ConeCangJieType,
        context: ResolutionContext,
        analyzer: PostponedAtomAnalyzer,
    ) {
        val atomExpr = topLevelAtoms.firstOrNull()?.expression
        c.runCompletion(completionMode, topLevelAtoms, candidateReturnType, context, analyzer)
    }

    /**
     * 在指定完成模式下驱动约束系统完成循环。
     */
    private fun ConstraintSystemCompletionContext.runCompletion(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ConeResolutionAtom>,
        topLevelType: ConeCangJieType,
        context: ResolutionContext,
        analyzer: PostponedAtomAnalyzer,
    ) {
        val topLevelTypeVariables = topLevelType.extractTypeVariables()
        context.session.inferenceLogger?.logStage("Call Completion", this)

        // 本次可变 completion 的任务边界按实际 VM 保留；固定根后不能丢掉其已发现的子任务。
        // 该集合只调度当前 CS 的语义约束，不持有或发布 source/FULL/writer 权限。
        val completionVariables = linkedMapOf<TypeConstructorMarker, TypeVariableMarker>()

        completion@ while (true) {
            // 分叉点约束处理（用于多候选重载的分叉推断场景）
            if (completionMode.shouldForkPointConstraintsBeResolved) {
                resolveForkPointsConstraints()
            }

            val postponedArguments = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)

            // UNTIL_FIRST_LAMBDA 模式：遇到 lambda 时暂停，等待外部处理后继续
            if (completionMode.isUntilFirstLambda() && hasLambdaToAnalyze(postponedArguments)) return

            // 在分析、Join 或固定改变实际 atom/约束前保存边界；后续注册仍从原图及其约束收集。
            getOrderedAllTypeVariables(topLevelAtoms, completionVariables)

            // IDE 模式：上下文敏感解析（待 IDE 插件支持时完善）
            if (analyzeContextSensitiveResolutionAlternatives(postponedArguments, analyzer)) continue

            // Stage 1：分析参数类型已全部固定的延迟参数
            if (analyzeArgumentWithFixedParameterTypes(postponedArguments) {
                    analyzer.analyze(it)
                }
            ) continue

            // 官方可见 Join 是 select 自己的结果约束；在固定 R 前消费实际已分析的分支。
            // 普通 source generic 的变量仍走下方原来的固定与无法推断路径。
            if (completionMode == ConstraintSystemCompletionMode.FULL &&
                constrainCompletedSyntheticSelectResults(topLevelAtoms, context)
            ) continue

            val variableForFixation = findFirstVariableForFixation(
                topLevelAtoms, completionVariables, postponedArguments, completionMode, topLevelType,
            )
            val isThereAnyReadyForFixationVariable = variableForFixation != null

            // 无延迟参数且无待固定变量，推断完成
            if (postponedArguments.isEmpty() && !isThereAnyReadyForFixationVariable) break

            /**
             * 可修订期望类型的延迟 atom（lambda 和可调用引用）：
             * - [ConeLambdaWithTypeVariableAsExpectedTypeAtom]：期望类型为类型变量的 lambda，
             *   后续可修订为更具体的函数类型（Stage 2），并转换为 [ConeResolvedLambdaAtom]（Stage 4）
             * - [ConeResolvedCallableReferenceAtom]：可调用引用，期望类型修订逻辑待完善
             */
            val postponedArgumentsWithRevisableType =
                postponedArguments.filterIsInstance<PostponedAtomWithRevisableExpectedType>()
            val dependencyProvider = TypeVariableDependencyInformationProvider(
                notFixedTypeVariables, postponedArguments, topLevelType, this,
                languageVersionSettings,
            )

            // Stage 2：为延迟参数收集参数类型并构建新的函数期望类型
            val wasBuiltNewExpectedTypeForSomeArgument =
                postponedArgumentsInputTypesResolver.collectParameterTypesAndBuildNewExpectedTypes(
                    postponedArgumentsWithRevisableType,
                    completionMode,
                    dependencyProvider,
                    topLevelTypeVariables,
                )

            if (wasBuiltNewExpectedTypeForSomeArgument) continue

            val postponedAtomsDependingOnFunctionType =
                postponedArguments.filter { it is ConeFunctionTypeRelatedPostponedResolvedAtom }

            if (completionMode.allLambdasShouldBeAnalyzed) {
                // Stage 3：固定所有延迟参数的参数类型变量
                for (argument in postponedAtomsDependingOnFunctionType) {
                    val nextVariable = postponedArgumentsInputTypesResolver.findNextReadyVariableForParameterType(
                        argument, postponedArguments, topLevelType, dependencyProvider,
                    )
                    if (nextVariable != null && fixVariableIfReady(nextVariable, completionMode, topLevelAtoms))
                        continue@completion
                }

                // Stage 4：将可修订期望类型的 atom 转换为带新函数期望类型的 atom
                for (argument in postponedArgumentsWithRevisableType) {
                    val argumentWasTransformed = transformToAtomWithNewFunctionExpectedType(
                        this, context, argument,
                    )
                    if (argumentWasTransformed) continue@completion
                }
            }

            // Stage 5：分析下一个就绪的延迟参数
            if (analyzeNextReadyPostponedArgument(postponedArguments, completionMode) {
                    analyzer.analyze(it)
                }
            ) continue

            // Stage 6：固定下一个约束充分的类型变量
            if (variableForFixation != null && fixVariableIfReady(variableForFixation, completionMode, topLevelAtoms)) continue

            // Stage 7：尝试通过 PCLA 完成调用（带 lambda 参数的延迟调用推断，特性开关控制）
            val areThereAppearedProperConstraintsForSomeVariable = tryToCompleteWithPCLA(
                completionMode, postponedArguments, analyzer,
            )
            if (areThereAppearedProperConstraintsForSomeVariable) continue

            if (completionMode.fixNotInferredTypeVariablesToErrorType) {
                // Stage 8：对无法推断的类型变量报告"信息不足"错误
                if (reportNotEnoughTypeInformation(
                    completionMode, topLevelAtoms, completionVariables, topLevelType, postponedArguments,
                )) continue
            }

            // Stage 9：强制分析剩余未分析的函数类型相关延迟参数并重新执行各阶段
            if (completionMode.allLambdasShouldBeAnalyzed) {
                if (analyzeRemainingNotAnalyzedPostponedArgument(postponedAtomsDependingOnFunctionType) {
                        analyzer.analyze(it)
                    }
                ) continue
            }

            // Stage 10：强制分析所有剩余未分析的延迟参数（仅 FULL 模式）
            if (completionMode.allPostponedAtomsShouldBeAnalyzed) {
                if (analyzeRemainingNotAnalyzedPostponedArgument(postponedArguments) {
                        analyzer.analyze(it)
                    }
                ) continue
            }

            break
        }
    }

    /**
     * 查找当前约束系统中下一个应固定的类型变量。
     */
    private fun ConstraintSystemCompletionContext.findFirstVariableForFixation(
        topLevelAtoms: List<ConeResolutionAtom>,
        completionVariables: MutableMap<TypeConstructorMarker, TypeVariableMarker>,
        postponedArguments: List<ConePostponedResolvedAtom>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: ConeCangJieType,
    ): VariableFixationFinder.VariableForFixation? {
        val allTypeVariables = getOrderedAllTypeVariables(topLevelAtoms, completionVariables)
        val variableForFixation = variableFixationFinder.findFirstVariableForFixation(
            allTypeVariables, postponedArguments, completionMode, topLevelType,
        )
        return variableForFixation
    }

    /**
     * 尝试通过 PCLA（带 lambda 参数的延迟调用推断）完成推断。
     *
     * 场景：泛型函数接收 lambda，lambda 体内的调用需要反向推断外层类型参数。
     * 由完成模式决定是否分析这些 lambda，并将函数体约束保留在同一候选系统中。
     *
     * @return 若通过 PCLA 产生了新的正确约束则返回 true。
     */
    private fun ConstraintSystemCompletionContext.tryToCompleteWithPCLA(
        completionMode: ConstraintSystemCompletionMode,
        postponedArguments: List<ConePostponedResolvedAtom>,
        analyzer: PostponedAtomAnalyzer,
    ): Boolean {
        if (!completionMode.allLambdasShouldBeAnalyzed) return false

        val lambdaArguments = postponedArguments
            .filterIsInstance<ConeResolvedLambdaAtom>()
            .takeIf { it.isNotEmpty() } ?: return false

        fun ConeCangJieType.inputTypeVariablesForPCLA(): Set<TypeVariableTypeConstructorMarker> =
            buildSet {
                (typeConstructor() as? TypeVariableTypeConstructorMarker)?.let(::add)
                addAll(extractTypeVariables())
            }

        /*
         * Kotlin 的 extractTypeVariables() 只收集类型实参中的变量。仓颉官方
         * SynLamExpr 对无上下文 lambda 参数创建的 placeholder 本身就是输入类型，
         * 成员访问约束必须能通过 PCLA 回写到这个根类型变量。
         */
        fun ConeResolvedLambdaAtom.notFixedInputTypeVariables() =
            inputTypes.flatMap { it.inputTypeVariablesForPCLA() }.filter { it !in fixedTypeVariables }

        var anyAnalyzed = false
        for (argument in lambdaArguments) {
            val notFixedInputTypeVariables = argument.notFixedInputTypeVariables()
            if (notFixedInputTypeVariables.isEmpty()) continue
            // 对齐 FIR：未固定输入由 lambda atom 与 PCLA common system 管理，不能写入
            // K1 的 postponedTypeVariables。系统替换假定该列表为空，否则长调用链会反复追加旧变量。
            analyzer.analyze(argument, withPCLASession = true)
            anyAnalyzed = true
        }

        return anyAnalyzed
    }

    /**
     * 将可修订期望类型的 atom 转换为带新函数期望类型的 atom。
     *
     * 仅处理 lambda（[ConeLambdaWithTypeVariableAsExpectedTypeAtom]），
     * 可调用引用（[ConeResolvedCallableReferenceAtom]）暂不支持转换。
     *
     * 仓颉函数类型使用类型系统的显式函数种类协议；参数个数不代表类型种类。
     */
    private fun transformToAtomWithNewFunctionExpectedType(
        c: ConstraintSystemCompletionContext,
        resolutionContext: ResolutionContext,
        argument: PostponedAtomWithRevisableExpectedType,
    ): Boolean = with(c) {
        val revisedExpectedType = argument.revisedExpectedType
            ?.takeIf { it.isFunctionType() }?.asCone()
            ?: return false

        when (argument) {
            is ConeLambdaWithTypeVariableAsExpectedTypeAtom ->
                argument.transformToResolvedLambda(c.getBuilder(), resolutionContext, revisedExpectedType)
            is ConeResolvedCallableReferenceAtom -> return false
            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }

        return true
    }

    /**
     * IDE 模式：上下文敏感解析。
     *
     * 处理限定符（qualifier）在不同上下文下有多个候选的情况，
     * 通过 ideMode 开关控制，仅在 IDE 增量分析时启用。
     * 待 IDE 插件支持时完善。
     */
    private fun analyzeContextSensitiveResolutionAlternatives(
        postponedArguments: List<ConePostponedResolvedAtom>,
        analyzer: PostponedAtomAnalyzer,
    ): Boolean {
        if (!languageVersionSettings.getFlag(AnalysisFlags.ideMode)) return false
        var wasAny = false
        for (atom in postponedArguments) {
            if (atom is ConeContextSensitiveAlternativeForQualifierAtom) {
                analyzer.analyze(atom, withPCLASession = false)
                wasAny = true
            }
        }
        return wasAny
    }

    /**
     * 把官方 SynIfExpr / SynNormalMatchCaseBody 的可见 Join 接入实际 select R 的约束完成。
     * 只写当前系统的 equality；不写 AST、完成凭据、候选状态或 source 诊断。
     */
    private fun ConstraintSystemCompletionContext.constrainCompletedSyntheticSelectResults(
        topLevelAtoms: List<ConeResolutionAtom>,
        context: ResolutionContext,
    ): Boolean {
        val builder = getBuilder()
        val storage = builder.currentStorage()
        val substitutor = builder.buildCurrentSubstitutor().asCone()
        val visited = java.util.Collections.newSetFromMap(
            java.util.IdentityHashMap<Candidate, Boolean>(),
        )
        val candidates = ArrayList<Candidate>()
        topLevelAtoms.forEach { atom ->
            atom.processCandidatesAndPostponedAtoms({ candidate ->
                if (visited.add(candidate)) candidates.add(candidate)
            }) {}
        }
        var addedConstraint = false
        for (candidate in candidates) {
            if (candidate.callInfo.callKind != CallKind.SyntheticSelect ||
                candidate.isSourceCompletionWithdrawn ||
                candidate.implicitTypeComputationScope?.graph?.isCandidateWithdrawn(candidate) == true
            ) continue
            // 当前 owner 正在正式推断；遍历到的 child 却可能已冻结，统一借用其原 CS 快照。
            // snapshot.hasContradiction 只读已记录错误；未决 fork 不作为成功事实，也不在此探查。
            val candidateStorage = candidate.withConstraintSystemForCompletionResults { it.captureStorage() }
            if (candidate.diagnostics.any { !it.isSuccess } || candidateStorage.hasContradiction ||
                candidateStorage.constraintsFromAllForkPoints.isNotEmpty()
            ) continue
            val carrier = candidate.callInfo.callSite as? CfirResolvable ?: continue
            if ((carrier.calleeReference as? CfirNamedReferenceWithCandidate)?.candidate !== candidate) continue
            val resultVariableType = candidate.substitutedReturnTypeFromCalculator() as? ConeTypeVariableType ?: continue
            val constraints = notFixedTypeVariables[resultVariableType.typeConstructor] ?: continue
            val variable = constraints.typeVariable as? ConeTypeVariable ?: continue
            if (variable.typeConstructor in outerTypeVariables.orEmpty()) continue
            if (candidate.freshVariables.none { it === variable } ||
                storage.allTypeVariables[variable.typeConstructor] !== variable ||
                candidateStorage.allTypeVariables[variable.typeConstructor] !== variable
            ) continue
            // 已确定的 source target 由原 expected-type equality 及逐分支检查负责。
            // 后到达的正式目标记录在实际 root VM 的约束中；原 CallInfo 仍可为 ContextDependent。
            // 这里只停止无目标 Join 的默认化，不把 expected 约束当成分支已检查成功的证明。
            val hasFormalCompletionTarget = constraints.constraints.any { constraint ->
                if (constraint.position.from !is ConeExpectedTypeConstraintPosition ||
                    constraint.kind != ConstraintKind.UPPER && constraint.kind != ConstraintKind.EQUALITY
                ) return@any false
                var closed = true
                constraint.type.asCone().forEachTypeIncludingMetadata { type ->
                    if (type is ConeTypeVariableType || type is ConeStubType || type is ConeErrorType ||
                        type is ConeIdealLiteralType || type is ConePrimitiveType && type.kind.isIdeal
                    ) closed = false
                }
                closed
            }
            if (hasFormalCompletionTarget) continue
            val contextualType = (candidate.callInfo.resolutionMode as? ResolutionMode.WithExpectedType)?.expectedType
            if (contextualType != null && !contextualType.contains {
                    it is ConeTypeVariableType || it is ConeErrorType || it is ConeStubType
                }
            ) continue
            val branches = when (carrier) {
                is CfirIfExpression -> listOf(carrier.thenBranch, checkNotNull(carrier.elseBranch))
                is CfirMatchExpression -> {
                    if (carrier.exhaustiveness is CfirMatchExhaustivenessStatus.NonExhaustive) continue
                    carrier.branches.map { it.body }
                }
                is CfirTryExpression -> listOf(carrier.tryBlock) + carrier.catches.map { it.body }
                else -> error("Unexpected synthetic select carrier: " + carrier.javaClass.name)
            }
            if (branches.isEmpty() || candidate.arguments.size != branches.size ||
                candidate.arguments.zip(branches).any { (atom, branch) -> atom.expression !== branch }
            ) continue
            val sourceError = (carrier as CfirExpression).firstErrorTypeOrNull(context.session)
            if (sourceError != null && !sourceError.isPostponedInference) continue
            val types = candidate.arguments.map { atom ->
                completedSelectArgumentType(atom, substitutor, context)
            }
            if (types.any { it == null }) continue
            val completedTypes = types.filterNotNull()
            // try/catch 保留官方按源码顺序 Join 的契约；finally 不是候选实参。
            val joined = if (carrier is CfirTryExpression) {
                var result: ConeCangJieType? = completedTypes.first()
                for (next in completedTypes.drop(1)) {
                    val previous = result ?: break
                    result = context.session.typeContext.commonVisibleSuperTypeOrNull(listOf(previous, next))
                }
                result
            } else {
                context.session.typeContext.commonVisibleSuperTypeOrNull(completedTypes)
            } ?: continue
            if (constraints.constraints.any { constraint ->
                    constraint.kind == ConstraintKind.EQUALITY &&
                        AbstractTypeChecker.equalTypes(context.session.typeContext, constraint.type.asCone(), joined)
                }
            ) continue
            builder.addEqualityConstraint(variable.defaultType, joined, ConeArgumentConstraintPosition(carrier))
            addedConstraint = true
        }
        return addedConstraint
    }

    /** 从实际 atom 取得当前 owner 已解出的值；不由 fixed 表或 snapshot 推定 child 的 FULL 完成。 */
    private fun ConstraintSystemCompletionContext.completedSelectArgumentType(
        atom: ConeResolutionAtom,
        substitutor: ConeSubstitutor,
        context: ResolutionContext,
    ): ConeCangJieType? {
        val sourceError = atom.expression.firstErrorTypeOrNull(context.session)
        if (sourceError != null && !sourceError.isPostponedInference) return null
        val type = when (atom) {
            is ConeResolutionAtomWithSingleChild -> {
                val child = atom.subAtom
                if (child != null) completedSelectArgumentType(child, substitutor, context)
                else atom.expression.coneTypeOrNull
            }
            is ConeResolutionAtomWithPostponedChild -> atom.subAtom?.let {
                completedSelectArgumentType(it, substitutor, context)
            }
            is ConeAtomWithCandidate -> {
                val actual = atom.candidate
                if (actual.isSourceCompletionWithdrawn ||
                    actual.implicitTypeComputationScope?.graph?.isCandidateWithdrawn(actual) == true ||
                    (atom.expression as? CfirResolvable)?.calleeReference.let {
                        (it as? CfirNamedReferenceWithCandidate)?.candidate !== actual
                    } || getOrderedNotAnalyzedPostponedArguments(actual).isNotEmpty()
                ) return null
                val childStorage = actual.withConstraintSystemForCompletionResults { it.captureStorage() }
                if (actual.diagnostics.any { !it.isSuccess } || childStorage.hasContradiction ||
                    childStorage.constraintsFromAllForkPoints.isNotEmpty()
                ) return null
                // 父表仍属当前正常 completion；child 一律从 immutable snapshot 构造替换器。
                val parentStorage = getBuilder().currentStorage()
                val childSubstitutor = childStorage.buildCurrentSubstitutor(context.session.typeContext, emptyMap()).asCone()
                for (variable in actual.freshVariables) {
                    val constructor = variable.typeConstructor
                    if (childStorage.allTypeVariables[constructor] !== variable) return null
                    val parentVariable = parentStorage.allTypeVariables[constructor]
                    val childFixed = childStorage.fixedTypeVariables[constructor]?.asCone()
                    // 父、子都必须持有实际 VM；父表缺失不能由独立 child 的 fixed 值补成成功。
                    if (parentVariable !== variable) return null
                    val parentFixed = parentStorage.fixedTypeVariables[constructor]?.asCone() ?: return null
                    val parentSolved = substitutor.substituteOrSelf(parentFixed)
                    if (parentSolved.contains { it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType }) return null
                    if (childFixed != null) {
                        val childSolved = substitutor.substituteOrSelf(childSubstitutor.substituteOrSelf(childFixed))
                        if (childSolved.contains { it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType } ||
                            !AbstractTypeChecker.equalTypes(context.session.typeContext, childSolved, parentSolved)
                        ) return null
                    } else {
                        // frozen partial child 可以仍保留未定行；这仅证明父 owner 现在解出了同一 VM。
                        if (childStorage.notFixedTypeVariables[constructor]?.typeVariable !== variable) return null
                    }
                }
                // completeCall 已发布实际调用的 initial/partial 类型；不重新消费 callable-value 的 invoke 槽。
                childSubstitutor.substituteOrSelf(atom.expression.coneTypeOrNull ?: return null)
            }
            is ConeResolvedLambdaAtom -> {
                if (!atom.analyzed ||
                    (atom.expression as? CfirAnonymousFunctionExpression)?.anonymousFunction !== atom.anonymousFunction ||
                    atom.returnStatements.any { completedSelectArgumentType(it, substitutor, context) == null }
                ) return null
                val shape = atom.expectedType as? ConeFunctionType ?: return null
                ConeFunctionType(atom.parameterTypes, atom.returnType, shape.isCFunc, shape.isClosureType,
                    shape.hasVariableLenArg, shape.attributes)
            }
            is ConeLambdaWithTypeVariableAsExpectedTypeAtom -> atom.subAtom?.let {
                completedSelectArgumentType(it, substitutor, context)
            }
            is ConeResolvedCallableReferenceAtom -> {
                if (!atom.analyzed || atom.needsResolution) return null
                atom.expression.coneTypeOrNull
            }
            is ConeSimpleNameForContextSensitiveResolution,
            is ConeContextSensitiveAlternativeForQualifierAtom -> {
                if (!(atom as ConePostponedResolvedAtom).analyzed) return null
                atom.expression.coneTypeOrNull
            }
            is ConeSimpleLeafResolutionAtom -> atom.expression.coneTypeOrNull
        } ?: return null
        val completed = substitutor.substituteOrSelf(type)
        if (completed.contains { it is ConeTypeVariableType || it is ConeErrorType || it is ConeStubType }) return null
        return IdealTypeResolver.replaceIdealTypes(completed)
    }

    /** 当类型变量已满足固定条件时执行固定。 */
    private fun ConstraintSystemCompletionContext.fixVariableIfReady(
        variableForFixation: VariableFixationFinder.VariableForFixation,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ConeResolutionAtom>,
    ): Boolean {
        val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
        if (!variableForFixation.isReady) return false

        fixVariable(this, variableWithConstraints, topLevelAtoms)
        return true
    }

    /**
     * 报告所有无法推断的类型变量错误。
     *
     * 依次找出约束不足的类型变量，为每个变量添加诊断错误并固定为错误类型，
     * 确保推断不会卡住，后续阶段可以继续执行。
     */
    private fun ConstraintSystemCompletionContext.reportNotEnoughTypeInformation(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ConeResolutionAtom>,
        completionVariables: MutableMap<TypeConstructorMarker, TypeVariableMarker>,
        topLevelType: ConeCangJieType,
        postponedArguments: List<ConePostponedResolvedAtom>,
    ): Boolean {
        while (true) {
            val variableForFixation =
                findFirstVariableForFixation(topLevelAtoms, completionVariables, postponedArguments, completionMode, topLevelType)
                    ?: return false
            // 延迟分析或错误恢复可能刚刚解除依赖，交回正常完成循环处理新就绪变量。
            if (variableForFixation.isReady) return true
            if (variableForFixation.dependencyBlockedVariables.isNotEmpty()) {
                // 官方不能选出依赖组时保留整组未解状态。恢复用错误类型不能成为
                // 剩余变量的求解证据，否则会因逐个注入错误而偶然解开原本不可解的环。
                for (variable in variableForFixation.dependencyBlockedVariables) {
                    val constraints = notFixedTypeVariables[variable] ?: continue
                    processVariableWhenNotEnoughInformation(constraints, topLevelAtoms)
                }
                continue
            }
            val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
            processVariableWhenNotEnoughInformation(variableWithConstraints, topLevelAtoms)
        }
    }

    /**
     * 对单个无法推断的类型变量报告错误并固定为错误类型。
     *
     * 根据类型变量的种类生成不同的错误诊断：
     * - 来自声明类型参数（[ConeTypeParameterBasedTypeVariable]）：报告具体参数名
     * - 来自 lambda 参数类型推断（[ConeTypeVariableForLambdaParameterType]）：报告 lambda 参数无法推断
     * - 其他：报告通用无法推断错误
     */
    private fun ConstraintSystemCompletionContext.processVariableWhenNotEnoughInformation(
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ConeResolutionAtom>,
    ) {
        val typeVariable = variableWithConstraints.typeVariable
        // 官方 SynLamExpr 以函数体错误决定 lambda 失败，不再把返回占位的约束不足
        // 报成参数缺少注解。只在最终无法固定变量时传播根错误，不能在临时写回阶段
        // 固化它，否则依赖参数定型后重查的成员访问会失去继续求解的机会。
        if (typeVariable is ConeTypeVariableForLambdaReturnType) {
            val lambda = typeVariable.argument as? CfirAnonymousFunction
            val returnError = lambda?.let { function ->
                dataFlowAnalyzer.returnExpressionsOfAnonymousFunction(function)
                    .firstNotNullOfOrNull { it.expression.coneTypeOrNull as? ConeErrorType }
            }
            if (returnError != null) {
                fixVariable(
                    typeVariable,
                    ConeErrorType(ConeUnreportedDuplicateDiagnostic(returnError.diagnostic)),
                    ConeFixVariableConstraintPosition(typeVariable),
                )
                return
            }
        }
        val source = findStatementOfFirstAtomWithVariable(typeVariable, topLevelAtoms)
        val resolvedAtom = source?.statement ?: topLevelAtoms.firstOrNull()?.expression

        if (resolvedAtom != null) {
            val origin = source?.candidate
            val constructor = origin?.symbol?.takeIf { it.isBound }?.cfir as? CfirEnumConstructor
            if (origin != null && constructor != null && constructor.valueParameters.isEmpty() &&
                origin.callInfo.callSite is CfirNamedAccessExpression && origin.callInfo.explicitReceiver == null &&
                !origin.callInfo.hasExplicitTypeArguments
            ) {
                addError(BareEnumConstructorInsufficientInformation(typeVariable, resolvedAtom, origin))
            } else {
                addError(NotEnoughInformationForTypeParameter(typeVariable, resolvedAtom, false))
            }
        }

        val resultErrorType = when (typeVariable) {
            is ConeTypeParameterBasedTypeVariable ->
                createCannotInferErrorType(
                    typeVariable.typeParameterSymbol,
                    "Cannot infer argument for type parameter ${typeVariable.typeParameterSymbol.name}",
                    isUninferredParameter = true,
                    isGenericFunctionCallInferenceFailure = true,
                )
            is ConeTypeVariableForLambdaParameterType -> createCannotInferErrorType(
                typeParameterSymbol = null,
                message = "Cannot infer lambda parameter type",
            )
            else -> createCannotInferErrorType(
                typeParameterSymbol = null,
                message = "Cannot infer type variable $typeVariable",
            )
        }

        fixVariable(typeVariable, resultErrorType, ConeFixVariableConstraintPosition(typeVariable))
    }

    /**
     * 收集所有顶层 atom 中尚未固定的类型变量，按声明顺序排列。
     *
     * 遍历顺序：候选的新鲜变量 → lambda 返回类型变量 → 可修订期望类型 atom 中的变量。
     * 保持有序性确保变量固定顺序具有确定性，避免推断结果因遍历顺序不同而变化。
     */
    private fun ConstraintSystemCompletionContext.getOrderedAllTypeVariables(
        topLevelAtoms: List<ConeResolutionAtom>,
        completionVariables: MutableMap<TypeConstructorMarker, TypeVariableMarker>,
    ): List<TypeConstructorMarker> {
        val result = LinkedHashSet<TypeConstructorMarker>(notFixedTypeVariables.size)

        // 固定、回滚或重查移除的 VM 不再调度；同名、同声明或替换后的 VM 不能继承旧任务身份。
        completionVariables.entries.removeAll { (constructor, variable) ->
            allTypeVariables[constructor] !== variable || notFixedTypeVariables[constructor]?.typeVariable !== variable
        }
        result.addAll(completionVariables.keys)

        fun ConeTypeVariable?.toTypeConstructor(): TypeConstructorMarker? =
            this?.typeConstructor?.takeIf {
                allTypeVariables[it] === this && notFixedTypeVariables[it]?.typeVariable === this
            }

        fun ConeCangJieType.collectBoundaryTypeVariables() {
            // 仓颉函数/元组的 typeArguments 为空；完成边界必须遍历真实语义组成类型。
            contains { type ->
                val constructor = type.typeConstructor()
                if (constructor in notFixedTypeVariables) {
                    result.add(constructor)
                }
                false
            }
        }

        fun PostponedAtomWithRevisableExpectedType.collectNotFixedVariables() {
            val revisedExpectedType = revisedExpectedType as? ConeCangJieType ?: return
            for (typeArgument in revisedExpectedType.typeArguments) {
                val constructor = typeArgument.type.typeConstructor() ?: continue
                if (constructor in notFixedTypeVariables) {
                    result.add(constructor)
                }
            }
        }

        fun ConeResolvedCallableReferenceAtom.collectNotFixedVariables() {
            val revisedExpectedType = revisedExpectedType as? ConeCangJieType ?: return
            for (typeArgument in revisedExpectedType.typeArguments) {
                val constructor = typeArgument.type.typeConstructor() ?: continue
                if (constructor in notFixedTypeVariables) {
                    result.add(constructor)
                }
            }
        }

        fun ConeResolutionAtom.collectAllTypeVariables() {
            processCandidatesAndPostponedAtoms(
                candidateProcessor = { candidate ->
                    candidate.freshVariables.mapNotNullTo(result) { typeVariable ->
                        typeVariable.toTypeConstructor()
                    }
                    candidate.additionalCompletionVariables.filterTo(result) { typeConstructor ->
                        typeConstructor in notFixedTypeVariables
                    }
                    // 仓颉构造器的 scope 可以已经实例化 owner，签名中的实际变量不再
                    // 属于本候选的 freshVariables。按真实返回签名保留这些完成边界变量。
                    candidate.substitutedReturnTypeFromCalculator().collectBoundaryTypeVariables()
                }
            ) { postponedAtom ->
                when (postponedAtom) {
                    is ConeResolvedLambdaAtom -> {
                        result.addIfNotNull(postponedAtom.typeVariableForLambdaReturnType.toTypeConstructor())
                        postponedAtom.inputTypes.forEach { inputType ->
                            inputType.collectBoundaryTypeVariables()
                        }
                    }
                    is ConeLambdaWithTypeVariableAsExpectedTypeAtom -> {
                        postponedAtom.collectNotFixedVariables()
                    }
                    is ConeResolvedCallableReferenceAtom -> {
                        if (postponedAtom.needsResolution) {
                            postponedAtom.collectNotFixedVariables()
                        }
                    }
                    is ConeSimpleNameForContextSensitiveResolution,
                    is ConeContextSensitiveAlternativeForQualifierAtom -> {
                        // 上下文敏感解析的 atom 尚未解析，无类型变量可收集
                    }
                }
            }
        }

        for (topLevelAtom in topLevelAtoms) {
            topLevelAtom.collectAllTypeVariables()
        }

        // 已记录的返回约束可以依赖 scope 产生的变量，而其调用 carrier 已被暂定写回。
        // 完成边界沿当前 CS 的真实约束取得传递依赖；未注册或无关的变量不进入求解集合。
        val dependencies = ArrayDeque(result)
        while (dependencies.isNotEmpty()) {
            val variable = dependencies.removeFirst()
            for (constraint in notFixedTypeVariables.getValue(variable).constraints) {
                constraint.type.contains { type ->
                    val constructor = type.typeConstructor()
                    if (constructor in notFixedTypeVariables && result.add(constructor)) dependencies.addLast(constructor)
                    false
                }
            }
        }

        // 保留已实际发现的闭包，而非下轮从已经删去的根约束重新猜测；不扫描全 CS 作为任务源。
        for (constructor in result) {
            completionVariables[constructor] = notFixedTypeVariables.getValue(constructor).typeVariable
        }

        return result.toList()
    }

/**
     * 根据当前约束计算类型变量结果类型并固定该变量。
     *
     * 当结果类型是推断失败标记（[ResultTypeResolver.SOLVER_FAILURE_MARKER]）时，
     * 不直接固定该中性错误类型，而是复用"无法推断"错误路径（[processVariableWhenNotEnoughInformation]）
     * 报告 UNABLE_TO_INFER_GENERIC_FUNC 并固定为带具体参数名的错误类型，确保互斥下界、
     * 约束矛盾等推断失败场景产生用户可见诊断。
     */
    private fun fixVariable(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
        topLevelAtoms: List<ConeResolutionAtom>,
    ) {
        val resultType = with(c) {
            inferenceComponents.resultTypeResolver.findResultType(
                variableWithConstraints,
                TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN,
            )
        }

        if (resultType is ConeErrorType && resultType.diagnostic.reason == ResultTypeResolver.SOLVER_FAILURE_MARKER) {
            with(c) { processVariableWhenNotEnoughInformation(variableWithConstraints, topLevelAtoms) }
            return
        }

        val variable = variableWithConstraints.typeVariable
        c.fixVariable(variable, resultType, ConeFixVariableConstraintPosition(variable))
    }

    /**
     * 约束系统完成器的静态辅助入口。
     */
    companion object {
        /**
         * 收集候选中尚未分析的延迟 atom。
         */
        internal fun getOrderedNotAnalyzedPostponedArguments(
            candidate: Candidate,
        ): List<ConePostponedResolvedAtom> {
            val callSite = candidate.callInfo.callSite as CfirExpression
            return getOrderedNotAnalyzedPostponedArguments(
                listOf(ConeAtomWithCandidate(callSite, candidate))
            )
        }

        /**
         * 收集所有顶层 atom 中尚未分析的延迟 atom，保持声明顺序。
         *
         * 使用慢速断言（RUN_SLOW_ASSERTIONS）验证所有 atom 都被正确收集，
         * 仅在调试/测试模式下启用，避免正常编译时的性能损耗。
         */
        private fun getOrderedNotAnalyzedPostponedArguments(
            topLevelAtoms: List<ConeResolutionAtom>,
        ): List<ConePostponedResolvedAtom> {
            val notAnalyzedArguments = arrayListOf<ConePostponedResolvedAtom>()
            for (topLevelAtom in topLevelAtoms) {
                val isPostponedAtomFoundForAssertion = runIf(AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                    mutableMapOf<ConePostponedResolvedAtom, Boolean>()
                }

                topLevelAtom.processCandidatesAndPostponedAtoms(
                    candidateProcessor = { candidate ->
                        if (isPostponedAtomFoundForAssertion != null) {
                            for (atom in candidate.postponedAtoms) {
                                isPostponedAtomFoundForAssertion.computeIfAbsent(atom) { false }
                            }
                        }
                    },
                    postponedAtomsProcessor = { atom ->
                        notAnalyzedArguments.addIfNotNull(atom.takeUnless { it.analyzed })
                        isPostponedAtomFoundForAssertion?.put(atom, true)
                    }
                )

                check(
                    isPostponedAtomFoundForAssertion == null ||
                            isPostponedAtomFoundForAssertion.values.all { it }
                ) {
                    "Some postponed atoms were not collected."
                }
            }
            return notAnalyzedArguments
        }

        /**
         * 在顶层 atom 中查找包含指定类型变量的第一个语句节点。
         * 用于在报告"无法推断"错误时定位错误发生的源码位置。
         */
        private data class InsufficientInformationSource(val statement: CfirStatement, val candidate: Candidate?)

        private fun findStatementOfFirstAtomWithVariable(
            typeVariable: TypeVariableMarker,
            topLevelAtoms: List<ConeResolutionAtom>,
        ): InsufficientInformationSource? {
            val typeConstructor = (typeVariable as? ConeTypeVariable)?.typeConstructor

            fun ConeCangJieType.containsTypeConstructor(constructor: TypeConstructorMarker): Boolean {
                if (this is ConeTypeVariableType && this.typeConstructor == constructor) return true
                return typeArguments.any { typeProjection ->
                    typeProjection.type.containsTypeConstructor(constructor)
                }
            }

            fun ConeResolutionAtom.findFirstStatementContainingVariable(): InsufficientInformationSource? {
                var result: InsufficientInformationSource? = null

                fun suggestElement(element: CfirElement, candidate: Candidate? = null) {
                    if (result == null && element is CfirStatement) {
                        result = InsufficientInformationSource(element, candidate)
                    }
                }

                this.processCandidatesAndPostponedAtoms(
                    candidateProcessor = { candidate ->
                        if (typeVariable in candidate.freshVariables) {
                            suggestElement(candidate.callInfo.callSite, candidate)
                        }
                    },
                    postponedAtomsProcessor = { postponedAtom ->
                        if (postponedAtom is ConeResolvedLambdaAtom) {
                            if (postponedAtom.typeVariableForLambdaReturnType == typeVariable) {
                                suggestElement(postponedAtom.anonymousFunction)
                            } else if (typeConstructor != null) {
                                postponedAtom.inputTypes.forEachIndexed { index, inputType ->
                                    if (inputType.containsTypeConstructor(typeConstructor)) {
                                        postponedAtom.anonymousFunction.valueParameters
                                            .getOrNull(index)
                                            ?.let { suggestElement(it) }
                                    }
                                }
                            }
                        }
                    }
                )

                return result
            }

            return topLevelAtoms.firstNotNullOfOrNull(
                ConeResolutionAtom::findFirstStatementContainingVariable,
            )
        }

        /**
         * 创建"无法推断"的错误类型。
         *
         * @param typeParameterSymbol 来自声明的类型参数符号，null 表示非声明类型参数（如 lambda 参数）。
         * @param message 错误原因描述。
         * @param isUninferredParameter 是否为未推断的类型参数。
         */
        private fun createCannotInferErrorType(
            typeParameterSymbol: CfirTypeParameterSymbol?,
            message: String,
            isUninferredParameter: Boolean = false,
            isGenericFunctionCallInferenceFailure: Boolean = false,
        ): ConeErrorType {
            val diagnostic = when (typeParameterSymbol) {
                null -> ConeCannotInferValueParameterType(
                    valueParameter = null,
                    reason = message,
                )
                else -> if (isGenericFunctionCallInferenceFailure) {
                    ConeCannotInferGenericFunctionTypeParameterType(
                        typeParameter = typeParameterSymbol,
                        reason = message,
                    )
                } else {
                    ConeCannotInferTypeParameterType(
                        typeParameter = typeParameterSymbol,
                        reason = message,
                    )
                }
            }
            return ConeErrorType(diagnostic, isUninferredParameter)
        }
    }
}

/**
 * 候选当前约束系统 builder 视图。
 */
val Candidate.csBuilder: ConstraintSystemImpl get() = system.getBuilder()
