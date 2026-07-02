package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.isLambdaParameterTypeOmitted
import org.cangnova.cangjie.cfir.declarations.lambdaParameterShapeExpectedFunctionType
import org.cangnova.cangjie.cfir.diagnostic.AmbiguousArgumentType
import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.diagnostic.InapplicableWrongReceiver
import org.cangnova.cangjie.cfir.diagnostic.LambdaParameterCountMismatch
import org.cangnova.cangjie.cfir.diagnostic.LambdaParameterTypeMismatch
import org.cangnova.cangjie.cfir.diagnostic.UnsuccessfulCallableReferenceArgument
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirTupleLiteral
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.calls.*
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.addSubsystemFromAtom
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeExplicitTypeParameterConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeReceiverConstraintPosition
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeRegularLambdaArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaParameterType
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaReturnType
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInArguments
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConeTypeIntersector
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.types.arrayLiteralElementType
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.expandedClassIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.addEqualityConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.isSubtypeConstraintCompatible
import org.cangnova.cangjie.resolve.calls.inference.runTransaction
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE
import org.cangnova.cangjie.resolve.calls.inference.model.ArgumentConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 实参检查处理器。
 *
 * 该对象负责把实参表达式类型与候选参数期望类型写入约束系统，
 * 并为 lambda、函数引用、数组字面量等需要延迟处理的实参创建 postponed atom。
 */
internal object ArgumentCheckingProcessor {
    /**
     * 单个实参检查过程需要的上下文。
     */
    private data class ArgumentContext(
        /**
         * 当前正在检查的调用候选。
         */
        val candidate: Candidate,
        /**
         * 当前候选的约束系统 builder。
         */
        val csBuilder: ConstraintSystemBuilder,
        /**
         * 当前实参位置上的期望类型。
         */
        val expectedType: ConeCangJieType?,
        /**
         * 诊断 sink；completion 阶段仅构造 atom 时可为空。
         */
        val sink: CheckerSink?,
        /**
         * 当前调用解析上下文。
         */
        val context: ResolutionContext,
        /**
         * 当前实参是否作为 receiver 检查。
         */
        val isReceiver: Boolean,
        /**
         * 当前 receiver 是否为 dispatch receiver。
         */
        val isDispatch: Boolean,
        /**
         * 当该实参来自 lambda 返回表达式时，对应的匿名函数。
         */
        val anonymousFunctionIfReturnExpression: CfirAnonymousFunction? = null,
    ) : SessionHolder {
        /**
         * 当前上下文所属会话。
         */
        override val session: CfirSession
            get() = context.session

        /**
         * 向 sink 报告诊断；sink 为空时忽略。
         */
        fun reportDiagnostic(diagnostic: ResolutionDiagnostic) {
            sink?.reportDiagnostic(diagnostic)
        }
    }

    /**
     * 解析并检查一个表达式实参。
     */
    fun resolveArgumentExpression(
        candidate: Candidate,
        atom: ConeResolutionAtom,
        expectedType: ConeCangJieType?,
        sink: CheckerSink,
        context: ResolutionContext,
        isReceiver: Boolean,
        isDispatch: Boolean,
        anonymousFunctionIfReturnExpression: CfirAnonymousFunction? = null,
    ) {
        ArgumentContext(
            candidate = candidate,
            csBuilder = candidate.system.getBuilder(),
            expectedType = expectedType,
            sink = sink,
            context = context,
            isReceiver = isReceiver,
            isDispatch = isDispatch,
            anonymousFunctionIfReturnExpression = anonymousFunctionIfReturnExpression,
        ).resolveArgumentExpression(atom)
    }

    /**
     * 直接使用已知实参类型进行适用性检查。
     */
    fun resolvePlainArgumentType(
        candidate: Candidate,
        atom: ConeResolutionAtom,
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType?,
        sink: CheckerSink,
        context: ResolutionContext,
        isReceiver: Boolean,
        isDispatch: Boolean,
        sourceForReceiver: CjSourceElement? = null,
    ) {
        ArgumentContext(
            candidate = candidate,
            csBuilder = candidate.system.getBuilder(),
            expectedType = expectedType,
            sink = sink,
            context = context,
            isReceiver = isReceiver,
            isDispatch = isDispatch,
        ).resolvePlainArgumentType(atom, argumentType, sourceForReceiver)
    }

    /**
     * completion 阶段按修订后的期望类型创建已解析 lambda atom。
     */
    fun createResolvedLambdaAtomDuringCompletion(
        candidate: Candidate,
        csBuilder: ConstraintSystemBuilder,
        atom: ConeResolutionAtomWithPostponedChild,
        expectedType: ConeCangJieType?,
        context: ResolutionContext,
        returnTypeVariable: ConeTypeVariableForLambdaReturnType?,
        anonymousFunctionIfReturnExpression: CfirAnonymousFunction? = null,
    ): ConeResolvedLambdaAtom {
        return ArgumentContext(
            candidate = candidate,
            csBuilder = csBuilder,
            expectedType = expectedType,
            sink = null,
            context = context,
            isReceiver = false,
            isDispatch = false,
            anonymousFunctionIfReturnExpression = anonymousFunctionIfReturnExpression,
        ).createResolvedLambdaAtom(atom, duringCompletion = true, returnTypeVariable = returnTypeVariable)
    }

    /**
     * 按 atom 类型分派实参解析。
     */
    private fun ArgumentContext.resolveArgumentExpression(atom: ConeResolutionAtom) {
        when (atom) {
            is ConeResolutionAtomWithPostponedChild -> when (atom.expression) {
                is CfirAnonymousFunctionExpression -> preprocessLambdaArgument(atom)
                is CfirNamedAccessExpression -> preprocessFunctionReferenceArgument(atom, atom.expression)
                else -> {
                    atom.useFallbackSubAtom()
                    val child = atom.subAtom
                    if (child != null) {
                        resolveArgumentExpression(child)
                    } else {
                        resolvePlainExpressionArgument(atom)
                    }
                }
            }

            is ConeResolutionAtomWithSingleChild -> {
                val child = atom.subAtom
                if (child != null) {
                    resolveArgumentExpression(child)
                } else {
                    resolvePlainExpressionArgument(atom)
                }
            }

            is ConeSimpleLeafResolutionAtom,
            is ConeAtomWithCandidate -> resolvePlainExpressionArgument(atom)

            is ConePostponedResolvedAtom -> Unit
        }
    }

    /**
     * 在期望类型已知时把函数引用实参转为 postponed callable reference atom。
     */
    private fun ArgumentContext.preprocessFunctionReferenceArgument(
        atom: ConeResolutionAtomWithPostponedChild,
        expression: CfirNamedAccessExpression,
    ) {
        val fallback = atom.fallbackSubAtom ?: run {
            atom.useFallbackSubAtom()
            resolvePlainExpressionArgument(atom)
            return
        }
        val targetExpectedType = expectedType
        if (targetExpectedType == null) {
            atom.useFallbackSubAtom()
            resolveArgumentExpression(fallback)
            return
        }

        val postponedAtom = ConeResolvedCallableReferenceAtom(
            expression = expression,
            expectedType = targetExpectedType,
        )
        atom.setPostponedSubAtom(postponedAtom)
        candidate.addPostponedAtom(postponedAtom)
    }

    /**
     * 解析普通表达式实参类型。
     */
    private fun ArgumentContext.resolvePlainExpressionArgument(atom: ConeResolutionAtom) {
        val targetTypedEnumType = expectedType?.let { atom.expression.applyNoArgEnumConstructorTargetType(it, session) }
        val argumentType = targetTypedEnumType
            ?: arrayLiteralTypeFromExpectedType(atom.expression)
            ?: tupleLiteralTypeFromExpectedType(atom.expression)
            ?: atom.expression.coneTypeOrNull
            ?: (atom as? ConeAtomWithCandidate)?.candidate?.substitutedReturnType()
            ?: return
        resolvePlainArgumentType(atom, argumentType)
    }

    /**
     * 当期望类型可确定数组字面量元素类型时，为数组字面量补齐整体类型。
     */
    private fun ArgumentContext.arrayLiteralTypeFromExpectedType(expression: CfirExpression): ConeCangJieType? {
        val arrayLiteral = expression as? CfirArrayLiteral ?: return null
        val expandedExpectedType = expectedType?.fullyExpandedType(session) ?: return null
        val expectedElementType = expandedExpectedType.arrayLiteralElementType ?: return null
        if (expandedExpectedType is ConeVArrayType && expandedExpectedType.size != arrayLiteral.elements.size.toLong()) {
            return null
        }

        val elementsCompatible = arrayLiteral.elements.all { element ->
            val elementType = element.coneTypeOrNull?.let { IdealTypeResolver.resolveIfIdeal(it, expectedElementType) }
                ?: return@all false
            elementType is ConeErrorType ||
                    expectedElementType is ConeErrorType ||
                    AbstractTypeChecker.equalTypes(session.typeContext, elementType, expectedElementType) ||
                    AbstractTypeChecker.isSubtypeOf(session.typeContext, elementType, expectedElementType)
        }
        if (!elementsCompatible) return null

        arrayLiteral.replaceConeTypeOrNull(expandedExpectedType)
        return expandedExpectedType
    }

    /**
     * 当期望类型是 tuple 时，允许元素级 expected type 修复 `None` 等需要上下文的子表达式。
     */
    private fun ArgumentContext.tupleLiteralTypeFromExpectedType(expression: CfirExpression): ConeCangJieType? {
        val tupleLiteral = expression as? CfirTupleLiteral ?: return null
        val expectedTupleType = expectedType?.fullyExpandedType(session) as? ConeTupleType ?: return null
        if (expectedTupleType.elementTypes.size != tupleLiteral.elements.size) return null

        val elementsCompatible = tupleLiteral.elements.zip(expectedTupleType.elementTypes).all { (element, expectedElementType) ->
            val elementType = typeForExpectedTupleElement(element, expectedElementType)
                ?: return@all false
            elementType is ConeErrorType ||
                    expectedElementType is ConeErrorType ||
                    AbstractTypeChecker.equalTypes(session.typeContext, elementType, expectedElementType) ||
                    AbstractTypeChecker.isSubtypeOf(session.typeContext, elementType, expectedElementType)
        }
        if (!elementsCompatible) return null

        tupleLiteral.replaceConeTypeOrNull(expectedTupleType)
        return expectedTupleType
    }

    /**
     * tuple 参数的元素级目标类型传播。
     *
     * 这里复用无参 enum constructor 的目标类型语义，使 `None` 在参数 tuple 内也能从
     * 形参的 `Option<T>` 元素类型定型，而不是保留无上下文解析阶段的泛型推断错误。
     */
    private fun ArgumentContext.typeForExpectedTupleElement(
        element: CfirExpression,
        expectedElementType: ConeCangJieType,
    ): ConeCangJieType? {
        if (element is CfirWrappedExpression) {
            val innerType = typeForExpectedTupleElement(element.expression, expectedElementType)
            if (innerType != null) {
                element.replaceConeTypeOrNull(innerType)
                return innerType
            }
        }

        val directType = element.coneTypeOrNull?.let { IdealTypeResolver.resolveIfIdeal(it, expectedElementType) }
        val targetTypedEnumType = element.applyNoArgEnumConstructorTargetType(expectedElementType, session)
        return targetTypedEnumType ?: directType
    }

    /**
     * 创建实参约束位置；lambda 返回表达式使用专门的位置对象。
     */
    private fun  ArgumentContext.createArgumentConstraintPosition(atom: ConeResolutionAtom): ArgumentConstraintPosition<*> {
        return when (val containingLambda = anonymousFunctionIfReturnExpression) {
            null -> ConeArgumentConstraintPosition(atom.expression)
            else -> ConeRegularLambdaArgumentConstraintPosition(containingLambda, atom.expression)
        }
    }

    /**
     * 将普通实参类型写入 subtype 约束并报告适用性错误。
     */
    private fun ArgumentContext.resolvePlainArgumentType(
        atom: ConeResolutionAtom,
        argumentType: ConeCangJieType,
        sourceForReceiver: CjSourceElement? = null,
    ) {
        val expression = atom.expression
        val position = when {
            isReceiver -> ConeReceiverConstraintPosition(expression, sourceForReceiver)
            else -> createArgumentConstraintPosition(atom)
        }
        val preparedType = prepareArgumentType(argumentType, context.session)
        checkApplicabilityForArgumentType(atom, preparedType, position)
    }

    /**
     * 检查实参类型是否适用于期望类型。
     */
    private fun ArgumentContext.checkApplicabilityForArgumentType(
        atom: ConeResolutionAtom,
        argumentTypeBeforeCapturing: ConeCangJieType,
        position: ConstraintPosition,
    ) {
        if (expectedType == null) return

        if (atom is ConeAtomWithCandidate) {
            candidate.system.addSubsystemFromAtom(atom)
        }

        val argumentTypeAfterCurrentSubstitution = csBuilder.buildCurrentSubstitutor()
            .asCone()
            .substituteOrNull(argumentTypeBeforeCapturing)
            ?: argumentTypeBeforeCapturing
        val argumentType = when {
            /*
             * 当前候选系统里的 fresh type variable 必须继续作为约束目标。
             * 若在这里把 `id<T>(1)` 的返回 `T` 还原成声明类型参数或上界，外层
             * `foo(Int64)` 就无法把 expected type 反向写入内层调用系统。
             */
            isCurrentInferenceVariableType(argumentTypeAfterCurrentSubstitution) -> argumentTypeAfterCurrentSubstitution
            else -> substituteTypeParameterUpperBoundIfNeeded(argumentTypeAfterCurrentSubstitution, expectedType, session)
        }
        val expression = atom.expression
        fun subtypeError(actualExpectedType: ConeCangJieType): ResolutionDiagnostic {
            fun tryGetConeTypeThatCompatibleWithKtType(type: ConeCangJieType): ConeCangJieType {
                if (type is ConeTypeVariableType) {
                    val lookupTag = type.typeConstructor

                    val constraints = csBuilder.currentStorage().notFixedTypeVariables[lookupTag]?.constraints
                    val constraintTypes = constraints?.mapNotNull { it.type as? ConeCangJieType }
                    if (!constraintTypes.isNullOrEmpty()) {
                        return ConeTypeIntersector.intersectTypes(session.typeContext, constraintTypes)
                    }

                    val originalTypeParameter = lookupTag.originalTypeParameter as? ConeTypeParameterLookupTag
                    if (originalTypeParameter != null) {
                        return ConeTypeParameterTypeImpl(originalTypeParameter , type.attributes)
                    }
                } else if (type is ConeIdealLiteralType) {
                    return type.defaultType
                }

                return type
            }

            if (argumentType is ConeErrorType || actualExpectedType is ConeErrorType) return ErrorTypeInArguments

            val preparedExpectedType = tryGetConeTypeThatCompatibleWithKtType(actualExpectedType)
            val preparedActualType = tryGetConeTypeThatCompatibleWithKtType(argumentType)
            return ArgumentTypeMismatch(
                preparedExpectedType,
                preparedActualType,
                expression,
                false,
                anonymousFunctionIfReturnExpression,
                csBuilder.hasContradiction,
            )
        }

        val sameClassifierConstraintsAdded = addSameClassifierTypeArgumentConstraints(argumentType, expectedType, position)
        if (sameClassifierConstraintsAdded) return
        if (candidate.isEnumConstructorPayloadInference() &&
            addIdealLiteralEqualityConstraintForCurrentInferenceVariable(argumentType, expectedType, position)
        ) return
        if (addLocalLambdaParameterShapeConstraint(argumentType, expectedType, position)) return
        if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) return
        if (addIdealLiteralConstraintForCurrentInferenceVariable(argumentType, expectedType, position)) return
        if (argumentType.isIdealTypeForTypeParameterExpectedType(expectedType)) return
        if (isReceiver) {
            csBuilder.addSubtypeConstraint(argumentType, expectedType, position)
            reportDiagnostic(InapplicableWrongReceiver(expectedType, argumentType))
            return
        }
        if (expression.isFunctionDeclarationReferenceArgument()) {
            val expandedArgumentType = argumentType.fullyExpandedType(session)
            val expandedExpectedType = expectedType.fullyExpandedType(session)
            if (expandedArgumentType is ConeFunctionType && expandedExpectedType is ConeFunctionType) {
                reportDiagnostic(UnsuccessfulCallableReferenceArgument(expression))
                return
            }
        }
        reportDiagnostic(subtypeError(expectedType))
    }

    /**
     * 同构泛型实参约束下沉。
     *
     * 官方 LocalTypeArgumentSynthesis 会从 `Array<Int64>` 对 `Array<T>` 这类同构类型中继续
     * 提取元素级约束，用于完成函数/owner 泛型参数。底层 subtype 检查只回答整体是否可适用，
     * 不保证把每个类型实参都登记到当前候选约束系统，因此这里在实参检查共享层补齐该输入。
     */
    private fun ArgumentContext.addSameClassifierTypeArgumentConstraints(
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType,
        position: ConstraintPosition,
    ): Boolean {
        var hasAcceptedConstraint = false
        return csBuilder.runTransaction {
            hasAcceptedConstraint = addSameClassifierTypeArgumentConstraintsInTransaction(
                argumentType,
                expectedType,
                position,
            )
            hasAcceptedConstraint && !hasContradiction
        } && hasAcceptedConstraint
    }

    /**
     * 在同一个事务中递归分解 invariant 类型实参。
     *
     * 若分解过程中发现固定类型实参不相等或新增约束不兼容，外层事务会回滚本次
     * 同构下沉留下的全部部分约束，再交给普通 subtype 路径产出诊断。
     */
    private fun ArgumentContext.addSameClassifierTypeArgumentConstraintsInTransaction(
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType,
        position: ConstraintPosition,
    ): Boolean {
        val actualClassifier = argumentType.fullyExpandedType(session) as? ConeLookupTagBasedType ?: return false
        val expectedClassifier = expectedType.fullyExpandedType(session) as? ConeLookupTagBasedType ?: return false
        if (actualClassifier.expandedClassIdOrPrimitiveClassId != expectedClassifier.expandedClassIdOrPrimitiveClassId) {
            return false
        }
        if (actualClassifier.typeArguments.size != expectedClassifier.typeArguments.size) return false

        var hasAcceptedConstraint = false
        for ((actualArgument, expectedArgument) in actualClassifier.typeArguments.zip(expectedClassifier.typeArguments)) {
            val actualArgumentType = actualArgument.type
            val expectedArgumentType = expectedArgument.type
            if (!typeContainsCurrentInferenceVariable(expectedArgumentType)) {
                if (!isInvariantArgumentCompatible(actualArgumentType, expectedArgumentType, session)) return false
                continue
            }

            val decomposedNested = addSameClassifierTypeArgumentConstraintsInTransaction(
                actualArgumentType,
                expectedArgumentType,
                position,
            )
            if (decomposedNested) {
                hasAcceptedConstraint = true
                continue
            }

            val (currentVariableType, otherType) = when {
                isCurrentInferenceVariableType(actualArgumentType) -> actualArgumentType to expectedArgumentType
                isCurrentInferenceVariableType(expectedArgumentType) -> expectedArgumentType to actualArgumentType
                else -> return false
            }
            if (!csBuilder.addEqualityConstraintIfCompatible(currentVariableType, otherType, position)) {
                return false
            }
            hasAcceptedConstraint = true
        }
        return hasAcceptedConstraint
    }

    /**
     * 仓颉普通泛型实参是不变的；固定类型实参必须语义相等。
     */
    private fun isInvariantArgumentCompatible(
        actualType: ConeCangJieType,
        expectedType: ConeCangJieType,
        session: CfirSession,
    ): Boolean {
        if (actualType is ConeErrorType || expectedType is ConeErrorType) return true
        return AbstractTypeChecker.equalTypes(session.typeContext, actualType, expectedType)
    }

    /**
     * 当前类型树中是否含本候选约束系统登记过的 fresh type variable。
     *
     * 成员签名可能保留外层 lambda placeholder 或声明类型参数对应的 `ConeTypeVariableType`。
     * 这些变量不属于当前候选系统，不能作为同构类型实参下沉的目标，否则约束注入器会把
     * 外来 constructor 当作本系统变量处理。
     */
    private fun ArgumentContext.typeContainsCurrentInferenceVariable(type: ConeCangJieType): Boolean = when (type) {
        is ConeTypeVariableType -> type.typeConstructor in csBuilder.currentStorage().allTypeVariables
        is ConeLookupTagBasedType -> type.typeArguments.any { typeContainsCurrentInferenceVariable(it.type) }
        is ConeFunctionType -> type.parameterTypes.any { typeContainsCurrentInferenceVariable(it) } ||
                typeContainsCurrentInferenceVariable(type.returnType)
        is ConeTupleType -> type.elementTypes.any { typeContainsCurrentInferenceVariable(it) }
        is ConeVArrayType -> typeContainsCurrentInferenceVariable(type.elementType)
        else -> false
    }

    /** 判断类型根节点是否是当前候选约束系统登记过的 fresh type variable。 */
    private fun ArgumentContext.isCurrentInferenceVariableType(type: ConeCangJieType): Boolean =
        type is ConeTypeVariableType && type.typeConstructor in csBuilder.currentStorage().allTypeVariables

    /**
     * enum constructor payload 中的理想字面量要以 ILT equality 进入推断。
     *
     * `Some(1)` 的 owner 类型实参随后可能由外层 `Option<Int32>` 或函数值调用继续约束；
     * 因此这里保留 `IdealInt` 本身，而不是提前落成默认 `Int64`。
     */
    private fun ArgumentContext.addIdealLiteralEqualityConstraintForCurrentInferenceVariable(
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType,
        position: ConstraintPosition,
    ): Boolean {
        if (!isCurrentInferenceVariableType(expectedType)) return false
        if (!argumentType.isIdealLiteralOrPrimitive()) return false
        return csBuilder.addEqualityConstraintIfCompatible(expectedType, argumentType, position)
    }

    /**
     * 无上下文局部 lambda 的参数 placeholder 在函数值调用处需要承接实参形状。
     *
     * 对 `f(Some(1))` 这类调用，形参 `_RP0` 不能只得到一个整体 subtype 探测结果；
     * 它必须保留 `Option<T>` 这类 nominal 结构，后续 completion 才能在 `T` 固定后
     * 把最终参数类型写回 lambda header。
     */
    private fun ArgumentContext.addLocalLambdaParameterShapeConstraint(
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType,
        position: ConstraintPosition,
    ): Boolean {
        val expectedVariableType = expectedType as? ConeTypeVariableType ?: return false
        val expectedVariable = csBuilder.currentStorage()
            .allTypeVariables[expectedVariableType.typeConstructor]
        if (expectedVariable !is ConeTypeVariableForLambdaParameterType) return false
        if (!argumentType.hasCallableValueArgumentShape()) return false
        return csBuilder.addEqualityConstraintIfCompatible(expectedType, argumentType, position)
    }

    /** 是否是理想字面量或 primitive 形式的 IdealInt/IdealFloat。 */
    private fun ConeCangJieType.isIdealLiteralOrPrimitive(): Boolean =
        this is ConeIdealLiteralType || this is ConePrimitiveType && kind.isIdeal

    /** 函数值调用实参中可写回 lambda 参数 placeholder 的稳定类型形状。 */
    private fun ConeCangJieType.hasCallableValueArgumentShape(): Boolean = when (this) {
        is ConeLookupTagBasedType,
        is ConeFunctionType,
        is ConeTupleType,
        is ConeVArrayType,
        -> true

        else -> false
    }

    /** 当前候选是否是可从 payload 参与 owner 泛型推断的 enum constructor。 */
    private fun Candidate.isEnumConstructorPayloadInference(): Boolean {
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return false
        return enumConstructor.valueParameters.isNotEmpty() && !callInfo.hasExplicitTypeArguments
    }

    /**
     * 函数声明名在函数类型上下文中作为引用实参时，签名不适用应归入 callable-reference 诊断。
     */
    private fun CfirExpression.isFunctionDeclarationReferenceArgument(): Boolean {
        if (this is CfirFunctionCall) return false
        val access = this as? CfirNamedAccessExpression ?: return false
        val symbol = when (val reference = access.calleeReference) {
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            else -> null
        }
        return symbol?.takeIf { it.isBound }?.cfir is CfirFunction
    }

    /**
     * 官方 LocalTypeArgumentSynthesis 会让 IdealInt/IdealFloat 先参与泛型参数推断，
     * 再由后续目标类型或其他约束把 ideal 类型收束成具体原始类型。
     * 因此实参检查不能在 `IdealInt <: T` 这类声明类型参数位置提前报参数不匹配。
     */
    private fun ConeCangJieType.isIdealTypeForTypeParameterExpectedType(
        expectedType: ConeCangJieType,
    ): Boolean {
        if (expectedType !is ConeTypeParameterType) return false
        return this.isIdealLiteralOrPrimitive()
    }

    /**
     * 局部无上下文 lambda initializer 的函数值调用中，形参 expected type 可能是当前
     * 候选系统导入的 placeholder type variable。官方 ideal literal 仍应作为推断输入，
     * 因此在底层 subtype 探测不能直接接受 `IdealInt <: α` 时，使用其默认 primitive
     * 类型继续约束该 placeholder。
     */
    private fun ArgumentContext.addIdealLiteralConstraintForCurrentInferenceVariable(
        argumentType: ConeCangJieType,
        expectedType: ConeCangJieType,
        position: ConstraintPosition,
    ): Boolean {
        if (!isCurrentInferenceVariableType(expectedType)) return false
        if (argumentType !is ConeIdealLiteralType && (argumentType !is ConePrimitiveType || !argumentType.kind.isIdeal)) {
            return false
        }
        val approximatedArgumentType = IdealTypeResolver.resolveIfIdeal(argumentType)
        return csBuilder.addSubtypeConstraintIfCompatible(approximatedArgumentType, expectedType, position)
    }

    /**
     * 判断当前实参检查是否应运行转换流程。
     */
    private fun  ArgumentContext.shouldRunConversion(): Boolean {
        // Currently, we only apply conversions for arguments, not lambda's return expressions
//        if (anonymousFunctionIfReturnExpression != null) {
//            // For latest LV it's equal to `return false`
//            return !LanguageFeature.DoNotRunSuspendConversionForLambdaReturnStatements.isEnabled()
//        }
        return true
    }

    /**
     * 预处理 lambda 实参，必要时创建 postponed lambda atom。
     */
    private fun ArgumentContext.preprocessLambdaArgument(atom: ConeResolutionAtomWithPostponedChild) {
        if (createLambdaWithTypeVariableAsExpectedTypeAtomIfNeeded(atom)) return
        createResolvedLambdaAtom(atom, duringCompletion = false, returnTypeVariable = null)
    }

    /**
     * 当 lambda 的期望类型本身是类型变量时创建可修订期望类型 atom。
     */
    private fun ArgumentContext.createLambdaWithTypeVariableAsExpectedTypeAtomIfNeeded(
        atom: ConeResolutionAtomWithPostponedChild,
    ): Boolean {
        val expectedType = expectedType as? ConeTypeVariableType ?: return false
        val explicitTypeArgument = csBuilder.currentStorage()
            .notFixedTypeVariables[expectedType.typeConstructor]
            ?.constraints
            ?.find { constraint ->
                constraint.kind == ConstraintKind.EQUALITY &&
                    constraint.position.from is ConeExplicitTypeParameterConstraintPosition
            }
            ?.type as? ConeCangJieType

        if (explicitTypeArgument != null && explicitTypeArgument.typeArguments.isEmpty()) {
            return false
        }

        val lambdaAtom = ConeLambdaWithTypeVariableAsExpectedTypeAtom(
            expression = atom.lambdaExpression,
            anonymousFunction = atom.lambdaExpression.anonymousFunction,
            expectedType = expectedType,
            candidateOfOuterCall = candidate,
            anonymousFunctionIfReturnExpression = anonymousFunctionIfReturnExpression,
        )
        val lambdaFunctionShape = buildLambdaFunctionShapeForTypeVariableExpectedType(atom.lambdaExpression)
        csBuilder.addSubtypeConstraint(
            lambdaFunctionShape,
            expectedType,
            ConeArgumentConstraintPosition(atom.lambdaExpression),
        )
        lambdaAtom.reviseExpectedType(lambdaFunctionShape)
        candidate.addPostponedAtom(lambdaAtom)
        atom.setPostponedSubAtom(lambdaAtom)
        return true
    }

    /**
     * expected type 仍是类型变量时，先把 lambda 自身的函数值形状登记进约束系统。
     *
     * 这条结构约束必须早于 completion 固定 expected 变量，否则无上下文 lambda 实参会在
     * Stage 8 被当成缺少参数类型处理，来不及通过 body 中的表达式继续推断参数类型。
     */
    private fun ArgumentContext.buildLambdaFunctionShapeForTypeVariableExpectedType(
        expression: CfirAnonymousFunctionExpression,
    ): ConeFunctionType {
        val anonymousFunction = expression.anonymousFunction
        val parameterTypes = anonymousFunction.valueParameters.mapIndexed { index, parameter ->
            parameter.returnTypeRef.coneTypeOrNull
                ?: ConeTypeVariableForLambdaParameterType(
                    TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE + index,
                ).also(csBuilder::registerVariable).defaultType
        }
        val returnType = anonymousFunction.returnTypeRef.coneTypeOrNull
            ?: ConeTypeVariableForLambdaReturnType(
                anonymousFunction,
                TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE,
            ).also(csBuilder::registerVariable).defaultType
        return ConeFunctionType(
            parameterTypes = parameterTypes,
            returnType = returnType,
        )
    }

    /**
     * 创建已解析 lambda atom，并把对应函数类型约束写入候选约束系统。
     */
    private fun ArgumentContext.createResolvedLambdaAtom(
        atom: ConeResolutionAtomWithPostponedChild,
        duringCompletion: Boolean,
        returnTypeVariable: ConeTypeVariableForLambdaReturnType?,
    ): ConeResolvedLambdaAtom {
        val expression = atom.lambdaExpression
        val anonymousFunction = expression.anonymousFunction
        val expectedFunctionType = expectedType as? ConeFunctionType

        val declaredParameterTypes = anonymousFunction.valueParameters.mapIndexed { index, parameter ->
            parameter.returnTypeRef.coneTypeOrNull
                ?: expectedFunctionType?.parameterTypes?.getOrNull(index)
                ?: ConeTypeVariableForLambdaParameterType(
                    TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE + index,
                ).also(csBuilder::registerVariable).defaultType
        }
        val shapeDiagnostic = expectedFunctionType
            ?.let { lambdaParameterShapeDiagnostic(anonymousFunction, it, declaredParameterTypes) }
        if (shapeDiagnostic != null) {
            anonymousFunction.lambdaParameterShapeExpectedFunctionType = expectedFunctionType
        }
        val parameterTypes = if (shapeDiagnostic is LambdaParameterTypeMismatch) {
            declaredParameterTypes.mapIndexed { index, declaredType ->
                expectedFunctionType?.parameterTypes?.getOrNull(index) ?: declaredType
            }
        } else {
            declaredParameterTypes
        }

        val createdReturnTypeVariable = if (anonymousFunction.returnTypeRef.coneTypeOrNull == null &&
            returnTypeVariable == null &&
            expectedFunctionType == null
        ) {
            ConeTypeVariableForLambdaReturnType(anonymousFunction, TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE)
                .also(csBuilder::registerVariable)
        } else {
            null
        }

        val lambdaReturnType = expectedFunctionType?.returnType
            ?: returnTypeVariable?.defaultType
            ?: anonymousFunction.returnTypeRef.coneTypeOrNull
            ?: createdReturnTypeVariable!!.defaultType

        val resolvedAtom = ConeResolvedLambdaAtom(
            expression = expression,
            anonymousFunction = anonymousFunction,
            expectedType = expectedType,
            parameterTypes = parameterTypes,
            returnType = lambdaReturnType,
            typeVariableForLambdaReturnType = returnTypeVariable ?: createdReturnTypeVariable,
        )

        atom.setPostponedSubAtom(resolvedAtom)
        candidate.addPostponedAtom(resolvedAtom)

        val targetExpectedType = expectedType
        if (targetExpectedType != null) {
            val lambdaType = ConeFunctionType(
                parameterTypes = parameterTypes,
                returnType = lambdaReturnType,
                isCFunc = expectedFunctionType?.isCFunc ?: false,
                isClosureType = expectedFunctionType?.isClosureType ?: false,
                hasVariableLenArg = expectedFunctionType?.hasVariableLenArg ?: false,
                attributes = expectedFunctionType?.attributes ?: org.cangnova.cangjie.cfir.types.ConeAttributes.Empty,
            )
            val position = ConeArgumentConstraintPosition(expression)
            if (duringCompletion) {
                csBuilder.addSubtypeConstraint(lambdaType, targetExpectedType, position)
            } else {
                val compatible = csBuilder.isSubtypeConstraintCompatible(lambdaType, targetExpectedType)
                csBuilder.addSubtypeConstraint(lambdaType, targetExpectedType, position)
                if (!compatible) {
                    if (targetExpectedType !is ConeErrorType && shapeDiagnostic == null) {
                        reportDiagnostic(
                            ArgumentTypeMismatch(
                                expectedType = targetExpectedType,
                                actualType = lambdaType,
                                argument = expression,
                                isMismatchDueToNullability = false,
                                anonymousFunctionIfReturnExpression = anonymousFunctionIfReturnExpression,
                                systemHadContradiction = csBuilder.hasContradiction,
                            ),
                        )
                    }
                }
            }
        }

        return resolvedAtom
    }

    /**
     * 检查 lambda 头部与目标函数类型的形状兼容性。
     *
     * 官方 `ChkLamParamTys` 先处理参数个数，再处理显式参数类型。含省略参数时，
     * 这些错误要作为 lambda 头部错误释放，不能退化成参数类型注解缺失或 body 级联错误。
     */
    private fun ArgumentContext.lambdaParameterShapeDiagnostic(
        anonymousFunction: CfirAnonymousFunction,
        expectedFunctionType: ConeFunctionType,
        declaredParameterTypes: List<ConeCangJieType>,
    ): ResolutionDiagnostic? {
        val valueParameters = anonymousFunction.valueParameters
        val hasOmittedParameterType = valueParameters.any { it.hasOmittedLambdaParameterType() }

        if (valueParameters.size != expectedFunctionType.parameterTypes.size) {
            return if (hasOmittedParameterType) {
                LambdaParameterCountMismatch(
                    anonymousFunction = anonymousFunction,
                    expectedCount = expectedFunctionType.parameterTypes.size,
                    actualCount = valueParameters.size,
                )
            } else {
                null
            }
        }

        if (!hasOmittedParameterType) return null
        valueParameters.forEachIndexed { index, parameter ->
            if (parameter.hasOmittedLambdaParameterType()) return@forEachIndexed
            val actualType = declaredParameterTypes.getOrNull(index) ?: return@forEachIndexed
            val expectedType = expectedFunctionType.parameterTypes.getOrNull(index) ?: return@forEachIndexed
            if (!isCompatibleExplicitLambdaParameterType(expectedType, actualType)) {
                return LambdaParameterTypeMismatch(
                    anonymousFunction = anonymousFunction,
                    parameter = parameter,
                    expectedType = expectedType,
                    actualType = actualType,
                )
            }
        }

        return null
    }

    /** 源码中是否省略了 lambda 参数类型。 */
    private fun CfirValueParameter.hasOmittedLambdaParameterType(): Boolean =
        isLambdaParameterTypeOmitted == true ||
                returnTypeRef.source?.kind == CjFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter

    /**
     * Lambda 参数按函数类型参数逆变检查；官方这里不做自动装箱。
     */
    private fun ArgumentContext.isCompatibleExplicitLambdaParameterType(
        expectedType: ConeCangJieType,
        actualType: ConeCangJieType,
    ): Boolean {
        if (expectedType is ConeErrorType || actualType is ConeErrorType) return true
        val expectedFunctionType = ConeFunctionType(
            parameterTypes = listOf(expectedType),
            returnType = expectedType,
        )
        val actualFunctionType = ConeFunctionType(
            parameterTypes = listOf(actualType),
            returnType = expectedType,
        )
        return AbstractTypeChecker.isSubtypeOf(session.typeContext, actualFunctionType, expectedFunctionType)
    }

    /**
     * 将 postponed child atom 的表达式读取为匿名函数表达式。
     */
    private val ConeResolutionAtomWithPostponedChild.lambdaExpression: CfirAnonymousFunctionExpression
        get() = expression as? CfirAnonymousFunctionExpression
            ?: error("Expected anonymous function expression, but was ${expression::class}")
}
