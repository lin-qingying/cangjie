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
import org.cangnova.cangjie.cfir.resolve.calls.CandidateProcessingMode
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.cangjieVariadicParameterForMapping
import org.cangnova.cangjie.cfir.resolve.calls.hasUncertainExpectedTypeCompatibilityShape
import org.cangnova.cangjie.cfir.resolve.calls.substituteExplicitTypeArgumentConstraints
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
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.resolve.providers.semanticExtendedType
import org.cangnova.cangjie.cfir.resolve.withExpectedType
import org.cangnova.cangjie.cfir.resolve.inference.inferenceComponents
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.AbstractCandidate
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInArguments
import org.cangnova.cangjie.cfir.semantics.InvalidCallableReturnTypeInOverloadSet
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.accessibilityChecker
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.CjValueArgument
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
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
import java.util.IdentityHashMap

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

/** 命名值访问在当前解析步骤中的结构用途。 */
enum class NamedValueAccessPurpose {
    /** 普通源码值访问，解析结果会直接进入最终 CFIR。 */
    Regular,

    /** 隐式 `invoke` 改写中只用于构造临时 receiver 的值访问。 */
    ImplicitInvokeReceiver,

    /**
     * 模式裸名字的枚举构造器探测。探测失败表示普通绑定变量，不能产生最终诊断。
     */
    PatternConstructorProbe,
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
    /**
     * 函数调用节点到其不可变 tower discovery 的映射。
     *
     * completion 会把携带 Candidate 的引用改写成 applied reference，因此 discovery 不能只挂在
     * 可变 Candidate 上；调用节点对象在同一 body resolve 生命周期内保持稳定，适合作为 identity key。
     */
    private val expectedTypeRefinementDiscoveries =
        IdentityHashMap<CfirFunctionCall, List<CfirCallableCandidateDiscovery>>()

    /**
     * 清理当前 body-resolve 事务中暂存的 expected-return discovery。
     *
     * discovery 只服务于同一调用树内的 expected type 二次规约；它不是 session
     * 级缓存。显式在事务边界清理，避免候选描述中的 scope、symbol 和表达式图把
     * 已完成解析的文件长期保留。
     */
    fun clearExpectedTypeRefinementDiscoveries() {
        expectedTypeRefinementDiscoveries.clear()
    }

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
     * 该入口覆盖普通函数、enum constructor、class constructor、
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
        /*
         * `~>` 产生的 `composition` 不是用户源码名字查找：官方通过
         * `CreateRefExprInCore("composition")` 直接引用 core 声明。当前 LLT 使用的
         * std.core cjo 没有导出这项内部声明，因此不能把它交给普通 tower，也不能让
         * 当前文件的同名声明参与候选。其余调用仍保持统一的 scope-based discovery。
         */
        val compilerCoreIntrinsicResult = functionCall.compilerCoreIntrinsicResultOrNull(callee.name, resolutionMode)
        val result = compilerCoreIntrinsicResult ?: collectCandidates(
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
        var classLikeCallResolved = compilerCoreIntrinsicResult != null
        if (!classLikeCallResolved && !isCollectionLiteralCall && !result.hasExcludedCallableLookup) {
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
        if (
            !classLikeCallResolved &&
            result.candidates.isEmpty() &&
            !result.hasExcludedCallableLookup &&
            !isCollectionLiteralCall
        ) {
            // 阶段2a：普通函数搜索为空时，先尝试枚举构造器搜索（对齐官方两阶段语义：普通函数完全遮蔽枚举构造器）
            val enumResult = collectCandidates(
                functionCall,
                callee.name,
                CallKind.EnumConstructorCall,
                origin = functionCall.origin,
                resolutionMode = resolutionMode,
            )
            if (enumResult.candidates.isNotEmpty() || enumResult.hasExcludedCallableLookup) {
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
                if (variableAccessResult.hasExcludedCallableLookup) {
                    effectiveResult = variableAccessResult
                } else {
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
        }

        if (
            matchedClassifier == null &&
            effectiveResult.candidates.isEmpty() &&
            !effectiveResult.hasExcludedCallableLookup &&
            expectedCandidates == null
        ) {
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
            forwardedDiagnostics = effectiveResult.forwardedDiagnostics,
            callableLookupOutcomes = effectiveResult.callableLookupOutcomes,
        )

        functionCall.replaceCalleeReference(nameReference)
        val candidate = (nameReference as? CfirNamedReferenceWithCandidate)?.candidate
        reportBodyResolutionErrorToOverloadByLambdaCandidate(nameReference, candidate)
        candidate?.updateSourcesOfReceivers()
        return functionCall
    }

    /**
     * 为已有对应 CFIR 调用来源的 compiler-core intrinsic 建立候选。
     *
     * 这里不把缺失的 core 实现回退到普通 import/provider 查询；每个分支都必须有
     * 官方解糖代码和固定签名作为依据。未知 intrinsic 返回 null，由调用者保留既有解析路径，
     * 直到它拥有自己的语义模型。
     */
    private fun CfirFunctionCall.compilerCoreIntrinsicResultOrNull(
        name: Name,
        resolutionMode: ResolutionMode,
    ): ResolutionResult? {
        if (origin != CfirFunctionCallOrigin.CompilerCoreIntrinsic) return null
        return when (name.asString()) {
            "composition" -> collectCompilerCoreCompositionCandidates(this, name, resolutionMode)
            else -> null
        }
    }

    /**
     * 在不重复 tower discovery 的前提下，用已发现重载集合和新的 expected type 重建候选。
     *
     * 官方调用检查先发现声明集合，再在候选局部 checkpoint 中按目标返回类型检查；这里仅当
     * 所有已有候选的返回类型都可确定分类且得到唯一 survivor 时，读取 discovery 的不可变字段
     * 创建 fresh candidate。旧候选的约束系统、stage 进度、诊断和 replacement 均不会复用。
     * 返回类型仍含推断变量、错误恢复分量或无法唯一规约时返回 null，由调用方执行完整 resolver。
     */
    fun resolveCallFromPrecollectedCandidates(
        functionCall: CfirFunctionCall,
        resolutionMode: ResolutionMode,
        discoveries: List<CfirCallableCandidateDiscovery>,
    ): Pair<Candidate, CfirFunctionCall>? {
        if (discoveries.size <= 1) return null
        val callee = functionCall.calleeReference as? CfirNamedReference ?: return null
        val callKind = when {
            discoveries.all { discovery ->
                discovery.symbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor
            } -> CallKind.EnumConstructorCall

            discoveries.all { discovery ->
                discovery.symbol.takeIf { it.isBound }?.cfir is CfirFunction
            } -> CallKind.Function

            else -> return null
        }
        val info = buildCallInfo(
            qualifiedAccess = functionCall,
            name = callee.name,
            forceCallKind = callKind,
            origin = functionCall.origin,
            resolutionMode = resolutionMode,
        )
        val discovery = uniquePrecollectedCandidateByExpectedReturnType(info, discoveries)
            ?: return null
        val freshCandidate = CandidateFactory(transformer.resolutionContext, info).createCandidateFromDiscovery(
            callInfo = info,
            discovery = discovery,
        )
        val (reducedCandidates, applicability) = reduceCandidateSet(
            candidates = listOf(freshCandidate),
            info = info,
            collectorApplicability = CandidateApplicability.HIDDEN,
        )
        val reference = createResolvedNamedReference(
            reference = callee,
            name = callee.name,
            callInfo = info,
            candidates = reducedCandidates,
            applicability = applicability,
            explicitReceiver = functionCall.explicitReceiver,
        )
        functionCall.replaceCalleeReference(reference)
        val selectedCandidate = (reference as? CfirNamedReferenceWithCandidate)?.candidate ?: return null
        reportBodyResolutionErrorToOverloadByLambdaCandidate(reference, selectedCandidate)
        selectedCandidate.updateSourcesOfReceivers()
        return selectedCandidate to functionCall
    }

    /** 返回调用初次名字查找保存的不可变候选描述集合。 */
    fun expectedTypeRefinementDiscovery(
        functionCall: CfirFunctionCall,
    ): List<CfirCallableCandidateDiscovery>? = expectedTypeRefinementDiscoveries[functionCall]

    /**
     * 对已发现候选做无副作用的 expected-return 分类，并仅返回唯一确定 survivor。
     */
    private fun uniquePrecollectedCandidateByExpectedReturnType(
        info: CallInfo,
        discoveries: List<CfirCallableCandidateDiscovery>,
    ): CfirCallableCandidateDiscovery? {
        val expectedType = info.resolutionMode.expectedType
            ?.fullyExpandedType(session)
            ?: return null
        if (expectedType.hasUncertainExpectedTypeCompatibilityShape()) return null

        var matchingDiscovery: CfirCallableCandidateDiscovery? = null
        for (discovery in discoveries) {
            val candidateReturnType = discovery.deterministicReturnType ?: return null
            if (!AbstractTypeChecker.isSubtypeOf(session.typeContext, candidateReturnType, expectedType)) continue
            if (matchingDiscovery != null) return null
            matchingDiscovery = discovery
        }
        return matchingDiscovery
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
        constructedTargetType: ConeCangJieType,
        resolutionMode: ResolutionMode,
    ): CfirFunctionCall {
        val callee = functionCall.calleeReference as? CfirNamedReference ?: return functionCall
        targetDeclaration.symbol.lazyResolveToPhase(CfirResolvePhase.STATUS)
        val actualTarget = targetDeclaration.symbol.cfir
        val callInfo = createDelegatingConstructorCallInfo(functionCall, actualTarget, resolutionMode)
        val declaredMemberScope = CfirClassUseSiteMemberScope(
            session = session,
            classSymbol = actualTarget.symbol,
            symbolProvider = session.symbolProvider,
            ownerType = constructedTargetType,
            dispatchReceiverType = constructedTargetType,
            scopeKind = CfirClassMemberScopeKind.DECLARATION_SITE,
        )
        val constructorScope = CfirClassSubstitutionScope(
            session = session,
            useSiteMemberScope = declaredMemberScope,
            dispatchReceiverType = constructedTargetType,
        )
        val constructorSymbols = buildList {
            constructorScope.processDeclaredConstructors(::add)
        }
        val candidateFactory = CandidateFactory(transformer.resolutionContext, callInfo)
        val constructorCandidates = constructorSymbols.map { constructorSymbol ->
            candidateFactory.createCandidate(
                callInfo = callInfo,
                symbol = constructorSymbol,
                originScope = constructorScope,
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
                if (transformer.resolutionContext.candidateProcessingMode == CandidateProcessingMode.ARGUMENT_SHAPE) {
                    return@reduceCollectedCandidates currentCandidates
                }
                reduceArityOnlyErrorCandidates(functionCall, currentCandidates)
                    ?: currentCandidates.singleOrNull()?.let(::setOf)
                    ?: conflictResolver.chooseMaximallySpecificCandidates(currentCandidates)
            },
        )
        val reducedResult = ResolutionResult(
            info = callInfo,
            applicability = applicability,
            candidates = reducedCandidates,
            forwardedDiagnostics = emptyList(),
        ).reduceCandidatesByLambdaBody(functionCall)

        val nameReference = createResolvedNamedReference(
            callee,
            actualTarget.name,
            reducedResult.info,
            reducedResult.candidates,
            reducedResult.applicability,
            explicitReceiver = null,
            createResolvedReferenceWithoutCandidateForLocalVariables = false,
        )

        functionCall.replaceCalleeReference(nameReference)
        val candidate = (nameReference as? CfirNamedReferenceWithCandidate)?.candidate
        reportBodyResolutionErrorToOverloadByLambdaCandidate(nameReference, candidate)
        candidate?.updateSourcesOfReceivers()
        return functionCall
    }

    /**
     * 为普通函数、构造器和 enum constructor 选择最有解释力的纯参数数量失败候选。
     *
     * 该排序只参与失败诊断恢复，不参与任何成功 overload resolution。调用解析阶段已经
     * 完整处理候选后，若当前最优失败组的每个候选都只包含 missing/extra 参数诊断，
     * 则按调用实参数量到候选真实可接受 arity 区间的距离选择；距离相同时优先上界更小
     * 的区间，使同等距离的 extra-argument 诊断稳定优先于 missing-argument 诊断。
     * 默认参数与本次参数映射实际采用的仓颉变参形状都计入区间。
     */
    private fun reduceArityOnlyErrorCandidates(
        functionCall: CfirFunctionCall,
        candidates: Set<Candidate>,
    ): Set<Candidate>? {
        if (candidates.size <= 1) return null
        if (candidates.any { candidate ->
                val declaration = candidate.symbol.takeIf { it.isBound }?.cfir
                declaration !is CfirFunction && declaration !is CfirConstructor && declaration !is CfirEnumConstructor
            }
        ) return null
        if (candidates.any { candidate -> !candidate.hasOnlyArityMappingErrors() }) return null

        val argumentCount = functionCall.argumentList.arguments.size
        val selected = candidates.minWithOrNull(
            compareBy<Candidate> { candidate -> candidate.arityDistance(argumentCount) }
                .thenBy { candidate -> candidate.maximumAcceptedArity() },
        ) ?: return null
        return setOf(selected)
    }

    /** 判断候选失败是否完全由缺失或多余实参构成。 */
    private fun Candidate.hasOnlyArityMappingErrors(): Boolean =
        diagnostics.isNotEmpty() && diagnostics.all { diagnostic ->
            diagnostic is NoValueForParameter || diagnostic is TooManyArguments
        }

    /** 计算实参数量到候选真实可接受 arity 区间的距离。 */
    private fun Candidate.arityDistance(argumentCount: Int): Int {
        val requiredArity = requiredAcceptedArity()
        val maximumArity = maximumAcceptedArity()
        return when {
            argumentCount < requiredArity -> requiredArity - argumentCount
            argumentCount > maximumArity -> argumentCount - maximumArity
            else -> 0
        }
    }

    /**
     * 计算本次参数映射形状下的最小可接受实参数量。
     *
     * 只有映射器实际选择了仓颉变参路径时才排除变参形参，避免把普通 `Array<T>` 参数
     * 无条件当作可省略参数。
     */
    private fun Candidate.requiredAcceptedArity(): Int {
        val variadicParameter = cangjieVariadicParameterForCall
        return declaredParametersForMapping().count { parameter ->
            parameter != variadicParameter && parameter.defaultValue == null
        }
    }

    /** 计算本次参数映射形状下的最大可接受实参数量；仓颉变参路径没有有限上界。 */
    private fun Candidate.maximumAcceptedArity(): Int =
        if (cangjieVariadicParameterForCall != null) Int.MAX_VALUE else declaredParametersForMapping().size

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
        purpose: NamedValueAccessPurpose = NamedValueAccessPurpose.Regular,
    ): CfirExpression {
        return resolveNamedValueAccessAndSelectCandidateImpl(
            qualifiedAccess = qualifiedAccess,
            isUsedAsReceiver = isUsedAsReceiver,
            resolutionMode = resolutionMode,
            isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
            callSite = callSite,
            purpose = purpose,
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
            purpose = NamedValueAccessPurpose.Regular,
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
        purpose: NamedValueAccessPurpose = NamedValueAccessPurpose.Regular,
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
            // 类型参数在值位置仍应保留其 classifier 身份与真实类型，后置 checker 再报告非法值使用。
            if (!basicResult.isSuccess) {
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
            val enumValueCandidates = enumResult.candidates.filter { candidate ->
                (candidate.symbol.takeIf { it.isBound }?.cfir as? CfirEnumConstructor)
                    ?.valueParameters
                    ?.isEmpty() == true
            }
            if (enumValueCandidates.isNotEmpty()) {
                result = enumResult.withCandidates(enumValueCandidates)
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
        if (purpose == NamedValueAccessPurpose.PatternConstructorProbe &&
            reducedCandidates.none { candidate ->
                candidate.symbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor
            }
        ) {
            // `case Day(x)` 中的 x 先按裸模式名探测；没有枚举构造器时它是绑定，
            // 该次临时访问的 unresolved 结果不能污染最终 body diagnostics。
            return transformedAccess
        }
        transformedAccess.resolveEnumConstructorAsImplicitInvokeReceiver(
            callee = callee,
            candidates = reducedCandidates,
            purpose = purpose,
        )?.let { return it }
        if (!acceptCandidates(reducedCandidates)) return transformedAccess

        val nameReference = createResolvedNamedReference(
            reference = callee,
            name = callee.name,
            callInfo = result.info,
            candidates = reducedCandidates,
            applicability = result.applicability,
            explicitReceiver = transformedAccess.explicitReceiver,
            expectedCallKind = if (functionCallExpected) CallKind.Function else null,
            forwardedDiagnostics = result.forwardedDiagnostics,
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

        val bestCandidateIdentities = result.candidates.mapTo(linkedSetOf()) { candidate ->
            candidate.lookupIdentity()
        }
        return collector.allCandidates.map { candidate ->
            OverloadCandidate(
                candidate,
                isInBestCandidates = candidate.lookupIdentity() in bestCandidateIdentities,
            )
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
            for ((storage, choices1) in partials) {
                val choices = callableReferenceChoices(atom, storage)
                for (choice in choices) {
                    nextPartials += PartialResolution(
                        storage = choice.candidate.system.currentStorage(),
                        choices = choices1 + choice,
                    )
                }
            }
            if (nextPartials.isEmpty()) {
                if (atom.failureKind == null) {
                    atom.markNoMatchingFunctionReference()
                }
                return CallableReferenceResolutionResult.FAILURE
            }
            partials = nextPartials
        }

        if (partials.size != 1) {
            if (atoms.any { atom -> atom.isPostponedBecauseOfAmbiguity }) {
                val choicesByAtom = atoms.associateWith { atom ->
                    partials
                        .asSequence()
                        .flatMap { partial -> partial.choices.asSequence() }
                        .filter { choice -> choice.atom === atom }
                        .distinctBy { choice -> choice.candidate }
                        .toList()
                }
                val ambiguousArgumentAtom = choicesByAtom.entries.firstOrNull { (_, choices) ->
                    choices.distinctResultingTypes().size > 1
                }
                if (ambiguousArgumentAtom != null) {
                    val representative = ambiguousArgumentAtom.value.firstOrNull()
                    if (representative != null) {
                        containingCallCandidate.system.replaceContentWith(representative.candidate.system.currentStorage())
                        containingCallCandidate.additionalCompletionVariables +=
                            representative.candidate.freshVariables.map { variable -> variable.typeConstructor }
                        containingCallCandidate.additionalCompletionVariables +=
                            representative.candidate.additionalCompletionVariables
                        representative.apply()
                        ambiguousArgumentAtom.key.commitResultingCallableReference()
                    }
                    ambiguousArgumentAtom.key.failureKind = CallableReferenceFailureKind.AMBIGUOUS_ARGUMENT_TYPE
                    return CallableReferenceResolutionResult.FAILURE
                }
                atoms.forEach { atom ->
                    val choices = choicesByAtom.getValue(atom)
                    atom.markAmbiguousFunctionReference(choices)
                }
                return CallableReferenceResolutionResult.FAILURE
            }

            atoms.forEach { atom -> atom.postponeBecauseOfAmbiguity() }
            return CallableReferenceResolutionResult.POSTPONED
        }

        val resolved = partials.single()
        containingCallCandidate.system.replaceContentWith(resolved.storage)
        for (choice in resolved.choices) {
            containingCallCandidate.additionalCompletionVariables +=
                choice.candidate.freshVariables.map { variable -> variable.typeConstructor }
            containingCallCandidate.additionalCompletionVariables +=
                choice.candidate.additionalCompletionVariables
            choice.candidate.system.replaceContentWith(resolved.storage)
            choice.apply()
        }
        return CallableReferenceResolutionResult.RESOLVED
    }

    /** 将 completion 后仍有多个匹配项的 callable reference 写回为专用歧义诊断。 */
    private fun ConeResolvedCallableReferenceAtom.markAmbiguousFunctionReference(
        choices: List<CallableReferenceChoice>,
    ) {
        val expression = expression as? CfirNamedAccessExpression
        val reference = expression?.calleeReference as? CfirNamedReference
        if (expression == null || reference == null || choices.isEmpty()) {
            markResolved()
            return
        }

        val diagnostic = ConeAmbiguousFunctionReferenceError(
            name = reference.name,
            candidatesWithErrors = choices.associate { choice -> choice.candidate to null },
        )
        resultingReference = buildErrorNamedReference {
            source = reference.source
            name = reference.name
            this.diagnostic = diagnostic
        }
        resultingTypeForCallableReference = ConeErrorType(diagnostic, delegatedType = expectedType)
        failureKind = CallableReferenceFailureKind.AMBIGUITY
        markResolved()
    }

    /**
     * 将目标函数类型下没有可用声明的 callable reference 物化为错误引用。
     *
     * Eager resolve 会同时把失败记到外层候选，使该候选退出 overload 集；但错误诊断
     * 的源码归属属于函数引用自身。必须在 postponed atom 上写回错误引用，completion
     * 和 checker 才能保留该锚点，而不会把它降级为外层 synthetic invoke 的失败。
     */
    private fun ConeResolvedCallableReferenceAtom.markNoMatchingFunctionReference() {
        val expression = expression as? CfirNamedAccessExpression
        val reference = expression?.calleeReference as? CfirNamedReference
        if (expression == null || reference == null) {
            failureKind = CallableReferenceFailureKind.NO_MATCH
            markResolved()
            return
        }

        val diagnostic = ConeNoMatchingFunctionReferenceError(reference.name)
        resultingReference = buildErrorNamedReference {
            source = reference.source
            name = reference.name
            this.diagnostic = diagnostic
        }
        resultingTypeForCallableReference = ConeErrorType(diagnostic, delegatedType = expectedType)
        failureKind = CallableReferenceFailureKind.NO_MATCH
        markResolved()
        commitResultingCallableReference()
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
            atom.resultingReference = CfirNamedReferenceWithCandidate(
                reference.source,
                reference.name,
                candidate,
            )
            atom.resultingTypeForCallableReference = resultingType
            atom.failureKind = null
            atom.markResolved()
        }
    }

    /**
     * 为一个 callable reference atom 枚举在当前约束系统快照下可成功的候选选择。
     *
     * expected type 会先按外层候选约束系统替换，再在该分支的 base system 上运行局部 tower resolve。
     * 普通 collector 只保留当前最佳 tower group，避免把被遮蔽或低优先级声明引入函数引用歧义。
     */
    private fun callableReferenceChoices(
        atom: ConeResolvedCallableReferenceAtom,
        baseSystem: ConstraintStorage,
    ): List<CallableReferenceChoice> {
        val expression = atom.expression as? CfirNamedAccessExpression ?: return emptyList()
        val expectedType = atom.expectedTypeForCallableReference(baseSystem) ?: return emptyList()
        val callInfo = expression.callableReferenceCallInfo(expectedType)
        val collector = CfirCandidateCollector(components, components.resolutionStageRunner)
        val resultCollector = towerResolver.runResolver(
            info = callInfo,
            context = transformer.resolutionContext,
            externalCollector = collector,
            candidateFactory = CandidateFactory(transformer.resolutionContext, baseSystem),
        )
        val (reducedCandidates, _) = reduceCandidates(resultCollector, callInfo)
        val functionCandidates = reducedCandidates.filter { candidate ->
            candidate.symbol.takeIf { it.isBound }?.cfir is CfirFunction
        }
        val hasExplicitTypeArguments = expression.typeArguments.isNotEmpty() || callInfo.hasExplicitTypeArguments
        val originalCandidates = if (hasExplicitTypeArguments) {
            functionCandidates
        } else {
            functionCandidates.filter { candidate ->
                val function = candidate.symbol.takeIf { it.isBound }?.cfir as? CfirFunction
                function?.typeParameters.isNullOrEmpty()
            }
        }
        if (!hasExplicitTypeArguments && originalCandidates.isEmpty() && functionCandidates.isNotEmpty()) {
            atom.markGenericTypeArgumentRequired()
            return emptyList()
        }
        if (originalCandidates.isEmpty()) return emptyList()

        val choices = originalCandidates.mapNotNull { candidate ->
            val resultingType = candidate.currentFunctionReferenceType() ?: return@mapNotNull null
            CallableReferenceChoice(atom, expression, candidate, resultingType)
        }
        if (choices.size <= 1) return choices

        val mostSpecificCandidates = conflictResolver.chooseMaximallySpecificCandidates(choices.map { it.candidate })
        return choices.filter { choice -> choice.candidate in mostSpecificCandidates }
    }

    /**
     * 完成函数引用候选并用约束系统最终 substitutor 构造对外函数类型。
     */
    private fun Candidate.currentFunctionReferenceType(): ConeCangJieType? {
        val rawResultingType = resultingTypeForCallableReference ?: return null
        if (!isSuccessful || system.hasContradiction) return null
        val resultingSubstitutor = system.currentStorage()
            .buildCurrentSubstitutor(session.typeContext, emptyMap())
            .asCone()
        return substituteExplicitTypeArgumentConstraints(
            resultingSubstitutor.substituteOrSelf(rawResultingType),
        )
    }

    /** 按类型系统相等性对 callable-reference 结果函数类型去重。 */
    private fun List<CallableReferenceChoice>.distinctResultingTypes(): List<ConeCangJieType> {
        val result = mutableListOf<ConeCangJieType>()
        for ((_, _, _, resultingType) in this) {
            if (result.none { type -> AbstractTypeChecker.equalTypes(session.typeContext, type, resultingType) }) {
                result += resultingType
            }
        }
        return result
    }

    /** 将只有泛型候选的裸函数引用写回为缺少显式类型实参。 */
    private fun ConeResolvedCallableReferenceAtom.markGenericTypeArgumentRequired() {
        val expression = expression as? CfirNamedAccessExpression ?: return
        val reference = expression.calleeReference as? CfirNamedReference ?: return
        val diagnostic = ConeSimpleDiagnostic(
            "generic function reference should be used with type argument",
            DiagnosticKind.GenericTypeWithoutTypeArgument,
        )
        resultingReference = buildErrorNamedReference {
            source = reference.source
            name = reference.name
            this.diagnostic = diagnostic
        }
        resultingTypeForCallableReference = ConeErrorType(diagnostic, delegatedType = expectedType)
        failureKind = CallableReferenceFailureKind.GENERIC_TYPE_ARGUMENT_REQUIRED
        markResolved()
        commitResultingCallableReference()
    }

    /** 失败候选已确定专用诊断时，将 atom 的局部结果提交到对应实参表达式。 */
    private fun ConeResolvedCallableReferenceAtom.commitResultingCallableReference() {
        val expression = expression as? CfirNamedAccessExpression ?: return
        resultingReference?.let(expression::replaceCalleeReference)
        resultingTypeForCallableReference?.let(expression::replaceConeTypeOrNull)
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
        val info = buildCallInfo(
            qualifiedAccess = qualifiedAccess,
            name = name,
            forceCallKind = forceCallKind,
            isUsedAsGetClassReceiver = isUsedAsGetClassReceiver,
            origin = origin,
            containingDeclarations = containingDeclarations,
            callSite = callSite,
            resolutionMode = resolutionMode,
            collectionLiteralContext = collectionLiteralContext,
        )

        return collectCandidates(info = info, resolutionContext = resolutionContext, collector = collector)
    }

    /**
     * 为 tower discovery 与已发现候选的 expected-return 细化构造同一份调用信息。
     *
     * 两条路径必须共享实参、receiver、call kind 与外层 collection-literal 语境；否则 fresh
     * candidate 会在与原始名字查找不同的调用模型上执行 stages。
     */
    private fun buildCallInfo(
        qualifiedAccess: CfirQualifiedAccessExpression,
        name: Name,
        forceCallKind: CallKind? = null,
        isUsedAsGetClassReceiver: Boolean = false,
        origin: CfirFunctionCallOrigin = CfirFunctionCallOrigin.Regular,
        containingDeclarations: List<CfirDeclaration> = transformer.components.containingDeclarations,
        callSite: CfirElement = qualifiedAccess,
        resolutionMode: ResolutionMode,
        collectionLiteralContext: CollectionLiteralOuterCandidateContext? = null,
    ): CallInfo {
        val explicitReceiver = qualifiedAccess.explicitReceiver
        val arguments = (qualifiedAccess as? CfirFunctionCall)?.argumentList?.arguments ?: emptyList()
        val typeArguments = qualifiedAccess.typeArguments

        val callKind = when {
            forceCallKind != null -> forceCallKind
            collectionLiteralContext != null -> CallKind.Function
            qualifiedAccess is CfirFunctionCall -> CallKind.Function
            else -> CallKind.NamedValueAccess
        }

        return CallInfo(
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
        var result = ResolutionResult(
            info = info,
            applicability = applicability,
            candidates = reducedCandidates,
            forwardedDiagnostics = resultCollector.forwardedDiagnostics(),
            callableLookupOutcomes = resultCollector.excludedCallableLookupOutcomes(),
        )
        if (callSite is CfirQualifiedAccessExpression) {
            result = result.reduceCandidatesByLambdaBody(callSite)
        }

        val expectedTypeRefinementDiscovery = buildExpectedTypeRefinementDiscovery(
            info = info,
            candidates = resultCollector.candidatesDiscoveredInBestGroup(),
        )
        val functionCall = info.callSite as? CfirFunctionCall
        if (functionCall != null && (info.callKind == CallKind.Function || info.callKind == CallKind.EnumConstructorCall)) {
            if (expectedTypeRefinementDiscovery.isEmpty()) {
                expectedTypeRefinementDiscoveries.remove(functionCall)
            } else {
                expectedTypeRefinementDiscoveries[functionCall] = expectedTypeRefinementDiscovery
            }
        }

        return result
    }

    /**
     * 把初次 tower 名字查找的完整成功候选集冻结成 expected-return 细化描述。
     *
     * 任一候选仍不成功、符号不是 callable 或返回类型不能确定时，描述仍保留但其返回类型为空；
     * 后续规约会整体回退完整 resolver，不会从不完整信息中删除候选。
     */
    private fun buildExpectedTypeRefinementDiscovery(
        info: CallInfo,
        candidates: Collection<Candidate>,
    ): List<CfirCallableCandidateDiscovery> {
        if (info.callKind != CallKind.Function && info.callKind != CallKind.EnumConstructorCall) return emptyList()
        if (candidates.size <= 1 || candidates.any { !it.isSuccessful }) return emptyList()

        val discoveries = ArrayList<CfirCallableCandidateDiscovery>(candidates.size)
        for (candidate in candidates) {
            val symbol = candidate.symbol as? CfirCallableSymbol<*> ?: return emptyList()
            val returnType = components.initialTypeOfCandidate(candidate).fullyExpandedType(session)
            val deterministicReturnType = returnType.takeIf {
                candidate.system.isProperType(it) && !it.hasUncertainExpectedTypeCompatibilityShape()
            }
            discoveries += CfirCallableCandidateDiscovery(
                symbol = symbol,
                dispatchReceiverExpression = candidate.dispatchReceiver?.expression,
                givenExtensionReceiverExpression = candidate.givenExtensionReceiver?.expression,
                explicitReceiverKind = candidate.explicitReceiverKind,
                originScope = candidate.originScope,
                accessibilityResult = candidate.discoveryAccessibilityResult,
                lookupProvenance = candidate.lookupProvenance,
                isFromCompanionObjectTypeScope = candidate.isFromCompanionObjectTypeScope,
                isFromOriginalTypeInPresenceOfSmartCast = candidate.isFromOriginalTypeInPresenceOfSmartCast,
                deterministicReturnType = deterministicReturnType,
            )
        }
        return discoveries.distinctBy { discovery -> discovery.refinementIdentityKey() }
    }

    /**
     * 同一声明可能同时经文件 scope 与 package scope 被发现；expected-return 细化按真实调用目标去重，
     * 但保留 receiver 身份与 smart-cast 来源不同的候选，因为它们携带不同的调用约束。
     */
    private fun CfirCallableCandidateDiscovery.refinementIdentityKey(): ExpectedTypeRefinementDiscoveryKey =
        ExpectedTypeRefinementDiscoveryKey(
            symbol = symbol,
            dispatchReceiverExpression = dispatchReceiverExpression,
            givenExtensionReceiverExpression = givenExtensionReceiverExpression,
            explicitReceiverKind = explicitReceiverKind,
            isFromCompanionObjectTypeScope = isFromCompanionObjectTypeScope,
            isFromOriginalTypeInPresenceOfSmartCast = isFromOriginalTypeInPresenceOfSmartCast,
        )

    /** expected-return discovery 的语义调用身份；origin scope 不参与同声明重复发现的区分。 */
    private data class ExpectedTypeRefinementDiscoveryKey(
        val symbol: CfirCallableSymbol<*>,
        val dispatchReceiverExpression: CfirExpression?,
        val givenExtensionReceiverExpression: CfirExpression?,
        val explicitReceiverKind: org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind,
        val isFromCompanionObjectTypeScope: Boolean,
        val isFromOriginalTypeInPresenceOfSmartCast: Boolean,
    )

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
            // 源码类型实参属于被调用的函数值表达式；函数类型 synthetic invoke
            // 自身没有声明类型参数，不能继承 receiver 的类型实参。
            typeArguments = emptyList(),
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
            // synthetic invoke 候选由本入口即时创建，必须完整执行 visibility/type-argument/
            // argument-mapping/type-check stages，不能借 collector success 快路径跳过。
            collectorApplicability = CandidateApplicability.HIDDEN,
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
        return when (val type = rawType.fullyExpandedType(session)) {
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
        if (transformer.resolutionContext.candidateProcessingMode == CandidateProcessingMode.ARGUMENT_SHAPE) {
            return candidates
        }
        if (candidates.size <= 1 || info.callKind != CallKind.NamedValueAccess) return candidates
        val expectedFunctionType = info.resolutionMode.expectedType
            ?.fullyExpandedType() as? ConeFunctionType ?: return candidates

        val functionCandidates = candidates.filter { candidate ->
            candidate.symbol.takeIf { it.isBound }?.cfir is CfirFunction
        }
        if (functionCandidates.size != candidates.size) return candidates
        val expression = info.callSite as? CfirNamedAccessExpression ?: return candidates

        val matchingCandidates = functionCandidates.filterTo(linkedSetOf()) { candidate ->
            candidate.completedFunctionReferenceType(expression, expectedFunctionType) != null
        }
        return when (matchingCandidates.size) {
            0 -> candidates
            1 -> matchingCandidates
            else -> matchingCandidates
        }
    }

    /**
     * 将函数值 overload set 在完整目标函数类型下归类为成功、无匹配或函数引用歧义。
     *
     * 含未固定类型变量的目标类型仍由 postponed argument completion 处理；这里仅在目标类型
     * 已经确定时生成官方 `ChkRefExpr` 对应的结构化结果。
     */
    private fun functionReferenceTargetDiagnostic(
        info: CallInfo,
        candidates: Collection<Candidate>,
    ): ConeDiagnostic? {
        if (info.callKind != CallKind.NamedValueAccess || candidates.isEmpty()) return null
        val expectedFunctionType = info.resolutionMode.expectedType
            ?.fullyExpandedType() as? ConeFunctionType ?: return null
        if (expectedFunctionType.contains { type -> type is ConeTypeVariableType }) return null
        if (candidates.any { candidate -> candidate.symbol.takeIf { it.isBound }?.cfir !is CfirFunction }) return null
        val expression = info.callSite as? CfirNamedAccessExpression ?: return null

        val matchingCandidates = candidates.filter { candidate ->
            candidate.completedFunctionReferenceType(expression, expectedFunctionType) != null
        }
        return when (matchingCandidates.size) {
            0 -> ConeNoMatchingFunctionReferenceError(info.name)
            1 -> null
            else -> ConeAmbiguousFunctionReferenceError(
                name = info.name,
                candidatesWithErrors = matchingCandidates.associateWith { null },
            )
        }
    }

    /**
     * 在已确定的目标函数类型下独立完成一个函数值候选，并返回完成后的函数类型。
     *
     * 该探测只服务于普通 named-value access 的最终重载规约：目标类型不含外层待固定变量，
     * 因而可以在隔离的候选约束系统中完成泛型函数实例化。postponed callable-reference
     * 实参使用外层调用的分支约束系统，不经过此入口，避免提前固定外层类型变量。
     * 隔离候选上的可见性、receiver 等解析诊断不参与函数类型兼容性；本入口只依据参数形状、
     * resulting type、约束一致性和最终函数类型子类型关系作出判断，原候选继续拥有并报告这些错误。
     */
    private fun Candidate.completedFunctionReferenceType(
        expression: CfirNamedAccessExpression,
        expectedFunctionType: ConeFunctionType,
    ): ConeCangJieType? {
        if (!hasCompatibleCallableReferenceParameterShape(expectedFunctionType, session.typeContext)) return null

        val callInfo = expression.callableReferenceCallInfo(expectedFunctionType)
        val probeCandidate = CandidateFactory(transformer.resolutionContext, callInfo)
            .createCallableReferenceCandidate(callInfo, this)
        components.resolutionStageRunner.fullyProcessCandidate(
            probeCandidate,
            transformer.resolutionContext,
        )
        val rawResultingType = probeCandidate.resultingTypeForCallableReference ?: return null
        if (probeCandidate.system.hasContradiction) return null

        components.callCompleter.runCompletionForCall(
            candidate = probeCandidate,
            completionMode = ConstraintSystemCompletionMode.FULL,
            call = expression,
            initialType = rawResultingType,
        )
        if (probeCandidate.system.hasContradiction) return null

        val resultingType = probeCandidate.system.currentStorage()
            .buildAbstractResultingSubstitutor(session.typeContext)
            .asCone()
            .substituteOrSelf(rawResultingType)
        return resultingType.takeIf { type ->
            AbstractTypeChecker.isSubtypeOfForFunctionReference(
                session.typeContext,
                type,
                expectedFunctionType,
            )
        }
    }

    /**
     * 对 tower collector 的最佳候选进行完整 stage 处理和最具体候选规约。
     *
     * 带 expected type 的独立函数值保留同组完整 discovery 供函数类型选择；无 expected type
     * 默认先按 accessibility 过滤，并由 static qualifier 专用策略决定是否保留可见性前的歧义集合。
     * 其他路径继续按适用性、尾随 lambda、expected return type 和 fresh receiver 规则逐步收窄。
     */
    private fun reduceCandidates(
        collector: CfirCandidateCollector,
        info: CallInfo,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
    ): Pair<Set<Candidate>, CandidateApplicability> {
        val discoveredFunctionValueCandidates = collector.functionValueCandidates()
        val functionValueCandidates = when {
            !info.isStandaloneFunctionValueAccess() -> discoveredFunctionValueCandidates
            else -> {
                val accessibleFunctionValueCandidates = discoveredFunctionValueCandidates.filter { candidate ->
                    candidate.isSuccessful
                }
                val preVisibilityStaticQualifierOverloadSet =
                    info.preVisibilityStaticQualifierOverloadSet(discoveredFunctionValueCandidates)
                val selectableFunctionValueCandidates =
                    preVisibilityStaticQualifierOverloadSet ?: accessibleFunctionValueCandidates
                when {
                    info.resolutionMode.expectedType != null ->
                        discoveredFunctionValueCandidates

                    info.hasExplicitTypeArguments ->
                        accessibleFunctionValueCandidates

                    else -> {
                        val nonGenericCandidates = selectableFunctionValueCandidates.filter { candidate ->
                            val function = candidate.symbol.takeIf { it.isBound }?.cfir as? CfirFunction
                            function?.typeParameters.isNullOrEmpty()
                        }
                        nonGenericCandidates.ifEmpty { selectableFunctionValueCandidates }
                    }
                }
            }
        }
        val bestCandidates = collector.bestCandidates()
        val preserveFunctionValueOverloadSet =
            info.callKind == CallKind.NamedValueAccess &&
                    functionValueCandidates.size > 1 &&
                    bestCandidates.isNotEmpty() &&
                    bestCandidates.all { candidate ->
                        candidate.symbol.takeIf { it.isBound }?.cfir is CfirFunction
                    } &&
                    collector.functionValueCandidatesGroup() == collector.bestGroup
        val namedValueCandidates = when {
            preserveFunctionValueOverloadSet -> functionValueCandidates
            else -> bestCandidates
        }
        val overloadCandidates = when (info.callKind) {
            CallKind.Function -> collector.candidatesDiscoveredInBestGroup()
            else -> namedValueCandidates
        }

        return reduceCandidateSet(
            candidates = namedValueCandidates,
            overloadCandidates = overloadCandidates,
            collectorApplicability = if (preserveFunctionValueOverloadSet) {
                CandidateApplicability.RESOLVED
            } else {
                collector.currentApplicability
            },
            info = info,
            preserveNamedValueCandidateSet = preserveFunctionValueOverloadSet,
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
        overloadCandidates: Collection<Candidate> = candidates,
        preserveNamedValueCandidateSet: Boolean = false,
        resolutionContext: ResolutionContext = transformer.resolutionContext,
    ): Pair<Set<Candidate>, CandidateApplicability> {
        val returnTypeReduction = reduceInvalidReturnTypeOverloads(
            selectedCandidates = candidates,
            overloadCandidates = overloadCandidates,
            info = info,
            collectorApplicability = collectorApplicability,
        )
        return reduceCollectedCandidates(
            candidates = returnTypeReduction.candidates,
            collectorApplicability = returnTypeReduction.collectorApplicability,
            isCandidateSuccessful = Candidate::isSuccessful,
            candidateApplicability = Candidate::lowestApplicability,
            fullyProcessCandidate = { candidate ->
                components.resolutionStageRunner.fullyProcessCandidate(candidate, resolutionContext)
            },
            chooseMostSpecific = { candidates ->
                if (transformer.resolutionContext.candidateProcessingMode == CandidateProcessingMode.ARGUMENT_SHAPE) {
                    return@reduceCollectedCandidates candidates
                }
                if (preserveNamedValueCandidateSet) {
                    return@reduceCollectedCandidates candidates
                }
                val arityFilteredCandidates = reduceCandidatesByArgumentCount(info, candidates)
                val syntaxFilteredCandidates =
                    reduceTrailingLambdaCandidatesByParameterType(info, arityFilteredCandidates)
                val expectedTypeFilteredCandidates =
                    reduceCandidatesByExpectedReturnType(info, syntaxFilteredCandidates)
                val callSite = info.callSite as? CfirFunctionCall
                if (callSite != null) {
                    reduceArityOnlyErrorCandidates(callSite, expectedTypeFilteredCandidates)?.let {
                        return@reduceCollectedCandidates it
                    }
                }
                reduceFreshTypeVariableReceiverCandidates(expectedTypeFilteredCandidates)?.let {
                    return@reduceCollectedCandidates it
                }
                // 官方对多个合法 enum constructor 不执行普通函数的 most-specific 规约。
                // payload 必须留在各候选分支中按 expected type 检查，否则会提前提交某个
                // owner，并错误丢失泛型/非泛型 constructor 之间的真实歧义。
                if (info.resolutionMode.expectedType == null &&
                    expectedTypeFilteredCandidates.size > 1 &&
                    expectedTypeFilteredCandidates.all { candidate ->
                        candidate.symbol.takeIf { it.isBound }?.cfir is CfirEnumConstructor
                    }
                ) {
                    return@reduceCollectedCandidates expectedTypeFilteredCandidates
                }
                expectedTypeFilteredCandidates.singleOrNull()?.let(::setOf)
                    ?: conflictResolver.chooseMaximallySpecificCandidates(expectedTypeFilteredCandidates)
            },
        )
    }

    /**
     * 在候选适用性规约前排除返回类型已经失效的 overload。
     *
     * 单候选保留函数体根错误，不制造调用级级联；多候选中若仍有合法返回类型，只让合法候选
     * 继续参与规约；若所有候选均失效，则附加结构化诊断并强制进入不可适用分组。
     */
    private fun reduceInvalidReturnTypeOverloads(
        selectedCandidates: Collection<Candidate>,
        overloadCandidates: Collection<Candidate>,
        info: CallInfo,
        collectorApplicability: CandidateApplicability,
    ): InvalidReturnTypeOverloadReduction {
        if (overloadCandidates.size <= 1 || info.callKind != CallKind.Function) {
            return InvalidReturnTypeOverloadReduction(selectedCandidates, collectorApplicability)
        }
        val functions = overloadCandidates.map { candidate ->
            candidate to (candidate.symbol.takeIf { it.isBound }?.cfir as? CfirFunction
                ?: return InvalidReturnTypeOverloadReduction(selectedCandidates, collectorApplicability))
        }
        val candidatesWithResolvedReturnTypes = functions.map { (candidate, function) ->
            candidate to (function.returnTypeRef as? CfirResolvedTypeRef)?.coneType
        }
        if (candidatesWithResolvedReturnTypes.any { (_, returnType) -> returnType == null }) {
            return InvalidReturnTypeOverloadReduction(selectedCandidates, collectorApplicability)
        }
        val validCandidates = candidatesWithResolvedReturnTypes
            .filterTo(linkedSetOf()) { (_, returnType) -> returnType !is ConeErrorType }
            .mapTo(linkedSetOf()) { (candidate, _) -> candidate }
        if (validCandidates.size == overloadCandidates.size) {
            return InvalidReturnTypeOverloadReduction(selectedCandidates, collectorApplicability)
        }
        if (validCandidates.isNotEmpty()) {
            return InvalidReturnTypeOverloadReduction(validCandidates, CandidateApplicability.HIDDEN)
        }

        selectedCandidates.forEach { candidate -> candidate.addDiagnostic(InvalidCallableReturnTypeInOverloadSet) }
        return InvalidReturnTypeOverloadReduction(selectedCandidates, CandidateApplicability.INAPPLICABLE)
    }

    /** 错误返回类型 overload 规约后的候选集合与 collector 适用性。 */
    private data class InvalidReturnTypeOverloadReduction(
        val candidates: Collection<Candidate>,
        val collectorApplicability: CandidateApplicability,
    )

    /**
     * 按仓颉 callable 参数数量形状预筛选 overload 候选。
     *
     * 官方 `FuncDeclsGroupByFixedPositionalArity` 会在最终候选比较前优先保留
     * 能接受当前实参数量的声明。这里同时考虑默认参数和仓颉 Array 变参；若没有
     * 任何候选满足数量范围，则保留原集合，让参数映射阶段产生真实 arity 诊断。
     */
    private fun reduceCandidatesByArgumentCount(
        info: CallInfo,
        candidates: Set<Candidate>,
    ): Set<Candidate> {
        if (candidates.size <= 1 || info.callKind != CallKind.Function) return candidates
        val argumentCount = info.arguments.size
        val matchingCandidates = candidates.filterTo(linkedSetOf()) { candidate ->
            val parameters = candidate.declaredParametersForMapping()
            val declaration = candidate.symbol.takeIf { it.isBound }?.cfir
            if (declaration is CfirVariable) {
                return@filterTo argumentCount == parameters.size
            }

            val variadicParameter = candidate.cangjieVariadicParameterForMapping(parameters)
            val requiredCount = parameters.count { parameter ->
                parameter != variadicParameter && parameter.defaultValue == null
            }
            argumentCount >= requiredCount &&
                    (variadicParameter != null || argumentCount <= parameters.size)
        }
        return matchingCandidates.ifEmpty { candidates }
    }

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

        val valueParameterTypes = when {
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
        val ownerExtend = callableSymbol.getContainingExtend()
            ?.takeIf {
                session.accessibilityChecker.checkExtend(
                    it,
                    CfirAccessContext(
                        useSiteFile = components.file,
                        containingDeclarations = transformer.components.containingDeclarations,
                        kind = CfirAccessKind.EXTEND,
                    ),
                ) is CfirAccessibilityResult.Accessible
            }
            ?: return null
        val actualReceiverType = givenExtensionReceiver?.expression?.coneTypeOrNull
        if (actualReceiverType != null) {
            findExtendDeclarationSubstitution(session, ownerExtend, actualReceiverType)
                ?.substitutedReceiverType
                ?.let { return it }
        }
        return ownerExtend.semanticExtendedType(session)
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
        forwardedDiagnostics: List<ResolutionDiagnostic> = emptyList(),
        callableLookupOutcomes: List<CfirCallableLookupOutcome.Excluded> = emptyList(),
    ): CfirNamedReference {
        val source = reference.source
        val operatorToken = runIf(callInfo.origin == CfirFunctionCallOrigin.Operator) {
            OperatorNameConventions.TOKENS_BY_OPERATOR_NAME[name]
        }
        val argumentTypes = callInfo.arguments.mapNotNull { it.coneTypeOrNull }
        val hasInvalidTypeParameterUpperBoundReceiver =
            explicitReceiver?.coneTypeOrNull?.isTypeParameterWithInvalidDeclaredUpperBounds(session) == true
        val memberLookupDominatingDiagnostic = if (candidates.isEmpty()) {
            explicitReceiver?.receiverErrorRootDiagnosticOrNull()
                ?: forwardedDiagnostics.memberLookupDominatingDiagnostic(callInfo, source)
        } else {
            null
        }
        val functionReferenceTargetDiagnostic = functionReferenceTargetDiagnostic(callInfo, candidates)

        // 根据期望的调用种类生成诊断
        val diagnostic = when {
            hasInvalidTypeParameterUpperBoundReceiver ->
                ConeUnreportedDuplicateDiagnostic(ConeSimpleDiagnostic("type parameter upper bound is already invalid"))

            memberLookupDominatingDiagnostic != null ->
                ConeUnreportedDuplicateDiagnostic(memberLookupDominatingDiagnostic)

            functionReferenceTargetDiagnostic != null -> functionReferenceTargetDiagnostic
            callInfo.isStandaloneGenericFunctionValueSet(candidates) ->
                ConeGenericFunctionReferenceWithoutTypeArgumentsError()

            expectedCallKind != null -> when (expectedCallKind) {
                CallKind.Function,
                CallKind.DelegatingConstructorCall,
                    -> {
                    val hasValueParameters = candidates.any {
                        (it.symbol as? CfirFunctionSymbol<*>)?.valueParameterSymbols?.isNotEmpty() == true
                    }
                    ConeFunctionCallExpectedError(
                        name,
                        hasValueParameters,
                        candidates as Collection<AbstractCallCandidate<*>>
                    )
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
                            val declarationErrorType = (symbol?.takeIf { it.isBound }?.cfir as? CfirVariable)
                                ?.returnTypeRef
                                ?.coneTypeOrNull as? ConeErrorType
                            when {
                                declarationErrorType != null -> ConeUnreportedDuplicateDiagnostic(declarationErrorType.diagnostic)
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
                val enumConstructorOnValueReceiver = explicitReceiver?.let { receiver ->
                    receiver.enumConstructorMemberOnValueReceiver(name)
                }
                when {
                    callableLookupOutcomes.isNotEmpty() && operatorToken == null ->
                        ConeNoMatchingFunctionCallError(name)

                    callableLookupOutcomes.isNotEmpty() ->
                        ConeUnresolvedNameError(
                            name = name,
                            operator = operatorToken,
                            receiverType = explicitReceiver?.coneTypeOrNull,
                            argumentTypes = argumentTypes,
                        )

                    enumConstructorOnValueReceiver != null -> {
                        val receiverType = explicitReceiver.coneTypeOrNull
                            ?.fullyExpandedType(session)
                        if (receiverType == null) {
                            ConeUnresolvedNameError(name, operatorToken, argumentTypes = argumentTypes)
                        } else if (callInfo.callKind == CallKind.Function &&
                            enumConstructorOnValueReceiver.valueParameters.isEmpty()
                        ) {
                            ConeNoMatchingInvokeOperatorError(name, receiverType)
                        } else {
                            ConeNotMemberOfError(
                                memberName = name,
                                kind = if (callInfo.callKind == CallKind.Function) "method" else "member",
                                typeName = receiverType.classIdOrPrimitiveClassId?.shortClassName ?: name,
                            )
                        }
                    }

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

                            actualDeclaration is CfirEnum ->
                                ConeNoMatchingInvokeOperatorError(
                                    actualClassifier.name,
                                    actualClassifier.constructType()
                                )

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
                val dominatedNestedDiagnostics = dominatedNestedAmbiguities(candidates, callInfo.arguments)
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
                    dominatedNestedDiagnostics = dominatedNestedDiagnostics,
                    isErrorArgumentCascade = candidates.isErrorArgumentCascade(callInfo.arguments),
                )
            }

            else -> {
                val candidate = candidates.single()
                val enumConstructorValueReceiverDiagnostic = candidate.enumConstructorValueReceiverDiagnostic()
                val genericTypeInconsistentError = candidate.staticQualifierGenericTypeInconsistentError()
                when {
                    enumConstructorValueReceiverDiagnostic != null -> enumConstructorValueReceiverDiagnostic
                    genericTypeInconsistentError != null -> genericTypeInconsistentError
                    candidate.isGenericFunctionReferenceWithoutTypeArguments() ->
                        ConeGenericFunctionReferenceWithoutTypeArgumentsError()

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

    /**
     * receiver 已经拥有错误类型时，selector 的空候选只是该根错误的级联。
     */
    private fun CfirExpression.receiverErrorRootDiagnosticOrNull(): ConeDiagnostic? {
        val diagnostic = (coneTypeOrNull as? ConeErrorType)?.diagnostic ?: return null
        return diagnostic.unwrapUnreportedDuplicateDiagnostic()
    }

    /** 递归展开不重复上报包装，保留真正的根诊断。 */
    private tailrec fun ConeDiagnostic.unwrapUnreportedDuplicateDiagnostic(): ConeDiagnostic =
        if (this is ConeUnreportedDuplicateDiagnostic) original.unwrapUnreportedDuplicateDiagnostic() else this

    /**
     * 选择同文件中位于访问之后的声明父类型 blocker。
     *
     * 声明已经位于访问之前时，lookup 结果是完整的最终结果，成员缺失仍应正常报告；
     * 只有访问先于声明时，后续声明的主错误才支配当前空候选。
     */
    private fun List<ResolutionDiagnostic>.memberLookupDominatingDiagnostic(
        callInfo: CallInfo,
        accessSource: org.cangnova.cangjie.source.AbstractCjSourceElement?,
    ): ConeDiagnostic? {
        val accessOffset = accessSource?.startOffset
            ?: callInfo.callSite.source?.startOffset
            ?: return null
        return asSequence()
            .filterIsInstance<MemberLookupBlockedByDeclaredSupertype>()
            .firstOrNull { blocker ->
                val declaration = blocker.ownerSymbol.cfir
                val declarationOffset = declaration.source?.startOffset ?: return@firstOrNull false
                blocker.ownerSymbol.getContainingFile() == callInfo.containingFile &&
                        declarationOffset > accessOffset
            }
            ?.rootDiagnostic
    }

    /**
     * 外层调用的全部成功候选都以候选局部 replacement 解释同一个原始实参时，
     * 原始实参上预解析遗留的歧义已被外层结构化歧义支配。
     *
     * 各候选 replacement 可能选择不同符号，因此这里只把原始节点的诊断标记为
     * 不重复上报，绝不提交任一分支的 replacement。
     */
    private fun dominatedNestedAmbiguities(
        outerCandidates: Collection<Candidate>,
        originalArguments: List<CfirExpression>,
    ): Set<ConeDiagnostic> {
        if (outerCandidates.isEmpty()) return emptySet()

        if (outerCandidates.any { !it.isSuccessful }) return emptySet()

        val dominated = linkedSetOf<ConeDiagnostic>()
        for (argument in originalArguments) {
            if (outerCandidates.any { candidate -> candidate.argumentReplacements?.containsKey(argument) != true }) {
                continue
            }
            val nestedCall = argument as? CfirFunctionCall ?: continue
            val reference = nestedCall.calleeReference as? CfirNamedReference ?: continue
            val diagnostic = (reference as? CfirDiagnosticHolder)?.diagnostic as? ConeAmbiguityError ?: continue
            dominated += diagnostic
        }
        return dominated
    }

    /**
     * 多构造候选共享同一个带 nested 根诊断的 error argument 时，构造歧义只是错误恢复级联。
     * 仅接受 `ConeUnreportedDuplicateDiagnostic` 载体，普通独立实参错误不触发该抑制。
     */
    private fun Collection<Candidate>.isErrorArgumentCascade(arguments: List<CfirExpression>): Boolean {
        if (isEmpty() || any { candidate ->
                candidate.symbol.takeIf { it.isBound }?.cfir.let { declaration ->
                    declaration !is CfirConstructor && declaration !is CfirEnumConstructor
                }
            }
        ) {
            return false
        }
        return arguments.any { argument ->
            val errorType = argument.coneTypeOrNull as? ConeErrorType ?: return@any false
            errorType.diagnostic is ConeUnreportedDuplicateDiagnostic
        }
    }

    /** 从表达式 callee reference 中提取已携带的诊断，用于避免 receiver 错误重复上报。 */
    private fun CfirExpression.diagnosticFromCalleeReference(): ConeDiagnostic? =
        ((this as? CfirResolvable)?.calleeReference as? CfirDiagnosticHolder)?.diagnostic

    /**
     * 查找运行时 enum 值接收者上的同名构造器。
     *
     * enum constructor 只属于 enum 类型限定符的静态构造语法；当 receiver 已经是一个
     * enum 值时，官方语义把无参构造器视为不可调用的值，把有参构造器视为非法实例成员。
     * 该判定集中在候选为空的共享恢复路径，避免把 enum sugar 当成普通实例成员成功解析。
     */
    private fun CfirExpression.enumConstructorMemberOnValueReceiver(name: Name): CfirEnumConstructor? {
        if (qualifierScopeOrNull(session, components.scopeSession) != null) return null
        val receiverType = coneTypeOrNull?.fullyExpandedType(session) ?: return null
        val receiverClassId = receiverType.classIdOrPrimitiveClassId ?: return null
        val enumSymbol = session.symbolProvider.getClassLikeSymbolByClassId(receiverClassId)
            ?: return null
        val enumDeclaration = enumSymbol.cfir as? CfirEnum ?: return null
        return enumDeclaration.declarations
            .filterIsInstance<CfirEnumConstructor>()
            .firstOrNull { it.name == name }
    }

    /**
     * 将隐式 `invoke` 探测中的无参 enum constructor 临时访问规范化为 enum 值。
     *
     * 该节点不会作为独立成员访问提交；它只向后续统一的 `invoke` 查找暴露运行时
     * receiver 的真实实例化 enum 类型。普通实例成员候选始终优先，带 payload 的
     * constructor 也不会进入这条恢复路径。
     */
    private fun CfirQualifiedAccessExpression.resolveEnumConstructorAsImplicitInvokeReceiver(
        callee: CfirNamedReference,
        candidates: Collection<Candidate>,
        purpose: NamedValueAccessPurpose,
    ): CfirQualifiedAccessExpression? {
        if (purpose != NamedValueAccessPurpose.ImplicitInvokeReceiver) return null
        if (candidates.any { it.symbol !is CfirEnumConstructorSymbol }) return null

        val receiver = explicitReceiver ?: return null
        val constructor = receiver.enumConstructorMemberOnValueReceiver(callee.name) ?: return null
        if (constructor.valueParameters.isNotEmpty()) return null
        val receiverType = receiver.coneTypeOrNull?.fullyExpandedType(session) ?: return null

        replaceCalleeReference(
            buildResolvedNamedReference {
                source = callee.source
                name = callee.name
                resolvedSymbol = constructor.symbol
            }
        )
        replaceConeTypeOrNull(receiverType)
        return this
    }

    /** 判断运行时 enum receiver 上是否存在同名无参 constructor 值。 */
    fun isNoArgEnumConstructorOnValueReceiver(receiver: CfirExpression?, name: Name): Boolean =
        receiver?.enumConstructorMemberOnValueReceiver(name)?.valueParameters?.isEmpty() == true

    /**
     * 将成功进入 tower 的 enum constructor 候选重新按 receiver 语义分类。
     *
     * 成员 scope 为类型限定访问保留 enum constructor，但同一 scope 也可能被运行时 enum
     * receiver 复用；候选存在不代表实例访问合法。无参构造器在官方 AST 中是不可变值，
     * `value.C()` 因此进入 invoke 失败；有参构造器及普通值访问则属于非法 enum 实例成员。
     */
    private fun Candidate.enumConstructorValueReceiverDiagnostic(): ConeDiagnostic? {
        val constructorSymbol = symbol as? CfirEnumConstructorSymbol ?: return null
        val receiver = callInfo.explicitReceiver ?: return null
        val constructor = receiver.enumConstructorMemberOnValueReceiver(constructorSymbol.name) ?: return null
        val receiverType = receiver.coneTypeOrNull?.fullyExpandedType(session) ?: return null

        return if (callInfo.callKind == CallKind.Function && constructor.valueParameters.isEmpty()) {
            ConeNoMatchingInvokeOperatorError(constructorSymbol.name, receiverType)
        } else {
            ConeNotMemberOfError(
                memberName = constructorSymbol.name,
                kind = if (callInfo.callKind == CallKind.Function) "method" else "member",
                typeName = receiverType.classIdOrPrimitiveClassId?.shortClassName ?: constructorSymbol.name,
            )
        }
    }

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

    /** 泛型函数作为值使用时必须显式提供函数自身的类型实参。 */
    private fun Candidate.isGenericFunctionReferenceWithoutTypeArguments(): Boolean {
        if (!callInfo.isStandaloneFunctionValueAccess() || callInfo.hasExplicitTypeArguments) return false
        val function = symbol.takeIf { it.isBound }?.cfir as? CfirFunction ?: return false
        return function.typeParameters.isNotEmpty()
    }

    /**
     * 真正的独立函数值访问不属于函数调用 callee/implicit-invoke 的探测节点。
     * 后两者必须保留完整函数候选集，继续由 Function call stages 和静态 qualifier 推断处理。
     */
    private fun CallInfo.isStandaloneFunctionValueAccess(): Boolean =
        callKind == CallKind.NamedValueAccess && callSite !is CfirFunctionCall

    /**
     * 返回类型限定符上、可见性过滤前的 static 函数重载集合。
     *
     * 官方语义只对无目标类型的独立命名函数值访问先做 static overload 歧义判断；
     * 本 helper 只决定该无目标类型特例。带目标类型的函数引用由独立 expected-type
     * 规约使用完整 discovery 选择声明并随后报告所选候选错误；显式类型实参、普通调用
     * 以及无目标类型的对象 receiver 仍先执行 accessibility 过滤。候选来源限定为
     * collector 已选中的同一最佳 tower group，并再次校验真实声明为 static 函数，
     * 避免把实例成员或其他 callable kind 引入该集合。
     */
    private fun CallInfo.preVisibilityStaticQualifierOverloadSet(
        discoveredFunctionValueCandidates: Collection<Candidate>,
    ): List<Candidate>? {
        if (!isStandaloneFunctionValueAccess() || resolutionMode.expectedType != null || hasExplicitTypeArguments) {
            return null
        }
        val receiver = explicitReceiver?.unwrapSmartcastExpression() ?: return null
        if (receiver.qualifierScopeOrNull(session, components.scopeSession) == null) return null

        val staticFunctionCandidates = discoveredFunctionValueCandidates.filter { candidate ->
            val function = candidate.symbol.takeIf { it.isBound }?.cfir as? CfirFunction
            function?.status?.isStatic == true
        }
        return staticFunctionCandidates.takeIf { candidates -> candidates.size > 1 }
    }

    /**
     * 无目标类型且没有显式类型实参时，若可访问的函数值候选全部是泛型函数，
     * 独立函数引用无法实例化其类型参数，应直接报告无法推断而不是重载歧义。
     */
    private fun CallInfo.isStandaloneGenericFunctionValueSet(candidates: Collection<Candidate>): Boolean {
        if (!isStandaloneFunctionValueAccess() || resolutionMode.expectedType != null || hasExplicitTypeArguments) {
            return false
        }
        if (candidates.isEmpty()) return false
        return candidates.all { candidate ->
            val function = candidate.symbol.takeIf { it.isBound }?.cfir as? CfirFunction
            function?.typeParameters?.isNotEmpty() == true
        }
    }

    /**
     * `Box.create()` 这类裸泛型类名静态成员调用属于调用推断错误。
     * 当 owner 泛型参数没有显式实参，且完全没有出现在可调用签名中时，
     * 调用上下文不可能为这些参数提供约束。
     * 构造器的 owner 类型参数由构造调用自身推断，不属于 static member qualifier 诊断。
     */
    private fun Candidate.hasUninferableBareStaticGenericQualifier(): Boolean {
        if (callInfo.callSite !is CfirFunctionCall) return false
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
        val expectedType = callInfo.resolutionMode.expectedType
        if (expectedType != null) {
            val returnType = components.returnTypeCalculator.tryCalculateReturnType(callable).coneType
            if (returnType.referencesAnyTypeParameter(ownerTypeParameterSymbols)) return false
        }

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
        val ownerExtend = callableSymbol.getContainingExtend()
            ?.takeIf {
                session.accessibilityChecker.checkExtend(
                    it,
                    CfirAccessContext(
                        useSiteFile = components.file,
                        containingDeclarations = transformer.components.containingDeclarations,
                        kind = CfirAccessKind.EXTEND,
                    ),
                ) is CfirAccessibilityResult.Accessible
            }
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
        when (type) {
            is ConeTypeParameterType -> type.lookupTag.typeParameterSymbol in symbols
            is ConeTypeVariableType -> {
                val original = type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
                original?.typeParameterSymbol in symbols
            }

            else -> false
        }
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
        if (actualDeclaration is CfirEnum) {
            return ResolutionResult(
                info = createClassifierCallInfo(functionCall, classifier, resolutionMode),
                applicability = CandidateApplicability.HIDDEN,
                candidates = emptyList(),
                forwardedDiagnostics = emptyList(),
            )
        }

        val classifierSubstitutor = classifierSubstitutorForCall(functionCall, classifier)
        val explicitTypeArguments = functionCall.typeArguments.mapNotNull { typeArgument ->
            typeArgument.coneTypeOrNull
        }
        val constructedTypeArguments = when {
            functionCall.typeArguments.isNotEmpty() && explicitTypeArguments.size == functionCall.typeArguments.size ->
                explicitTypeArguments

            else -> actualDeclaration.typeParameters.map { it.symbol.constructType() }
        }
        val rawConstructedType = actualClassifier.constructType(constructedTypeArguments)
        val constructedType = classifierSubstitutor.substituteOrSelf(rawConstructedType)
        val constructorScope: CfirScope = if (classifier is CfirTypeAliasSymbol) {
            classifier.cfir.scopeProvider
                .getTypealiasConstructorScope(classifier.cfir, session, components.scopeSession)
        } else {
            val declaredMemberScope = CfirClassUseSiteMemberScope(
                session = session,
                classSymbol = actualClassifier,
                symbolProvider = session.symbolProvider,
                ownerType = constructedType,
                dispatchReceiverType = constructedType,
                scopeKind = CfirClassMemberScopeKind.DECLARATION_SITE,
            )
            CfirClassSubstitutionScope(session, declaredMemberScope, constructedType)
        }
        val constructorSymbols = buildList {
            constructorScope.processDeclaredConstructors(::add)
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
                originScope = constructorScope,
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
                if (transformer.resolutionContext.candidateProcessingMode == CandidateProcessingMode.ARGUMENT_SHAPE) {
                    return@reduceCollectedCandidates currentCandidates
                }
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
        ).reduceCandidatesByLambdaBody(functionCall)
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

        val arrayCandidates = builtinArrayConstructorKinds(functionCall, target, argumentCount).map { kind ->
            candidateFactory.createBuiltinArrayConstructorCandidate(
                callInfo = callInfo,
                kind = kind,
                target = target,
            )
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
                if (transformer.resolutionContext.candidateProcessingMode == CandidateProcessingMode.ARGUMENT_SHAPE) {
                    return@reduceCollectedCandidates currentCandidates
                }
                currentCandidates.singleOrNull()?.let(::setOf)
                    ?: conflictResolver.chooseMaximallySpecificCandidates(currentCandidates)
            },
        )
        return ResolutionResult(
            info = callInfo,
            applicability = applicability,
            candidates = reducedCandidates,
            forwardedDiagnostics = emptyList(),
        ).reduceCandidatesByLambdaBody(functionCall)
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

        // 零实参是空指针构造，其余一律按指针转换形状建候选。
        // 实参数量是否合法由 CfirMapArguments 在参数映射阶段唯一判定，
        // 这里不再预挂参数数量诊断，避免与映射阶段产出互相冲突的调用级诊断。
        val kind = when (callInfo.arguments.size) {
            0 -> BuiltinPointerConstructorKind.EMPTY
            else -> BuiltinPointerConstructorKind.CONVERT_POINTER
        }
        val candidates = listOf(
            candidateFactory.createBuiltinPointerConstructorCandidate(
                callInfo = callInfo,
                kind = kind,
                target = target,
            ),
        )
        val (reducedCandidates, applicability) = reduceCollectedCandidates(
            candidates = candidates,
            collectorApplicability = CandidateApplicability.HIDDEN,
            isCandidateSuccessful = Candidate::isSuccessful,
            candidateApplicability = Candidate::lowestApplicability,
            fullyProcessCandidate = { candidate ->
                components.resolutionStageRunner.fullyProcessCandidate(candidate, transformer.resolutionContext)
            },
            chooseMostSpecific = { currentCandidates ->
                if (transformer.resolutionContext.candidateProcessingMode == CandidateProcessingMode.ARGUMENT_SHAPE) {
                    return@reduceCollectedCandidates currentCandidates
                }
                currentCandidates.singleOrNull()?.let(::setOf)
                    ?: conflictResolver.chooseMaximallySpecificCandidates(currentCandidates)
            },
        )
        return ResolutionResult(
            info = callInfo,
            applicability = applicability,
            candidates = reducedCandidates,
            forwardedDiagnostics = emptyList(),
        ).reduceCandidatesByLambdaBody(functionCall)
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
                if (transformer.resolutionContext.candidateProcessingMode == CandidateProcessingMode.ARGUMENT_SHAPE) {
                    return@reduceCollectedCandidates currentCandidates
                }
                currentCandidates.singleOrNull()?.let(::setOf)
                    ?: conflictResolver.chooseMaximallySpecificCandidates(currentCandidates)
            },
        )
        return ResolutionResult(
            info = callInfo,
            applicability = applicability,
            candidates = reducedCandidates,
            forwardedDiagnostics = emptyList(),
        ).reduceCandidatesByLambdaBody(functionCall)
    }

    /**
     * 收集 `std.core.composition<T1, T2, T3>` 的 compiler-core 候选。
     *
     * 官方定义的固定形状是
     * `(T1 -> T2, T2 -> T3) -> T1 -> T3`。该声明是 `~>` 解糖的内部目标，
     * 不是用户可见的普通顶层函数；因此以合成声明承载统一的泛型推断、参数映射和
     * lambda 完成流程，而不依赖当前 std.core cjo 是否导出该内部符号。
     */
    private fun collectCompilerCoreCompositionCandidates(
        functionCall: CfirFunctionCall,
        name: Name,
        resolutionMode: ResolutionMode,
    ): ResolutionResult {
        val callInfo = createCompilerCoreIntrinsicCallInfo(functionCall, name, resolutionMode)
        val candidateFactory = CandidateFactory(transformer.resolutionContext, callInfo)
        val candidate = candidateFactory.createCompilerCoreCompositionCandidate(callInfo)
        val (reducedCandidates, applicability) = reduceCollectedCandidates(
            candidates = listOf(candidate),
            collectorApplicability = CandidateApplicability.HIDDEN,
            isCandidateSuccessful = Candidate::isSuccessful,
            candidateApplicability = Candidate::lowestApplicability,
            fullyProcessCandidate = { currentCandidate ->
                components.resolutionStageRunner.fullyProcessCandidate(currentCandidate, transformer.resolutionContext)
            },
            chooseMostSpecific = { currentCandidates -> currentCandidates },
        )
        return ResolutionResult(
            info = callInfo,
            applicability = applicability,
            candidates = reducedCandidates,
            forwardedDiagnostics = emptyList(),
        ).reduceCandidatesByLambdaBody(functionCall)
    }

    /**
     * 根据目标数组种类和实参数量给出内建 Array/VArray 构造候选形状。
     *
     * 普通 Array 区分空数组、collection 构造、init 函数和重复元素；
     * VArray 的单实参命名形式代表重复元素，否则优先按 init 函数处理。
     *
     * 实参数量超出全部内建构造形状的最大形参数量时，仍然只合成 init 函数形状的单一候选，
     * 由 `CfirMapArguments` 在参数映射阶段唯一判定并报告参数数量错误；
     * 本函数只决定候选形状，不产出任何诊断。
     */
    private fun builtinArrayConstructorKinds(
        functionCall: CfirFunctionCall,
        target: BuiltinArrayConstructorTarget,
        argumentCount: Int,
    ): List<BuiltinArrayConstructorKind> {
        if (argumentCount > BUILTIN_ARRAY_CONSTRUCTOR_MAX_ARITY) {
            return listOf(BuiltinArrayConstructorKind.INIT_FUNCTION)
        }
        return when (target) {
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
        return separatorIndex > 0 && Name.identifierIfValid(rawText.substring(0, separatorIndex).trim()) != null
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

    /** 为 compiler-core intrinsic 创建与源码调用共享的 [CallInfo]。 */
    private fun createCompilerCoreIntrinsicCallInfo(
        functionCall: CfirFunctionCall,
        name: Name,
        resolutionMode: ResolutionMode,
    ): CallInfo = CallInfo(
        callSite = functionCall,
        callKind = CallKind.Function,
        name = name,
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
     * 判断显式 receiver 裸名访问是否需要延迟到外层成员调用共同完成。
     *
     * 无参 enum constructor 可写成 `T1.a16(arg)`，其中 enum owner 的类型实参要由外层成员
     * 调用实参反推，不能先按独立 receiver 完成。但类型参数、class-like qualifier 和导入包
     * qualifier 也具有同样的裸名语法，必须保留 `ReceiverResolution` 语义进入正常 qualifier
     * 解析；否则 `U.foo()` 这类泛型静态访问会在左侧 `U` 处退化为普通未解析名称。
     */
    fun isContextDependentBareEnumConstructorReceiverCandidate(expression: CfirExpression): Boolean {
        val access = expression as? CfirQualifiedAccessExpression ?: return false
        if (access is CfirFunctionCall) return false
        if (access.explicitReceiver != null) return false
        if (access.typeArguments.isNotEmpty()) return false
        val callee = access.calleeReference as? CfirNamedReference ?: return false
        val name = callee.name

        if (components.file.resolveImportedPackageQualifier(name, session) != null) return false
        if (towerResolver.findTypeParameters(name).isNotEmpty()) return false
        if (towerResolver.findClassifiers(name)
                .any { it.isValidClassifierExpression(isUsedAsReceiver = true) }
        ) return false
        return true
    }

    /**
     * 查找可能被当前调用语法当成构造调用目标的 classifier。
     *
     * 有显式 receiver 时在 qualifier 的静态 scope 中查找；裸名调用统一走 tower 中已经
     * 安装的文件、包、显式 import 与默认 import scope。
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
    ): CfirClassLikeSymbol<*>? = qualifierClassifierCandidates(receiver, name)
        .firstOrNull()
        ?.symbol as? CfirClassLikeSymbol<*>

    /** 通过统一 type candidate collector 查找 qualifier scope 中的 classifier。 */
    private fun qualifierClassifierCandidates(
        receiver: CfirExpression,
        name: Name,
    ): List<CfirTypeCandidateCollector.TypeCandidate> {
        val unwrappedReceiver = receiver.unwrapSmartcastExpression()
        val scopes = sequenceOf(
            unwrappedReceiver.importedPackageQualifierScopeOrNull(components.file, session),
            unwrappedReceiver.qualifierScopeOrNull(session, components.scopeSession),
        ).filterNotNull().toList()
        return CfirTypeCandidateCollector(
            session = session,
            context = CfirAccessContext(
                useSiteFile = components.file,
                containingDeclarations = components.containingDeclarations,
                receiverType = unwrappedReceiver.resolvedType,
                qualifierSymbol = unwrappedReceiver.resolvedQualifierClassifier(session),
                kind = CfirAccessKind.TYPE,
            ),
        ).firstVisibleScopeCandidates(scopes, name)
    }

    /** 返回构造目标 classifier 在当前查找位置携带的 use-site substitutor。 */
    private fun classifierSubstitutorForCall(
        access: CfirQualifiedAccessExpression,
        classifier: CfirClassLikeSymbol<*>,
    ): ConeSubstitutor {
        val explicitReceiver =
            access.explicitReceiver ?: return towerResolver.findClassifierSubstitutor(classifier.name, classifier)
        return qualifierClassifierCandidates(explicitReceiver, classifier.name)
            .firstOrNull { it.symbol == classifier }
            ?.substitutor
            ?: ConeSubstitutor.Empty
    }

    /**
     * 保留候选收集阶段算出的适用性与诊断，只替换 enum 值访问可消费的候选集合。
     */
    private fun ResolutionResult.withCandidates(candidates: Collection<Candidate>): ResolutionResult =
        copy(candidates = candidates)

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
        /** 名字已发现但未创建 Candidate 的结构化 callable 查找结果。 */
        val callableLookupOutcomes: List<CfirCallableLookupOutcome.Excluded> = emptyList(),
    ) {
        /** 当前名称是否已经在某个 tower group 形成 callable 排除截止面。 */
        val hasExcludedCallableLookup: Boolean
            get() = callableLookupOutcomes.isNotEmpty()
    }

    /**
     * 对已规约候选集合继续执行 lambda body 重载缩减，并同步候选集合整体适用性。
     *
     * 普通 tower 候选、class constructor fallback 和内建 constructor 都会先各自完成
     * stage 规约；若候选仍只能由 lambda body 目标类型区分，就必须进入同一个
     * overload-by-lambda owner，而不能只让普通 tower 路径拥有该能力。
     */
    private fun ResolutionResult.reduceCandidatesByLambdaBody(
        callSite: CfirQualifiedAccessExpression,
    ): ResolutionResult {
        if (transformer.resolutionContext.candidateProcessingMode == CandidateProcessingMode.ARGUMENT_SHAPE) {
            return this
        }
        if (!components.context.shouldReduceOverloadByLambdaCandidates()) return this

        val candidateSet = candidates.toSet()
        val reducedCandidates = overloadByLambdaBodyResolver.reduceCandidates(callSite, candidateSet)
        if (reducedCandidates == candidateSet) return this

        return copy(
            applicability = reducedCandidates.normalizedReductionApplicability(applicability),
            candidates = reducedCandidates,
        )
    }

    /** 由候选集合当前诊断状态重新计算整体规约适用性。 */
    private fun Collection<Candidate>.normalizedReductionApplicability(
        fallback: CandidateApplicability,
    ): CandidateApplicability =
        maxOfOrNull { candidate ->
            normalizeReductionApplicability(
                isSuccessful = candidate.isSuccessful,
                applicability = candidate.lowestApplicability,
            )
        } ?: fallback
}

/**
 * 内建 `Array<T>` / `VArray<T, $N>` 构造形状中最大的形参数量。
 *
 * 官方 `ArrayExpr` 最多接受 `(size, initElement)` 或 `(size, repeat!: T)` 两个形参；
 * 实参数量超过该值时只保留单一候选形状，参数数量诊断由 `CfirMapArguments` 唯一产出。
 */
private const val BUILTIN_ARRAY_CONSTRUCTOR_MAX_ARITY = 2

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
    /** 按 symbol 与完整 lookup provenance 共同去重保存所有结构候选。 */
    private val allCandidatesMap = mutableMapOf<CfirCandidateLookupIdentity, Candidate>()

    /** 记录候选后继续执行普通 collector 的适用性处理。 */
    override fun consumeCandidate(
        group: CfirTowerGroup,
        candidate: Candidate,
        context: ResolutionContext
    ): CandidateApplicability {
        allCandidatesMap.getOrPut(candidate.lookupIdentity()) { candidate }
        return super.consumeCandidate(group, candidate, context)
    }

    /** 收集全部候选时永不在当前 tower group 提前停止。 */
    override fun shouldStopAtTheGroup(group: CfirTowerGroup): Boolean = false

    /** 返回按符号去重后的全部候选集合。 */
    val allCandidates: Collection<Candidate>
        get() = allCandidatesMap.values
}
