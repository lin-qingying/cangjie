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
import org.cangnova.cangjie.cfir.calls.qualifierScopeOrNull
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
import org.cangnova.cangjie.cfir.resolve.calls.CandidateProcessingMode
import org.cangnova.cangjie.cfir.resolve.calls.applySpawnExpectedFutureType
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.futureTypeOrNull
import org.cangnova.cangjie.cfir.resolve.calls.synthesizeSpawnType
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessAnalyzer
import org.cangnova.cangjie.cfir.resolve.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.cfir.resolve.providers.classifyDeclaredSupertype
import org.cangnova.cangjie.cfir.resolve.providers.constructorDependencyTypeOrNull
import org.cangnova.cangjie.cfir.resolve.providers.scopeTraversalTypeOrNull
import org.cangnova.cangjie.cfir.resolve.transformers.CfirSpecificTypeResolverTransformer
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirPCLAInferenceSession
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirTowerDataMode
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.resultType
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForLambdaParameterType
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeVariableForPostponedAtom
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.AbstractCandidate
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInArguments
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.source.*
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker

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
    /**
     * effect handler 解析过程中可恢复的 handler 上下文。
     *
     * 当前只需要记录 command/resume 之间传递的命令结果类型。
     */
    private data class EffectHandlerContext(
        /** 当前 handler 期望 resume 返回的 command result 类型。 */
        val commandResultType: ConeCangJieType,
    )

    /** overload-by-lambda 试跑中数组元素逐项解析的结果。 */
    private data class ArrayLiteralCandidateTrialResult(
        val arrayLiteral: CfirArrayLiteral,
        val stoppedOnFailure: Boolean,
    )

    /** 多重赋值中按源码顺序发现的首个结构或分量类型不兼容。 */
    private data class MultipleAssignmentTypeMismatch(
        val expectedType: ConeCangJieType,
        val actualType: ConeCangJieType,
    )

    /**
     * 构造器委托调用的名义目标。
     *
     * [declaration] 提供构造器声明集合，[constructedType] 保留当前继承边上的完整类型实参，
     * 后续必须基于该具体类型生成 substitution override，不能再退化为原始声明签名。
     */
    private data class DelegatingConstructorTarget(
        val declaration: CfirClassLikeDeclaration,
        val constructedType: ConeCangJieType,
    )

    /** 当前会话的内建类型集合。 */
    private val builtinTypes get() = session.builtinTypes
    /** 表达式中显式类型引用的专用解析器。 */
    private val specificTypeResolverTransformer = CfirSpecificTypeResolverTransformer(session)
    /** 当前 body resolve 组件中的调用解析器。 */
    private val callResolver get() = components.callResolver
    /** `quote` 表达式解糖后的 token 流类型。 */
    private val stdAstTokensClassId = ClassId(FqName("std.ast"), Name.identifier("Tokens"))
    /** 标准库 `Option.Some` 模式构造器名称。 */
    private val optionSomeConstructorName = Name.identifier("Some")
    /** 标准库 `Option.None` 模式构造器名称。 */
    private val optionNoneConstructorName = Name.identifier("None")
    /** PCLA 中为 `match` Option 模式创建元素类型变量时使用的稳定调试名前缀。 */
    private var optionPatternElementTypeVariableIndex: Int = 0
    /** 嵌套 effect handler 上下文栈。 */
    private val effectHandlerStack = ArrayDeque<EffectHandlerContext>()
    // optional-chain 内部的 `?` 节点承担 Kotlin FIR checked safe-call subject 的角色。
    /** 当前嵌套 optional-chain 解析深度。 */
    private var optionalChainResolveDepth: Int = 0

    /** 构造表达式解析阶段使用的错误类型。 */
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

    /**
     * 通用表达式解析入口。
     *
     * 特殊表达式由具体 override 处理；没有专用类型计算的表达式会得到 inference error 类型。
     */
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

    /** 解析 wrapped expression，并把内部表达式类型提升到 wrapper。 */
    override fun transformWrappedExpression(
        wrappedExpression: CfirWrappedExpression,
        data: ResolutionMode,
    ): CfirExpression {
        wrappedExpression.transformChildren(transformer, data)
        wrappedExpression.replaceConeTypeOrNull(wrappedExpression.expression.coneTypeOrNull)
        components.dataFlowAnalyzer.exitWrappedExpression(wrappedExpression)
        return wrappedExpression
    }

    /** 解析 optional 表达式；optional-chain 内部会临时使用 Option<T> 的元素类型。 */
    override fun transformOptionalExpression(
        optionalExpression: CfirOptionalExpression,
        data: ResolutionMode,
    ): CfirExpression {
        optionalExpression.transformChildren(transformer, data)
        val expressionType = optionalExpression.expression.coneTypeOrNull
        // 链内 selector 必须在 Option<T> 的 T 上解析；外层 chain 节点再统一恢复 Option<result>。
        val resultType = if (optionalChainResolveDepth > 0) {
            expressionType?.optionElementType ?: expressionType
        } else {
            expressionType
        }
        optionalExpression.replaceConeTypeOrNull(resultType)
        return optionalExpression
    }

    /** 解析 optional-chain 表达式，并把 selector 结果重新提升为 Option<result>。 */
    override fun transformOptionalChainExpression(
        optionalChainExpression: CfirOptionalChainExpression,
        data: ResolutionMode,
    ): CfirExpression {
        components.dataFlowAnalyzer.enterOptionalChain(optionalChainExpression)
        optionalChainResolveDepth++
        try {
            optionalChainExpression.transformChildren(transformer, data)
        } finally {
            optionalChainResolveDepth--
        }

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

    /** 解析 `this` receiver 表达式的绑定符号和结果类型。 */
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

    /** 解析 `super` receiver 的显式或隐式 super type。 */
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

        val owner = context.containingClassLikeDeclaration
        val receiverType = when {
            owner == null -> errorType("`super` is only allowed inside class-like declarations")
            resolvedSuperTypeRef is CfirResolvedTypeRef -> resolveExplicitSuperReceiverType(owner, resolvedSuperTypeRef)
            else -> resolveImplicitSuperReceiverType(owner)
        }

        superReceiverExpression.replaceConeTypeOrNull(receiverType)
        return superReceiverExpression
    }

    /** 解析字面量表达式，并按 expected type 对 ideal 类型做收敛。 */
    override fun transformLiteralExpression(
        literalExpression: CfirLiteralExpression,
        data: ResolutionMode,
    ): CfirExpression {
        val synthesized = synthesizeLiteralType(literalExpression.kind)
        val expectedType = data.expectedTypeOrNull
        val resolvedType = when {
            expectedType?.isRune == true &&
                    literalExpression.isSingleRuneStringLiteral() &&
                    (context.variableBeingInitialized != null ||
                            context.fieldBeingInitialized != null ||
                            context.isInsideAssignmentRhs) -> ConePrimitiveType.RUNE
            expectedType != null -> IdealTypeResolver.resolveIfIdeal(synthesized, expectedType)
            else -> synthesized
        }
        literalExpression.replaceConeTypeOrNull(resolvedType)
        recordAssignmentRhsLiteralMismatch(literalExpression, resolvedType)
        components.dataFlowAnalyzer.exitLiteralExpression(literalExpression)
        return literalExpression
    }

    /**
     * 在字面量 owner 仍持有失效前实际类型时完成 assignment RHS 的目标类型结论。
     *
     * 官方 `Check(ctx, lTy, lit)` 对直接 Bool/Int/Float/Rune 使用
     * `CANNOT_CONVERT_LITERAL`；String 保留有效根类型并由赋值层追加 wrapper，Unit
     * 虽保留 Unit 值类型但根检查失败后不允许 wrapper。
     */
    private fun recordAssignmentRhsLiteralMismatch(
        literalExpression: CfirLiteralExpression,
        actualType: ConeCangJieType,
    ) {
        val expectedType = context.assignmentRhsExpectedTypeFor(literalExpression) ?: return
        val integerRangeMismatch = literalExpression.isOutOfAssignmentIntegerRange(expectedType)
        if (!integerRangeMismatch &&
            AbstractTypeChecker.isSubtypeOf(session.typeContext, actualType, expectedType) == true
        ) return

        val (primaryDiagnostic, rootValidity) = when (literalExpression.kind) {
            CfirLiteralKind.BOOLEAN ->
                CfirAssignmentTypeMismatchPrimaryDiagnostic.CannotConvertLiteral("boolean") to
                        CfirAssignmentRhsRootValidity.INVALID_AFTER_MISMATCH

            CfirLiteralKind.INT ->
                CfirAssignmentTypeMismatchPrimaryDiagnostic.CannotConvertLiteral("integer") to
                        CfirAssignmentRhsRootValidity.INVALID_AFTER_MISMATCH

            CfirLiteralKind.FLOAT ->
                CfirAssignmentTypeMismatchPrimaryDiagnostic.CannotConvertLiteral("floating-point") to
                        CfirAssignmentRhsRootValidity.INVALID_AFTER_MISMATCH

            CfirLiteralKind.RUNE ->
                CfirAssignmentTypeMismatchPrimaryDiagnostic.CannotConvertLiteral("character") to
                        CfirAssignmentRhsRootValidity.INVALID_AFTER_MISMATCH

            CfirLiteralKind.STRING ->
                CfirAssignmentTypeMismatchPrimaryDiagnostic.TypeMismatch to
                        CfirAssignmentRhsRootValidity.VALID_AFTER_MISMATCH

            CfirLiteralKind.UNIT ->
                CfirAssignmentTypeMismatchPrimaryDiagnostic.TypeMismatch to
                        CfirAssignmentRhsRootValidity.INVALID_AFTER_MISMATCH
        }
        context.recordAssignmentRhsExpectedTypeMismatch(
            expression = literalExpression,
            actualType = actualType,
            primaryDiagnostic = primaryDiagnostic,
            rhsRootValidity = rootValidity,
        )
    }

    /**
     * 判断无显式后缀的正整数字面量是否超出赋值目标 primitive/VArray 元素范围。
     *
     * 目标类型可能是窄元素 VArray；不能只比较 IdealInt 归一化后的 cone 类型，否则
     * `1000` 会先被解析成目标类型而丢失官方 `CANNOT_CONVERT_LITERAL` 语义。
     */
    private fun CfirLiteralExpression.isOutOfAssignmentIntegerRange(
        expectedType: ConeCangJieType,
    ): Boolean {
        if (kind != CfirLiteralKind.INT) return false
        val parsed = CfirIntConstantEvalUtils.parseIntLiteral(this) ?: return false
        if (parsed.explicitSuffix != null) return false
        val targetType = expectedType.fullyExpandedType().arrayLiteralElementType ?: expectedType
        val range = CfirIntConstantEvalUtils.rangeForPositiveLiteralTargetType(targetType) ?: return false
        return !range.contains(parsed.value)
    }

    /** 根据字面量种类合成初始类型。 */
    private fun synthesizeLiteralType(kind: CfirLiteralKind): ConeCangJieType = when (kind) {
        CfirLiteralKind.INT     -> ConePrimitiveType.IDEAL_INT
        CfirLiteralKind.FLOAT   -> ConePrimitiveType.IDEAL_FLOAT
        CfirLiteralKind.BOOLEAN -> builtinTypes.boolType
        CfirLiteralKind.RUNE    -> ConePrimitiveType.RUNE
        CfirLiteralKind.STRING  -> stdlibStringType()
        CfirLiteralKind.UNIT    -> builtinTypes.unitType
    }

    // ── Named Access ─────────────────────────────────────────────────────────

    /** 解析命名访问表达式。 */
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

    /** 解析限定访问表达式。 */
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

    /** 限定访问解析的内部入口，可区分 receiver 解析和 getClass receiver 场景。 */
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
                    qualifiedAccessExpression.replaceInitializerReferenceIfNeeded()?.let {
                        return@whileAnalysing it
                    }
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
                                    transformedCallee.replaceInitializerReferenceIfNeeded()?.let {
                                        return@whileAnalysing it
                                    }
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
            recordAssignmentRhsTypeMismatchIfNeeded(
                expression = qualifiedAccessExpression,
                actualType = resolvedExpression.coneTypeOrNull,
            )
            resolvedExpression
        }

    // ── Function Call ─────────────────────────────────────────────────────────

    /** 解析普通函数调用表达式。 */
    override fun transformFunctionCall(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirExpression =
        transformFunctionCallInternal(functionCall, data, CallResolutionMode.REGULAR)

    /** 解析 `++` / `--` 表达式，结果类型固定为 Unit。 */
    override fun transformIncrementDecrementExpression(
        incrementDecrementExpression: CfirIncrementDecrementExpression,
        data: ResolutionMode,
    ): CfirExpression =
        whileAnalysing(session, incrementDecrementExpression) {
            incrementDecrementExpression.transformAnnotations(transformer, data)
            incrementDecrementExpression.transformExpression(transformer, ResolutionMode.ContextIndependent)

            // 仓颉 `++` / `--` 不是可重载调用；合法表达式的结果类型固定为 Unit。
            incrementDecrementExpression.replaceConeTypeOrNull(builtinTypes.unitType)
            incrementDecrementExpression
        }

    /**
     * 函数调用解析的统一入口。
     *
     * 负责处理构造器委托、mock intrinsic、显式 receiver、实参、内建操作符、
     * 普通调用候选选择、隐式 invoke 和最终调用补全。
     */
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
            if (
                functionCall.origin == CfirFunctionCallOrigin.Operator &&
                calleeReference is CfirNamedReferenceImpl &&
                functionCall.coneTypeOrNull?.let { it !is ConeErrorType } == true
            ) {
                return@whileAnalysing functionCall
            }
            if (calleeReference is CfirNamedReferenceWithCandidate) {
                if (functionCall.coneTypeOrNull == null) {
                    storeTypeFromCallee(functionCall)
                }
                return@whileAnalysing functionCall
            }
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

                components.dataFlowAnalyzer.exitCallExplicitReceiver()
                if (withResolvedExplicitReceiver.hasErrorExplicitReceiver()) {
                    components.dataFlowAnalyzer.exitCallArguments()
                    withResolvedExplicitReceiver
                } else {
                    if (withResolvedExplicitReceiver.commitNeedNamedArgumentShapeFailure(data)) {
                        components.dataFlowAnalyzer.exitCallArguments()
                        return@whileAnalysing completeFunctionCall(
                            withResolvedExplicitReceiver,
                            data,
                            skipEvenPartialCompletion = false,
                        )
                    }

                    val argumentResolutionMode = withResolvedExplicitReceiver.builtinExponentiationArgumentResolutionMode()
                        ?: ResolutionMode.ContextDependent
                    val transformedArgumentList: CfirArgumentList = context.withCallArgumentResolution {
                        withResolvedExplicitReceiver.argumentList.transform(transformer, argumentResolutionMode)
                    }
                    withResolvedExplicitReceiver.replaceArgumentList(transformedArgumentList)
                    components.dataFlowAnalyzer.exitCallArguments()
                    withResolvedExplicitReceiver
                }
            } else {
                functionCall
            }

            tryResolveBuiltinOperatorCall(withTransformedArguments, data)?.let { builtinOperatorCall ->
                recordAssignmentRhsTypeMismatchIfNeeded(
                    expression = builtinOperatorCall,
                    actualType = builtinOperatorCall.coneTypeOrNull,
                )
                return@whileAnalysing builtinOperatorCall
            }

            if (!choosingOptionForAugmentedAssignment) {
                tryResolveFreshValueParameterInvokeCall(withTransformedArguments, data)?.let { freshInvokeCall ->
                    return@whileAnalysing completeFunctionCall(
                        freshInvokeCall,
                        data,
                        skipEvenPartialCompletion = false,
                    )
                }
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

    /**
     * 显式 receiver 已经是错误类型时，当前调用的参数只会制造级联诊断。
     */
    private fun CfirFunctionCall.hasErrorExplicitReceiver(): Boolean =
        explicitReceiver?.coneTypeOrNull is ConeErrorType

    /**
     * 在实参值解析前提交确定的“必须使用命名实参”绑定失败。
     *
     * probe 复用完整调用入口，因此普通函数、enum constructor 与 class constructor 共享同一套
     * tower 和 fallback 规则；隔离副本保证未命中时不会把候选、错误引用或约束系统写回原调用。
     * 只有最终最佳候选集合中的每个候选都仅包含 [NeedNamedArgument] 时才提交，避免 shape 阶段
     * 缺少实参类型证据时提前锁定其他 overload 失败。
     */
    private fun CfirFunctionCall.commitNeedNamedArgumentShapeFailure(
        resolutionMode: ResolutionMode,
    ): Boolean {
        val probe = buildFunctionCallCopy(this) {}
        val resolvedProbe = resolutionContext.withCandidateProcessingMode(CandidateProcessingMode.ARGUMENT_SHAPE) {
            callResolver.resolveCallAndSelectCandidate(probe, resolutionMode)
        }
        val shapeFailures = resolvedProbe.needNamedArgumentShapeFailuresOrNull() ?: return false

        /*
         * 官方 PreCheck 会在调用值检查前解析显式类型引用。命名实参形态已经使实参值无效时，
         * 仍需保留该子树中独立的 type-ref 诊断，但绝不能进入表达式 body resolve。
         * SpecificTypeResolver 是纯类型引用树 transformer，正好提供这条阶段边界。
         */
        val typeResolutionConfiguration = currentTypeResolutionConfiguration()
        shapeFailures.forEach { failure ->
            failure.argument.transformSingle(specificTypeResolverTransformer, typeResolutionConfiguration)
        }

        replaceCalleeReference(resolvedProbe.calleeReference)
        replaceDispatchReceiver(resolvedProbe.dispatchReceiver)
        replaceConeTypeOrNull(resolvedProbe.coneTypeOrNull)
        return true
    }

    /**
     * 返回 shape probe 最终最佳候选上的命名实参失败；任一候选含其他失败原因时拒绝提前提交。
     */
    private fun CfirFunctionCall.needNamedArgumentShapeFailuresOrNull(): List<NeedNamedArgument>? {
        val diagnostic = (calleeReference as? CfirDiagnosticHolder)?.diagnostic ?: return null
        val candidates = (diagnostic as? ConeDiagnosticWithCandidates)?.candidates
            ?: return null
        if (candidates.isEmpty()) return null

        val failures = mutableListOf<NeedNamedArgument>()
        for (candidate in candidates) {
            val callCandidate = candidate as? AbstractCallCandidate<*> ?: return null
            if (callCandidate.errors.isNotEmpty() || callCandidate.diagnostics.isEmpty()) return null
            for (candidateDiagnostic in callCandidate.diagnostics) {
                failures += candidateDiagnostic as? NeedNamedArgument ?: return null
            }
        }
        return failures
    }

    /**
     * 解析 PCLA 中 fresh value-parameter 的直接调用。
     *
     * 普通无 receiver 调用查找函数声明；lambda 参数 `g` 的类型尚未固定为函数类型时，
     * 不能等普通查找失败后才补语义，否则 `g(0)` 的函数形状约束无法进入同一轮 body 推断。
     */
    private fun tryResolveFreshValueParameterInvokeCall(
        originalCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirFunctionCall? {
        if (originalCall.explicitReceiver != null) return null
        if (components.context.inferenceSession !is CfirPCLAInferenceSession) return null
        val originalCallee = originalCall.calleeReference as? CfirNamedReference ?: return null
        if (originalCallee.name == OperatorNameConventions.INVOKE) return null

        val resolvedAccess = callResolver.resolveNamedValueAccessAndSelectCandidate(
            qualifiedAccess = buildNamedAccessExpression {
                source = originalCall.source
                calleeReference = buildNamedReference {
                    source = originalCallee.source
                    name = originalCallee.name
                }
                typeArguments.addAll(originalCall.typeArguments)
            },
            isUsedAsReceiver = true,
            isUsedAsGetClassReceiver = false,
            callSite = originalCall,
            resolutionMode = data,
        ) as? CfirQualifiedAccessExpression ?: return null

        val receiverType = resolvedAccess.variableTypeFromResolvedReferenceOrNull() as? ConeTypeVariableType
            ?: return null
        if (receiverType.typeConstructor.originalTypeParameter != null) return null
        resolvedAccess.replaceConeTypeOrNull(receiverType)
        resolvedAccess.applyFreshFunctionInvokeShape(originalCall.argumentList.arguments) ?: return null

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
            origin = originalCall.origin
        }

        return callResolver.resolveCallAndSelectCandidate(invokeCall, data)
            .takeUnless { (it.calleeReference as? CfirDiagnosticHolder)?.diagnostic is ConeUnresolvedNameError }
    }

    /** 对已经选中候选的函数调用执行补全，并维护 DFA 调用边界。 */
    private fun completeFunctionCall(
        callForCompletion: CfirFunctionCall,
        data: ResolutionMode,
        skipEvenPartialCompletion: Boolean,
    ): CfirExpression {
        components.dataFlowAnalyzer.enterFunctionCall(callForCompletion)
        val result = components.callCompleter.completeCall(
            callForCompletion,
            data,
            skipEvenPartialCompletion = skipEvenPartialCompletion,
        )
        components.dataFlowAnalyzer.exitFunctionCall(result, data.forceFullCompletion)
        return result
    }

    /** 尝试直接解析内建操作符调用，避免进入普通 overload resolution。 */
    private fun tryResolveBuiltinOperatorCall(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirFunctionCall? {
        if (functionCall.origin != CfirFunctionCallOrigin.Operator) return null
        val callee = functionCall.calleeReference as? CfirNamedReference ?: return null
        val explicitReceiver = functionCall.explicitReceiver ?: return null
        functionCall.tryResolveBuiltinOperatorWithFreshLambdaOperands(callee.name, data.expectedTypeOrNull)
            ?.let { return it }
        functionCall.tryResolveOperatorWithOnlyFreshLambdaOperands(callee.name)?.let { return it }
        val receiverType = explicitReceiver.stableBuiltinOperatorOperandTypeOrNull() ?: return null
        val argumentTypes = functionCall.argumentList.arguments.map { argument ->
            argument.stableBuiltinOperatorOperandTypeOrNull() ?: return null
        }
        val propagatedArgumentError = argumentTypes.firstNotNullOfOrNull { argumentType ->
            argumentType.propagatedErrorTypeOrNull()
        }
        if (
            callee.name == OperatorNameConventions.EXPONENTIATION &&
            propagatedArgumentError != null &&
            BuiltinPrimitiveOperators.isBuiltinPrimitiveOperand(receiverType)
        ) {
            return functionCall.invalidExponentiationOperatorOrNull(callee, receiverType, argumentTypes)
        }
        (receiverType.propagatedErrorTypeOrNull() ?: propagatedArgumentError)?.let { propagatedErrorType ->
            functionCall.replaceConeTypeOrNull(propagatedErrorType)
            return functionCall
        }
        if (functionCall.isInvalidFloatExponentiationCall(receiverType, argumentTypes)) {
            return functionCall.invalidExponentiationOperatorOrNull(callee, receiverType, argumentTypes)
        }
        val builtinMatch = CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
            callee.name,
            receiverType,
            argumentTypes,
        ) ?: return functionCall.invalidExponentiationOperatorOrNull(callee, receiverType, argumentTypes)
        val returnType = if (callee.name == OperatorNameConventions.EXPONENTIATION) {
            builtinMatch.returnType
        } else {
            data.expectedTypeOrNull?.let { expectedType ->
                IdealTypeResolver.resolveIfIdeal(builtinMatch.returnType, expectedType)
            } ?: builtinMatch.returnType
        }
        functionCall.replaceConeTypeOrNull(returnType)
        return functionCall
    }

    /**
     * 返回可供 primitive operator 表消费的稳定操作数类型。
     *
     * 泛型调用作为 operator 操作数时，调用仍处于 partial completion，结果 fresh variable
     * 尚未写回表达式节点；但它可能已经被调用自身实参约束为唯一 primitive lower/equality。
     * 这里只读取候选约束系统的结构化结果，不固定变量、不修改子调用 AST，也不消费 outer
     * expected-type 约束，因此不会把上下文相关的泛型调用提前完成。
     */
    private fun CfirExpression.stableBuiltinOperatorOperandTypeOrNull(): ConeCangJieType? {
        coneTypeOrNull?.let { type ->
            if (type is ConeErrorType) return type
            if (BuiltinPrimitiveOperators.isBuiltinPrimitiveOperand(type)) return type
        }

        val call = this as? CfirFunctionCall ?: return null
        val candidate = (call.calleeReference as? CfirNamedReferenceWithCandidate)?.candidate ?: return null
        val returnType = candidate.substitutedReturnType()
        val returnVariable = returnType as? ConeTypeVariableType ?: return null
        val constraints = candidate.system.currentStorage()
            .notFixedTypeVariables[returnVariable.typeConstructor]
            ?.constraints
            ?: return null
        val stableTypes = constraints
            .asSequence()
            .filter { constraint ->
                constraint.kind == org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind.LOWER ||
                        constraint.kind == org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind.EQUALITY
            }
            .mapNotNull { constraint -> constraint.type as? ConeCangJieType }
            .map { type -> IdealTypeResolver.resolveIfIdeal(type) }
            .filter(BuiltinPrimitiveOperators::isBuiltinPrimitiveOperand)
            .distinct()
            .toList()
        return stableTypes.singleOrNull()
    }

    /**
     * 无上下文 lambda 中 operator 的所有操作数都只是 placeholder 时，仍要给表达式本身一个类型。
     *
     * 这里仅写入由语法确定的约束：逻辑运算直接约束为 Bool；其余同构 primitive 运算把
     * 所有 operand 与结果 fresh type 连接起来，等待外层 expected type、字面量或调用实参继续定型。
     */
    private fun CfirFunctionCall.tryResolveOperatorWithOnlyFreshLambdaOperands(operatorName: Name): CfirFunctionCall? {
        val operandExpressions = listOfNotNull(explicitReceiver) + argumentList.arguments
        if (operandExpressions.isEmpty()) return null
        val operandTypes = operandExpressions.map { expression ->
            expression.lambdaPrimitiveOperandTypeOrNull() ?: return null
        }
        if (operandTypes.any { it.operatorInferenceTypeVariableConstructorOrNull() == null }) return null

        when {
            operatorName.isBooleanResultOperator() -> {
                linkFreshOperatorOperandsTo(operandTypes.first(), operandTypes.drop(1))
                replaceConeTypeOrNull(ConePrimitiveType(PrimitiveTypeKind.BOOLEAN))
            }

            operatorName.isHomogeneousResultOperator() -> {
                val resultType = operandTypes.first()
                linkFreshOperatorOperandsTo(resultType, operandTypes.drop(1))
                replaceConeTypeOrNull(resultType)
            }

            else -> return null
        }
        return this
    }

    /**
     * 用官方 primitive operator 签名为无上下文 lambda 参数 placeholder 注入约束。
     *
     * 完整 primitive 运算仍由 [tryResolveBuiltinOperatorCall] 的普通路径处理；这里仅覆盖
     * PCLA 中操作数尚含 fresh lambda type variable 的场景。若已知操作数无法筛出唯一
     * 内建签名，则保持未解析状态，交给后续 completion 报告 lambda 参数无法推断。
     */
    private fun CfirFunctionCall.tryResolveBuiltinOperatorWithFreshLambdaOperands(
        operatorName: Name,
        expectedReturnType: ConeCangJieType?,
    ): CfirFunctionCall? {
        val signature = inferUniqueBuiltinPrimitiveOperatorSignature(
            operatorName = operatorName,
            expectedReturnType = expectedReturnType,
            receiverExpression = explicitReceiver ?: return null,
            argumentExpressions = argumentList.arguments,
        ) ?: return null
        applyBuiltinPrimitiveOperatorSignature(signature)
        replaceConeTypeOrNull(ConePrimitiveType(signature.returnKind))
        return this
    }

    /**
     * 从 primitive 内建签名表中筛选唯一可用签名。
     */
    private fun inferUniqueBuiltinPrimitiveOperatorSignature(
        operatorName: Name,
        expectedReturnType: ConeCangJieType?,
        receiverExpression: CfirExpression,
        argumentExpressions: List<CfirExpression>,
    ): BuiltinPrimitiveOperatorSignature? {
        if (components.context.inferenceSession !is CfirPCLAInferenceSession) return null

        val operandExpressions = listOf(receiverExpression) + argumentExpressions
        val operandTypes = operandExpressions.map { it.lambdaPrimitiveOperandTypeOrNull() ?: return null }
        val expectedReturnKind = expectedReturnType
            ?.takeUnless { it.isUnit }
            ?.let { BuiltinPrimitiveOperators.primitiveOperandKind(it) }
        val freshConstructors = operandTypes.map { it.operatorInferenceTypeVariableConstructorOrNull() }
        if (freshConstructors.all { it == null }) return null
        // 全部操作数都是待推断类型变量时，只有外层 expected return type 能提供 primitive 签名证据。
        if (freshConstructors.all { it != null } && expectedReturnKind == null) return null

        val signatures = BuiltinPrimitiveOperators.signaturesForOperator(operatorName, argumentExpressions.size)
        return signatures
            .filter { signature ->
                if (expectedReturnKind != null && signature.returnKind != expectedReturnKind) return@filter false

                val expectedKinds = listOf(signature.receiverKind) + signature.parameterKinds
                operandTypes.indices.all { index ->
                    val type = operandTypes[index]
                    type.operatorInferenceTypeVariableConstructorOrNull() != null ||
                            type.matchesPrimitiveOperatorExpectedKind(expectedKinds[index])
                } && sameFreshOperandsHaveSameExpectedKind(freshConstructors, expectedKinds)
            }
            .singleOrNull()
    }

    /**
     * 同一个 fresh lambda 参数在同一个运算中出现多次时，签名的对应 operand 类型必须一致。
     */
    private fun sameFreshOperandsHaveSameExpectedKind(
        freshConstructors: List<TypeConstructorMarker?>,
        expectedKinds: List<PrimitiveTypeKind>,
    ): Boolean {
        val expectedByConstructor = linkedMapOf<TypeConstructorMarker, PrimitiveTypeKind>()
        for ((constructor, expectedKind) in freshConstructors.zip(expectedKinds)) {
            constructor ?: continue
            val previous = expectedByConstructor.putIfAbsent(constructor, expectedKind)
            if (previous != null && previous != expectedKind) return false
        }
        return true
    }

    /**
     * 将唯一 primitive 签名写回 operand 类型和 PCLA 约束系统。
     */
    private fun CfirFunctionCall.applyBuiltinPrimitiveOperatorSignature(
        signature: BuiltinPrimitiveOperatorSignature,
    ) {
        val operandExpressions = listOfNotNull(explicitReceiver) + argumentList.arguments
        val expectedTypes = (listOf(signature.receiverKind) + signature.parameterKinds).map(::ConePrimitiveType)
        for ((expression, expectedType) in operandExpressions.zip(expectedTypes)) {
            expression.applyPrimitiveOperatorExpectedType(expectedType)
        }
    }

    /**
     * 对 fresh lambda operand 加入 subtype 约束；对 ideal literal operand 则按签名目标类型落地。
     */
    private fun CfirExpression.applyPrimitiveOperatorExpectedType(expectedType: ConePrimitiveType) {
        val currentType = lambdaPrimitiveOperandTypeOrNull() ?: return
        if (currentType.operatorInferenceTypeVariableConstructorOrNull() != null) {
            components.context.inferenceSession.addSubtypeConstraintIfCompatible(currentType, expectedType)
            return
        }

        val approximatedType = IdealTypeResolver.resolveIfIdeal(currentType, expectedType)
        if (approximatedType != currentType) {
            replaceConeTypeOrNull(approximatedType)
        }
    }

    /**
     * 判断类型是否为无上下文 lambda 参数 placeholder 对应的 fresh type variable。
     */
    private fun ConeCangJieType.freshLambdaTypeVariableConstructorOrNull(): TypeConstructorMarker? =
        (this as? ConeTypeVariableType)
            ?.typeConstructor
            ?.takeIf { it.originalTypeParameter == null }

    /**
     * operator 语法可反推当前候选约束系统中的类型变量。
     *
     * 无上下文 lambda placeholder 和外层泛型候选变量都需要参与；后者对应
     * `f<T>({ x => x + 1 })` 这类通过 lambda body 反推 `T` 的场景。
     */
    private fun ConeCangJieType.operatorInferenceTypeVariableConstructorOrNull(): TypeConstructorMarker? =
        (this as? ConeTypeVariableType)?.typeConstructor

    /** 在 PCLA 里用双向 subtype 约束表达 operator operand 与结果的同构关系。 */
    private fun linkFreshOperatorOperandsTo(resultType: ConeCangJieType, operandTypes: List<ConeCangJieType>) {
        for (operandType in operandTypes) {
            components.context.inferenceSession.addSubtypeConstraintIfCompatible(operandType, resultType)
            components.context.inferenceSession.addSubtypeConstraintIfCompatible(resultType, operandType)
        }
    }

    /** 比较/相等 operator：结果由语法确定为 Bool，operand 类型仍等待后续证据。 */
    private fun Name.isBooleanResultOperator(): Boolean =
        this == OperatorNameConventions.EQUALS ||
                this == OperatorNameConventions.NOT_EQUALS ||
                this == OperatorNameConventions.COMPARE_LT ||
                this == OperatorNameConventions.COMPARE_LTEQ ||
                this == OperatorNameConventions.COMPARE_GT ||
                this == OperatorNameConventions.COMPARE_GTEQ

    /** 同构结果 operator：结果类型与 operand 类型相同。 */
    private fun Name.isHomogeneousResultOperator(): Boolean =
        this == OperatorNameConventions.UNARY_MINUS ||
                this == OperatorNameConventions.UNARY_PLUS ||
                this == OperatorNameConventions.PLUS ||
                this == OperatorNameConventions.MINUS ||
                this == OperatorNameConventions.TIMES ||
                this == OperatorNameConventions.DIV ||
                this == OperatorNameConventions.REM ||
                this == OperatorNameConventions.EXPONENTIATION ||
                this == OperatorNameConventions.AND ||
                this == OperatorNameConventions.OR ||
                this == OperatorNameConventions.XOR ||
                this == OperatorNameConventions.LEFT_SHIFT ||
                this == OperatorNameConventions.RIGHT_SHIFT

    /**
     * 读取 operand 类型；当表达式是 lambda 参数引用但表达式节点尚未写入类型时，
     * 从参数声明的 placeholder type ref 恢复类型并同步到表达式。
     */
    private fun CfirExpression.lambdaPrimitiveOperandTypeOrNull(): ConeCangJieType? {
        coneTypeOrNull?.let { return it }
        val parameterType = lambdaValueParameterTypeOrNull() ?: return null
        replaceConeTypeOrNull(parameterType)
        return parameterType
    }

    /**
     * 从 lambda 参数引用中取得声明侧 placeholder 类型。
     */
    private fun CfirExpression.lambdaValueParameterTypeOrNull(): ConeCangJieType? {
        val access = this as? CfirQualifiedAccessExpression ?: return null
        val symbol = when (val reference = access.calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            is CfirNamedReferenceWithCandidate -> reference.candidateSymbol
            else -> null
        } as? CfirValueParameterSymbol ?: return null
        return symbol.cfir.returnTypeRef.coneTypeOrNull
    }

    /**
     * 判断已知 operand 是否可按官方 primitive operator 规则视为目标 primitive 种类。
     */
    private fun ConeCangJieType.matchesPrimitiveOperatorExpectedKind(expectedKind: PrimitiveTypeKind): Boolean {
        val normalizedType = IdealTypeResolver.resolveIfIdeal(this)
        val actualKind = BuiltinPrimitiveOperators.primitiveOperandKind(normalizedType) ?: return false
        return actualKind == expectedKind || actualKind == PrimitiveTypeKind.NOTHING
    }

    /** 为内建幂运算选择实参解析模式。 */
    private fun CfirFunctionCall.builtinExponentiationArgumentResolutionMode(): ResolutionMode? {
        if (origin != CfirFunctionCallOrigin.Operator) return null
        val callee = calleeReference as? CfirNamedReference ?: return null
        if (callee.name != OperatorNameConventions.EXPONENTIATION) return null
        val expectedKinds = BuiltinPrimitiveOperators.exponentiationArgumentExpectedKinds(explicitReceiver?.coneTypeOrNull)
        if (expectedKinds.isEmpty()) return null
        val argument = argumentList.arguments.singleOrNull() ?: return null
        if (argument.looksLikeConstructorCall()) {
            return ResolutionMode.ContextIndependent
        }
        if (expectedKinds.size == 1) {
            val expectedKind = expectedKinds.single()
            if (
                expectedKind == PrimitiveTypeKind.UINT64 &&
                CfirIntConstantEvalUtils.parseSignedIntExpression(argument)?.value?.let { it < BigInteger.ZERO } == true
            ) {
                return ResolutionMode.ContextIndependent
            }
            return withExpectedType(ConePrimitiveType(expectedKind))
        }

        val literalKind = argument.exponentiationLiteralKindOrNull()
        if (literalKind == null) {
            return if (PrimitiveTypeKind.FLOAT64 in expectedKinds) {
                withExpectedType(ConePrimitiveType.FLOAT64)
            } else {
                null
            }
        }
        val expectedKind = when {
            literalKind == CfirLiteralKind.INT && PrimitiveTypeKind.INT64 in expectedKinds -> PrimitiveTypeKind.INT64
            literalKind == CfirLiteralKind.FLOAT && PrimitiveTypeKind.FLOAT64 in expectedKinds -> PrimitiveTypeKind.FLOAT64
            else -> return null
        }
        return withExpectedType(ConePrimitiveType(expectedKind))
    }

    /** 判断表达式形态是否像无参类型构造调用。 */
    private fun CfirExpression.looksLikeConstructorCall(): Boolean {
        val call = this as? CfirFunctionCall ?: return false
        if (call.explicitReceiver != null || call.argumentList.arguments.isNotEmpty()) return false
        val callee = call.calleeReference as? CfirNamedReference ?: return false
        return callee.name.asString().firstOrNull()?.isUpperCase() == true
    }

    /** 提取幂运算实参中的字面量种类，支持一元正负包装。 */
    private fun CfirExpression.exponentiationLiteralKindOrNull(): CfirLiteralKind? {
        (this as? CfirLiteralExpression)?.let { return it.kind }
        val call = this as? CfirFunctionCall ?: return null
        if (call.argumentList.arguments.isNotEmpty()) return null
        val reference = call.calleeReference as? CfirNamedReference ?: return null
        if (
            reference.name != OperatorNameConventions.UNARY_MINUS &&
            reference.name != OperatorNameConventions.UNARY_PLUS
        ) {
            return null
        }
        return (call.explicitReceiver as? CfirLiteralExpression)?.kind
    }

    /** 幂运算非法时构造 unresolved operator 诊断并回写调用。 */
    private fun CfirFunctionCall.invalidExponentiationOperatorOrNull(
        callee: CfirNamedReference,
        receiverType: ConeCangJieType,
        argumentTypes: List<ConeCangJieType>,
    ): CfirFunctionCall? {
        if (callee.name != OperatorNameConventions.EXPONENTIATION) return null
        if (argumentTypes.size != 1) return null
        if (!BuiltinPrimitiveOperators.isBuiltinPrimitiveOperand(receiverType)) return null

        val operatorToken = OperatorNameConventions.TOKENS_BY_OPERATOR_NAME[callee.name] ?: return null
        val diagnostic = ConeUnresolvedNameError(
            callee.name,
            operatorToken,
            explicitReceiver?.invalidBinaryOperatorOperandType(receiverType) ?: receiverType,
            listOf(argumentList.arguments.single().invalidBinaryOperatorOperandType(argumentTypes.single())),
        )
        replaceCalleeReference(
            buildErrorNamedReference {
                source = callee.source
                name = callee.name
                this.diagnostic = diagnostic
            }
        )
        replaceConeTypeOrNull(ConeErrorType(diagnostic))
        argumentList.arguments.single().dropExponentiationExpectedTypeMismatch()
        return this
    }

    /** 删除幂运算实参上由 expected type 造成的派生 type mismatch。 */
    private fun CfirExpression.dropExponentiationExpectedTypeMismatch() {
        val errorType = coneTypeOrNull as? ConeErrorType ?: return
        val typeMismatch = errorType.diagnostic as? ConeTypeMismatchError ?: return
        replaceConeTypeOrNull(typeMismatch.actualType)
    }

    /** 判断是否为 Float64 ** Float64 这类仓颉不允许的幂运算组合。 */
    private fun CfirFunctionCall.isInvalidFloatExponentiationCall(
        receiverType: ConeCangJieType,
        argumentTypes: List<ConeCangJieType>,
    ): Boolean {
        val callee = calleeReference as? CfirNamedReference ?: return false
        if (callee.name != OperatorNameConventions.EXPONENTIATION) return false
        val receiverKind = (receiverType as? ConePrimitiveType)?.kind
        if (receiverKind != PrimitiveTypeKind.FLOAT64 && receiverKind != PrimitiveTypeKind.IDEAL_FLOAT) return false
        if ((argumentTypes.singleOrNull() as? ConePrimitiveType)?.kind != PrimitiveTypeKind.FLOAT64) return false

        val argumentCall = argumentList.arguments.singleOrNull() as? CfirFunctionCall ?: return false
        val argumentFunction = when (val argumentReference = argumentCall.calleeReference) {
            is CfirNamedReferenceWithCandidateBase -> argumentReference.candidateSymbol
            is CfirResolvedNamedReference -> argumentReference.resolvedSymbol
            else -> return false
        }.takeIf { it.isBound }?.cfir as? CfirFunction ?: return false
        val parameterType = argumentFunction.valueParameters.singleOrNull()?.returnTypeRef?.coneTypeOrNull as? ConePrimitiveType
            ?: return false
        return parameterType.kind != PrimitiveTypeKind.INT64
    }

    /** 把 ideal 字面量类型规范化成二元操作诊断中应展示的具体操作数类型。 */
    private fun CfirExpression.invalidBinaryOperatorOperandType(type: ConeCangJieType): ConeCangJieType {
        val primitive = type as? ConePrimitiveType ?: return type
        return when (primitive.kind) {
            PrimitiveTypeKind.IDEAL_INT -> {
                val value = CfirIntConstantEvalUtils.parseSignedIntExpression(this)?.value
                if (value != null && value > BigInteger.valueOf(Long.MAX_VALUE)) {
                    ConePrimitiveType.UINT64
                } else {
                    ConePrimitiveType.INT64
                }
            }

            PrimitiveTypeKind.IDEAL_FLOAT -> ConePrimitiveType.FLOAT64
            else -> type
        }
    }

    /** 解析 `this(...)` / `super(...)` 构造器委托调用。 */
    private fun transformConstructorDelegationCall(
        functionCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirFunctionCall {
        when (functionCall.calleeReference) {
            is CfirResolvedNamedReference,
            is CfirErrorNamedReference,
            -> {
                functionCall.replaceConeTypeOrNull(builtinTypes.unitType)
                return functionCall
            }
            else -> Unit
        }

        functionCall.transformAnnotations(transformer, data)
        resolveAccessTypeArguments(functionCall)

        val containingConstructor = context.containers.lastOrNull() as? CfirConstructor
        val containingClass = context.containers
            .dropLast(1)
            .lastOrNull { declaration -> declaration is CfirClassLikeDeclaration } as? CfirClassLikeDeclaration
        val target = when (functionCall.origin) {
            CfirFunctionCallOrigin.ConstructorDelegationThis -> containingClass?.constructorDelegationSelfTargetOrNull()
            CfirFunctionCallOrigin.ConstructorDelegationSuper -> containingClass?.directConcreteSuperConstructorTargetOrNull()
            else -> null
        }

        if (
            containingConstructor == null ||
            target == null
        ) {
            components.dataFlowAnalyzer.enterCallArguments(functionCall, functionCall.argumentList.arguments)
            functionCall.replaceArgumentList(
                functionCall.argumentList.transform(transformer, ResolutionMode.ContextIndependent)
            )
            components.dataFlowAnalyzer.exitCallArguments()
            functionCall.replaceConeTypeOrNull(functionCall.invalidConstructorDelegationCallType())
            return functionCall
        }

        val isConstructorHeaderCall = context.towerDataMode == CfirTowerDataMode.CONSTRUCTOR_HEADER
        if (!isConstructorHeaderCall) {
            components.dataFlowAnalyzer.enterCallArguments(functionCall, functionCall.argumentList.arguments)
            functionCall.replaceArgumentList(
                functionCall.argumentList.transform(transformer, ResolutionMode.ContextDependent)
            )
            components.dataFlowAnalyzer.exitCallArguments()
        } else {
            components.dataFlowAnalyzer.enterCallArguments(functionCall, functionCall.argumentList.arguments)
            context.forDelegatedConstructorCallChildren(containingConstructor, containingClass, components) {
                functionCall.replaceArgumentList(
                    functionCall.argumentList.transform(transformer, ResolutionMode.ContextDependent)
                )
            }
            components.dataFlowAnalyzer.exitCallArguments()
        }

        /*
         * 参数映射与候选诊断必须始终由调用解析器统一产生。非首语句的 delegation
         * 虽然还会由 declaration checker 报告位置非法，但不能因此跳过调用解析，
         * 否则参数数量、无构造器和歧义会退回到 checker 的第二套实现。
         */
        val resolveCall = {
            callResolver.resolveDelegatingConstructorCallAndSelectCandidate(
                functionCall,
                target.declaration,
                target.constructedType,
                ResolutionMode.ContextIndependent,
            )
        }
        val resolvedCall = if (isConstructorHeaderCall) {
            context.forDelegatedConstructorCallResolution(resolveCall)
        } else {
            resolveCall()
        }

        components.dataFlowAnalyzer.enterFunctionCall(resolvedCall)
        val result = components.callCompleter.completeCall(resolvedCall, ResolutionMode.ContextIndependent)
        components.dataFlowAnalyzer.exitFunctionCall(result, data.forceFullCompletion)
        /*
         * 构造器 header 中的委托调用表达式只承担初始化顺序语义，公开类型必须是 Unit；
         * 非 header 的 `super(...)` / `this(...)` 则仍是后续成员访问的 nominal receiver。
         * 即使委托调用本身因位置或参数非法而带错误，保留 target.constructedType 也能让
         * `super().a` 继续经过正常成员查找并报告唯一的 NOT_MEMBER_OF，而不是退化成
         * Unit 上的 unresolved/cascade 诊断。
         */
        result.replaceConeTypeOrNull(
            if (isConstructorHeaderCall) builtinTypes.unitType else target.constructedType,
        )
        return result
    }

    /**
     * 构造器外部的 `this(...)` / `super(...)` 已由 checker 报告专用诊断。
     * 解析阶段保留一个不再二次上报的错误类型，避免赋值/成员访问继续产生级联误报。
     */
    private fun CfirFunctionCall.invalidConstructorDelegationCallType(): ConeErrorType =
        ConeErrorType(
            ConeUnreportedDuplicateDiagnostic(
                ConeSimpleDiagnostic(
                    "constructor delegation call is outside constructor",
                    DiagnosticKind.SuperNotAllowed,
                )
            )
        )

    /**
     * 为 `this(...)` 构造当前声明自身的实例化目标。
     *
     * 自身类型参数仍作为类型实参保留，使同类构造器委托和父类构造器委托经过同一 substitution seam。
     */
    private fun CfirClassLikeDeclaration.constructorDelegationSelfTargetOrNull(): DelegatingConstructorTarget? {
        val classSymbol = symbol as? CfirClassLikeSymbol<*> ?: return null
        val typeArguments = typeParameters.map { parameter ->
            ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag())
        }
        return DelegatingConstructorTarget(
            declaration = this,
            constructedType = constructClassLikeType(classSymbol, classSymbol.classId, typeArguments),
        )
    }

    /** 返回 class-like 的第一个直接具体父类型及其声明。 */
    private fun CfirClassLikeDeclaration.directConcreteSuperConstructorTargetOrNull(): DelegatingConstructorTarget? =
        superTypeRefs
            .mapNotNull { superTypeRef -> superTypeRef.toDelegatingConstructorTargetOrNull() }
            .firstOrNull { target -> target.declaration !is CfirInterface }

    /** 从 resolved super typeRef 同时保留完整父类型和对应声明。 */
    private fun CfirTypeRef.toDelegatingConstructorTargetOrNull(): DelegatingConstructorTarget? {
        val constructedType = classifyDeclaredSupertype(session)
            .constructorDependencyTypeOrNull(includeLoopError = false)
            ?: return null
        val declaration = constructedType.toResolvedSuperDeclarationOrNull() ?: return null
        return DelegatingConstructorTarget(declaration, constructedType)
    }

    /** 从 cone type 解析对应 class-like 声明。 */
    private fun ConeCangJieType.toResolvedSuperDeclarationOrNull(): CfirClassLikeDeclaration? {
        val expanded = fullyExpandTypeAliasForConstructorDelegation()
        val classId = when (expanded) {
            is ConePrimitiveType -> expanded.kind.classId
            is ConeClassLikeType -> expanded.classId
            is ConeStructType -> expanded.classId
            is ConeEnumType -> expanded.classId
            is ConeTypeAliasType -> expanded.classId
            else -> null
        } ?: return null

        return session.cfirProvider.getCfirClassifierByFqName(classId)
            ?: session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
    }

    /** 为构造器委托目标解析展开 typealias。 */
    private fun ConeCangJieType.fullyExpandTypeAliasForConstructorDelegation(): ConeCangJieType {
        var current = this
        while (current is ConeTypeAliasType && current.expandedType != null) {
            current = current.expandedType ?: break
        }
        return current
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

    /** 在普通调用失败后尝试把值访问改写为隐式 invoke 调用。 */
    private fun tryResolveImplicitInvokeCall(
        originalCalleeReference: CfirReference,
        originalCall: CfirFunctionCall,
        resolvedCall: CfirFunctionCall,
        data: ResolutionMode,
    ): CfirFunctionCall? {
        val originalCallee = originalCalleeReference as? CfirNamedReference ?: return null
        val diagnostic = (resolvedCall.calleeReference as? CfirDiagnosticHolder)?.diagnostic
        val noArgEnumValueCalledWithArguments = diagnostic.isNoArgEnumValueCalledWithArguments(originalCall)
        val noArgEnumValueOnValueReceiver =
            diagnostic is ConeNotMemberOfError &&
                callResolver.isNoArgEnumConstructorOnValueReceiver(
                    originalCall.explicitReceiver,
                    originalCallee.name,
                )
        val isVArraySizeCall = originalCall.isVArraySizeCall()
        if (
            originalCall.explicitReceiver != null &&
            !noArgEnumValueCalledWithArguments &&
            !noArgEnumValueOnValueReceiver &&
            !isVArraySizeCall
        ) return null

        val shouldPreserveOriginalDiagnostic =
            diagnostic !is ConeUnresolvedNameError &&
                !noArgEnumValueCalledWithArguments &&
                !noArgEnumValueOnValueReceiver
        val canTryImplicitInvoke = when (diagnostic) {
            is ConeUnresolvedNameError -> true
            is ConeNotMemberOfError -> noArgEnumValueOnValueReceiver
            is ConeInapplicableCandidateError ->
                diagnostic.candidateSymbol is CfirEnumConstructorSymbol ||
                        diagnostic.candidateSymbol is CfirVariableSymbol<*>
            is ConeConstraintSystemHasContradiction ->
                diagnostic.candidateSymbol is CfirEnumConstructorSymbol ||
                        diagnostic.candidateSymbol is CfirVariableSymbol<*>
            is ConeAmbiguityError -> !diagnostic.applicability.isSuccess &&
                    diagnostic.candidateSymbols.all { it is CfirEnumConstructorSymbol }
            else -> false
        }
        if (!canTryImplicitInvoke) return null

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
            purpose = NamedValueAccessPurpose.ImplicitInvokeReceiver,
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

        resolvedAccess.applyFreshFunctionInvokeShape(originalCall.argumentList.arguments)

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
            origin = originalCall.origin
        }

        val invokeResult = callResolver.resolveCallAndSelectCandidate(invokeCall, data)
            .takeUnless { (it.calleeReference as? CfirDiagnosticHolder)?.diagnostic is ConeUnresolvedNameError }

        if (invokeResult != null) return invokeResult

        if (shouldPreserveOriginalDiagnostic) return null

        // 接收者自身已携带错误类型时，调用失败只是既有错误的级联，不再生成 callee 未解析诊断。
        val receiverType = resolvedAccess.coneTypeOrNull
        if (receiverType is ConeErrorType) {
            resolvedCall.replaceConeTypeOrNull(receiverType)
            return resolvedCall
        }

        // 变量已解析但类型上没有 invoke 操作符 → 报告专用诊断
        if (receiverType != null && receiverType !is ConeErrorType) {
            val diagnosticSource = if (isVArraySizeCall) {
                originalCall.explicitReceiver?.source ?: originalCallee.source
            } else if (noArgEnumValueOnValueReceiver) {
                // 运行时 enum 值上的非法 constructor 成员只突出 selector；
                // 类型限定的 `Enum.Entry(args)` 仍突出完整 enum value 表达式。
                originalCallee.source
            } else {
                originalCallee.source.enumValueAccessSource(originalCall.explicitReceiver?.source)
            }
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

    /**
     * 无上下文 lambda 参数被直接调用时，把 fresh 参数类型变量约束为函数类型。
     *
     * `g(0)` 这类形态没有显式 receiver，普通函数类型 tower level只有在 `g` 已经是
     * 函数类型时才会生效。官方 `SynLamExpr` 会把该调用语法作为输入约束，这里在
     * 隐式 invoke 改写前补齐 `(argTypes...) -> R` 形状，让后续统一走 function-type invoke。
     */
    private fun CfirQualifiedAccessExpression.applyFreshFunctionInvokeShape(
        arguments: List<CfirExpression>,
    ): ConeFunctionType? {
        val receiverType = coneTypeOrNull ?: variableTypeFromResolvedReferenceOrNull() ?: return null
        if (coneTypeOrNull == null) {
            replaceConeTypeOrNull(receiverType)
        }
        val receiverVariableType = receiverType as? ConeTypeVariableType ?: return null
        if (receiverVariableType.typeConstructor.originalTypeParameter != null) return null
        val pclaSession = components.context.inferenceSession as? CfirPCLAInferenceSession ?: return null

        val parameterTypes = arguments.mapIndexed { index, argument ->
            argument.coneTypeOrNull
                ?.let(IdealTypeResolver::resolveIfIdeal)
                ?: ConeTypeVariableForLambdaParameterType("InvokeParameter$index")
                    .also(pclaSession::registerInferenceVariable)
                    .defaultType
        }
        val returnVariable = ConeTypeVariableForPostponedAtom("InvokeReturn")
            .also(pclaSession::registerInferenceVariable)
        val functionType = ConeFunctionType(
            parameterTypes = parameterTypes,
            returnType = returnVariable.defaultType,
        )
        pclaSession.addSubtypeConstraintIfCompatible(receiverVariableType, functionType)
        replaceConeTypeOrNull(functionType)
        return functionType
    }

    /**
     * 从已解析的变量引用中恢复声明侧类型。
     *
     * 隐式 invoke 的 receiver 不只可能是 lambda 参数，也可能来自局部字段或模式绑定；
     * 它们统一由 [CfirVariableSymbol] 承载，必须走同一声明类型恢复路径。
     */
    private fun CfirQualifiedAccessExpression.variableTypeFromResolvedReferenceOrNull(): ConeCangJieType? {
        val symbol = when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            is CfirNamedReferenceWithCandidate -> reference.candidateSymbol
            else -> null
        } as? CfirVariableSymbol<*> ?: return null
        return symbol.cfir.returnTypeRef.coneTypeOrNull
    }

    /** 判断调用是否是 VArray 的 size 访问。 */
    private fun CfirFunctionCall.isVArraySizeCall(): Boolean {
        val callee = calleeReference as? CfirNamedReference ?: return false
        if (callee.name.asString() != "size") return false
        return explicitReceiver?.coneTypeOrNull?.fullyExpandedType(session) is ConeVArrayType
    }

    /** 构造 enum value 访问诊断使用的合成 source。 */
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

        /** 判断候选是否绑定到无参 enum constructor，用于把调用错误改写成 enum 值的 `invoke` 错误。 */
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

    /** 从函数调用的 callee 写回结果类型。 */
    private fun storeTypeFromCallee(functionCall: CfirFunctionCall) {
        storeTypeFromCallee(functionCall as CfirQualifiedAccessExpression)
    }

    /** 从限定访问的 callee 写回结果类型。 */
    internal fun storeTypeFromCallee(
        qualifiedAccessExpression: CfirQualifiedAccessExpression,
        @Suppress("UNUSED_PARAMETER") isLhsOfAssignment: Boolean = false,
    ) {
        qualifiedAccessExpression.replaceConeTypeOrNull(components.typeFromCallee(qualifiedAccessExpression))
    }

    /** 解析限定访问或调用的显式 receiver。 */
    fun <Q : CfirQualifiedAccessExpression> transformExplicitReceiverOf(qualifiedAccessExpression: Q): Q {
        if (qualifiedAccessExpression.explicitReceiver == null) return qualifiedAccessExpression
        val receiverResolutionMode = qualifiedAccessExpression.explicitReceiverResolutionMode()
        qualifiedAccessExpression.transformExplicitReceiver(
            transformer,
            receiverResolutionMode,
        )
        qualifiedAccessExpression.explicitReceiver?.materializeResolvedReceiverType()
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
        if (explicitReceiver?.let(callResolver::isContextDependentBareEnumConstructorReceiverCandidate) == true) {
            return ResolutionMode.ContextDependent
        }
        if (callee.name != OperatorNameConventions.INVOKE) return ResolutionMode.ReceiverResolution
        return if (explicitReceiver?.requiresOuterInvokeConstraints() == true) {
            ResolutionMode.ContextDependent
        } else {
            ResolutionMode.ReceiverResolution
        }
    }

    /**
     * 判断函数类型 `invoke` 的接收者调用是否确实需要由外层实参继续固定返回类型。
     *
     * 首次解析时尚无候选，先保留一轮外层约束入口；候选形成后，仅返回类型仍含本候选
     * 系统未固定变量的调用继续延迟。无论哪一轮，receiver 调用都会立即暴露当前结构化结果类型。
     */
    private fun CfirExpression.requiresOuterInvokeConstraints(): Boolean = when (this) {
        is CfirFunctionCall -> {
            val candidate = (calleeReference as? CfirNamedReferenceWithCandidate)?.candidate
            if (candidate == null) {
                // 首次进入 nested invoke 时尚未收集内层候选，必须先保留外层约束入口。
                coneTypeOrNull == null
            } else {
                val notFixedTypeVariables = candidate.system.currentStorage().notFixedTypeVariables
                notFixedTypeVariables.isNotEmpty() && candidate.substitutedReturnType().contains { type ->
                    type is ConeTypeVariableType && type.typeConstructor in notFixedTypeVariables
                }
            }
        }

        is CfirWrappedExpression -> expression.requiresOuterInvokeConstraints()
        else -> false
    }

    /**
     * 把 receiver 调用当前已经确定的候选结果类型暴露给外层 tower。
     *
     * PSI 操作符 lowering 会把已解析调用直接复用为下一个 operator call 的 dispatch receiver；
     * 即使失败候选未进入 completion writer，receiver 转换契约也必须在返回前写入候选初始类型。
     * Context-dependent 调用仍保留 fresh variables 和约束系统供外层 `invoke` 继续求解。
     */
    private fun CfirExpression.materializeResolvedReceiverType() {
        when (this) {
            is CfirFunctionCall -> {
                if (coneTypeOrNull != null) return
                val candidate = (calleeReference as? CfirNamedReferenceWithCandidate)?.candidate ?: return
                replaceConeTypeOrNull(components.initialTypeOfCandidate(candidate))
            }

            is CfirWrappedExpression -> {
                expression.materializeResolvedReceiverType()
                if (coneTypeOrNull == null) {
                    replaceConeTypeOrNull(expression.coneTypeOrNull)
                }
            }
        }
    }

    /** 执行命名值访问解析并选择候选。 */
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

    /** 函数调用解析模式。 */
    internal enum class CallResolutionMode {
        /** 普通调用解析模式。 */
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

    /**
     * 解析块表达式并把块类型同步为尾表达式类型。
     *
     * 非尾语句独立解析，尾语句继承外层 expected type；这保证 `if`、`try`、`match`
     * 等把块作为结果表达式的结构能够把上下文类型继续下推到真正产生值的位置。
     */
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
        val resultType = if (lastExpr is CfirExpression) {
            lastExpr.coneTypeOrNull ?: builtinTypes.unitType
        } else {
            builtinTypes.unitType
        }
        recordAssignmentRhsTypeMismatchIfNeeded(block, resultType)
        block.replaceConeTypeOrNull(resultType)
        components.dataFlowAnalyzer.exitBlock(block)
        return block
    }

    // ── Match ─────────────────────────────────────────────────────────────────

    /**
     * 解析 `match` 表达式的 subject、分支模式、guard 与分支体。
     *
     * subject 错误会以未上报重复诊断形式传播到整个表达式；正常路径中分支体会接收
     * 外层 expected type，随后计算穷尽性和分支结果 Join 类型。
     */
    override fun transformMatchExpression(
        matchExpression: CfirMatchExpression,
        data: ResolutionMode,
    ): CfirExpression {
        components.dataFlowAnalyzer.enterMatchExpression(matchExpression)
        matchExpression.subject?.resolveIndependently()
        val subjectType = matchExpression.subject?.coneTypeOrNull
            ?: matchExpression.subject?.lambdaPrimitiveOperandTypeOrNull()
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
        val patternSubjectType = inferOptionSubjectTypeFromFreshSelector(matchExpression, subjectType) ?: subjectType
        val branchTypes = matchExpression.branches.map { branch ->
            resolveBranch(branch, patternSubjectType, branchResolutionMode)
        }

        matchExpression.replaceExhaustiveness(resolveMatchExhaustiveness(matchExpression))
        val resultType = computeMatchResultType(branchTypes, data.expectedTypeOrNull)
        recordAssignmentRhsTypeMismatchIfNeeded(matchExpression, resultType)
        matchExpression.replaceConeTypeOrNull(resultType)
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

    /**
     * 在 PCLA 中，`match (x) { case Some(v) => ... }` 的 selector 可能仍是无上下文
     * lambda 参数 placeholder。标准库 `Some`/`None` 模式本身要求 selector 为
     * `Option<T>`，因此这里先创建共享的 `T` 并把 `x <: Option<T>` 注入当前
     * common system，再让普通模式绑定路径把 `T` 传给 payload binding。
     */
    private fun inferOptionSubjectTypeFromFreshSelector(
        matchExpression: CfirMatchExpression,
        subjectType: ConeCangJieType?,
    ): ConeCangJieType? {
        if (subjectType == null || subjectType.freshLambdaTypeVariableConstructorOrNull() == null) return null
        if (components.context.inferenceSession !is CfirPCLAInferenceSession) return null
        if (matchExpression.branches.none { branch -> branch.pattern.containsStdlibOptionPatternShape() }) return null

        val elementVariable = ConeTypeVariableForLambdaParameterType(
            "OptionPatternElement${optionPatternElementTypeVariableIndex++}",
        )
        components.context.inferenceSession.registerInferenceVariable(elementVariable)
        val optionType = constructNamedType(
            classId = StdlibClassIds.Option,
            typeArguments = listOf(elementVariable.defaultType),
        )
        components.context.inferenceSession.addSubtypeConstraintIfCompatible(subjectType, optionType)
        return optionType
    }

    /** 判断模式语法是否包含标准库 `Option` 构造器形态。 */
    private fun CfirPattern.containsStdlibOptionPatternShape(): Boolean = when (this) {
        is CfirEnumPattern -> isStdlibOptionPatternShape() ||
                arguments.any { argument -> argument.containsStdlibOptionPatternShape() }
        is CfirVarOrEnumPattern -> name == optionNoneConstructorName
        is CfirBindingPattern -> nestedPattern?.containsStdlibOptionPatternShape() == true
        is CfirTuplePattern -> elements.any { element -> element.containsStdlibOptionPatternShape() }
        is CfirOrPattern -> alternatives.any { alternative -> alternative.containsStdlibOptionPatternShape() }
        else -> false
    }

    /** `Some(payload)` / `None` 在语法层对应标准库 Option 模式构造器。 */
    private fun CfirEnumPattern.isStdlibOptionPatternShape(): Boolean {
        val name = constructorNameOrNull() ?: return false
        return when (name) {
            optionSomeConstructorName -> arguments.size == 1
            optionNoneConstructorName -> arguments.isEmpty()
            else -> false
        }
    }

    /**
     * 在独立局部作用域中解析单个 `match` 分支。
     *
     * 分支解析包括延迟模式判定、模式绑定类型写回、绑定符号注册、guard 解析和分支体解析；
     * 返回值是分支体的最终类型，用于外层 `match` 结果类型合成。
     */
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

    /**
     * 将裸名字模式解析为 enum 构造模式或绑定模式。
     *
     * 优先使用 subject expected type 中的 enum 声明消解歧义；没有 expected enum 类型时，
     * 再走当前作用域的普通值解析以识别可见 enum constructor。
     */
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

    /**
     * 按 expected enum 类型查找无载荷 enum constructor，并构造已解析引用。
     *
     * 该路径只接受 payload arity 为 0 的构造项，因为裸名字模式不能携带 enum payload。
     */
    private fun resolveExpectedEnumConstructorReferenceOrNull(
        pattern: CfirVarOrEnumPattern,
        expectedType: ConeCangJieType?,
    ): CfirReference? {
        if (expectedType?.optionElementType != null && pattern.name == optionNoneConstructorName) {
            return buildNamedReference {
                source = pattern.source
                name = pattern.name
            }
        }

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

    /**
     * 为延迟解析后的 enum pattern 计算每个 payload pattern 对应的 expected type。
     *
     * 解析过程会根据 expected enum 类型、constructor 名称和实参个数定位 enum constructor，
     * 并把构造项载荷参数类型按 enum 实例类型完成替换。
     */
    private fun resolveEnumArgumentTypesForDeferredPattern(
        pattern: CfirEnumPattern,
        expectedType: ConeCangJieType?,
    ): List<ConeCangJieType> {
        val optionArgumentTypes = expectedType?.let { resolveStdlibOptionArgumentTypesForDeferredPattern(pattern, it) }
        if (optionArgumentTypes != null) return optionArgumentTypes

        val enumType = expectedType?.expandedPatternEnumType(session) ?: return emptyList()
        val enumDeclaration = session.symbolProvider.getClassLikeSymbolByClassId(enumType.classId)?.cfir as? CfirEnum
            ?: return emptyList()
        val constructorAccess = pattern.constructorReference.enumPatternConstructorAccessOrNull() ?: return emptyList()
        if (!constructorAccess.matchesEnumOwner(enumDeclaration, enumType)) return emptyList()
        val enumConstructor = enumDeclaration.declarations
            .filterIsInstance<CfirEnumConstructor>()
            .firstOrNull { constructor ->
                constructor.name == constructorAccess.constructorName && constructor.payloadArity() == pattern.arguments.size
            }
            ?: return emptyList()

        return enumConstructor.substitutedPayloadParameterTypes(enumDeclaration, enumType)
    }

    /**
     * 延迟模式解析阶段的标准库 `Option<T>` payload 类型投影。
     *
     * 绑定变量写回阶段已经支持 Option，这里补齐的是嵌套 pattern 的 expected type 传递，
     * 例如 `case Some(None)` 中内层 `None` 也必须按 `Option<T>` 模式解析。
     */
    private fun resolveStdlibOptionArgumentTypesForDeferredPattern(
        pattern: CfirEnumPattern,
        expectedType: ConeCangJieType,
    ): List<ConeCangJieType>? {
        val optionArgumentType = expectedType.optionElementType ?: return null
        val constructorAccess = pattern.constructorReference.enumPatternConstructorAccessOrNull()
            ?.takeIf { it.matchesStdlibOptionOwner(expectedType) }
            ?: return null
        return when {
            constructorAccess.constructorName == optionSomeConstructorName &&
                    pattern.arguments.size == 1 -> listOf(optionArgumentType)
            constructorAccess.constructorName == optionNoneConstructorName &&
                    pattern.arguments.isEmpty() -> emptyList()
            constructorAccess.constructorName == optionSomeConstructorName ||
                    constructorAccess.constructorName == optionNoneConstructorName -> emptyList()
            else -> null
        }
    }

    /** 提取 enum pattern 构造器名称。 */
    private fun CfirEnumPattern.constructorNameOrNull(): Name? {
        return constructorReference.enumPatternConstructorAccessOrNull()?.constructorName
    }

    /**
     * 在当前作用域中按普通值访问规则解析裸 enum constructor 引用。
     *
     * 这里复用调用解析器的候选选择结果，并把候选引用统一规整成
     * [CfirResolvedNamedReference]，供后续 enum pattern 节点直接持有。
     */
    private fun resolveEnumConstructorReferenceOrNull(pattern: CfirVarOrEnumPattern): CfirReference? {
        val temporaryAccess = buildNamedAccessExpression {
            source = pattern.source
            calleeReference = buildNamedReference {
                source = pattern.source
                name = pattern.name
            }
        }
        val resolvedAccess = callResolver.resolveNamedValueAccessAndSelectCandidate(
            qualifiedAccess = temporaryAccess,
            isUsedAsReceiver = false,
            isUsedAsGetClassReceiver = false,
            callSite = temporaryAccess,
            resolutionMode = ResolutionMode.ContextIndependent,
            purpose = NamedValueAccessPurpose.PatternConstructorProbe,
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

    /**
     * 合成 `match` 表达式结果类型。
     *
     * 分支错误以未上报重复诊断传播；理想类型会结合外层 expected type 归一化；
     * 当任一分支已等于 expected type 时按官方语义直接采用 expected type，否则计算公共父类型。
     */
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

        inferFreshLambdaMatchJoinType(normalizedBranchTypes)?.let { return it }
        return commonSupertype(normalizedBranchTypes)
    }

    /**
     * PCLA 中的 `match` 分支 Join 需要像官方 `JoinAndMeet` 一样把分支结果反向约束到
     * 无上下文 lambda 参数 placeholder。否则 `{ x => match (...) { case ... => x; case ... => 1 } }`
     * 会把 `x` 与 `Int64` 直接 Join 成 `Any`，后续函数值调用无法再从返回位点收束参数类型。
     */
    private fun inferFreshLambdaMatchJoinType(
        branchTypes: List<ConeCangJieType>,
    ): ConeCangJieType? {
        if (components.context.inferenceSession !is CfirPCLAInferenceSession) return null
        val freshRootTypes = branchTypes.filter { branchType ->
            branchType.freshLambdaTypeVariableConstructorOrNull() != null
        }
        if (freshRootTypes.isEmpty()) return null

        val concreteTypes = branchTypes
            .filterNot { branchType -> branchType.freshLambdaTypeVariableConstructorOrNull() != null }
            .filter { branchType -> branchType != ConePrimitiveType.NOTHING }
        if (concreteTypes.isEmpty()) return null

        val joinedConcreteType = commonSupertype(concreteTypes)
        if (joinedConcreteType is ConeErrorType || joinedConcreteType == ConeAnyType) return null

        for (freshType in freshRootTypes) {
            components.context.inferenceSession.addSubtypeConstraintIfCompatible(freshType, joinedConcreteType)
        }
        return joinedConcreteType
    }

    // ── If ────────────────────────────────────────────────────────────────────

    /**
     * 解析 `if` 表达式的条件与分支，并合成整个表达式类型。
     *
     * 条件中包含 `let` pattern 时会为 then 分支建立局部绑定作用域；
     * 普通条件按 `Bool` expected type 解析。分支类型错误只传播错误类型，不重复上报诊断。
     */
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
        val resultType = IdealTypeResolver.resolveIfIdeal(mergedType, data.expectedTypeOrNull)
        recordAssignmentRhsTypeMismatchIfNeeded(ifExpression, resultType)
        ifExpression.replaceConeTypeOrNull(resultType)
        return ifExpression
    }

    /**
     * 解析独立出现的 `let pattern` 条件表达式。
     *
     * 该入口只完成 initializer、pattern 和布尔结果类型的解析，不向外层作用域注册绑定；
     * 条件控制流入口会在需要时调用 [resolveConditionWithPatternBindings] 完成注册。
     */
    override fun transformLetPatternExpression(
        letPatternExpression: CfirLetPatternExpression,
        data: ResolutionMode,
    ): CfirExpression {
        resolveLetPatternExpression(letPatternExpression, registerBindings = false)
        return letPatternExpression
    }

    /** 判断条件表达式中是否包含需要给分支引入绑定的 `let pattern`。 */
    private fun CfirExpression.containsLetPatternCondition(): Boolean = when (this) {
        is CfirLetPatternExpression -> true
        is CfirBinaryOp -> (kind == CfirBinaryOpKind.AND || kind == CfirBinaryOpKind.OR) &&
                (left.containsLetPatternCondition() || right.containsLetPatternCondition())
        else -> false
    }

    /**
     * 解析条件表达式并注册其中的 pattern binding。
     *
     * `&&` / `||` 条件保持布尔结果类型，同时递归解析左右侧以保证嵌套 pattern 的绑定信息
     * 能进入当前控制流作用域；非 pattern 条件退回普通 `Bool` expected type 解析。
     */
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

    /**
     * 解析 `let pattern` 的 initializer、模式结构和绑定变量类型。
     *
     * 延迟的裸名字模式会在 initializer 类型已知后再决定是 enum pattern 还是 binding pattern；
     * [registerBindings] 控制这些绑定是否写入当前局部作用域。
     */
    private fun resolveLetPatternExpression(
        letPatternExpression: CfirLetPatternExpression,
        registerBindings: Boolean,
    ) {
        letPatternExpression.transformInitializer(transformer, ResolutionMode.ContextIndependent)
        letPatternExpression.transformPattern(transformer, ResolutionMode.ContextIndependent)
        val patternExpectedType = inferPatternExpectedTypeFromFreshInitializer(
            pattern = letPatternExpression.pattern,
            initializerType = letPatternExpression.initializer.coneTypeOrNull,
        ) ?: letPatternExpression.initializer.coneTypeOrNull
        if (letPatternExpression is org.cangnova.cangjie.cfir.expressions.impl.CfirLetPatternExpressionImpl) {
            letPatternExpression.pattern = resolveDeferredMatchPattern(
                pattern = letPatternExpression.pattern,
                expectedType = patternExpectedType,
            )
        }
        resolvePatternBindingTypes(
            pattern = letPatternExpression.pattern,
            expectedType = patternExpectedType,
            typeResolver = specificTypeResolverTransformer,
        )
        if (registerBindings) {
            registerPatternBindings(letPatternExpression.pattern)
        }
        letPatternExpression.replaceConeTypeOrNull(builtinTypes.boolType)
    }

    /**
     * 在 PCLA let-pattern 中根据模式形态补充 fresh initializer 的 expected type。
     *
     * `while (let (Some(x), y) <- (a, b))` 这类条件里，initializer tuple 的元素可能仍是
     * lambda 参数 placeholder。标准库 Option 模式要求对应元素为 `Option<T>`；这里先把
     * 该约束写入 common system，并把 refined tuple expected type 传给后续 pattern 解析。
     */
    private fun inferPatternExpectedTypeFromFreshInitializer(
        pattern: CfirPattern,
        initializerType: ConeCangJieType?,
    ): ConeCangJieType? {
        if (initializerType == null) return null
        if (components.context.inferenceSession !is CfirPCLAInferenceSession) return null
        return inferPatternExpectedTypeFromFreshInitializerInternal(pattern, initializerType)
    }

    /** 递归计算 pattern 对 initializer fresh 类型的 refined expected type。 */
    private fun inferPatternExpectedTypeFromFreshInitializerInternal(
        pattern: CfirPattern,
        initializerType: ConeCangJieType,
    ): ConeCangJieType? {
        if (initializerType.freshLambdaTypeVariableConstructorOrNull() != null &&
            pattern.containsStdlibOptionPatternShape()
        ) {
            val elementVariable = ConeTypeVariableForLambdaParameterType(
                "OptionPatternElement${optionPatternElementTypeVariableIndex++}",
            )
            components.context.inferenceSession.registerInferenceVariable(elementVariable)
            val optionType = constructNamedType(
                classId = StdlibClassIds.Option,
                typeArguments = listOf(elementVariable.defaultType),
            )
            components.context.inferenceSession.addSubtypeConstraintIfCompatible(initializerType, optionType)
            return optionType
        }

        if (pattern is CfirTuplePattern && initializerType is ConeTupleType) {
            var changed = false
            val elementTypes = pattern.elements.mapIndexed { index, elementPattern ->
                val elementType = initializerType.elementTypes.getOrNull(index)
                    ?: return@mapIndexed null
                inferPatternExpectedTypeFromFreshInitializerInternal(elementPattern, elementType)
                    ?.also { changed = true }
                    ?: elementType
            }
            if (changed && elementTypes.all { it != null }) {
                return ConeTupleType(elementTypes.filterNotNull(), initializerType.attributes)
            }
        }

        if (pattern is CfirBindingPattern && pattern.nestedPattern != null) {
            return inferPatternExpectedTypeFromFreshInitializerInternal(pattern.nestedPattern!!, initializerType)
        }

        return null
    }

    /**
     * 解析 `quote` 表达式并合成 libast token 流类型。
     *
     * 官方 `SynQuoteExpr` 使用 desugarExpr 的类型；quote 在官方 DesugarMacro 中解糖为
     * `std.ast.Tokens(...)` 调用，因此 CFIR 在 resolve 阶段统一落到 `std.ast.Tokens`。
     */
    override fun transformQuoteExpression(
        quoteExpression: CfirQuoteExpression,
        data: ResolutionMode,
    ): CfirExpression {
        quoteExpression.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
        quoteExpression.transformInterpolations(transformer, ResolutionMode.ContextIndependent)
        quoteExpression.ensureQuoteExpressionType()
        return quoteExpression
    }

    /**
     * 为 quote 节点补齐 libast 公共类型。
     */
    private fun CfirQuoteExpression.ensureQuoteExpressionType() {
        replaceConeTypeOrNull(constructNamedType(stdAstTokensClassId))
    }

    // ── Return / Throw ────────────────────────────────────────────────────────

    /**
     * 解析 `return` 表达式并把结果表达式按目标 callable 的返回类型检查。
     *
     * 解析完成后 `return` 自身类型固定为 `Nothing`，并通知数据流分析器完成 jump 边。
     */
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

    /**
     * 解析抽象 loop jump 节点。
     *
     * 该入口服务于通用 visitor 分发；具体 `break`、`continue` 节点也会复用同一套类型与 CFG 处理。
     */
    override fun transformLoopJump(
        loopJump: CfirLoopJump,
        data: ResolutionMode,
    ): CfirExpression {
        return transformLoopJumpLike(loopJump, data)
    }

    /** 解析 `break` 表达式，并将其类型固定为 `Nothing`。 */
    override fun transformBreakExpression(
        breakExpression: CfirBreakExpression,
        data: ResolutionMode,
    ): CfirExpression {
        return transformLoopJumpLike(breakExpression, data)
    }

    /** 解析 `continue` 表达式，并将其类型固定为 `Nothing`。 */
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

    /**
     * 解析 `throw` 表达式。
     *
     * 子表达式独立解析，表达式本身类型为 `Nothing`，并在数据流图中关闭异常抛出边。
     */
    override fun transformThrowExpression(
        throwExpression: CfirThrowExpression,
        data: ResolutionMode,
    ): CfirExpression {
        throwExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        throwExpression.replaceConeTypeOrNull(ConePrimitiveType.NOTHING)
        components.dataFlowAnalyzer.exitThrowException(throwExpression)
        return throwExpression
    }

    /**
     * 解析 effect `perform` 表达式。
     *
     * feature 未启用时产生特性禁用错误；启用后要求 operand 类型最终能关联到
     * `Command<T>`，并把 `perform` 的结果类型设为命令结果 `T`。
     */
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

    /**
     * 解析 effect handler 中的 `resume` 表达式。
     *
     * 该入口校验 feature 开关、当前 handler 上下文、`throwing` 异常类型以及缺省 resume
     * 是否允许省略 `with` 值；表达式静态类型保持 `Nothing`。
     */
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

    /**
     * 解析赋值表达式。
     *
     * 下标赋值会解糖到 `set` 操作符解析；普通赋值独立解析左右值并通知数据流分析器记录变量写入。
     * 赋值表达式自身类型固定为 `Unit`。
     */
    override fun transformAssignment(
        assignment: CfirAssignment,
        data: ResolutionMode,
    ): CfirExpression {
        assignment.replaceTypeMismatchOutcome(null)
        val subscriptLValue = assignment.lValue as? CfirSubscriptExpression
        if (subscriptLValue != null) {
            assignment.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
            subscriptLValue.transformReceiver(transformer, ResolutionMode.ReceiverResolution)
            subscriptLValue.transformIndices(transformer, ResolutionMode.ContextIndependent)
            assignment.transformRValue(transformer, ResolutionMode.ContextIndependent)

            /*
             * 复合下标赋值的 rValue 会以同一个 subscript 节点作为 operator receiver，
             * 因而上一步已经按 get 语义解析出元素类型。set 调用只验证回写是否合法，
             * 其 Unit 返回类型不能覆盖左值元素类型，否则后续会把 `a[i] += x` 误判为
             * `Unit <op> T`。这与 Kotlin FIR 对 indexed augmented assignment 分离
             * lhsGetCall 和 setCall 的结构一致。
             */
            val compoundElementType = assignment.compoundSubscriptElementTypeOrNull(subscriptLValue)
            resolveSubscriptSetAssignment(assignment, subscriptLValue, data)
            if (compoundElementType != null && subscriptLValue.coneTypeOrNull !is ConeErrorType) {
                subscriptLValue.replaceConeTypeOrNull(compoundElementType)
            }
            assignment.replaceConeTypeOrNull(builtinTypes.unitType)
            components.dataFlowAnalyzer.exitVariableAssignment(assignment)
            return assignment
        }

        assignment.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
        assignment.transformLValue(transformer, ResolutionMode.ContextIndependent)
        val lValueType = assignment.lValue.coneTypeOrNull?.takeUnless { it is ConeErrorType }
        val rValueMode = lValueType?.let(::withExpectedType) ?: ResolutionMode.ContextIndependent
        val ordinaryExpectedType = lValueType.takeUnless { assignment.lValue is CfirTupleLiteral }
        val assignmentRhsRoot = assignment.rValue
        val typeMismatchOutcome = context.withAssignmentRhs(
            rootExpression = assignmentRhsRoot,
            expectedType = ordinaryExpectedType,
        ) {
            assignment.transformRValue(transformer, rValueMode)
            recordAssignmentRhsTypeMismatchIfNeeded(
                expression = assignmentRhsRoot,
                actualType = assignment.rValue.coneTypeOrNull,
            )
        }
        assignment.rValue.applySingleRuneStringLiteralConversion(lValueType)
        val resolvedRValueType = assignment.rValue.coneTypeOrNull
        assignment.replaceTypeMismatchOutcome(typeMismatchOutcome)
        val multipleAssignmentMismatch = (assignment.lValue as? CfirTupleLiteral)
            ?.let { tupleTarget ->
                resolvedRValueType?.let { actualType ->
                    tupleTarget.firstMultipleAssignmentTypeMismatch(actualType)
                }
            }
        assignment.replaceConeTypeOrNull(
            when {
                multipleAssignmentMismatch != null -> ConeErrorType(
                    ConeUnreportedDuplicateDiagnostic(
                        ConeMismatchedTypesMultipleAssignError(
                            expectedType = multipleAssignmentMismatch.expectedType,
                            actualType = multipleAssignmentMismatch.actualType,
                        ),
                    ),
                    delegatedType = builtinTypes.unitType,
                )

                assignment.lValue is CfirTupleLiteral && resolvedRValueType is ConeErrorType ->
                    resolvedRValueType.propagatedErrorTypeOrNull() ?: resolvedRValueType

                else -> builtinTypes.unitType
            }
        )
        components.dataFlowAnalyzer.recordAssignment(assignment)
        components.dataFlowAnalyzer.exitVariableAssignment(assignment)
        return assignment
    }

    /**
     * 按多重赋值解糖后的目标顺序检查 tuple 结构、元数和分量兼容性。
     *
     * RHS 已在进入该函数前完整完成解析；这里只分类首个失败，不改变求值顺序。
     * 嵌套 tuple 递归保持相同规则，下划线目标不要求分量类型。
     */
    private fun CfirExpression.firstMultipleAssignmentTypeMismatch(
        actualType: ConeCangJieType,
    ): MultipleAssignmentTypeMismatch? {
        if (actualType is ConeErrorType) return null
        if (isMultipleAssignmentDiscardTarget()) return null

        val expectedType = coneTypeOrNull ?: return null
        if (expectedType is ConeErrorType) {
            return MultipleAssignmentTypeMismatch(expectedType, actualType)
        }

        if (this is CfirTupleLiteral) {
            val expandedActualType = actualType.fullyExpandedType()
            val actualElementTypes = (expandedActualType as? ConeTupleType)?.elementTypes
                ?: return MultipleAssignmentTypeMismatch(expectedType, actualType)
            if (elements.size != actualElementTypes.size) {
                return MultipleAssignmentTypeMismatch(expectedType, actualType)
            }
            for (index in elements.indices) {
                elements[index]
                    .firstMultipleAssignmentTypeMismatch(actualElementTypes[index])
                    ?.let { return it }
            }
            return null
        }

        val expandedExpectedType = expectedType.fullyExpandedType()
        val expandedActualType = actualType.fullyExpandedType()
        return if (
            AbstractTypeChecker.isSubtypeOf(
                session.typeContext,
                expandedActualType,
                expandedExpectedType,
            ) == true
        ) {
            null
        } else {
            MultipleAssignmentTypeMismatch(expectedType, actualType)
        }
    }

    /** 判断当前目标是否为多重赋值中的丢弃占位 `_`。 */
    private fun CfirExpression.isMultipleAssignmentDiscardTarget(): Boolean {
        val access = this as? CfirQualifiedAccessExpression ?: return false
        val reference = access.calleeReference as? CfirNamedReference ?: return false
        return reference.name.asString() == "_"
    }

    /**
     * 取得复合下标赋值中已经按 get 语义解析出的元素类型。
     *
     * raw builder 使用 operator call 表示 `a[i] <op>= value`，并把同一个 subscript
     * 节点作为 call receiver；普通 `a[i] = value` 不满足该结构，因此不会误入。
     */
    private fun CfirAssignment.compoundSubscriptElementTypeOrNull(
        subscriptExpression: CfirSubscriptExpression,
    ): ConeCangJieType? {
        val operatorCall = rValue as? CfirFunctionCall ?: return null
        if (operatorCall.origin != CfirFunctionCallOrigin.Operator) return null
        if (operatorCall.source != source) return null
        val operatorReceiver = operatorCall.explicitReceiver as? CfirSubscriptExpression ?: return null
        if (operatorReceiver !== subscriptExpression && operatorReceiver.source != subscriptExpression.source) return null
        return operatorReceiver.coneTypeOrNull?.takeUnless { it is ConeErrorType }
    }

    // ── Tuple / Array / String Literals ──────────────────────────────────────

    /** 解析 tuple 字面量，并按元素的解析后类型构造 [ConeTupleType]。 */
    override fun transformTupleLiteral(
        tupleLiteral: CfirTupleLiteral,
        data: ResolutionMode,
    ): CfirExpression {
        tupleLiteral.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
        val expectedElementTypes = (data.expectedTypeOrNull?.fullyExpandedType() as? ConeTupleType)?.elementTypes
        val elements = tupleLiteral.elements as? MutableList<CfirExpression>
            ?: error("CfirTupleLiteral elements must be mutable during body resolve")
        for (index in elements.indices) {
            val elementMode = expectedElementTypes
                ?.getOrNull(index)
                ?.takeUnless { it is ConeErrorType }
                ?.let(::withExpectedType)
                ?: ResolutionMode.ContextIndependent
            elements[index] = elements[index].transform(transformer, elementMode)
        }
        val elementTypes = tupleLiteral.elements.map {
            it.coneTypeOrNull ?: errorType("unresolved element")
        }
        val resultType = ConeTupleType(elementTypes)
        recordAssignmentRhsTypeMismatchIfNeeded(tupleLiteral, resultType)
        tupleLiteral.replaceConeTypeOrNull(resultType)
        return tupleLiteral
    }

    /**
     * 解析数组字面量并推断 `Array<T>` 或 `VArray<T, N>` 类型。
     *
     * 当外层 expected type 提供元素类型时，会把该类型下推到每个元素并在元素级报告不匹配；
     * 没有 expected type 时通过元素公共父类型推断，无法形成可接受公共类型则产生数组字面量错误。
     */
    override fun transformArrayLiteral(
        arrayLiteral: CfirArrayLiteral,
        data: ResolutionMode,
    ): CfirExpression {
        val expectedType = data.expectedTypeOrNull?.fullyExpandedType()
        val expectedElementType = expectedType?.arrayLiteralElementType

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
            val actualType = arrayLiteral.constructArrayTypeFromElements()
            recordAssignmentRhsTypeMismatchIfNeeded(arrayLiteral, actualType)
            return if (context.assignmentRhsExpectedTypeFor(arrayLiteral) != null && actualType !is ConeErrorType) {
                // assignment frame 负责根诊断；节点保留失效前类型，避免通用错误节点
                // collector 再次报告同一个类型不匹配。
                arrayLiteral.replaceConeTypeOrNull(actualType)
                arrayLiteral
            } else {
                arrayLiteral.asTypeMismatchExpression(
                    expectedType = expectedType,
                    actualType = actualType,
                )
            }
        }

        val elementResolutionMode = expectedElementType?.let(::withExpectedType) ?: ResolutionMode.ContextIndependent
        val resolvedArrayLiteral = if (
            expectedElementType != null &&
            components.context.shouldShortCircuitOverloadByLambdaTargetChecks()
        ) {
            val trialResult = arrayLiteral.transformElementsForOverloadByLambdaCandidateTrial(
                expectedElementType,
                elementResolutionMode,
            )
            if (trialResult.stoppedOnFailure) return trialResult.arrayLiteral
            trialResult.arrayLiteral
        } else {
            arrayLiteral.transformChildren(transformer, elementResolutionMode) as CfirArrayLiteral
        }
        resolvedArrayLiteral.ensureQuoteElementTypes()

        val assignmentRootActualType = resolvedArrayLiteral.actualArrayTypeBeforeExpectedElementFailure(
            expectedType = expectedType,
        )
        if (assignmentRootActualType != null && context.assignmentRhsExpectedTypeFor(arrayLiteral) != null) {
            recordAssignmentRhsTypeMismatchIfNeeded(arrayLiteral, assignmentRootActualType)
            val assignmentExpectedType = context.assignmentRhsExpectedTypeFor(arrayLiteral)
            if (assignmentExpectedType != null &&
                AbstractTypeChecker.isSubtypeOf(
                    session.typeContext,
                    assignmentRootActualType,
                    assignmentExpectedType,
                ) != true
            ) {
                // 外层 assignment 负责该根类型不匹配；不要再制造子元素诊断，
                // 以免掩盖官方根 Check 的结果。
                resolvedArrayLiteral.replaceConeTypeOrNull(assignmentRootActualType)
                return resolvedArrayLiteral
            }
        }

        val arrayLiteralWithElementDiagnostics = if (expectedElementType != null) {
            resolvedArrayLiteral.withElementTypeDiagnostics(expectedElementType)
        } else {
            resolvedArrayLiteral
        }

        /*
         * 数组元素内部已有解析根错误时，数组仍需保留可恢复的目标 Array/VArray 形状，
         * 但其顶层类型必须继续是 error type。否则元素错误被下面的类型收集过滤后，外层
         * constructor/call 会把数组当作正常实参并派生新的候选或类型诊断。
         */
        val nestedElementDiagnostic = arrayLiteralWithElementDiagnostics.elements
            .firstNotNullOfOrNull { element -> element.rootErrorDiagnosticOrNull() }
            ?.unwrapUnreportedDuplicate()
        if (nestedElementDiagnostic != null) {
            val delegatedArrayType = expectedElementType?.let { elementType ->
                constructArrayLiteralType(
                    expectedType,
                    elementType,
                    arrayLiteralWithElementDiagnostics.elements.size,
                )
            }
            arrayLiteralWithElementDiagnostics.replaceConeTypeOrNull(
                ConeErrorType(
                    ConeUnreportedDuplicateDiagnostic(nestedElementDiagnostic),
                    delegatedType = delegatedArrayType,
                )
            )
            return arrayLiteralWithElementDiagnostics
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
        arrayLiteralWithElementDiagnostics.replaceConeTypeOrNull(
            constructArrayLiteralType(expectedType, elementType, arrayLiteralWithElementDiagnostics.elements.size)
        )
        return arrayLiteralWithElementDiagnostics
    }

    /** 在数组元素被 expected-type 包装成错误节点前保留数组根的实际推断类型。 */
    private fun CfirArrayLiteral.actualArrayTypeBeforeExpectedElementFailure(
        expectedType: ConeCangJieType?,
    ): ConeCangJieType? {
        val elementTypes = elements
            .mapNotNull { it.coneTypeOrNull }
            .filterNot { it is ConeErrorType }
        val elementType = inferredElementTypeOrNull(elementTypes) ?: return null
        return constructArrayLiteralType(expectedType, elementType, elements.size)
    }

    /**
     * overload-by-lambda rollback 试跑中的目标类型数组检查。
     *
     * 官方 `ChkArrayLit` 在第一个目标元素检查失败后停止。这里仅在 speculative 候选试跑中
     * 采用同样的首个失败点短路；普通路径和最终选中候选仍走完整 `transformChildren`。
     */
    private fun CfirArrayLiteral.transformElementsForOverloadByLambdaCandidateTrial(
        expectedElementType: ConeCangJieType,
        elementResolutionMode: ResolutionMode,
    ): ArrayLiteralCandidateTrialResult {
        transformAnnotations(transformer, elementResolutionMode)

        val checkedElements = elements.toMutableList()
        for (index in checkedElements.indices) {
            val transformedElement = checkedElements[index].transform<CfirExpression, ResolutionMode>(
                transformer,
                elementResolutionMode,
            )
            val actualType = transformedElement.coneTypeOrNull
            val elementToStore = if (
                actualType != null &&
                actualType !is ConeErrorType &&
                !actualType.isCompatibleWith(expectedElementType)
            ) {
                transformedElement.asTypeMismatchExpression(expectedElementType, actualType)
            } else {
                transformedElement
            }
            checkedElements[index] = elementToStore

            val shouldStop = elementToStore !== transformedElement ||
                    transformedElement.hasOverloadByLambdaArrayElementFailure()
            if (shouldStop) {
                components.context.reportOverloadByLambdaCandidateDiagnostic(ErrorTypeInArguments)
                val failedArrayLiteral = replaceElementsIfNeeded(checkedElements)
                failedArrayLiteral.replaceConeTypeOrNull(
                    errorType("array literal element type mismatch", DiagnosticKind.Other)
                )
                return ArrayLiteralCandidateTrialResult(failedArrayLiteral, stoppedOnFailure = true)
            }
        }

        return ArrayLiteralCandidateTrialResult(
            replaceElementsIfNeeded(checkedElements),
            stoppedOnFailure = false,
        )
    }

    /** 判断已解析数组元素是否已经足以判定当前 OBL 试跑候选失败。 */
    private fun CfirExpression.hasOverloadByLambdaArrayElementFailure(): Boolean =
        coneTypeOrNull is ConeErrorType ||
                rootErrorDiagnosticOrNull() != null ||
                hasErrorCalleeReferenceOrFailedCandidate() ||
                components.context.hasCurrentShortCircuitableOverloadByLambdaCandidateFailure() ||
                components.context.hasCurrentInferenceSessionContradiction()

    /** 递归查找元素树中已经解析出的错误引用或失败候选引用。 */
    private fun CfirExpression.hasErrorCalleeReferenceOrFailedCandidate(): Boolean {
        var result = false
        accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (result) return
                val reference = (element as? CfirResolvable)?.calleeReference
                when (reference) {
                    is CfirErrorNamedReference -> {
                        result = true
                        return
                    }
                    is CfirNamedReferenceWithCandidate -> {
                        if (!reference.candidate.isSuccessful) {
                            result = true
                            return
                        }
                    }

                    else -> Unit
                }
                element.acceptChildren(this)
            }
        })
        return result
    }

    /**
     * 数组元素类型收集前确保 quote 元素已经完成类型合成。
     */
    private fun CfirArrayLiteral.ensureQuoteElementTypes() {
        elements.forEach { element ->
            if (element is CfirQuoteExpression && element.coneTypeOrNull == null) {
                element.ensureQuoteExpressionType()
            }
        }
    }

    /**
     * 按 expected element type 为数组字面量元素补充类型不匹配错误节点。
     *
     * 已经是错误类型或缺失类型的元素保持原样，避免在同一元素上重复诊断。
     */
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

    /**
     * 从数组元素类型集合推断可接受的数组元素类型。
     *
     * 推断结果会先消解 ideal type，再过滤掉只能退到不可见/过宽 `Any` 的组合。
     */
    private fun CfirArrayLiteral.inferredElementTypeOrNull(elementTypes: List<ConeCangJieType>): ConeCangJieType? {
        elementTypes.firstOrNull()?.let { firstType ->
            if (elementTypes.all { it == firstType }) {
                return IdealTypeResolver.resolveIfIdeal(firstType)
            }
        }
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

    /** 判断类型是否是仓颉顶层 `Any`，用于数组公共元素类型过滤。 */
    private fun ConeCangJieType.isAnyType(): Boolean {
        return this === ConeAnyType || (this is ConeClassLikeType && classId == StdlibClassIds.Any)
    }

    /**
     * 在数组元素列表发生改变时构造拷贝；未改变时复用原字面量节点。
     *
     * 这避免纯检查路径不必要地替换 CFIR 节点，同时保证错误元素包装能写回树。
     */
    private fun CfirArrayLiteral.replaceElementsIfNeeded(newElements: List<CfirExpression>): CfirArrayLiteral {
        if (newElements == elements) return this
        return buildArrayLiteralCopy(this) {
            elements.clear()
            elements.addAll(newElements)
        }
    }

    /** 判断实际类型是否可以作为 expected type 的元素值使用。 */
    private fun ConeCangJieType.isCompatibleWith(expectedType: ConeCangJieType): Boolean =
        AbstractTypeChecker.equalTypes(session.typeContext, this, expectedType) ||
                AbstractTypeChecker.isSubtypeOf(session.typeContext, this, expectedType)

    /** 将普通表达式包装成携带类型不匹配诊断的错误表达式。 */
    private fun CfirExpression.asTypeMismatchExpression(
        expectedType: ConeCangJieType,
        actualType: ConeCangJieType,
    ): CfirErrorExpression = buildErrorExpression {
        source = this@asTypeMismatchExpression.source
        diagnostic = ConeTypeMismatchError(expectedType, actualType)
        nonExpressionElement = this@asTypeMismatchExpression
    }

    /** 为无法推断出统一元素类型的数组字面量构造错误表达式。 */
    private fun CfirArrayLiteral.asInconsistentElementTypeExpression(): CfirErrorExpression = buildErrorExpression {
        source = this@asInconsistentElementTypeExpression.source
        diagnostic = ConeInconsistentArrayLiteralElementTypeError()
    }

    /**
     * 在没有 expected type 时，根据当前元素类型合成数组类型。
     *
     * 该函数用于错误恢复路径，因此无法得到公共父类型时会返回携带简单诊断的错误元素类型。
     */
    private fun CfirArrayLiteral.constructArrayTypeFromElements(): ConeCangJieType {
        val elementType = elements
            .mapNotNull { it.coneTypeOrNull }
            .filterNot { it is ConeErrorType }
            .let { session.typeContext.commonSuperTypeOrNull(it) }
            ?: ConeErrorType(ConeSimpleDiagnostic("array literal element type"))
        return constructArrayType(elementType)
    }

    /** 构造标准库 `Array<elementType>` 类型。 */
    private fun constructArrayType(elementType: ConeCangJieType): ConeCangJieType {
        return constructNamedType(
            classId = StdlibClassIds.Array,
            typeArguments = listOf(elementType),
        )
    }

    /**
     * 根据 expected type 和元素个数构造数组字面量最终类型。
     *
     * expected type 为 `VArray` 时保留其属性并把 size 固定为字面量元素个数；
     * 其他情况按普通 `Array<T>` 处理。
     */
    private fun constructArrayLiteralType(
        expectedType: ConeCangJieType?,
        elementType: ConeCangJieType,
        elementCount: Int,
    ): ConeCangJieType {
        return when (expectedType) {
            is ConeVArrayType -> ConeVArrayType(
                elementType = elementType,
                size = elementCount.toLong(),
                attributes = expectedType.attributes,
            )
            else -> constructArrayType(elementType)
        }
    }

    /**
     * 解析字符串插值表达式，子表达式独立解析，整体类型固定为标准库 `String`。
     *
     * 官方 `SynLitConstStringExpr` 会综合每个插值块并要求 `${expr}` 的类型满足
     * `core.ToString`。这里把同一语义作为 PCLA 约束写入当前推断会话，使
     * `{ x => "${x}" }`、`{ g => "${g(0)}" }` 这类无上下文 lambda 能通过插值
     * 语法反推出参数和嵌套函数返回边界。
     */
    override fun transformStringInterpolation(
        stringInterpolation: CfirStringInterpolation,
        data: ResolutionMode,
    ): CfirExpression {
        stringInterpolation.transformChildren(transformer, ResolutionMode.ContextIndependent)
        stringInterpolation.addStringInterpolationToStringConstraints()
        val stringType = stdlibStringType()
        val partErrorType = stringInterpolation.parts.firstNotNullOfOrNull { part ->
            part.coneTypeOrNull?.propagatedErrorTypeOrNull()
        }
        stringInterpolation.replaceConeTypeOrNull(
            partErrorType?.withStringInterpolationDelegatedType(stringType) ?: stringType
        )
        return stringInterpolation
    }

    /**
     * 插值字符串的局部错误应让整个表达式成为错误类型，同时保留 String 的近似结果类型。
     */
    private fun ConeErrorType.withStringInterpolationDelegatedType(stringType: ConeCangJieType): ConeErrorType =
        ConeErrorType(
            diagnostic,
            isUninferredParameter = isUninferredParameter,
            delegatedType = stringType,
            typeArguments = typeArguments,
            attributes = attributes,
        )

    /** 为字符串插值中的表达式部分添加 `expr <: ToString` 约束。 */
    private fun CfirStringInterpolation.addStringInterpolationToStringConstraints() {
        val toStringType = constructNamedType(StdlibClassIds.ToString)
        for (part in parts) {
            if (part is CfirLiteralExpression) continue
            val partType = part.coneTypeOrNull ?: continue
            if (partType is ConeErrorType) continue
            components.context.inferenceSession.addSubtypeConstraintIfCompatible(partType, toStringType)
        }
    }

    // ── Comparison / Binary / Type Operators ──────────────────────────────────

    /**
     * 解析比较表达式。
     *
     * 左右操作数先独立解析，再通过内建操作符或普通操作符调用解析确定结果类型；
     * 比较表达式的正常结果类型应为 `Bool`，错误路径保留操作符解析诊断。
     */
    override fun transformComparisonExpression(
        comparisonExpression: CfirComparisonExpression,
        data: ResolutionMode,
    ): CfirExpression {
        comparisonExpression.transformChildren(transformer, ResolutionMode.ContextIndependent)
        comparisonExpression.replaceConeTypeOrNull(resolveComparisonExpressionType(comparisonExpression, data))
        return comparisonExpression
    }

    /**
     * 计算比较表达式的结果类型。
     *
     * tuple 相等比较、内建操作符和用户定义操作符按优先级依次尝试；
     * 候选选择或调用完成中的诊断会转换成带 `Bool` delegated type 的错误类型。
     */
    private fun resolveComparisonExpressionType(
        comparisonExpression: CfirComparisonExpression,
        data: ResolutionMode,
    ): ConeCangJieType {
        val leftType = comparisonExpression.left.coneTypeOrNull
        val rightType = comparisonExpression.right.coneTypeOrNull
        if (leftType == null || rightType == null) return builtinTypes.boolType

        val operatorName = comparisonExpression.operation.toOperatorName()
        if (
            (operatorName == OperatorNameConventions.EQUALS || operatorName == OperatorNameConventions.NOT_EQUALS) &&
            leftType is ConeTupleType &&
            rightType is ConeTupleType &&
            leftType.elementTypes.size == rightType.elementTypes.size
        ) {
            return builtinTypes.boolType
        }
        CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
            operatorName,
            leftType,
            listOf(rightType),
        )?.let { return it.returnType }
        inferUniqueBuiltinPrimitiveOperatorSignature(
            operatorName = operatorName,
            expectedReturnType = builtinTypes.boolType,
            receiverExpression = comparisonExpression.left,
            argumentExpressions = listOf(comparisonExpression.right),
        )?.let { signature ->
            comparisonExpression.left.applyPrimitiveOperatorExpectedType(ConePrimitiveType(signature.receiverKind))
            comparisonExpression.right.applyPrimitiveOperatorExpectedType(ConePrimitiveType(signature.parameterKinds.single()))
            return ConePrimitiveType(signature.returnKind)
        }

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

    /**
     * 解析二元逻辑、合并、pipeline 和 composition 表达式。
     *
     * 逻辑运算固定为 `Bool`，`??` 走 Option 元素类型规则，flow 运算会解糖成普通函数调用。
     */
    override fun transformBinaryOp(
        binaryOp: CfirBinaryOp,
        data: ResolutionMode,
    ): CfirExpression {
        val resultType = when (binaryOp.kind) {
            CfirBinaryOpKind.AND,
            CfirBinaryOpKind.OR,
            -> {
                binaryOp.transformChildren(transformer, ResolutionMode.ContextIndependent)
                binaryOp.applyLogicalOperatorOperandConstraints()
                binaryOp.resolveLogicalOperatorType()
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
     * `&&` / `||` 是不可重载的二元逻辑运算；左右操作数必须都是 Bool。
     *
     * 非法逻辑表达式仍挂在二元表达式根节点上，后续 error collector 会通过统一的
     * `ConeUnresolvedNameError` -> `INVALID_BINARY_OPERATOR` 映射报告操作符诊断。
     */
    private fun CfirBinaryOp.resolveLogicalOperatorType(): ConeCangJieType {
        val leftType = left.coneTypeOrNull ?: return builtinTypes.boolType
        val rightType = right.coneTypeOrNull ?: return builtinTypes.boolType

        (leftType.propagatedErrorTypeOrNull() ?: rightType.propagatedErrorTypeOrNull())?.let { errorType ->
            return errorType
        }

        val operatorName = logicalOperatorName()
        CfirBuiltinOperatorResolver.tryResolveBuiltinOperator(
            operatorName,
            leftType,
            listOf(rightType),
        )?.let { return it.returnType }

        val diagnostic = ConeUnresolvedNameError(
            operatorName,
            OperatorNameConventions.TOKENS_BY_OPERATOR_NAME.getValue(operatorName),
            left.invalidBinaryOperatorOperandType(leftType),
            listOf(right.invalidBinaryOperatorOperandType(rightType)),
        )
        return ConeErrorType(diagnostic, delegatedType = builtinTypes.boolType)
    }

    /** 将 CFIR logical binary kind 映射到内部操作符名称。 */
    private fun CfirBinaryOp.logicalOperatorName(): Name =
        when (kind) {
            CfirBinaryOpKind.AND -> OperatorNameConventions.ANDAND
            CfirBinaryOpKind.OR -> OperatorNameConventions.OROR
            else -> error("Expected logical binary operation, got $kind")
        }

    /**
     * `&&` / `||` 的 primitive 语义要求左右 operand 都是 Bool。
     *
     * 普通非法逻辑表达式的诊断仍由现有 checker surface 处理；这里仅在 PCLA 中将
     * fresh lambda 参数 placeholder 约束为 Bool，避免成功 lambda 被错误保留为未推断。
     */
    private fun CfirBinaryOp.applyLogicalOperatorOperandConstraints() {
        if (components.context.inferenceSession !is CfirPCLAInferenceSession) return
        val boolType = ConePrimitiveType(PrimitiveTypeKind.BOOLEAN)
        val leftType = left.coneTypeOrNull ?: return
        val rightType = right.coneTypeOrNull ?: return
        if (leftType.freshLambdaTypeVariableConstructorOrNull() == null &&
            rightType.freshLambdaTypeVariableConstructorOrNull() == null
        ) {
            return
        }
        if (leftType.freshLambdaTypeVariableConstructorOrNull() == null &&
            !leftType.matchesPrimitiveOperatorExpectedKind(PrimitiveTypeKind.BOOLEAN)
        ) {
            return
        }
        if (rightType.freshLambdaTypeVariableConstructorOrNull() == null &&
            !rightType.matchesPrimitiveOperatorExpectedKind(PrimitiveTypeKind.BOOLEAN)
        ) {
            return
        }
        left.applyPrimitiveOperatorExpectedType(boolType)
        right.applyPrimitiveOperatorExpectedType(boolType)
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
        binaryOp.transformAnnotations(transformer, data)
        val desugaredCall = when (binaryOp.kind) {
            CfirBinaryOpKind.PIPELINE -> buildPipelineCall(binaryOp)
            CfirBinaryOpKind.COMPOSITION -> buildCompositionCall(binaryOp)
            else -> error("Expected flow binary operation, got ${binaryOp.kind}")
        }
        val resolvedCall = transformFunctionCallInternal(desugaredCall, data, CallResolutionMode.REGULAR)
        val resultType = resolvedCall.coneTypeOrNull
        val flowResultType = binaryOp.flowOperandRootErrorOrNull(resultType)
            ?: resultType.takeUnless { resolvedCall.hasReportedCallDiagnostic() }
            ?: ConeErrorType(ConeUnreportedDuplicateDiagnostic(ConeSimpleDiagnostic("invalid flow expression")))
        resolvedCall.replaceConeTypeOrNull(flowResultType)
        binaryOp.replaceConeTypeOrNull(flowResultType)
        return resolvedCall
    }

    /**
     * 将 pipeline 表达式 `a |> f` 构造成 `f(a)` 形式的函数调用节点。
     *
     * 右侧函数部分保持函数调用 callee 形态，让实参映射、重载和函数值 invoke
     * 继续由统一调用解析流程处理。
     */
    private fun buildPipelineCall(binaryOp: CfirBinaryOp): CfirFunctionCall {
        val functionPart = binaryOp.right.asFlowFunctionCallPart()
        return buildFunctionCall {
            source = binaryOp.source
            calleeReference = functionPart.reference
            explicitReceiver = functionPart.receiver
            argumentList = buildArgumentList {
                source = binaryOp.source
                arguments.add(binaryOp.left)
            }
            typeArguments.addAll(functionPart.typeArguments)
            origin = CfirFunctionCallOrigin.Regular
        }
    }

    /**
     * 将 composition 表达式 `f ~> g` 构造成标准库 `composition(f, g)` 调用。
     *
     * 两个操作数先按 flow 函数值语境规整：函数引用保持命名访问形态，非函数值
     * 转为 `operator ()` 的命名值访问，再交给 `composition` 的形参约束解析。
     */
    private fun buildCompositionCall(binaryOp: CfirBinaryOp): CfirFunctionCall =
        buildFunctionCall {
            source = binaryOp.source
            calleeReference = buildNamedReference {
                source = binaryOp.source
                name = Name.identifier("composition")
            }
            argumentList = buildArgumentList {
                source = binaryOp.source
                arguments.add(binaryOp.left.asFlowFunctionValue())
                arguments.add(binaryOp.right.asFlowFunctionValue())
            }
            origin = CfirFunctionCallOrigin.CompilerCoreIntrinsic
        }

    /**
     * flow 函数部分的调用结构。
     */
    private data class FlowFunctionCallPart(
        val reference: CfirReference,
        val receiver: CfirExpression?,
        val typeArguments: List<CfirTypeRef>,
    )

    /**
     * 对齐官方 `TryDesugarFunctionCallExpr`：函数引用保持 callee，其他表达式作为
     * `operator ()` 接收者处理，由现有隐式/显式 invoke 候选解析完成后续语义。
     */
    private fun CfirExpression.asFlowFunctionCallPart(): FlowFunctionCallPart {
        val access = this as? CfirQualifiedAccessExpression
        val reference = access?.calleeReference as? CfirNamedReference
        return if (access != null && access !is CfirFunctionCall && reference != null) {
            FlowFunctionCallPart(
                reference = buildNamedReference {
                    source = reference.source
                    name = reference.name
                },
                receiver = access.explicitReceiver,
                typeArguments = access.typeArguments,
            )
        } else {
            FlowFunctionCallPart(
                reference = buildNamedReference {
                    source = source
                    name = OperatorNameConventions.INVOKE
                },
                receiver = this,
                typeArguments = emptyList(),
            )
        }
    }

    /**
     * composition 的操作数是函数值；非函数类型的普通值通过 `operator ()` 取函数值。
     */
    private fun CfirExpression.asFlowFunctionValue(): CfirExpression {
        val resolved = transform<CfirExpression, ResolutionMode>(transformer, ResolutionMode.ContextDependent)
        val expandedType = resolved.coneTypeOrNull?.fullyExpandedType(session)
        if (expandedType == null || expandedType is ConeFunctionType || expandedType is ConeErrorType) return resolved

        return buildNamedAccessExpression {
            source = resolved.source
            calleeReference = buildNamedReference {
                source = resolved.source
                name = OperatorNameConventions.INVOKE
            }
            explicitReceiver = resolved
        }
    }

    /** 判断解糖后的 flow 调用是否已经携带调用层诊断。 */
    private fun CfirExpression.hasReportedCallDiagnostic(): Boolean =
        ((this as? CfirResolvable)?.calleeReference as? CfirDiagnosticHolder)?.diagnostic != null

    /**
     * flow 任一操作数内部已有根错误时，外层 flow 结果保持错误类型，避免再派生
     * 赋值、返回值或后续调用的二次诊断。
     */
    private fun CfirBinaryOp.flowOperandRootErrorOrNull(
        delegatedType: ConeCangJieType?,
    ): ConeErrorType? =
        left.rootErrorDiagnosticOrNull()?.let { diagnostic ->
            ConeErrorType(
                ConeUnreportedDuplicateDiagnostic(diagnostic),
                delegatedType = flowRecoverableDelegatedType(delegatedType, diagnostic),
            )
        } ?: right.rootErrorDiagnosticOrNull()?.let { diagnostic ->
            ConeErrorType(
                ConeUnreportedDuplicateDiagnostic(diagnostic),
                delegatedType = flowRecoverableDelegatedType(delegatedType, diagnostic),
            )
        }

    /**
     * composition 的结果仍是函数值；操作数根错误已确定时，用错误函数类型承载恢复语境，
     * 防止后续 `let h = f ~> g; h(...)` 退化为未解析变量调用。
     */
    private fun CfirBinaryOp.flowRecoverableDelegatedType(
        delegatedType: ConeCangJieType?,
        diagnostic: ConeDiagnostic,
    ): ConeCangJieType? {
        if (kind != CfirBinaryOpKind.COMPOSITION) return null
        if (delegatedType?.fullyExpandedType(session) is ConeFunctionType) return delegatedType
        val duplicateDiagnostic = ConeUnreportedDuplicateDiagnostic(diagnostic)
        val errorComponentType = ConeErrorType(duplicateDiagnostic)
        return ConeFunctionType(
            parameterTypes = listOf(errorComponentType),
            returnType = errorComponentType,
        )
    }

    /**
     * 递归提取表达式树中已经确定的解析错误。
     */
    private fun CfirExpression.rootErrorDiagnosticOrNull(): ConeDiagnostic? {
        var result: ConeDiagnostic? = null
        accept(object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (result != null) return
                val expression = element as? CfirExpression
                val errorType = expression?.coneTypeOrNull as? ConeErrorType
                if (errorType != null) {
                    result = errorType.diagnostic
                    return
                }
                val referenceDiagnostic = (element as? CfirResolvable)
                    ?.calleeReference
                    ?.let { it as? CfirDiagnosticHolder }
                    ?.diagnostic
                if (referenceDiagnostic != null) {
                    result = referenceDiagnostic
                    return
                }
                element.acceptChildren(this)
            }
        })
        return result
    }

    /** 展开错误恢复传播过程中叠加的“不重复上报”包装。 */
    private tailrec fun ConeDiagnostic.unwrapUnreportedDuplicate(): ConeDiagnostic =
        if (this is ConeUnreportedDuplicateDiagnostic) original.unwrapUnreportedDuplicate() else this

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
            ?: binaryOp.inferCoalescingElementTypeFromFreshLeftOperand(data)
        if (leftElementType == null) {
            binaryOp.transformRight(transformer, ResolutionMode.ContextIndependent)
            return errorType("coalescing left operand must be Option")
        }

        val resultType = coalescingResultType(leftElementType, data.expectedTypeOrNull)
        binaryOp.transformRight(transformer, withExpectedType(resultType))
        return resultType
    }

    /**
     * PCLA 中 `x ?? y` 的左操作数可能仍是无上下文 lambda 参数 placeholder。
     *
     * 官方 `??` 语义要求左侧为 `Option<T>` 且表达式结果为 `T`。当左侧还没有定型时，
     * 这里用外层 expected type 或右操作数推断出的 `T` 反向约束左侧 placeholder 为
     * `Option<T>`，让后续 completion 固定 lambda 参数类型，而不是提前把 `??` 标为错误。
     */
    private fun CfirBinaryOp.inferCoalescingElementTypeFromFreshLeftOperand(
        data: ResolutionMode,
    ): ConeCangJieType? {
        val leftType = left.lambdaPrimitiveOperandTypeOrNull()
            ?: left.coneTypeOrNull
            ?: return null
        if (leftType.freshLambdaTypeVariableConstructorOrNull() == null) return null
        if (components.context.inferenceSession !is CfirPCLAInferenceSession) return null

        val expectedElementType = data.expectedTypeOrNull
        if (expectedElementType != null) {
            left.applyCoalescingLeftExpectedType(expectedElementType)
            return expectedElementType
        }

        right.transform<CfirExpression, ResolutionMode>(transformer, ResolutionMode.ContextDependent)
        val rightType = right.coneTypeOrNull
            ?.let { IdealTypeResolver.resolveIfIdeal(it) }
            ?: return null
        if (rightType is ConeErrorType) return null

        left.applyCoalescingLeftExpectedType(rightType)
        return rightType
    }

    /**
     * 将 fresh lambda 左操作数约束为 `Option<elementType>`。
     */
    private fun CfirExpression.applyCoalescingLeftExpectedType(elementType: ConeCangJieType) {
        val currentType = lambdaPrimitiveOperandTypeOrNull() ?: coneTypeOrNull ?: return
        if (currentType.freshLambdaTypeVariableConstructorOrNull() == null) return
        val optionType = constructNamedType(
            classId = StdlibClassIds.Option,
            typeArguments = listOf(elementType),
        )
        components.context.inferenceSession.addSubtypeConstraintIfCompatible(currentType, optionType)
    }

    /**
     * 根据左操作数 Option 元素类型和外层 expected type 选择 `??` 的结果类型。
     *
     * 当元素类型可作为 expected type 使用时返回 expected type，以保持上下文驱动的类型收窄；
     * 否则保留左侧元素类型。
     */
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

    /**
     * 解析 `is` / `as` 类型操作表达式。
     *
     * `is` 固定返回 `Bool`；仓颉 `as` 为安全转换，静态结果类型为 `Option<T>`。
     */
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

    /**
     * 解析基础数值类型转换表达式。
     *
     * 目标类型和实参先独立解析；随后校验目标必须是 primitive，实参必须符合官方数值转换规则。
     * 外层 expected type 只作为上下文提示，类型不匹配由对应 checker 统一报告。
     */
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

        typeConversion.replaceConeTypeOrNull(synthesizedType)
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

    /**
     * 解析 `for-in` 表达式。
     *
     * 迭代对象先独立定型，再由 iterable 类型推断循环变量 pattern 的 expected type；
     * pattern binding 会注册到循环体局部作用域，整个循环表达式类型固定为 `Unit`。
     */
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

    /**
     * 从 iterable 类型推断 `for-in` 元素类型。
     *
     * 数组、VArray、Range 和泛型容器按已知结构提取元素类型；无法识别时返回错误类型，
     * 使循环变量仍能继续以错误恢复类型参与后续解析。
     */
    private fun inferIterableElementType(iterableType: ConeCangJieType?): ConeCangJieType {
        if (iterableType == null) return errorType("iterable has no type")
        val expandedIterableType = iterableType.fullyExpandedType(session)
        expandedIterableType.arrayElementType?.let { return it }
        val classifierType = expandedIterableType as? ConeClassifierType
        if (classifierType?.lookupTag?.classId == StdlibClassIds.Range) {
            return expandedIterableType.typeArguments.firstOrNull()?.type ?: ConePrimitiveType.INT64
        }
        expandedIterableType
            .findCorrespondingClassLikeSupertype(session, StdlibClassIds.Iterable)
            ?.typeArguments
            ?.singleOrNull()
            ?.type
            ?.let { return it }
        return errorType("cannot infer element type from: $iterableType")
    }

    /**
     * 解析 `while` / `do-while` 循环表达式。
     *
     * 入口负责按循环形态通知 CFG 分析器，并处理条件中的 `let pattern` 作用域；
     * 循环表达式本身不产生值，类型固定为 `Unit`。
     */
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

    /**
     * 解析 effect `handle` 子句。
     *
     * 命令模式类型引用先解析；feature 开启时根据 `Command<T>` 建立 handler 上下文，
     * body 解析完成后以 body 类型作为 handle 子句类型，命令不匹配则保留 delegated body 类型。
     */
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

    /**
     * 解析 `try` 表达式及其 resources、catch、handle、finally 子结构。
     *
     * 普通 try/catch 会把外层 expected type 下推到各结果 block；try-with-resources 固定为 `Unit`。
     * 最终类型由 try block、catch block 和 handle block 逐步 Join 得到，并保留 handler 类型不匹配诊断。
     */
    override fun transformTryExpression(
        tryExpression: CfirTryExpression,
        data: ResolutionMode,
    ): CfirExpression {
        tryExpression.transformAnnotations(transformer, data)
        components.dataFlowAnalyzer.enterTryExpression(tryExpression)
        val isTryWithResources = tryExpression.resources.isNotEmpty()
        val expectedType = data.expectedTypeOrNull
        val branchResolutionMode = if (isTryWithResources) {
            ResolutionMode.ContextDependent
        } else {
            (data as? ResolutionMode.WithExpectedType)
                ?.takeUnless { it.fromCast }
                ?.copy(forceFullCompletion = false)
                ?: ResolutionMode.ContextDependent
        }
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
                 * 官方仓颉 `SynTryWithResourcesExpr` 会综合资源声明和 try block，
                 * 但 try-with-resources 表达式自身类型固定为 `Unit`，不会把外层
                 * target type 下推到 try block 尾表达式。
                 */
                isTryWithResources -> builtinTypes.unitType

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

    /**
     * 解析单个 `catch` 子句。
     *
     * catch pattern 的类型引用会先被解析并写回 binding variable，再在新的 block 作用域中解析子句体；
     * catch 子句类型等于 body 类型，缺失时回退为 `Unit`。
     */
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

    /**
     * 解析下标访问表达式。
     *
     * tuple 和 VArray 走结构化快速路径；普通数组和用户类型会解析内建 `[]` 或 `get` 操作符。
     * 接收者或索引上的错误类型会优先传播，避免派生出重复的下标诊断。
     */
    override fun transformSubscriptExpression(
        subscriptExpression: CfirSubscriptExpression,
        data: ResolutionMode,
    ): CfirExpression {
        subscriptExpression.transformAnnotations(transformer, ResolutionMode.ContextIndependent)
        subscriptExpression.transformReceiver(transformer, ResolutionMode.ReceiverResolution)
        subscriptExpression.transformIndices(transformer, ResolutionMode.ContextIndependent)
        val receiverType = subscriptExpression.receiver.coneTypeOrNull
        val receiverErrorType = receiverType?.propagatedErrorTypeOrNull()
        val indexErrorType = subscriptExpression.indices.firstNotNullOfOrNull { index ->
            index.coneTypeOrNull?.propagatedErrorTypeOrNull()
        }
        if (receiverErrorType != null || indexErrorType != null) {
            subscriptExpression.replaceConeTypeOrNull(
                receiverErrorType ?: indexErrorType ?: errorType("subscript operand has error type")
            )
            return subscriptExpression
        }
        val expandedReceiverType = receiverType?.fullyExpandedType(session)
        val resultType = if (subscriptExpression.receiver.isTypeQualifierReceiver()) {
            if (receiverType != null) {
                resolveSubscriptExpressionType(subscriptExpression, receiverType, data)
            } else {
                errorType("receiver has no type")
            }
        } else {
            when (val effectiveReceiverType = expandedReceiverType ?: receiverType) {
                is ConeTupleType -> {
                    val indexValue = extractConstantIntIndex(subscriptExpression.indices.firstOrNull())
                    if (indexValue != null && indexValue in effectiveReceiverType.elementTypes.indices) {
                        val elementType = effectiveReceiverType.elementTypes[indexValue]
                        elementType.propagatedErrorTypeOrNull() ?: elementType
                    } else {
                        errorType("tuple index out of bounds or non-constant")
                    }
                }
                is ConeVArrayType -> effectiveReceiverType.elementType.propagatedErrorTypeOrNull()
                    ?: effectiveReceiverType.elementType
                else -> {
                    val arrayElementType = effectiveReceiverType?.arrayElementType ?: receiverType?.arrayElementType
                    arrayElementType?.propagatedErrorTypeOrNull() ?: arrayElementType
                        ?: if (receiverType != null) {
                            resolveSubscriptExpressionType(subscriptExpression, receiverType, data)
                        } else {
                            errorType("receiver has no type")
                        }
                }
            }
        }
        subscriptExpression.replaceConeTypeOrNull(resultType)
        return subscriptExpression
    }

    /** 类型 qualifier 不是运行时数组值，不能走 tuple/VArray/Array 快速元素访问路径。 */
    private fun CfirExpression.isTypeQualifierReceiver(): Boolean =
        qualifierScopeOrNull(session, components.scopeSession) != null

    /**
     * 解析普通下标访问的结果类型。
     *
     * 优先匹配内建 `[]` 操作符；没有内建匹配时构造 `get` 调用并走统一调用解析与调用完成流程。
     */
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

    /**
     * 解析下标赋值的 `set` 操作符。
     *
     * VArray 赋值直接以元素类型作为下标表达式类型；其他接收者会构造 `set(receiver, indices, value)`
     * 对应的操作符调用，并把调用完成结果写回下标表达式。
     */
    private fun resolveSubscriptSetAssignment(
        assignment: CfirAssignment,
        subscriptExpression: CfirSubscriptExpression,
        data: ResolutionMode,
    ) {
        val receiverType = subscriptExpression.receiver.coneTypeOrNull?.fullyExpandedType(session)
        if (receiverType is ConeVArrayType && !subscriptExpression.receiver.isTypeQualifierReceiver()) {
            subscriptExpression.replaceConeTypeOrNull(receiverType.elementType)
            return
        }

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

    /**
     * 从下标表达式中提取非负 Int 范围内的编译期整数字面量索引。
     *
     * 该函数只接受无显式后缀或 `i64` 后缀的整数字面量，用于 tuple 静态下标检查。
     */
    private fun extractConstantIntIndex(expr: CfirExpression?): Int? {
        val parsed = expr?.let(CfirIntConstantEvalUtils::parseSignedIntExpression) ?: return null
        if (parsed.explicitSuffix != null && parsed.explicitSuffix != "i64") return null
        if (parsed.value < BigInteger.ZERO || parsed.value > BigInteger.valueOf(Int.MAX_VALUE.toLong())) return null
        return parsed.value.toInt()
    }

    // ── Lambda ────────────────────────────────────────────────────────────────

    /**
     * 解析匿名函数表达式。
     *
     * 显式返回类型和形参类型先独立解析；上下文相关模式下只存储延迟解析上下文，
     * 有 expected function type 时通过 synthetic outer call 完成参数和返回类型约束。
     */
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

            resolveAnonymousFunctionExplicitParameterTypes(anonFunc)

            if (data is ResolutionMode.ContextDependent) {
                components.dataFlowAnalyzer.enterAnonymousFunctionExpression(anonymousFunctionExpression)
                context.storeContextForAnonymousFunction(anonFunc)
                return@withClearedEffectHandlers anonymousFunctionExpression
            }

            components.syntheticCallGenerator.resolveAnonymousFunctionExpressionWithSyntheticOuterCall(
                anonymousFunctionExpression = anonymousFunctionExpression,
                expectedTypeData = data as? ResolutionMode.WithExpectedType,
                context = transformer.resolutionContext,
            )
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

    /**
     * 解析 range 表达式并构造 `Range<T>` 类型。
     *
     * 当外层 expected type 已经是 `Range<T>` 时直接使用其元素类型；否则按起止表达式类型推断元素类型。
     */
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

    /**
     * 解析 `spawn` 表达式。
     *
     * 官方 `ChkSpawnExpr` 在外层目标类型是 `Future<T>` 时，会把 task 按 `() -> T`
     * 检查；没有目标类型时才先综合 task 返回类型再构造 `Future<returnType>`。
     */
    override fun transformSpawnExpression(
        spawnExpression: CfirSpawnExpression,
        data: ResolutionMode,
    ): CfirExpression {
        spawnExpression.transformAnnotations(transformer, data)

        val expectedFutureType = data.expectedTypeOrNull?.futureTypeOrNull()
        val expectedTaskReturnType = expectedFutureType?.typeArguments?.singleOrNull()?.type
        spawnExpression.transformBody(
            transformer,
            expectedTaskReturnType?.let(::withExpectedType) ?: ResolutionMode.ContextIndependent,
        )
        spawnExpression.transformThreadContextArgument(transformer, ResolutionMode.ContextIndependent)

        val resultType = data.expectedTypeOrNull
            ?.let { spawnExpression.applySpawnExpectedFutureType(it, session) }
            ?: spawnExpression.synthesizeSpawnType(session)
        spawnExpression.replaceConeTypeOrNull(resultType)
        return spawnExpression
    }

    /**
     * 对已经完成候选选择的访问表达式补齐调用完成或 callee 类型。
     *
     * 带候选的引用交给 call completer；已解析命名引用但表达式类型缺失时，从 callee 元素合成类型。
     */
    private fun <T> completeResolvedAccess(
        access: T,
        data: ResolutionMode,
    ): T where T : CfirExpression, T : CfirResolvable {
        val candidateReference = access.calleeReference as? CfirNamedReferenceWithCandidate
        if (candidateReference != null) {
            access.replaceInitializerReferenceIfNeeded()?.let { return it }
            return components.callCompleter.completeCall(access, data)
        }

        access.replaceInitializerReferenceIfNeeded()?.let { return it }

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

    /**
     * initializer 中需要在解析阶段改写的自引用。
     */
    private fun <T> T.replaceInitializerReferenceIfNeeded(): T? where T : CfirExpression, T : CfirResolvable =
        replaceCurrentFieldInitializerReferenceIfNeeded()

    /**
     * 字段 initializer 中，裸名字引用当前字段自身时，官方解析阶段报未声明名。
     *
     * 显式 `this.a` 保持为成员访问，交给初始化检查报告初始化前使用。
     */
    private fun <T> T.replaceCurrentFieldInitializerReferenceIfNeeded(): T? where T : CfirExpression, T : CfirResolvable {
        val field = context.fieldBeingInitialized ?: return null
        if ((this as? CfirQualifiedAccessExpression)?.explicitReceiver != null) return null
        val reference = calleeReference
        val resolvedSymbol = when (reference) {
            is CfirNamedReferenceWithCandidate -> reference.candidateSymbol
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            else -> return null
        }
        if (resolvedSymbol != field.symbol) return null

        val namedReference = reference as? CfirNamedReference ?: return null
        val diagnostic = ConeUnresolvedNameError(namedReference.name)
        replaceCalleeReference(
            buildErrorNamedReference {
                source = namedReference.source
                name = namedReference.name
                this.diagnostic = diagnostic
            }
        )
        replaceConeTypeOrNull(ConeErrorType(diagnostic))
        return this
    }

    // ── Stdlib / ClassId Helpers ──────────────────────────────────────────────

    /**
     * 构造标准库 `String` 类型。
     *
     * 优先使用当前 session 中可见的类符号以保留具体分类；符号缺失时构造 lookup-tag 类型用于恢复。
     */
    private fun stdlibStringType(): ConeCangJieType {
        val symbol = components.symbolProvider.getClassLikeSymbolByClassId(StdlibClassIds.String)
        if (symbol != null) {
            return constructClassLikeType(symbol, StdlibClassIds.String, emptyList())
        }
        return ConeClassLikeType(StdlibClassIds.String.toLookupTag())
    }

    /**
     * 根据 [ClassId] 和类型实参构造分类器类型。
     *
     * 若符号可解析，则按实际声明种类构造 class/interface/struct/enum/typealias/primitive 类型；
     * 否则保留 lookup tag 以便后续阶段继续错误恢复。
     */
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

    /**
     * 按已解析符号种类构造对应的 Cone 类型。
     *
     * 该函数集中维护标准库 helper 对 class-like、typealias、primitive、struct 和 enum 的类型映射。
     */
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

    /**
     * 计算表达式分支类型的公共父类型。
     *
     * `Nothing` 不参与非空分支 Join；所有分支均为 `Nothing` 时保持 `Nothing`，
     * 类型上下文无法给出公共父类型时使用 `Any` 作为恢复类型。
     */
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

    /**
     * 解析 effect command pattern 中的类型引用。
     *
     * 配置会补充当前容器链上的类型参数，保证 handler 内的局部/成员类型参数能够被 pattern 类型引用看到。
     */
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

    /**
     * 解析 catch pattern 并把 catch binding 变量写入当前作用域。
     *
     * 多个 catch 类型会合成公共父类型；没有显式类型时使用标准库 `Exception` 作为绑定变量类型。
     */
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

    /**
     * 解析 catch pattern 的异常类型引用。
     *
     * 与 command pattern 一样，这里显式补充当前容器的类型参数作用域，避免局部泛型 catch 类型解析失败。
     */
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

    /** 提取 catch pattern 中已经解析成功的异常类型列表。 */
    private fun CfirCatchPattern.resolvedCatchTypes(): List<ConeCangJieType> {
        if (typeRefs.isEmpty()) return emptyList()
        return typeRefs.mapNotNull { typeRef ->
            (typeRef as? CfirResolvedTypeRef)?.coneType
        }
    }

    /** 在类型的超类型链上查找标准库 effect `Command` 类型。 */
    private fun findCommandSupertype(type: ConeCangJieType?): ConeClassLikeType? {
        if (type == null) return null
        return collectSupertypeChain(type, session.typeContext)
            .filterIsInstance<ConeClassLikeType>()
            .firstOrNull { it.lookupTag.classId == StdlibClassIds.Command }
    }

    /** 判断类型是否可以作为 `resume throwing` 的异常类结果。 */
    private fun isExceptionLikeType(type: ConeCangJieType): Boolean {
        val exceptionType = constructNamedType(StdlibClassIds.Exception)
        val errorType = constructNamedType(StdlibClassIds.Error)
        return AbstractTypeChecker.isSubtypeOf(session.typeContext, type, exceptionType) == true ||
                AbstractTypeChecker.isSubtypeOf(session.typeContext, type, errorType) == true
    }

    /**
     * 归一化参与 Join 的类型。
     *
     * 错误类型如果携带 delegated type，则使用 delegated type 参与分支合成，
     * 保留错误诊断的同时避免 Join 被错误包装阻断。
     */
    private fun normalizeTypeForJoin(type: ConeCangJieType?): ConeCangJieType? {
        return when (type) {
            is ConeErrorType -> type.delegatedType ?: type
            else -> type
        }
    }

    /**
     * 在解析匿名函数时暂时清空 effect handler 栈。
     *
     * handler 上下文不应跨越函数边界进入 lambda/body，因此这里保存外层快照，
     * block 执行完成后再恢复。
     */
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

    /**
     * 广度优先收集一个类型的超类型闭包。
     *
     * 该 helper 服务于 effect command / exception-like 判定，使用 visited 集合避免递归继承环导致无限遍历。
     */
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
     * `super` receiver 的定型和合法性诊断分离：
     * 1. class 内的合法 `super` 仍只绑定当前 class 声明的直接 concrete 父类型；
     * 2. interface 内的非法 `super` 先绑定直接父接口做错误恢复，避免 selector/call 级联；
     * 3. struct/enum 等非 class/interface owner 不合成可用 receiver 类型。
     *
     * 合法性本身由 `CfirIllegalSuperReferenceChecker` 按官方规则统一报告。
     */
    private fun resolveImplicitSuperReceiverType(owner: CfirClassLikeDeclaration): ConeCangJieType {
        val directReceiverSuperTypes = owner.directSuperReceiverTypes()
        return when (directReceiverSuperTypes.size) {
            1 -> directReceiverSuperTypes.single()
            0 -> errorType("`super` requires a direct receiver supertype in ${owner.name}")
            else -> errorType("`super` is ambiguous because ${owner.name} declares multiple direct receiver supertypes")
        }
    }

    /**
     * 预留给未来显式 `super<T>` / `super<Base>` 语法：
     * 即使语法层已经指定了目标类型，也必须严格受当前 owner 可绑定的直接父类型约束。
     */
    private fun resolveExplicitSuperReceiverType(
        owner: CfirClassLikeDeclaration,
        resolvedSuperTypeRef: CfirResolvedTypeRef,
    ): ConeCangJieType {
        val requestedType = resolvedSuperTypeRef.coneType
        if (!requestedType.isDirectSuperReceiverTypeFor(owner)) {
            return errorType("`super` can only target a direct receiver supertype of ${owner.name}")
        }

        val directReceiverSuperTypes = owner.directSuperReceiverTypes()
        if (directReceiverSuperTypes.none { it == requestedType }) {
            return errorType("`super` can only target a direct receiver supertype of ${owner.name}")
        }

        return requestedType
    }

    /**
     * 提取当前 owner 中可用于 `super` receiver 恢复的直接父类型。
     *
     * class 的合法 receiver 只取 concrete 父类型；若源码只声明接口父类型，则按官方
     * `AddObjectSuperClass` 语义补入隐式 `Object` receiver。interface 分支只服务非法
     * `super` 后续成员解析恢复，因此只取直接父接口，合法性不在这里放行。
     */
    private fun CfirClassLikeDeclaration.directSuperReceiverTypes(): List<ConeCangJieType> {
        val declaredReceiverTypes = superTypeRefs
            .mapNotNull { superTypeRef ->
                superTypeRef.classifyDeclaredSupertype(session).scopeTraversalTypeOrNull()
            }
            .filter { candidate -> candidate.isDirectSuperReceiverTypeFor(this) }
        return declaredReceiverTypes.withImplicitClassObjectSuperReceiverType(this)
    }

    /**
     * 普通 class 没有显式 concrete 父类时，官方前置检查会补 `std.core.Object`。
     * body resolve 的 `super` receiver 候选需要与类型系统和 member scope 共享同一层继承语义。
     */
    private fun List<ConeCangJieType>.withImplicitClassObjectSuperReceiverType(
        owner: CfirClassLikeDeclaration,
    ): List<ConeCangJieType> {
        if (owner !is CfirClass) return this
        if (owner.symbol.classId == StdlibClassIds.Object) return this
        if (any { candidate -> candidate.isConcreteSuperclassCandidate() }) return this

        val implicitObject = ConeClassLikeType(StdlibClassIds.Object.toLookupTag())
        return (this + implicitObject).distinct()
    }

    /** 判断类型是否可作为当前 owner 的 `super` receiver 类型。 */
    private fun ConeCangJieType.isDirectSuperReceiverTypeFor(owner: CfirClassLikeDeclaration): Boolean = when (owner) {
        is CfirClass -> this is ConeClassLikeType && !isInterface
        is CfirInterface -> this is ConeClassLikeType && isInterface
        else -> false
    }

    /** 判断类型是否占用 class 的 concrete 父类槽位。 */
    private fun ConeCangJieType.isConcreteSuperclassCandidate(): Boolean =
        this is ConeClassLikeType && !isInterface

    // ── Small Extension Utilities ─────────────────────────────────────────────

    /** 按上下文无关模式解析当前表达式，用于 subject、iterable、receiver 等独立定型位置。 */
    private fun CfirExpression.resolveIndependently() {
        transform<CfirElement, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
    }

    /** 在表达式已独立定型后，按上下文无关模式解析可选 block 体。 */
    private fun CfirExpression.resolveIndependently(body: CfirBlock?) {
        body?.transform<CfirElement, ResolutionMode>(transformer, ResolutionMode.ContextIndependent)
    }

    /**
     * 从解析模式中提取外层 expected type。
     *
     * 只有 [ResolutionMode.WithExpectedType] 携带 expected type；其他模式返回 `null`。
     */
    private val ResolutionMode.expectedTypeOrNull: ConeCangJieType?
        get() = (this as? ResolutionMode.WithExpectedType)?.expectedTypeRef?.coneType

    /**
     * 由当前组合表达式的 result-type owner 固化 assignment RHS mismatch。
     *
     * 该 helper 只读取当前 frame 的根 identity；调用方必须在替换根类型前传入实际类型，
     * 因而不会依据 assignment 的 receiver、RHS 语法或最终 ConeErrorType 逆向猜测语义。
     */
    private fun recordAssignmentRhsTypeMismatchIfNeeded(
        expression: CfirExpression,
        actualType: ConeCangJieType?,
        rhsRootValidity: CfirAssignmentRhsRootValidity =
            CfirAssignmentRhsRootValidity.INVALID_AFTER_MISMATCH,
        primaryDiagnostic: CfirAssignmentTypeMismatchPrimaryDiagnostic =
            CfirAssignmentTypeMismatchPrimaryDiagnostic.TypeMismatch,
    ) {
        val actual = actualType ?: return
        if (actual is ConeErrorType) return
        val expected = context.assignmentRhsExpectedTypeFor(expression) ?: return
        if (AbstractTypeChecker.isSubtypeOf(session.typeContext, actual, expected) == true) return
        context.recordAssignmentRhsExpectedTypeMismatch(
            expression = expression,
            actualType = actual,
            primaryDiagnostic = primaryDiagnostic,
            rhsRootValidity = rhsRootValidity,
        )
    }

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

    /**
     * 归一化 range 元素类型。
     *
     * ideal integer 在没有更强上下文时按仓颉默认规则落到 `Int64`，其他 ideal type 通过统一 resolver 消解。
     */
    private fun normalizeRangeElementType(type: ConeCangJieType): ConeCangJieType {
        val normalized = IdealTypeResolver.resolveIfIdeal(type, null)
        return if (normalized is ConePrimitiveType && normalized.kind == PrimitiveTypeKind.IDEAL_INT) {
            ConePrimitiveType.INT64
        } else {
            normalized
        }
    }

    /** 判断类型是否是标准库 `Range`，并在 typealias 场景下沿展开类型继续查找。 */
    private fun ConeCangJieType.rangeTypeOrNull(): ConeClassifierType? = when (this) {
        is ConeClassLikeType -> takeIf { classId == StdlibClassIds.Range }
        is ConeStructType -> takeIf { classId == StdlibClassIds.Range }
        is ConeTypeAliasType -> expandedType?.rangeTypeOrNull()
        else -> null
    }

    /**
     * 解析限定访问上的显式类型实参。
     *
     * 访问表达式可能出现在局部/成员泛型上下文中，因此这里补齐当前容器链类型参数后再解析 typeRef。
     */
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

    /**
     * 解析显式 `super<T>` 目标类型引用。
     *
     * 已解析或隐式 typeRef 直接复用；普通 typeRef 使用当前容器类型参数上下文完成解析。
     */
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

    /**
     * 提取声明自身携带的类型参数列表。
     *
     * 该 helper 用于构造局部 typeRef 解析配置，覆盖 class、callable、extend、pattern variable
     * 和宏声明等可能把类型参数暴露给表达式阶段的声明节点。
     */
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
