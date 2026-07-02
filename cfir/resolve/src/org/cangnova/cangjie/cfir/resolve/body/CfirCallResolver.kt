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
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.calls.qualifierScopeOrNull
import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.buildProperty
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.diagnostic.*
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.expressions.builder.buildNamedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.*
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedCallableReferenceAtom
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.*
import org.cangnova.cangjie.cfir.resolve.calls.overloads.CfirOverloadByLambdaBodyResolver
import org.cangnova.cangjie.cfir.resolve.calls.overloads.ConeCallConflictResolver
import org.cangnova.cangjie.cfir.resolve.calls.overloads.callConflictResolverFactory
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirCreateFreshTypeVariableSubstitutorStage
import org.cangnova.cangjie.cfir.resolve.calls.stages.ResolutionStageRunner
import org.cangnova.cangjie.cfir.resolve.calls.stages.fullyProcessCandidate
import org.cangnova.cangjie.cfir.resolve.calls.tower.CfirTowerGroup
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirPCLAInferenceSession
import org.cangnova.cangjie.cfir.resolve.providers.findExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.withExpectedType
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.AbstractCandidate
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInArguments
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ReceiverConstraintPosition
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.fakeElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.safeSubstitute
import org.cangnova.cangjie.utils.runIf

/**
 * callable reference 实参解析结果。
 *
 * `POSTPONED` 表示首轮重载歧义仍可能在外层约束完成后收敛，调用方不能把外层候选判为失败。
 */
internal enum class CallableReferenceResolutionResult {
    RESOLVED,
    POSTPONED,
    FAILURE,
}

/**
 * CFIR body resolve 阶段的统一调用解析入口。
 *
 * 该 resolver 负责把函数调用、命名值访问、构造调用、内建 Array/VArray/Pointer/CString
 * 构造形式和 callable reference 候选都规整到同一套 tower resolve、candidate stage、
 * overload reduction 和错误引用构造流程中。
 */
class CfirCallResolver(
    /** Body resolve 阶段共享组件，提供 session、scope、候选阶段运行器和上下文状态。 */
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    /** 负责按 tower scope 收集候选的底层解析器，默认使用当前组件创建。 */
    private val towerResolver: CfirTowerResolver =
        CfirTowerResolver(components, components.resolutionStageRunner),
) : SessionHolder {

    /** 当前解析 session。 */
    override val session: CfirSession get() = components.session

    /** 表达式 transformer 由外部在构造后注入，用于递归解析 receiver、实参和局部表达式。 */
    private lateinit var transformer: CfirExpressionsResolveTransformer

    /** 初始化与当前调用解析器互相递归依赖的表达式 transformer。 */
    fun initTransformer(transformer: CfirExpressionsResolveTransformer) {
        this.transformer = transformer
    }

    /** 负责候选冲突规约和最具体候选选择的语义组件。 */
    val conflictResolver: ConeCallConflictResolver =
        session.callConflictResolverFactory.create(session.inferenceComponents, components)

    /** 仅在需要根据 lambda body 进一步规约重载时延迟创建。 */
    private val overloadByLambdaBodyResolver: CfirOverloadByLambdaBodyResolver by lazy(LazyThreadSafetyMode.NONE) {
        CfirOverloadByLambdaBodyResolver(components, conflictResolver)
    }

    /** 将 [ResolutionResult] 的适用性规约为候选收集是否成功。 */
    @ApplicabilityDetail
    private val ResolutionResult.isSuccess: Boolean
        get() = applicability.isSuccess

    /**
     * 解析函数调用并把最终候选或错误诊断写回 callee reference。
     *
     * 该入口覆盖普通函数、enum constructor、class constructor、同文件 classifier fallback、
     * 内建 Array/VArray/Pointer/CString 构造以及 collection literal 外层候选语境。
     */
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
        val sameFileClassifierForCall = if (!isCollectionLiteralCall && functionCall.explicitReceiver == null) {
            findSameFileTopLevelClassifier(components.file, callee.name)?.symbol
        } else {
            null
        }
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
        if (!isCollectionLiteralCall) {
            if (sameFileClassifierForCall != null) {
                effectiveResult = collectClassConstructorCandidates(
                    functionCall = functionCall,
                    classifier = sameFileClassifierForCall,
                    resolutionMode = resolutionMode,
                )
                classLikeCallResolved = true
            } else {
                val directVArrayTarget = functionCall.directVArrayConstructorTargetOrNull(callee.name)
                if (directVArrayTarget != null) {
                    effectiveResult = collectBuiltinArrayConstructorCandidates(
                        functionCall = functionCall,
                        name = callee.name,
                        target = directVArrayTarget,
                        resolutionMode = resolutionMode,
                    )
                    classLikeCallResolved = true
                } else if (callee.name.asString() == "CPointer" && result.candidates.isEmpty()) {
                    effectiveResult = collectBuiltinPointerConstructorCandidates(
                        functionCall = functionCall,
                        name = callee.name,
                        target = BuiltinPointerConstructorTarget(),
                        resolutionMode = resolutionMode,
                    )
                    classLikeCallResolved = true
                } else if (callee.name.asString() == "CString" && result.candidates.isEmpty()) {
                    effectiveResult = collectBuiltinCStringConstructorCandidates(
                        functionCall = functionCall,
                        name = callee.name,
                        resolutionMode = resolutionMode,
                    )
                    classLikeCallResolved = true
                } else if (callee.name.asString() == "Array") {
                    val classifier = findClassifierForCall(functionCall, callee.name)
                    if (classifier != null && classifier.isStdlibArrayClassifier()) {
                        effectiveResult = collectBuiltinArrayConstructorCandidates(
                            functionCall = functionCall,
                            name = classifier.name,
                            target = BuiltinArrayConstructorTarget.Array,
                            resolutionMode = resolutionMode,
                        )
                        classLikeCallResolved = true
                    }
                }
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
                // 阶段2b：枚举构造器也未找到，先按无参值访问解析 callee，再决定是否作为函数值调用。
                val valueAccess = buildCalleeValueAccess(functionCall, callee)
                val variableAccessResult = collectCandidates(
                    valueAccess,
                    callee.name,
                    CallKind.NamedValueAccess,
                    origin = functionCall.origin,
                    callSite = functionCall,
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
                } else {
                    val callableValueInvokeResult = collectCallableValueInvokeCandidates(
                        functionCall = functionCall,
                        name = callee.name,
                        valueAccessResult = variableAccessResult,
                    )
                    if (callableValueInvokeResult != null) {
                        effectiveResult = callableValueInvokeResult
                    } else if (variableAccessResult.candidates.isNotEmpty()) {
                        expectedCallKind = CallKind.NamedValueAccess
                        expectedCandidates = variableAccessResult.candidates
                    }
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

    /**
     * 解析 `this(...)` / `super(...)` 委托构造调用。
     *
     * 目标 class-like 声明先 lazy resolve 到 STATUS，然后只从目标声明的构造器集合创建候选；
     * 构造器候选的可见性与模态依赖 STATUS 阶段补全，不能停在 TYPES。
     * 最终候选或构造器诊断会写回调用 callee。
     */
    fun resolveDelegatingConstructorCallAndSelectCandidate(
        functionCall: CfirFunctionCall,
        targetDeclaration: CfirClassLikeDeclaration,
        resolutionMode: ResolutionMode,
    ): CfirFunctionCall {
        val callee = functionCall.calleeReference as? CfirNamedReference ?: return functionCall
        targetDeclaration.symbol.lazyResolveToPhase(CfirResolvePhase.STATUS)
        val actualTarget = targetDeclaration.symbol.cfir
        val callInfo = createDelegatingConstructorCallInfo(functionCall, actualTarget, resolutionMode)
        val constructorSymbols = actualTarget.declarations
            .filterIsInstance<CfirConstructor>()
            .map(CfirConstructor::symbol)
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

        val nameReference = createResolvedNamedReference(
            callee,
            actualTarget.name,
            callInfo,
            reducedCandidates,
            applicability,
            explicitReceiver = null,
            createResolvedReferenceWithoutCandidateForLocalVariables = false,
        )

        functionCall.replaceCalleeReference(nameReference)
        val candidate = (nameReference as? CfirNamedReferenceWithCandidate)?.candidate
        reportBodyResolutionErrorToOverloadByLambdaCandidate(nameReference, candidate)
        candidate?.updateSourcesOfReceivers()
        return functionCall
    }

    /** 判断 classifier 或其 typealias 展开结果是否为标准库 `Array`。 */
    private fun CfirClassLikeSymbol<*>?.isStdlibArrayClassifier(): Boolean {
        val actualClassifier = (this as? CfirTypeAliasSymbol)?.fullyExpandedClass(session) ?: this
        return actualClassifier?.classId == StdlibClassIds.Array
    }

    /**
     * 解析命名值访问并选择候选。
     *
     * 该入口用于变量、属性、enum value、classifier 作为 receiver、package qualifier
     * 以及函数名作为值的访问场景；返回值可能是原访问节点或带诊断的访问表达式。
     */
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
    /** 兼容旧命名的变量访问解析入口，实际委托到 [resolveNamedValueAccessAndSelectCandidate]。 */
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

    /**
     * 命名值访问解析的实际实现。
     *
     * 这里统一处理显式 receiver、package qualifier、VArray.size、classifier/type parameter
     * fallback、enum value fallback、候选过滤回调、错误引用构造和结果类型写回。
     */
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
        tryResolveVArraySizeAccess(transformedAccess, callee)?.let { return it }

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
                } else if (importedPackageQualifier.isUnresolved) {
                    val diagnostic = ConeUnresolvedNameError(callee.name)
                    val unreportedDiagnostic = ConeUnreportedDuplicateDiagnostic(diagnostic)
                    transformedAccess.replaceCalleeReference(
                        buildErrorNamedReference {
                            source = callee.source?.fakeElement(CjFakeSourceElementKind.UnresolvedImportQualifier)
                            name = callee.name
                            this.diagnostic = unreportedDiagnostic
                        }
                    )
                    transformedAccess.replaceConeTypeOrNull(ConeErrorType(unreportedDiagnostic))
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
            if (isUsedAsReceiver && !basicResult.isSuccess) {
                val typeParameter = towerResolver.findTypeParameters(callee.name).firstOrNull()
                if (typeParameter != null) {
                    transformedAccess.replaceCalleeReference(
                        buildResolvedNamedReference {
                            source = callee.source
                            name = callee.name
                            resolvedSymbol = typeParameter
                        }
                    )
                    transformedAccess.replaceConeTypeOrNull(typeParameter.constructType())
                    return transformedAccess
                }
            }

            if (!result.isSuccess || (isUsedAsReceiver && result.candidates.all { it.symbol is CfirClassLikeSymbol<*> })) {
                val classifier = towerResolver.findClassifiers(callee.name)
                    .firstOrNull { it.isValidClassifierExpression(isUsedAsReceiver) }
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

            if (isUsedAsReceiver && !result.isSuccess) {
                tryResolveSpecialBuiltinTypeQualifier(transformedAccess, callee)?.let { return it }
            }
        }

        val shouldTryEnumValueAccess =
            transformedAccess !is CfirFunctionCall &&
                    (result.candidates.isEmpty() || result.candidates.all { it.symbol is CfirEnumConstructorSymbol })

        var functionCallExpected = false
        if (shouldTryEnumValueAccess) {
            // 先尝试枚举构造器值表达式；它后续仍可作为 member access receiver。
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

    /**
     * 将没有 class-like symbol 的官方内建类型名解析为 qualifier carrier。
     *
     * `CString` 与 `CPointer<T>` 的 extend 成员由 built-in type key 查询；
     * 若同名普通值、类型参数或 class-like 声明已解析成功，则调用方不会进入这里。
     */
    private fun tryResolveSpecialBuiltinTypeQualifier(
        access: CfirQualifiedAccessExpression,
        callee: CfirNamedReference,
    ): CfirQualifiedAccessExpression? {
        if (access.explicitReceiver != null) return null
        val builtinType = when (callee.name) {
            StandardNames.CSTRING -> {
                if (access.typeArguments.isNotEmpty()) return null
                ConeCStringType()
            }
            StandardNames.CPOINTER -> {
                val pointeeTypeRef = access.typeArguments.singleOrNull() as? CfirResolvedTypeRef ?: return null
                ConePointerType(pointeeTypeRef.coneType)
            }
            else -> return null
        }
        access.replaceConeTypeOrNull(builtinType)
        return access
    }

    /**
     * 将 body resolve 阶段的调用错误同步给 overload-by-lambda 上下文。
     *
     * 当 callee 已经是错误引用或选中候选未成功时，lambda body 规约需要知道当前候选含错误参数，
     * 以避免后续把错误候选当成可继续推断的正常候选。
     */
    private fun reportBodyResolutionErrorToOverloadByLambdaCandidate(
        reference: CfirReference,
        selectedCandidate: Candidate?,
    ) {
        if (reference is CfirErrorNamedReference || selectedCandidate?.isSuccessful == false) {
            components.context.reportOverloadByLambdaCandidateDiagnostic(ErrorTypeInArguments)
        }
    }

    /**
     * 收集一个访问表达式的全部候选，并标记哪些候选属于当前最佳候选集合。
     *
     * 该入口用于 IDE/分析 API 等需要展示完整 overload set 的场景；普通值访问无结果时，
     * 会额外尝试函数调用 kind 以覆盖函数名作为值的候选。
     */
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

    /**
     * 在外层调用候选的约束系统中解析 postponed callable reference 参数。
     *
     * 每个 callable reference atom 可能有多种候选选择；这里逐 atom 扩展约束系统快照，
     * 只有最终唯一组合成功时才把约束系统和表达式 callee reference 正式回写。
     */
    internal fun resolveCallableReferenceArguments(
        containingCallCandidate: Candidate,
        atoms: List<ConeResolvedCallableReferenceAtom>,
    ): CallableReferenceResolutionResult {
        /** 一条 callable reference 部分解析路径的约束系统快照与已选候选。 */
        data class PartialResolution(
            val storage: ConstraintStorage,
            val choices: List<CallableReferenceChoice>,
        )

        var partials = listOf(
            PartialResolution(
                storage = containingCallCandidate.system.currentStorage(),
                choices = emptyList(),
            )
        )

        for (atom in atoms) {
            /** 当前 atom 展开后形成的下一批部分解析路径。 */
            val nextPartials = mutableListOf<PartialResolution>()
            for (partial in partials) {
                val choices = callableReferenceChoices(containingCallCandidate, atom, partial.storage)
                for (choice in choices) {
                    nextPartials += PartialResolution(
                        storage = choice.candidate.system.currentStorage(),
                        choices = partial.choices + choice,
                    )
                }
            }
            if (nextPartials.isEmpty()) {
                atoms.forEach { atom -> atom.markResolved() }
                return CallableReferenceResolutionResult.FAILURE
            }
            partials = nextPartials
        }

        if (partials.size != 1) {
            if (atoms.any { atom -> atom.isPostponedBecauseOfAmbiguity }) {
                atoms.forEach { atom -> atom.markResolved() }
                return CallableReferenceResolutionResult.FAILURE
            }

            atoms.forEach { atom -> atom.postponeBecauseOfAmbiguity() }
            return CallableReferenceResolutionResult.POSTPONED
        }

        val resolved = partials.single()
        containingCallCandidate.system.replaceContentWith(resolved.storage)
        for (choice in resolved.choices) {
            choice.candidate.system.replaceContentWith(resolved.storage)
            choice.apply()
        }
        return CallableReferenceResolutionResult.RESOLVED
    }

    /**
     * 单个 callable reference atom 的候选选择结果。
     *
     * 该结构保存临时候选、表达式节点和结果函数类型，只有整组 callable reference 唯一成功后才会 apply。
     */
    private data class CallableReferenceChoice(
        /** 被解析的 callable reference atom。 */
        val atom: ConeResolvedCallableReferenceAtom,
        /** 对应的命名访问表达式。 */
        val expression: CfirNamedAccessExpression,
        /** 当前选择的候选。 */
        val candidate: Candidate,
        /** callable reference 解析后的函数类型。 */
        val resultingType: ConeCangJieType,
    ) {
        /** 将 callable reference 候选正式写回表达式和 atom 状态。 */
        fun apply() {
            val reference = expression.calleeReference as? CfirNamedReference ?: return
            candidate.updateSourcesOfReceivers()
            expression.replaceCalleeReference(
                CfirNamedReferenceWithCandidate(
                    reference.source,
                    reference.name,
                    candidate,
                )
            )
            expression.replaceConeTypeOrNull(resultingType)
            atom.resultingTypeForCallableReference = resultingType
            atom.markResolved()
        }
    }

    /**
     * 为一个 callable reference atom 枚举在当前约束系统快照下可成功的候选选择。
     *
     * expected type 会先按外层候选约束系统替换，再用 candidate factory 复制原候选并完整跑 stages。
     */
    private fun callableReferenceChoices(
        containingCallCandidate: Candidate,
        atom: ConeResolvedCallableReferenceAtom,
        baseSystem: ConstraintStorage,
    ): List<CallableReferenceChoice> {
        val expression = atom.expression as? CfirNamedAccessExpression ?: return emptyList()
        val expectedType = atom.expectedTypeForCallableReference(baseSystem) ?: return emptyList()
        val originalCandidates = expression.callableReferenceCandidates()
        if (originalCandidates.isEmpty()) return emptyList()

        val callInfo = expression.callableReferenceCallInfo(expectedType)
        val candidateFactory = CandidateFactory(transformer.resolutionContext, baseSystem)
        val choices = originalCandidates.mapNotNull { originalCandidate ->
            val candidate = candidateFactory.createCallableReferenceCandidate(callInfo, originalCandidate)
            components.resolutionStageRunner.fullyProcessCandidate(candidate, transformer.resolutionContext)
            val resultingType = candidate.resultingTypeForCallableReference ?: return@mapNotNull null
            if (!candidate.isSuccessful || candidate.system.hasContradiction) {
                return@mapNotNull null
            }
            CallableReferenceChoice(atom, expression, candidate, resultingType)
        }
        if (choices.size <= 1) return choices

        val mostSpecificCandidates = conflictResolver.chooseMaximallySpecificCandidates(choices.map { it.candidate })
        return choices.filter { choice -> choice.candidate in mostSpecificCandidates }
    }

    /**
     * 计算 callable reference 的当前 expected type。
     *
     * revised expected type 优先于原始 expected type，并会通过外层约束系统当前 substitutor 替换。
     */
    private fun ConeResolvedCallableReferenceAtom.expectedTypeForCallableReference(
        baseSystem: ConstraintStorage,
    ): ConeCangJieType? {
        val rawExpectedType = revisedExpectedType?.asCone() ?: expectedType ?: return null
        val substitutor = baseSystem
            .buildCurrentSubstitutor(session.typeContext, emptyMap())
            .asCone()
        return substitutor.substituteOrSelf(rawExpectedType)
    }

    /**
     * 从 callable reference 表达式已有 callee reference 中提取候选。
     *
     * 已选候选直接复用；歧义错误只保留函数声明候选，避免把非 callable 值混入 callable reference 解析。
     */
    private fun CfirNamedAccessExpression.callableReferenceCandidates(): List<Candidate> {
        return when (val reference = calleeReference) {
            is CfirNamedReferenceWithCandidate -> listOf(reference.candidate)
            is CfirErrorNamedReference -> {
                val ambiguity = reference.diagnostic as? ConeAmbiguityError ?: return emptyList()
                ambiguity.candidates.filterIsInstance<Candidate>().filter { candidate ->
                    candidate.symbol.takeIf { it.isBound }?.cfir is CfirFunction
                }
            }
            else -> emptyList()
        }
    }

    /**
     * 为 callable reference 候选重跑构造 call info。
     *
     * callable reference 在这里按 named value access 处理，实参为空，expected type 作为解析模式传入。
     */
    private fun CfirNamedAccessExpression.callableReferenceCallInfo(
        expectedType: ConeCangJieType,
    ): CallInfo {
        val reference = calleeReference as? CfirNamedReference
        return CallInfo(
            callSite = this,
            callKind = CallKind.NamedValueAccess,
            name = reference?.name ?: Name.special("<callable-reference>"),
            explicitReceiver = explicitReceiver,
            arguments = emptyList(),
            isUsedAsGetClassReceiver = false,
            typeArguments = typeArguments,
            session = session,
            containingFile = components.file,
            containingDeclarations = components.containingDeclarations,
            resolutionMode = withExpectedType(expectedType),
        )
    }

    /**
     * 从 CFIR 访问表达式构造 [CallInfo] 并收集候选。
     *
     * call kind 可由调用节点、强制参数或 collection literal 外层上下文决定；
     * 该函数只负责构造解析输入，实际 tower resolve 与候选规约委托给另一个 overload。
     */
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

    /**
     * 运行 tower resolver 并规约候选集合。
     *
     * 规约顺序为 tower 收集、适用性分组、函数值 expected type 过滤、
     * overload-by-lambda 过滤，最终返回候选集合、适用性与转发诊断。
     */
    private fun collectCandidates(
        info: CallInfo,
        resolutionContext: ResolutionContext,
        collector: CfirCandidateCollector? = null,
    ): ResolutionResult {
        val resultCollector = towerResolver.runResolver(info, resolutionContext, collector)
        var (reducedCandidates, applicability) = reduceCandidates(resultCollector, info = info)
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
     * 将函数调用 callee 单独解析成值访问。
     *
     * `f(x)` 的 fallback 不能把 `x` 作为 `NamedValueAccess` 的实参收集，否则函数值变量
     * 会先得到 `TooManyArguments`，无法再进入函数值调用推断。
     */
    private fun buildCalleeValueAccess(
        functionCall: CfirFunctionCall,
        callee: CfirNamedReference,
    ): CfirQualifiedAccessExpression =
        buildNamedAccessExpression {
            source = functionCall.source
            calleeReference = buildNamedReference {
                source = callee.source
                name = callee.name
            }
            explicitReceiver = functionCall.explicitReceiver
            typeArguments.addAll(functionCall.typeArguments)
        }

    /**
     * 从已解析的 callee 值候选创建函数值调用候选。
     *
     * 这对齐 Kotlin FIR 的 common invoke receiver 结构：callee 值候选作为
     * [CallInfo.candidateForCommonInvokeReceiver] 保留，原始调用实参参与函数值候选的
     * 参数映射、约束完成和 completion 写回。
     */
    private fun collectCallableValueInvokeCandidates(
        functionCall: CfirFunctionCall,
        name: Name,
        valueAccessResult: ResolutionResult,
    ): ResolutionResult? {
        val callableValueCandidates = valueAccessResult.candidates
            .filter { candidate -> candidate.isCallableValueCandidate() }
        if (callableValueCandidates.isEmpty()) return null

        val invokeInfo = valueAccessResult.info.copy(
            callKind = CallKind.Function,
            arguments = functionCall.argumentList.arguments,
            typeArguments = functionCall.typeArguments,
            name = name,
            implicitInvokeMode = ImplicitInvokeMode.Regular,
        )
        val candidateFactory = CandidateFactory(transformer.resolutionContext, invokeInfo)
        val invokeCandidates = callableValueCandidates.map { valueCandidate ->
            candidateFactory.createCallableValueInvokeCandidate(
                callInfo = invokeInfo.copy(candidateForCommonInvokeReceiver = valueCandidate),
                callableValueCandidate = valueCandidate,
            )
        }
        val (reducedCandidates, applicability) = reduceCandidateSet(
            candidates = invokeCandidates,
            info = invokeInfo,
            collectorApplicability = CandidateApplicability.RESOLVED,
        )

        return ResolutionResult(
            info = invokeInfo,
            applicability = applicability,
            candidates = reducedCandidates,
            forwardedDiagnostics = valueAccessResult.forwardedDiagnostics,
        )
    }

    /**
     * 判断值访问候选是否可作为函数值调用。
     */
    private fun Candidate.isCallableValueCandidate(): Boolean {
        val variable = symbol.takeIf { it.isBound }?.cfir as? CfirVariable ?: return false
        if (variable.localLambdaInitializerInferenceDataOrNull() != null) return true
        val rawType = variable.returnTypeRef.coneTypeOrNull ?: return false
        if (isFreshLambdaValueParameterCallableCandidate(variable, rawType)) return true
        val type = rawType.fullyExpandedType(session)
        return when (type) {
            is ConeFunctionType -> true
            is ConeErrorType -> type.delegatedType?.fullyExpandedType(session) is ConeFunctionType
            else -> false
        }
    }

    /**
     * PCLA 中无上下文 lambda 形参可通过调用语法反推为函数值。
     *
     * 这里只承认当前候选约束系统中的 fresh 变量；声明类型参数或普通未解析类型不能
     * 通过该入口伪装成函数类型，仍交给常规调用诊断处理。
     */
    private fun Candidate.isFreshLambdaValueParameterCallableCandidate(
        variable: CfirVariable,
        type: ConeCangJieType,
    ): Boolean {
        if (components.context.inferenceSession !is CfirPCLAInferenceSession) return false
        if (variable !is CfirValueParameter) return false
        val variableType = type as? ConeTypeVariableType ?: return false
        if (variableType.typeConstructor.originalTypeParameter != null) return false
        return variableType.typeConstructor in system.currentStorage().allTypeVariables
    }

    /**
     * 仓颉尾随 lambda 只参与最后一个函数类型形参的候选。
     *
     * 官方 `SyntaxFilterCandidates` 对 trailing closure 会直接检查最后一个
     * parameter type 是否为函数类型；否则该候选属于语法层不匹配，不能进入
     * lambda body 重载规约，否则 lambda 会失去目标函数类型并退化成参数标注错误。
     */
    private fun reduceTrailingLambdaCandidatesByParameterType(
        info: CallInfo,
        candidates: Set<Candidate>,
    ): Set<Candidate> {
        if (candidates.size <= 1 || !info.hasTrailingLambdaArgument()) return candidates

        val matchingCandidates = candidates.filterTo(linkedSetOf()) { candidate ->
            val lastParameterType = candidate.declaredParametersForMapping()
                .lastOrNull()
                ?.returnTypeRef
                ?.coneTypeOrNull
                ?.let(candidate.substitutor::substituteOrSelf)
                ?.fullyExpandedType(session)

            lastParameterType is ConeFunctionType
        }

        return matchingCandidates.ifEmpty { candidates }
    }

    /** 判断调用信息中是否包含尾随 lambda 实参。 */
    private fun CallInfo.hasTrailingLambdaArgument(): Boolean {
        return (callSite as? CfirFunctionCall)?.hasTrailingLambda == true ||
            arguments.any { argument ->
                (argument as? CfirAnonymousFunctionExpression)?.isTrailingLambda == true
            }
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

    /**
     * 对 tower collector 的最佳候选进行完整 stage 处理和最具体候选规约。
     *
     * 函数名作为值且没有 expected type 时会保留函数 overload set；其他路径会按适用性、
     * 尾随 lambda、expected return type 和 fresh receiver 规则逐步收窄。
     */
    private fun reduceCandidates(
        collector: CfirCandidateCollector,
        info: CallInfo,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
    ): Pair<Set<Candidate>, CandidateApplicability> {
        val functionValueCandidates = collector.functionValueCandidates()
        val preserveFunctionValueOverloadSet =
            info.callKind == CallKind.NamedValueAccess &&
                    info.resolutionMode.expectedType == null &&
                    functionValueCandidates.size > 1

        return reduceCandidateSet(
            candidates = if (preserveFunctionValueOverloadSet) functionValueCandidates else collector.bestCandidates(),
            collectorApplicability = if (preserveFunctionValueOverloadSet) {
                CandidateApplicability.RESOLVED
            } else {
                collector.currentApplicability
            },
            info = info,
            preserveFunctionValueOverloadSet = preserveFunctionValueOverloadSet,
            resolutionContext = resolutionContext,
        )
    }

    /**
     * 对已经构造好的候选集合执行统一 stage 处理和最具体规约。
     */
    private fun reduceCandidateSet(
        candidates: Collection<Candidate>,
        info: CallInfo,
        collectorApplicability: CandidateApplicability,
        preserveFunctionValueOverloadSet: Boolean = false,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
    ): Pair<Set<Candidate>, CandidateApplicability> =
        reduceCollectedCandidates(
            candidates = candidates,
            collectorApplicability = collectorApplicability,
            isCandidateSuccessful = Candidate::isSuccessful,
            candidateApplicability = Candidate::lowestApplicability,
            fullyProcessCandidate = { candidate ->
                components.resolutionStageRunner.fullyProcessCandidate(candidate, resolutionContext)
            },
            chooseMostSpecific = { candidates ->
                if (preserveFunctionValueOverloadSet) {
                    return@reduceCollectedCandidates candidates
                }
                val syntaxFilteredCandidates = reduceTrailingLambdaCandidatesByParameterType(info, candidates)
                val expectedTypeFilteredCandidates = reduceCandidatesByExpectedReturnType(info, syntaxFilteredCandidates)
                reduceFreshTypeVariableReceiverCandidates(expectedTypeFilteredCandidates)?.let {
                    return@reduceCollectedCandidates it
                }
                expectedTypeFilteredCandidates.singleOrNull()?.let(::setOf)
                    ?: conflictResolver.chooseMaximallySpecificCandidates(expectedTypeFilteredCandidates)
            },
        )

    /**
     * 仓颉 check-mode 会让表达式目标类型参与重载选择。这里在候选已经完成参数适用性检查后，
     * 用候选返回类型过滤 expected type 不可能满足的重载，避免后续 completion 才发现返回类型不匹配。
     */
    private fun reduceCandidatesByExpectedReturnType(
        info: CallInfo,
        candidates: Set<Candidate>,
    ): Set<Candidate> {
        if (candidates.size <= 1) return candidates
        val expectedType = info.resolutionMode.expectedType?.fullyExpandedType() ?: return candidates
        val matchingCandidates = candidates.filterTo(linkedSetOf()) { candidate ->
            val candidateReturnType = components.initialTypeOfCandidate(candidate).fullyExpandedType()
            AbstractTypeChecker.isSubtypeOf(session.typeContext, candidateReturnType, expectedType)
        }
        return matchingCandidates.ifEmpty { candidates }
    }

    /**
     * fresh lambda receiver 的成员访问对应官方 `TryInitializeBaseSum` / `FilterSumUpperbound`。
     *
     * 当 receiver 仍是 lambda 参数 placeholder 时，多个 owner 声明只是在为同一个
     * callable shape 提供接收者类型约束；若参数检查后所有候选的可调用签名完全等价，
     * 该集合不应退化为普通重载歧义。签名不等价时继续交给普通冲突解析，避免吞掉真实歧义。
     */
    private fun reduceFreshTypeVariableReceiverCandidates(candidates: Set<Candidate>): Set<Candidate>? {
        if (candidates.size <= 1) return null

        val receiverTypeConstructor = candidates.first().freshTypeVariableReceiverConstructor() ?: return null
        if (candidates.any { it.freshTypeVariableReceiverConstructor() != receiverTypeConstructor }) {
            return null
        }
        if (candidates.any { !it.isSuccessful }) return null

        val receiverExpression = candidates.first().freshTypeVariableReceiverExpression() ?: return null
        val knownConstraintFilteredCandidates =
            reduceFreshReceiverCandidatesByKnownConstraints(candidates, receiverTypeConstructor, receiverExpression)
                ?: candidates
        val ownerFilteredCandidates =
            reduceFreshReceiverCandidatesByOwnerSum(knownConstraintFilteredCandidates, receiverTypeConstructor)
                ?: knownConstraintFilteredCandidates

        val shapes = ownerFilteredCandidates.map { candidate ->
            candidate.freshReceiverCallableShape() ?: return null
        }
        val firstShape = shapes.first()
        if (shapes.drop(1).all { it.isEquivalentTo(firstShape) }) {
            if (ownerFilteredCandidates.size > 1) {
                firstShape.candidate.freshReceiverConstraintToDrop = FreshReceiverConstraintToDrop(
                    receiverTypeConstructor = receiverTypeConstructor,
                    receiverExpression = receiverExpression,
                )
            }
            return setOf(firstShape.candidate)
        }

        return null
    }

    /**
     * 使用同一 fresh receiver 已有的非当前 receiver 约束过滤 owner 候选。
     *
     * 例如 `getB19(x); x.foo19(y)` 中 `getB19(x)` 已产生 `x <: B19`，
     * 因此 `foo19` 的 owner 只能保留 `B19` 可作为子类型的候选 owner。
     */
    private fun reduceFreshReceiverCandidatesByKnownConstraints(
        candidates: Set<Candidate>,
        receiverTypeConstructor: TypeConstructorMarker,
        receiverExpression: CfirExpression,
    ): Set<Candidate>? {
        val filtered = candidates.filterTo(linkedSetOf()) { candidate ->
            val ownerType = candidate.freshReceiverExpectedOwnerType() ?: return@filterTo false
            candidate.isFreshReceiverOwnerCompatibleWithKnownConstraints(
                receiverTypeConstructor,
                receiverExpression,
                ownerType,
            )
        }
        return filtered.takeIf { it.isNotEmpty() && it.size < candidates.size }
    }

    /**
     * 使用 PCLA 会话维护的 owner 候选集合过滤 fresh receiver 候选。
     *
     * 该集合对应官方 `SynLamExpr` 的成员语法候选交集；集合尚未收窄到单个 owner 时，
     * resolver 仍可选择等价签名代表，但稍后会丢弃该代表候选的 receiver 约束。
     */
    private fun reduceFreshReceiverCandidatesByOwnerSum(
        candidates: Set<Candidate>,
        receiverTypeConstructor: TypeConstructorMarker,
    ): Set<Candidate>? {
        val ownerTypesByCandidate = candidates.mapNotNull { candidate ->
            val ownerType = candidate.freshReceiverExpectedOwnerType() ?: return@mapNotNull null
            candidate to ownerType
        }
        if (ownerTypesByCandidate.size != candidates.size) return null

        val refinedOwnerTypes = components.context.inferenceSession.refineFreshReceiverCandidateOwners(
            receiverTypeConstructor,
            ownerTypesByCandidate.map { it.second },
        ) ?: return null
        val filtered = ownerTypesByCandidate.mapNotNullTo(linkedSetOf()) { (candidate, ownerType) ->
            candidate.takeIf {
                refinedOwnerTypes.any { refinedOwnerType -> ownerType.isSameTypeAs(refinedOwnerType) }
            }
        }
        return filtered.takeIf { it.isNotEmpty() && it.size < candidates.size }
    }

    /**
     * fresh receiver 候选的可调用形状。
     *
     * 当多个候选只是在为同一个 fresh receiver 类型变量提供约束时，
     * 用值参数类型和返回类型判断这些候选是否语义等价。
     */
    private data class FreshReceiverCallableShape(
        /** 代表该形状的原始候选。 */
        val candidate: Candidate,
        /** 经过当前约束系统归一化后的值参数类型。 */
        val valueParameterTypes: List<ConeCangJieType>,
        /** 经过当前约束系统归一化后的返回类型。 */
        val returnType: ConeCangJieType,
    )

    /** 判断两个 fresh receiver callable shape 是否有完全相同的参数和返回类型。 */
    private fun FreshReceiverCallableShape.isEquivalentTo(other: FreshReceiverCallableShape): Boolean {
        if (valueParameterTypes.size != other.valueParameterTypes.size) return false
        if (!returnType.isSameTypeAs(other.returnType)) return false
        return valueParameterTypes.zip(other.valueParameterTypes).all { (left, right) ->
            left.isSameTypeAs(right)
        }
    }

    /** 使用当前 session type context 判断两个 Cone 类型是否相等。 */
    private fun ConeCangJieType.isSameTypeAs(other: ConeCangJieType): Boolean =
        AbstractTypeChecker.equalTypes(session.typeContext, this, other)

    /**
     * 从候选中抽取 fresh receiver 规约需要比较的 callable shape。
     *
     * 参数类型来自已完成的 argument mapping，返回类型来自 return type calculator，
     * 两者都会按候选 substitutor 和约束系统当前状态归一化。
     */
    private fun Candidate.freshReceiverCallableShape(): FreshReceiverCallableShape? {
        if (!symbol.isBound) return null
        val declaration = symbol.cfir as? CfirCallableDeclaration ?: return null

        val valueParameterTypes: List<ConeCangJieType> = when {
            argumentMappingInitialized -> buildList<ConeCangJieType> {
                for (parameter in argumentMapping.values) {
                    val parameterType = parameter.returnTypeRef.coneTypeOrNull ?: return null
                    add(normalizeFreshReceiverShapeType(parameterType))
                }
            }
            declaration is CfirProperty -> emptyList()
            declaration is CfirFunction && declaration.valueParameters.isEmpty() -> emptyList()
            else -> return null
        }
        val returnType = components.returnTypeCalculator
            .tryCalculateReturnType(declaration)
            .coneType
            .let { returnType -> normalizeFreshReceiverShapeType(returnType) }

        return FreshReceiverCallableShape(this, valueParameterTypes, returnType)
    }

    /** 返回 fresh receiver 候选的声明 owner 类型。 */
    private fun Candidate.freshReceiverExpectedOwnerType(): ConeCangJieType? {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return null
        val receiverExpression = freshTypeVariableReceiverExpression() ?: return null
        val receiverType = when (receiverExpression) {
            dispatchReceiverExpression() -> callableSymbol.dispatchReceiverType
            givenExtensionReceiver?.expression -> expectedExtensionReceiverType()
            else -> null
        } ?: return null
        return normalizeFreshReceiverShapeType(receiverType)
    }

    /** 计算 extension 候选在当前 use-site 下的 receiver 类型。 */
    private fun Candidate.expectedExtensionReceiverType(): ConeCangJieType? {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return null
        val originalSymbol = callableSymbol.unwrapSubstitutionOverrides()
        val extendProvider = session.extendProvider
        val ownerExtend = extendProvider.getContainingExtend(originalSymbol)
            ?.takeIf(extendProvider::isExtendAccessible)
            ?: return null
        val actualReceiverType = givenExtensionReceiver?.expression?.coneTypeOrNull
        if (actualReceiverType != null) {
            findExtendDeclarationSubstitution(session, ownerExtend, actualReceiverType)
                ?.substitutedReceiverType
                ?.let { return it }
        }
        return ownerExtend.extendedTypeRef.coneTypeOrNull
    }

    /**
     * 将候选签名类型归一化到 fresh receiver 规约可比较的代表类型。
     *
     * 先应用候选 substitutor，再应用约束系统当前 substitutor，最后把未固定类型变量替换为代表约束类型。
     */
    private fun Candidate.normalizeFreshReceiverShapeType(type: ConeCangJieType): ConeCangJieType {
        val substituted = substitutor.substituteOrSelf(type)
        val currentSubstitutor = system.buildCurrentSubstitutor()
        val currentType = currentSubstitutor.safeSubstitute(system, substituted).asCone()
        return normalizeFreshReceiverTypeArguments(representativeConstraintType(currentType))
    }

    /** 递归归一化 owner/callable shape 中嵌套的 fresh 类型变量。 */
    private fun Candidate.normalizeFreshReceiverTypeArguments(type: ConeCangJieType): ConeCangJieType {
        val representative = representativeConstraintType(type)
        if (representative !== type) {
            return normalizeFreshReceiverTypeArguments(representative)
        }

        fun List<ConeTypeProjection>.normalizeProjections(): List<ConeTypeProjection> =
            map { projection -> normalizeFreshReceiverTypeArguments(projection.type) }

        return when (type) {
            is ConeClassLikeType -> ConeClassLikeType(
                lookupTag = type.lookupTag,
                typeArguments = type.typeArguments.normalizeProjections(),
                attributes = type.attributes,
                isInterface = type.isInterface,
                isThisType = type.isThisType,
            )
            is ConeStructType -> ConeStructType(
                lookupTag = type.lookupTag,
                typeArguments = type.typeArguments.normalizeProjections(),
                attributes = type.attributes,
            )
            is ConeEnumType -> ConeEnumType(
                lookupTag = type.lookupTag,
                typeArguments = type.typeArguments.normalizeProjections(),
                attributes = type.attributes,
                isRefEnum = type.isRefEnum,
            )
            is ConeTypeAliasType -> ConeTypeAliasType(
                classId = type.classId,
                expandedType = type.expandedType?.let { expandedType ->
                    normalizeFreshReceiverTypeArguments(expandedType)
                },
                typeArguments = type.typeArguments.normalizeProjections(),
                attributes = type.attributes,
            )
            is ConeFunctionType -> ConeFunctionType(
                parameterTypes = type.parameterTypes.map { parameterType ->
                    normalizeFreshReceiverTypeArguments(parameterType)
                },
                returnType = normalizeFreshReceiverTypeArguments(type.returnType),
                isCFunc = type.isCFunc,
                isClosureType = type.isClosureType,
                hasVariableLenArg = type.hasVariableLenArg,
                attributes = type.attributes,
            )
            is ConeTupleType -> ConeTupleType(
                elementTypes = type.elementTypes.map { elementType ->
                    normalizeFreshReceiverTypeArguments(elementType)
                },
                attributes = type.attributes,
            )
            is ConeVArrayType -> ConeVArrayType(
                elementType = normalizeFreshReceiverTypeArguments(type.elementType),
                size = type.size,
                attributes = type.attributes,
            )
            is ConePointerType -> ConePointerType(
                pointeeType = normalizeFreshReceiverTypeArguments(type.pointeeType),
                attributes = type.attributes,
            )
            else -> type
        }
    }

    /**
     * 判断 owner 候选是否与 fresh receiver 已有约束兼容。
     *
     * 当前成员访问自身产生的 receiver 约束只说明“若选择该候选则需要满足该 owner”，
     * 不能反过来作为该候选可行的证据；其它实参、expected type 和前序成员访问约束才参与过滤。
     */
    private fun Candidate.isFreshReceiverOwnerCompatibleWithKnownConstraints(
        receiverTypeConstructor: TypeConstructorMarker,
        receiverExpression: CfirExpression,
        ownerType: ConeCangJieType,
    ): Boolean {
        val constraints = system.currentStorage()
            .notFixedTypeVariables[receiverTypeConstructor]
            ?.constraints
            .orEmpty()
            .filterNot { constraint ->
                val position = constraint.position.from
                position is ReceiverConstraintPosition<*> && position.argument === receiverExpression
            }
            .filter { constraint ->
                constraint.kind == ConstraintKind.LOWER ||
                    constraint.kind == ConstraintKind.UPPER ||
                    constraint.kind == ConstraintKind.EQUALITY
            }
            .mapNotNull { constraint -> constraint.type as? ConeCangJieType }
            .filterNot { constraintType -> constraintType is ConeErrorType }

        if (constraints.isEmpty()) return true
        return constraints.all { constraintType ->
            AbstractTypeChecker.isSubtypeOf(session.typeContext, constraintType, ownerType)
        }
    }

    /**
     * 为未固定类型变量选择可比较的代表约束类型。
     *
     * 没有下界/等式约束时保留类型变量；单个约束直接使用；多个约束取交集以表达共同要求。
     */
    private fun Candidate.representativeConstraintType(type: ConeCangJieType): ConeCangJieType {
        if (type !is ConeTypeVariableType) return type

        val lowerOrEqualConstraints = system.currentStorage()
            .notFixedTypeVariables[type.typeConstructor]
            ?.constraints
            .orEmpty()
            .filter { constraint ->
                constraint.kind == ConstraintKind.LOWER || constraint.kind == ConstraintKind.EQUALITY
            }
            .mapNotNull { constraint -> constraint.type as? ConeCangJieType }
            .filterNot { constraintType -> constraintType is ConeErrorType }

        return when (lowerOrEqualConstraints.size) {
            0 -> type
            1 -> lowerOrEqualConstraints.single()
            else -> ConeTypeIntersector.intersectTypes(session.typeContext, lowerOrEqualConstraints)
        }
    }

    /**
     * 返回候选 dispatch receiver 上的 fresh type variable constructor。
     *
     * 只接受非声明类型参数产生的 fresh 变量，用于识别 lambda receiver placeholder 的成员访问。
     */
    private fun Candidate.freshTypeVariableReceiverConstructor(): org.cangnova.cangjie.type.model.TypeConstructorMarker? {
        val receiverType = freshTypeVariableReceiverExpression()?.coneTypeOrNull as? ConeTypeVariableType ?: return null
        if (receiverType.typeConstructor.originalTypeParameter != null) return null
        return receiverType.typeConstructor
    }

    /** 返回 fresh lambda placeholder 对应的 dispatch 或 extension receiver 表达式。 */
    private fun Candidate.freshTypeVariableReceiverExpression(): CfirExpression? {
        dispatchReceiverExpression()?.takeIf { receiverExpression ->
            receiverExpression.coneTypeOrNull.isFreshLambdaReceiverTypeVariable()
        }?.let { return it }
        return givenExtensionReceiver?.expression?.takeIf { receiverExpression ->
            receiverExpression.coneTypeOrNull.isFreshLambdaReceiverTypeVariable()
        }
    }

    /** 判断类型是否为无上下文 lambda receiver 使用的 fresh type variable。 */
    private fun ConeCangJieType?.isFreshLambdaReceiverTypeVariable(): Boolean =
        this is ConeTypeVariableType && typeConstructor.originalTypeParameter == null

    /**
     * 根据候选规约结果构造最终 callee reference。
     *
     * 该函数集中处理未解析、候选错误、歧义、classifier 被当成函数调用、
     * 命名值成功引用和需要保留 candidate 的调用引用。
     */
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
        val hasInvalidTypeParameterUpperBoundReceiver =
            explicitReceiver?.coneTypeOrNull?.isTypeParameterWithInvalidDeclaredUpperBounds(session) == true

        // 根据期望的调用种类生成诊断
        val diagnostic = when {
            hasInvalidTypeParameterUpperBoundReceiver ->
                ConeUnreportedDuplicateDiagnostic(ConeSimpleDiagnostic("type parameter upper bound is already invalid"))
            expectedCallKind != null -> when (expectedCallKind) {
                CallKind.Function,
                CallKind.DelegatingConstructorCall,
                -> {
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
                    callInfo.callKind == CallKind.DelegatingConstructorCall -> ConeNoConstructorError
                    explicitReceiver?.importedPackageQualifierOrNull(components.file, session)?.packageFqName != null ->
                        ConeNotMemberOfError(
                            memberName = name,
                            kind = if (callInfo.callKind == CallKind.Function) "method" else "member",
                            typeName = explicitReceiver.importedPackageQualifierNameOrNull() ?: name,
                        )
                    explicitReceiver?.diagnosticFromCalleeReference() != null ->
                        ConeUnreportedDuplicateDiagnostic(explicitReceiver.diagnosticFromCalleeReference()!!)
                    matchedClassifier != null && callInfo.callKind == CallKind.Function -> {
                        val typeAliasExpandedType = (matchedClassifier as? CfirTypeAliasSymbol)
                            ?.takeIf { it.isBound }
                            ?.cfir
                            ?.expandedTypeRef
                            ?.coneTypeOrNull
                            ?.fullyExpandedType(session)
                        val actualClassifier = (matchedClassifier as? CfirTypeAliasSymbol)?.fullyExpandedClass(session)
                            ?: matchedClassifier
                        val actualDeclaration = actualClassifier.cfir as? CfirClassLikeDeclaration
                        when {
                            typeAliasExpandedType is ConeFunctionType ->
                                ConeFunctionExpectedError(name.asString(), typeAliasExpandedType)
                            actualDeclaration is org.cangnova.cangjie.cfir.declarations.CfirEnum ->
                                ConeNoMatchingInvokeOperatorError(actualClassifier.name, actualClassifier.constructType())
                            else -> ConeNoConstructorError
                        }
                    }
                    name == OperatorNameConventions.INVOKE && explicitReceiver is CfirLiteralExpression ->
                        ConeNoMatchOperatorFunctionCallError
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
                    isCallLike = callInfo.callKind == CallKind.Function ||
                            callInfo.callKind == CallKind.DelegatingConstructorCall ||
                            callInfo.callKind == CallKind.EnumConstructorCall,
                )
            }

            else -> {
                val candidate = candidates.single()
                val genericTypeInconsistentError = candidate.staticQualifierGenericTypeInconsistentError()
                when {
                    genericTypeInconsistentError != null -> genericTypeInconsistentError
                    candidate.hasUninferableBareStaticGenericQualifier() -> ConeUnableToInferGenericFuncError()
                    !candidate.isSuccessful -> createConeDiagnosticForCandidateWithError(applicability, candidate)
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

    /** 从表达式 callee reference 中提取已携带的诊断，用于避免 receiver 错误重复上报。 */
    private fun CfirExpression.diagnosticFromCalleeReference(): ConeDiagnostic? =
        ((this as? CfirResolvable)?.calleeReference as? CfirDiagnosticHolder)?.diagnostic

    /**
     * 泛型 static qualifier 经由继承或 extend 提升成员时，参数约束会先作用到
     * qualifier owner 的 fresh type variable。若同一个 owner 参数被实参推成多个
     * 不同类型，官方 `CheckAndGetMappingForTypeDecl` 报 generic-type-inconsistent，
     * 而不是普通参数不匹配或泛型推断失败。
     */
    private fun Candidate.staticQualifierGenericTypeInconsistentError(): ConeGenericTypeInconsistentError? {
        if (!symbol.isBound) return null
        val callable = symbol.cfir as? CfirCallableDeclaration ?: return null
        if (!callable.status.isStatic || callable is CfirConstructor || callable is CfirEnumConstructor) return null

        val receiver = callInfo.explicitReceiver as? CfirQualifiedAccessExpression ?: return null
        val qualifierOwnerTypeParameters = receiver.staticQualifierOwnerTypeParameterSymbols()
        if (qualifierOwnerTypeParameters.isEmpty()) return null

        val storage = system.currentStorage()
        for (freshVariable in freshVariables.filterIsInstance<ConeTypeParameterBasedTypeVariable>()) {
            if (freshVariable.typeParameterSymbol !in qualifierOwnerTypeParameters) continue

            val constrainedTypes = mutableListOf<ConeCangJieType>()
            fun addConstrainedType(type: ConeCangJieType?) {
                if (type == null || type is ConeErrorType) return
                if (constrainedTypes.none { existing -> existing.isSameTypeAs(type) }) {
                    constrainedTypes += type
                }
            }

            addConstrainedType(storage.fixedTypeVariables[freshVariable.typeConstructor] as? ConeCangJieType)
            storage.notFixedTypeVariables[freshVariable.typeConstructor]
                ?.constraints
                ?.filter { constraint ->
                    constraint.kind == ConstraintKind.LOWER || constraint.kind == ConstraintKind.EQUALITY
                }
                ?.forEach { constraint -> addConstrainedType(constraint.type as? ConeCangJieType) }

            if (constrainedTypes.size > 1) {
                return ConeGenericTypeInconsistentError(freshVariable.typeParameterSymbol.name, this)
            }
        }

        return null
    }

    /**
     * 返回 static qualifier 自身声明的泛型参数集合。
     *
     * 继承成员和 extend 成员的 callable owner 可能不是 qualifier class 本身；
     * 这里必须以源码中的 nominal base 为准，才能对齐官方 baseExpr 映射检查。
     */
    private fun CfirQualifiedAccessExpression.staticQualifierOwnerTypeParameterSymbols(): Set<CfirTypeParameterSymbol> {
        resolvedQualifierTypeAliasSymbol()?.cfir?.let { typeAlias ->
            val expandedType = typeAlias.expandedTypeRef.coneTypeOrNull ?: return emptySet()
            return typeAlias.typeParameters
                .filter { parameter -> expandedType.referencesTypeParameter(parameter.symbol) }
                .mapTo(linkedSetOf()) { parameter -> parameter.symbol }
        }

        val owner = unwrapSmartcastExpression().resolvedQualifierClassifier(session)?.cfir
            as? CfirTypeParameterRefsOwner
            ?: return emptySet()
        return owner.typeParameters.mapTo(linkedSetOf()) { parameter -> parameter.symbol }
    }

    /**
     * `Box.create()` 这类裸泛型类名静态成员调用属于调用推断错误。
     * 当 owner 泛型参数没有显式实参，且完全没有出现在可调用签名中时，
     * 调用上下文不可能为这些参数提供约束。
     * 构造器的 owner 类型参数由构造调用自身推断，不属于 static member qualifier 诊断。
     */
    private fun Candidate.hasUninferableBareStaticGenericQualifier(): Boolean {
        if (callInfo.callKind == CallKind.NamedValueAccess) return false
        val callable = symbol.cfir as? CfirCallableDeclaration ?: return false
        if (callable is CfirConstructor) return false
        if (callable is CfirEnumConstructor) return false
        if (!callable.status.isStatic) return false

        val receiver = callInfo.explicitReceiver as? CfirQualifiedAccessExpression ?: return false
        if (receiver.typeArguments.isNotEmpty()) return false

        val owner = receiver.unwrapSmartcastExpression().resolvedQualifierClassifier(session)?.cfir
            as? CfirClassLikeDeclaration ?: return false
        val ownerTypeParameterSymbols = candidateBareStaticOuterTypeParameters(owner, receiver)
            .mapTo(linkedSetOf()) { it.symbol }
        if (ownerTypeParameterSymbols.isEmpty()) return false
        if (hasExplicitTypeArgumentsForBareStaticQualifier(ownerTypeParameterSymbols)) return false

        val notFixedTypeVariables = system.currentStorage().notFixedTypeVariables
        if (notFixedTypeVariables.isEmpty()) return false

        // 这里必须以候选约束系统的最终状态为准。static extend/typealias/父类提升会在
        // fresh-variable 阶段把 qualifier owner 参数纳入同一个推断系统，重扫替换后的签名
        // 会把已经由实参或父类型映射固定的参数误判成不可推断。
        val storage = system.currentStorage()
        return freshVariables
            .asSequence()
            .filterIsInstance<ConeTypeParameterBasedTypeVariable>()
            .any { freshVariable ->
                if (freshVariable.typeParameterSymbol !in ownerTypeParameterSymbols) return@any false
                val fixedType = storage.fixedTypeVariables[freshVariable.typeConstructor] as? ConeCangJieType
                val variableWithConstraints = notFixedTypeVariables[freshVariable.typeConstructor]
                val lowerOrEqualTypes = fixedType?.let(::listOf)
                    ?: variableWithConstraints?.constraints
                        ?.filterNot { constraint -> constraint.kind.isUpper() }
                        ?.mapNotNull { constraint -> constraint.type as? ConeCangJieType }
                    ?: return@any false
                if (lowerOrEqualTypes.isEmpty()) return@any true

                val upperTypes = freshVariable.typeParameterSymbol.resolvedBounds
                    .map { bound -> substitutor.substituteOrSelf(bound.coneType) } +
                    variableWithConstraints?.constraints
                        ?.filter { constraint -> constraint.kind.isUpper() }
                        ?.mapNotNull { constraint -> constraint.type as? ConeCangJieType }
                        .orEmpty()
                upperTypes.isNotEmpty() && lowerOrEqualTypes.any { lowerType ->
                    upperTypes.any { upperType ->
                        !AbstractTypeChecker.isSubtypeOf(session.typeContext, lowerType, upperType)
                    }
                }
            }
    }

    /**
     * 显式类型实参已经按候选的 fresh-variable 参数序列绑定到裸 qualifier owner 时，
     * 该 qualifier 不再属于“泛型类型缺失类型实参”的场景。
     */
    private fun Candidate.hasExplicitTypeArgumentsForBareStaticQualifier(
        ownerTypeParameterSymbols: Set<CfirTypeParameterSymbol>,
    ): Boolean {
        val explicitCount = callInfo.typeArguments.count { it is CfirResolvedTypeRef }
        if (explicitCount == 0) return false

        val declaration = symbol.takeIf { it.isBound }?.cfir ?: return false
        val candidateTypeParameters = CfirCreateFreshTypeVariableSubstitutorStage
            .collectCandidateTypeParametersForFreshVariables(session, this, declaration)
        if (candidateTypeParameters.size != explicitCount) return false

        val explicitlyMappedSymbols = candidateTypeParameters.mapTo(linkedSetOf()) { it.symbol }
        return explicitlyMappedSymbols.containsAll(ownerTypeParameterSymbols)
    }

    /**
     * 官方 `GetAllGenericTys` 对 static member 使用 callable 的外层声明泛型。
     *
     * 普通 class static 成员的外层声明是 qualifier class；static extend 成员的外层
     * 声明是 owner extend，不能拿 qualifier class 的类型参数判断是否可推断。
    */
    private fun Candidate.candidateBareStaticOuterTypeParameters(
        owner: CfirClassLikeDeclaration,
        receiver: CfirQualifiedAccessExpression,
    ): List<CfirTypeParameterRef> {
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return owner.typeParameters
        val ownerExtend = session.extendProviderOrNull
            ?.getContainingExtend(callableSymbol.unwrapSubstitutionOverrides())
            ?.takeIf { session.extendProviderOrNull?.isExtendAccessible(it) == true }
        if (ownerExtend != null) return ownerExtend.typeParameters

        receiver.resolvedQualifierTypeAliasSymbol()?.cfir?.let { typeAlias ->
            val expandedType = typeAlias.expandedTypeRef.coneTypeOrNull ?: return emptyList()
            return typeAlias.typeParameters.filter { parameter ->
                expandedType.referencesTypeParameter(parameter.symbol)
            }
        }

        return owner.typeParameters
    }

    /** 若 qualifier 是已解析 typealias，则返回对应 typealias 符号。 */
    private fun CfirQualifiedAccessExpression.resolvedQualifierTypeAliasSymbol(): CfirTypeAliasSymbol? {
        val resolvedReference = calleeReference as? CfirResolvedNamedReference ?: return null
        return resolvedReference.resolvedSymbol as? CfirTypeAliasSymbol
    }

    /** 判断类型内部是否直接或间接引用指定类型参数符号。 */
    private fun ConeCangJieType.referencesTypeParameter(
        symbol: CfirTypeParameterSymbol,
    ): Boolean = contains { type ->
        type is ConeTypeParameterType && type.lookupTag.typeParameterSymbol == symbol
    }

    /** 判断类型内部是否引用给定类型参数符号集合中的任意一个。 */
    private fun ConeCangJieType.referencesAnyTypeParameter(
        symbols: Set<CfirTypeParameterSymbol>,
    ): Boolean = contains { type ->
        type is ConeTypeParameterType && type.lookupTag.typeParameterSymbol in symbols
    }

    /**
     * 识别源码直接写出的 `VArray<T, $N>(...)` 构造目标。
     *
     * 只有 callee 名称为 `VArray` 且调用节点带有尺寸字面量时才合成 VArray 内建构造目标。
     */
    private fun CfirFunctionCall.directVArrayConstructorTargetOrNull(
        name: Name,
    ): BuiltinArrayConstructorTarget.VArray? {
        if (name.asString() != "VArray") return null
        val sizeLiteral = varraySizeLiteral ?: return null
        return BuiltinArrayConstructorTarget.VArray(sizeLiteral = sizeLiteral)
    }

    /**
     * 将展开到 `VArray` 的 typealias 调用识别为内建 VArray 构造。
     *
     * 展开后会保留别名类型参数，用于后续候选阶段把显式 typealias 实参映射到元素类型。
     */
    private fun CfirTypeAliasSymbol.typeAliasVArrayConstructorTarget(
        typeArgumentRefs: List<CfirTypeRef>,
    ): BuiltinArrayConstructorTarget.VArray? {
        lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
        val alias = cfir
        val appliedArguments = typeArgumentRefs.mapNotNull { typeArgumentRef ->
            (typeArgumentRef as? CfirResolvedTypeRef)?.coneType
        }
        val aliasType: ConeCangJieType = ConeTypeAliasType(
            classId = classId,
            typeArguments = appliedArguments,
        )
        val expandedType = aliasType.fullyExpandedType(session) as? ConeVArrayType ?: return null
        return BuiltinArrayConstructorTarget.VArray(
            sizeLiteral = "$${expandedType.size}",
            elementType = expandedType.elementType,
            typeParameters = alias.typeParameters.map { typeParameter ->
                BuiltinConstructorTypeParameter(
                    name = typeParameter.name,
                    originalSymbol = typeParameter.symbol,
                )
            },
        )
    }

    /**
     * 将展开到 `CPointer` 的 typealias 调用识别为内建 pointer 构造。
     *
     * 返回目标携带 pointee 类型和 typealias 类型参数，供候选创建阶段完成约束映射。
     */
    private fun CfirTypeAliasSymbol.typeAliasPointerConstructorTarget(
        typeArgumentRefs: List<CfirTypeRef>,
    ): BuiltinPointerConstructorTarget? {
        lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
        val alias = cfir
        val appliedArguments = typeArgumentRefs.mapNotNull { typeArgumentRef ->
            (typeArgumentRef as? CfirResolvedTypeRef)?.coneType
        }
        val aliasType: ConeCangJieType = ConeTypeAliasType(
            classId = classId,
            typeArguments = appliedArguments,
        )
        val expandedType = aliasType.fullyExpandedType(session) as? ConePointerType ?: return null
        return BuiltinPointerConstructorTarget(
            pointeeType = expandedType.pointeeType,
            typeParameters = alias.typeParameters.map { typeParameter ->
                BuiltinConstructorTypeParameter(
                    name = typeParameter.name,
                    originalSymbol = typeParameter.symbol,
                )
            },
        )
    }

    /** 判断 typealias 展开结果是否为内建 `CString` 构造目标。 */
    private fun CfirTypeAliasSymbol.isTypeAliasCStringConstructorTarget(): Boolean {
        lazyResolveToPhase(CfirResolvePhase.SUPER_TYPES)
        val aliasType: ConeCangJieType = ConeTypeAliasType(classId = classId)
        return aliasType.fullyExpandedType(session) is ConeCStringType
    }

    /**
     * 解析 `VArray` 实例的合成 `size` 属性访问。
     *
     * VArray size 是类型尺寸参数对应的内建只读属性，这里合成局部 property 符号并把访问类型写为 `Int64`。
     */
    private fun tryResolveVArraySizeAccess(
        qualifiedAccess: CfirQualifiedAccessExpression,
        callee: CfirNamedReference,
    ): CfirExpression? {
        if (callee.name.asString() != "size") return null
        qualifiedAccess.explicitReceiver
            ?.coneTypeOrNull
            ?.fullyExpandedType(session) as? ConeVArrayType ?: return null

        val propertySymbol = CfirPropertySymbol(CallableId(callee.name))
        val int64Type = ConePrimitiveType(PrimitiveTypeKind.INT64)
        buildProperty {
            source = callee.source ?: qualifiedAccess.source
            moduleData = session.moduleData
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Synthetic.Default
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = true
            dispatchReceiverType = null
            symbol = propertySymbol
            status = CfirDeclarationStatusImpl()
            returnTypeRef = buildResolvedTypeRef {
                source = callee.source ?: qualifiedAccess.source
                coneType = int64Type
            }
            name = callee.name
            getter = null
            setter = null
        }
        qualifiedAccess.replaceCalleeReference(
            buildResolvedNamedReference {
                source = callee.source
                name = callee.name
                resolvedSymbol = propertySymbol
            }
        )
        qualifiedAccess.replaceConeTypeOrNull(int64Type)
        return qualifiedAccess
    }

    /**
     * 为 class-like 调用收集构造候选。
     *
     * 该入口先识别 typealias 到 VArray/Pointer/CString 的内建构造，再处理普通 class/typealias
     * 构造器 scope；enum 被当成函数调用时不创建构造候选，交由诊断路径报告。
     */
    private fun collectClassConstructorCandidates(
        functionCall: CfirFunctionCall,
        classifier: CfirClassLikeSymbol<*>,
        resolutionMode: ResolutionMode,
    ): ResolutionResult {
        val varrayAliasTarget = (classifier as? CfirTypeAliasSymbol)
            ?.typeAliasVArrayConstructorTarget(functionCall.typeArguments)
        if (varrayAliasTarget != null) {
            return collectBuiltinArrayConstructorCandidates(
                functionCall = functionCall,
                name = classifier.name,
                target = varrayAliasTarget,
                resolutionMode = resolutionMode,
            )
        }
        val pointerAliasTarget = (classifier as? CfirTypeAliasSymbol)
            ?.typeAliasPointerConstructorTarget(functionCall.typeArguments)
        if (pointerAliasTarget != null) {
            return collectBuiltinPointerConstructorCandidates(
                functionCall = functionCall,
                name = classifier.name,
                target = pointerAliasTarget,
                resolutionMode = resolutionMode,
            )
        }
        if ((classifier as? CfirTypeAliasSymbol)?.isTypeAliasCStringConstructorTarget() == true) {
            return collectBuiltinCStringConstructorCandidates(
                functionCall = functionCall,
                name = classifier.name,
                resolutionMode = resolutionMode,
            )
        }

        val actualClassifier = (classifier as? CfirTypeAliasSymbol)?.fullyExpandedClass(session) ?: classifier
        actualClassifier.lazyResolveToPhase(CfirResolvePhase.STATUS)
        val actualDeclaration = actualClassifier.cfir
        if (actualClassifier.classId == StdlibClassIds.Array) {
            return collectBuiltinArrayConstructorCandidates(
                functionCall = functionCall,
                name = classifier.name,
                target = BuiltinArrayConstructorTarget.Array,
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

        val constructorSymbols = buildList {
            if (classifier is CfirTypeAliasSymbol) {
                classifier.cfir.scopeProvider
                    .getTypealiasConstructorScope(classifier.cfir, session, components.scopeSession)
                    .processDeclaredConstructors(::add)
            } else {
                actualClassifier.cfir.declarations
                    .filterIsInstance<org.cangnova.cangjie.cfir.declarations.CfirConstructor>()
                    .mapTo(this, CfirConstructor::symbol)
            }
        }
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
                reduceConstructorCandidatesByOwnerTypeInference(
                    candidates = currentCandidates,
                    ownerTypeParameters = actualDeclaration.typeParameters,
                    hasExplicitTypeArguments = functionCall.typeArguments.isNotEmpty(),
                )?.let { return@reduceCollectedCandidates it }
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
     * 裸泛型 owner 的构造调用需要优先保留能约束 owner 类型参数的候选。
     *
     * 官方实例化检查以构造出的 nominal 类型实参为事实来源，随后检查该实例化下
     * 所有成员签名是否冲突；因此 `A(0)` 这类调用不能让不含 `T` 的 `init(Int64)`
     * 抢走结果类型代表，否则 owner `T` 会在 completion 阶段退化成“无法推断”。
     */
    private fun reduceConstructorCandidatesByOwnerTypeInference(
        candidates: Set<Candidate>,
        ownerTypeParameters: List<CfirTypeParameterRef>,
        hasExplicitTypeArguments: Boolean,
    ): Set<Candidate>? {
        if (hasExplicitTypeArguments || candidates.size <= 1 || ownerTypeParameters.isEmpty()) return null
        if (candidates.any { it.symbol.takeIf(CfirBasedSymbol<*>::isBound)?.cfir !is CfirConstructor }) return null

        val ownerTypeParameterSymbols = ownerTypeParameters.mapTo(linkedSetOf()) { it.symbol }
        val constrainingCandidates = candidates.filterTo(linkedSetOf()) { candidate ->
            val constructor = candidate.symbol.cfir as? CfirConstructor ?: return@filterTo false
            constructor.valueParameters.any { parameter ->
                parameter.returnTypeRef.coneTypeOrNull?.referencesAnyTypeParameter(ownerTypeParameterSymbols) == true
            }
        }
        return constrainingCandidates.takeIf { it.isNotEmpty() && it.size < candidates.size }
    }

    /**
     * 仓颉 `Array<T>(...)` 对应官方 `ArrayExpr` 内建表达式，
     * 不是 `std.core.Array` 的用户构造器。这里只在 classifier fallback
     * 层合成官方允许的调用形状，后续仍交给统一 call-resolution stages
     * 处理显式类型实参、命名实参、lambda 期望类型与约束系统。
     */
    private fun collectBuiltinArrayConstructorCandidates(
        functionCall: CfirFunctionCall,
        name: Name,
        target: BuiltinArrayConstructorTarget,
        resolutionMode: ResolutionMode,
    ): ResolutionResult {
        val callInfo = createBuiltinArrayConstructorCallInfo(functionCall, name, target, resolutionMode)
        val candidateFactory = CandidateFactory(transformer.resolutionContext, callInfo)
        val argumentCount = callInfo.arguments.size

        val arrayCandidates = if (argumentCount > 2) {
            listOf(
                candidateFactory.createBuiltinArrayConstructorCandidate(
                    callInfo = callInfo,
                    kind = BuiltinArrayConstructorKind.INIT_FUNCTION,
                    target = target,
                ).also { candidate ->
                    candidate.addDiagnostic(TooManyArguments(functionCall, callInfo.name))
                },
            )
        } else {
            val arrayCandidateKinds = builtinArrayConstructorKinds(functionCall, target, argumentCount)
            arrayCandidateKinds.map { kind ->
                candidateFactory.createBuiltinArrayConstructorCandidate(
                    callInfo = callInfo,
                    kind = kind,
                    target = target,
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

    /**
     * 仓颉 `CPointer<T>(...)` 对应官方 `PointerExpr` 内建表达式。
     *
     * 这里与 Array/VArray 一样只合成候选，不在 resolver 层提前判定类型成功；
     * 显式类型实参、参数个数、命名实参与约束求解继续由统一 stages 处理。
     */
    private fun collectBuiltinPointerConstructorCandidates(
        functionCall: CfirFunctionCall,
        name: Name,
        target: BuiltinPointerConstructorTarget,
        resolutionMode: ResolutionMode,
    ): ResolutionResult {
        val callInfo = createBuiltinConstructorCallInfo(functionCall, name, resolutionMode)
        val candidateFactory = CandidateFactory(transformer.resolutionContext, callInfo)
        val argumentCount = callInfo.arguments.size

        val candidates = if (argumentCount > 1) {
            listOf(
                candidateFactory.createBuiltinPointerConstructorCandidate(
                    callInfo = callInfo,
                    kind = BuiltinPointerConstructorKind.CONVERT_POINTER,
                    target = target,
                ).also { candidate ->
                    candidate.addDiagnostic(TooManyArguments(functionCall, callInfo.name))
                },
            )
        } else {
            val kind = when (argumentCount) {
                0 -> BuiltinPointerConstructorKind.EMPTY
                else -> BuiltinPointerConstructorKind.CONVERT_POINTER
            }
            listOf(
                candidateFactory.createBuiltinPointerConstructorCandidate(
                    callInfo = callInfo,
                    kind = kind,
                    target = target,
                ),
            )
        }
        val (reducedCandidates, applicability) = reduceCollectedCandidates(
            candidates = candidates,
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
     * 仓颉 `CString(CPointer<UInt8>)` 对应官方 CString built-in call。
     */
    private fun collectBuiltinCStringConstructorCandidates(
        functionCall: CfirFunctionCall,
        name: Name,
        resolutionMode: ResolutionMode,
    ): ResolutionResult {
        val callInfo = createBuiltinConstructorCallInfo(functionCall, name, resolutionMode)
        val candidateFactory = CandidateFactory(transformer.resolutionContext, callInfo)
        val candidate = candidateFactory.createBuiltinCStringConstructorCandidate(callInfo)
        val (reducedCandidates, applicability) = reduceCollectedCandidates(
            candidates = listOf(candidate),
            collectorApplicability = CandidateApplicability.HIDDEN,
            isCandidateSuccessful = Candidate::isSuccessful,
            candidateApplicability = Candidate::lowestApplicability,
            fullyProcessCandidate = { currentCandidate ->
                components.resolutionStageRunner.fullyProcessCandidate(currentCandidate, transformer.resolutionContext)
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
     * 根据目标数组种类和实参数量给出内建 Array/VArray 构造候选形状。
     *
     * 普通 Array 区分空数组、collection 构造、init 函数和重复元素；
     * VArray 的单实参命名形式代表重复元素，否则优先按 init 函数处理。
     */
    private fun builtinArrayConstructorKinds(
        functionCall: CfirFunctionCall,
        target: BuiltinArrayConstructorTarget,
        argumentCount: Int,
    ): List<BuiltinArrayConstructorKind> = when (target) {
        BuiltinArrayConstructorTarget.Array -> when (argumentCount) {
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
        is BuiltinArrayConstructorTarget.VArray -> when (argumentCount) {
            0 -> listOf(BuiltinArrayConstructorKind.EMPTY)
            1 -> if (functionCall.argumentList.arguments.singleOrNull()?.hasExplicitArgumentName() == true) {
                listOf(BuiltinArrayConstructorKind.REPEAT_ELEMENT)
            } else {
                listOf(BuiltinArrayConstructorKind.INIT_FUNCTION)
            }
            else -> listOf(BuiltinArrayConstructorKind.REPEAT_ELEMENT)
        }
    }

    /**
     * 判断表达式对应的源实参是否显式写了参数名。
     *
     * PSI 可用时直接读取 [CjValueArgument]；轻树/合成源不可用时通过源文本中的 `name:` 形态做保守识别。
     */
    private fun CfirExpression.hasExplicitArgumentName(): Boolean {
        val source = when (this) {
            is CfirBlock -> source?.takeIf { statements.size == 1 }
            else -> source
        } ?: return false

        val psiArgument = source.psi as? CjValueArgument
        if (psiArgument != null) return psiArgument.getArgumentName() != null

        val rawText = source.text?.toString()?.trim().orEmpty()
        val separatorIndex = rawText.indexOf(':')
        if (separatorIndex <= 0) return false
        return Name.identifierIfValid(rawText.substring(0, separatorIndex).trim()) != null
    }

    /** 为 class-like 构造调用创建统一 [CallInfo]。 */
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

    /**
     * 为 Array/VArray 内建构造创建 [CallInfo]。
     *
     * VArray 直写尺寸参数会在 type arguments 中剔除 `$N`，typealias VArray 构造则保留别名实参。
     */
    private fun createBuiltinArrayConstructorCallInfo(
        functionCall: CfirFunctionCall,
        name: Name,
        target: BuiltinArrayConstructorTarget,
        resolutionMode: ResolutionMode,
    ): CallInfo = CallInfo(
        callSite = functionCall,
        callKind = CallKind.Function,
        name = name,
        origin = functionCall.origin,
        explicitReceiver = functionCall.explicitReceiver,
        arguments = functionCall.argumentList.arguments,
        isUsedAsGetClassReceiver = false,
        typeArguments = functionCall.builtinArrayConstructorTypeArguments(target),
        session = session,
        containingFile = components.file,
        containingDeclarations = transformer.components.containingDeclarations,
        resolutionMode = resolutionMode,
    )

    /** 为 Pointer/CString 等非数组内建构造创建 [CallInfo]。 */
    private fun createBuiltinConstructorCallInfo(
        functionCall: CfirFunctionCall,
        name: Name,
        resolutionMode: ResolutionMode,
    ): CallInfo = CallInfo(
        callSite = functionCall,
        callKind = CallKind.Function,
        name = name,
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

    /**
     * 直写 `VArray<T, $N>` 中 `$N` 是内建 VArray 尺寸参数，不是合成构造函数的泛型实参。
     *
     * typealias 构造仍然保留别名声明的全部显式实参，因为这些实参用于展开后的 VArray 元素类型。
     */
    private fun CfirFunctionCall.builtinArrayConstructorTypeArguments(
        target: BuiltinArrayConstructorTarget,
    ): List<CfirTypeRef> {
        if (target is BuiltinArrayConstructorTarget.VArray && target.elementType == null) {
            return typeArguments.take(target.typeParameters.size)
        }
        return typeArguments
    }

    /** 为委托构造调用创建只面向目标声明构造器集合的 [CallInfo]。 */
    private fun createDelegatingConstructorCallInfo(
        functionCall: CfirFunctionCall,
        targetDeclaration: CfirClassLikeDeclaration,
        resolutionMode: ResolutionMode,
    ): CallInfo = CallInfo(
        callSite = functionCall,
        callKind = CallKind.DelegatingConstructorCall,
        name = targetDeclaration.name,
        origin = functionCall.origin,
        explicitReceiver = null,
        arguments = functionCall.argumentList.arguments,
        isUsedAsGetClassReceiver = false,
        typeArguments = functionCall.typeArguments,
        session = session,
        containingFile = components.file,
        containingDeclarations = transformer.components.containingDeclarations,
        resolutionMode = resolutionMode,
    )

    /**
     * 查找可能被当前调用语法当成构造调用目标的 classifier。
     *
     * 有显式 receiver 时只在 qualifier 的静态 scope 中查找；裸名调用会先走 tower classifier，
     * 再按同文件、显式 import、默认 import 兜底查找顶层 classifier。
     */
    private fun findClassifierForCall(
        qualifiedAccess: CfirQualifiedAccessExpression,
        name: Name,
    ): CfirClassLikeSymbol<*>? {
        val explicitReceiver = qualifiedAccess.explicitReceiver
        return if (explicitReceiver != null) {
            findClassifierInQualifierScope(explicitReceiver, name)
        } else {
            towerResolver.findClassifiers(name)
                .filterIsInstance<CfirClassLikeSymbol<*>>()
                .firstOrNull()
                ?: resolveTopLevelClassifierByShortName(name)
        }
    }

    /**
     * 按短名解析顶层 class-like 符号。
     *
     * 查找顺序为同文件声明、显式 import / star import、当前包、默认 import；
     * 该逻辑补足 tower 在某些构造调用 fallback 中无法直接拿到 classifier 的场景。
     */
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

    /** 在当前文件顶层声明中按短名查找 class-like 声明。 */
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

    /**
     * 将默认导入路径转换成指定短名可能对应的 [ClassId] 候选。
     *
     * star import 追加包下短名；普通 import 需要短名或别名与目标短名一致。
     */
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

    /**
     * 在 qualifier 的静态作用域中按名称查找嵌套 classifier。
     *
     * receiver 会先去掉 smartcast 包装，再通过 qualifier scope 访问静态 class-like 成员。
     */
    private fun findClassifierInQualifierScope(
        receiver: CfirExpression,
        name: Name,
    ): CfirClassLikeSymbol<*>? {
        val unwrappedReceiver = receiver.unwrapSmartcastExpression()
        val staticScope = unwrappedReceiver.qualifierScopeOrNull(session, components.scopeSession) ?: return null
        var result: CfirClassLikeSymbol<*>? = null
        staticScope.processClassifiersByName(name) { classifier ->
            if (result == null) {
                result = classifier
            }
        }
        return result
    }

    /** 判断 classifier 是否能作为表达式出现；type parameter 只允许在 receiver 语境中使用。 */
    private fun CfirClassifierSymbol<*>.isValidClassifierExpression(isUsedAsReceiver: Boolean): Boolean =
        this is CfirClassLikeSymbol<*> || (isUsedAsReceiver && this is CfirTypeParameterSymbol)

    /**
     * 为单个候选或空候选构造错误 callee reference。
     *
     * 未解析/隐藏候选会创建错误候选以承载诊断；已有候选上的适用性错误则保留原候选，
     * 让后续阶段仍能读取候选信息。
     */
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

    /** 调用候选收集与规约的结果包。 */
    private data class ResolutionResult(
        /** 当前调用的结构化解析输入。 */
        val info: CallInfo,
        /** 候选集合整体适用性。 */
        val applicability: CandidateApplicability,
        /** 已按适用性和最具体规则规约后的候选集合。 */
        val candidates: Collection<Candidate>,
        /** tower resolver 转发出的非候选诊断。 */
        val forwardedDiagnostics: List<ResolutionDiagnostic>,
    )
}

/** overload candidate set 中的一个候选及其是否属于当前最佳候选集合。 */
data class OverloadCandidate(val candidate: Candidate, val isInBestCandidates: Boolean)

/**
 * 通用候选集合规约工具。
 *
 * 调用方提供候选成功判定、适用性读取、完整 stage 处理和最具体候选选择逻辑；
 * 本函数负责按 collector 适用性、候选适用性分组和错误成功状态返回最终集合。
 */
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

/**
 * 归一化候选规约后的适用性。
 *
 * 候选 stage 报告成功或原适用性本身失败时保持原值；否则把“已解析但内部有错误”
 * 标记成 [CandidateApplicability.RESOLVED_WITH_ERROR]。
 */
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

/**
 * 收集所有候选的 tower collector。
 *
 * 与普通 collector 不同，该 collector 不在某个 tower group 停止，而是把每个 symbol 的首个候选保存下来，
 * 供 IDE/分析 API 查看完整 overload set。
 */
class AllCandidatesCollector(
    components: BodyResolveComponents,
    resolutionStageRunner: ResolutionStageRunner
) : CfirAllCandidatesCollector(
    components as CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    resolutionStageRunner
) {
    /** 按符号去重保存所有被 tower 访问到的候选。 */
    private val allCandidatesMap = mutableMapOf<CfirBasedSymbol<*>, Candidate>()

    /** 记录候选后继续执行普通 collector 的适用性处理。 */
    override fun consumeCandidate(
        group: CfirTowerGroup,
        candidate: Candidate,
        context: ResolutionContext
    ): CandidateApplicability {
        allCandidatesMap.getOrPut(candidate.symbol) { candidate }
        return super.consumeCandidate(group, candidate, context)
    }

    /** 收集全部候选时永不在当前 tower group 提前停止。 */
    override fun shouldStopAtTheGroup(group: CfirTowerGroup): Boolean = false

    /** 返回按符号去重后的全部候选集合。 */
    val allCandidates: Collection<Candidate>
        get() = allCandidatesMap.values
}
