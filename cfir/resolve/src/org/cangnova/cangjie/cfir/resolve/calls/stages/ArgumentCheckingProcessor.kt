package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostic.AmbiguousArgumentType
import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.diagnostic.InapplicableWrongReceiver
import org.cangnova.cangjie.cfir.diagnostic.UnsuccessfulCallableReferenceArgument
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.calls.*
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
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
import org.cangnova.cangjie.cfir.types.ConeTypeIntersector
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.types.arrayLiteralElementType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import org.cangnova.cangjie.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.isSubtypeConstraintCompatible
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_FOR_LAMBDA_RETURN_TYPE
import org.cangnova.cangjie.resolve.calls.inference.components.PostponedArgumentInputTypesResolver.Companion.TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE
import org.cangnova.cangjie.resolve.calls.inference.model.ArgumentConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintPosition
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
        val argumentType = arrayLiteralTypeFromExpectedType(atom.expression) ?: atom.expression.coneTypeOrNull ?: return
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

        val argumentType = substituteTypeParameterUpperBoundIfNeeded(argumentTypeBeforeCapturing, expectedType, session)
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

        if (csBuilder.addSubtypeConstraintIfCompatible(argumentType, expectedType, position)) return
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
        return this is ConeIdealLiteralType || this is ConePrimitiveType && kind.isIdeal
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
        candidate.addPostponedAtom(lambdaAtom)
        atom.setPostponedSubAtom(lambdaAtom)
        return true
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

        val parameterTypes = anonymousFunction.valueParameters.mapIndexed { index, parameter ->
            parameter.returnTypeRef.coneTypeOrNull
                ?: expectedFunctionType?.parameterTypes?.getOrNull(index)
                ?: ConeTypeVariableForLambdaParameterType(
                    TYPE_VARIABLE_NAME_PREFIX_FOR_LAMBDA_PARAMETER_TYPE + index,
                ).also(csBuilder::registerVariable).defaultType
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
                    if (targetExpectedType !is ConeErrorType) {
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
     * 将 postponed child atom 的表达式读取为匿名函数表达式。
     */
    private val ConeResolutionAtomWithPostponedChild.lambdaExpression: CfirAnonymousFunctionExpression
        get() = expression as? CfirAnonymousFunctionExpression
            ?: error("Expected anonymous function expression, but was ${expression::class}")
}
