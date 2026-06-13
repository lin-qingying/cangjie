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

import org.cangnova.cangjie.ImportPath
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ConeCannotRefToPackageNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeFunctionExpectedError
import org.cangnova.cangjie.cfir.diagnostic.ConeFunctionCallExpectedError
import org.cangnova.cangjie.cfir.diagnostic.ConeNoConstructorError
import org.cangnova.cangjie.cfir.diagnostic.ConeNoMatchingInvokeOperatorError
import org.cangnova.cangjie.cfir.diagnostic.ConeNotMemberOfError
import org.cangnova.cangjie.cfir.diagnostic.ConePackageNameConflictError
import org.cangnova.cangjie.cfir.diagnostic.ConeResolutionToClassifierError
import org.cangnova.cangjie.cfir.diagnostic.ConeHiddenCandidateError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnableToInferGenericFuncError
import org.cangnova.cangjie.cfir.diagnostic.TooManyArguments
import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCallOrigin
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.expressions.unwrapSmartcastExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.CollectionLiteralOuterCandidateContext
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.createConeDiagnosticForCandidateWithError
import org.cangnova.cangjie.cfir.resolve.doesResolutionResultOverrideOtherToPreserveCompatibility
import org.cangnova.cangjie.cfir.resolve.expectedType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedClass
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.functionTypeForFunctionValueCandidate
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.BuiltinArrayConstructorKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CandidateFactory
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirAllCandidatesCollector
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidateCollector
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.createErrorReferenceWithErrorCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.createErrorReferenceWithExistingCandidate
import org.cangnova.cangjie.cfir.resolve.calls.overloads.ConeCallConflictResolver
import org.cangnova.cangjie.cfir.resolve.calls.overloads.CfirOverloadByLambdaBodyResolver
import org.cangnova.cangjie.cfir.resolve.calls.overloads.callConflictResolverFactory
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStageRunner
import org.cangnova.cangjie.cfir.resolve.calls.stages.fullyProcessCandidate
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerGroup
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.resolve.typeFromCallee
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.scopes.impl.staticScopeForQualifierType
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.AbstractCandidate
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInArguments
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIdealLiteralType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.utils.runIf

class CfirCallResolver(
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val towerResolver: CfirTowerResolver =
        CfirTowerResolver(components, components.resolutionStageRunner),
) : SessionHolder {

    override val session: CfirSession get() = components.session

    private lateinit var transformer: CfirExpressionsResolveTransformer

    fun initTransformer(transformer: CfirExpressionsResolveTransformer) {
        this.transformer = transformer
    }

    val conflictResolver: ConeCallConflictResolver =
        session.callConflictResolverFactory.create(session.inferenceComponents, components)

    private val overloadByLambdaBodyResolver: CfirOverloadByLambdaBodyResolver by lazy(LazyThreadSafetyMode.NONE) {
        CfirOverloadByLambdaBodyResolver(components, conflictResolver)
    }

    @ApplicabilityDetail
    private val ResolutionResult.isSuccess: Boolean
        get() = applicability.isSuccess

    fun resolveCallAndSelectCandidate(
        functionCall: CfirFunctionCall,
        resolutionMode: ResolutionMode,
        collectionLiteralContext: CollectionLiteralOuterCandidateContext? = null,
    ): CfirFunctionCall {
        val callee = functionCall.calleeReference as? CfirNamedReference ?: return functionCall
        // 导入包限定符只参与静态包成员查找；函数调用路径同样不能把它保留为 dispatch receiver。
        if (functionCall.explicitReceiver?.importedPackageQualifierOrNull(components.file, session) != null) {
            functionCall.replaceDispatchReceiver(null)
        }
        val isCollectionLiteralCall = collectionLiteralContext != null
        val result = collectCandidates(
            qualifiedAccess = functionCall,
            name = callee.name,
            origin = functionCall.origin,
            resolutionMode = resolutionMode,
            collectionLiteralContext = collectionLiteralContext,
        )
        var effectiveResult = result
        var expectedCallKind: CallKind? = null
        var expectedCandidates: Collection<Candidate>? = null
        var matchedClassifier: CfirClassLikeSymbol<*>? = null
        var classLikeCallResolved = false
        if (!isCollectionLiteralCall && callee.name.asString() == "Array") {
            val classifier = findClassifierForCall(functionCall, callee.name)
            if (classifier != null && classifier.isStdlibArrayClassifier()) {
                effectiveResult = collectBuiltinArrayConstructorCandidates(
                    functionCall = functionCall,
                    classifier = classifier,
                    resolutionMode = resolutionMode,
                )
                classLikeCallResolved = true
            }
        }
        if (!classLikeCallResolved && result.candidates.isEmpty() && !isCollectionLiteralCall) {
            // 阶段2a：普通函数搜索为空时，先尝试枚举构造器搜索（对齐官方两阶段语义：普通函数完全遮蔽枚举构造器）
            val enumResult = collectCandidates(
                functionCall,
                callee.name,
                CallKind.EnumConstructorCall,
                origin = functionCall.origin,
                resolutionMode = resolutionMode,
            )
            if (enumResult.candidates.isNotEmpty()) {
                effectiveResult = enumResult
            } else {
                // 阶段2b：枚举构造器也未找到，fallback 到 NamedValueAccess（无参枚举作为值访问等场景）
                val variableAccessResult = collectCandidates(
                    functionCall,
                    callee.name,
                    CallKind.NamedValueAccess,
                    origin = functionCall.origin,
                    resolutionMode = resolutionMode,
                )
                matchedClassifier = findClassifierForCall(functionCall, callee.name)
                val constructorResult = matchedClassifier?.let { classifier ->
                    collectClassConstructorCandidates(
                        functionCall = functionCall,
                        classifier = classifier,
                        resolutionMode = resolutionMode,
                    )
                }
                if (constructorResult != null && constructorResult.candidates.isNotEmpty()) {
                    effectiveResult = constructorResult
                    matchedClassifier = null
                } else if (variableAccessResult.candidates.isNotEmpty()) {
                    expectedCallKind = CallKind.NamedValueAccess
                    expectedCandidates = variableAccessResult.candidates
                }
            }
        }

        if (matchedClassifier == null && effectiveResult.candidates.isEmpty() && expectedCandidates == null) {
            matchedClassifier = findClassifierForCall(functionCall, callee.name)
        }

        val nameReference = createResolvedNamedReference(
            callee,
            callee.name,
            effectiveResult.info,
            effectiveResult.candidates,
            effectiveResult.applicability,
            functionCall.explicitReceiver,
            matchedClassifier = matchedClassifier,
            expectedCallKind = expectedCallKind,
            expectedCandidates = expectedCandidates,
        )

        functionCall.replaceCalleeReference(nameReference)
        val candidate = (nameReference as? CfirNamedReferenceWithCandidate)?.candidate
        reportBodyResolutionErrorToOverloadByLambdaCandidate(nameReference, candidate)
        candidate?.updateSourcesOfReceivers()
        return functionCall
    }

    private fun CfirClassLikeSymbol<*>?.isStdlibArrayClassifier(): Boolean {
        val actualClassifier = (this as? CfirTypeAliasSymbol)?.fullyExpandedClass(session) ?: this
        return actualClassifier?.classId == StdlibClassIds.Array
    }

    fun resolveNamedValueAccessAndSelectCandidate(
        qualifiedAccess: CfirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        isUsedAsGetClassReceiver: Boolean,
        callSite: CfirElement,
        resolutionMode: ResolutionMode,
    ): CfirExpression {
        return resolveNamedValueAccessAndSelectCandidateImpl(
            qualifiedAccess = qualifiedAccess,
            isUsedAsReceiver = isUsedAsReceiver,
            resolutionMode = resolutionMode,
            isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
            callSite = callSite,
        ) { true }
    }

    @Deprecated(
        message = "Use resolveNamedValueAccessAndSelectCandidate instead",
        replaceWith = ReplaceWith(
            "resolveNamedValueAccessAndSelectCandidate(qualifiedAccess, isUsedAsReceiver, isUsedAsGetClassReceiver, callSite, resolutionMode)"
        ),
    )
    fun resolveVariableAccessAndSelectCandidate(
        qualifiedAccess: CfirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        isUsedAsGetClassReceiver: Boolean,
        callSite: CfirElement,
        resolutionMode: ResolutionMode,
    ): CfirExpression =
        resolveNamedValueAccessAndSelectCandidate(
            qualifiedAccess = qualifiedAccess,
            isUsedAsReceiver = isUsedAsReceiver,
            isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
            callSite = callSite,
            resolutionMode = resolutionMode,
        )

    @OptIn(ApplicabilityDetail::class)
    private fun resolveNamedValueAccessAndSelectCandidateImpl(
        qualifiedAccess: CfirQualifiedAccessExpression,
        isUsedAsReceiver: Boolean,
        resolutionMode: ResolutionMode,
        isUsedAsGetClassReceiver: Boolean,
        callSite: CfirElement = qualifiedAccess,
        acceptCandidates: (Collection<Candidate>) -> Boolean,
    ): CfirExpression {
        val callee = qualifiedAccess.calleeReference as? CfirNamedReference ?: return qualifiedAccess

        val transformedAccess = transformer.transformExplicitReceiverOf(qualifiedAccess)
        // 导入包限定符对齐 Kotlin FirResolvedQualifier / PackageQualifierReceiver：
        // 它只提供静态成员查找作用域，不是运行期值接收者，不能继续保留为 dispatch receiver。
        if (transformedAccess.explicitReceiver?.importedPackageQualifierOrNull(components.file, session) != null) {
            transformedAccess.replaceDispatchReceiver(null)
        }

        val basicResult by lazy(LazyThreadSafetyMode.NONE) {
            collectCandidates(
                qualifiedAccess = transformedAccess,
                name = callee.name,
                isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
                callSite = callSite,
                resolutionMode = resolutionMode,
            )
        }

        if (isUsedAsReceiver) {
            val importedPackageQualifier = components.file.resolveImportedPackageQualifier(callee.name, session)
            if (importedPackageQualifier != null) {
                if (importedPackageQualifier.isAmbiguous) {
                    transformedAccess.replaceCalleeReference(
                        buildErrorNamedReference {
                            source = callee.source
                            name = callee.name
                            diagnostic = ConePackageNameConflictError(callee.name)
                        }
                    )
                } else {
                    // 当前 CFIR 没有 Kotlin `FirResolvedQualifier` 的独立节点。
                    // 已确认的导入包限定符在表达式管线中需要一个非错误、无值的稳定类型，
                    // 后续成员查找仍由 import binding 推导出的 package member scope 承担。
                    transformedAccess.replaceConeTypeOrNull(session.builtinTypes.unitType)
                }
                return transformedAccess
            }
        }

        if (isUsedAsReceiver || !basicResult.isSuccess) {
            val classifierFromQualifier = transformedAccess.explicitReceiver?.let {
                findClassifierInQualifierScope(it, callee.name)
            }
            if (classifierFromQualifier != null) {
                transformedAccess.replaceCalleeReference(
                    buildResolvedNamedReference {
                        source = callee.source
                        name = callee.name
                        resolvedSymbol = classifierFromQualifier
                    }
                )
                return transformedAccess
            }
        }

        var result = basicResult

        if (transformedAccess.explicitReceiver == null) {
            if (!result.isSuccess || (isUsedAsReceiver && result.candidates.all { it.symbol is CfirClassLikeSymbol<*> })) {
                val classifier = towerResolver.findClassifiers(callee.name).firstOrNull()
                if (classifier != null) {
                    transformedAccess.replaceCalleeReference(
                        buildResolvedNamedReference {
                            source = callee.source
                            name = callee.name
                            resolvedSymbol = classifier
                        }
                    )
                    return transformedAccess
                }
            }
        }

        val shouldTryEnumValueAccess =
            !isUsedAsReceiver &&
                    transformedAccess !is CfirFunctionCall &&
                    (result.candidates.isEmpty() || result.candidates.all { it.symbol is CfirEnumConstructorSymbol })

        var functionCallExpected = false
        if (shouldTryEnumValueAccess) {
            // 先尝试枚举构造器（作为值访问，对应无参枚举构造器的直接引用）
            val enumResult = collectCandidates(
                qualifiedAccess = transformedAccess,
                name = callee.name,
                forceCallKind = CallKind.EnumConstructorCall,
                isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
                callSite = callSite,
                resolutionMode = resolutionMode,
            )
            if (enumResult.candidates.isNotEmpty()) {
                result = enumResult
            } else if (result.candidates.isEmpty()) {
                val newResult = collectCandidates(
                    qualifiedAccess = transformedAccess,
                    name = callee.name,
                    forceCallKind = CallKind.Function,
                    isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
                    callSite = callSite,
                    resolutionMode = resolutionMode,
                )
                if (newResult.candidates.isNotEmpty()) {
                    result = newResult
                    functionCallExpected = newResult.applicability > CandidateApplicability.INAPPLICABLE_WRONG_RECEIVER
                }
            }
        }

        val reducedCandidates = result.candidates
        if (!acceptCandidates(reducedCandidates)) return transformedAccess

        val nameReference = createResolvedNamedReference(
            reference = callee,
            name = callee.name,
            callInfo = result.info,
            candidates = reducedCandidates,
            applicability = result.applicability,
            explicitReceiver = transformedAccess.explicitReceiver,
            expectedCallKind = if (functionCallExpected) CallKind.Function else null,
        )

        transformedAccess.replaceCalleeReference(nameReference)
        if (reducedCandidates.size == 1) {
            val candidate = reducedCandidates.single()
            reportBodyResolutionErrorToOverloadByLambdaCandidate(nameReference, candidate)
            candidate.updateSourcesOfReceivers()
        } else {
            reportBodyResolutionErrorToOverloadByLambdaCandidate(nameReference, null)
        }
        transformer.storeTypeFromCallee(transformedAccess)
        return transformedAccess
    }

    private fun reportBodyResolutionErrorToOverloadByLambdaCandidate(
        reference: CfirReference,
        selectedCandidate: Candidate?,
    ) {
        if (reference is CfirErrorNamedReference || selectedCandidate?.isSuccessful == false) {
            components.context.reportOverloadByLambdaCandidateDiagnostic(ErrorTypeInArguments)
        }
    }

    fun collectAllCandidates(
        qualifiedAccess: CfirQualifiedAccessExpression,
        name: Name,
        containingDeclarations: List<CfirDeclaration> = transformer.components.containingDeclarations,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
        resolutionMode: ResolutionMode,
    ): List<OverloadCandidate> {
        val collector = AllCandidatesCollector(components, components.resolutionStageRunner)
        var result = collectCandidates(
            qualifiedAccess = qualifiedAccess,
            name = name,
            containingDeclarations = containingDeclarations,
            resolutionContext = resolutionContext,
            collector = collector,
            resolutionMode = resolutionMode,
        )

        if (result.candidates.isEmpty() && qualifiedAccess !is CfirFunctionCall) {
            val functionResult = collectCandidates(
                qualifiedAccess = qualifiedAccess,
                name = name,
                forceCallKind = CallKind.Function,
                containingDeclarations = containingDeclarations,
                resolutionContext = resolutionContext,
                collector = collector,
                resolutionMode = resolutionMode,
            )
            if (functionResult.candidates.isNotEmpty()) {
                result = functionResult
            }
        }

        return collector.allCandidates.map { candidate ->
            OverloadCandidate(candidate, isInBestCandidates = candidate in result.candidates)
        }
    }

    private fun collectCandidates(
        qualifiedAccess: CfirQualifiedAccessExpression,
        name: Name,
        forceCallKind: CallKind? = null,
        isUsedAsGetClassReceiver: Boolean = false,
        origin: CfirFunctionCallOrigin = CfirFunctionCallOrigin.Regular,
        containingDeclarations: List<CfirDeclaration> = transformer.components.containingDeclarations,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
        collector: CfirCandidateCollector? = null,
        callSite: CfirElement = qualifiedAccess,
        resolutionMode: ResolutionMode,
        collectionLiteralContext: CollectionLiteralOuterCandidateContext? = null,
    ): ResolutionResult {
        val explicitReceiver = qualifiedAccess.explicitReceiver
        val arguments = (qualifiedAccess as? CfirFunctionCall)?.argumentList?.arguments ?: emptyList()
        val typeArguments = qualifiedAccess.typeArguments

        val callKind = when {
            forceCallKind != null -> forceCallKind
            collectionLiteralContext != null -> CallKind.Function
            qualifiedAccess is CfirFunctionCall -> CallKind.Function
            else -> CallKind.NamedValueAccess
        }

        val info = CallInfo(
            callSite = callSite,
            callKind = callKind,
            name = name,
            origin = origin,
            explicitReceiver = explicitReceiver,
            arguments = arguments,
            isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
            typeArguments = typeArguments,
            session = session,
            containingFile = components.file,
            containingDeclarations = containingDeclarations,
            resolutionMode = resolutionMode,
            containingCandidateForCollectionLiteral = collectionLiteralContext?.containingCandidate,
        )

        return collectCandidates(info = info, resolutionContext = resolutionContext, collector = collector)
    }

    private fun collectCandidates(
        info: CallInfo,
        resolutionContext: ResolutionContext,
        collector: CfirCandidateCollector? = null,
    ): ResolutionResult {
        val resultCollector = towerResolver.runResolver(info, resolutionContext, collector)
        var (reducedCandidates, applicability) = reduceCandidates(resultCollector)
        reducedCandidates = reduceFunctionValueCandidatesByExpectedType(info, reducedCandidates)
        val callSite = info.callSite
        if (callSite is CfirQualifiedAccessExpression && components.context.shouldReduceOverloadByLambdaCandidates()) {
            reducedCandidates = overloadByLambdaBodyResolver.reduceCandidates(callSite, reducedCandidates)
        }

        return ResolutionResult(
            info = info,
            applicability = applicability,
            candidates = reducedCandidates,
            forwardedDiagnostics = resultCollector.forwardedDiagnostics(),
        )
    }

    /**
     * 有目标函数类型时，函数名作为值的重载引用按完整函数类型过滤。
     *
     * 官方 Sema 在 `ChkRefExpr` 中会把目标类型传给 `CollectValidFuncTys`，
     * 只有候选函数类型可作为目标函数类型的子类型时才保留；没有目标类型时才继续报告歧义。
     */
    private fun reduceFunctionValueCandidatesByExpectedType(
        info: CallInfo,
        candidates: Set<Candidate>,
    ): Set<Candidate> {
        if (candidates.size <= 1 || info.callKind != CallKind.NamedValueAccess) return candidates
        val expectedFunctionType = info.resolutionMode.expectedType
            ?.fullyExpandedType() as? ConeFunctionType ?: return candidates

        val functionCandidates = candidates.filter { candidate ->
            candidate.symbol.takeIf { it.isBound }?.cfir is CfirFunction
        }
        if (functionCandidates.size != candidates.size) return candidates

        val matchingCandidates = functionCandidates.filterTo(linkedSetOf()) { candidate ->
            val functionType = components.functionTypeForFunctionValueCandidate(candidate)
            AbstractTypeChecker.isSubtypeOf(session.typeContext, functionType, expectedFunctionType)
        }

        return when (matchingCandidates.size) {
            0 -> candidates
            1 -> matchingCandidates
            else -> conflictResolver.chooseMaximallySpecificCandidates(matchingCandidates)
        }
    }

    private fun reduceCandidates(
        collector: CfirCandidateCollector,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
    ): Pair<Set<Candidate>, CandidateApplicability> {
        return reduceCollectedCandidates(
            candidates = collector.bestCandidates(),
            collectorApplicability = collector.currentApplicability,
            isCandidateSuccessful = Candidate::isSuccessful,
            candidateApplicability = Candidate::lowestApplicability,
            fullyProcessCandidate = { candidate ->
                components.resolutionStageRunner.fullyProcessCandidate(candidate, resolutionContext)
            },
            chooseMostSpecific = { candidates ->
                candidates.singleOrNull()?.let(::setOf)
                    ?: conflictResolver.chooseMaximallySpecificCandidates(candidates)
            },
        )
    }

    private fun createResolvedNamedReference(
        reference: CfirReference,
        name: Name,
        callInfo: CallInfo,
        candidates: Collection<Candidate>,
        applicability: CandidateApplicability,
        explicitReceiver: CfirExpression? = null,
        createResolvedReferenceWithoutCandidateForLocalVariables: Boolean = true,
        matchedClassifier: CfirClassLikeSymbol<*>? = null,
        expectedCallKind: CallKind? = null,
        expectedCandidates: Collection<Candidate>? = null,
    ): CfirNamedReference {
        val source = reference.source
        val operatorToken = runIf(callInfo.origin == CfirFunctionCallOrigin.Operator) {
            OperatorNameConventions.TOKENS_BY_OPERATOR_NAME[name]
        }
        val argumentTypes = callInfo.arguments.mapNotNull { it.coneTypeOrNull }

        // 根据期望的调用种类生成诊断
        val diagnostic = when {
            expectedCallKind != null -> when (expectedCallKind) {
                CallKind.Function -> {
                    val hasValueParameters = candidates.any {
                        (it.symbol as? CfirFunctionSymbol<*>)?.valueParameterSymbols?.isNotEmpty() == true
                    }
                    ConeFunctionCallExpectedError(name, hasValueParameters, candidates as Collection<AbstractCallCandidate<*>>)
                }
                else -> {
                    val singleExpectedCandidate = expectedCandidates?.singleOrNull()
                    var symbol = singleExpectedCandidate?.symbol
                    if (symbol is CfirTypeAliasSymbol) {
                        symbol = symbol.fullyExpandedClass(session) ?: symbol
                    }

                    when (symbol) {
                        is CfirClassLikeSymbol<*> -> {
                            ConeResolutionToClassifierError(singleExpectedCandidate!!, symbol)
                        }
                        else -> {
                            val receiverType = explicitReceiver?.coneTypeOrNull
                            when {
                                receiverType != null && !receiverType.isUnit -> {
                                    val declarationType = (symbol as? CfirCallableSymbol<*>)?.let {
                                        components.returnTypeCalculator.tryCalculateReturnType(it.cfir).coneType
                                    }

                                    if (singleExpectedCandidate?.isSuccessful == false && declarationType is ConeFunctionType) {
                                        createConeDiagnosticForCandidateWithError(
                                            singleExpectedCandidate.lowestApplicability,
                                            singleExpectedCandidate,
                                        )
                                    } else {
                                        ConeFunctionExpectedError(
                                            name.asString(),
                                            declarationType ?: receiverType,
                                        )
                                    }
                                }
                                singleExpectedCandidate != null && !singleExpectedCandidate.isSuccessful -> {
                                    createConeDiagnosticForCandidateWithError(
                                        singleExpectedCandidate.lowestApplicability,
                                        singleExpectedCandidate,
                                    )
                                }
                                else -> ConeUnresolvedNameError(name, operatorToken, argumentTypes = argumentTypes)
                            }
                        }
                    }
                }
            }

            candidates.isEmpty() -> {
                when {
                    explicitReceiver?.importedPackageQualifierOrNull(components.file, session)?.packageFqName != null ->
                        ConeNotMemberOfError(
                            memberName = name,
                            kind = if (callInfo.callKind == CallKind.Function) "method" else "member",
                            typeName = explicitReceiver.importedPackageQualifierNameOrNull() ?: name,
                        )
                    explicitReceiver?.diagnosticFromCalleeReference() != null ->
                        ConeUnreportedDuplicateDiagnostic(explicitReceiver.diagnosticFromCalleeReference()!!)
                    matchedClassifier != null && callInfo.callKind == CallKind.Function -> {
                        val actualClassifier = (matchedClassifier as? CfirTypeAliasSymbol)?.fullyExpandedClass(session)
                            ?: matchedClassifier
                        val actualDeclaration = actualClassifier.cfir as? CfirClassLikeDeclaration
                        if (actualDeclaration is org.cangnova.cangjie.cfir.declarations.CfirEnum) {
                            ConeNoMatchingInvokeOperatorError(actualClassifier.name, actualClassifier.constructType())
                        } else {
                            ConeNoConstructorError
                        }
                    }
                    name.asString() == "invoke" && explicitReceiver is CfirLiteralExpression ->
                        ConeFunctionExpectedError(
                            explicitReceiver.value?.toString() ?: "",
                            explicitReceiver.coneTypeOrNull ?: components.typeFromCallee(reference),
                        )
                    else -> {
                        val receiverType = explicitReceiver?.coneTypeOrNull
                        when {
                            receiverType is ConeClassLikeType && receiverType.isInterface -> ConeNoConstructorError
                            // 裸名若与已知包 FqName 重合,归类为"不能独立引用包名",
                            // 对齐 C++ sema_cannot_ref_to_pkg_name。
                            explicitReceiver == null && components.symbolProvider.hasPackage(
                                org.cangnova.cangjie.name.FqName.topLevel(name)
                            ) -> ConeCannotRefToPackageNameError(
                                org.cangnova.cangjie.name.FqName.topLevel(name)
                            )
                            // 保留 unresolved 兜底分类；macro/finalizer 等 function-like 声明
                            // 的局部可见性由 BODY_RESOLVE 入口建立作用域，这里不再为其做补丁判定。
                            else ->
                                ConeUnresolvedNameError(name, operatorToken, receiverType, argumentTypes)
                        }
                    }
                }
            }

            candidates.size > 1 -> {
                val candidatesWithErrors = candidates.associateWith {
                    runIf(!it.isSuccessful) { createConeDiagnosticForCandidateWithError(it.applicability, it) }
                }
                ConeAmbiguityError(
                    name,
                    applicability,
                    candidatesWithErrors as Map<AbstractCandidate, ConeDiagnostic?>,
                    isCallLike = callInfo.callKind == CallKind.Function || callInfo.callKind == CallKind.EnumConstructorCall,
                )
            }

            else -> {
                val candidate = candidates.single()
                when {
                    !candidate.isSuccessful -> createConeDiagnosticForCandidateWithError(applicability, candidate)
                    candidate.isBareGenericEnumValueConstructorWithoutMatchingExpectedType() -> ConeSimpleDiagnostic(
                        "generic enum constructor should be used with type argument",
                        DiagnosticKind.GenericTypeWithoutTypeArgument,
                    )
                    candidate.hasUninferableBareStaticGenericQualifier() -> ConeUnableToInferGenericFuncError()
                    else -> null
                }
            }
        }

        if (diagnostic != null) {
            return createErrorReferenceForSingleCandidate(
                candidates.singleOrNull(),
                diagnostic as ConeDiagnostic,
                callInfo,
                source,
            )
        }

        // 成功的候选
        val candidate = candidates.single()

        // 优化：对于没有类型参数的局部变量或属性，直接创建已解析引用而不保留候选
        val symbol = candidate.symbol
        val canDropCandidateForNamedValueAccess = when (symbol) {
            is CfirPropertySymbol -> symbol.cfir.typeParameters.isEmpty()
            is CfirVariableSymbol -> true
            else -> false
        }

        if (
            !candidate.usedOuterCs && callInfo.callKind == CallKind.NamedValueAccess && explicitReceiver?.coneTypeOrNull !is ConeIdealLiteralType && canDropCandidateForNamedValueAccess && createResolvedReferenceWithoutCandidateForLocalVariables && !candidate.doesResolutionResultOverrideOtherToPreserveCompatibility()
        ) {
            return buildResolvedNamedReference {
                this.source = source
                this.name = name
                resolvedSymbol = symbol
            }
        }

        return CfirNamedReferenceWithCandidate(source, name, candidate)
    }

    private fun CfirExpression.diagnosticFromCalleeReference(): ConeDiagnostic? =
        ((this as? CfirResolvable)?.calleeReference as? CfirDiagnosticHolder)?.diagnostic

    /**
     * `Box.create()` 这类裸泛型类名静态成员调用属于调用推断错误。
     * 当 owner 泛型参数没有显式实参，且完全没有出现在可调用签名中时，
     * 调用上下文不可能为这些参数提供约束。
     */
    private fun Candidate.hasUninferableBareStaticGenericQualifier(): Boolean {
        val callable = symbol.cfir as? CfirCallableDeclaration ?: return false
        if (!callable.status.isStatic) return false

        val receiver = callInfo.explicitReceiver as? CfirQualifiedAccessExpression ?: return false
        if (receiver.typeArguments.isNotEmpty()) return false

        val owner = receiver.unwrapSmartcastExpression().resolvedQualifierClassifier(session)?.cfir
            as? CfirClassLikeDeclaration ?: return false
        val ownerTypeParameterSymbols = owner.typeParameters.mapTo(linkedSetOf()) { it.symbol }
        if (ownerTypeParameterSymbols.isEmpty()) return false

        val signatureTypes = buildList {
            callable.returnTypeRef.coneTypeOrNull?.let(::add)
            when (callable) {
                is CfirFunction -> callable.valueParameters.mapNotNullTo(this) { it.returnTypeRef.coneTypeOrNull }
                is CfirConstructor -> callable.valueParameters.mapNotNullTo(this) { it.returnTypeRef.coneTypeOrNull }
                is CfirEnumConstructor -> callable.valueParameters.mapNotNullTo(this) { it.returnTypeRef.coneTypeOrNull }
                else -> Unit
            }
        }

        return ownerTypeParameterSymbols.any { ownerTypeParameter ->
            signatureTypes.none { type -> type.referencesTypeParameter(ownerTypeParameter) }
        }
    }

    /**
     * 官方 enum sugar 中，无参泛型 enum constructor 在没有同 owner enum 的期望类型、
     * 且没有显式类型实参时，报告裸泛型类型实参缺失。
     */
    private fun Candidate.isBareGenericEnumValueConstructorWithoutMatchingExpectedType(): Boolean {
        val enumConstructor = symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor ?: return false
        if (enumConstructor.valueParameters.isNotEmpty()) return false
        if (callInfo.typeArguments.isNotEmpty()) return false

        val enumConstructorSymbol = symbol as? CfirEnumConstructorSymbol ?: return false
        val ownerClassId = session.cfirProvider.getContainingClass(enumConstructorSymbol)?.classId ?: return false
        val ownerEnum = session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir as? CfirEnum
            ?: return false
        if (ownerEnum.typeParameters.isEmpty()) return false

        val expectedEnumClassId = callInfo.resolutionMode.expectedType
            ?.fullyExpandedType()
            ?.enumConstructorOwnerClassIdOrNull()
        return expectedEnumClassId != ownerClassId
    }

    private fun ConeCangJieType.enumConstructorOwnerClassIdOrNull(): ClassId? = when (this) {
        is ConeEnumType -> classId
        is ConeClassLikeType -> classId.takeIf { it == StdlibClassIds.Option }
        else -> null
    }

    private fun ConeCangJieType.referencesTypeParameter(
        symbol: org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol,
    ): Boolean = contains { type ->
        type is ConeTypeParameterType && type.lookupTag.typeParameterSymbol == symbol
    }

    private fun collectClassConstructorCandidates(
        functionCall: CfirFunctionCall,
        classifier: CfirClassLikeSymbol<*>,
        resolutionMode: ResolutionMode,
    ): ResolutionResult {
        val actualClassifier = (classifier as? CfirTypeAliasSymbol)?.fullyExpandedClass(session) ?: classifier
        actualClassifier.lazyResolveToPhase(CfirResolvePhase.TYPES)
        val actualDeclaration = actualClassifier.cfir
        if (actualClassifier.classId == StdlibClassIds.Array) {
            return collectBuiltinArrayConstructorCandidates(
                functionCall = functionCall,
                classifier = classifier,
                resolutionMode = resolutionMode,
            )
        }
        if (actualDeclaration is org.cangnova.cangjie.cfir.declarations.CfirEnum) {
            return ResolutionResult(
                info = createClassifierCallInfo(functionCall, classifier, resolutionMode),
                applicability = CandidateApplicability.HIDDEN,
                candidates = emptyList(),
                forwardedDiagnostics = emptyList(),
            )
        }

        val constructorSymbols = actualClassifier.cfir.declarations
            .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirConstructor>()
            .map(CfirConstructor::symbol)
        if (constructorSymbols.isEmpty()) {
            return ResolutionResult(
                info = createClassifierCallInfo(functionCall, classifier, resolutionMode),
                applicability = CandidateApplicability.HIDDEN,
                candidates = emptyList(),
                forwardedDiagnostics = emptyList(),
            )
        }

        val callInfo = createClassifierCallInfo(functionCall, classifier, resolutionMode)
        val candidateFactory = CandidateFactory(transformer.resolutionContext, callInfo)
        val constructorCandidates = constructorSymbols.map { constructorSymbol ->
            candidateFactory.createCandidate(
                callInfo = callInfo,
                symbol = constructorSymbol,
                originScope = null,
            )
        }
        val (reducedCandidates, applicability) = reduceCollectedCandidates(
            candidates = constructorCandidates,
            // 这些候选是当前函数内即时创建的，并没有像 tower collector 那样预先跑过 stages。
            // 必须先完整处理后，再根据适用性做筛选。
            collectorApplicability = CandidateApplicability.HIDDEN,
            isCandidateSuccessful = Candidate::isSuccessful,
            candidateApplicability = Candidate::lowestApplicability,
            fullyProcessCandidate = { candidate ->
                components.resolutionStageRunner.fullyProcessCandidate(candidate, transformer.resolutionContext)
            },
            chooseMostSpecific = { currentCandidates ->
                currentCandidates.singleOrNull()?.let(::setOf)
                    ?: conflictResolver.chooseMaximallySpecificCandidates(currentCandidates)
            },
        )
        return ResolutionResult(
            info = callInfo,
            applicability = applicability,
            candidates = reducedCandidates,
            forwardedDiagnostics = emptyList(),
        )
    }

    /**
     * 仓颉 `Array<T>(...)` 对应官方 `ArrayExpr` 内建表达式，
     * 不是 `std.core.Array` 的用户构造器。这里只在 classifier fallback
     * 层合成官方允许的调用形状，后续仍交给统一 call-resolution stages
     * 处理显式类型实参、命名实参、lambda 期望类型与约束系统。
     */
    private fun collectBuiltinArrayConstructorCandidates(
        functionCall: CfirFunctionCall,
        classifier: CfirClassLikeSymbol<*>,
        resolutionMode: ResolutionMode,
    ): ResolutionResult {
        val callInfo = createClassifierCallInfo(functionCall, classifier, resolutionMode)
        val candidateFactory = CandidateFactory(transformer.resolutionContext, callInfo)
        val argumentCount = callInfo.arguments.size

        val arrayCandidates = if (argumentCount > 2) {
            listOf(
                candidateFactory.createBuiltinArrayConstructorCandidate(
                    callInfo = callInfo,
                    kind = BuiltinArrayConstructorKind.INIT_FUNCTION,
                ).also { candidate ->
                    candidate.addDiagnostic(TooManyArguments(functionCall, callInfo.name))
                },
            )
        } else {
            val arrayCandidateKinds = when (argumentCount) {
                0 -> listOf(BuiltinArrayConstructorKind.EMPTY)
                1 -> if (functionCall.hasTrailingLambda) {
                    listOf(BuiltinArrayConstructorKind.INIT_FUNCTION)
                } else {
                    listOf(BuiltinArrayConstructorKind.COLLECTION)
                }
                else -> listOf(
                    BuiltinArrayConstructorKind.INIT_FUNCTION,
                    BuiltinArrayConstructorKind.REPEAT_ELEMENT,
                )
            }
            arrayCandidateKinds.map { kind ->
                candidateFactory.createBuiltinArrayConstructorCandidate(
                    callInfo = callInfo,
                    kind = kind,
                )
            }
        }
        val (reducedCandidates, applicability) = reduceCollectedCandidates(
            candidates = arrayCandidates,
            collectorApplicability = CandidateApplicability.HIDDEN,
            isCandidateSuccessful = Candidate::isSuccessful,
            candidateApplicability = Candidate::lowestApplicability,
            fullyProcessCandidate = { candidate ->
                components.resolutionStageRunner.fullyProcessCandidate(candidate, transformer.resolutionContext)
            },
            chooseMostSpecific = { currentCandidates ->
                currentCandidates.singleOrNull()?.let(::setOf)
                    ?: conflictResolver.chooseMaximallySpecificCandidates(currentCandidates)
            },
        )
        return ResolutionResult(
            info = callInfo,
            applicability = applicability,
            candidates = reducedCandidates,
            forwardedDiagnostics = emptyList(),
        )
    }

    private fun createClassifierCallInfo(
        functionCall: CfirFunctionCall,
        classifier: CfirClassLikeSymbol<*>,
        resolutionMode: ResolutionMode,
    ): CallInfo = CallInfo(
        callSite = functionCall,
        callKind = CallKind.Function,
        name = classifier.name,
        origin = functionCall.origin,
        explicitReceiver = functionCall.explicitReceiver,
        arguments = functionCall.argumentList.arguments,
        isUsedAsGetClassReceiver = false,
        typeArguments = functionCall.typeArguments,
        session = session,
        containingFile = components.file,
        containingDeclarations = transformer.components.containingDeclarations,
        resolutionMode = resolutionMode,
    )

    private fun findClassifierForCall(
        qualifiedAccess: CfirQualifiedAccessExpression,
        name: Name,
    ): CfirClassLikeSymbol<*>? {
        val explicitReceiver = qualifiedAccess.explicitReceiver
        return if (explicitReceiver != null) {
            findClassifierInQualifierScope(explicitReceiver, name)
        } else {
            towerResolver.findClassifiers(name).firstOrNull() ?: resolveTopLevelClassifierByShortName(name)
        }
    }

    private fun resolveTopLevelClassifierByShortName(name: Name): CfirClassLikeSymbol<*>? {
        val file = components.file
        val packageCandidates = LinkedHashSet<ClassId>()
        val explicitImportCandidates = LinkedHashSet<ClassId>()

        findSameFileTopLevelClassifier(file, name)?.let { declaration ->
            return declaration.symbol
        }

        for (importInfo in file.imports) {
            val importedFqName = importInfo.importedFqName ?: continue
            if (importInfo.isAllUnder) {
                explicitImportCandidates += ClassId(importedFqName, name)
                continue
            }

            val importedName = importInfo.aliasName?.asString() ?: importedFqName.shortName().asString()
            if (importedName == name.asString()) {
                explicitImportCandidates += ClassId.topLevel(importedFqName)
            }
        }

        packageCandidates += ClassId(file.packageDirective.packageFqName, name)

        val defaultImportCandidates = LinkedHashSet<ClassId>()
        val defaultImportsProvider = session.defaultImportsProvider
        val defaultImports = defaultImportsProvider.getDefaultImports(includeLowPriorityImports = true)
            .filter { it.fqName !in defaultImportsProvider.excludedImports }
        addDefaultImportCandidates(defaultImportCandidates, defaultImports, name)

        return sequenceOf(
            packageCandidates,
            explicitImportCandidates,
            defaultImportCandidates,
        ).flatMap { it.asSequence() }
            .firstNotNullOfOrNull(components.symbolProvider::getClassLikeSymbolByClassId)
    }

    private fun findSameFileTopLevelClassifier(
        file: org.cangnova.cangjie.cfir.declarations.CfirFile,
        shortName: Name,
    ): CfirClassLikeDeclaration? {
        return file.declarations
            .asSequence()
            .filterIsInstance<CfirClassLikeDeclaration>()
            .filter { declaration -> declaration.name == shortName }
            .firstOrNull()
    }

    private fun addDefaultImportCandidates(
        candidates: MutableSet<ClassId>,
        imports: List<ImportPath>,
        shortName: Name,
    ) {
        val simpleName = shortName.asString()
        for (importPath in imports) {
            if (importPath.isAllUnder) {
                candidates += ClassId(importPath.fqName, shortName)
                continue
            }

            val importedName = importPath.alias?.asString() ?: importPath.fqName.shortName().asString()
            if (importedName == simpleName) {
                candidates += ClassId.topLevel(importPath.fqName)
            }
        }
    }

    private fun findClassifierInQualifierScope(
        receiver: CfirExpression,
        name: Name,
    ): CfirClassLikeSymbol<*>? {
        val unwrappedReceiver = receiver.unwrapSmartcastExpression()
        val qualifierClassifier = unwrappedReceiver.resolvedQualifierClassifier(session) ?: return null
        val qualifierType = unwrappedReceiver.coneTypeOrNull ?: qualifierClassifier.constructType()
        val staticScope =
            qualifierClassifier.staticScopeForQualifierType(session, components.scopeSession, qualifierType)
        var result: CfirClassLikeSymbol<*>? = null
        staticScope.processClassifiersByName(name) { classifier ->
            if (result == null) {
                result = classifier
            }
        }
        return result
    }

    private fun createErrorReferenceForSingleCandidate(
        candidate: Candidate?,
        diagnostic: ConeDiagnostic,
        callInfo: CallInfo,
        source: org.cangnova.cangjie.source.CjSourceElement?,
    ): CfirNamedReference {
        if (candidate == null) {
            return createErrorReferenceWithErrorCandidate(
                callInfo = callInfo,
                diagnostic = diagnostic,
                source = source,
                resolutionContext = transformer.resolutionContext,
                resolutionStageRunner = components.resolutionStageRunner,
            )
        } else {
            return when (diagnostic) {
                is ConeUnresolvedError,
                is ConeHiddenCandidateError,
                -> createErrorReferenceWithErrorCandidate(
                    callInfo = callInfo,
                    diagnostic = diagnostic,
                    source = source,
                    resolutionContext = transformer.resolutionContext,
                    resolutionStageRunner = components.resolutionStageRunner,
                )

                else -> createErrorReferenceWithExistingCandidate(
                    candidate = candidate,
                    diagnostic = diagnostic,
                    source = source,
                    resolutionContext = transformer.resolutionContext,
                    resolutionStageRunner = components.resolutionStageRunner,
                )
            }
        }
    }

    private data class ResolutionResult(
        val info: CallInfo,
        val applicability: CandidateApplicability,
        val candidates: Collection<Candidate>,
        val forwardedDiagnostics: List<ResolutionDiagnostic>,
    )
}

/** A candidate in the overload candidate set. */
data class OverloadCandidate(val candidate: Candidate, val isInBestCandidates: Boolean)

@OptIn(ApplicabilityDetail::class)
internal fun <T> reduceCollectedCandidates(
    candidates: Collection<T>,
    collectorApplicability: CandidateApplicability,
    isCandidateSuccessful: (T) -> Boolean,
    candidateApplicability: (T) -> CandidateApplicability,
    fullyProcessCandidate: (T) -> Unit,
    chooseMostSpecific: (Set<T>) -> Set<T>,
): Pair<Set<T>, CandidateApplicability> {
    if (candidates.isEmpty()) {
        return emptySet<T>() to collectorApplicability
    }

    val candidateSet = candidates.toSet()
    if (collectorApplicability.isSuccess) {
        return chooseMostSpecific(candidateSet) to collectorApplicability
    }

    if (candidateSet.size == 1) {
        val candidate = candidateSet.single()
        fullyProcessCandidate(candidate)
        return setOf(candidate) to normalizeReductionApplicability(
            isSuccessful = isCandidateSuccessful(candidate),
            applicability = candidateApplicability(candidate),
        )
    }

    val groupedByApplicability = candidateSet.groupBy { candidate ->
        fullyProcessCandidate(candidate)
        normalizeReductionApplicability(
            isSuccessful = isCandidateSuccessful(candidate),
            applicability = candidateApplicability(candidate),
        )
    }

    val selectedGroup = groupedByApplicability.maxBy { it.key }
    return chooseMostSpecific(selectedGroup.value.toSet()) to selectedGroup.key
}

@OptIn(ApplicabilityDetail::class)
private fun normalizeReductionApplicability(
    isSuccessful: Boolean,
    applicability: CandidateApplicability,
): CandidateApplicability {
    if (isSuccessful || !applicability.isSuccess) {
        return applicability
    }

    return CandidateApplicability.RESOLVED_WITH_ERROR
}

class AllCandidatesCollector(
    components: BodyResolveComponents,
    resolutionStageRunner: ResolutionStageRunner
) : CfirAllCandidatesCollector(
    components as CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    resolutionStageRunner
) {
    private val allCandidatesMap = mutableMapOf<CfirBasedSymbol<*>, Candidate>()

    override fun consumeCandidate(
        group: CfirTowerGroup,
        candidate: Candidate,
        context: ResolutionContext
    ): CandidateApplicability {
        allCandidatesMap.getOrPut(candidate.symbol) { candidate }
        return super.consumeCandidate(group, candidate, context)
    }

    override fun shouldStopAtTheGroup(group: CfirTowerGroup): Boolean = false

    val allCandidates: Collection<Candidate>
        get() = allCandidatesMap.values
}
