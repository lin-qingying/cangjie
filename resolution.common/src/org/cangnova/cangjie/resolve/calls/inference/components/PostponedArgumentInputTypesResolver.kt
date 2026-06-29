package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.VariableWithConstraints
import org.cangnova.cangjie.resolve.calls.model.LambdaWithTypeVariableAsExpectedTypeMarker
import org.cangnova.cangjie.resolve.calls.model.PostponedAtomWithRevisableExpectedType
import org.cangnova.cangjie.resolve.calls.model.PostponedCallableReferenceMarker
import org.cangnova.cangjie.resolve.calls.model.PostponedResolvedAtomMarker
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.utils.SmartSet
import java.util.Stack

/**
 * 本文件中 completion context 的简写。
 */
private typealias Context = ConstraintSystemCompletionContext
/**
 * 根据类型变量提供已解析 atom 的查询函数。
 */
private typealias ResolvedAtomProvider = (TypeVariableMarker) -> Any?

/**
 * 延迟参数输入类型解析器。
 *
 * 负责在类型推断完成阶段，为尚未确定参数类型的延迟参数（lambda、可调用引用）
 * 构建新的函数期望类型，从而使推断引擎能够进一步固定相关类型变量。
 *
 * 仓颉中函数是一级公民，函数类型是独立的普通类型（如 `(Int) -> String`），
 * 通过约束中的函数类型实参直接提取参数类型，无需 Kotlin 内置函数类型反射。
 */
class PostponedArgumentInputTypesResolver(
    /** 推断结果类型解析器。 */
    private val resultTypeResolver: ResultTypeResolver,
    /** 类型变量固定候选查找器。 */
    private val variableFixationFinder: VariableFixationFinder,
    /** 解析输入类型所需的约束系统工具上下文。 */
    private val resolutionTypeSystemContext: ConstraintSystemUtilContext,
) {
    /**
     * 从约束和声明中收集到的参数类型信息，用于构建新的函数期望类型。
     *
     * @param parametersFromDeclaration lambda 声明中显式指定的参数类型列表，null 表示未声明。
     * @param parametersFromDeclarationOfRelatedLambdas 相关联的其他 lambda 声明中的参数类型集合。
     * @param parametersFromConstraints 从约束中提取的参数类型集合，每项为带方向的类型列表。
     * @param annotations 函数类型上的注解列表。
     */
    private class ParameterTypesInfo(
        /** lambda 声明中显式指定的参数类型列表。 */
        val parametersFromDeclaration: List<CangJieTypeMarker?>?,
        /** 相关 lambda 声明中显式指定的参数类型集合。 */
        val parametersFromDeclarationOfRelatedLambdas: Set<List<CangJieTypeMarker?>>?,
        /** 从约束中提取的参数类型集合。 */
        val parametersFromConstraints: Set<List<TypeWithKind>>?,
        /** 函数类型上的注解列表。 */
        val annotations: List<AnnotationMarker>?,
    )

    /**
     * 从函数类型中提取参数类型列表。
     *
     * 仓颉函数类型是普通类型，参数类型即为类型实参（去掉最后一个返回类型实参）。
     */
    context(c: Context)
    private fun CangJieTypeMarker.extractCallableArgumentTypes(): List<CangJieTypeMarker> {
        return with(resolutionTypeSystemContext) {
            extractBuiltinFunctionArgumentTypes()
        }
    }

    /**
     * 带约束方向的类型包装，用于记录从约束中提取的参数类型及其子类型方向。
     *
     * @param type 参数类型。
     * @param direction 约束方向，默认为上界（UPPER）。
     */
    data class TypeWithKind(
        /** 参数类型。 */
        val type: CangJieTypeMarker,
        /** 该类型来自约束的方向。 */
        val direction: ConstraintKind = ConstraintKind.UPPER,
    )

    /**
     * 从变量的约束中查找所有函数类型约束，提取为带方向的类型列表。
     *
     * 同时考虑浅层依赖变量的约束，确保间接关联的函数类型也能被发现。
     */
    context(c: Context)
    private fun VariableWithConstraints.findFunctionalTypesInConstraints(
        variableDependencyProvider: TypeVariableDependencyInformationProvider,
    ): List<TypeWithKind> {
        fun List<Constraint>.extractFunctionalTypes() = mapNotNull { constraint ->
            TypeWithKind(constraint.type, constraint.kind)
        }

        val typeVariableTypeConstructor = typeVariable.freshTypeConstructor()
        val dependentVariables =
            variableDependencyProvider.getShallowlyDependentVariables(typeVariableTypeConstructor).orEmpty() +
                    typeVariableTypeConstructor

        return dependentVariables.flatMap { type ->
            val constraints = c.notFixedTypeVariables[type]?.constraints ?: return@flatMap emptyList()
            // 仓颉函数类型是普通类型，通过参数数量 > 0 判断是否为函数类型约束
            val constraintsWithFunctionalType = constraints.filter { constraint ->
                constraint.type.argumentsCount() > 0
            }
            constraintsWithFunctionalType.extractFunctionalTypes()
        }
    }

    /**
     * 提取延迟参数的参数类型信息。
     *
     * 综合以下来源的参数类型：
     * 1. lambda/函数表达式的显式声明（parametersFromDeclaration）
     * 2. 约束中的函数类型实参（parametersFromConstraints）
     * 3. 相关联的其他 lambda 声明（parametersFromDeclarationOfRelatedLambdas）
     *
     * 若约束中存在参数数量不一致的函数类型，则返回 null（无法确定参数列表）。
     */
    context(c: Context)
    private fun extractParameterTypesInfo(
        argument: PostponedAtomWithRevisableExpectedType,
        postponedArguments: List<PostponedAtomWithRevisableExpectedType>,
        variableDependencyProvider: TypeVariableDependencyInformationProvider,
    ): ParameterTypesInfo? {
        val expectedType = argument.expectedType ?: return null
        val variableWithConstraints = c.notFixedTypeVariables[expectedType.typeConstructor()] ?: return null
        val functionalTypesFromConstraints =
            variableWithConstraints.findFunctionalTypesInConstraints(variableDependencyProvider)

        // 约束中存在参数数量不一致的函数类型，无法确定参数列表，提前返回
        if (functionalTypesFromConstraints.distinctBy { it.type.argumentsCount() }.size > 1)
            return null

        val parameterTypesFromDeclaration =
            if (argument is LambdaWithTypeVariableAsExpectedTypeMarker) argument.parameterTypesFromDeclaration
            else null

        // 提取约束中函数类型的参数类型，方向取反（lambda 参数是逆变的）
        val parameterTypesFromConstraints = functionalTypesFromConstraints.mapTo(SmartSet.create()) { typeWithKind ->
            typeWithKind.type.extractCallableArgumentTypes().map { parameterType ->
                TypeWithKind(parameterType, typeWithKind.direction.opposite())
            }
        }

        val annotations = functionalTypesFromConstraints.map { it.type.getAttributes() }.flatten().distinct()

        val (parameterTypesFromDeclarationOfRelatedLambdas, maxParameterCount) =
            computeParameterInfoFromRelatedLambdas(
                argument,
                postponedArguments,
                variableDependencyProvider,
                parameterTypesFromConstraints,
                parameterTypesFromDeclaration,
            )

        val isLambda = with(resolutionTypeSystemContext) { argument.isLambda() }

        return ParameterTypesInfo(
            parametersFromDeclaration = if (parameterTypesFromDeclaration != null && isLambda &&
                parameterTypesFromDeclaration.size == maxParameterCount
            ) parameterTypesFromDeclaration else parameterTypesFromDeclaration,
            parametersFromDeclarationOfRelatedLambdas = parameterTypesFromDeclarationOfRelatedLambdas,
            parametersFromConstraints = parameterTypesFromConstraints,
            annotations = annotations,
        )
    }

    /**
     * 从相关联的其他 lambda 声明中提取参数类型信息。
     *
     * 当多个 lambda 共享同一期望类型变量（浅层依赖）时，
     * 其他 lambda 的显式参数声明可以辅助推断当前 lambda 的参数类型。
     *
     * @return 相关 lambda 的参数类型集合，以及所有来源中的最大参数数量。
     */
    private data class ParameterInfoFromRelatedLambdas(
        /** 相关 lambda 声明中提取到的参数类型集合。 */
        val parameterTypesFromDeclarationOfRelatedLambdas: Set<List<CangJieTypeMarker?>>?,
        /** 所有输入来源中的最大参数数量。 */
        val maxParameterCount: Int,
    )

    /**
     * 从相关 lambda 和约束中计算参数类型信息。
     */
    context(c: Context)
    private fun computeParameterInfoFromRelatedLambdas(
        argument: PostponedAtomWithRevisableExpectedType,
        postponedArguments: List<PostponedAtomWithRevisableExpectedType>,
        dependencyProvider: TypeVariableDependencyInformationProvider,
        parameterTypesFromConstraints: Set<List<TypeWithKind>>?,
        parameterTypesFromDeclaration: List<CangJieTypeMarker?>?,
    ): ParameterInfoFromRelatedLambdas = with(resolutionTypeSystemContext) {
        val argumentExpectedTypeConstructor = argument.expectedType?.typeConstructor()

        // 收集与当前 lambda 共享期望类型变量的其他 lambda 的参数声明
        val parameterTypesFromDeclarationOfRelatedLambdas: List<List<CangJieTypeMarker?>> =
            if (argumentExpectedTypeConstructor != null) {
                postponedArguments.mapNotNull { anotherArgument ->
                    if (anotherArgument == argument || anotherArgument !is LambdaWithTypeVariableAsExpectedTypeMarker)
                        return@mapNotNull null

                    val anotherExpectedTypeConstructor =
                        anotherArgument.expectedType?.typeConstructor() ?: return@mapNotNull null
                    if (!dependencyProvider.areVariablesDependentShallowly(
                            argumentExpectedTypeConstructor, anotherExpectedTypeConstructor
                        )
                    ) return@mapNotNull null

                    anotherArgument.parameterTypesFromDeclaration
                }
            } else {
                emptyList()
            }

        val maxParameterCount = maxOf(
            parameterTypesFromConstraints?.maxOfOrNull { it.size } ?: 0,
            parameterTypesFromDeclarationOfRelatedLambdas.maxOfOrNull { it.size } ?: 0,
            parameterTypesFromDeclaration?.size ?: 0,
        )

        return ParameterInfoFromRelatedLambdas(
            parameterTypesFromDeclarationOfRelatedLambdas = parameterTypesFromDeclarationOfRelatedLambdas.toSet(),
            maxParameterCount = maxParameterCount,
        )
    }

    /**
     * 为延迟参数的返回类型创建新的类型变量。
     *
     * - lambda → 创建 lambda 返回类型变量
     * - 可调用引用 → 创建可调用引用返回类型变量
     */
    context(c: Context)
    private fun createTypeVariableForReturnType(
        argument: PostponedAtomWithRevisableExpectedType,
    ): TypeVariableMarker {
        return when (argument) {
            is LambdaWithTypeVariableAsExpectedTypeMarker ->
                resolutionTypeSystemContext.createTypeVariableForLambdaReturnType()
            is PostponedCallableReferenceMarker ->
                resolutionTypeSystemContext.createTypeVariableForCallableReferenceReturnType()
            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }.also { c.getBuilder().registerVariable(it) }
    }

    /**
     * 为延迟参数的第 [index] 个参数创建新的类型变量。
     */
    private fun Context.createTypeVariableForParameterType(
        argument: PostponedAtomWithRevisableExpectedType,
        index: Int,
    ): TypeVariableMarker {
        return when (argument) {
            is LambdaWithTypeVariableAsExpectedTypeMarker ->
                resolutionTypeSystemContext.createTypeVariableForLambdaParameterType(argument, index)
            is PostponedCallableReferenceMarker ->
                resolutionTypeSystemContext.createTypeVariableForCallableReferenceParameterType(argument, index)
            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }.also { getBuilder().registerVariable(it) }
    }

    /**
     * 为参数类型列表中的每个参数创建类型变量，并根据约束方向注册对应约束。
     *
     * 每个参数位置可能来自多个来源（声明、约束、相关 lambda），
     * 同一位置的多个类型按方向（等式/上界/下界）注册为约束，由推断引擎求解。
     *
     * @param parameterTypes 每个参数位置对应的带方向类型列表（外层按来源分组，内层按参数位置）。
     * @return 参数类型变量列表（包装为类型实参）。
     */
    private fun Context.createTypeVariablesForParameters(
        argument: PostponedAtomWithRevisableExpectedType,
        parameterTypes: List<List<TypeWithKind?>>,
    ): List<TypeArgumentMarker> {
        if (parameterTypes.isEmpty()) return emptyList()
        val csBuilder = getBuilder()
        // 将参数类型从"按来源分组"转置为"按参数位置分组"
        val allGroupedParameterTypes = parameterTypes.first().indices.map { i -> parameterTypes.map { it.getOrNull(i) } }

        return allGroupedParameterTypes.mapIndexed { index, types ->
            val parameterTypeVariable = createTypeVariableForParameterType(argument, index)
            val typeVariableConstructor = parameterTypeVariable.freshTypeConstructor()

            for (typeWithKind in types) {
                if (typeVariableConstructor in fixedTypeVariables) break
                if (typeWithKind == null) continue

                when (typeWithKind.direction) {
                    ConstraintKind.EQUALITY -> csBuilder.addEqualityConstraint(
                        parameterTypeVariable.defaultType(), typeWithKind.type,
                        resolutionTypeSystemContext.createArgumentConstraintPosition(argument),
                    )
                    ConstraintKind.UPPER -> csBuilder.addSubtypeConstraint(
                        parameterTypeVariable.defaultType(), typeWithKind.type,
                        resolutionTypeSystemContext.createArgumentConstraintPosition(argument),
                    )
                    ConstraintKind.LOWER -> csBuilder.addSubtypeConstraint(
                        typeWithKind.type, parameterTypeVariable.defaultType(),
                        resolutionTypeSystemContext.createArgumentConstraintPosition(argument),
                    )
                }
            }

            val resultType = fixedTypeVariables[typeVariableConstructor] ?: parameterTypeVariable.defaultType()
            resultType.asTypeArgument()
        }
    }

    /**
     * 计算最终的函数类型构造器。
     *
     * - lambda：直接按参数数量获取函数类型构造器。
     * - 可调用引用：先尝试从约束中解析结果类型，若已是函数类型则直接使用其构造器，
     *   否则按参数数量获取标准函数类型构造器。
     */
    private fun Context.computeResultingFunctionalConstructor(
        argument: PostponedAtomWithRevisableExpectedType,
        parametersNumber: Int,
        resultTypeResolver: ResultTypeResolver,
    ): TypeConstructorMarker {
        val expectedType = argument.expectedType
            ?: throw IllegalStateException("Postponed argument's expected type must not be null")

        val expectedTypeConstructor = expectedType.typeConstructor()

        return when (argument) {
            is LambdaWithTypeVariableAsExpectedTypeMarker ->
                getFunctionTypeConstructor(parametersNumber)
            is PostponedCallableReferenceMarker -> {
                val computedResultType = resultTypeResolver.findResultType(
                    notFixedTypeVariables.getValue(expectedTypeConstructor),
                    TypeVariableDirectionCalculator.ResolveDirection.TO_SUPERTYPE,
                )
                with(resolutionTypeSystemContext) {
                    // 若推断出的结果类型已经是函数类型且有实参，直接复用其构造器
                    if (computedResultType.argumentsCount() > 1) {
                        computedResultType.typeConstructor()
                    } else {
                        getBuiltinFunctionTypeConstructor(parametersNumber)
                    }
                }
            }
            else -> throw IllegalStateException("Unsupported postponed argument type of $argument")
        }
    }

    /**
     * 在给定类型中查找目标类型变量的路径（类型构造器 + 实参位置的序列）。
     *
     * 用于在顶层类型变量的约束中定位期望类型变量的嵌套位置，
     * 以便按路径缓存和查找已构建的函数期望类型。
     *
     * @return 从根类型到目标变量的路径，若未找到则返回 null。
     */
    private fun Context.computeTypeVariablePathInsideGivenType(
        type: CangJieTypeMarker,
        targetVariable: TypeConstructorMarker,
        path: Stack<Pair<TypeConstructorMarker, Int>> = Stack(),
    ): List<Pair<TypeConstructorMarker, Int>>? {
        val typeConstructor = type.typeConstructor()
        if (typeConstructor == targetVariable) return emptyList()

        for (i in 0 until type.argumentsCount()) {
            val argument = type.getArgument(i)
            val argumentType = argument.getType() ?: continue

            if (argumentType.typeConstructor() == targetVariable) {
                return path.toList() + (typeConstructor to i)
            } else if (argumentType.argumentsCount() != 0) {
                path.push(typeConstructor to i)
                computeTypeVariablePathInsideGivenType(argumentType, targetVariable, path)?.let { return it }
                path.pop()
            }
        }
        return null
    }

    /**
     * 从顶层类型变量集合中选出与目标变量深度相关且尚未固定的第一个变量。
     *
     * 用于在约束中定位包含期望类型变量的顶层变量，以便按路径缓存函数期望类型。
     */
    private fun Context.selectFirstRelatedVariable(
        variables: Set<TypeVariableTypeConstructorMarker>,
        targetVariable: TypeConstructorMarker,
        variableDependencyProvider: TypeVariableDependencyInformationProvider,
    ): TypeVariableTypeConstructorMarker? {
        val relatedVariables =
            variableDependencyProvider.getDeeplyDependentVariables(targetVariable).orEmpty() +
                    variableDependencyProvider.getShallowlyDependentVariables(targetVariable)

        return variables.firstOrNull { it in relatedVariables && it in notFixedTypeVariables }
    }

    /**
     * 为延迟参数构建新的函数期望类型。
     *
     * 流程：
     * 1. 检查期望类型变量是否仍未固定。
     * 2. 尝试从缓存中取已构建的函数期望类型（按顶层变量路径或期望类型变量直接索引）。
     * 3. 若无缓存，收集参数类型来源，为每个参数和返回值创建类型变量。
     * 4. 构建新的函数类型，注册子类型约束，并写入缓存。
     *
     * 若所有参数类型已在声明中完全指定且不是函数表达式，则无需构建新期望类型，返回 null。
     */
    private fun Context.buildNewFunctionalExpectedType(
        argument: PostponedAtomWithRevisableExpectedType,
        parameterTypesInfo: ParameterTypesInfo,
        variableDependencyProvider: TypeVariableDependencyInformationProvider,
        topLevelTypeVariables: Set<TypeVariableTypeConstructorMarker>,
    ): CangJieTypeMarker? = with(resolutionTypeSystemContext) {
        val expectedType = argument.expectedType ?: return null
        val expectedTypeConstructor = expectedType.typeConstructor()

        if (expectedTypeConstructor !in notFixedTypeVariables) return null

        // 尝试定位包含当前期望类型变量的顶层变量及其路径，用于缓存复用
        val relatedTopLevelVariable =
            selectFirstRelatedVariable(topLevelTypeVariables, expectedTypeConstructor, variableDependencyProvider)
        val pathFromRelatedTopLevelVariable = if (relatedTopLevelVariable != null) {
            val constraintTypes = notFixedTypeVariables.getValue(relatedTopLevelVariable).constraints.map { it.type }.toSet()
            val containingType = constraintTypes.find { constraintType ->
                constraintType.contains { it.typeConstructor() == expectedTypeConstructor }
            }
            containingType?.let { computeTypeVariablePathInsideGivenType(it, expectedTypeConstructor) }
        } else null

        // 命中缓存则直接返回
        if (pathFromRelatedTopLevelVariable != null && relatedTopLevelVariable != null) {
            getBuilder().getBuiltFunctionalExpectedTypeForPostponedArgument(
                relatedTopLevelVariable, pathFromRelatedTopLevelVariable
            )?.let { return it }
        } else {
            getBuilder().getBuiltFunctionalExpectedTypeForPostponedArgument(expectedTypeConstructor)
                ?.let { return it }
        }

        val parametersFromConstraints = parameterTypesInfo.parametersFromConstraints
        val parametersFromDeclaration = parameterTypesInfo.parametersFromDeclaration
        val parametersFromDeclarations =
            parameterTypesInfo.parametersFromDeclarationOfRelatedLambdas.orEmpty() + parametersFromDeclaration

        val areAllParameterTypesSpecified =
            !parametersFromDeclaration.isNullOrEmpty() && parametersFromDeclaration.all { it != null }

        // 所有参数类型已完全指定且不是函数表达式，无需构建新期望类型
        if (areAllParameterTypesSpecified && !argument.isFunctionExpression()) {
            return null
        }

        val allParameterTypes =
            (parametersFromConstraints.orEmpty() + parametersFromDeclarations.map { parameters ->
                parameters?.map { it.wrapToTypeWithKind() }
            }).filterNotNull()

        if (allParameterTypes.isEmpty()) return null

        val variablesForParameterTypes = createTypeVariablesForParameters(argument, allParameterTypes)
        val variableForReturnType = createTypeVariableForReturnType(argument)
        val functionalConstructor = computeResultingFunctionalConstructor(
            argument, variablesForParameterTypes.size, resultTypeResolver,
        )

        // 构建新的函数类型：(参数类型变量...) -> 返回类型变量
        val newExpectedType = createSimpleType(
            functionalConstructor,
            variablesForParameterTypes + variableForReturnType.defaultType().asTypeArgument(),
            attributes = parameterTypesInfo.annotations,
        )

        // 注册新函数类型与原期望类型的子类型约束
        getBuilder().addSubtypeConstraint(
            newExpectedType,
            expectedType,
            createArgumentConstraintPosition(argument),
        )

        // 写入缓存
        if (pathFromRelatedTopLevelVariable != null && relatedTopLevelVariable != null) {
            getBuilder().putBuiltFunctionalExpectedTypeForPostponedArgument(
                relatedTopLevelVariable, pathFromRelatedTopLevelVariable, newExpectedType,
            )
        } else {
            getBuilder().putBuiltFunctionalExpectedTypeForPostponedArgument(expectedTypeConstructor, newExpectedType)
        }

        return newExpectedType
    }

    /**
     * 遍历所有延迟参数，收集参数类型并构建新的函数期望类型。
     *
     * 在部分完成模式（PARTIAL）下，只处理函数表达式（参数类型完全显式声明），
     * 跳过 lambda（可能在完全模式下出现更精确的函数类型约束）。
     *
     * @return 若有任何延迟参数的期望类型被更新，则返回 true。
     */
    context(c: Context)
    fun collectParameterTypesAndBuildNewExpectedTypes(
        postponedArguments: List<PostponedAtomWithRevisableExpectedType>,
        completionMode: ConstraintSystemCompletionMode,
        dependencyProvider: TypeVariableDependencyInformationProvider,
        topLevelTypeVariables: Set<TypeVariableTypeConstructorMarker>,
    ): Boolean = with(resolutionTypeSystemContext) {
        // 从声明中提取参数类型（在任何模式下都可以，声明不会因完成进度改变）
        for (argument in postponedArguments) {
            if (argument !is LambdaWithTypeVariableAsExpectedTypeMarker) continue
            if (argument.parameterTypesFromDeclaration != null) continue
            argument.updateParameterTypesFromDeclaration(extractLambdaParameterTypesFromDeclaration(argument))
        }

        return postponedArguments.any { argument ->
            // 部分完成模式下只处理函数表达式，lambda 推迟到完全模式
            if (completionMode == ConstraintSystemCompletionMode.PARTIAL && !argument.isFunctionExpression())
                return@any false
            // 已有修订期望类型则跳过
            if (argument.revisedExpectedType != null) return@any false

            val parameterTypesInfo =
                extractParameterTypesInfo(argument, postponedArguments, dependencyProvider) ?: return@any false
            val newExpectedType =
                c.buildNewFunctionalExpectedType(argument, parameterTypesInfo, dependencyProvider, topLevelTypeVariables)
                    ?: return@any false

            argument.reviseExpectedType(newExpectedType)
            true
        }
    }

    /**
     * 深度收集类型中所有相关的类型变量（包括通过依赖关系间接关联的变量）。
     */
    context(c: Context)
    private fun CangJieTypeMarker.getAllDeeplyRelatedTypeVariables(
        variableDependencyProvider: TypeVariableDependencyInformationProvider,
    ): Collection<TypeVariableTypeConstructorMarker> {
        val collectedVariables = mutableSetOf<TypeVariableTypeConstructorMarker>()
        getAllDeeplyRelatedTypeVariables(variableDependencyProvider, collectedVariables)
        return collectedVariables
    }

    /**
     * 递归收集类型中所有深度相关类型变量。
     */
    context(c: Context)
    private fun CangJieTypeMarker.getAllDeeplyRelatedTypeVariables(
        variableDependencyProvider: TypeVariableDependencyInformationProvider,
        typeVariableCollector: MutableSet<TypeVariableTypeConstructorMarker>,
    ) {
        val typeConstructor = typeConstructor()
        when {
            typeConstructor is TypeVariableTypeConstructorMarker -> {
                // 当前类型本身是类型变量，收集其所有深度依赖变量
                val relatedVariables =
                    variableDependencyProvider.getDeeplyDependentVariables(typeConstructor).orEmpty()
                typeVariableCollector.add(typeConstructor)
                typeVariableCollector.addAll(relatedVariables.filterIsInstance<TypeVariableTypeConstructorMarker>())
            }
            argumentsCount() > 0 -> {
                // 递归处理所有类型实参
                for (index in 0 until argumentsCount()) {
                    val argumentType = getArgument(index).getType() ?: continue
                    argumentType.getAllDeeplyRelatedTypeVariables(variableDependencyProvider, typeVariableCollector)
                }
            }
        }
    }

    /**
     * 若延迟参数的下一个就绪参数类型变量存在，则将其固定。
     *
     * @return 是否成功固定了一个变量。
     */
    context(c: Context)
    fun fixNextReadyVariableForParameterTypeIfNeeded(
        argument: PostponedResolvedAtomMarker,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        topLevelType: CangJieTypeMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
        resolvedAtomProvider: ResolvedAtomProvider,
    ): Boolean {
        return fixNextReadyVariableForParameterType(
            resolvedAtomProvider,
            findNextReadyVariableForParameterType(argument, postponedArguments, topLevelType, dependencyProvider),
        )
    }

    /**
     * 查找延迟参数中下一个可以固定的参数类型变量。
     *
     * 从延迟参数的函数期望类型中提取参数类型，
     * 找出其中已就绪（约束充分）的类型变量作为固定候选。
     */
    context(c: Context)
    fun findNextReadyVariableForParameterType(
        argument: PostponedResolvedAtomMarker,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        topLevelType: CangJieTypeMarker,
        dependencyProvider: TypeVariableDependencyInformationProvider,
    ): VariableFixationFinder.VariableForFixation? {
        val expectedType = argument.expectedFunctionType() ?: return null

        if (c.lexicographicVariableReadinessCalculation) {
            if (argument is LambdaWithTypeVariableAsExpectedTypeMarker) return null
            if (argument is PostponedCallableReferenceMarker && !argument.needsResolution) return null
        }

        return findNextVariableForParameterType(
            if (!c.lexicographicVariableReadinessCalculation)
                expectedType.extractCallableArgumentTypes()
            else
                argument.inputTypes,
            dependencyProvider, postponedArguments, topLevelType,
        )
    }

    /**
     * 获取延迟参数的函数期望类型。
     *
     * 优先使用修订后的期望类型（revisedExpectedType），
     * 回退到原始期望类型，若不是函数类型则返回 null。
     */
    context(c: Context)
    private fun PostponedResolvedAtomMarker.expectedFunctionType(): CangJieTypeMarker? {
        val expectedType =
            (this as? PostponedAtomWithRevisableExpectedType)?.revisedExpectedType ?: expectedType
            ?: return null
        // 仓颉函数类型通过参数数量判断（有实参则为函数类型）
        return if (expectedType.argumentsCount() > 0) expectedType else null
    }

    /**
     * 固定就绪的参数类型变量。
     *
     * 使用结果类型解析器确定固定值，方向为 UNKNOWN（由约束决定）。
     *
     * @return 是否成功固定了变量（变量存在且已就绪）。
     */
    context(c: Context)
    private fun fixNextReadyVariableForParameterType(
        resolvedAtomByTypeVariableProvider: ResolvedAtomProvider,
        variableForFixation: VariableFixationFinder.VariableForFixation?,
    ): Boolean = with(resolutionTypeSystemContext) {
        if (variableForFixation == null || !variableForFixation.isReady) return false

        val variableWithConstraints = c.notFixedTypeVariables.getValue(variableForFixation.variable)
        val resultType = resultTypeResolver.findResultType(
            variableWithConstraints,
            TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN,
        )
        val variable = variableWithConstraints.typeVariable

        c.fixVariable(
            variable,
            resultType,
            createFixVariableConstraintPosition(variable, resolvedAtomByTypeVariableProvider(variable)),
        )
        return true
    }

    /**
     * 从参数类型集合中找到下一个可固定的类型变量。
     *
     * 过滤掉外部类型变量（outerTypeVariables），只考虑当前推断范围内的变量。
     */
    context(c: Context)
    private fun findNextVariableForParameterType(
        types: Collection<CangJieTypeMarker>,
        dependencyProvider: TypeVariableDependencyInformationProvider,
        postponedArguments: List<PostponedResolvedAtomMarker>,
        topLevelType: CangJieTypeMarker,
    ): VariableFixationFinder.VariableForFixation? {
        val outerTypeVariables = c.outerTypeVariables.orEmpty()
        val relatedVariables = types
            .flatMap { it.getAllDeeplyRelatedTypeVariables(dependencyProvider) }
            .filter { it !in outerTypeVariables }

        return variableFixationFinder.findFirstVariableForFixation(
            relatedVariables,
            postponedArguments,
            ConstraintSystemCompletionMode.FULL,
            topLevelType,
        )
    }

    /** 将可空类型包装为 [TypeWithKind]，null 时返回 null。 */
    private fun CangJieTypeMarker?.wrapToTypeWithKind() = this?.let { TypeWithKind(it) }

    companion object {
        /** lambda 参数类型变量的名称前缀。 */
        const val TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE = "_RP"
        /** lambda 返回类型变量的名称。 */
        const val TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE = "_R"
        /** 可调用引用参数类型变量的名称前缀。 */
        const val TYPE_VARIABLE_NAME_PREFIX_FOR_CR_PARAMETER_TYPE = "_QP"
        /** 可调用引用返回类型变量的名称。 */
        const val TYPE_VARIABLE_NAME_FOR_CR_RETURN_TYPE = "_Q"
    }
}
