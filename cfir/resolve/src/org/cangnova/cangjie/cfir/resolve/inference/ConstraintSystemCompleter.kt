package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferTypeParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferGenericFunctionTypeParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferValueParameterType
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.*
import org.cangnova.cangjie.cfir.resolve.calls.ConePostponedResolvedAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.processCandidatesAndPostponedAtoms
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeFixVariableConstraintPosition
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaParameterType
import org.cangnova.cangjie.cfir.session.inferenceLogger
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeVariable
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionContext
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
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
 * 7. 尝试通过 PCLA 完成调用（特性开关控制）
 * 8. 报告无法推断的类型变量错误
 * 9. 强制分析剩余未分析的延迟参数
 */
class ConstraintSystemCompleter(components: BodyResolveComponents) {
    private val inferenceComponents = components.session.inferenceComponents
    private val variableFixationFinder = inferenceComponents.variableFixationFinder
    private val postponedArgumentsInputTypesResolver = inferenceComponents.postponedArgumentInputTypesResolver
    private val languageVersionSettings = components.session.languageVersionSettings

    /**
     * 延迟 atom 分析器接口。
     *
     * 由调用方实现，负责对单个延迟 atom 执行完整的语义分析。
     * [withPCLASession] 为 true 时表示在 PCLA 模式下重新分析，
     * lambda 体内的约束需要反向传播到外层推断系统。
     */
    fun interface PostponedAtomAnalyzer {
        fun analyzeInternal(
            postponedResolvedAtom: ConePostponedResolvedAtom,
            withPCLASession: Boolean,
        )
    }

    private fun PostponedAtomAnalyzer.analyze(
        postponedResolvedAtom: ConePostponedResolvedAtom,
        withPCLASession: Boolean = false,
    ) {
        return analyzeInternal(postponedResolvedAtom, withPCLASession)
    }

    fun complete(
        c: ConstraintSystemCompletionContext,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ConeResolutionAtom>,
        candidateReturnType: ConeCangJieType,
        context: ResolutionContext,
        analyzer: PostponedAtomAnalyzer,
    ) {
        c.runCompletion(completionMode, topLevelAtoms, candidateReturnType, context, analyzer)
    }

    private fun ConstraintSystemCompletionContext.runCompletion(
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ConeResolutionAtom>,
        topLevelType: ConeCangJieType,
        context: ResolutionContext,
        analyzer: PostponedAtomAnalyzer,
    ) {
        val topLevelTypeVariables = topLevelType.extractTypeVariables()
        context.session.inferenceLogger?.logStage("Call Completion", this)

        completion@ while (true) {
            // 分叉点约束处理（用于多候选重载的分叉推断场景）
            if (completionMode.shouldForkPointConstraintsBeResolved) {
                resolveForkPointsConstraints()
            }

            val postponedArguments = getOrderedNotAnalyzedPostponedArguments(topLevelAtoms)

            // UNTIL_FIRST_LAMBDA 模式：遇到 lambda 时暂停，等待外部处理后继续
            if (completionMode.isUntilFirstLambda() && hasLambdaToAnalyze(postponedArguments)) return

            // IDE 模式：上下文敏感解析（待 IDE 插件支持时完善）
            if (analyzeContextSensitiveResolutionAlternatives(postponedArguments, analyzer)) continue

            // Stage 1：分析参数类型已全部固定的延迟参数
            if (analyzeArgumentWithFixedParameterTypes(postponedArguments) {
                    analyzer.analyze(it)
                }
            ) continue

            val variableForFixation = findFirstVariableForFixation(
                topLevelAtoms, postponedArguments, completionMode, topLevelType,
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
                reportNotEnoughTypeInformation(
                    completionMode, topLevelAtoms, topLevelType, postponedArguments,
                )
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

    private fun ConstraintSystemCompletionContext.findFirstVariableForFixation(
        topLevelAtoms: List<ConeResolutionAtom>,
        postponedArguments: List<ConePostponedResolvedAtom>,
        completionMode: ConstraintSystemCompletionMode,
        topLevelType: ConeCangJieType,
    ): VariableFixationFinder.VariableForFixation? {
        val allTypeVariables = getOrderedAllTypeVariables(topLevelAtoms)
        return variableFixationFinder.findFirstVariableForFixation(
            allTypeVariables, postponedArguments, completionMode, topLevelType,
        )
    }

    /**
     * 尝试通过 PCLA（带 lambda 参数的延迟调用推断）完成推断。
     *
     * 场景：泛型函数接收 lambda，lambda 体内的调用需要反向推断外层类型参数。
     * 通过特性开关控制是否启用，当前保留原始实现。
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

        fun ConeResolvedLambdaAtom.notFixedInputTypeVariables() =
            inputTypes.flatMap { it.extractTypeVariables() }.filter { it !in fixedTypeVariables }

        val lambdaArgumentsWithNotFixedInputs = lambdaArguments.filter { it.notFixedInputTypeVariables().isNotEmpty() }
        val dangerousMultiLambdaBuilderInference = lambdaArgumentsWithNotFixedInputs.size >= 2
        val builder = getBuilder()

        var anyAnalyzed = false
        for ((index, argument) in lambdaArgumentsWithNotFixedInputs.withIndex()) {
            val notFixedInputTypeVariables = argument.notFixedInputTypeVariables()

            if (dangerousMultiLambdaBuilderInference && index > 0) {
                for (variable in notFixedInputTypeVariables) {
                    val typeParameter = variable.typeParameter ?: continue
                    addError(AnonymousFunctionBasedMultiLambdaBuilderInferenceRestriction(argument.anonymousFunction, typeParameter))
                }
                builder.markCouldBeResolvedWithUnrestrictedBuilderInference()
            }

            for (variable in notFixedInputTypeVariables) {
                builder.markPostponedVariable(notFixedTypeVariables.getValue(variable).typeVariable)
            }
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
     * 仓颉函数类型通过 argumentsCount() > 0 判断。
     */
    private fun transformToAtomWithNewFunctionExpectedType(
        c: ConstraintSystemCompletionContext,
        resolutionContext: ResolutionContext,
        argument: PostponedAtomWithRevisableExpectedType,
    ): Boolean = with(c) {
        val revisedExpectedType = argument.revisedExpectedType
            ?.takeIf { it.argumentsCount() > 0 }?.asCone()
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

    private fun ConstraintSystemCompletionContext.fixVariableIfReady(
        variableForFixation: VariableFixationFinder.VariableForFixation,
        completionMode: ConstraintSystemCompletionMode,
        topLevelAtoms: List<ConeResolutionAtom>,
    ): Boolean {
        val variableWithConstraints = notFixedTypeVariables.getValue(variableForFixation.variable)
        if (!variableForFixation.isReady) return false
        val resultType = with(this) {
            inferenceComponents.resultTypeResolver.findResultTypeOrNull(
                variableWithConstraints,
                TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN,
            )
        } as? ConeCangJieType
        if (resultType == null) {
            if (!completionMode.fixNotInferredTypeVariablesToErrorType) return false
            processVariableWhenNotEnoughInformation(variableWithConstraints, topLevelAtoms)
            return true
        }
        fixResolvedVariable(this, variableWithConstraints, resultType)
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
        topLevelType: ConeCangJieType,
        postponedArguments: List<ConePostponedResolvedAtom>,
    ) {
        while (true) {
            val variableForFixation =
                findFirstVariableForFixation(topLevelAtoms, postponedArguments, completionMode, topLevelType)
                    ?: break
            assert(!variableForFixation.isReady) {
                "At this stage there should be no remaining variables with proper constraints"
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
        val resolvedAtom =
            findStatementOfFirstAtomWithVariable(typeVariable, topLevelAtoms)
                ?: topLevelAtoms.firstOrNull()?.expression

        if (resolvedAtom != null) {
            addError(NotEnoughInformationForTypeParameter(typeVariable, resolvedAtom, false))
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
    ): List<TypeConstructorMarker> {
        val result = LinkedHashSet<TypeConstructorMarker>(notFixedTypeVariables.size)

        fun ConeTypeVariable?.toTypeConstructor(): TypeConstructorMarker? =
            this?.typeConstructor?.takeIf { it in notFixedTypeVariables.keys }

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
                }
            ) { postponedAtom ->
                when (postponedAtom) {
                    is ConeResolvedLambdaAtom -> {
                        result.addIfNotNull(postponedAtom.typeVariableForLambdaReturnType.toTypeConstructor())
                    }
                    is ConeLambdaWithTypeVariableAsExpectedTypeAtom -> {
                        postponedAtom.collectNotFixedVariables()
                    }
                    is ConeResolvedCallableReferenceAtom -> {
                        postponedAtom.collectNotFixedVariables()
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

        return result.toList()
    }

    /**
     * 固定类型变量为推断结果类型。
     * 使用 UNKNOWN 方向让结果类型解析器根据约束自行决定固定方向。
     */
    private fun fixResolvedVariable(
        c: ConstraintSystemCompletionContext,
        variableWithConstraints: VariableWithConstraints,
        resultType: ConeCangJieType,
    ) {
        val variable = variableWithConstraints.typeVariable
        c.fixVariable(variable, resultType, ConeFixVariableConstraintPosition(variable))
    }

    companion object {
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
        private fun findStatementOfFirstAtomWithVariable(
            typeVariable: TypeVariableMarker,
            topLevelAtoms: List<ConeResolutionAtom>,
        ): CfirStatement? {
            fun ConeResolutionAtom.findFirstStatementContainingVariable(): CfirStatement? {
                var result: CfirStatement? = null

                fun suggestElement(element: CfirElement) {
                    if (result == null && element is CfirStatement) {
                        result = element
                    }
                }

                this.processCandidatesAndPostponedAtoms(
                    candidateProcessor = { candidate ->
                        if (typeVariable in candidate.freshVariables) {
                            suggestElement(candidate.callInfo.callSite)
                        }
                    },
                    postponedAtomsProcessor = { postponedAtom ->
                        if (postponedAtom is ConeResolvedLambdaAtom) {
                            if (postponedAtom.typeVariableForLambdaReturnType == typeVariable) {
                                suggestElement(postponedAtom.anonymousFunction)
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

val Candidate.csBuilder: ConstraintSystemImpl get() = system.getBuilder()
