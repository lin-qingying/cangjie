/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirFunctionTarget
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotInferValueParameterType
import org.cangnova.cangjie.cfir.diagnostic.ConeConstraintSystemHasContradiction
import org.cangnova.cangjie.cfir.diagnostic.ConeInapplicableCandidateError
import org.cangnova.cangjie.cfir.diagnostic.ConeTypeParameterInQualifiedAccess
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.buildResolvedArgumentList
import org.cangnova.cangjie.cfir.expressions.builder.buildArrayLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildBlockCopy
import org.cangnova.cangjie.cfir.expressions.builder.buildReturnExpression
import org.cangnova.cangjie.cfir.lookupTracker
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.render
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzer
import org.cangnova.cangjie.cfir.resolve.body.buildAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtomWithPostponedChild
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedLambdaAtom
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirErrorReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.substituteExplicitTypeArgumentConstraints
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.toErrorReference
import org.cangnova.cangjie.cfir.resolve.transformers.CfirAbstractTreeTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.IntegerLiteralAndOperatorApproximationTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.fakeElement
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import java.util.IdentityHashMap

class CfirCallCompletionResultsWriterTransformer(
    override val session: CfirSession,
    override val scopeSession: ScopeSession,
    private val finalSubstitutor: ConeSubstitutor,
    private val typeCalculator: ReturnTypeCalculator,
    private val typeApproximator: ConeTypeApproximator,
    private val dataFlowAnalyzer: CfirDataFlowAnalyzer,
    private val integerOperatorApproximator: IntegerLiteralAndOperatorApproximationTransformer,
    private val context: BodyResolveContext,
    private val mode: Mode = Mode.Normal,
    private var insideAnnotationContext: Boolean = false,
) : CfirAbstractTreeTransformer<ExpectedArgumentType?>(phase = CfirResolvePhase.BODY_RESOLVE),
    SessionAndScopeSessionHolder {


    private fun <T : CfirQualifiedAccessExpression> prepareQualifiedTransform(
        qualifiedAccessExpression: T, calleeReference: CfirNamedReferenceWithCandidate,
    ): T {
        val subCandidate = calleeReference.candidate

        val declaration = subCandidate.symbol.cfir

        val type = if (declaration is CfirFunction && subCandidate.callInfo.callKind == CallKind.NamedValueAccess) {
            computeNamedValueFunctionType(declaration, subCandidate)
        } else if (declaration is CfirCallableDeclaration) {
            val calculated = typeCalculator.tryCalculateReturnType(declaration)
            calculated.coneType
        } else {
            // this branch is for cases when we have
            // some invalid qualified access expression itself.
            // e.g. `T::toString` where T is a generic type.
            // in these cases we should report an error on
            // the calleeReference.source which is not a fake source.
            ConeErrorType(
                when (declaration) {
                    is CfirTypeParameter -> ConeTypeParameterInQualifiedAccess(declaration.symbol)
                    else -> ConeSimpleDiagnostic("Callee reference to candidate without return type: ${declaration.render()}")
                }
            )
        }

        val resolvedReference =calleeReference.toResolvedReference()

        qualifiedAccessExpression.replaceCalleeReference(resolvedReference)
        qualifiedAccessExpression.replaceDispatchReceiver(
            subCandidate.dispatchReceiverExpression()?.transformSingle(integerOperatorApproximator, null)
        )
        qualifiedAccessExpression.replaceTypeArguments(computeTypeArguments(qualifiedAccessExpression, subCandidate))
        qualifiedAccessExpression.replaceConeTypeOrNull(type)

        runPCLARelatedTasksForCandidate(subCandidate)
        return qualifiedAccessExpression
    }

    /**
     * 仓颉允许把函数名作为值使用，例如 `let f = obj.foo`。
     * 这种访问完成后表达式类型应是函数类型，而不是 `foo` 的返回值类型；
     * 否则后续 `f()` 无法进入函数类型 `invoke` 的 tower level。
     */
    private fun computeNamedValueFunctionType(
        declaration: CfirFunction,
        candidate: Candidate,
    ): ConeCangJieType {
        val parameterTypes = declaration.valueParameters.map { parameter ->
            val parameterType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                ?: return ConeErrorType(ConeSimpleDiagnostic("Unresolved function parameter type"))
            parameterType.substituteType(candidate)
        }

        typeCalculator.tryCalculateReturnType(declaration)
        val returnType = finallySubstituteOrSelf(candidate.substitutedReturnType()).approximateThisTypeForDeclaration()
        return ConeFunctionType(parameterTypes, returnType)
    }

    private fun ConeCangJieType.substituteType(
        candidate: Candidate,
        // Substitutor from type variables (not type parameters)
        substitutor: ConeSubstitutor = finalSubstitutor,
    ): ConeCangJieType {
        // Type parameters are replaced with type variables
        val initialType = candidate.substitutor.substituteOrSelf(this)
        // Type variables are replaced with final type arguments
        val substitutedType = finallySubstituteOrNull(initialType, substitutor) ?: initialType
        // Everything is approximated
        val finalType = typeApproximator.approximateToSuperType(
            type = substitutedType,
            TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2,
        ) ?: substitutedType

        // This is probably a temporary hack, but it seems necessary because elvis has that attribute and it may leak further like
        // fun <E> foo() = materializeNullable<E>() ?: materialize<E>() // `foo` return type unexpectedly gets inferred to @Exact E
        //
        // In FE1.0, it's not necessary since the annotation for elvis have some strange form (see org.jetbrains.kotlin.resolve.descriptorUtil.AnnotationsWithOnly)
        // that is not propagated further.
        return finalType
    }

    private fun CfirNamedReferenceWithCandidate.computeAllArguments(
        originalArgumentList: CfirArgumentList,
        predefinedMapping: LinkedHashMap<CfirExpression, CfirValueParameter>? = null,
    ): List<CfirExpression> {
        return when {
            this.isError -> originalArgumentList.arguments
            predefinedMapping != null -> predefinedMapping.keys.toList()
            else -> candidate.argumentMapping.keys.unwrapAtoms()
        }
    }

    override fun transformFunctionCall(functionCall: CfirFunctionCall, data: ExpectedArgumentType?): CfirExpression {
        data?.argumentReplacements?.get(functionCall)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }

        val calleeReference = functionCall.calleeReference as? CfirNamedReferenceWithCandidate ?: return functionCall
        val result = prepareQualifiedTransform(functionCall, calleeReference)
        val candidate = calleeReference.candidate
        val originalArgumentList = result.argumentList

        val resultType = candidate.callFailureDiagnosticForResultType()?.let { diagnostic ->
            ConeErrorType(ConeUnreportedDuplicateDiagnostic(diagnostic))
        } ?: result.resolvedType.substituteType(candidate)
        val allArgs = calleeReference.computeAllArguments(originalArgumentList)
        val (regularMapping, allArgsMapping) = candidate.handleVarargsAndReturnResultingArgumentsMapping(
            argumentList = allArgs,
            callSource = functionCall.source,
        )
        val expectedArgumentsTypeMapping = candidate.createArgumentsMapping(forErrorReference = calleeReference.isError)
        result.replaceArgumentList(
            rewriteArgumentList(
                candidate = candidate,
                originalArgumentList = originalArgumentList,
                expectedArgumentsTypeMapping = expectedArgumentsTypeMapping,
                regularMapping = regularMapping,
                allArgsMapping = allArgsMapping,
                forErrorReference = calleeReference.isError,
            )
        )

        result.replaceConeTypeOrNull(resultType)
        session.lookupTracker?.recordTypeResolveAsLookup(resultType, functionCall.source, context.file.source)
        result.addNonFatalDiagnostics(candidate)
        return result
    }

    private fun rewriteArgumentList(
        candidate: Candidate,
        originalArgumentList: CfirArgumentList,
        expectedArgumentsTypeMapping: ExpectedArgumentType.ArgumentsMap?,
        regularMapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
        allArgsMapping: LinkedHashMap<CfirExpression, CfirValueParameter?>,
        forErrorReference: Boolean,
    ): CfirArgumentList {
        val transformedRegularMapping = LinkedHashMap<CfirExpression, CfirValueParameter>(regularMapping.size)
        val transformedAllArgsMapping = LinkedHashMap<CfirExpression, CfirValueParameter?>(allArgsMapping.size)
        val transformedArguments = IdentityHashMap<CfirExpression, CfirExpression>()

        fun transform(argument: CfirExpression, parameter: CfirValueParameter?): CfirExpression =
            transformedArguments.getOrPut(argument) {
                val transformed = transformCallArgument(argument, expectedArgumentsTypeMapping)
                    .withResolvedArrayArgumentType(candidate, parameter)
                transformed
            }

        for ((argument, parameter) in allArgsMapping) {
            transformedAllArgsMapping[transform(argument, parameter)] = parameter
        }

        for ((argument, parameter) in regularMapping) {
            transformedRegularMapping[transform(argument, parameter)] = parameter
        }

        return if (forErrorReference) {
            buildArgumentListForErrorCall(originalArgumentList, transformedAllArgsMapping)
        } else {
            buildResolvedArgumentList(originalArgumentList, transformedRegularMapping)
        }
    }

    private fun CfirExpression.withResolvedArrayArgumentType(
        candidate: Candidate,
        parameter: CfirValueParameter?,
    ): CfirExpression {
        if (this !is CfirArrayLiteral || elements.isNotEmpty() || parameter == null) return this
        val expectedArrayType = parameter.returnTypeRef.coneTypeOrNull
            ?.substituteType(candidate)
            ?.fullyExpandedType()
            ?.takeIf { it.arrayLiteralElementType != null }
            ?: return this
        replaceConeTypeOrNull(expectedArrayType)
        return this
    }

    private fun transformCallArgument(
        originalArgument: CfirExpression,
        expectedArgumentsTypeMapping: ExpectedArgumentType.ArgumentsMap?,
    ): CfirExpression {
        val argumentBeforeTransform =
            (expectedArgumentsTypeMapping?.argumentReplacements?.get(originalArgument) ?: originalArgument) as CfirExpression
        val transformedArgument =
            argumentBeforeTransform.transformSingle(this, expectedArgumentsTypeMapping) as CfirExpression
        val replacedAfterTransform =
            (expectedArgumentsTypeMapping?.argumentReplacements?.get(transformedArgument) ?: transformedArgument) as CfirExpression
        val expectedType = expectedArgumentsTypeMapping?.getExpectedType(originalArgument)
            ?: expectedArgumentsTypeMapping?.getExpectedType(argumentBeforeTransform)
            ?: expectedArgumentsTypeMapping?.getExpectedType(replacedAfterTransform)
        val expectedArrayType = expectedType?.fullyExpandedType()
            ?.takeIf { it.arrayLiteralElementType != null }
        if (replacedAfterTransform is CfirArrayLiteral &&
            replacedAfterTransform.elements.isEmpty() &&
            expectedArrayType != null
        ) {
            replacedAfterTransform.replaceConeTypeOrNull(expectedArrayType)
        }
        return replacedAfterTransform.transformSingle(integerOperatorApproximator, expectedType) as CfirExpression
    }

    private fun Candidate.createArgumentsMapping(forErrorReference: Boolean): ExpectedArgumentType.ArgumentsMap? {
        val lambdasReturnType = postponedAtoms.filterIsInstance<ConeResolvedLambdaAtom>().associate { atom ->
            atom.anonymousFunction to finallySubstituteOrSelf(substitutor.substituteOrSelf(atom.returnType))
        }
        val arguments = LinkedHashMap<CfirElement, ConeCangJieType>()

        fun registerExpectedType(argument: CfirExpression, expectedType: ConeCangJieType) {
            arguments[argument] = expectedType
            if (argument is CfirAnonymousFunctionExpression) {
                arguments[argument.anonymousFunction] = expectedType
            }
        }

        val cangjieVariadicParameter = cangjieVariadicParameterForCall
        for ((atom, valueParameter) in argumentMapping) {
            val parameterType = valueParameter.returnTypeRef.coneTypeOrNull
                ?.substituteType(this)
                ?.let { substituteExplicitTypeArgumentConstraints(it) }
                ?: continue
            val expectedType = if (valueParameter == cangjieVariadicParameter) {
                parameterType.arrayElementType ?: parameterType
            } else {
                parameterType
            }
            registerExpectedType(atom.expression, expectedType)
            val unwrappedArgument = atom.unwrapAtom()
            if (unwrappedArgument !== atom.expression) {
                registerExpectedType(unwrappedArgument, expectedType)
            }
        }

        val argumentReplacements = this@createArgumentsMapping.argumentReplacements
        argumentReplacements?.forEach { (original, replacement) ->
            val expectedType = arguments[original] ?: return@forEach
            if (replacement is CfirExpression) {
                registerExpectedType(replacement, expectedType)
            }
        }

        if (lambdasReturnType.isEmpty() && arguments.isEmpty() && argumentReplacements.isNullOrEmpty()) return null
        return ExpectedArgumentType.ArgumentsMap(
            map = arguments,
            lambdasReturnTypes = lambdasReturnType,
            forErrorReference = forErrorReference,
            argumentReplacements,
        )
    }
    private data class ResultingArgumentsMapping(
        val regularMapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
        val allArgsMapping: LinkedHashMap<CfirExpression, CfirValueParameter?>,
    )

    private fun Candidate.handleVarargsAndReturnResultingArgumentsMapping(
        argumentList: List<CfirExpression>,
        callSource: CjSourceElement?,
        precomputedArgumentMapping: LinkedHashMap<CfirExpression, CfirValueParameter>? = null,
    ): ResultingArgumentsMapping {
        val argumentMapping = precomputedArgumentMapping ?: this.argumentMapping.unwrapAtoms()
        val variadicParameter = cangjieVariadicParameterForCall
        return if (variadicParameter != null) {
            val resolvedArrayType = variadicParameter.returnTypeRef.coneTypeOrNull?.substituteType(this)
                ?: ConeErrorType(ConeSimpleDiagnostic("Unresolved variadic array parameter type"))
            val argumentMappingWithAllArgs = remapArgumentsWithCangjieVararg(
                variadicParameter = variadicParameter,
                resolvedArrayType = resolvedArrayType,
                argumentMapping = argumentMapping,
                argumentList = argumentList,
                callSource = callSource,
                parameters = declaredParametersForMapping(),
            )
            ResultingArgumentsMapping(
                argumentMappingWithAllArgs.filterValuesNotNullToLinkedMap(),
                argumentMappingWithAllArgs,
            )
        } else {
            ResultingArgumentsMapping(
                argumentMapping,
                argumentList.associateWithTo(LinkedHashMap()) { argumentMapping[it] },
            )
        }
    }

    private fun remapArgumentsWithCangjieVararg(
        variadicParameter: CfirValueParameter,
        resolvedArrayType: ConeCangJieType,
        argumentMapping: LinkedHashMap<CfirExpression, CfirValueParameter>,
        argumentList: List<CfirExpression>,
        callSource: CjSourceElement?,
        parameters: List<CfirValueParameter>,
    ): LinkedHashMap<CfirExpression, CfirValueParameter?> {
        val result = LinkedHashMap<CfirExpression, CfirValueParameter?>()
        val variadicArguments = mutableListOf<CfirExpression>()
        val variadicParameterIndex = parameters.indexOf(variadicParameter)
        var variadicArrayAdded = false

        fun flushVariadicArguments(source: CjSourceElement?) {
            if (variadicArrayAdded) return
            val variadicArray = buildArrayLiteral {
                this.source = source?.fakeElement(CjFakeSourceElementKind.VarargArgument)
                coneTypeOrNull = resolvedArrayType
                elements.addAll(variadicArguments)
            }
            result[variadicArray] = variadicParameter
            variadicArguments.clear()
            variadicArrayAdded = true
        }

        for (argument in argumentList) {
            val parameter = argumentMapping[argument]
            if (parameter == variadicParameter) {
                variadicArguments += argument
            } else {
                val parameterIndex = parameter?.let { parameters.indexOf(it) } ?: -1
                if (
                    !variadicArrayAdded &&
                    variadicParameterIndex >= 0 &&
                    parameterIndex > variadicParameterIndex
                ) {
                    flushVariadicArguments(variadicArguments.firstOrNull()?.source ?: argument.source)
                }
                result[argument] = parameter
            }
        }
        flushVariadicArguments(variadicArguments.firstOrNull()?.source ?: callSource)
        return result
    }

    override fun transformNamedAccessExpression(
        namedAccessExpression: CfirNamedAccessExpression,
        data: ExpectedArgumentType?
    ): CfirExpression {
        data?.argumentReplacements?.get(namedAccessExpression)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }
        val replacement = replacementFor(namedAccessExpression)
        if (replacement != null) {
            return replacement.transformSingle(this, data)
        }
        return transformQualifiedAccessExpression(namedAccessExpression, data)
    }

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        data: ExpectedArgumentType?
    ): CfirExpression {
        data?.argumentReplacements?.get(qualifiedAccessExpression)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }
        val replacement = replacementFor(qualifiedAccessExpression)
        if (replacement != null) {
            return replacement.transformSingle(this, data)
        }

        val calleeReference = qualifiedAccessExpression.calleeReference as? CfirNamedReferenceWithCandidate
            ?: return qualifiedAccessExpression
        val result = prepareQualifiedTransform(qualifiedAccessExpression, calleeReference)
        val candidate = calleeReference.candidate
        result.transformChildren(this, data)
        val resultType = result.coneTypeOrNull?.substituteType(candidate)
        if (resultType != null) {
            // qualified access 完成后必须写回最终替换类型；无参 enum constructor
            // 如 `None` 的 owner 泛型依赖 expected type 约束，不能保留声明原始类型。
            val approximatedType = integerOperatorApproximator.approximateType(
                resultType,
                data?.getExpectedType(qualifiedAccessExpression),
            ) ?: resultType
            result.replaceConeTypeOrNull(approximatedType)
            session.lookupTracker?.recordTypeResolveAsLookup(
                approximatedType,
                qualifiedAccessExpression.source,
                context.file.source,
            )
        }
        result.addNonFatalDiagnostics(candidate)
        return result
    }

    override fun transformArrayLiteral(arrayLiteral: CfirArrayLiteral, data: ExpectedArgumentType?): CfirExpression {
        data?.argumentReplacements?.get(arrayLiteral)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }
        val expectedArrayType = data?.getExpectedType(arrayLiteral)?.fullyExpandedType()
            ?.takeIf { it.arrayLiteralElementType != null }
        if (arrayLiteral.elements.isEmpty() && expectedArrayType != null) {
            arrayLiteral.replaceConeTypeOrNull(expectedArrayType)
            return arrayLiteral
        }
        arrayLiteral.transformChildren(this, data)
        arrayLiteral.replaceConeTypeOrNull(
            integerOperatorApproximator.approximateType(
                arrayLiteral.coneTypeOrNull,
                data?.getExpectedType(arrayLiteral),
            )
        )
        return arrayLiteral
    }

    override fun transformBlock(block: CfirBlock, data: ExpectedArgumentType?): CfirExpression {
        val expectedType = data?.getExpectedType(block)
        if (expectedType != null && block.statements.singleOrNull() is CfirExpression) {
            block.transformStatements(this, expectedType.toExpectedType(data.argumentReplacements))
            block.transformOtherChildren(this, data)
            block.replaceConeTypeOrNull((block.statements.single() as CfirExpression).coneTypeOrNull)
            return block
        }

        block.transformChildren(this, data)
        block.replaceConeTypeOrNull((block.statements.lastOrNull() as? CfirExpression)?.coneTypeOrNull)
        return block
    }

    override fun transformWrappedExpression(wrappedExpression: CfirWrappedExpression, data: ExpectedArgumentType?): CfirExpression {
        val expectedType = data?.getExpectedType(wrappedExpression)
        val expressionData = expectedType?.toExpectedType(data.argumentReplacements) ?: data
        wrappedExpression.transformChildren(this, expressionData)
        wrappedExpression.replaceConeTypeOrNull(wrappedExpression.expression.coneTypeOrNull)
        return wrappedExpression
    }

    override fun transformRangeExpression(rangeExpression: CfirRangeExpression, data: ExpectedArgumentType?): CfirExpression {
        data?.argumentReplacements?.get(rangeExpression)?.let { replacement ->
            return replacement.transformSingle(this, data)
        }

        val expectedRangeType = data?.getExpectedType(rangeExpression)?.rangeTypeOrNull()
        val endpointExpectedType = expectedRangeType?.typeArguments?.singleOrNull()?.type
        val endpointData = endpointExpectedType?.toExpectedType(data?.argumentReplacements)
        rangeExpression.transformAnnotations(this, data)
        rangeExpression.transformStart(this, endpointData)
        rangeExpression.transformEnd(this, endpointData)
        rangeExpression.transformStep(this, ConePrimitiveType.INT64.toExpectedType(data?.argumentReplacements))
        if (expectedRangeType != null) {
            rangeExpression.replaceConeTypeOrNull(expectedRangeType)
        }
        return rangeExpression
    }

    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: CfirAnonymousFunctionExpression,
        data: ExpectedArgumentType?
    ): CfirExpression {
        finalizeAnonymousFunction(
            function = anonymousFunctionExpression.anonymousFunction,
            data = data,
            anonymousFunctionExpression = anonymousFunctionExpression,
        )
        val expectedType = data?.getExpectedType(anonymousFunctionExpression)
            ?: data?.getExpectedType(anonymousFunctionExpression.anonymousFunction)
        val anonymousFunction = anonymousFunctionExpression.anonymousFunction
        rewriteAnonymousFunctionParameterTypes(
            anonymousFunction = anonymousFunction,
            expectedType = expectedType,
            containingCallIsError = (data as? ExpectedArgumentType.ArgumentsMap)?.forErrorReference == true,
        )
        val approximatedType = integerOperatorApproximator.approximateType(
            buildLambdaType(anonymousFunction, expectedType as? ConeFunctionType),
            expectedType,
        )
        if (approximatedType != null) {
            anonymousFunction.replaceTypeRef(
                approximatedType.toCfirResolvedTypeRef(anonymousFunction.typeRef.source, anonymousFunction.typeRef),
            )
        }
        return anonymousFunctionExpression
    }

    /**
     * 补全阶段必须把已选候选的函数形参类型写回 lambda 参数。
     *
     * Kotlin FIR 在 `FirCallCompleter.LambdaAnalyzerImpl` 中完成这一步；仓颉的
     * overload-by-lambda 会在候选回滚后再由 results writer 统一落树，因此这里
     * 对同一份 expected function type 做最终写回，而不是让 checker 兜底放行。
     */
    private fun rewriteAnonymousFunctionParameterTypes(
        anonymousFunction: CfirAnonymousFunction,
        expectedType: ConeCangJieType?,
        containingCallIsError: Boolean,
    ) {
        val expectedFunctionType = (expectedType as? ConeFunctionType)
            ?.takeUnless { containingCallIsError }
            ?: return

        anonymousFunction.replaceMatchingParameterFunctionType(expectedFunctionType)
        anonymousFunction.valueParameters.forEachIndexed { index, parameter ->
            val parameterType = expectedFunctionType.parameterTypes.getOrNull(index)
                ?.let(::finallySubstituteOrSelf)
                ?.let(::approximateLambdaInputType)
                ?: ConeErrorType(
                    ConeCannotInferValueParameterType(
                        parameter.symbol,
                        "Lambda or anonymous function has more parameters than expected",
                    ),
                )
            val source = parameter.source
                ?.fakeElement(CjFakeSourceElementKind.ImplicitReturnTypeOfLambdaValueParameter)
            val typeRef = if (parameter.returnTypeRef is CfirImplicitTypeRef) {
                parameterType.toCfirResolvedTypeRef(source)
            } else {
                parameter.returnTypeRef.resolvedTypeFromPrototype(parameterType, source)
            }
            parameter.replaceReturnTypeRef(typeRef)
        }
    }

    private fun approximateLambdaInputType(type: ConeCangJieType): ConeCangJieType {
        return typeApproximator.approximateToSuperType(
            type,
            TypeApproximatorConfiguration.IntermediateApproximationToSupertypeAfterCompletionInK2,
        ) ?: type
    }

    private fun computeTypeArgumentTypes(
        candidate: Candidate,
    ): List<ConeCangJieType> {
        val declaration = candidate.symbol.cfir as? CfirCallableDeclaration ?: return emptyList()

        return declaration.typeParameters.map {
            val typeParameter = ConeTypeParameterTypeImpl(it.symbol.toLookupTag())
            val substitution = candidate.substitutor.substituteOrSelf(typeParameter)
            finallySubstituteOrSelf(substitution).let { substitutedType ->
                typeApproximator.approximateToSuperType(
                    substitutedType, TypeApproximatorConfiguration.TypeArgumentApproximationAfterCompletionInK2,
                ) ?: substitutedType
            }
        }
    }

    /**
     * 对齐 Kotlin `FirCallCompletionResultsWriterTransformer.computeTypeArguments`：
     * 完成阶段会写回推断后的类型实参，但显式错误实参本身必须保留在树上，
     * 否则错误 type-ref 的诊断会在替换过程中被剥掉。
     */
    private fun computeTypeArguments(
        access: CfirQualifiedAccessExpression,
        candidate: Candidate,
    ): List<CfirResolvedTypeRef> {
        val typeArguments = computeTypeArgumentTypes(candidate).mapIndexed { index, type ->
            val sourceTypeArgument = candidate.typeArgumentMapping.sourceTypeRef(index)
            if (sourceTypeArgument?.coneType?.fullyExpandedType(session) is ConeErrorType) {
                return@mapIndexed sourceTypeArgument
            }

            val source = sourceTypeArgument?.source
            val delegatedTypeRef = sourceTypeArgument?.delegatedTypeRef ?: sourceTypeArgument
            when (type) {
                is ConeErrorType -> buildErrorTypeRef {
                    this.source = source
                    coneType = type
                    this.delegatedTypeRef = delegatedTypeRef
                    diagnostic = type.diagnostic
                }

                else -> buildResolvedTypeRef {
                    this.source = source
                    coneType = type
                    this.delegatedTypeRef = delegatedTypeRef
                }
            }
        }

        if (typeArguments.size >= access.typeArguments.size) return typeArguments

        return typeArguments + access.typeArguments
            .subList(typeArguments.size, access.typeArguments.size)
            .filterIsInstance<CfirResolvedTypeRef>()
    }



    private fun replacementFor(expression: CfirExpression): CfirExpression? {
        val candidateReference = (expression as? org.cangnova.cangjie.cfir.expressions.CfirResolvable)
            ?.calleeReference as? CfirNamedReferenceWithCandidate ?: return null
        return candidateReference.candidate.argumentReplacements?.get(expression)
    }

    private fun resolvedReferenceFor(
        calleeReference: CfirNamedReferenceWithCandidate,
        resultType: ConeCangJieType,
    ): CfirNamedReference {
        val resolved = calleeReference.toResolvedReference()
        return if (resolved is CfirResolvedNamedReference) {
            buildAppliedCallableReference(calleeReference.name, calleeReference.candidate, resultType, finalSubstitutor)
        } else {
            resolved
        }
    }

    private fun runPCLARelatedTasksForCandidate(candidate: Candidate) {
        for (postponedCall in candidate.postponedPCLACalls) {
            postponedCall.expression.transform<CfirElement, ExpectedArgumentType?>(this, null)
        }

        for (callback in candidate.onPCLACompletionResultsWritingCallbacks) {
            callback(finalSubstitutor)
        }

        for (lambda in candidate.lambdasAnalyzedWithPCLA) {
            finalizeAnonymousFunction(lambda as? CfirFunction ?: continue, null)
        }
    }

    private fun finalizeAnonymousFunction(
        function: CfirFunction,
        data: ExpectedArgumentType?,
        anonymousFunctionExpression: CfirAnonymousFunctionExpression? = null,
    ) {
        val anonymousFunction = function as? CfirAnonymousFunction ?: return
        val initialReturnType = anonymousFunction.returnTypeRef.coneTypeOrNull
        val returnExpressions = dataFlowAnalyzer
            .returnExpressionsOfAnonymousFunction(anonymousFunction)
            .replacePostponedAtomsInReturnExpressions(data)
        val containingCallIsError = (data as? ExpectedArgumentType.ArgumentsMap)?.forErrorReference == true
        val expectedFunctionType =
            (data?.let { context ->
                anonymousFunctionExpression?.let(context::getExpectedType)
            } as? ConeFunctionType)
                ?: (data?.getExpectedType(anonymousFunction) as? ConeFunctionType)
        /**
         * 对齐 Kotlin FIR 的语义意图：
         * 对“作为实参传入的 lambda”，它的返回类型首先应该受参数函数类型约束，
         * 而不是盲目继承前序阶段残留在 `returnTypeRef` 上的旧值。
         *
         * 当前仓颉在若干 trailing-lambda 场景里，早期阶段会把匿名函数的返回类型
         * 暂时写成外层 `Unit` 语境，若这里继续优先读取旧值，就会把合法的
         * `(Int64) -> Int64` lambda 错误降级成 `RETURN_TYPE_MISMATCH(expected Unit)`.
         *
         * 因此只要存在参数位 expected function type，就优先以它的 return type 作为
         * 匿名函数最终定型入口；没有参数位约束时，再回退到已有 returnTypeRef。
         */
        val expectedReturnType = expectedFunctionType?.returnType?.let(::finallySubstituteOrSelf)
            ?: initialReturnType?.let(::finallySubstituteOrSelf)
            ?: if (!containingCallIsError) {
                (data as? ExpectedArgumentType.ArgumentsMap)?.lambdasReturnTypes?.get(anonymousFunction)
            } else {
                null
            }
            ?: returnExpressions.firstNotNullOfOrNull { it.expression.coneTypeOrNull }

        val newData = expectedReturnType?.toExpectedType(data?.argumentReplacements)
        for (returnExpression in returnExpressions) {
            if (newData != null) {
                returnExpression.expression.transform<CfirElement, ExpectedArgumentType?>(this, newData)
            } else {
                returnExpression.expression.transform(this, null)
            }
            returnExpression.expression.transformSingle(integerOperatorApproximator, expectedReturnType)
        }

        anonymousFunction.body?.let { body ->
            if (newData != null) {
                body.transform<CfirElement, ExpectedArgumentType?>(this, newData)
            } else {
                body.transform(this, null)
            }
            body.transformSingle(integerOperatorApproximator, expectedReturnType)
        }

        val resultReturnType = computeAnonymousFunctionReturnType(
            anonymousFunction = anonymousFunction,
            expectedReturnType = expectedReturnType,
            returnExpressions = returnExpressions,
        )
        if (initialReturnType != resultReturnType) {
            anonymousFunction.replaceReturnTypeRef(
                anonymousFunction.returnTypeRef.resolvedTypeFromPrototype(resultReturnType, anonymousFunction.source)
            )
        }
        anonymousFunction.addReturnToLastStatementIfNeeded()
    }

    private fun buildLambdaType(
        function: CfirFunction,
        expectedFunctionType: ConeFunctionType? = null,
    ): ConeCangJieType? {
        val parameterTypes = function.valueParameters.mapNotNull { it.returnTypeRef.coneTypeSafe<ConeCangJieType>() }
        val returnType =
            function.returnTypeRef.coneTypeSafe<ConeCangJieType>() ?: function.body?.coneTypeOrNull ?: return null
        return ConeFunctionType(
            parameterTypes = parameterTypes,
            returnType = returnType,
            isCFunc = expectedFunctionType?.isCFunc ?: false,
            isClosureType = expectedFunctionType?.isClosureType ?: false,
            hasVariableLenArg = expectedFunctionType?.hasVariableLenArg ?: false,
            attributes = expectedFunctionType?.attributes ?: org.cangnova.cangjie.cfir.types.ConeAttributes.Empty,
        )
    }

    private fun computeAnonymousFunctionReturnType(
        anonymousFunction: CfirAnonymousFunction,
        expectedReturnType: ConeCangJieType?,
        returnExpressions: Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo>,
    ): ConeCangJieType {
        if (anonymousFunction.isLambda && expectedReturnType?.isUnit == true) {
            return expectedReturnType
        }

        val inferredReturnType = session.typeContext.commonSuperTypeOrNull(
            returnExpressions.mapNotNull { it.expression.coneTypeOrNull }
        ) ?: anonymousFunction.body?.coneTypeOrNull
            ?: session.builtinTypes.unitType

        return if (anonymousFunction.isLambda && expectedReturnType != null && !inferredReturnType.isUnit) {
            expectedReturnType
        } else {
            inferredReturnType
        }
    }

    private fun CfirAnonymousFunction.addReturnToLastStatementIfNeeded() {
        val currentBody = body ?: return
        val returnType = returnTypeRef.coneTypeOrNull ?: return
        if (returnType.isUnit) return

        val lastStatement = currentBody.statements.lastOrNull() as? CfirExpression ?: return
        if (lastStatement is CfirReturnExpression) return

        val newBody = buildBlockCopy(currentBody) {
            statements.clear()
            statements.addAll(currentBody.statements.dropLast(1))
                statements.add(
                    buildReturnExpression {
                        source = (
                            lastStatement.source
                                ?: currentBody.source
                                ?: this@addReturnToLastStatementIfNeeded.source
                            )?.fakeElement(CjFakeSourceElementKind.ImplicitReturn.FromLastStatement)
                        coneTypeOrNull = lastStatement.coneTypeOrNull
                        target = CfirFunctionTarget(labelName = null, isLambda = this@addReturnToLastStatementIfNeeded.isLambda).also {
                            it.bind(this@addReturnToLastStatementIfNeeded)
                        }
                        result = lastStatement
                    }
                )
            }

        (this as? org.cangnova.cangjie.cfir.declarations.impl.CfirAnonymousFunctionImpl)?.body = newBody
    }

    private fun Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo>.replacePostponedAtomsInReturnExpressions(
        data: ExpectedArgumentType?,
    ): Collection<CfirDataFlowAnalyzer.CfirAnonymousFunctionReturnExpressionInfo> {
        return map { returnInfo ->
            val replacement =
                (data?.argumentReplacements?.get(returnInfo.expression) ?: replacePostponedAtom(returnInfo.expression)) as CfirExpression
            if (replacement === returnInfo.expression) {
                returnInfo
            } else {
                returnInfo.copy(expression = replacement)
            }
        }
    }

    private fun replacePostponedAtom(expression: CfirExpression): CfirExpression {
        val candidate = (expression as? org.cangnova.cangjie.cfir.expressions.CfirResolvable)
            ?.calleeReference as? CfirNamedReferenceWithCandidate
            ?: return expression
        val resolvedCandidate = candidate.candidate
        return when {
            expression is CfirQualifiedAccessExpression || expression is CfirNamedAccessExpression || expression is CfirFunctionCall -> {
                expression.transform<CfirElement, ExpectedArgumentType?>(this, null) as CfirExpression
            }

            else -> {
                val argumentReplacements = resolvedCandidate.argumentReplacements
                if (argumentReplacements?.containsKey(expression) == true) {
                    argumentReplacements.getValue(expression)
                } else {
                    expression
                }
            }
        }
    }

    @OptIn(ApplicabilityDetail::class)
    private fun Candidate.callFailureDiagnosticForResultType(): ConeDiagnostic? {
        if (!lowestApplicability.isSuccess) {
            return ConeInapplicableCandidateError(lowestApplicability, this)
        }
        if (!isSuccessful) {
            require(system.hasContradiction) {
                "Candidate is not successful, but system has no contradiction"
            }
            return ConeConstraintSystemHasContradiction(this)
        }
        return null
    }

    private fun CfirNamedReferenceWithCandidate.hasAdditionalResolutionErrors(): Boolean = false

    @OptIn(ApplicabilityDetail::class)
    private fun CfirNamedReferenceWithCandidate.toResolvedReference(): CfirNamedReference {
        val errorDiagnostic = when {
            this is CfirErrorReferenceWithCandidate -> this.diagnostic
            !candidate.lowestApplicability.isSuccess ->
                ConeInapplicableCandidateError(candidate.lowestApplicability, candidate)

            !candidate.isSuccessful -> {
                require(candidate.system.hasContradiction) {
                    "Candidate is not successful, but system has no contradiction"
                }
                ConeConstraintSystemHasContradiction(candidate)

            }

            hasAdditionalResolutionErrors() ->
                ConeConstraintSystemHasContradiction(candidate)

            else -> null
        }

        return when (errorDiagnostic) {
            null -> buildResolvedNamedReference {
                source = this@toResolvedReference.source
                name = this@toResolvedReference.name
                resolvedSymbol = this@toResolvedReference.candidateSymbol
            }

            else -> toErrorReference(errorDiagnostic)
        }
    }

    private fun finallySubstituteOrNull(
        type: ConeCangJieType,
        substitutor: ConeSubstitutor = finalSubstitutor,
    ): ConeCangJieType? {
        val result = substitutor.substituteOrNull(type)
        if (result == null && type is ConeIdealLiteralType) {
            return type.approximateIntegerLiteralType()
        }
        return result?.approximateIntegerLiteralType()
    }

    private fun finallySubstituteOrSelf(type: ConeCangJieType): ConeCangJieType {
        return finallySubstituteOrNull(type) ?: type
    }

    enum class Mode {
        Normal,

        // Retained only as an upstream-aligned seam. The current local direct chain has
        // no delegated-property inference session or writer-construction call site that
        // selects this mode, so this enum value is intentionally unreachable for now.
        DelegatedPropertyCompletion,
    }

    private inline fun <T> withCollectionLiteralInAnnotationResolution(block: () -> T): T {
        val savedInsideAnnotationContext = insideAnnotationContext
        insideAnnotationContext = true
        return try {
            block()
        } finally {
            insideAnnotationContext = savedInsideAnnotationContext
        }
    }

    override fun <E : CfirElement> transformElement(element: E, data: ExpectedArgumentType?): E {
        if (element is CfirDeclaration) return element
        return super.transformElement(element, data)
    }
}

sealed class ExpectedArgumentType(
    val argumentReplacements: Map<CfirElement, CfirExpression>?,
) {
    class ArgumentsMap(
        val map: Map<CfirElement, ConeCangJieType>,
        val lambdasReturnTypes: Map<CfirAnonymousFunction, ConeCangJieType>,
        val forErrorReference: Boolean,
        argumentReplacements: Map<CfirElement, CfirExpression>?,
    ) : ExpectedArgumentType(argumentReplacements)

    class ExpectedType(
        val type: ConeCangJieType,
        argumentReplacements: Map<CfirElement, CfirExpression>?,
    ) : ExpectedArgumentType(argumentReplacements)
}

private fun ExpectedArgumentType.getExpectedType(argument: CfirElement): ConeCangJieType? = when (this) {
    is ExpectedArgumentType.ArgumentsMap -> map[argument]
    is ExpectedArgumentType.ExpectedType -> type
}

private fun ConeCangJieType.rangeTypeOrNull(): ConeClassifierType? = when (this) {
    is ConeClassLikeType -> takeIf { classId == StdlibClassIds.Range }
    is ConeStructType -> takeIf { classId == StdlibClassIds.Range }
    is ConeTypeAliasType -> expandedType?.rangeTypeOrNull()
    else -> null
}

private fun ConeCangJieType.toExpectedType(
    argumentReplacements: Map<CfirElement, CfirExpression>?,
): ExpectedArgumentType = ExpectedArgumentType.ExpectedType(this, argumentReplacements)

private fun ConeCangJieType.approximateIntegerLiteralType(): ConeCangJieType =
    when (this) {
        is ConeIdealLiteralType -> getApproximatedType()
        is ConePrimitiveType -> IdealTypeResolver.resolveIfIdeal(this)
        else -> this
    }

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.resolvedTypeFromPrototype(
    type: ConeCangJieType,
    source: org.cangnova.cangjie.source.CjSourceElement?,
): CfirResolvedTypeRef {
    return when (type) {
        is ConeErrorType -> buildErrorTypeRef {
            this.source = source ?: this@resolvedTypeFromPrototype.source
            coneType = type
            delegatedTypeRef = this@resolvedTypeFromPrototype
            diagnostic = type.diagnostic
        }

        else -> buildResolvedTypeRef {
            this.source = source ?: this@resolvedTypeFromPrototype.source
            coneType = type
            delegatedTypeRef = this@resolvedTypeFromPrototype
        }
    }
}

private fun Collection<ConeResolutionAtom>.unwrapAtoms(): List<CfirExpression> {
    return map { it.unwrapAtom() }
}

private fun ConeResolutionAtom.unwrapAtom(): CfirExpression {
    return when (this) {
//        is ConeCollectionLiteralAtom -> subAtom?.unwrapAtom() ?: expression
        is ConeResolutionAtomWithPostponedChild -> subAtom?.unwrapAtom() ?: expression
        else -> expression
    }
}

fun <V> LinkedHashMap<ConeResolutionAtom, V>.unwrapAtoms(): LinkedHashMap<CfirExpression, V> {
    return mapKeysToLinkedMap { it.unwrapAtom() }
}

private fun <K, V : Any> LinkedHashMap<K, V?>.filterValuesNotNullToLinkedMap(): LinkedHashMap<K, V> {
    val result = LinkedHashMap<K, V>()
    for ((key, value) in this) {
        if (value != null) {
            result[key] = value
        }
    }
    return result
}

inline fun <K1, K2, V> LinkedHashMap<K1, V>.mapKeysToLinkedMap(transform: (K1) -> K2): LinkedHashMap<K2, V> {
    return mapKeysTo(LinkedHashMap()) { transform(it.key) }
}

internal fun CfirQualifiedAccessExpression.addNonFatalDiagnostics(candidate: Candidate){
//    TODO 用于增加非致命性错误
}
