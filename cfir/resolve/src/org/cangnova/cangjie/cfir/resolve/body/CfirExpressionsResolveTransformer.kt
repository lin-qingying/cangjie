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

package org.cangnova.cangjie.cfir.resolve.body

import java.math.BigInteger
import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostic.*
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.patterns.builder.*
import org.cangnova.cangjie.cfir.references.*
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.references.impl.CfirNamedReferenceImpl
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.*
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessAnalyzer
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSpecificTypeResolverTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.resultType
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.semantics.AbstractCandidate
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.source.*
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * Expression resolve transformer.
 *
 * Responsibility: compute and propagate expression types only.
 * This includes literals, accesses, calls, patterns, control-flow, and lambdas.
 *
 * Diagnostic reporting is intentionally NOT performed here. Resolution keeps
 * candidate diagnostics attached to the resolver/completion pipeline output,
 * and a dedicated checker pass reports them after body resolve completes.
 */
@OptIn(CfirImplementationDetail::class, ApplicabilityDetail::class)
open class CfirExpressionsResolveTransformer(
    transformer: CfirAbstractBodyResolveTransformerDispatcher,
) : CfirPartialBodyResolveTransformer(transformer) {
    private data class EffectHandlerContext(
        val commandResultType: ConeCangJieType,
    )

    private val builtinTypes get() = session.builtinTypes
    private val specificTypeResolverTransformer = CfirSpecificTypeResolverTransformer(session)
    private val callResolver get() = components.callResolver
    private val effectHandlerStack = ArrayDeque<EffectHandlerContext>()
    private fun errorType(
        reason: String,
        kind: DiagnosticKind = DiagnosticKind.Other,
        delegatedType: ConeCangJieType? = null,
    ): ConeErrorType = ConeErrorType(ConeSimpleDiagnostic(reason, kind), delegatedType = delegatedType)

    /**
     * 将已经由子表达式承载的错误类型向外传播，避免组合表达式重新报告同一个根因。
     */
    private fun ConeCangJieType.propagatedErrorTypeOrNull(): ConeErrorType? {
        val errorType = this as? ConeErrorType ?: return null
        if (errorType.diagnostic is ConeUnreportedDuplicateDiagnostic) return errorType
        return ConeErrorType(
            ConeUnreportedDuplicateDiagnostic(errorType.diagnostic),
            isUninferredParameter = errorType.isUninferredParameter,
            delegatedType = errorType.delegatedType,
            typeArguments = errorType.typeArguments,
            attributes = errorType.attributes,
        )
    }

    init {
        components.callResolver.initTransformer(this)
    }

    // ── Literals ─────────────────────────────────────────────────────────────

    override fun transformExpression(expression: CfirExpression, data: ResolutionMode): CfirExpression {
        if (expression is CfirThisReceiverExpression) {
            return transformThisReceiverExpression(expression, data)
        }
        if (!expression.hasResolvedType && expression !is CfirWrappedExpression) {
            expression.resultType = ConeErrorType(
                ConeSimpleDiagnostic(
                    "Type calculating for ${expression::class} is not supported",
                    DiagnosticKind.InferenceError
                )
            )
        }
        return (expression.transformChildren(transformer, data) as CfirExpression)
    }

    override fun transformWrappedExpression(
        wrappedExpression: CfirWrappedExpression,
        data: ResolutionMode,
    ): CfirExpression {
        wrappedExpression.transformChildren(transformer, data)
        wrappedExpression.replaceConeTypeOrNull(wrappedExpression.expression.coneTypeOrNull)
        components.dataFlowAnalyzer.exitWrappedExpression(wrappedExpression)
        return wrappedExpression
    }

    override fun transformOptionalExpression(
        optionalExpression: CfirOptionalExpression,
        data: ResolutionMode,
    ): CfirExpression {
        optionalExpression.transformChildren(transformer, data)
        optionalExpression.replaceConeTypeOrNull(optionalExpression.expression.coneTypeOrNull)
        return optionalExpression
    }

    override fun transformOptionalChainExpression(
        optionalChainExpression: CfirOptionalChainExpression,
        data: ResolutionMode,
    ): CfirExpression {
        components.dataFlowAnalyzer.enterOptionalChain(optionalChainExpression)
        optionalChainExpression.transformChildren(transformer, data)

        val chainRoot = optionalChainExpression.expression.optionalChainRootExpression()
        val rootType = chainRoot?.coneTypeOrNull
        if (rootType == null) {
            optionalChainExpression.replaceConeTypeOrNull(
                ConeErrorType(ConeSimpleDiagnostic("optional chain root type is unresolved", DiagnosticKind.InferenceError))
            )
            components.dataFlowAnalyzer.exitOptionalChain(optionalChainExpression)
            return optionalChainExpression
        }

        if (!rootType.isOption) {
            optionalChainExpression.replaceConeTypeOrNull(ConeErrorType(ConeOptionalChainNonOptionalError(rootType)))
            components.dataFlowAnalyzer.exitOptionalChain(optionalChainExpression)
            return optionalChainExpression
        }

        val liftedResultType = liftOptionalChainResultType(optionalChainExpression.expression.coneTypeOrNull)
        optionalChainExpression.replaceConeTypeOrNull(liftedResultType)
        components.dataFlowAnalyzer.exitOptionalChain(optionalChainExpression)
        return optionalChainExpression
    }

    private fun transformThisReceiverExpression(
        thisReceiverExpression: CfirThisReceiverExpression,
        data: ResolutionMode,
    ): CfirExpression {
        thisReceiverExpression.transformAnnotations(transformer, data)

        if (thisReceiverExpression.coneTypeOrNull == null) {
            val thisReference = thisReceiverExpression.calleeReference
            val resultType = components.typeFromCallee(thisReference)
            thisReceiverExpression.replaceConeTypeOrNull(resultType)
            thisReference.replaceDiagnostic((resultType as? ConeErrorType)?.diagnostic)

            if (thisReference.boundSymbol == null && resultType !is ConeErrorType) {
                components.implicitValueStorage[null].singleOrNull()?.let { implicitReceiver ->
                    thisReference.replaceBoundSymbol(implicitReceiver.boundSymbol)
                }
            }
        }

        return thisReceiverExpression
    }

    override fun transformSuperReceiverExpression(
        superReceiverExpression: CfirSuperReceiverExpression,
        data: ResolutionMode,
    ): CfirExpression {
        superReceiverExpression.transformAnnotations(transformer, data)

        val superReference = superReceiverExpression.calleeReference
        val resolvedSuperTypeRef = resolveSuperTypeRef(superReference.superTypeRef)
        if (resolvedSuperTypeRef !== superReference.superTypeRef) {
            superReference.replaceSuperTypeRef(resolvedSuperTypeRef)
        }

        val owner = context.containingRegularClass
        val receiverType = when {
            owner == null -> errorType("`super` is only allowed inside class declarations")
            resolvedSuperTypeRef is CfirResolvedTypeRef -> resolveExplicitSuperReceiverType(owner, resolvedSuperTypeRef)
            else -> resolveImplicitSuperReceiverType(owner)
        }

        superReceiverExpression.replaceConeTypeOrNull(receiverType)
        return superReceiverExpression
    }

    override fun transformLiteralExpression(
        literalExpression: CfirLiteralExpression,
        data: ResolutionMode,
    ): CfirExpression {
        val synthesized = synthesizeLiteralType(literalExpression.kind)
        val expectedType = data.expectedTypeOrNull
        val resolvedType = if (expectedType != null) {
            IdealTypeResolver.resolveIfIdeal(synthesized, expectedType)
        } else {
            synthesized
        }
        literalExpression.replaceConeTypeOrNull(resolvedType)
        components.dataFlowAnalyzer.exitLiteralExpression(literalExpression)
        return literalExpression
    }

    private fun synthesizeLiteralType(kind: CfirLiteralKind): ConeCangJieType = when (kind) {
        CfirLiteralKind.INT     -> ConePrimitiveType.IDEAL_INT
        CfirLiteralKind.FLOAT   -> ConePrimitiveType.IDEAL_FLOAT
        CfirLiteralKind.BOOLEAN -> builtinTypes.boolType
        CfirLiteralKind.RUNE    -> ConePrimitiveType.RUNE
        CfirLiteralKind.STRING  -> stdlibStringType()
        CfirLiteralKind.UNIT    -> builtinTypes.unitType
    }

    // ── Named Access ─────────────────────────────────────────────────────────

    override fun transformNamedAccessExpression(
        namedAccessExpression: CfirNamedAccessExpression,
        data: ResolutionMode,
    ): CfirExpression =
        transformQualifiedAccessExpression(
            qualifiedAccessExpression = namedAccessExpression,
            data = data,
            isUsedAsReceiver = data is ResolutionMode.ReceiverResolution,
            isUsedAsGetClassReceiver = false,
        )

    // ── Qualified Access ──────────────────────────────────────────────────────

    override fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        data: ResolutionMode,
    ): CfirExpression =
        transformQualifiedAccessExpression(
            qualifiedAccessExpression = qualifiedAccessExpression,
            data = data,
            isUsedAsReceiver = data is ResolutionMode.ReceiverResolution,
            isUsedAsGetClassReceiver = false,
        )

    private fun transformQualifiedAccessExpression(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        data: ResolutionMode,
        isUsedAsReceiver: Boolean,
        isUsedAsGetClassReceiver: Boolean,
    ): CfirExpression =
        whileAnalysing(session, qualifiedAccessExpression) {
            val calleeReference = qualifiedAccessExpression.calleeReference
            if (
                isUsedAsReceiver &&
                calleeReference is CfirErrorNamedReference &&
                qualifiedAccessExpression.importedPackageQualifierOrNull(components.file, components.session) != null
            ) {
                qualifiedAccessExpression.replaceCalleeReference(
                    buildNamedReference {
                        source = calleeReference.source
                        name = calleeReference.name
                    }
                )
                qualifiedAccessExpression.replaceConeTypeOrNull(components.session.builtinTypes.unitType)
                return@whileAnalysing qualifiedAccessExpression
            }

            // 本地暂未建模 Kotlin FirResolvedQualifier 节点。
            // 已由 receiver resolution 确认的导入包限定符在后续遍历中保持稳定，
            // 不能再次按普通值表达式解析成 unresolved package name。
            if (
                qualifiedAccessExpression.coneTypeOrNull != null &&
                qualifiedAccessExpression.importedPackageQualifierOrNull(components.file, components.session) != null
            ) {
                return@whileAnalysing qualifiedAccessExpression
            }

            if (qualifiedAccessExpression.coneTypeOrNull != null && calleeReference !is CfirNamedReferenceImpl) {
                return@whileAnalysing qualifiedAccessExpression
            }

            qualifiedAccessExpression.transformAnnotations(transformer, data)
            resolveAccessTypeArguments(qualifiedAccessExpression)

            val resolvedExpression = when (qualifiedAccessExpression.calleeReference) {
                is CfirThisReference -> {
                    if (qualifiedAccessExpression.coneTypeOrNull == null) {
                        val resultType = components.typeFromCallee(qualifiedAccessExpression)
                        qualifiedAccessExpression.replaceConeTypeOrNull(resultType)
                        (qualifiedAccessExpression.calleeReference as? CfirThisReference)
                            ?.replaceDiagnostic((resultType as? ConeErrorType)?.diagnostic)
                    }
                    qualifiedAccessExpression
                }

                is CfirErrorNamedReference -> {
                    if (
                        isUsedAsReceiver &&
                        qualifiedAccessExpression.importedPackageQualifierOrNull(components.file, components.session) != null
                    ) {
                        val errorReference = qualifiedAccessExpression.calleeReference as CfirErrorNamedReference
                        qualifiedAccessExpression.replaceCalleeReference(
                            buildNamedReference {
                                source = errorReference.source
                                name = errorReference.name
                            }
                        )
                        qualifiedAccessExpression.replaceConeTypeOrNull(components.session.builtinTypes.unitType)
                        return@whileAnalysing qualifiedAccessExpression
                    }
                    if (qualifiedAccessExpression.coneTypeOrNull == null) {
                        storeTypeFromCallee(qualifiedAccessExpression)
                    }
                    qualifiedAccessExpression
                }

                is CfirResolvedNamedReference -> {
                    if (qualifiedAccessExpression.coneTypeOrNull == null) {
                        storeTypeFromCallee(qualifiedAccessExpression)
                    }
                    qualifiedAccessExpression
                }

                is CfirNamedReference -> {
                    val transformedCallee = resolveQualifiedAccessAndSelectCandidate(
                        qualifiedAccessExpression = qualifiedAccessExpression,
                        isUsedAsReceiver = isUsedAsReceiver,
                        isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
                        callSite = qualifiedAccessExpression,
                        data = data,
                    )
                    if (transformedCallee is CfirQualifiedAccessExpression) {
                        val candidateReference = transformedCallee.calleeReference as? CfirNamedReferenceWithCandidate
                        if (candidateReference != null) {
                            completeResolvedAccess(transformedCallee, data)
                        } else {
                            when (transformedCallee.calleeReference) {
                                is CfirResolvedNamedReference,
                                is CfirErrorNamedReference,
                                is CfirThisReference,
                                -> {
                                    if (transformedCallee.coneTypeOrNull == null) {
                                        storeTypeFromCallee(transformedCallee)
                                    }
                                    transformedCallee
                                }

                                else -> transformedCallee
                            }
                        }
                    } else {
                        transformedCallee
                    }
                }

                else -> {
                    qualifiedAccessExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
                    if (qualifiedAccessExpression.coneTypeOrNull == null) {
                        qualifiedAccessExpression.replaceConeTypeOrNull(
                            ConeErrorType(ConeSimpleDiagnostic("non-name reference", DiagnosticKind.Other))
                        )
                    }
                    qualifiedAccessExpression
                }
            }
            components.dataFlowAnalyzer.exitQualifiedAccessExpression(qualifiedAccessExpression)
            resolvedExpression
        }

    // ── Function Call ─────────────────────────────────────────────────────────

    override fun transformFunctionCall(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirExpression =
        transformFunctionCallInternal(functionCall, data, CallResolutionMode.REGULAR)

    override fun transformIncrementDecrementExpression(
        incrementDecrementExpression: CfirIncrementDecrementExpression,
        data: ResolutionMode,
    ): CfirExpression =
        whileAnalysing(session, incrementDecrementExpression) {
            incrementDecrementExpression.transformAnnotations(transformer, data)
            incrementDecrementExpression.transformExpression(transformer, ResolutionMode.ContextIndependent)

            val operatorCall = buildFunctionCall {
                source = incrementDecrementExpression.operationSource ?: incrementDecrementExpression.source
                calleeReference = buildNamedReference {
                    source = incrementDecrementExpression.operationSource ?: incrementDecrementExpression.source
                    name = incrementDecrementExpression.operationName
                }
                argumentList = buildArgumentList()
                explicitReceiver = incrementDecrementExpression.expression
                origin = CfirFunctionCallOrigin.Operator
            }
            val resolvedOperatorCall = transformFunctionCallInternal(operatorCall, data, CallResolutionMode.REGULAR)
            incrementDecrementExpression.replaceConeTypeOrNull(
                resolvedOperatorCall.coneTypeOrNull ?: builtinTypes.unitType
            )
            incrementDecrementExpression
        }

    internal fun transformFunctionCallInternal(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
        callResolutionMode: CallResolutionMode,
    ): CfirExpression =
        whileAnalysing(session, functionCall) {
            if (functionCall.origin.isConstructorDelegation) {
                return@whileAnalysing transformConstructorDelegationCall(functionCall, data)
            }
            if (functionCall.origin == CfirFunctionCallOrigin.MockIntrinsic) {
                return@whileAnalysing transformMockIntrinsicCall(functionCall, data)
            }

            val calleeReference = functionCall.calleeReference
            if (
                (calleeReference is CfirResolvedNamedReference || calleeReference is CfirErrorNamedReference) &&
                functionCall.coneTypeOrNull == null
            ) {
                storeTypeFromCallee(functionCall)
            }
            if (calleeReference is CfirNamedReferenceWithCandidate) return@whileAnalysing functionCall
            if (calleeReference !is CfirNamedReferenceImpl) {
                if (calleeReference !is CfirResolvedNamedReference) {
                    functionCall.transformChildren(transformer, ResolutionMode.ContextIndependent)
                }
                return@whileAnalysing functionCall
            }

            functionCall.transformAnnotations(transformer, data)
            resolveAccessTypeArguments(functionCall)

            val choosingOptionForAugmentedAssignment = callResolutionMode == CallResolutionMode.OPTION_FOR_AUGMENTED_ASSIGNMENT
            val withTransformedArguments = if (!choosingOptionForAugmentedAssignment) {
                components.dataFlowAnalyzer.enterCallArguments(functionCall, functionCall.argumentList.arguments)

                val withResolvedExplicitReceiver = when (callResolutionMode) {
                    CallResolutionMode.PROVIDE_DELEGATE -> functionCall
                    else -> transformExplicitReceiverOf(functionCall)
                }

                withResolvedExplicitReceiver.also {
                    components.dataFlowAnalyzer.exitCallExplicitReceiver()
                    it.replaceArgumentList(
                        it.argumentList.transform(transformer, ResolutionMode.ContextDependent)
                    )
                    components.dataFlowAnalyzer.exitCallArguments()
                }
            } else {
                functionCall
            }

            tryResolveBuiltinOperatorCall(withTransformedArguments, data)?.let { builtinOperatorCall ->
                return@whileAnalysing builtinOperatorCall
            }

            // 保存原始引用，resolveCallAndSelectCandidate 会原地修改 calleeReference
            val originalCalleeReference = withTransformedArguments.calleeReference
            val resolvedCall = callResolver.resolveCallAndSelectCandidate(withTransformedArguments, data)
            val callForCompletion = if (!choosingOptionForAugmentedAssignment) {
                tryResolveImplicitInvokeCall(originalCalleeReference, withTransformedArguments, resolvedCall, data) ?: resolvedCall
            } else {
                resolvedCall
            }

            if (!choosingOptionForAugmentedAssignment) {
                components.dataFlowAnalyzer.enterFunctionCall(callForCompletion)
            }

            val result = components.callCompleter.completeCall(
                callForCompletion,
                data,
                skipEvenPartialCompletion = choosingOptionForAugmentedAssignment,
            )

            if (!choosingOptionForAugmentedAssignment) {
                components.dataFlowAnalyzer.exitFunctionCall(result, data.forceFullCompletion)
            }

            result
        }

    private fun tryResolveBuiltinOperatorCall(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirFunctionCall? {
        if (functionCall.origin != CfirFunctionCallOrigin.Operator) return null
        val callee = functionCall.calleeReference as? CfirNamedReference ?: return null
        val explicitReceiver = functionCall.explicitReceiver ?: return null
        val receiverType = explicitReceiver.coneTypeOrNull ?: return null
        val argumentTypes = functionCall.argumentList.arguments.map { argument ->
            argument.coneTypeOrNull ?: return null
        }
        (receiverType.propagatedErrorTypeOrNull() ?: argumentTypes.firstNotNullOfOrNull { argumentType ->
            argumentType.propagatedErrorTypeOrNull()
        })?.let { propagatedErrorType ->
            functionCall.replaceConeTypeOrNull(propagatedErrorType)
            return functionCall
        }

        val builtinMatch = CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
            callee.name,
            receiverType,
            argumentTypes,
        )
        if (builtinMatch == null) {
            if (!CfirBuiltinOperatorResolver.canDiagnoseInvalidPrimitiveOperator(callee.name, receiverType, argumentTypes)) {
                return null
            }
            val operatorToken = OperatorNameConventions.TOKENS_BY_OPERATOR_NAME[callee.name]
            val diagnostic = ConeUnresolvedNameError(
                callee.name,
                operatorToken,
                receiverType,
                argumentTypes,
            )
            functionCall.replaceCalleeReference(
                buildErrorNamedReference {
                    source = callee.source
                    name = callee.name
                    this.diagnostic = diagnostic
                },
            )
            functionCall.replaceConeTypeOrNull(ConeErrorType(diagnostic))
            return functionCall
        }
        val returnType = data.expectedTypeOrNull?.let { expectedType ->
            IdealTypeResolver.resolveIfIdeal(builtinMatch.returnType, expectedType)
        } ?: builtinMatch.returnType
        functionCall.replaceConeTypeOrNull(returnType)
        return functionCall
    }

    /**
     * 构造器 delegation 调用不参与普通 tower resolve。
     *
     * `this(...)` / `super(...)` 的候选筛选、循环检测、父类构造器要求等
     * 都属于 constructor 语义，由专门的 declaration / expression checker 负责。
     * 这里仅解析其实参表达式，并把整条调用标记为 `Unit`，避免它先退化成普通 unresolved call。
     */
    private fun transformConstructorDelegationCall(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirFunctionCall {
        functionCall.transformAnnotations(transformer, data)
        resolveAccessTypeArguments(functionCall)

        components.dataFlowAnalyzer.enterCallArguments(functionCall, functionCall.argumentList.arguments)
        functionCall.replaceArgumentList(
            functionCall.argumentList.transform(transformer, ResolutionMode.ContextIndependent)
        )
        components.dataFlowAnalyzer.exitCallArguments()

        functionCall.replaceConeTypeOrNull(builtinTypes.unitType)
        return functionCall
    }

    /**
     * mock intrinsic 调用不能退化成普通 unresolved call。
     *
     * 官方编译器会先把这类调用识别成 intrinsic call，再由 test/mock 语义阶段处理。
     * 本地先在 resolve 阶段保留其特殊 owner：
     * 1. 解析类型参数和实参，保证 checker 拿到稳定的目标类型；
     * 2. 不再让普通 call resolver 产出 `UNRESOLVED_REFERENCE` 噪声。
     */
    private fun transformMockIntrinsicCall(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirFunctionCall {
        functionCall.transformAnnotations(transformer, data)
        resolveAccessTypeArguments(functionCall)

        components.dataFlowAnalyzer.enterCallArguments(functionCall, functionCall.argumentList.arguments)
        val withResolvedExplicitReceiver = transformExplicitReceiverOf(functionCall).also {
            components.dataFlowAnalyzer.exitCallExplicitReceiver()
            it.replaceArgumentList(
                it.argumentList.transform(transformer, ResolutionMode.ContextDependent)
            )
            components.dataFlowAnalyzer.exitCallArguments()
        }

        if (withResolvedExplicitReceiver.coneTypeOrNull == null) {
            withResolvedExplicitReceiver.replaceConeTypeOrNull(
                withResolvedExplicitReceiver.typeArguments.firstOrNull()?.coneTypeOrNull
            )
        }

        return withResolvedExplicitReceiver
    }

    private fun tryResolveImplicitInvokeCall(
        originalCalleeReference: CfirReference,
        originalCall: CfirFunctionCall,
        resolvedCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirFunctionCall? {
        val diagnostic = (resolvedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic
        val noArgEnumValueCalledWithArguments = diagnostic.isNoArgEnumValueCalledWithArguments(originalCall)
        if (originalCall.explicitReceiver != null && !noArgEnumValueCalledWithArguments) return null

        val shouldPreserveOriginalDiagnostic =
            diagnostic !is ConeUnresolvedNameError && !noArgEnumValueCalledWithArguments
        val canTryImplicitInvoke = when (diagnostic) {
            is ConeUnresolvedNameError -> true
            is ConeInapplicableCandidateError -> diagnostic.candidateSymbol is CfirEnumConstructorSymbol
            is ConeConstraintSystemHasContradiction -> diagnostic.candidateSymbol is CfirEnumConstructorSymbol
            is ConeAmbiguityError -> !diagnostic.applicability.isSuccess &&
                    diagnostic.candidateSymbols.all { it is CfirEnumConstructorSymbol }
            else -> false
        }
        if (!canTryImplicitInvoke) return null

        val originalCallee = originalCalleeReference as? CfirNamedReferenceImpl ?: return null
        if (originalCallee.name == OperatorNameConventions.INVOKE) return null

        val resolvedAccess = callResolver.resolveNamedValueAccessAndSelectCandidate(
            qualifiedAccess = buildNamedAccessExpression {
                source = originalCall.source
                calleeReference = buildNamedReference {
                    source = originalCallee.source
                    name = originalCallee.name
                }
                explicitReceiver = originalCall.explicitReceiver
                typeArguments.addAll(originalCall.typeArguments)
            },
            isUsedAsReceiver = true,
            isUsedAsGetClassReceiver = false,
            callSite = originalCall,
            resolutionMode = data,
        ) as? CfirQualifiedAccessExpression ?: return null
        // 构造器匹配已有诊断时，只有裸 enum value 访问本身成功，才继续尝试 `operator ()`。
        // 否则保留原构造器参数映射诊断，避免被裸访问的派生错误覆盖。
        if (
            shouldPreserveOriginalDiagnostic &&
            (resolvedAccess.calleeReference as? CfirDiagnosticHolder)?.diagnostic != null
        ) {
            return null
        }

        when (resolvedAccess.calleeReference) {
            is CfirResolvedNamedReference,
            is CfirNamedReferenceWithCandidate,
            -> Unit

            else -> return null
        }

        val invokeCall = buildFunctionCall {
            source = originalCall.source
            calleeReference = buildNamedReference {
                source = originalCallee.source
                name = OperatorNameConventions.INVOKE
            }
            explicitReceiver = resolvedAccess
            argumentList = buildArgumentList {
                arguments.addAll(originalCall.argumentList.arguments)
            }
            typeArguments.addAll(originalCall.typeArguments)
            origin = originalCall.origin
        }

        val invokeResult = callResolver.resolveCallAndSelectCandidate(invokeCall, data)
            .takeUnless { (it.calleeReference as? CfirDiagnosticHolder)?.diagnostic is ConeUnresolvedNameError }

        if (invokeResult != null) return invokeResult

        if (shouldPreserveOriginalDiagnostic) return null

        // 变量已解析但类型上没有 invoke 操作符 → 报告专用诊断
        val receiverType = resolvedAccess.coneTypeOrNull
        if (receiverType != null && receiverType !is ConeErrorType) {
            val diagnosticSource = originalCallee.source.enumValueAccessSource(originalCall.explicitReceiver?.source)
            resolvedCall.replaceCalleeReference(
                buildErrorNamedReference {
                    source = diagnosticSource
                    name = originalCallee.name
                    this.diagnostic = ConeNoMatchingInvokeOperatorError(originalCallee.name, receiverType)
                }
            )
            return resolvedCall
        }

        return null
    }

    private fun CjSourceElement?.enumValueAccessSource(explicitReceiverSource: CjSourceElement?): CjSourceElement? {
        if (this == null || explicitReceiverSource == null) return this
        return realElement().fakeElement(
            CjFakeSourceElementKind.ReferenceInAtomicQualifiedAccess,
            CjSourceElementOffsetStrategy.Custom.Delegated(
                startOffsetAnchor = explicitReceiverSource,
                endOffsetAnchor = this,
            ),
        )
    }

    /**
     * 仓颉无参 enum constructor 是 enum 值，不是可调用声明。
     * 当源码写成 `Enum.Entry(args)` 时，应先把 `Entry` 解析成值，再走 `invoke` 失败；
     * 有参 enum constructor 仍保留参数映射诊断（缺参、参数类型错误等）。
     */
    private fun ConeDiagnostic?.isNoArgEnumValueCalledWithArguments(originalCall: CfirFunctionCall): Boolean {
        if (originalCall.argumentList.arguments.isEmpty()) return false
        fun AbstractCandidate.isNoArgEnumConstructorCandidate(): Boolean {
            val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return false
            return enumConstructor.valueParameters.isEmpty()
        }

        return when (this) {
            is ConeInapplicableCandidateError -> candidate.isNoArgEnumConstructorCandidate()
            is ConeConstraintSystemHasContradiction -> candidate.isNoArgEnumConstructorCandidate()
            is ConeAmbiguityError -> !applicability.isSuccess && candidates.all { it.isNoArgEnumConstructorCandidate() }
            else -> false
        }
    }

    private fun storeTypeFromCallee(functionCall: CfirFunctionCall) {
        storeTypeFromCallee(functionCall as CfirQualifiedAccessExpression)
    }

    internal fun storeTypeFromCallee(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        @Suppress("UNUSED_PARAMETER") isLhsOfAssignment: Boolean = false,
    ) {
        qualifiedAccessExpression.replaceConeTypeOrNull(components.typeFromCallee(qualifiedAccessExpression))
    }

    fun <Q : CfirQualifiedAccessExpression> transformExplicitReceiverOf(qualifiedAccessExpression: Q): Q {
        if (qualifiedAccessExpression.explicitReceiver == null) return qualifiedAccessExpression
        qualifiedAccessExpression.transformExplicitReceiver(
            transformer,
            qualifiedAccessExpression.explicitReceiverResolutionMode(),
        )
        return qualifiedAccessExpression
    }

    /**
     * 函数类型 `invoke` 的接收者可以是另一个仍待外层实参约束的调用。
     *
     * 这类 receiver 若按独立 `ReceiverResolution` 强制完成，会提前把只出现在
     * 返回函数类型中的泛型变量报告为无法推断；实际语义需要由外层 `invoke`
     * 的实参继续约束它们。
     */
    private fun CfirQualifiedAccessExpression.explicitReceiverResolutionMode(): ResolutionMode {
        val callee = calleeReference as? CfirNamedReference ?: return ResolutionMode.ReceiverResolution
        if (callee.name != OperatorNameConventions.INVOKE) return ResolutionMode.ReceiverResolution
        return if (explicitReceiver?.isNestedCallInvokeReceiver() == true) {
            ResolutionMode.ContextDependent
        } else {
            ResolutionMode.ReceiverResolution
        }
    }

    private fun CfirExpression.isNestedCallInvokeReceiver(): Boolean = when (this) {
        is CfirFunctionCall -> true
        is CfirWrappedExpression -> expression.isNestedCallInvokeReceiver()
        else -> false
    }

    protected open fun resolveQualifiedAccessAndSelectCandidate(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        isUsedAsGetClassReceiver: Boolean,
        callSite: CfirElement,
        data: ResolutionMode,
    ): CfirExpression {
        return callResolver.resolveNamedValueAccessAndSelectCandidate(
            qualifiedAccess = qualifiedAccessExpression,
            isUsedAsReceiver = isUsedAsReceiver,
            isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
            callSite = callSite,
            resolutionMode = data,
        )
    }

    internal enum class CallResolutionMode {
        REGULAR,

        /**
         * For PROVIDE_DELEGATE we skip transforming explicit receiver of the call since it's already been resolved
         * at [FirDeclarationsResolveTransformer.transformPropertyAccessorsWithDelegate]
         */
        PROVIDE_DELEGATE,

        /**
         * When we're resolving an operator like `a += b` we try to resolve it with different options of desugaring like
         * `a = a.plus(b)` and `a.plusAssign(b)` until find something that looks successful.
         * But at this stage, we skip transformation of receiver, arguments and skip completion in any form.
         */
        OPTION_FOR_AUGMENTED_ASSIGNMENT,
    }

    // ── Block ─────────────────────────────────────────────────────────────────

    override fun transformBlock(block: CfirBlock, data: ResolutionMode): CfirExpression {
        components.dataFlowAnalyzer.enterBlock(block)
        val statements = block.statements as? MutableList<CfirStatement>
            ?: error("CfirBlock statements must be mutable during body resolve")
        val lastIndex = statements.lastIndex

        /**
         * 对齐 Kotlin `FirExpressionsResolveTransformer.transformBlockInCurrentScope`：
         * - 非尾语句始终按 `ContextIndependent` 解析；
         * - 尾语句继承外层 `ResolutionMode`；
         * - 若外层带 expected type，则显式标记 `lastStatementInBlock`。
         *
         * 这样 try/catch/if/match 等通过 block 承载结果值的路径，才能把 expected
         * type 准确传到尾表达式，而不是被 block 这一层截断。
         */
        for (index in statements.indices) {
            val statementMode = when {
                index != lastIndex -> ResolutionMode.ContextIndependent
                data is ResolutionMode.WithExpectedType -> data.copy(lastStatementInBlock = true)
                else -> data
            }
            statements[index] = statements[index].transform(transformer, statementMode)
        }
        block.transformOtherChildren(transformer, data)
        val lastExpr = block.statements.lastOrNull()
        block.replaceConeTypeOrNull(
            if (lastExpr is CfirExpression) lastExpr.coneTypeOrNull ?: builtinTypes.unitType
            else builtinTypes.unitType
        )
        components.dataFlowAnalyzer.exitBlock(block)
        return block
    }

    // ── Match ─────────────────────────────────────────────────────────────────

    override fun transformMatchExpression(
        matchExpression: CfirMatchExpression,
        data: ResolutionMode,
    ): CfirExpression {
        components.dataFlowAnalyzer.enterMatchExpression(matchExpression)
        matchExpression.subject?.resolveIndependently()
        val subjectType = matchExpression.subject?.coneTypeOrNull
        val subjectErrorType = subjectType as? ConeErrorType
        if (matchExpression.subject != null && subjectErrorType != null) {
            matchExpression.replaceExhaustiveness(CfirMatchExhaustivenessStatus.Unknown)
            matchExpression.replaceConeTypeOrNull(
                ConeErrorType(ConeUnreportedDuplicateDiagnostic(subjectErrorType.diagnostic))
            )
            components.dataFlowAnalyzer.exitMatchExpression(
                matchExpression,
                syntheticElseDecision = components.dataFlowAnalyzer.matchSyntheticElseDecision(matchExpression),
                callCompleted = data.forceFullCompletion,
            )
            return matchExpression
        }

        val branchResolutionMode = (data as? ResolutionMode.WithExpectedType)
            ?.takeUnless { it.fromCast }
            ?.copy(forceFullCompletion = false)
            ?: ResolutionMode.ContextDependent
        val branchTypes = matchExpression.branches.map { branch ->
            resolveBranch(branch, subjectType, branchResolutionMode)
        }

        matchExpression.replaceExhaustiveness(resolveMatchExhaustiveness(matchExpression))
        matchExpression.replaceConeTypeOrNull(computeMatchResultType(branchTypes, data.expectedTypeOrNull))
        components.dataFlowAnalyzer.exitMatchExpression(
            matchExpression,
            syntheticElseDecision = components.dataFlowAnalyzer.matchSyntheticElseDecision(matchExpression),
            callCompleted = data.forceFullCompletion,
        )
        return matchExpression
    }

    /**
     * BODY_RESOLVE 阶段将 shared semantics 的穷尽性结论正式回写到 tree。
     *
     * 若 shared analyzer 暂时无法给出稳定结论，则保持 `Unknown`，
     * 让 CFG 走“保守地补 synthetic else”而不是把内部分析失败固化成 tree-level Error。
     */
    private fun resolveMatchExhaustiveness(matchExpression: CfirMatchExpression): CfirMatchExhaustivenessStatus {
        return when (val result = ExhaustivenessAnalyzer.checkMatch(matchExpression, session)) {
            ExhaustivenessResult.Exhaustive -> CfirMatchExhaustivenessStatus.Exhaustive(
                source = CfirMatchExhaustivenessStatus.Source.BodyResolve,
            )

            is ExhaustivenessResult.NonExhaustive -> CfirMatchExhaustivenessStatus.NonExhaustive(
                missingCaseTexts = result.getMissingPatternTexts(),
                source = CfirMatchExhaustivenessStatus.Source.BodyResolve,
            )

            is ExhaustivenessResult.Error,
            ExhaustivenessResult.Skipped,
            -> CfirMatchExhaustivenessStatus.Unknown
        }
    }

    private fun resolveBranch(
        branch: CfirMatchBranch,
        subjectType: ConeCangJieType?,
        bodyResolutionMode: ResolutionMode,
    ): ConeCangJieType {
        return withNewLocalScope {
            components.dataFlowAnalyzer.enterMatchBranchCondition(branch)
            branch.transformPattern(transformer, ResolutionMode.ContextIndependent)
            if (branch is org.cangnova.cangjie.cfir.expressions.impl.CfirMatchBranchImpl) {
                branch.pattern = resolveDeferredMatchPattern(branch.pattern, subjectType)
            }
            resolvePatternBindingTypes(branch.pattern, subjectType, specificTypeResolverTransformer)
            registerPatternBindings(branch.pattern)

            branch.transformGuard(transformer, ResolutionMode.ContextIndependent)
            components.dataFlowAnalyzer.exitMatchBranchCondition(branch)
            branch.transformBody(transformer, bodyResolutionMode)
            components.dataFlowAnalyzer.exitMatchBranchResult(branch)

            val bodyType = branch.body.coneTypeOrNull ?: builtinTypes.unitType
            branch.replaceConeTypeOrNull(bodyType)
            bodyType
        }
    }

    /**
     * 对齐官方 `VarOrEnumPattern` 的延迟决议：
     * 先保留裸名字歧义，进入 body resolve 后再根据当前作用域中是否可见 enum constructor
     * 决定它究竟是 enum pattern 还是 binding pattern。
     */
    private fun resolveDeferredMatchPattern(
        pattern: CfirPattern,
        expectedType: ConeCangJieType?,
    ): CfirPattern {
        return when (pattern) {
            is CfirVarOrEnumPattern -> resolveVarOrEnumPattern(pattern, expectedType)
            is CfirBindingPattern -> {
                val nestedPattern = pattern.nestedPattern ?: return pattern
                val nestedExpectedType = (pattern.typeRef as? CfirResolvedTypeRef)?.coneType ?: expectedType
                val resolvedNestedPattern = resolveDeferredMatchPattern(nestedPattern, nestedExpectedType)
                if (resolvedNestedPattern === nestedPattern) pattern else buildBindingPatternCopy(pattern) {
                    this.nestedPattern = resolvedNestedPattern
                }
            }

            is CfirTuplePattern -> {
                val tupleType = expectedType as? ConeTupleType
                val resolvedElements = pattern.elements.mapIndexed { index, element ->
                    resolveDeferredMatchPattern(element, tupleType?.elementTypes?.getOrNull(index))
                }
                if (resolvedElements.zip(pattern.elements).all { (resolved, original) -> resolved === original }) {
                    pattern
                } else {
                    buildTuplePatternCopy(pattern) {
                        elements.clear()
                        elements.addAll(resolvedElements)
                    }
                }
            }

            is CfirEnumPattern -> {
                val argumentTypes = resolveEnumArgumentTypesForDeferredPattern(pattern, expectedType)
                val resolvedArguments = pattern.arguments.mapIndexed { index, argument ->
                    resolveDeferredMatchPattern(argument, argumentTypes.getOrNull(index))
                }
                if (resolvedArguments.zip(pattern.arguments).all { (resolved, original) -> resolved === original }) {
                    pattern
                } else {
                    buildEnumPatternCopy(pattern) {
                        arguments.clear()
                        arguments.addAll(resolvedArguments)
                    }
                }
            }

            is CfirOrPattern -> {
                val resolvedAlternatives = pattern.alternatives.map { alternative ->
                    resolveDeferredMatchPattern(alternative, expectedType)
                }
                if (resolvedAlternatives.zip(pattern.alternatives).all { (resolved, original) -> resolved === original }) {
                    pattern
                } else {
                    buildOrPattern {
                        source = pattern.source
                        alternatives.clear()
                        alternatives.addAll(resolvedAlternatives)
                    }
                }
            }

            else -> pattern
        }
    }

    private fun resolveVarOrEnumPattern(
        pattern: CfirVarOrEnumPattern,
        expectedType: ConeCangJieType?,
    ): CfirPattern {
        val expectedEnumConstructorReference = resolveExpectedEnumConstructorReferenceOrNull(pattern, expectedType)
        if (expectedEnumConstructorReference != null) {
            return buildEnumPattern {
                source = pattern.source
                constructorReference = expectedEnumConstructorReference
            }
        }

        val enumConstructorReference = resolveEnumConstructorReferenceOrNull(pattern)
        if (enumConstructorReference != null) {
            return buildEnumPattern {
                source = pattern.source
                constructorReference = enumConstructorReference
            }
        }

        return buildBindingPattern {
            source = pattern.source
            name = pattern.name
            bindingVariable = pattern.bindingVariable
        }
    }

    private fun resolveExpectedEnumConstructorReferenceOrNull(
        pattern: CfirVarOrEnumPattern,
        expectedType: ConeCangJieType?,
    ): CfirReference? {
        val enumType = expectedType?.expandedPatternEnumType(session) ?: return null
        val enumDeclaration = session.symbolProvider.getClassLikeSymbolByClassId(enumType.classId)?.cfir as? CfirEnum
            ?: return null
        val enumConstructor = enumDeclaration.declarations
            .filterIsInstance<CfirEnumConstructor>()
            .firstOrNull { constructor -> constructor.name == pattern.name && constructor.payloadArity() == 0 }
            ?: return null

        return buildResolvedNamedReference {
            source = pattern.source
            name = pattern.name
            resolvedSymbol = enumConstructor.symbol
        }
    }

    private fun resolveEnumArgumentTypesForDeferredPattern(
        pattern: CfirEnumPattern,
        expectedType: ConeCangJieType?,
    ): List<ConeCangJieType> {
        val enumType = expectedType?.expandedPatternEnumType(session) ?: return emptyList()
        val enumDeclaration = session.symbolProvider.getClassLikeSymbolByClassId(enumType.classId)?.cfir as? CfirEnum
            ?: return emptyList()
        val constructorName = when (val reference = pattern.constructorReference) {
            is CfirResolvedNamedReference -> reference.name
            is CfirNamedReference -> reference.name
            else -> return emptyList()
        }
        val enumConstructor = enumDeclaration.declarations
            .filterIsInstance<CfirEnumConstructor>()
            .firstOrNull { constructor -> constructor.name == constructorName && constructor.payloadArity() == pattern.arguments.size }
            ?: return emptyList()

        return enumConstructor.substitutedPayloadParameterTypes(enumDeclaration, enumType)
    }

    private fun resolveEnumConstructorReferenceOrNull(pattern: CfirVarOrEnumPattern): CfirReference? {
        val temporaryAccess = buildNamedAccessExpression {
            source = pattern.source
            calleeReference = buildNamedReference {
                source = pattern.source
                name = pattern.name
            }
        }
        val resolvedAccess = callResolver.resolveVariableAccessAndSelectCandidate(
            qualifiedAccess = temporaryAccess,
            isUsedAsReceiver = false,
            isUsedAsGetClassReceiver = false,
            callSite = temporaryAccess,
            resolutionMode = ResolutionMode.ContextIndependent,
        ) as? CfirQualifiedAccessExpression ?: return null
        val resolvedReference = resolvedAccess.calleeReference

        return when {
            resolvedReference is CfirResolvedNamedReference && resolvedReference.resolvedSymbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor ->
                resolvedReference

            resolvedReference is CfirResolvedAppliedCallableReference && resolvedReference.resolvedSymbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor ->
                buildResolvedNamedReference {
                    source = resolvedReference.source ?: pattern.source
                    name = resolvedReference.name
                    resolvedSymbol = resolvedReference.resolvedSymbol
                }

            resolvedReference is CfirNamedReferenceWithCandidate &&
                    resolvedReference.candidate.symbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor ->
                buildResolvedNamedReference {
                    source = resolvedReference.source ?: pattern.source
                    name = resolvedReference.name
                    resolvedSymbol = resolvedReference.candidate.symbol
                }

            else -> null
        }
    }

    private fun computeMatchResultType(
        branchTypes: List<ConeCangJieType>,
        expectedType: ConeCangJieType?,
    ): ConeCangJieType {
        if (branchTypes.isEmpty()) return builtinTypes.unitType

        branchTypes.firstOrNull { it is ConeErrorType }?.let { errorType ->
            return ConeErrorType(ConeUnreportedDuplicateDiagnostic((errorType as ConeErrorType).diagnostic))
        }

        val normalizedBranchTypes = branchTypes.map { branchType ->
            IdealTypeResolver.resolveIfIdeal(branchType, expectedType)
        }
        val first = normalizedBranchTypes.first()
        if (normalizedBranchTypes.all { it == first }) return first

        /**
         * 官方 `ChkMatchExprSetTy` 在外层存在 target type 且任一分支类型等于 target 时，
         * 直接把整个 match 视为 target，避免 Join 得到比上下文更宽的可见公共父类型。
         */
        if (expectedType != null && normalizedBranchTypes.any { branchType ->
                AbstractTypeChecker.equalTypes(session.typeContext, branchType, expectedType)
            }
        ) {
            return expectedType
        }

        return commonSupertype(normalizedBranchTypes)
    }

    // ── If ────────────────────────────────────────────────────────────────────

    override fun transformIfExpression(
        ifExpression: CfirIfExpression,
        data: ResolutionMode,
    ): CfirExpression {
        val branchResolutionMode = (data as? ResolutionMode.WithExpectedType)
            ?.takeUnless { it.fromCast }
            ?.copy(forceFullCompletion = false)
            ?: ResolutionMode.ContextDependent

        if (ifExpression.condition.containsLetPatternCondition()) {
            withNewLocalScope {
                resolveConditionWithPatternBindings(ifExpression.condition)
                ifExpression.transformThenBranch(transformer, branchResolutionMode)
            }
        } else {
            ifExpression.transformCondition(transformer, withExpectedType(builtinTypes.boolType))
            ifExpression.transformThenBranch(transformer, branchResolutionMode)
        }
        ifExpression.transformElseBranch(transformer, branchResolutionMode)

        val thenType = ifExpression.thenBranch.coneTypeOrNull
        val elseType = ifExpression.elseBranch?.coneTypeOrNull
        val branchErrorType = listOfNotNull(thenType as? ConeErrorType, elseType as? ConeErrorType)
            .firstOrNull()
        val mergedType = when {
            // 分支错误已经由分支表达式自身报告；if 只传播 InvalidTy 语义，
            // 避免把同一个分支诊断重新挂到组合表达式上。
            branchErrorType != null -> ConeErrorType(ConeUnreportedDuplicateDiagnostic(branchErrorType.diagnostic))
            thenType == null -> elseType ?: builtinTypes.unitType
            elseType == null -> builtinTypes.unitType
            thenType == elseType -> thenType
            else -> commonSupertype(listOf(thenType, elseType))
        }
        ifExpression.replaceConeTypeOrNull(
            IdealTypeResolver.resolveIfIdeal(mergedType, data.expectedTypeOrNull)
        )
        return ifExpression
    }

    override fun transformLetPatternExpression(
        letPatternExpression: CfirLetPatternExpression,
        data: ResolutionMode,
    ): CfirExpression {
        resolveLetPatternExpression(letPatternExpression, registerBindings = false)
        return letPatternExpression
    }

    private fun CfirExpression.containsLetPatternCondition(): Boolean = when (this) {
        is CfirLetPatternExpression -> true
        is CfirBinaryOp -> (kind == CfirBinaryOpKind.AND || kind == CfirBinaryOpKind.OR) &&
                (left.containsLetPatternCondition() || right.containsLetPatternCondition())
        else -> false
    }

    private fun resolveConditionWithPatternBindings(condition: CfirExpression): CfirExpression {
        return when (condition) {
            is CfirLetPatternExpression -> {
                resolveLetPatternExpression(condition, registerBindings = true)
                condition
            }

            is CfirBinaryOp if condition.kind == CfirBinaryOpKind.AND || condition.kind == CfirBinaryOpKind.OR -> {
                condition.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
                resolveConditionWithPatternBindings(condition.left)
                resolveConditionWithPatternBindings(condition.right)
                condition.replaceConeTypeOrNull(builtinTypes.boolType)
                condition
            }

            else -> condition.transform<CfirExpression, ResolutionMode>(transformer, withExpectedType(builtinTypes.boolType))
        }
    }

    private fun resolveLetPatternExpression(
        letPatternExpression: CfirLetPatternExpression,
        registerBindings: Boolean,
    ) {
        letPatternExpression.transformInitializer(transformer, ResolutionMode.ContextIndependent)
        letPatternExpression.transformPattern(transformer, ResolutionMode.ContextIndependent)
        if (letPatternExpression is org.cangnova.cangjie.cfir.expressions.impl.CfirLetPatternExpressionImpl) {
            letPatternExpression.pattern = resolveDeferredMatchPattern(
                pattern = letPatternExpression.pattern,
                expectedType = letPatternExpression.initializer.coneTypeOrNull,
            )
        }
        resolvePatternBindingTypes(
            pattern = letPatternExpression.pattern,
            expectedType = letPatternExpression.initializer.coneTypeOrNull,
            typeResolver = specificTypeResolverTransformer,
        )
        if (registerBindings) {
            registerPatternBindings(letPatternExpression.pattern)
        }
        letPatternExpression.replaceConeTypeOrNull(builtinTypes.boolType)
    }

    // ── Return / Throw ────────────────────────────────────────────────────────

    override fun transformReturnExpression(
        returnExpression: CfirReturnExpression,
        data: ResolutionMode,
    ): CfirExpression {
        components.dataFlowAnalyzer.enterJump(returnExpression)
        val expectedReturnTypeRef = (returnExpression.target.labeledElement.returnTypeRef as? CfirResolvedTypeRef)
            ?.takeUnless { it.coneType is ConeErrorType }
        val resultResolutionMode = expectedReturnTypeRef?.let(::withExpectedType) ?: ResolutionMode.ContextIndependent
        returnExpression.transformResult(transformer, resultResolutionMode)
        returnExpression.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
        returnExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        components.dataFlowAnalyzer.exitJump(returnExpression)
        return returnExpression
    }

    override fun transformLoopJump(
        jumpExpression: CfirLoopJump,
        data: ResolutionMode,
    ): CfirExpression {
        return transformLoopJumpLike(jumpExpression, data)
    }

    override fun transformBreakExpression(
        breakExpression: CfirBreakExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return transformLoopJumpLike(breakExpression, data)
    }

    override fun transformContinueExpression(
        continueExpression: CfirContinueExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return transformLoopJumpLike(continueExpression, data)
    }

    /**
     * loop jump 的公共 resolve 入口。
     *
     * Kotlin FIR 的基础 transformer 不会把 break/continue 自动委派到 loop-jump 抽象层，
     * 因此需要由具体节点 override 显式复用这段处理逻辑。
     */
    private fun transformLoopJumpLike(
        jumpExpression: CfirLoopJump,
        data: ResolutionMode,
    ): CfirExpression {
        jumpExpression.transformAnnotations(transformer, data)
        if (jumpExpression.coneTypeOrNull == null) {
            jumpExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        }
        components.dataFlowAnalyzer.exitJump(jumpExpression)
        return jumpExpression
    }

    override fun transformThrowExpression(
        throwExpression: CfirThrowExpression,
        data: ResolutionMode,
    ): CfirExpression {
        throwExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        throwExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        components.dataFlowAnalyzer.exitThrowException(throwExpression)
        return throwExpression
    }

    override fun transformPerformExpression(
        performExpression: CfirPerformExpression,
        data: ResolutionMode,
    ): CfirExpression {
        performExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)

        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.EffectHandlers)) {
            performExpression.replaceConeTypeOrNull(
                ConeErrorType(ConeEffectsFeatureDisabledError("perform"))
            )
            return performExpression
        }

        val commandSupertype = findCommandSupertype(performExpression.expression.coneTypeOrNull)
        performExpression.replaceConeTypeOrNull(
            commandSupertype?.typeArguments?.firstOrNull()?.type
                ?: ConeErrorType(
                    ConeCommandIncompatibleTypeError(performExpression.expression.coneTypeOrNull),
                ),
        )
        return performExpression
    }

    override fun transformResumeExpression(
        resumeExpression: CfirResumeExpression,
        data: ResolutionMode,
    ): CfirExpression {
        resumeExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)

        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.EffectHandlers)) {
            resumeExpression.replaceConeTypeOrNull(
                ConeErrorType(ConeEffectsFeatureDisabledError("resume"))
            )
            return resumeExpression
        }

        val handlerContext = effectHandlerStack.lastOrNull()
        if (handlerContext == null) {
            resumeExpression.replaceConeTypeOrNull(
                ConeErrorType(ConeImplicitResumeOutsideHandlerError)
            )
            return resumeExpression
        }

        resumeExpression.replaceConeTypeOrNull(builtinTypes.nothingType)

        val throwingType = resumeExpression.throwingExpression?.coneTypeOrNull
        if (throwingType != null && !isExceptionLikeType(throwingType)) {
            resumeExpression.replaceConeTypeOrNull(
                ConeErrorType(
                    ConeResumeThrowingMismatchTypeError(throwingType),
                    delegatedType = builtinTypes.nothingType,
                ),
            )
            return resumeExpression
        }

        if (resumeExpression.withExpression == null && resumeExpression.throwingExpression == null) {
            if (AbstractTypeChecker.isSubtypeOf(session.typeContext, handlerContext.commandResultType, builtinTypes.unitType) != true) {
                resumeExpression.replaceConeTypeOrNull(
                    ConeErrorType(
                        ConeResumeNoWithError(handlerContext.commandResultType),
                        delegatedType = builtinTypes.nothingType,
                    ),
                )
            }
        }

        return resumeExpression
    }

    // ── Assignment ────────────────────────────────────────────────────────────

    override fun transformAssignment(
        assignment: CfirAssignment,
        data: ResolutionMode,
    ): CfirExpression {
        val subscriptLValue = assignment.lValue as? CfirSubscriptExpression
        if (subscriptLValue != null) {
            assignment.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
            subscriptLValue.transformReceiver(transformer, ResolutionMode.ContextIndependent)
            subscriptLValue.transformIndices(transformer, ResolutionMode.ContextIndependent)
            assignment.transformRValue(transformer, ResolutionMode.ContextIndependent)

            resolveSubscriptSetAssignment(assignment, subscriptLValue, data)
            assignment.replaceConeTypeOrNull(builtinTypes.unitType)
            components.dataFlowAnalyzer.exitVariableAssignment(assignment)
            return assignment
        }

        assignment.transformChildren(transformer, ResolutionMode.ContextIndependent)
        assignment.replaceConeTypeOrNull(builtinTypes.unitType)
        components.dataFlowAnalyzer.recordAssignment(assignment)
        components.dataFlowAnalyzer.exitVariableAssignment(assignment)
        return assignment
    }

    // ── Tuple / Array / String Literals ──────────────────────────────────────

    override fun transformTupleLiteral(
        tupleLiteral: CfirTupleLiteral,
        data: ResolutionMode,
    ): CfirExpression {
        tupleLiteral.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val elementTypes = tupleLiteral.elements.map {
            it.coneTypeOrNull ?: errorType("unresolved element")
        }
        tupleLiteral.replaceConeTypeOrNull(ConeTupleType(elementTypes))
        return tupleLiteral
    }

    override fun transformArrayLiteral(
        arrayLiteral: CfirArrayLiteral,
        data: ResolutionMode,
    ): CfirExpression {
        val expectedType = data.expectedTypeOrNull?.fullyExpandedType()
        val expectedElementType = expectedType?.arrayElementType

        if (expectedType is ConeErrorType && expectedElementType == null) {
            arrayLiteral.transformChildren(transformer, ResolutionMode.ContextIndependent)
            if (arrayLiteral.coneTypeOrNull == null) {
                arrayLiteral.replaceConeTypeOrNull(
                    arrayLiteral.constructArrayTypeFromElements()
                )
            }
            return arrayLiteral
        }

        if (expectedType != null && expectedElementType == null) {
            arrayLiteral.transformChildren(transformer, ResolutionMode.ContextIndependent)
            return arrayLiteral.asTypeMismatchExpression(
                expectedType = expectedType,
                actualType = arrayLiteral.constructArrayTypeFromElements(),
            )
        }

        val elementResolutionMode = expectedElementType?.let(::withExpectedType) ?: ResolutionMode.ContextIndependent
        arrayLiteral.transformChildren(transformer, elementResolutionMode)

        val arrayLiteralWithElementDiagnostics = if (expectedElementType != null) {
            arrayLiteral.withElementTypeDiagnostics(expectedElementType)
        } else {
            arrayLiteral
        }

        val elementTypes = arrayLiteralWithElementDiagnostics.elements
            .mapNotNull { it.coneTypeOrNull }
            .filterNot { it is ConeErrorType }
        val elementType = expectedElementType ?: arrayLiteralWithElementDiagnostics.inferredElementTypeOrNull(elementTypes)

        if (elementType == null) {
            if (elementTypes.isEmpty()) {
                arrayLiteralWithElementDiagnostics.replaceConeTypeOrNull(
                    errorType("array literal type cannot be inferred", DiagnosticKind.EmptyArrayLiteralTypeUndefined)
                )
                return arrayLiteralWithElementDiagnostics
            }
            return arrayLiteralWithElementDiagnostics.asInconsistentElementTypeExpression()
        }
        arrayLiteralWithElementDiagnostics.replaceConeTypeOrNull(constructArrayType(elementType))
        return arrayLiteralWithElementDiagnostics
    }

    private fun CfirArrayLiteral.withElementTypeDiagnostics(expectedElementType: ConeCangJieType): CfirArrayLiteral {
        val checkedElements = elements.map { element ->
            val actualType = element.coneTypeOrNull
            if (actualType == null || actualType is ConeErrorType ||
                actualType.isCompatibleWith(expectedElementType)
            ) {
                element
            } else {
                element.asTypeMismatchExpression(expectedElementType, actualType)
            }
        }
        return replaceElementsIfNeeded(checkedElements)
    }

    private fun CfirArrayLiteral.inferredElementTypeOrNull(elementTypes: List<ConeCangJieType>): ConeCangJieType? {
        val commonType = session.typeContext.commonSuperTypeOrNull(elementTypes) ?: return null
        val resolvedType = IdealTypeResolver.resolveIfIdeal(commonType)
        return resolvedType.takeIf { it.isAcceptableInferredArrayElementType(elementTypes) }
    }

    /**
     * `JoinAsVisibleTy` 失败不能被 `Any` 顶类型吞掉。
     *
     * 仓颉官方数组字面量推断要求元素类型存在可见公共父类型；纯值类型/基本类型组合
     * 只能退化到 `Any` 时，应报告元素类型不一致，而不是推断为 `Array<Any>`。
     */
    private fun ConeCangJieType.isAcceptableInferredArrayElementType(elementTypes: List<ConeCangJieType>): Boolean {
        if (!isAnyType()) return true
        return elementTypes.any { it is ConeClassLikeType && !it.isAnyType() }
    }

    private fun ConeCangJieType.isAnyType(): Boolean {
        return this === ConeAnyType || (this is ConeClassLikeType && classId == StdlibClassIds.Any)
    }

    private fun CfirArrayLiteral.replaceElementsIfNeeded(newElements: List<CfirExpression>): CfirArrayLiteral {
        if (newElements == elements) return this
        return buildArrayLiteralCopy(this) {
            elements.clear()
            elements.addAll(newElements)
        }
    }

    private fun ConeCangJieType.isCompatibleWith(expectedType: ConeCangJieType): Boolean =
        AbstractTypeChecker.equalTypes(session.typeContext, this, expectedType) ||
                AbstractTypeChecker.isSubtypeOf(session.typeContext, this, expectedType)

    private fun CfirExpression.asTypeMismatchExpression(
        expectedType: ConeCangJieType,
        actualType: ConeCangJieType,
    ): CfirErrorExpression = buildErrorExpression {
        source = this@asTypeMismatchExpression.source
        diagnostic = ConeTypeMismatchError(expectedType, actualType)
        nonExpressionElement = this@asTypeMismatchExpression
    }

    private fun CfirArrayLiteral.asInconsistentElementTypeExpression(): CfirErrorExpression = buildErrorExpression {
        source = this@asInconsistentElementTypeExpression.source
        diagnostic = ConeInconsistentArrayLiteralElementTypeError()
    }

    private fun CfirArrayLiteral.constructArrayTypeFromElements(): ConeCangJieType {
        val elementType = elements
            .mapNotNull { it.coneTypeOrNull }
            .filterNot { it is ConeErrorType }
            .let { session.typeContext.commonSuperTypeOrNull(it) }
            ?: ConeErrorType(ConeSimpleDiagnostic("array literal element type"))
        return constructArrayType(elementType)
    }

    private fun constructArrayType(elementType: ConeCangJieType): ConeCangJieType {
        return constructNamedType(
            classId = StdlibClassIds.Array,
            typeArguments = listOf(elementType),
        )
    }

    override fun transformStringInterpolation(
        stringInterpolation: CfirStringInterpolation,
        data: ResolutionMode,
    ): CfirExpression {
        stringInterpolation.transformChildren(transformer, ResolutionMode.ContextIndependent)
        stringInterpolation.replaceConeTypeOrNull(stdlibStringType())
        return stringInterpolation
    }

    // ── Comparison / Binary / Type Operators ──────────────────────────────────

    override fun transformComparisonExpression(
        comparisonExpression: CfirComparisonExpression,
        data: ResolutionMode,
    ): CfirExpression {
        comparisonExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        comparisonExpression.replaceConeTypeOrNull(resolveComparisonExpressionType(comparisonExpression, data))
        return comparisonExpression
    }

    private fun resolveComparisonExpressionType(
        comparisonExpression: CfirComparisonExpression,
        data: ResolutionMode,
    ): ConeCangJieType {
        val leftType = comparisonExpression.left.coneTypeOrNull
        val rightType = comparisonExpression.right.coneTypeOrNull
        if (leftType == null || rightType == null) return builtinTypes.boolType

        val operatorName = comparisonExpression.operation.toOperatorName()
        CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
            operatorName,
            leftType,
            listOf(rightType),
        )?.let { return it.returnType }

        val comparisonCall = buildFunctionCall {
            source = comparisonExpression.source
            calleeReference = buildNamedReference {
                source = comparisonExpression.source
                name = operatorName
            }
            explicitReceiver = comparisonExpression.left
            argumentList = buildArgumentList {
                source = comparisonExpression.source
                arguments.add(comparisonExpression.right)
            }
            origin = CfirFunctionCallOrigin.Operator
        }

        val resolvedCall = callResolver.resolveCallAndSelectCandidate(comparisonCall, data)
        (resolvedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            return ConeErrorType(diagnostic, delegatedType = builtinTypes.boolType)
        }

        val completedCall = components.callCompleter.completeCall(resolvedCall, data)
        (completedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            return ConeErrorType(diagnostic, delegatedType = builtinTypes.boolType)
        }

        return completedCall.coneTypeOrNull ?: builtinTypes.boolType
    }

    override fun transformBinaryOp(
        binaryOp: CfirBinaryOp,
        data: ResolutionMode,
    ): CfirExpression {
        val resultType = when (binaryOp.kind) {
            CfirBinaryOpKind.AND,
            CfirBinaryOpKind.OR,
            -> {
                binaryOp.transformChildren(transformer, ResolutionMode.ContextIndependent)
                builtinTypes.boolType
            }

            CfirBinaryOpKind.COALESCING -> transformCoalescingExpression(binaryOp, data)

            CfirBinaryOpKind.PIPELINE,
            CfirBinaryOpKind.COMPOSITION,
            -> return transformFlowExpression(binaryOp, data)
        }
        binaryOp.replaceConeTypeOrNull(resultType)
        return binaryOp
    }

    /**
     * 仓颉 flow 表达式对齐官方 `DesugarFlowExpr`：
     * `a |> f` 解糖为 `f(a)`，`f ~> g` 解糖为 `std.core.composition(f, g)`。
     * 解糖后的调用继续走统一调用解析，复用函数类型 invoke、泛型约束与变参映射。
     */
    private fun transformFlowExpression(
        binaryOp: CfirBinaryOp,
        data: ResolutionMode,
    ): CfirExpression {
        val desugaredCall = when (binaryOp.kind) {
            CfirBinaryOpKind.PIPELINE -> buildPipelineCall(binaryOp)
            CfirBinaryOpKind.COMPOSITION -> buildCompositionCall(binaryOp)
            else -> error("Expected flow binary operation, got ${binaryOp.kind}")
        }
        val resolvedCall = transformFunctionCallInternal(desugaredCall, data, CallResolutionMode.REGULAR)
        binaryOp.replaceConeTypeOrNull(resolvedCall.coneTypeOrNull)
        return resolvedCall
    }

    private fun buildPipelineCall(binaryOp: CfirBinaryOp): CfirFunctionCall =
        buildFunctionCall {
            source = binaryOp.source
            calleeReference = buildNamedReference {
                source = binaryOp.right.source ?: binaryOp.source
                name = OperatorNameConventions.INVOKE
            }
            explicitReceiver = binaryOp.right
            argumentList = buildArgumentList {
                source = binaryOp.source
                arguments.add(binaryOp.left)
            }
            origin = CfirFunctionCallOrigin.Operator
        }

    private fun buildCompositionCall(binaryOp: CfirBinaryOp): CfirFunctionCall =
        buildFunctionCall {
            source = binaryOp.source
            calleeReference = buildNamedReference {
                source = binaryOp.source
                name = Name.identifier("composition")
            }
            argumentList = buildArgumentList {
                source = binaryOp.source
                arguments.add(binaryOp.left)
                arguments.add(binaryOp.right)
            }
            origin = CfirFunctionCallOrigin.CompilerCoreIntrinsic
        }

    /**
     * 对齐官方 `ChkCoalescingExpr`：`left: Option<T>` 时，`left ?? right`
     * 先以 `T` 检查右操作数，并且整个表达式的类型为 `T`。
     */
    private fun transformCoalescingExpression(
        binaryOp: CfirBinaryOp,
        data: ResolutionMode,
    ): ConeCangJieType {
        binaryOp.transformAnnotations(transformer, data)
        binaryOp.transformLeft(transformer, ResolutionMode.ContextIndependent)

        val leftElementType = binaryOp.left.coneTypeOrNull?.optionElementType
        if (leftElementType == null) {
            binaryOp.transformRight(transformer, ResolutionMode.ContextIndependent)
            return errorType("coalescing left operand must be Option")
        }

        val resultType = coalescingResultType(leftElementType, data.expectedTypeOrNull)
        binaryOp.transformRight(transformer, withExpectedType(resultType))
        return resultType
    }

    private fun coalescingResultType(
        leftElementType: ConeCangJieType,
        expectedType: ConeCangJieType?,
    ): ConeCangJieType {
        if (expectedType == null) return leftElementType
        return if (AbstractTypeChecker.isSubtypeOf(session.typeContext, leftElementType, expectedType) == true) {
            expectedType
        } else {
            leftElementType
        }
    }

    override fun transformTypeOperator(
        typeOperator: CfirTypeOperator,
        data: ResolutionMode,
    ): CfirExpression {
        typeOperator.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val resultType = when (typeOperator.operation) {
            CfirTypeOperationKind.IS -> builtinTypes.boolType
            CfirTypeOperationKind.AS -> {
                val typeRef = typeOperator.typeRef
                // 仓颉 `as` 是安全转换，静态结果类型为 Option<T>，成功为 Some(value)，失败为 None。
                if (typeRef is CfirResolvedTypeRef) {
                    constructNamedType(
                        classId = StdlibClassIds.Option,
                        typeArguments = listOf(typeRef.coneType),
                    )
                }
                else errorType("unresolved type in as-expression")
            }
        }
        typeOperator.replaceConeTypeOrNull(resultType)
        return typeOperator
    }

    override fun transformTypeConversion(
        typeConversion: CfirTypeConversion,
        data: ResolutionMode,
    ): CfirExpression {
        typeConversion.transformAnnotations(transformer, data)
        typeConversion.transformTargetTypeRef(transformer, ResolutionMode.ContextIndependent)
        typeConversion.transformArgument(transformer, ResolutionMode.ContextIndependent)

        val targetType = typeConversion.targetTypeRef.coneTypeOrNull
        val targetPrimitiveType = targetType as? ConePrimitiveType
        if (targetPrimitiveType == null) {
            val resultType = when (targetType) {
                is ConeErrorType -> ConeErrorType(ConeUnreportedDuplicateDiagnostic(targetType.diagnostic))
                else -> errorType("type conversion target is not a primitive type")
            }
            typeConversion.replaceConeTypeOrNull(resultType)
            return typeConversion
        }

        val argumentType = typeConversion.argument.coneTypeOrNull
        if (argumentType == null) {
            typeConversion.replaceConeTypeOrNull(errorType("type conversion argument type is unresolved"))
            return typeConversion
        }
        if (argumentType is ConeErrorType) {
            typeConversion.replaceConeTypeOrNull(ConeErrorType(ConeUnreportedDuplicateDiagnostic(argumentType.diagnostic)))
            return typeConversion
        }

        val normalizedArgumentType = IdealTypeResolver.resolveIfIdeal(argumentType)
        if (normalizedArgumentType != argumentType) {
            typeConversion.argument.replaceConeTypeOrNull(normalizedArgumentType)
        }

        val synthesizedType = if (targetPrimitiveType.canConvertFrom(normalizedArgumentType)) {
            targetPrimitiveType
        } else {
            errorType("numeric conversion requires numeric operand")
        }

        val expectedType = data.expectedTypeOrNull
        val checkedType = if (
            expectedType != null &&
            synthesizedType !is ConeErrorType &&
            AbstractTypeChecker.isSubtypeOf(session.typeContext, synthesizedType, expectedType) != true
        ) {
            ConeErrorType(
                diagnostic = ConeTypeMismatchError(expectedType, synthesizedType),
                delegatedType = synthesizedType,
            )
        } else {
            synthesizedType
        }

        typeConversion.replaceConeTypeOrNull(checkedType)
        return typeConversion
    }

    /**
     * 对齐官方 `SynNumTypeConvExpr`：
     * - `Nothing` 可转换到 `Rune` 或任意数值类型；
     * - `Rune` 可转换到 `UInt32`；
     * - 整数可转换到 `Rune`；
     * - 数值类型之间可相互转换。
     */
    private fun ConePrimitiveType.canConvertFrom(argumentType: ConeCangJieType): Boolean {
        val sourceKind = (argumentType as? ConePrimitiveType)?.kind ?: return false
        val targetKind = kind
        val isNothingToRuneOrNumeric =
            sourceKind == PrimitiveTypeKind.NOTHING && (targetKind == PrimitiveTypeKind.RUNE || targetKind.isNumeric)
        val isRuneToUInt32 =
            sourceKind == PrimitiveTypeKind.RUNE && targetKind == PrimitiveTypeKind.UINT32
        val isIntegerToRune =
            targetKind == PrimitiveTypeKind.RUNE && sourceKind.isInteger
        val isBetweenNumeric =
            targetKind.isNumeric && sourceKind.isNumeric
        return isNothingToRuneOrNumeric || isRuneToUInt32 || isIntegerToRune || isBetweenNumeric
    }

    // ── For-In / Loop ─────────────────────────────────────────────────────────

    override fun transformForInExpression(
        forInExpression: CfirForInExpression,
        data: ResolutionMode,
    ): CfirExpression {
        forInExpression.iterable.resolveIndependently()
        val iterVarType = inferIterableElementType(forInExpression.iterable.coneTypeOrNull)

        val varDecl = forInExpression.variable
        if (varDecl.returnTypeRef !is CfirResolvedTypeRef && varDecl.returnTypeRef !is CfirImplicitTypeRef) {
            varDecl.replaceReturnTypeRef(
                specificTypeResolverTransformer.transformTypeRef(
                    varDecl.returnTypeRef,
                    currentTypeResolutionConfiguration(),
                ),
            )
        } else if (varDecl.returnTypeRef !is CfirResolvedTypeRef) {
            varDecl.replaceReturnTypeRef(
                varDecl.returnTypeRef.resolvedTypeFromPrototype(iterVarType, varDecl.returnTypeRef.source)
            )
        }

        varDecl.transformPattern(transformer, ResolutionMode.ContextIndependent)
        if (varDecl is org.cangnova.cangjie.cfir.declarations.impl.CfirPatternVariableImpl) {
            varDecl.pattern = resolveDeferredMatchPattern(
                pattern = varDecl.pattern,
                expectedType = varDecl.returnTypeRef.coneTypeOrNull ?: iterVarType,
            )
        }
        resolvePatternBindingTypes(
            pattern = varDecl.pattern,
            expectedType = varDecl.returnTypeRef.coneTypeOrNull ?: iterVarType,
            typeResolver = specificTypeResolverTransformer,
        )

        withNewLocalScope {
            registerPatternBindings(varDecl.pattern)
            forInExpression.transformBody(transformer, ResolutionMode.ContextIndependent)
        }

        forInExpression.replaceConeTypeOrNull(builtinTypes.unitType)
        return forInExpression
    }

    private fun inferIterableElementType(iterableType: ConeCangJieType?): ConeCangJieType {
        if (iterableType == null) return errorType("iterable has no type")
        iterableType.arrayElementType?.let { return it }
        when (iterableType) {
            is ConeClassLikeType -> {
                if (iterableType.classId == StdlibClassIds.Range) {
                    return iterableType.typeArguments.firstOrNull()?.type ?: ConePrimitiveType.INT64
                }
                val typeArgs = iterableType.typeArguments
                if (typeArgs.isNotEmpty()) return typeArgs.first().type
            }
            is ConeStructType -> {
                if (iterableType.classId == StdlibClassIds.Range) {
                    return iterableType.typeArguments.firstOrNull()?.type ?: ConePrimitiveType.INT64
                }
            }
            else -> Unit
        }
        return errorType("cannot infer element type from: $iterableType")
    }

    override fun transformLoopExpression(
        loopExpression: CfirLoopExpression,
        data: ResolutionMode,
    ): CfirExpression {
        loopExpression.transformAnnotations(transformer, data)
        if (loopExpression.isDoWhile) {
            components.dataFlowAnalyzer.enterDoWhileLoop(loopExpression)
            loopExpression.transformBody(transformer, ResolutionMode.ContextIndependent)
            components.dataFlowAnalyzer.enterDoWhileLoopCondition(loopExpression)
            loopExpression.transformCondition(transformer, ResolutionMode.ContextIndependent)
            components.dataFlowAnalyzer.exitDoWhileLoop(loopExpression)
        } else {
            components.dataFlowAnalyzer.enterWhileLoop(loopExpression)
            if (loopExpression.condition.containsLetPatternCondition()) {
                withNewLocalScope {
                    resolveConditionWithPatternBindings(loopExpression.condition)
                    components.dataFlowAnalyzer.exitWhileLoopCondition(loopExpression)
                    loopExpression.transformBody(transformer, ResolutionMode.ContextIndependent)
                    components.dataFlowAnalyzer.exitWhileLoop(loopExpression)
                }
            } else {
                loopExpression.transformCondition(transformer, ResolutionMode.ContextIndependent)
                components.dataFlowAnalyzer.exitWhileLoopCondition(loopExpression)
                loopExpression.transformBody(transformer, ResolutionMode.ContextIndependent)
                components.dataFlowAnalyzer.exitWhileLoop(loopExpression)
            }
        }
        loopExpression.replaceConeTypeOrNull(builtinTypes.unitType)
        return loopExpression
    }

    // ── Try / Catch ───────────────────────────────────────────────────────────

    override fun transformHandleClause(
        handleClause: CfirHandleClause,
        data: ResolutionMode,
    ): CfirExpression {
        handleClause.transformAnnotations(transformer, data)
        resolveCommandPatternTypeRefs(handleClause.commandPattern)

        if (!session.languageVersionSettings.supportsFeature(LanguageFeature.EffectHandlers)) {
            handleClause.transformBody(transformer, ResolutionMode.ContextIndependent)
            val delegatedType = normalizeTypeForJoin(handleClause.body.coneTypeOrNull) ?: builtinTypes.unitType
            handleClause.replaceConeTypeOrNull(
                ConeErrorType(
                    ConeEffectsFeatureDisabledError("handle"),
                    delegatedType = delegatedType,
                )
            )
            return handleClause
        }

        val commandResultType = resolveHandleCommandResultType(handleClause.commandPattern)
        val effectiveResultType = commandResultType ?: constructNamedType(StdlibClassIds.Any)

        effectHandlerStack.addLast(EffectHandlerContext(effectiveResultType))
        try {
            handleClause.transformBody(transformer, ResolutionMode.ContextIndependent)
        } finally {
            effectHandlerStack.removeLast()
        }

        val bodyType = handleClause.body.coneTypeOrNull ?: builtinTypes.unitType
        val normalizedBodyType = normalizeTypeForJoin(bodyType) ?: bodyType
        handleClause.replaceConeTypeOrNull(
            if (commandResultType == null) {
                ConeErrorType(
                    ConeCommandHandleTypeError(handleClause.commandPattern.typeRefs.firstOrNull()?.coneType),
                    delegatedType = normalizedBodyType,
                )
            } else {
                normalizedBodyType
            }
        )
        return handleClause
    }

    override fun transformTryExpression(
        tryExpression: CfirTryExpression,
        data: ResolutionMode,
    ): CfirExpression {
        tryExpression.transformAnnotations(transformer, data)
        components.dataFlowAnalyzer.enterTryExpression(tryExpression)
        val expectedType = data.expectedTypeOrNull
        val branchResolutionMode = (data as? ResolutionMode.WithExpectedType)
            ?.takeUnless { it.fromCast }
            ?.copy(forceFullCompletion = false)
            ?: ResolutionMode.ContextDependent
        context.forBlock(session) {
            tryExpression.transformResources(transformer, ResolutionMode.ContextIndependent)
            tryExpression.transformTryBlock(transformer, branchResolutionMode)
            components.dataFlowAnalyzer.exitTryMainBlock()
        }
        for (catchClause in tryExpression.catches) {
            components.dataFlowAnalyzer.enterCatchClause(catchClause)
            catchClause.transform<CfirElement, ResolutionMode>(transformer, branchResolutionMode)
            components.dataFlowAnalyzer.exitCatchClause(catchClause)
        }
        for (handleClause in tryExpression.handlers) {
            components.dataFlowAnalyzer.enterHandleClause(handleClause)
            handleClause.transform<CfirElement, ResolutionMode>(transformer, branchResolutionMode)
            components.dataFlowAnalyzer.exitHandleClause(handleClause)
        }
        if (tryExpression.finallyBlock != null) {
            components.dataFlowAnalyzer.enterFinallyBlock()
            tryExpression.transformFinallyBlock(transformer, ResolutionMode.ContextIndependent)
            components.dataFlowAnalyzer.exitFinallyBlock()
        }
        components.dataFlowAnalyzer.exitTryExpression(data.forceFullCompletion)

        var currentJoinType = normalizeTypeForJoin(tryExpression.tryBlock.coneTypeOrNull) ?: builtinTypes.unitType
        tryExpression.catches.forEach { catchClause ->
            val catchType = normalizeTypeForJoin(catchClause.body.coneTypeOrNull) ?: builtinTypes.unitType
            currentJoinType = commonSupertype(listOf(currentJoinType, catchType))
        }

        var handleMismatchDiagnostic: ConeMismatchingHandleBlockError? = null
        tryExpression.handlers.forEach { handleClause ->
            val handleType = normalizeTypeForJoin(handleClause.coneTypeOrNull ?: handleClause.body.coneTypeOrNull)
                ?: builtinTypes.unitType
            val joinedType = commonSupertype(listOf(currentJoinType, handleType))
            if (joinedType is ConeUnionType) {
                val diagnostic = ConeMismatchingHandleBlockError(handleType, currentJoinType)
                handleClause.replaceConeTypeOrNull(
                    ConeErrorType(
                        diagnostic,
                        delegatedType = joinedType,
                    ),
                )
                handleMismatchDiagnostic = diagnostic
            } else {
                currentJoinType = joinedType
            }
        }

        tryExpression.replaceConeTypeOrNull(
            when {
                handleMismatchDiagnostic != null -> ConeErrorType(
                    handleMismatchDiagnostic!!,
                    delegatedType = currentJoinType,
                )

                /**
                 * 官方仓颉 `ChkTryExpr` 在存在外层 target type 时，以 target type
                 * 逐个检查 try/catch block，并把整个 try 视为该 target type。
                 * 这样分支上的类型错误会定位到尾表达式，而不会再向外层 `return try`
                 * 额外扩散一个 `RETURN_TYPE_MISMATCH`。
                 */
                expectedType != null && tryExpression.handlers.isEmpty() -> expectedType
                else -> currentJoinType
            }
        )
        return tryExpression
    }

    override fun transformCatch(
        catch: CfirCatch,
        data: ResolutionMode,
    ): CfirExpression {
        catch.transformAnnotations(transformer, data)
        context.forBlock(session) {
            resolveCatchPattern(catch.pattern)
            catch.transformBody(transformer, data)
        }

        catch.replaceConeTypeOrNull(catch.body.coneTypeOrNull ?: builtinTypes.unitType)
        return catch
    }

    // ── Subscript ─────────────────────────────────────────────────────────────

    override fun transformSubscriptExpression(
        subscriptExpression: CfirSubscriptExpression,
        data: ResolutionMode,
    ): CfirExpression {
        subscriptExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val resultType = when (val receiverType = subscriptExpression.receiver.coneTypeOrNull) {
            is ConeTupleType -> {
                val indexValue = extractConstantIntIndex(subscriptExpression.indices.firstOrNull())
                if (indexValue != null && indexValue in receiverType.elementTypes.indices) {
                    receiverType.elementTypes[indexValue]
                } else {
                    errorType("tuple index out of bounds or non-constant")
                }
            }
            is ConeVArrayType -> receiverType.elementType
            else -> {
                val arrayElementType = receiverType?.arrayElementType
                arrayElementType
                    ?: if (receiverType != null) {
                        resolveSubscriptExpressionType(subscriptExpression, receiverType, data)
                    } else {
                        errorType("receiver has no type")
                    }
            }
        }
        subscriptExpression.replaceConeTypeOrNull(resultType)
        return subscriptExpression
    }

    private fun resolveSubscriptExpressionType(
        subscriptExpression: CfirSubscriptExpression,
        receiverType: ConeCangJieType,
        data: ResolutionMode,
    ): ConeCangJieType {
        val argTypes = subscriptExpression.indices.mapNotNull { it.coneTypeOrNull }
        CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
            Name.identifier("[]"),
            receiverType,
            argTypes,
        )?.let { return it.returnType }

        val getCall = buildFunctionCall {
            source = subscriptExpression.source
            calleeReference = buildNamedReference {
                source = subscriptExpression.source
                name = OperatorNameConventions.GET
            }
            explicitReceiver = subscriptExpression.receiver
            argumentList = buildArgumentList {
                arguments.addAll(subscriptExpression.indices)
            }
            origin = CfirFunctionCallOrigin.Operator
        }

        val resolvedCall = callResolver.resolveCallAndSelectCandidate(getCall, data)
        (resolvedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            return ConeErrorType(diagnostic)
        }

        val completedCall = components.callCompleter.completeCall(resolvedCall, data)
        (completedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            return ConeErrorType(diagnostic)
        }

        return completedCall.coneTypeOrNull ?: errorType("no subscript operator for: $receiverType")
    }

    private fun resolveSubscriptSetAssignment(
        assignment: CfirAssignment,
        subscriptExpression: CfirSubscriptExpression,
        data: ResolutionMode,
    ) {
        val setCall = buildFunctionCall {
            source = subscriptExpression.source
            calleeReference = buildNamedReference {
                source = subscriptExpression.source
                name = OperatorNameConventions.SET
            }
            explicitReceiver = subscriptExpression.receiver
            argumentList = buildArgumentList {
                arguments.addAll(subscriptExpression.indices)
                arguments.add(assignment.rValue)
            }
            origin = CfirFunctionCallOrigin.Operator
        }

        val resolvedCall = callResolver.resolveCallAndSelectCandidate(setCall, data)
        (resolvedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            subscriptExpression.replaceConeTypeOrNull(ConeErrorType(diagnostic, delegatedType = builtinTypes.unitType))
            return
        }

        val completedCall = components.callCompleter.completeCall(resolvedCall, data)
        (completedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic?.let { diagnostic ->
            subscriptExpression.replaceConeTypeOrNull(ConeErrorType(diagnostic, delegatedType = builtinTypes.unitType))
            return
        }

        subscriptExpression.replaceConeTypeOrNull(completedCall.coneTypeOrNull ?: builtinTypes.unitType)
    }

    private fun extractConstantIntIndex(expr: CfirExpression?): Int? {
        val parsed = expr?.let(CfirIntConstantEvalUtils::parseSignedIntExpression) ?: return null
        if (parsed.explicitSuffix != null && parsed.explicitSuffix != "i64") return null
        if (parsed.value < BigInteger.ZERO || parsed.value > BigInteger.valueOf(Int.MAX_VALUE.toLong())) return null
        return parsed.value.toInt()
    }

    // ── Lambda ────────────────────────────────────────────────────────────────

    override fun transformAnonymousFunctionExpression(
        anonymousFunctionExpression: CfirAnonymousFunctionExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return withClearedEffectHandlers {
            val anonFunc = anonymousFunctionExpression.anonymousFunction
            anonFunc.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
            anonFunc.valueParameters.forEach { parameter ->
                parameter.transformReturnTypeRef(transformer, ResolutionMode.ContextIndependent)
            }
            val expectedFuncType = data.expectedTypeOrNull as? ConeFunctionType

            resolveAnonymousFunctionExplicitParameterTypes(anonFunc)
            val hasUnresolvedParameterType = anonFunc.valueParameters.any { it.returnTypeRef !is CfirResolvedTypeRef }
            if (expectedFuncType == null && hasUnresolvedParameterType) {
                // Keep top-level lambda shape unresolved until call completion provides an expected function type.
                // Eagerly fixing returnType here turns lambda return mismatches into outer argument mismatches.
                components.dataFlowAnalyzer.enterAnonymousFunctionExpression(anonymousFunctionExpression)
                context.storeContextForAnonymousFunction(anonFunc)
                return@withClearedEffectHandlers anonymousFunctionExpression
            }

            components.dataFlowAnalyzer.enterFunction(anonFunc)

            val parameterTypes = context.withTowerDataCleanup {
                context.addLocalScope(CfirLocalScope(session))
                val types = anonFunc.valueParameters.mapIndexed { i, param ->
                    val expectedParamType = expectedFuncType?.parameterTypes?.getOrNull(i)
                    val declaredParamType = (param.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                    if (param.returnTypeRef !is CfirResolvedTypeRef && expectedParamType != null) {
                        param.replaceReturnTypeRef(
                            param.returnTypeRef.resolvedTypeFromPrototype(expectedParamType, param.returnTypeRef.source)
                        )
                    }
                    context.storeValueParameterIfNeeded(param, session)
                    declaredParamType ?: expectedParamType
                }
                anonFunc.body?.resolveIndependently()
                types
            }

            val returnType = when {
                anonFunc.returnTypeRef is CfirResolvedTypeRef -> (anonFunc.returnTypeRef as CfirResolvedTypeRef).coneType
                expectedFuncType != null -> expectedFuncType.returnType
                else -> anonFunc.body?.coneTypeOrNull
            }

            if (returnType != null && anonFunc.returnTypeRef !is CfirResolvedTypeRef) {
                anonFunc.replaceReturnTypeRef(
                    returnType.toCfirResolvedTypeRef(anonFunc.returnTypeRef.source, anonFunc.returnTypeRef),
                )
            }

            if (returnType != null && parameterTypes.all { it != null }) {
                // CfirAnonymousFunctionExpression.coneTypeOrNull is derived from anonymousFunction.typeRef.
                // Keep the source of truth on declaration side instead of writing expression cone type directly.
                val lambdaType = ConeFunctionType(
                    parameterTypes = parameterTypes.filterNotNull(),
                    returnType = returnType,
                    isCFunc = expectedFuncType?.isCFunc ?: false,
                    isClosureType = expectedFuncType?.isClosureType ?: false,
                    hasVariableLenArg = expectedFuncType?.hasVariableLenArg ?: false,
                    attributes = expectedFuncType?.attributes ?: org.cangnova.cangjie.cfir.types.ConeAttributes.Empty,
                )
                anonFunc.replaceTypeRef(lambdaType.toCfirResolvedTypeRef(anonFunc.typeRef.source, anonFunc.typeRef))
            }
            anonFunc.replaceControlFlowGraphReference(components.dataFlowAnalyzer.exitFunction(anonFunc))
            components.dataFlowAnalyzer.enterAnonymousFunctionExpression(anonymousFunctionExpression)
            anonymousFunctionExpression
        }
    }

    /**
     * 解析 lambda 显式形参类型。
     *
     * 匿名函数没有 expected function type 时仍然可以通过显式形参类型独立定型；
     * 如果先按未解析 typeRef 判断是否推迟，会让 `let f = {x: T => ...}` 这类声明
     * 在隐式声明缓存写回时保留 `CfirImplicitTypeRef`。
     */
    private fun resolveAnonymousFunctionExplicitParameterTypes(anonymousFunction: CfirAnonymousFunction) {
        val config = currentTypeResolutionConfiguration()

        for (parameter in anonymousFunction.valueParameters) {
            val typeRef = parameter.returnTypeRef
            if (typeRef is CfirImplicitTypeRef || typeRef is CfirResolvedTypeRef) continue
            parameter.replaceReturnTypeRef(
                specificTypeResolverTransformer.transformTypeRef(typeRef, config),
            )
        }
    }

    /**
     * 表达式阶段解析局部类型引用时，必须携带当前容器链上的类型参数。
     * Kotlin FIR 通过 tower data 的 member type-parameter scope 统一暴露；
     * CFIR 的显式 typeRef 解析配置在这里补齐同一作用域信息。
     */
    private fun currentTypeResolutionConfiguration(): CfirTypeResolutionConfiguration {
        val additionalTypeParameters = context.containers
            .asSequence()
            .filterIsInstance<CfirDeclaration>()
            .flatMap { extractTypeParameters(it).asSequence() }
            .toList()

        return CfirTypeResolutionConfiguration(
            scopes = components.createCurrentScopeList(),
            containingClassDeclarations = context.containingClassDeclarations.toList(),
            useSiteFile = context.file,
            topContainer = context.containerIfAny,
        ).withAdditionalTypeParameters(additionalTypeParameters)
    }

    // ── Range ─────────────────────────────────────────────────────────────────

    override fun transformRangeExpression(
        rangeExpression: CfirRangeExpression,
        data: ResolutionMode,
    ): CfirExpression {
        rangeExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val expectedRangeType = data.expectedTypeOrNull?.rangeTypeOrNull()
        val elementType = expectedRangeType?.typeArguments?.singleOrNull()?.type
            ?: inferRangeElementType(rangeExpression)
        rangeExpression.replaceConeTypeOrNull(
            constructNamedType(
                classId = StdlibClassIds.Range,
                typeArguments = listOf(elementType),
            )
        )
        return rangeExpression
    }

    // ── Spawn ─────────────────────────────────────────────────────────────────

    override fun transformSpawnExpression(
        spawnExpression: CfirSpawnExpression,
        data: ResolutionMode,
    ): CfirExpression {
        spawnExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        val taskReturnType = spawnExpression.body.coneTypeOrNull ?: builtinTypes.unitType
        spawnExpression.replaceConeTypeOrNull(
            constructNamedType(
                classId = StdlibClassIds.Future,
                typeArguments = listOf(taskReturnType),
            )
        )
        return spawnExpression
    }

    private fun <T> completeResolvedAccess(
        access: T,
        data: ResolutionMode,
    ): T where T : CfirExpression, T : CfirResolvable {
        val candidateReference = access.calleeReference as? CfirNamedReferenceWithCandidate
        if (candidateReference != null) {
            return components.callCompleter.completeCall(access, data)
        }

        if (access.coneTypeOrNull == null) {
            when (access.calleeReference) {
                is CfirResolvedNamedReference,
                is CfirErrorNamedReference,
                -> access.replaceConeTypeOrNull(components.typeFromCallee(access))
                else -> Unit
            }
        }
        return access
    }

    // ── Stdlib / ClassId Helpers ──────────────────────────────────────────────

    private fun stdlibStringType(): ConeCangJieType {
        val symbol = components.symbolProvider.getClassLikeSymbolByClassId(StdlibClassIds.String)
        if (symbol != null) {
            return constructClassLikeType(symbol, StdlibClassIds.String, emptyList())
        }
        return ConeClassLikeType(StdlibClassIds.String.toLookupTag())
    }

    private fun constructNamedType(
        classId: ClassId,
        typeArguments: List<ConeTypeProjection> = emptyList(),
    ): ConeCangJieType {
        val symbol = components.symbolProvider.getClassLikeSymbolByClassId(classId)
        return if (symbol != null) constructClassLikeType(symbol, classId, typeArguments)
        else ConeClassLikeType(classId.toLookupTag(), typeArguments)
    }

    /**
     * optional chain 的结果语义始终是 `Option<result>`。
     *
     * 本轮不做官方的完整 match/Some/None 解糖，只在 resolve 入口保证类型提升语义成立。
     */
    private fun liftOptionalChainResultType(resultType: ConeCangJieType?): ConeCangJieType {
        val effectiveResultType = resultType ?: return ConeErrorType(
            ConeSimpleDiagnostic("optional chain result type is unresolved", DiagnosticKind.InferenceError)
        )
        return constructNamedType(
            classId = StdlibClassIds.Option,
            typeArguments = listOf(effectiveResultType),
        )
    }

    /**
     * 从整条 optional chain 内部链条中找到 quest 包装的链首表达式。
     *
     * 链内普通访问/调用/索引节点不参与 optional 语义判定，真正需要校验的是最外层
     * `CfirOptionalExpression` 对应的 base expression 类型。
     */
    private fun CfirExpression.optionalChainRootExpression(): CfirExpression? = when (this) {
        is CfirOptionalExpression -> expression
        is CfirQualifiedAccessExpression -> explicitReceiver?.optionalChainRootExpression()
            ?: dispatchReceiver?.optionalChainRootExpression()
        is CfirFunctionCall -> explicitReceiver?.optionalChainRootExpression()
        is CfirSubscriptExpression -> receiver.optionalChainRootExpression()
        else -> null
    }

    private fun constructClassLikeType(
        symbol: CfirClassLikeSymbol<*>,
        classId: ClassId,
        typeArguments: List<ConeTypeProjection>,
    ): ConeCangJieType = when (symbol) {
        is CfirTypeAliasSymbol -> ConeTypeAliasType(classId, typeArguments = typeArguments)

        is CfirPrimitiveTypeSymbol -> ConePrimitiveType(symbol.kind)
        is CfirInterfaceSymbol -> ConeClassLikeType(classId.toLookupTag(), typeArguments, isInterface = true)
        is CfirStructSymbol -> ConeStructType(classId.toLookupTag(), typeArguments)
        is CfirEnumSymbol -> ConeEnumType(classId.toLookupTag(), typeArguments, isRefEnum = symbol.isRefEnum)
        else -> ConeClassLikeType(classId.toLookupTag(), typeArguments)
    }

    // ── Common Supertype ──────────────────────────────────────────────────────

    private fun commonSupertype(types: List<ConeCangJieType>): ConeCangJieType {
        if (types.isEmpty()) return builtinTypes.unitType
        val first = types.first()
        if (types.all { it == first }) return first

        val nonNothing = types.filter { it != ConePrimitiveType.NOTHING }
        if (nonNothing.isEmpty()) return ConePrimitiveType.NOTHING
        if (nonNothing.size == 1) return nonNothing.first()

        return session.typeContext.commonSuperTypeOrNull(nonNothing) ?: ConeAnyType
    }

    /**
     * 从某个 effect command 类型中提取 `Command<T>` 的 `T`。
     *
     * 这里直接沿解析后的超类型链查找 `stdx.effect.Command`，
     * 让 class/interface alias 展开后的实现类型都能复用同一条逻辑。
     */
    private fun resolveHandleCommandResultType(commandPattern: CfirCommandTypePattern): ConeCangJieType? {
        val commandType = (commandPattern.typeRefs.firstOrNull() as? CfirResolvedTypeRef)?.coneType ?: return null
        return findCommandSupertype(commandType)?.typeArguments?.firstOrNull()?.type
    }

    private fun resolveCommandPatternTypeRefs(commandPattern: CfirCommandTypePattern) {
        val additionalTypeParameters = context.containers
            .asSequence()
            .filterIsInstance<CfirDeclaration>()
            .flatMap { extractTypeParameters(it).asSequence() }
            .toList()

        val config = CfirTypeResolutionConfiguration(
            useSiteFile = context.file,
            topContainer = context.containers.lastOrNull(),
        ).withAdditionalTypeParameters(additionalTypeParameters)

        commandPattern.transformTypeRefs(specificTypeResolverTransformer, config)
    }

    private fun resolveCatchPattern(catchPattern: CfirCatchPattern) {
        resolveCatchPatternTypeRefs(catchPattern)
        catchPattern.transformBindingVariable(transformer, ResolutionMode.ContextIndependent)

        val catchTypes = catchPattern.resolvedCatchTypes()
        val bindingType = when {
            catchTypes.isEmpty() -> constructNamedType(StdlibClassIds.Exception)
            catchTypes.size == 1 -> catchTypes.single()
            else -> commonSupertype(catchTypes)
        }

        catchPattern.bindingVariable?.let { bindingVariable ->
            val currentTypeRef = bindingVariable.returnTypeRef
            bindingVariable.replaceReturnTypeRef(
                currentTypeRef.resolvedTypeFromPrototype(
                    bindingType,
                    currentTypeRef.source,
                ),
            )
            context.storeVariable(bindingVariable, session)
        }
    }

    private fun resolveCatchPatternTypeRefs(catchPattern: CfirCatchPattern) {
        val additionalTypeParameters = context.containers
            .asSequence()
            .filterIsInstance<CfirDeclaration>()
            .flatMap { extractTypeParameters(it).asSequence() }
            .toList()

        val config = CfirTypeResolutionConfiguration(
            useSiteFile = context.file,
            topContainer = context.containers.lastOrNull(),
        ).withAdditionalTypeParameters(additionalTypeParameters)

        catchPattern.transformTypeRefs(specificTypeResolverTransformer, config)
    }

    private fun CfirCatchPattern.resolvedCatchTypes(): List<ConeCangJieType> {
        if (typeRefs.isEmpty()) return emptyList()
        return typeRefs.mapNotNull { typeRef ->
            (typeRef as? CfirResolvedTypeRef)?.coneType
        }
    }

    private fun findCommandSupertype(type: ConeCangJieType?): ConeClassLikeType? {
        if (type == null) return null
        return collectSupertypeChain(type, session.typeContext)
            .filterIsInstance<ConeClassLikeType>()
            .firstOrNull { it.lookupTag.classId == StdlibClassIds.Command }
    }

    private fun isExceptionLikeType(type: ConeCangJieType): Boolean {
        val exceptionType = constructNamedType(StdlibClassIds.Exception)
        val errorType = constructNamedType(StdlibClassIds.Error)
        return AbstractTypeChecker.isSubtypeOf(session.typeContext, type, exceptionType) == true ||
                AbstractTypeChecker.isSubtypeOf(session.typeContext, type, errorType) == true
    }

    private fun normalizeTypeForJoin(type: ConeCangJieType?): ConeCangJieType? {
        return when (type) {
            is ConeErrorType -> type.delegatedType ?: type
            else -> type
        }
    }

    private inline fun <T> withClearedEffectHandlers(block: () -> T): T {
        if (effectHandlerStack.isEmpty()) return block()

        val snapshot = effectHandlerStack.toList()
        effectHandlerStack.clear()
        return try {
            block()
        } finally {
            effectHandlerStack.addAll(snapshot)
        }
    }

    private fun collectSupertypeChain(
        type: ConeCangJieType,
        context: ConeInferenceContext,
    ): List<ConeCangJieType> {
        val result = mutableListOf<ConeCangJieType>()
        val visited = mutableSetOf<ConeCangJieType>()
        val queue = ArrayDeque<ConeCangJieType>()
        queue.add(type)
        visited.add(type)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result += current
            val constructor = with(context) { (current as? ConeRigidType)?.typeConstructor() } ?: continue
            val supertypes = with(context) {
                constructor.supertypes().mapNotNull { it as? ConeCangJieType }
            }
            supertypes.forEach { supertype ->
                if (visited.add(supertype)) queue.add(supertype)
            }
        }
        return result
    }

    // ── Scope Utilities ───────────────────────────────────────────────────────

    /** 在新的空局部作用域里执行 [block]，退出后恢复外层作用域。薄壳包装 `context.forBlock`。 */
    private inline fun <T> withNewLocalScope(crossinline block: () -> T): T =
        context.forBlock(session) { block() }

    /**
     * `super` 的语义在仓颉里是固定的：
     * 1. 只能出现在 class 内部；
     * 2. 绑定到当前 class 声明的直接父 class；
     * 3. 不能落到接口父类型，也不能从继承链上做兜底推断。
     *
     * 这里在进入 tower resolve 前就把接收者类型确定下来，
     * 避免后续 `ExpressionReceiverValue.scope()` 再遇到未解析的 `super`。
     */
    private fun resolveImplicitSuperReceiverType(owner: CfirClass): ConeCangJieType {
        val directClassSuperTypes = owner.directClassSuperTypes()
        return when (directClassSuperTypes.size) {
            1 -> directClassSuperTypes.single()
            0 -> errorType("`super` requires a direct class supertype in ${owner.name}")
            else -> errorType("`super` is ambiguous because ${owner.name} declares multiple direct class supertypes")
        }
    }

    /**
     * 预留给未来显式 `super<T>` / `super<Base>` 语法：
     * 即使语法层已经指定了目标类型，也必须严格受“当前 class 的直接父 class”约束。
     */
    private fun resolveExplicitSuperReceiverType(
        owner: CfirClass,
        resolvedSuperTypeRef: CfirResolvedTypeRef,
    ): ConeCangJieType {
        val requestedType = resolvedSuperTypeRef.coneType
        if (!requestedType.isDirectClassSuperType()) {
            return errorType("`super` can only target a direct class supertype of ${owner.name}")
        }

        val directClassSuperTypes = owner.directClassSuperTypes()
        if (directClassSuperTypes.none { it == requestedType }) {
            return errorType("`super` can only target a direct class supertype of ${owner.name}")
        }

        return requestedType
    }

    private fun CfirClass.directClassSuperTypes(): List<ConeCangJieType> {
        return superTypeRefs
            .filterIsInstance<CfirResolvedTypeRef>()
            .map(CfirResolvedTypeRef::coneType)
            .filter { candidate -> candidate.isDirectClassSuperType() }
    }

    private fun ConeCangJieType.isDirectClassSuperType(): Boolean = when (this) {
        is ConeClassLikeType -> !isInterface
        is ConeStructType, is ConeEnumType -> true
        else -> false
    }

    // ── Small Extension Utilities ─────────────────────────────────────────────

    private fun CfirExpression.resolveIndependently() {
        transform<CfirElement, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
    }

    private fun CfirExpression.resolveIndependently(body: CfirBlock?) {
        body?.transform<CfirElement, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
    }

    private val ResolutionMode.expectedTypeOrNull: ConeCangJieType?
        get() = (this as? ResolutionMode.WithExpectedType)?.expectedTypeRef?.coneType

    /**
     * 对齐官方 `SynRangeExprInferElemTy` 的推断顺序。
     */
    private fun inferRangeElementType(rangeExpression: CfirRangeExpression): ConeCangJieType {
        val startType = rangeExpression.start.coneTypeOrNull
        val useStartType = rangeExpression.start !is CfirLiteralExpression || rangeExpression.end is CfirLiteralExpression
        if (startType != null && startType !is ConeErrorType && !startType.isNothing && useStartType) {
            return normalizeRangeElementType(startType)
        }

        val endType = rangeExpression.end.coneTypeOrNull
        if (endType != null && endType !is ConeErrorType && !endType.isNothing) {
            return normalizeRangeElementType(endType)
        }

        if (startType != null && startType !is ConeErrorType) {
            return normalizeRangeElementType(startType)
        }

        return ConePrimitiveType.INT64
    }

    private fun normalizeRangeElementType(type: ConeCangJieType): ConeCangJieType {
        val normalized = IdealTypeResolver.resolveIfIdeal(type, null)
        return if (normalized is ConePrimitiveType && normalized.kind == PrimitiveTypeKind.IDEAL_INT) {
            ConePrimitiveType.INT64
        } else {
            normalized
        }
    }

    private fun ConeCangJieType.rangeTypeOrNull(): ConeClassifierType? = when (this) {
        is ConeClassLikeType -> takeIf { classId == StdlibClassIds.Range }
        is ConeStructType -> takeIf { classId == StdlibClassIds.Range }
        is ConeTypeAliasType -> expandedType?.rangeTypeOrNull()
        else -> null
    }

    private fun <T : CfirQualifiedAccessExpression> resolveAccessTypeArguments(access: T): T {
        if (access.typeArguments.isEmpty()) return access

        val additionalTypeParameters = context.containers
            .asSequence()
            .filterIsInstance<CfirDeclaration>()
            .flatMap { extractTypeParameters(it).asSequence() }
            .toList()

        val config = CfirTypeResolutionConfiguration(
            useSiteFile = context.file,
            topContainer = context.containers.lastOrNull(),
        ).withAdditionalTypeParameters(additionalTypeParameters)

        val resolvedTypeArguments = access.typeArguments.map { typeRef ->
            when (typeRef) {
                is CfirResolvedTypeRef -> typeRef
                is CfirImplicitTypeRef -> typeRef
                else -> specificTypeResolverTransformer.transformTypeRef(typeRef, config)
            }
        }

        access.replaceTypeArguments(resolvedTypeArguments)
        return access
    }

    private fun resolveSuperTypeRef(typeRef: CfirTypeRef): CfirTypeRef {
        if (typeRef is CfirResolvedTypeRef || typeRef is CfirImplicitTypeRef) return typeRef

        val additionalTypeParameters = context.containers
            .asSequence()
            .filterIsInstance<CfirDeclaration>()
            .flatMap { extractTypeParameters(it).asSequence() }
            .toList()

        val config = CfirTypeResolutionConfiguration(
            useSiteFile = context.file,
            topContainer = context.containers.lastOrNull(),
        ).withAdditionalTypeParameters(additionalTypeParameters)

        return specificTypeResolverTransformer.transformTypeRef(typeRef, config)
    }

    private fun extractTypeParameters(declaration: CfirDeclaration): List<CfirTypeParameter> = when (declaration) {
        is CfirClass -> declaration.typeParameters
        is CfirInterface -> declaration.typeParameters
        is CfirStruct -> declaration.typeParameters
        is CfirEnum -> declaration.typeParameters
        is CfirFunction -> declaration.typeParameters
        is CfirConstructor -> declaration.typeParameters
        is CfirProperty -> declaration.typeParameters
        is CfirFieldVariable -> declaration.typeParameters
        is CfirValueParameter -> declaration.typeParameters
        is CfirExtend -> declaration.typeParameters
        is CfirTypeAlias -> declaration.typeParameters
        is CfirPatternVariable -> declaration.typeParameters
        is CfirMacroDeclaration -> declaration.typeParameters
        is CfirMainFunction -> declaration.typeParameters
        is CfirFinalizer -> declaration.typeParameters
        is CfirEnumConstructor -> declaration.typeParameters
        else -> emptyList()
    }
}
