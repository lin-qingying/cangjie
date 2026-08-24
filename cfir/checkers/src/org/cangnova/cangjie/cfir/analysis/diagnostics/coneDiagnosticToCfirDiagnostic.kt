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

package org.cangnova.cangjie.cfir.analysis.diagnostics

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.inheritanceCycleDiagnosticSource
import org.cangnova.cangjie.cfir.calls.resolvedQualifierClassifier
import org.cangnova.cangjie.cfir.calls.resolvedQualifierTypeParameter
import org.cangnova.cangjie.cfir.nameConflictsTracker
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostic.*
import org.cangnova.cangjie.cfir.diagnostics.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.inference.AnonymousFunctionBasedMultiLambdaBuilderInferenceRestriction
import org.cangnova.cangjie.cfir.resolve.inference.model.*
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.providers.getContainingClass
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.resovle.calls.ConeTypeParameterBasedTypeVariable
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.semantics.AbstractCallKind
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.semantics.AmbiguousClassifierTypeInCandidateSignature
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInCandidateSignature
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInArguments
import org.cangnova.cangjie.cfir.semantics.InvalidCallableReturnTypeInOverloadSet
import org.cangnova.cangjie.cfir.semantics.ResolutionDiagnostic
import org.cangnova.cangjie.cfir.semantics.hasBareGenericFunctionReferencePayloadArgument
import org.cangnova.cangjie.cfir.semantics.isSuccess
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.getAssignmentByLHS
import org.cangnova.cangjie.resolve.calls.inference.buildAbstractResultingSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.resolve.calls.tower.ApplicabilityDetail
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeKind
import org.cangnova.cangjie.source.*
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeParameterMarker

/**
 * 将 resolve/type inference 阶段产生的 Cone diagnostic 映射为用户可见 CFIR 诊断。
 *
 * 该入口负责按照 diagnostic 的精确子类型选择专用映射路径，并把调用表达式、
 * 赋值表达式、值参数等 source 上下文传递给下游，以保持诊断位置与官方语义一致。
 */
fun ConeDiagnostic.toCfirDiagnostics(
    session: CfirSession,
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    valueParameter: CfirValueParameter? = null,
    returnExpressionSource: AbstractCjSourceElement? = null,
): List<CjDiagnostic> {
    if (this is ConeUnreportedDuplicateDiagnostic) return emptyList()
    return when (this) {
        is ConeConstraintSystemHasContradiction -> mapSystemHasContradictionError(
            session,
            source,
            callOrAssignmentSource,
            returnExpressionSource,
        )
        is ConeInapplicableCandidateError -> mapInapplicableCandidateError(session, source, callOrAssignmentSource)
        is ConeAmbiguityError -> mapConeAmbiguityError(source, callOrAssignmentSource, session)
        is ConeAmbiguousFunctionReferenceError -> mapAmbiguousFunctionReferenceError(
            source,
            callOrAssignmentSource,
            session,
        )
        is ConeNoMatchingFunctionReferenceError -> listOfNotNull(
            CfirErrors.NO_MATCH_FUNCTION_DECLARATION_FOR_REF.on(
                (callOrAssignmentSource ?: source)?.firstCharacterDiagnosticSource() ?: return emptyList(),
                session,
            )
        )
        is ConeNoMatchingFunctionCallError -> listOfNotNull(
            mapNoMatchingFunctionCallError(source, callOrAssignmentSource, session)
        )
        is ConeUnresolvedNameError -> mapConeUnresolvedNameError(source, callOrAssignmentSource, session)
        is ConeHiddenCandidateError -> mapConeHiddenCandidateError(source, callOrAssignmentSource, session)
        is ConeVisibilityError -> listOfNotNull(mapConeVisibilityError(source, callOrAssignmentSource, session))
        is ConeObjectCannotAccessStaticMemberError ->
            listOfNotNull(mapObjectCannotAccessStaticMemberError(source, callOrAssignmentSource, session))
        is ConeIllegalAccessNonStaticMemberError ->
            listOfNotNull(mapIllegalAccessNonStaticMemberError(source, callOrAssignmentSource, session))
        else -> listOfNotNull(
            mapOtherDiagnostic(
                source,
                valueParameter,
                callOrAssignmentSource,
                session,
                returnExpressionSource,
            )
        )
    }
}

/**
 * 把“名字已发现但 callable 全部被排除”的结果映射到完整函数标识符。
 *
 * LightTree 的 callee source 可能只有首字符；项目诊断范围策略要求按函数名长度恢复
 * 完整 token。该映射不读取 Candidate，也不复用普通 visibility/hidden-candidate 路径。
 */
private fun ConeNoMatchingFunctionCallError.mapNoMatchingFunctionCallError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val anchor = source ?: callOrAssignmentSource ?: return null
    val tokenEnd = anchor.startOffset + name.asString().length
    val boundedEnd = callOrAssignmentSource
        ?.endOffset
        ?.let { callEnd -> tokenEnd.coerceAtMost(callEnd) }
        ?: tokenEnd
    return CfirErrors.NO_MATCH_FUNCTION_DECLARATION_FOR_CALL.on(
        CjOffsetsOnlySourceElement(anchor.startOffset, boundedEnd.coerceAtLeast(anchor.endOffset)),
        session,
    )
}

/**
 * 对象访问 static 成员时，诊断锚定在源码显式写出的对象接收者 token 上。
 */
private fun ConeObjectCannotAccessStaticMemberError.mapObjectCannotAccessStaticMemberError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = candidate.callInfo.explicitReceiver?.objectStaticReceiverDiagnosticSource()
        ?: source
        ?: callOrAssignmentSource
        ?: candidate.callInfo.callSite.source
        ?: return null
    return CfirErrors.OBJECT_CANNOT_ACCESS_STATIC_MEMBER.on(diagnosticSource, memberName, session)
}

/**
 * 构造器调用 `B().a` 的接收者 source 是整个 `B()`，但用户可见诊断应落在类型名 `B`。
 */
private fun CfirExpression.objectStaticReceiverDiagnosticSource(): CjSourceElement? {
    val access = this as? CfirQualifiedAccessExpression
    return access?.calleeReference?.source ?: source
}

/**
 * 类型名访问实例成员时，诊断锚定在源码显式写出的类型 qualifier 上。
 */
private fun ConeIllegalAccessNonStaticMemberError.mapIllegalAccessNonStaticMemberError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val explicitReceiver = candidate.callInfo.explicitReceiver
    val diagnosticSource = explicitReceiver?.source
        ?: source
        ?: callOrAssignmentSource
        ?: candidate.callInfo.callSite.source
        ?: return null
    val typeParameter = explicitReceiver?.resolvedQualifierTypeParameter()
    if (typeParameter != null) {
        return CfirErrors.STATIC_VARIABLE_USE_GENERIC_PARAMETER.on(
            diagnosticSource,
            typeParameter.name,
            session,
        )
    }
    return CfirErrors.ILLEGAL_ACCESS_NON_STATIC_MEMBER.on(diagnosticSource, memberName, session)
}

/**
 * 不可见函数候选在官方调用流程中会被过滤出可调用集合，
 * 最终表现为普通函数调用 no-match；属性/变量访问不走这条映射。
 */
private fun ConeHiddenCandidateError.mapConeHiddenCandidateError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): List<CjDiagnostic> {
    if (candidate.callInfo.callSite !is CfirFunctionCall) {
        return emptyList()
    }

    candidate.invalidBinaryOperatorDiagnosticForOperatorCall(source, callOrAssignmentSource, session)
        ?.let { diagnostic -> return listOf(diagnostic) }

    val diagnosticSource = source
        ?: callOrAssignmentSource
        ?: candidate.callInfo.callSite.source
        ?: return emptyList()
    if (candidate.symbol is CfirConstructorSymbol && candidate.callInfo.semanticCallKind == AbstractCallKind.Function) {
        return listOfNotNull(CfirErrors.NO_CONSTRUCTOR.on(diagnosticSource, session))
    }
    return listOfNotNull(
        CfirErrors.NO_MATCH_FUNCTION_DECLARATION_FOR_CALL.on(
            diagnosticSource.firstCharacterDiagnosticSource(),
            session,
        )
    )
}

/**
 * 将单个约束系统错误映射为对应 CFIR 诊断。
 *
 * 该函数处理实参约束、期望类型约束、类型信息不足、空交类型推断和 builder inference
 * 限制等细粒度错误，并在存在更具体诊断时避免退化为泛化的类型不匹配。
 */
private fun ConstraintSystemError.mapConstraintSystemError(
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
    session: CfirSession,
    candidate: AbstractCallCandidate<*>,
): CjDiagnostic? {
    return when (this) {
        is ConstraintMismatch -> {
            if (candidate.symbol.let { it as? CfirCallableSymbol<*> }?.cfir?.typeParameters?.isNotEmpty() == true &&
                !candidate.hasExplicitTypeArgumentsInCall()
            ) {
                ConeConstraintSystemHasContradiction(candidate)
                    .genericInferenceErrorDiagnostic(source, qualifiedAccessSource, session)
                    ?.let { return it }
            }

            val position = position.from
            val argumentAndReportSource: Pair<org.cangnova.cangjie.cfir.CfirElement?, CjSourceElement?> = when (position) {
                is ConeArgumentConstraintPosition -> position.argument to null
                is ConeLambdaArgumentConstraintPosition -> position.argument to position.anonymousFunctionReturnExpression?.source
                is ConeReceiverConstraintPosition -> {
                    val reportOn = position.argument.source?.takeIf { it.kind == CjRealSourceElementKind }
                    position.argument to reportOn
                }

                else -> null to null
            }
            val argument = argumentAndReportSource.first
            val reportOn = argumentAndReportSource.second

            argument?.let {
                if (!candidate.hasExplicitTypeArgumentsInCall()) {
                    (it as? org.cangnova.cangjie.cfir.expressions.CfirExpression)
                        ?.genericInferenceArgumentMismatchDiagnostic(session)
                        ?.let { inferenceDiagnostic ->
                            return inferenceDiagnostic
                        }
                }
                candidate.invalidBinaryOperatorDiagnosticForOperatorCall(source, qualifiedAccessSource, session)
                    ?.let { diagnostic -> return diagnostic }
                return argumentTypeMismatch(
                    source = reportOn ?: it.source ?: source,
                    argument = it as? CfirExpression,
                    expectedType = upperConeType.substituteTypeVariableTypes(candidate, session),
                    actualType = lowerConeType.substituteTypeVariableTypes(candidate, session),
                    isMismatchDueToNullability = false,
                    anonymousFunction = (position as? ConeLambdaArgumentConstraintPosition)?.argument,
                    candidate = candidate,
                    session = session,
                )
            }

            when (position) {
                is ConeExpectedTypeConstraintPosition ->
                    typeMismatchDiagnostic(
                        source = when (candidate.callInfo.callSite) {
                            is CfirNamedAccessExpression -> qualifiedAccessSource
                                ?: source
                                ?: candidate.callInfo.callSite.source

                            else -> qualifiedAccessSource ?: source
                        },
                        callOrAssignmentSource = qualifiedAccessSource,
                        expectedType = upperConeType.substituteTypeVariableTypes(candidate, session),
                        actualType = lowerConeType.substituteTypeVariableTypes(candidate, session),
                        isMismatchDueToNullability = false,
                        session = session,
                    )

                else -> null
            }
        }

        is NotEnoughInformationForTypeParameter<*> ->
            if (candidate.hasBareGenericFunctionReferencePayloadArgument()) {
                null
            } else if (candidate.hasNonCallBareStaticGenericQualifierTypeVariable(typeVariable, session)) {
                null
            } else if (candidate.hasExplicitTypeArgumentError()) {
                null
            } else if (
                candidate.isImplicitBuiltinArrayConstructorCall() ||
                candidate.isImplicitGenericCallWithTypeParameters() ||
                candidate.hasGenericCallNotEnoughTypeInformation(session) ||
                candidate.looksLikeImplicitGenericCallInferenceFailure(source, qualifiedAccessSource)
            ) {
                ConeConstraintSystemHasContradiction(candidate)
                    .unableToInferGenericFunctionDiagnostic(source, qualifiedAccessSource, session)
            } else {
                typeVariable.asDeclaredTypeParameterSymbolOrNull()?.let {
                    ConeConstraintSystemHasContradiction(candidate)
                        .unableToInferGenericFunctionDiagnostic(source, qualifiedAccessSource, session)
                }
            }

        is InferredEmptyIntersection -> {
            inferredIntoEmptyIntersection(
                source = candidate.sourceOfCallToSymbolWith(typeVariable) ?: source ?: qualifiedAccessSource,
                typeVariable = typeVariable,
                incompatibleTypes = incompatibleTypes.map { it.asCone() },
                causingTypes = causingTypes.map { it.asCone() },
                kind = kind,
                isError = this is InferredEmptyIntersectionError,
                session = session,
            )
        }

        is OnlyInputTypesDiagnostic ->
            typeVariable.asDeclaredTypeParameterSymbolOrNull()?.let {
                CfirErrors.TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR.on(source ?: qualifiedAccessSource ?: return null, it, session)
            }

        is AnonymousFunctionBasedMultiLambdaBuilderInferenceRestriction -> {
            val typeParameterSymbol = typeParameter.asDeclaredTypeParameterSymbolOrNull() ?: return null
            val containingDeclarationName = typeParameterSymbol.containingDeclarationSymbol.memberDeclarationNameOrNull()
                ?: error("containingDeclarationSymbol must have been a member declaration")
            CfirErrors.BUILDER_INFERENCE_MULTI_LAMBDA_RESTRICTION.on(
                anonymous.source ?: source ?: qualifiedAccessSource ?: return null,
                typeParameterSymbol.name,
                containingDeclarationName,
                session,
            )
        }

        is MultiLambdaBuilderInferenceRestriction<*> -> error("Unexpected bare MultiLambdaBuilderInferenceRestriction")

        else -> null
    }
}

/**
 * 将候选调用的约束系统矛盾映射为用户可见诊断列表。
 *
 * 映射顺序按根因优先级排列：先处理参数映射和显式类型实参上界，再处理泛型推断，
 * 最后才使用约束 mismatch 或通用 inference error 作为兜底诊断。
 */
private fun ConeConstraintSystemHasContradiction.mapSystemHasContradictionError(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
    returnExpressionSource: AbstractCjSourceElement? = null,
): List<CjDiagnostic> {
    candidate.unsuccessfulCallableReferenceArgumentDiagnostics(session, source, qualifiedAccessSource)
        ?.let { return it }

    candidate.cangjieVariadicRegularCallDiagnostics
        .mapCangjieVariadicRegularCallDiagnostics(
            session,
            source,
            qualifiedAccessSource,
            preferCallSourceForTooManyArguments = candidate.prefersCallSourceForTooManyArguments(),
        )
        .takeIf { it.isNotEmpty() }
        ?.let { return it }

    candidate.diagnostics
        .filter { it.isArgumentMappingDiagnostic }
        .mapCangjieVariadicRegularCallDiagnostics(
            session,
            source,
            qualifiedAccessSource,
            preferCallSourceForTooManyArguments = candidate.prefersCallSourceForTooManyArguments(),
        )
        .takeIf { it.isNotEmpty() }
        ?.let { return it }

    val errors = candidate.errors
    candidate.diagnostics
        .filterIsInstance<TooManyArguments>()
        .mapNotNull { diagnostic ->
            CfirErrors.TOO_MANY_ARGUMENTS.on(
                diagnostic.tooManyArgumentsSource(
                    source,
                    qualifiedAccessSource,
                    preferCallSource = candidate.prefersCallSourceForTooManyArguments(),
                ) ?: return@mapNotNull null,
                diagnostic.targetName,
                session,
            )
        }
        .takeIf { it.isNotEmpty() }
        ?.let { return it }
    if (candidate.callInfo.arguments.any { it.containsErrorDiagnosticInArgument() }) {
        if (errors.any { it is NotEnoughInformationForTypeParameter<*> && it.typeVariable is ConeTypeParameterBasedTypeVariable } &&
            !candidate.hasExplicitTypeArgumentsInCall()
        ) {
            // 泛型参数被推断为失败类型且调用未提供显式实参时，即使实参子树自身含有错误
            // 诊断，也通常应保留 callee 锚点的无法推断诊断。只有裸泛型函数值引用已拥有
            // 自身的“缺少类型实参”主错误，不能再把它提升为外层 enum payload 推断失败。
            return if (candidate.hasBareGenericFunctionReferencePayloadArgument()) {
                emptyList()
            } else {
                listOfNotNull(unableToInferGenericFunctionDiagnostic(source, qualifiedAccessSource, session))
            }
        }
        return emptyList()
    }
    if (errors.hasExplicitTypeArgumentConstraintMismatch()) {
        // 约束系统已经保留显式 type argument source、实际类型和替换后的声明上界，
        // 这里直接映射专用诊断。错误候选在 completion 后不保证仍以普通 qualified access
        // 形态遍历 checker，因此不能把唯一诊断责任推迟到 post-resolve checker。
        return candidate.explicitTypeArgumentConstraintDiagnostics(
            session = session,
            fallbackSource = qualifiedAccessSource ?: source,
        )
    }

    if (isBareStaticGenericQualifierInferenceError(session)) {
        return listOfNotNull(
            CfirErrors.UNABLE_TO_INFER_GENERIC_FUNC.on(
                qualifiedAccessSource ?: source ?: return emptyList(),
                session,
            )
        )
    }
    if (!candidate.hasBareGenericFunctionReferencePayloadArgument() &&
        candidate.hasGenericCallNotEnoughTypeInformation(session)
    ) {
        return listOfNotNull(unableToInferGenericFunctionDiagnostic(source, qualifiedAccessSource, session))
    }
    if (isImplicitEnumConstructorPayloadInferenceMismatch()) {
        return listOfNotNull(unableToInferGenericFunctionDiagnostic(source, qualifiedAccessSource, session))
    }
    if (hasGenericInferenceConstraintMismatch(session)) {
        return listOfNotNull(genericInferenceErrorDiagnostic(source, qualifiedAccessSource, session))
    }

    val hasNotEnoughInformationError = errors.any { it is NotEnoughInformationForTypeParameter<*> }
    val typeAliasConstructorUpperBoundMismatch =
        candidate.hasTypeAliasConstructorExpansionUpperBoundViolation(session) ||
            candidate.hasTypeAliasConstructorUpperBoundConstraintMismatch()
    return errors.coalesceArgumentConstraintMismatches().coalesceExpectedTypeConstraintMismatches().mapNotNull { error ->
        if (hasNotEnoughInformationError &&
            error is ConstraintMismatch &&
            error.position.from is ConeExpectedTypeConstraintPosition
        ) {
            // When generic arguments are absent and type inference itself failed,
            // expected-type mismatches are usually secondary noise.
            return@mapNotNull null
        }
        if (typeAliasConstructorUpperBoundMismatch &&
            error is ConstraintMismatch &&
            error.position.from is ConeExpectedTypeConstraintPosition
        ) {
            return@mapNotNull null
        }
        error.mapConstraintSystemError(
            source,
            qualifiedAccessSource,
            session,
            candidate,
        )
    }.ifEmpty {
        if (typeAliasConstructorUpperBoundMismatch) {
            // typealias 构造器展开后的上界违例已经由专项 checker 报告在别名名处；
            // fallback 只能用于真正缺少更具体诊断的约束系统，不能再泛化成 TYPE_MISMATCH。
            return emptyList()
        }
        if (errors.any { error ->
                when (error) {
                    is ConstrainingTypeIsError -> true
                    is NotEnoughInformationForTypeParameter<*> ->
                        error.typeVariable is ConeTypeParameterBasedTypeVariable ||
                            (error.resolvedAtom as? CfirAnonymousFunction)?.containsErrorType() == true

                    else -> false
                }
            }
        ) {
            return emptyList()
        }

        listOfNotNull(
            errors.firstNotNullOfOrNull { error ->
                when (error) {
                    is ConstraintMismatch -> {
                        if (error.position.from is FixVariableConstraintPosition<*>) {
                            val morePreciseDiagnosticExists = errors.any { other ->
                                other is ConstraintMismatch && other.position.from !is FixVariableConstraintPosition<*>
                            }
                            if (morePreciseDiagnosticExists) return@firstNotNullOfOrNull null
                        }

                        typeMismatchDiagnostic(
                            source = qualifiedAccessSource ?: source,
                            callOrAssignmentSource = qualifiedAccessSource,
                            expectedType = error.upperConeType.substituteTypeVariableTypes(candidate, session),
                            actualType = error.lowerConeType.substituteTypeVariableTypes(candidate, session),
                            isMismatchDueToNullability = false,
                            session = session,
                            returnExpressionSource = returnExpressionSource,
                        )
                    }

                    else -> CfirErrors.NEW_INFERENCE_ERROR.on(
                        qualifiedAccessSource ?: source ?: return@firstNotNullOfOrNull null,
                        "Inference error: ${error::class.simpleName}",
                        session,
                    )
                }
            }
        )
    }
}

/**
 * 判断错误是否来自 bare static generic qualifier 的类型参数推断失败。
 */
private fun ConeConstraintSystemHasContradiction.isBareStaticGenericQualifierInferenceError(
    session: CfirSession,
): Boolean {
    if (candidate.callInfo.callSite !is CfirFunctionCall) return false
    val callable = candidate.symbol.cfir as? CfirCallableDeclaration ?: return false
    if (!callable.status.isStatic) return false
    if (candidate.errors.none { it is NotEnoughInformationForTypeParameter<*> }) return false

    val receiver = candidate.callInfo.explicitReceiver as? CfirQualifiedAccessExpression ?: return false
    if (receiver.typeArguments.isNotEmpty()) return false

    val ownerSymbol = receiver.resolvedQualifierClassifier(session) ?: return false
    val ownerTypeParameterSymbols = ownerSymbol.cfir.typeParameters.mapTo(linkedSetOf()) { it.symbol }
    if (ownerTypeParameterSymbols.isEmpty()) return false
    val explicitCount = (candidate.callInfo.callSite as? CfirQualifiedAccessExpression)
        ?.typeArguments
        ?.count { it is CfirResolvedTypeRef }
        ?: 0
    return !candidate.hasExplicitTypeArgumentsForBareStaticQualifier(ownerTypeParameterSymbols, explicitCount)
}

/**
 * 非调用的 static member 引用若 qualifier 是裸泛型类型，官方把根因归到 qualifier
 * 自身缺少类型实参，而不是把 member reference 泛化成函数调用推断失败。
 */
private fun AbstractCallCandidate<*>.hasNonCallBareStaticGenericQualifierTypeVariable(
    typeVariable: org.cangnova.cangjie.type.model.TypeVariableMarker,
    session: CfirSession,
): Boolean {
    if (callInfo.callSite is CfirFunctionCall) return false
    val callable = symbol.cfir as? CfirCallableDeclaration ?: return false
    if (!callable.status.isStatic) return false

    val receiver = callInfo.explicitReceiver as? CfirQualifiedAccessExpression ?: return false
    if (receiver.typeArguments.isNotEmpty()) return false

    val ownerSymbol = receiver.resolvedBareClassLikeQualifierSymbol() ?: return false
    val ownerTypeParameterSymbols = ownerSymbol.cfir.typeParameters.mapTo(linkedSetOf()) { it.symbol }
    if (ownerTypeParameterSymbols.isEmpty()) return false

    val typeParameterSymbol = typeVariable.asDeclaredTypeParameterSymbolOrNull() ?: return false
    return typeParameterSymbol in ownerTypeParameterSymbols
}

/** 读取限定符表达式自身解析出的 class-like symbol；实例 receiver 不能走 static qualifier 规则。 */
private fun CfirQualifiedAccessExpression.resolvedBareClassLikeQualifierSymbol(): CfirClassLikeSymbol<*>? {
    val symbol = when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
        else -> null
    }
    return symbol as? CfirClassLikeSymbol<*>
}

/**
 * 判断调用是否为 bare static generic qualifier 提供了覆盖 owner 类型参数的显式类型实参。
 */
private fun AbstractCallCandidate<*>.hasExplicitTypeArgumentsForBareStaticQualifier(
    ownerTypeParameterSymbols: Set<CfirTypeParameterSymbol>,
    explicitCount: Int,
): Boolean {
    if (explicitCount == 0) return false

    val declaration = symbol.takeIf { it.isBound }?.cfir ?: return false
    if (declaration is CfirConstructor && explicitCount == ownerTypeParameterSymbols.size) return true

    val callableTypeParameters = (declaration as? CfirTypeParameterRefsOwner)?.typeParameters.orEmpty()
    if (callableTypeParameters.size != explicitCount) return false

    val explicitlyMappedSymbols = callableTypeParameters.mapTo(linkedSetOf()) { it.symbol }
    return explicitlyMappedSymbols.containsAll(ownerTypeParameterSymbols)
}

/**
 * 将不可适用候选错误映射为调用、实参、泛型推断或 unresolved reference 诊断。
 */
private fun ConeInapplicableCandidateError.mapInapplicableCandidateError(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): List<CjDiagnostic> {
    candidate.unsuccessfulCallableReferenceArgumentDiagnostics(session, source, qualifiedAccessSource)
        ?.let { return it }

    candidate.cangjieVariadicRegularCallDiagnostics
        .mapCangjieVariadicRegularCallDiagnostics(
            session,
            source,
            qualifiedAccessSource,
            preferCallSourceForTooManyArguments = candidate.prefersCallSourceForTooManyArguments(),
        )
        .takeIf { it.isNotEmpty() }
        ?.let { return it }

    val contradictionDiagnostic = ConeConstraintSystemHasContradiction(candidate)
    contradictionDiagnostic.multiLambdaBuilderInferenceDiagnostics(session, source, qualifiedAccessSource)
        .takeIf { it.isNotEmpty() }
        ?.let { return it }

    val noMatchingInvokeDiagnostic = mapNoMatchingInvokeOperatorDiagnostic(session, source, qualifiedAccessSource)
    val genericDiagnostic = (qualifiedAccessSource ?: source)?.let { diagnosticSource ->
        when {
            candidate.callInfo.semanticCallKind == AbstractCallKind.DelegatingConstructorCall ->
                CfirErrors.NO_MATCH_FUNCTION_DECLARATION_FOR_CALL.on(
                    diagnosticSource.firstCharacterDiagnosticSource(),
                    session,
                )

            candidateSymbol.cfir is org.cangnova.cangjie.cfir.declarations.CfirConstructor ||
                    candidateSymbol.cfir is CfirEnumConstructor ->
                CfirErrors.NO_CONSTRUCTOR.on(diagnosticSource, session)

            else -> CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, candidateSymbol.debugName, null, session)
        }
    }

    var suppressedRangeArgumentMismatch = false
    var suppressedErrorTypeInArguments = false
    var suppressedErrorTypeInCandidateSignature = false
    var suppressedAmbiguousClassifierTypeInCandidateSignature = false
    var invalidCallableReturnTypeInOverloadSet = false
    val hasErrorTypeInArguments = candidate.diagnostics.any {
        it == ErrorTypeInArguments ||
            it == ErrorTypeInCandidateSignature ||
            it == AmbiguousClassifierTypeInCandidateSignature
    }
    val diagnostics = candidate.diagnostics.filter { !it.isSuccess }.coalesceArgumentTypeMismatches().mapNotNull { rootCause ->
        when (rootCause) {
            ErrorTypeInArguments -> {
                suppressedErrorTypeInArguments = true
                null
            }

            ErrorTypeInCandidateSignature -> {
                suppressedErrorTypeInCandidateSignature = true
                null
            }

            AmbiguousClassifierTypeInCandidateSignature -> {
                suppressedAmbiguousClassifierTypeInCandidateSignature = true
                null
            }

            InvalidCallableReturnTypeInOverloadSet -> {
                invalidCallableReturnTypeInOverloadSet = true
                null
            }

            is UnsuccessfulCallableReferenceArgument -> null

            is ArgumentPassedTwice -> CfirErrors.ARGUMENT_PASSED_TWICE.on(
                rootCause.source,
                session,
            )

            is ArgumentTypeMismatch -> {
                candidate.multiLambdaBuilderInferenceDiagnosticFor(rootCause.argument, source, qualifiedAccessSource, session)
                    ?.let { return@mapNotNull it }
                if (!candidate.hasExplicitTypeArgumentsInCall()) {
                    rootCause.argument.genericInferenceArgumentMismatchDiagnostic(session)
                        ?.let { return@mapNotNull it }
                    rootCause.argument.genericInferenceCallTypeMismatchDiagnostic(session)
                        ?.let { return@mapNotNull it }
                }
                if (!candidate.usedOuterCs &&
                    rootCause.systemHadContradiction &&
                    !candidate.hasExplicitTypeArgumentsInCall()
                ) {
                    return@mapNotNull null
                }
                if (candidate.hasGenericCallNotEnoughTypeInformation(session) && rootCause.argument is CfirAnonymousFunctionExpression) {
                    return@mapNotNull null
                }

                val expectedType = rootCause.expectedType.substituteTypeVariableTypes(candidate, session)
                val actualType =
                    if (rootCause.argument is CfirAnonymousFunctionExpression && rootCause.argument.coneTypeOrNull?.isError == false) {
                        rootCause.argument.coneTypeOrNull!!
                    } else {
                        rootCause.actualType.substituteTypeVariableTypes(candidate, session)
                    }

                candidate.invalidBinaryOperatorDiagnosticForOperatorCall(source, qualifiedAccessSource, session)
                    ?.let { return@mapNotNull it }

                argumentTypeMismatch(
                    source = rootCause.argument.source ?: source,
                    argument = rootCause.argument,
                    expectedType = expectedType,
                    actualType = actualType,
                    isMismatchDueToNullability = rootCause.isMismatchDueToNullability,
                    anonymousFunction = rootCause.anonymousFunctionIfReturnExpression,
                    candidate = candidate,
                    session = session,
                ).also { diagnostic ->
                    if (diagnostic == null &&
                        expectedType.rangeElementTypeOrNull() != null &&
                        actualType.rangeElementTypeOrNull() != null
                    ) {
                        suppressedRangeArgumentMismatch = true
                    }
                }
            }

            is LambdaParameterCountMismatch -> CfirErrors.PARAM_COUNT_MISMATCH.on(
                rootCause.anonymousFunction.lambdaParameterListSource()
                    ?: rootCause.anonymousFunction.source
                    ?: source
                    ?: qualifiedAccessSource
                    ?: return@mapNotNull null,
                rootCause.expectedCount,
                rootCause.actualCount,
                session,
            )

            is LambdaParameterTypeMismatch -> CfirErrors.TYPE_MISMATCH.on(
                rootCause.parameter.source
                    ?: rootCause.anonymousFunction.source
                    ?: source
                    ?: qualifiedAccessSource
                    ?: return@mapNotNull null,
                rootCause.expectedType.substituteTypeVariableTypes(candidate, session),
                rootCause.actualType.substituteTypeVariableTypes(candidate, session),
                false,
                session,
            )

            is AmbiguousArgumentType -> {
                val diagnosticSource = rootCause.callSite.source
                    ?: qualifiedAccessSource
                    ?: source
                    ?: candidate.callInfo.callSite.source
                    ?: return@mapNotNull null
                CfirErrors.AMBIGUOUS_ARG_TYPE.on(
                    diagnosticSource.firstCharacterDiagnosticSource(),
                    candidate.callInfo.name,
                    session,
                )
            }

            is InferenceConstraintError -> {
                if (hasErrorTypeInArguments) return@mapNotNull null
                if (candidate.diagnostics.any { it is TooManyArguments }) return@mapNotNull null
                if (candidate.callInfo.arguments.any { it.containsErrorDiagnosticInArgument() }) return@mapNotNull null
                if (candidate.hasExplicitTypeArgumentsInCall() &&
                    candidate.diagnostics.any { it is ArgumentTypeMismatch }
                ) {
                    return@mapNotNull null
                }
                if (candidate.symbol.let { it as? CfirCallableSymbol<*> }?.cfir?.typeParameters?.isNotEmpty() == true &&
                    !candidate.hasExplicitTypeArgumentsInCall()
                ) {
                    ConeConstraintSystemHasContradiction(candidate)
                        .genericInferenceErrorDiagnostic(source, qualifiedAccessSource, session)
                } else {
                    CfirErrors.NEW_INFERENCE_ERROR.on(
                        qualifiedAccessSource ?: source ?: candidate.callInfo.callSite.source ?: return@mapNotNull null,
                        rootCause.message,
                        session,
                    )
                }
            }

            is MixingNamedAndPositionalArguments -> CfirErrors.MIXING_NAMED_AND_POSITIONAL_ARGUMENTS.on(
                rootCause.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
                session,
            )

            is NamedArgumentsNotAllowed -> CfirErrors.NAMED_ARGUMENTS_NOT_ALLOWED.on(
                rootCause.source,
                rootCause.targetDescription,
                session,
            )

            is NamedParameterNotFound -> CfirErrors.NAMED_PARAMETER_NOT_FOUND.on(
                rootCause.source,
                rootCause.name,
                session,
            )

            is NeedNamedArgument -> CfirErrors.NEED_NAMED_ARGUMENT.on(
                rootCause.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
                rootCause.parameter.name,
                session,
            )

            is TrailingLambdaCannotUsedForNonFunction ->
                CfirErrors.TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION.on(
                    rootCause.source,
                    rootCause.parameterType,
                    session,
                )

            is WrongArgumentCount -> CfirErrors.GENERIC_ARGUMENT_NO_MATCH.on(
                qualifiedAccessSource ?: source ?: candidate.callInfo.callSite.source ?: return@mapNotNull null,
                session,
            )

            is NoValueForParameter -> CfirErrors.NO_VALUE_FOR_PARAMETER.on(
                qualifiedAccessSource ?: source ?: return@mapNotNull null,
                rootCause.valueParameter.name,
                session,
            )

            is TooManyArguments -> CfirErrors.TOO_MANY_ARGUMENTS.on(
                rootCause.tooManyArgumentsSource(
                    source,
                    qualifiedAccessSource,
                    preferCallSource = candidate.prefersCallSourceForTooManyArguments(),
                ) ?: return@mapNotNull null,
                rootCause.targetName,
                session,
            )

            is WrongNumberOfArguments -> CfirErrors.WRONG_NUMBER_OF_ARGUMENTS.on(
                rootCause.source,
                session,
            ).also {
                System.err.println(
                    "CFIR_VARIADIC_ARITY_DIAGNOSTIC root=${rootCause.source.startOffset}..${rootCause.source.endOffset} " +
                        "source=${source?.startOffset}..${source?.endOffset} " +
                        "call=${qualifiedAccessSource?.startOffset}..${qualifiedAccessSource?.endOffset}"
                )
            }

            else -> genericDiagnostic
        }
    }

    if (diagnostics.isNotEmpty()) {
        val hasCallLevelArityDiagnostic = candidate.diagnostics.any { it is WrongNumberOfArguments }
        return listOfNotNull(
            noMatchingInvokeDiagnostic.takeUnless { hasCallLevelArityDiagnostic },
            candidate.parametersAndArgumentsMismatchDiagnostic(session)
                .takeUnless { hasCallLevelArityDiagnostic },
        ) + diagnostics
    }
    if (suppressedRangeArgumentMismatch) return listOfNotNull(noMatchingInvokeDiagnostic)
    if (suppressedAmbiguousClassifierTypeInCandidateSignature) {
        return listOfNotNull(
            noMatchingInvokeDiagnostic,
            candidate.ambiguousClassifierSignatureNoMatchDiagnostic(session),
        )
    }
    if (suppressedErrorTypeInCandidateSignature) return listOfNotNull(noMatchingInvokeDiagnostic)
    if (suppressedErrorTypeInArguments) return listOfNotNull(noMatchingInvokeDiagnostic)
    if (invalidCallableReturnTypeInOverloadSet) {
        return listOfNotNull(candidate.invalidReturnTypeOverloadNoMatchDiagnostic(session))
    }

    noMatchingInvokeDiagnostic?.let { return listOf(it) }

    genericInferenceInapplicableDiagnostic(session, source, qualifiedAccessSource)
        ?.let { return listOf(it) }

    val diagnosticSource = qualifiedAccessSource ?: source ?: return emptyList()
    return listOfNotNull(CfirErrors.UNRESOLVED_REFERENCE.on(diagnosticSource, candidateSymbol.debugName, null, session))
}

/** 错误返回类型使整个 overload 集合失效时，在 callable 名称上报告调用级 no-match。 */
private fun AbstractCallCandidate<*>.invalidReturnTypeOverloadNoMatchDiagnostic(session: CfirSession): CjDiagnostic? {
    if (callInfo.semanticCallKind != AbstractCallKind.Function) return null
    val call = callInfo.callSite as? CfirFunctionCall ?: return null
    val calleeSource = call.calleeReference.source ?: return null
    return CfirErrors.NO_MATCH_FUNCTION_DECLARATION_FOR_CALL.on(calleeSource, session)
}

/** 候选参数签名含 classifier 类型歧义时，在普通函数调用 callee 上保留官方 no-match。 */
private fun AbstractCallCandidate<*>.ambiguousClassifierSignatureNoMatchDiagnostic(session: CfirSession): CjDiagnostic? {
    if (callInfo.semanticCallKind != AbstractCallKind.Function) return null
    val call = callInfo.callSite as? CfirFunctionCall ?: return null
    val calleeSource = call.calleeReference.source ?: return null
    val source = CjOffsetsOnlySourceElement(
        startOffset = calleeSource.startOffset,
        endOffset = (calleeSource.startOffset + callInfo.name.asString().length)
            .coerceAtMost(call.source?.endOffset ?: calleeSource.endOffset),
    )
    return CfirErrors.NO_MATCH_FUNCTION_DECLARATION_FOR_CALL.on(source, session)
}

/**
 * Lambda 参数个数错误在 IDE 中应覆盖完整参数列表，而不是整个 lambda 表达式。
 */
private fun CfirAnonymousFunction.lambdaParameterListSource(): AbstractCjSourceElement? {
    val parameterSources = valueParameters.mapNotNull { it.source }
    val first = parameterSources.firstOrNull() ?: return null
    val last = parameterSources.last()
    return CjOffsetsOnlySourceElement(first.startOffset, last.endOffset)
}

/**
 * callable reference 实参解析失败时，保留函数引用自己的 no-match 诊断，
 * 不能把同一根因继续泛化成外层调用的 TYPE_MISMATCH。
 */
private fun AbstractCallCandidate<*>.unsuccessfulCallableReferenceArgumentDiagnostics(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): List<CjDiagnostic>? {
    val diagnostic = diagnostics.filterIsInstance<UnsuccessfulCallableReferenceArgument>().firstOrNull()
        ?: return null

    // receiver / qualifier 已拥有结构性主错误时，函数引用 no-match 只是同一错误的派生结果。
    // 该层统一拥有候选失败到用户诊断的映射，因此也应在这里截断级联诊断。
    if (diagnostic.argument.hasPrimaryErrorInCallableReferenceReceiver()) return emptyList()

    // 裸泛型函数引用已经在 argument 的 error reference 上报告“缺少类型实参”。
    // 该 structured provenance 只负责阻断任意外层调用的派生推断/无匹配诊断，
    // 不属于 enum constructor 的专用恢复规则。
    if (diagnostic.failureKind == CallableReferenceFailureKind.GENERIC_TYPE_ARGUMENT_REQUIRED) {
        return emptyList()
    }

    if (diagnostic.failureKind == CallableReferenceFailureKind.AMBIGUITY) {
        // 普通声明调用需要同时报告外层 no-match 与内层函数引用歧义；函数值 synthetic invoke
        // 只保留内层歧义，避免为 `f2(a.g)` 额外制造一个官方不存在的外层调用错误。
        if (symbol !is CfirFunctionSymbol<*>) return emptyList()
        val callDiagnosticSource = source ?: qualifiedAccessSource ?: callInfo.callSite.source ?: return emptyList()
        return listOfNotNull(
            CfirErrors.NO_MATCH_FUNCTION_DECLARATION_FOR_CALL.on(
                callDiagnosticSource.firstCharacterDiagnosticSource(),
                session,
            )
        )
    }

    // 裸函数名尚未进入目标类型判定时，主诊断仍属于该引用的 AMBIGUOUS_USE。
    if (diagnostic.argument.hasAmbiguousCalleeReference()) return emptyList()

    val diagnosticSource = diagnostic.argument.source ?: source ?: qualifiedAccessSource ?: callInfo.callSite.source ?: return null
    return listOfNotNull(
        CfirErrors.NO_MATCH_FUNCTION_DECLARATION_FOR_REF.on(
            diagnosticSource.firstCharacterDiagnosticSource(),
            session,
        )
    )
}

/**
 * 判断作为函数引用使用的表达式是否已经携带歧义引用诊断。
 */
private fun CfirExpression.hasAmbiguousCalleeReference(): Boolean {
    val reference = (this as? CfirResolvable)?.calleeReference as? CfirErrorNamedReference ?: return false
    return reference.diagnostic is ConeAmbiguityError
}

/** 判断 callable reference 的 receiver / qualifier 子树是否已经携带更具体的主错误。 */
private fun CfirExpression.hasPrimaryErrorInCallableReferenceReceiver(): Boolean {
    if (containsErrorDiagnosticInArgument()) return true
    val receiver = (this as? CfirQualifiedAccessExpression)?.explicitReceiver ?: return false
    return receiver.containsErrorDiagnosticInArgument()
}

/**
 * 映射仓颉普通可变参数调用中的参数映射诊断。
 */
private fun List<ResolutionDiagnostic>.mapCangjieVariadicRegularCallDiagnostics(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
    preferCallSourceForTooManyArguments: Boolean = false,
): List<CjDiagnostic> = coalesceArgumentMappingDiagnostics().mapNotNull { diagnostic ->
    when (diagnostic) {
        is ArgumentPassedTwice -> CfirErrors.ARGUMENT_PASSED_TWICE.on(
            diagnostic.source,
            session,
        )

        is MixingNamedAndPositionalArguments -> CfirErrors.MIXING_NAMED_AND_POSITIONAL_ARGUMENTS.on(
            diagnostic.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
            session,
        )

        is NamedArgumentsNotAllowed -> CfirErrors.NAMED_ARGUMENTS_NOT_ALLOWED.on(
            diagnostic.source,
            diagnostic.targetDescription,
            session,
        )

        is NamedParameterNotFound -> CfirErrors.NAMED_PARAMETER_NOT_FOUND.on(
            diagnostic.source,
            diagnostic.name,
            session,
        )

        is NeedNamedArgument -> CfirErrors.NEED_NAMED_ARGUMENT.on(
            diagnostic.argument.source ?: source ?: qualifiedAccessSource ?: return@mapNotNull null,
            diagnostic.parameter.name,
            session,
        )

        is TrailingLambdaCannotUsedForNonFunction ->
            CfirErrors.TRAILING_LAMBDA_CANNOT_USED_FOR_NON_FUNCTION.on(
                diagnostic.source,
                diagnostic.parameterType,
                session,
            )

        is NoValueForParameter -> CfirErrors.NO_VALUE_FOR_PARAMETER.on(
            qualifiedAccessSource ?: source ?: return@mapNotNull null,
            diagnostic.valueParameter.name,
            session,
        )

        is TooManyArguments -> CfirErrors.TOO_MANY_ARGUMENTS.on(
            diagnostic.tooManyArgumentsSource(
                source,
                qualifiedAccessSource,
                preferCallSource = preferCallSourceForTooManyArguments,
            ) ?: return@mapNotNull null,
            diagnostic.targetName,
            session,
        )

        is WrongNumberOfArguments -> CfirErrors.WRONG_NUMBER_OF_ARGUMENTS.on(
            diagnostic.source,
            session,
        )

        else -> null
    }
}

/**
 * 零形参候选收到实参时，参数个数错误属于完整调用 `foo(...)` / `A(...)`。
 */
private fun AbstractCallCandidate<*>.prefersCallSourceForTooManyArguments(): Boolean {
    val declaration = symbol.takeIf { it.isBound }?.cfir ?: return false
    return when (declaration) {
        // 构造器即使在类型层次上也是 CfirFunction，extra argument 仍应锚定具体实参；
        // 否则零参委托构造调用会错误地把整个 `this(...)` / `super(...)` 标红。
        is CfirConstructor -> false
        is CfirFunction -> declaration.valueParameters.isEmpty()
        is CfirEnumConstructor -> declaration.valueParameters.isEmpty()
        else -> false
    }
}

/**
 * 根据调用类别选择 `TOO_MANY_ARGUMENTS` 的诊断 source。
 */
private fun TooManyArguments.tooManyArgumentsSource(
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
    preferCallSource: Boolean,
): CjSourceElement? {
    if (preferCallSource) return qualifiedAccessSource ?: source ?: argument.source
    return argument.source ?: source ?: qualifiedAccessSource
}

/**
 * 官方参数个数错误是调用级诊断；本项目暂以 `NO_VALUE_FOR_PARAMETER`
 * 表达缺参语义时，同一候选上多个缺失形参应合并成一个用户可见诊断。
 */
private fun List<ResolutionDiagnostic>.coalesceArgumentMappingDiagnostics(): List<ResolutionDiagnostic> {
    var reportedNoValueForParameter = false
    return filter { diagnostic ->
        if (diagnostic !is NoValueForParameter) return@filter true
        if (reportedNoValueForParameter) return@filter false
        reportedNoValueForParameter = true
        true
    }
}

/**
 * 提取 builder inference 多 lambda 限制相关诊断。
 */
private fun ConeConstraintSystemHasContradiction.multiLambdaBuilderInferenceDiagnostics(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): List<CjDiagnostic> {
    return candidate.errors
        .filterIsInstance<ConstraintSystemError>()
        .filter {
            it is AnonymousFunctionBasedMultiLambdaBuilderInferenceRestriction ||
                it is MultiLambdaBuilderInferenceRestriction<*>
        }
        .mapNotNull { error ->
            error.mapConstraintSystemError(
                source = source,
                qualifiedAccessSource = qualifiedAccessSource,
                session = session,
                candidate = candidate,
            )
        }
}

/**
 * 针对指定 lambda 实参查找并映射 multi-lambda builder inference 限制。
 */
private fun AbstractCallCandidate<*>.multiLambdaBuilderInferenceDiagnosticFor(
    argument: org.cangnova.cangjie.cfir.CfirElement,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val anonymousFunction = (argument as? CfirAnonymousFunctionExpression)?.anonymousFunction ?: return null
    val restriction = errors
        .filterIsInstance<AnonymousFunctionBasedMultiLambdaBuilderInferenceRestriction>()
        .firstOrNull { it.anonymous == anonymousFunction }
        ?: return null
    return restriction.mapConstraintSystemError(
        source = source,
        qualifiedAccessSource = qualifiedAccessSource,
        session = session,
        candidate = this,
    )
}

/**
 * 在不可适用候选上构造泛型推断失败诊断。
 */
private fun ConeInapplicableCandidateError.genericInferenceInapplicableDiagnostic(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): CjDiagnostic? {
    val contradiction = ConeConstraintSystemHasContradiction(candidate)
    val hasInferenceConstraintDiagnostic = !candidate.hasExplicitTypeArgumentsInCall() &&
        candidate.diagnostics.any { it is InferenceConstraintError }
    if (!candidate.hasGenericInferenceArgumentMismatch(session) &&
        !candidate.hasGenericCallNotEnoughTypeInformation(session) &&
        !contradiction.hasGenericInferenceConstraintMismatch(session) &&
        !hasInferenceConstraintDiagnostic
    ) {
        return null
    }
    return contradiction.genericInferenceErrorDiagnostic(source, qualifiedAccessSource, session)
}

/**
 * 将 `invoke` operator 候选不可适用映射为 no matching invoke 诊断。
 */
private fun ConeInapplicableCandidateError.mapNoMatchingInvokeOperatorDiagnostic(
    session: CfirSession,
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): CjDiagnostic? {
    if (candidateSymbol.memberDeclarationNameOrNull() != OperatorNameConventions.INVOKE) return null
    val receiverType = candidate.callInfo.explicitReceiver?.coneTypeOrNull ?: return null
    if (receiverType is ConeErrorType) return null
    val diagnosticSource = qualifiedAccessSource ?: source ?: return null
    return CfirErrors.NO_MATCHING_OPERATOR_INVOKE.on(
        diagnosticSource,
        diagnosticSource.text?.toString().orEmpty(),
        receiverType,
        session,
    )
}

/**
 * 根据实参类型不匹配上下文选择最具体的实参诊断。
 */
private fun argumentTypeMismatch(
    source: CjSourceElement?,
    argument: CfirExpression?,
    expectedType: ConeCangJieType,
    actualType: ConeCangJieType,
    isMismatchDueToNullability: Boolean,
    anonymousFunction: CfirFunction?,
    candidate: AbstractCallCandidate<*>,
    session: CfirSession,
): CjDiagnostic? {
    if (source == null) return null
    if (expectedType.containsErrorType() || actualType.containsErrorType()) return null
    if (expectedType.rangeElementTypeOrNull() != null &&
        actualType.rangeElementTypeOrNull() != null
    ) {
        return null
    }
    specificTypeMismatchDiagnostic(
        source = source,
        expectedType = expectedType,
        actualType = actualType,
        expression = argument,
        session = session,
    )?.let { return it }

    if (argument?.isBareFunctionReferenceValue() == true) {
        return CfirErrors.TYPE_MISMATCH.on(
            source,
            expectedType,
            actualType,
            isMismatchDueToNullability,
            session,
        )
    }

    // lambda 自身是完整函数值表达式；当其不能作为普通实参适配目标类型时，
    // 官方按整个表达式报告 TYPE_MISMATCH，而不是声明调用实参的 ARGUMENT_TYPE_MISMATCH。
    if (argument is CfirAnonymousFunctionExpression) {
        return CfirErrors.TYPE_MISMATCH.on(
            source,
            expectedType,
            actualType,
            isMismatchDueToNullability,
            session,
        )
    }

    if (candidate.hasExplicitCallableTypeArgumentsInCall()) {
        return CfirErrors.TYPE_MISMATCH.on(
            source,
            expectedType,
            actualType,
            isMismatchDueToNullability,
            session,
        )
    }

    if (anonymousFunction != null) {
        val lambdaSource = anonymousFunction.source ?: source
        return CfirErrors.TYPE_MISMATCH.on(
            lambdaSource,
            expectedType,
            actualType,
            isMismatchDueToNullability,
            session,
        )
    }

    if (source.isTupleEqualityExpression()) {
        return CfirErrors.TYPE_MISMATCH.on(
            source,
            expectedType,
            actualType,
            isMismatchDueToNullability,
            session,
        )
    }

    // 官方 `ChkLitConstExprOf*` 在字面量与目标标量类型不兼容时报告 `sema_cannot_convert_literal`，
    // 该判定与调用/赋值/返回上下文无关。但 operator 调用另有归类：官方对 `0.1 + 1` 报
    // `sema_invalid_binary_expr`，因此 operator origin 不走字面量分支。
    if (candidate.callInfo.origin != CfirFunctionCallOrigin.Operator) {
        literalConversionDiagnostic(source, expectedType, argument, session)?.let { return it }
    }

    return CfirErrors.ARGUMENT_TYPE_MISMATCH.on(
        source,
        expectedType,
        actualType,
        isMismatchDueToNullability,
        session,
    )
}

/** 裸函数名在目标类型检查中仍是函数引用值，而不是普通调用实参节点。 */
private fun CfirExpression.isBareFunctionReferenceValue(): Boolean {
    val access = unwrapWrappedExpression() as? CfirQualifiedAccessExpression ?: return false
    if (access is CfirFunctionCall) return false
    return access.genericInferenceCallableSymbolOrNull() is CfirFunctionSymbol<*>
}

/**
 * 显式实例化泛型 owner 的构造调用若无法把裸函数引用匹配到非函数形参，
 * 官方除实参 `mismatched types` 外还报告一次 call-level 参数列表不匹配。
 */
private fun AbstractCallCandidate<*>.parametersAndArgumentsMismatchDiagnostic(
    session: CfirSession,
): CjDiagnostic? {
    if (symbol.cfir !is CfirConstructor || !hasExplicitTypeArgumentsInCall()) return null
    val hasFunctionReferenceMappingFailure = diagnostics.filterIsInstance<ArgumentTypeMismatch>().any { diagnostic ->
        diagnostic.argument.isBareFunctionReferenceValue() &&
            diagnostic.actualType.isFunctionTypeLike() &&
            !diagnostic.expectedType.isFunctionTypeLike()
    }
    if (!hasFunctionReferenceMappingFailure) return null

    val calleeSource = (callInfo.callSite as? CfirQualifiedAccessExpression)
        ?.calleeReference
        ?.source
        ?: return null
    val source = CjOffsetsOnlySourceElement(
        startOffset = calleeSource.startOffset,
        endOffset = (calleeSource.startOffset + callInfo.name.asString().length).coerceAtMost(calleeSource.endOffset),
    )
    return CfirErrors.PARAMETERS_AND_ARGUMENTS_MISMATCH.on(source, session)
}

/**
 * 判断 source 是否表示 tuple 之间的 `==` 或 `!=` 表达式。
 */
private fun CjSourceElement.isTupleEqualityExpression(): Boolean {
    val binaryExpression = psi as? CjBinaryExpression
    if (binaryExpression != null) {
        if (binaryExpression.operationToken != CjTokens.EQEQ &&
            binaryExpression.operationToken != CjTokens.EXCLEQ
        ) {
            return false
        }
        return binaryExpression.left is CjTupleExpression && binaryExpression.right is CjTupleExpression
    }

    val sourceText = text?.toString()?.trim() ?: return false
    val operator = when {
        "==" in sourceText -> "=="
        "!=" in sourceText -> "!="
        else -> return false
    }
    val operands = sourceText.split(operator, limit = 2)
    return operands.size == 2 && operands.all { operand ->
        val trimmed = operand.trim()
        trimmed.startsWith("(") && trimmed.endsWith(")") && "," in trimmed
    }
}

/**
 * 二元 operator 调用候选存在但实参类型不适用时，官方 Cangjie 归类为
 * `sema_invalid_binary_expr`，而不是普通函数实参类型不匹配。
 */
private fun AbstractCallCandidate<*>.invalidBinaryOperatorDiagnosticForOperatorCall(
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    if (callInfo.origin != CfirFunctionCallOrigin.Operator) return null
    val operatorToken = OperatorNameConventions.TOKENS_BY_OPERATOR_NAME[callInfo.name] ?: return null
    val leftType = callInfo.explicitReceiver?.coneTypeOrNull ?: return null
    val rightType = callInfo.arguments.singleOrNull()?.coneTypeOrNull ?: return null
    if (leftType.containsErrorType() || rightType.containsErrorType()) return null

    val diagnosticSource = source ?: qualifiedAccessSource ?: callInfo.callSite.source as? CjSourceElement ?: return null
    return CfirErrors.INVALID_BINARY_OPERATOR.on(
        diagnosticSource,
        operatorToken,
        leftType.renderInvalidBinaryOperatorType(session),
        rightType.renderInvalidBinaryOperatorType(session),
        session,
    )
}

/**
 * 提取标准库 Range 类型的元素类型。
 */
private fun ConeCangJieType.rangeElementTypeOrNull(): ConeCangJieType? = when (this) {
    is ConeClassLikeType -> if (classId == StdlibClassIds.Range) typeArguments.singleOrNull()?.type else null
    is ConeStructType -> if (classId == StdlibClassIds.Range) typeArguments.singleOrNull()?.type else null
    is ConeTypeAliasType -> expandedType?.rangeElementTypeOrNull()
    else -> null
}

/**
 * 函数值作为实参时，参数/返回值结构子类型检查失败属于函数类型整体不匹配。
 * 官方 Cangjie 在 `IsFuncSubtype` 失败后仍走 `sema_mismatched_types`，
 * 因此这里不降成普通实参专用诊断。
 */
private fun ConeCangJieType.isFunctionTypeLike(): Boolean = when (this) {
    is ConeFunctionType -> true
    is ConeTypeAliasType -> expandedType?.isFunctionTypeLike() == true
    else -> false
}

/**
 * 将候选歧义错误映射为构造器歧义、函数调用歧义、operator 歧义或基础类型 extend 歧义。
 */
private fun ConeAmbiguityError.mapConeAmbiguityError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): List<CjDiagnostic> {
    if (isErrorArgumentCascade) return emptyList()

    val operatorDiagnosticSource = callOrAssignmentSource ?: source
    if (operatorDiagnosticSource != null) {
        if (isOperatorAmbiguityCascadeFromErrorOperand()) return emptyList()
        invalidBinaryOperatorDiagnosticForOperatorAmbiguity(source, operatorDiagnosticSource, session)?.let { diagnostic ->
            return listOf(diagnostic)
        }
    }

    sharedOverloadArgumentTypeMismatchDiagnostic(session)?.let { return listOf(it) }

    @OptIn(ApplicabilityDetail::class)
    if (!applicability.isSuccess) {
        val candidateDiagnostics = candidatesWithErrors.values.map { coneDiagnostic ->
            coneDiagnostic?.toCfirDiagnostics(
                session = session,
                source = source,
                callOrAssignmentSource = callOrAssignmentSource,
                valueParameter = null,
            ).orEmpty()
        }

        val diagnosticsByKey = candidateDiagnostics
            .map { diagnostics -> diagnostics.distinctBy { it.diagnosticIdentityKey() } }
            .filter { it.isNotEmpty() }
        if (diagnosticsByKey.isNotEmpty()) {
            val sharedDiagnosticKeys = diagnosticsByKey
                .map { diagnostics -> diagnostics.map { it.diagnosticIdentityKey() }.toSet() }
                .reduce { acc, keys -> acc.intersect(keys) }

            val sharedDiagnostics = diagnosticsByKey
                .first()
                .filter { it.diagnosticIdentityKey() in sharedDiagnosticKeys }
            if (sharedDiagnostics.isNotEmpty()) {
                val isInvokeOperatorAmbiguity = candidateSymbols.any { symbol ->
                    symbol.memberDeclarationNameOrNull() == OperatorNameConventions.INVOKE
                }
                if (isInvokeOperatorAmbiguity) {
                    val sharedAnchorKeys = diagnosticsByKey
                        .map { diagnostics -> diagnostics.map { it.diagnosticAnchorKey() }.toSet() }
                        .reduce { acc, keys -> acc.intersect(keys) }
                    val sharedByAnchorDiagnostics = diagnosticsByKey
                        .first()
                        .filter { diagnostic ->
                            diagnostic.diagnosticAnchorKey() in sharedAnchorKeys &&
                                diagnostic.diagnosticIdentityKey() !in sharedDiagnosticKeys
                        }

                    return (sharedDiagnostics + sharedByAnchorDiagnostics)
                        .distinctBy { it.diagnosticAnchorKey() }
                }

                return sharedDiagnostics
            }

            return diagnosticsByKey.first()
        }
    }

    if (candidateSymbols.all { symbol ->
            symbol.cfir is org.cangnova.cangjie.cfir.declarations.CfirConstructor || symbol.cfir is CfirEnumConstructor
        }
    ) {
        if (isClassifierRedeclarationConstructorCascade(session)) return emptyList()
        val diagnosticSource = callOrAssignmentSource ?: source ?: return emptyList()
        return listOfNotNull(CfirErrors.AMBIGUOUS_CONSTRUCTOR_CALL.on(diagnosticSource, name, session))
    }

    val diagnosticSource = callOrAssignmentSource ?: source ?: return emptyList()
    val callLikeProbeSource = source ?: callOrAssignmentSource ?: diagnosticSource
    val psi = callLikeProbeSource.psi
    val isCallLikeContext = psi is CjCallExpression || PsiTreeUtil.getParentOfType(psi, CjCallExpression::class.java, false) != null

    // 检查是否为基本类型扩展歧义
    if (isCallLike || isCallLikeContext) {
        val extendOriginNames = candidateSymbols
            .mapNotNull { symbol -> (symbol as? CfirCallableSymbol<*>)?.primitiveExtendReceiverName() }
        if (extendOriginNames.size >= 2) {
            // 如果候选来自不同的 extend 目标类型，报告 AMBIGUOUS_MATCH_PRIMITIVE_EXTEND
            val distinctOrigins = extendOriginNames.distinct()
            if (distinctOrigins.size >= 2) {
                val primitiveExtendDiagnosticSource = source ?: diagnosticSource
                return listOfNotNull(
                    CfirErrors.AMBIGUOUS_MATCH_PRIMITIVE_EXTEND.on(
                        primitiveExtendDiagnosticSource,
                        name,
                        distinctOrigins.map { it },
                        session,
                    )
                )
            }
        }
    }

    val factory = if (isCallLike || isCallLikeContext) CfirErrors.AMBIGUOUS_FUNCTION_CALL else CfirErrors.AMBIGUOUS_USE
    val ambiguitySource = if (factory == CfirErrors.AMBIGUOUS_USE) {
        val completeTypeUseSource = typeUseSource
        if (completeTypeUseSource != null) {
            CjOffsetsOnlySourceElement(completeTypeUseSource.startOffset, completeTypeUseSource.endOffset)
        } else if (isFunctionValueAmbiguity()) {
            source ?: diagnosticSource
        } else {
            callOrAssignmentSource
                ?.takeIf { source == null || it.startOffset < source.startOffset || it.endOffset > source.endOffset }
                ?.offsetRangeSourceWhenPsiElementIsNarrower()
                ?: source?.qualifiedAmbiguousUseSource()
                ?: callOrAssignmentSource?.qualifiedAmbiguousUseSource()
                ?: diagnosticSource
        }
    } else {
        source ?: diagnosticSource
    }
    return listOfNotNull(factory.on(ambiguitySource, name, session))
}

/**
 * 多候选调用应报告所有候选共同失败的实参，而不是任一候选内部最先检查到的实参。
 *
 * 候选的参数类型不同会让各自首个 mismatch 不同；按原始实参顺序求 mismatch argument
 * 身份交集后，得到的才是 overload 集合无法规约的调用级根因。
 */
private fun ConeAmbiguityError.sharedOverloadArgumentTypeMismatchDiagnostic(
    session: CfirSession,
): CjDiagnostic? {
    val callCandidates = candidatesWithErrors.keys.filterIsInstance<AbstractCallCandidate<*>>()
    if (callCandidates.isEmpty() || callCandidates.size != candidatesWithErrors.size) return null
    val originalArguments = callCandidates.first().callInfo.arguments
    val sharedArgument = originalArguments.firstOrNull { argument ->
        callCandidates.all { candidate ->
            candidate.diagnostics.any { diagnostic ->
                diagnostic is ArgumentTypeMismatch && diagnostic.argument === argument
            }
        }
    } ?: return null
    val candidateMismatches = callCandidates.map { candidate ->
        val mismatch = candidate.diagnostics
            .filterIsInstance<ArgumentTypeMismatch>()
            .first { it.argument === sharedArgument }
        candidate to mismatch
    }
    val substitutedExpectedTypes = candidateMismatches.map { (candidate, mismatch) ->
        mismatch.expectedType.substituteTypeVariableTypes(candidate, session)
    }
    val expectedType = substitutedExpectedTypes.first()
    if (substitutedExpectedTypes.drop(1).any { otherExpectedType ->
            !AbstractTypeChecker.equalTypes(session.typeContext, expectedType, otherExpectedType)
        }
    ) {
        return null
    }
    val (representativeCandidate, mismatch) = candidateMismatches.first()
    val diagnosticSource = sharedArgument.source ?: return null
    val actualType = mismatch.actualType.substituteTypeVariableTypes(representativeCandidate, session)
    if (expectedType.containsErrorType() || actualType.containsErrorType()) return null

    specificTypeMismatchDiagnostic(
        source = diagnosticSource,
        expectedType = expectedType,
        actualType = actualType,
        expression = sharedArgument,
        session = session,
    )?.let { return it }
    return CfirErrors.TYPE_MISMATCH.on(
        diagnosticSource,
        expectedType,
        actualType,
        mismatch.isMismatchDueToNullability,
        session,
    )
}

/**
 * 同一重声明 classifier 产生的多个构造候选只是类型使用歧义的调用级联。
 *
 * provider/constructor scope 可以为每个重声明声明暴露构造器，但官方语义的主错误仍属于
 * classifier 重声明；只有候选全部是同一 [ClassId] 的构造器且冲突追踪器确认该 classifier
 * 确实重声明时，才抑制派生的构造调用歧义。真实的构造器重载歧义继续正常报告。
 */
private fun ConeAmbiguityError.isClassifierRedeclarationConstructorCascade(session: CfirSession): Boolean {
    val ownerClassIds = linkedSetOf<ClassId>()
    for (symbol in candidateSymbols) {
        if (symbol.cfir !is CfirConstructor && symbol.cfir !is CfirEnumConstructor) {
            return false
        }
        val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return false
        ownerClassIds += callableSymbol.callableId.classId ?: return false
    }
    val ownerClassId = ownerClassIds.singleOrNull() ?: return false
    val redeclaredClassifiers = session.nameConflictsTracker
        ?.getClassifierRedeclarations(ownerClassId)
        ?.map { it.classifierSymbol }
        ?.distinct()
        .orEmpty()
    return redeclaredClassifiers.size >= 2
}

/** 将目标函数类型下的多候选结果映射为专用函数引用歧义诊断。 */
private fun ConeAmbiguousFunctionReferenceError.mapAmbiguousFunctionReferenceError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): List<CjDiagnostic> {
    val diagnosticSource = callOrAssignmentSource ?: source ?: return emptyList()
    return listOfNotNull(
        CfirErrors.AMBIGUOUS_FUNCTION_REFERENCE.on(
            diagnosticSource.firstCharacterDiagnosticSource(),
            name,
            session,
        )
    )
}

/**
 * 函数名作为值使用时，歧义属于 selector 函数引用本身。
 *
 * `a.foo<T>` 中 receiver `a` 只负责提供成员 scope；真正歧义的是 `foo<T>` 这个函数值引用。
 */
private fun ConeAmbiguityError.isFunctionValueAmbiguity(): Boolean {
    val callCandidates = candidates.filterIsInstance<AbstractCallCandidate<*>>()
    return callCandidates.isNotEmpty() &&
            !isCallLike &&
            callCandidates.all { candidate ->
                candidate.callInfo.arguments.isEmpty() &&
                        candidate.symbol.takeIf { it.isBound }?.cfir is CfirFunction
            }
}

/**
 * 歧义的裸 qualified access 按官方位置和 IDE 诊断策略标整条访问表达式。
 */
private fun CjSourceElement.qualifiedAmbiguousUseSource(): AbstractCjSourceElement? {
    qualifiedAmbiguousUsePsiSource()?.let { return it }
    qualifiedAmbiguousUseLightTreeSource()?.let { return it }
    return qualifiedAmbiguousUseTextSource()
}

/**
 * PSI fake source 可能用 selector PSI 携带整条 qualified access 的自定义 offsets。
 * 渲染诊断时必须尊重 offsets，否则会退回只标 selector。
 */
private fun CjSourceElement.offsetRangeSourceWhenPsiElementIsNarrower(): AbstractCjSourceElement {
    val psi = psi ?: return this
    val range = psi.textRange
    if (startOffset == range.startOffset && endOffset == range.endOffset) return this
    return CjOffsetsOnlySourceElement(startOffset, endOffset)
}

/**
 * PSI 路径：若当前 source 是 qualified access 的 selector，扩展到顶层 selector qualified expression。
 */
private fun CjSourceElement.qualifiedAmbiguousUsePsiSource(): AbstractCjSourceElement? {
    val sourcePsi = psi ?: return null
    val sourceStart = startOffset
    val sourceEnd = endOffset

    fun CjQualifiedExpression.containsSourceAnchor(): Boolean =
        textRange.containsRange(sourceStart, sourceEnd) &&
            selectorExpression?.textRange?.containsRange(sourceStart, sourceEnd) == true

    var selected = (sourcePsi as? CjQualifiedExpression)?.takeIf { it.containsSourceAnchor() }
    var current = PsiTreeUtil.getParentOfType(sourcePsi, CjQualifiedExpression::class.java, false)
    while (current != null && current.containsSourceAnchor()) {
        selected = current
        current = PsiTreeUtil.getParentOfType(current, CjQualifiedExpression::class.java, true)
    }
    return (selected as? PsiElement)?.toCjPsiSourceElement()
}

/**
 * Light-tree 路径：沿 source 的父链寻找覆盖当前 selector 的 DOT_QUALIFIED_EXPRESSION。
 */
private fun CjSourceElement.qualifiedAmbiguousUseLightTreeSource(): AbstractCjSourceElement? {
    var node = lighterASTNode
    var selected: LighterASTNode? = null
    while (true) {
        if (node.tokenType == CjNodeTypes.DOT_QUALIFIED_EXPRESSION &&
            treeStructure.getStartOffset(node) < startOffset &&
            treeStructure.getEndOffset(node) >= endOffset
        ) {
            selected = node
        }
        val parent = treeStructure.getParent(node) ?: break
        node = parent
    }
    val qualifiedNode = selected ?: return null
    return CjOffsetsOnlySourceElement(
        startOffset = treeStructure.getStartOffset(qualifiedNode),
        endOffset = treeStructure.getEndOffset(qualifiedNode),
    )
}

/**
 * 最后兜住 offsets-only selector source：从文件文本中向左扩到限定访问起点。
 */
private fun CjSourceElement.qualifiedAmbiguousUseTextSource(): AbstractCjSourceElement? {
    val fileText = treeStructure.toString(treeStructure.root).toString()
    if (startOffset <= 1 || endOffset > fileText.length) return null
    if (fileText[startOffset - 1] != '.') return null

    var index = startOffset - 2
    while (index >= 0 && fileText[index].isQualifiedNamePart()) {
        index--
    }
    val qualifiedStart = index + 1
    if (qualifiedStart >= startOffset) return null
    return CjOffsetsOnlySourceElement(
        startOffset = qualifiedStart,
        endOffset = endOffset,
    )
}

/**
 * 判断字符是否可以出现在限定名片段中。
 */
private fun Char.isQualifiedNamePart(): Boolean =
    isLetterOrDigit() || this == '_' || this == '$' || this == '.'

/**
 * 二元 operator 的候选歧义如果由错误操作数触发，只保留操作数自己的根诊断。
 */
private fun ConeAmbiguityError.isOperatorAmbiguityCascadeFromErrorOperand(): Boolean {
    val candidate = candidates.filterIsInstance<AbstractCallCandidate<*>>()
        .firstOrNull { candidate ->
            candidate.callInfo.origin == CfirFunctionCallOrigin.Operator &&
                    candidate.callInfo.arguments.size == 1
        }
        ?: return false
    val leftType = candidate.callInfo.explicitReceiver?.coneTypeOrNull
    val rightType = candidate.callInfo.arguments.singleOrNull()?.coneTypeOrNull
    return leftType?.containsErrorType() == true || rightType?.containsErrorType() == true
}

/**
 * 将二元 operator 候选歧义中特定的 primitive operator 情况映射为 invalid binary operator。
 */
private fun ConeAmbiguityError.invalidBinaryOperatorDiagnosticForOperatorAmbiguity(
    source: CjSourceElement?,
    diagnosticSource: CjSourceElement,
    session: CfirSession,
): CjDiagnostic? {
    val operatorToken = OperatorNameConventions.TOKENS_BY_OPERATOR_NAME[name] ?: return null
    val candidate = candidates.filterIsInstance<AbstractCallCandidate<*>>()
        .firstOrNull { candidate ->
            candidate.callInfo.origin == CfirFunctionCallOrigin.Operator &&
                    candidate.callInfo.arguments.size == 1
        } ?: return null
    val leftType = candidate.callInfo.explicitReceiver?.coneTypeOrNull ?: return null
    val rightType = candidate.callInfo.arguments.singleOrNull()?.coneTypeOrNull ?: return null
    if (leftType !is ConePrimitiveType || rightType !is ConePrimitiveType) return null
    return CfirErrors.INVALID_BINARY_OPERATOR.on(
        source ?: diagnosticSource,
        operatorToken,
        leftType.renderInvalidBinaryOperatorType(session),
        rightType.renderInvalidBinaryOperatorType(session),
        session,
    )
}

/**
 * 获取 primitive extend 候选的接收者类型名称。
 */
private fun CfirCallableSymbol<*>.primitiveExtendReceiverName(): Name? {
    val ownerExtend = getContainingExtend() ?: return null
    val receiverType = ownerExtend.extendedTypeRef.coneTypeOrNull as? ConePrimitiveType ?: return null
    if (receiverType.kind.isIdeal) return null
    return receiverType.kind.classId.shortClassName
}

/**
 * 诊断去重使用的完整身份 key。
 *
 * @property factoryName 诊断工厂名称。
 * @property message 渲染后的诊断消息。
 * @property startOffset 首个诊断范围起始偏移。
 * @property endOffset 首个诊断范围结束偏移。
 */
private data class DiagnosticIdentityKey(
    /** 诊断工厂名称。 */
    val factoryName: String,
    /** 渲染后的诊断消息。 */
    val message: String,
    /** 首个诊断范围起始偏移。 */
    val startOffset: Int,
    /** 首个诊断范围结束偏移。 */
    val endOffset: Int,
)

/**
 * 诊断锚点去重 key，只比较工厂和 source 范围。
 *
 * @property factoryName 诊断工厂名称。
 * @property startOffset 首个诊断范围起始偏移。
 * @property endOffset 首个诊断范围结束偏移。
 */
private data class DiagnosticAnchorKey(
    /** 诊断工厂名称。 */
    val factoryName: String,
    /** 首个诊断范围起始偏移。 */
    val startOffset: Int,
    /** 首个诊断范围结束偏移。 */
    val endOffset: Int,
)

/**
 * 构造当前诊断的完整身份 key。
 */
private fun CjDiagnostic.diagnosticIdentityKey(): DiagnosticIdentityKey =
    DiagnosticIdentityKey(
        factoryName = factoryName,
        message = renderMessage(),
        startOffset = firstRange.startOffset,
        endOffset = firstRange.endOffset,
    )

/**
 * 构造当前诊断的锚点 key。
 */
private fun CjDiagnostic.diagnosticAnchorKey(): DiagnosticAnchorKey =
    DiagnosticAnchorKey(
        factoryName = factoryName,
        startOffset = firstRange.startOffset,
        endOffset = firstRange.endOffset,
    )

/**
 * 将 unresolved name 错误映射为成员缺失、operator 错误、extend super 或普通 unresolved reference。
 */
private fun ConeUnresolvedNameError.mapConeUnresolvedNameError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): List<CjDiagnostic> {
    mapExtendSuperDiagnostic(source, callOrAssignmentSource, session)?.let { diagnostic ->
        return listOf(diagnostic)
    }
    mapGenericUpperBoundAccessDiagnostic(source, callOrAssignmentSource, session)?.let { diagnostic ->
        return listOf(diagnostic)
    }
    mapSubscriptOperatorDiagnostic(source, callOrAssignmentSource, session)?.let { diagnostic ->
        return listOf(diagnostic)
    }

    // 一元运算符解析失败：有 operator 和 receiverType 但无参数
    mapInvalidUnaryExprDiagnostic(source, callOrAssignmentSource, session)?.let { diagnostic ->
        return listOf(diagnostic)
    }

    // 当有明确名义接收者类型但成员未找到时，优先报告 NOT_MEMBER_OF
    mapNotMemberOfDiagnostic(source, callOrAssignmentSource, session)?.let { diagnostic ->
        return listOf(diagnostic)
    }

    val diagnosticSource = source ?: callOrAssignmentSource ?: return emptyList()
    if (isBinaryOperatorCascadeFromErrorOperand()) return emptyList()
    buildInvalidBinaryOperatorDiagnostic(diagnosticSource, session)?.let { diagnostic ->
        return listOf(diagnostic)
    }

    return listOfNotNull(
        CfirErrors.UNRESOLVED_REFERENCE.on(
            diagnosticSource,
            name.asString(),
            operator,
            session,
        ),
    )
}

/**
 * `[]` / `[]=` 在 resolve 中都会先降成 operator 调用；
 * 这里把针对 `*operator_get` / `*operator_set` 的 unresolved 收束回语法级诊断。
 */
private fun ConeUnresolvedNameError.mapSubscriptOperatorDiagnostic(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    if (operator != "[]") return null
    val diagnosticSource = source ?: callOrAssignmentSource ?: return null
    val receiver = receiverType ?: ConeErrorType(ConeSimpleDiagnostic("unknown subscript receiver type"))

    return if (name == OperatorNameConventions.SET ||
        diagnosticSource.isAssignmentLeftHandSide() ||
        callOrAssignmentSource.isAssignmentExpression()
    ) {
        CfirErrors.CANNOT_ASSIGN_TO_SUBSCRIPT.on(diagnosticSource, session)
    } else {
        CfirErrors.INVALID_SUBSCRIPT_EXPR.on(
            diagnosticSource,
            receiver,
            argumentTypes.joinToString(prefix = "[", postfix = "]") {
                it.renderInvalidBinaryOperatorType(session)
            },
            session,
        )
    }
}

/**
 * 当接收者是类型参数而名称解析失败时，我们优先把它归类为“upper bounds 中没有该成员/方法”，
 * 而不是继续落到通用 unresolved。
 */
private fun ConeUnresolvedNameError.mapGenericUpperBoundAccessDiagnostic(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    if (operator != null) return null
    val typeParameterType = receiverType as? ConeTypeParameterType ?: return null
    val diagnosticSource = source ?: callOrAssignmentSource ?: return null
    val missingName = name
    val typeParameterName = typeParameterType.lookupTag.name

    val hostText = callOrAssignmentSource?.text?.toString().orEmpty()
    val looksLikeMethodCall = argumentTypes.isNotEmpty() || hostText.contains("${missingName.asString()}(")

    return if (!looksLikeMethodCall) {
        CfirErrors.GENERIC_NO_MEMBER_MATCH_IN_UPPER_BOUNDS.on(
            diagnosticSource,
            missingName,
            typeParameterName,
            session,
        )
    } else {
        CfirErrors.GENERIC_NO_METHOD_MATCH_IN_UPPER_BOUNDS.on(
            diagnosticSource,
            missingName,
            typeParameterName,
            session,
        )
    }
}

/**
 * 当接收者类型存在、非类型参数且是名义类型时，将 unresolved name 映射为 NOT_MEMBER_OF。
 *
 * 对齐官方 `DiagMemberAccessNotFound`：只有 `baseExpr->ty->IsNominal()` 时才报告
 * `sema_not_member_of`；primitive、tuple、function 等非名义类型继续报告 unresolved。
 */
private fun ConeUnresolvedNameError.mapNotMemberOfDiagnostic(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val receiver = receiverType ?: return null
    // 类型参数接收者已由 mapGenericUpperBoundAccessDiagnostic 处理
    if (receiver is ConeTypeParameterType) return null
    // 二元运算符已由 buildInvalidBinaryOperatorDiagnostic 处理
    if (operator != null) return null

    val nominalReceiver = receiver.fullyExpandedType(session)
    if (nominalReceiver !is ConeClassLikeType &&
        nominalReceiver !is ConeStructType &&
        nominalReceiver !is ConeEnumType
    ) {
        return null
    }

    val diagnosticSource = source ?: callOrAssignmentSource ?: return null
    val typeName = nominalReceiver.classIdOrPrimitiveClassId?.shortClassName ?: return null
    val kind = if (argumentTypes.isNotEmpty()) "method" else "member"

    return CfirErrors.NOT_MEMBER_OF.on(
        diagnosticSource,
        name,
        kind,
        typeName,
        session,
    )
}

/**
 * 一元运算符解析失败：有 operator 和 receiverType 但无参数。
 *
 * 对齐 C++ sema_invalid_unary_expr。
 */
private fun ConeUnresolvedNameError.mapInvalidUnaryExprDiagnostic(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val operatorToken = operator ?: return null
    val type = receiverType ?: return null
    // 一元运算符：有 operator + receiverType 但无参数
    if (argumentTypes.isNotEmpty()) return null

    val diagnosticSource = source ?: callOrAssignmentSource ?: return null
    return CfirErrors.INVALID_UNARY_EXPR.on(
        diagnosticSource,
        operatorToken,
        type,
        session,
    )
}

/**
 * 将 extend 中的 `super` 解析失败映射为专用诊断。
 */
private fun ConeUnresolvedNameError.mapExtendSuperDiagnostic(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = source ?: callOrAssignmentSource ?: return null
    val psi = diagnosticSource.psi ?: return null
    val unresolvedName = name.asString()
    val sourceText = psi.text
    if (unresolvedName != "super" && unresolvedName != "<super>" && !sourceText.contains("super")) {
        return null
    }
    if (PsiTreeUtil.getParentOfType(psi, CjExtend::class.java, false) == null) {
        return null
    }

    return CfirErrors.EXTEND_SUPER_NOT_ALLOWED.on(diagnosticSource, session)
}

/**
 * 构造二元运算符解析失败时的 invalid binary operator 诊断。
 */
private fun ConeUnresolvedNameError.buildInvalidBinaryOperatorDiagnostic(
    diagnosticSource: CjSourceElement,
    session: CfirSession,
): CjDiagnostic? {
    val operatorToken = operator ?: return null
    val leftType = receiverType ?: return null
    val rightType = argumentTypes.singleOrNull() ?: return null
    if (leftType.containsErrorType() || rightType.containsErrorType()) return null

    return CfirErrors.INVALID_BINARY_OPERATOR.on(
        diagnosticSource,
        operatorToken,
        leftType.renderInvalidBinaryOperatorType(session),
        rightType.renderInvalidBinaryOperatorType(session),
        session,
    )
}

/**
 * 二元 operator 的任一 operand 已经是错误类型时，外层 unresolved operator 只是级联。
 */
private fun ConeUnresolvedNameError.isBinaryOperatorCascadeFromErrorOperand(): Boolean {
    if (operator == null) return false
    val leftType = receiverType ?: return false
    val rightType = argumentTypes.singleOrNull() ?: return false
    return leftType.containsErrorType() || rightType.containsErrorType()
}

/**
 * 将可见性错误映射为构造器缺失、成员访问控制或不可见引用诊断。
 */
private fun ConeVisibilityError.mapConeVisibilityError(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = source ?: callOrAssignmentSource ?: return null

    // 可见性失败来自解析阶段候选筛选，这里只负责把已有 cone 诊断稳定映射到前端诊断。
    val invisibleSymbol = symbol
    if (invisibleSymbol is CfirConstructorSymbol) {
        return CfirErrors.NO_CONSTRUCTOR.on(diagnosticSource, session)
    }

    val invisibleName = when (invisibleSymbol) {
        is CfirCallableSymbol<*> -> invisibleSymbol.name.asString()
        is CfirClassLikeSymbol<*> -> invisibleSymbol.classId.shortClassName.asString()
        else -> invisibleSymbol.toString()
    }
    val visibilityText = when (invisibleSymbol) {
        is CfirCallableSymbol<*> -> invisibleSymbol.cfir.status.visibility.externalDisplayName
        is CfirClassLikeSymbol<*> -> invisibleSymbol.visibilityDisplayName()
        else -> "invisible"
    }
    val isMemberAccess = when (invisibleSymbol) {
        is CfirCallableSymbol<*> -> {
            invisibleSymbol.getContainingClass() != null || invisibleSymbol.getContainingExtend() != null
        }
        else -> false
    }

    return if (isMemberAccess) {
        if (invisibleSymbol is CfirCallableSymbol<*> && invisibleSymbol.cfir is CfirNamedFunction) {
            CfirErrors.INVISIBLE_MEMBER.on(diagnosticSource, invisibleName, visibilityText, session)
        } else {
            val accessSource = (callOrAssignmentSource ?: diagnosticSource).receiverTokenDiagnosticSource()
            CfirErrors.INVALID_ACCESS_CONTROL.on(accessSource, session)
        }
    } else {
        CfirErrors.INVISIBLE_REFERENCE.on(diagnosticSource, invisibleName, visibilityText, session)
    }
}

/**
 * 获取 class-like symbol 对应声明的外部可见性展示文本。
 */
private fun CfirClassLikeSymbol<*>.visibilityDisplayName(): String {
    return when (val declaration = cfir) {
        is CfirClass -> declaration.status.visibility.externalDisplayName
        is CfirInterface -> declaration.status.visibility.externalDisplayName
        is CfirStruct -> declaration.status.visibility.externalDisplayName
        is CfirEnum -> declaration.status.visibility.externalDisplayName
        is CfirTypeAlias -> declaration.status.visibility.externalDisplayName
        else -> "invisible"
    }
}

/**
 * 映射未被专用分支处理的 Cone diagnostic。
 *
 * 这里承接简单诊断、类型名解析、泛型参数、effect、类型不匹配等通用路径，
 * 并在可能时选择比 unresolved 或 inference error 更具体的 CFIR 诊断。
 */
private fun ConeDiagnostic.mapOtherDiagnostic(
    source: CjSourceElement?,
    valueParameter: CfirValueParameter?,
    callOrAssignmentSource: CjSourceElement?,
    session: CfirSession,
    returnExpressionSource: AbstractCjSourceElement? = null,
): CjDiagnostic? {
    val diagnosticSource = callOrAssignmentSource ?: source ?: return null
    // 官方 `DiagnoseForCallInference` 把泛型推断失败锚定在 callee 表达式而非整个调用。
    val genericInferenceAnchorSource = diagnosticSource.genericInferenceCalleeAnchorSource()
    val genericCallSource = callOrAssignmentSource ?: source
    val genericCallDiagnosticSource = genericCallSource
        ?.genericInferenceCallCalleeSource()
        ?.takeIf { genericCallSource.isImplicitGenericCallWithoutTypeArguments() }
    return when (this) {
        is ConeFunctionExpectedError,
        is ConeFunctionCallExpectedError,
        -> CfirErrors.INVALID_CALLED_OBJECT.on(diagnosticSource, session)

        is ConeCannotInferGenericFunctionTypeParameterType ->
            CfirErrors.UNABLE_TO_INFER_GENERIC_FUNC.on(
                genericInferenceAnchorSource,
                session,
            )

        is ConeCannotInferTypeParameterType ->
            CfirErrors.UNABLE_TO_INFER_GENERIC_FUNC.on(
                genericInferenceAnchorSource,
                session,
            )

        is ConeCannotInferValueParameterType ->
            CfirErrors.LAMBDA_MUST_HAVE_TYPE_ANNOTATION.on(diagnosticSource, session)

        is ConeNoConstructorError,
        is ConeNoImplicitDefaultConstructorOnExpectClass,
        is ConeResolutionToClassifierError,
        -> CfirErrors.NO_CONSTRUCTOR.on(diagnosticSource, session)

        is ConeTypeParameterInQualifiedAccess ->
            CfirErrors.GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT.on(
                diagnosticSource,
                symbol.name,
                session,
            )

        is ConeEnumTypeCannotBeUsedAsConstructorError ->
            CfirErrors.ENUM_TYPE_CANNOT_BE_USED_AS_CONSTRUCTOR.on(
                diagnosticSource,
                enumName,
                session,
            )

        is ConeNoMatchingInvokeOperatorError -> {
            val invokeDiagnosticSource = source ?: diagnosticSource
            CfirErrors.NO_MATCHING_OPERATOR_INVOKE.on(
                CjOffsetsOnlySourceElement(invokeDiagnosticSource.startOffset, invokeDiagnosticSource.endOffset),
                name.asString(),
                receiverType,
                session,
            )
        }

        is ConeNoMatchOperatorFunctionCallError -> {
            val invokeDiagnosticSource = source ?: diagnosticSource
            CfirErrors.NO_MATCH_OPERATOR_FUNCTION_CALL.on(
                CjOffsetsOnlySourceElement(invokeDiagnosticSource.startOffset, invokeDiagnosticSource.endOffset),
                session,
            )
        }

        is ConeEffectsFeatureDisabledError -> CfirErrors.EFFECTS_FEATURE_DISABLED.on(
            diagnosticSource,
            constructName,
            session,
        )

        is ConeCommandIncompatibleTypeError -> CfirErrors.COMMAND_INCOMPATIBLE_TYPE.on(
            diagnosticSource,
            actualType ?: ConeErrorType(ConeSimpleDiagnostic("unknown effect command type")),
            session,
        )

        is ConeCommandHandleTypeError -> CfirErrors.COMMAND_HANDLE_TYPE_ERROR.on(
            diagnosticSource,
            actualType ?: ConeErrorType(ConeSimpleDiagnostic("unknown handler command type")),
            session,
        )

        is ConeImplicitResumeOutsideHandlerError -> CfirErrors.IMPLICIT_RESUME_OUTSIDE_HANDLER.on(
            diagnosticSource,
            session,
        )

        is ConeResumeNoWithError -> CfirErrors.RESUME_NO_WITH.on(
            diagnosticSource,
            resumptionType,
            session,
        )

        is ConeResumeThrowingMismatchTypeError -> CfirErrors.RESUME_THROWING_MISMATCH_TYPE.on(
            diagnosticSource,
            actualType ?: ConeErrorType(ConeSimpleDiagnostic("unknown resume throwing type")),
            session,
        )

        is ConeMismatchingHandleBlockError -> CfirErrors.MISMATCHING_HANDLE_BLOCK.on(
            diagnosticSource,
            actualType,
            expectedType,
            session,
        )

        is ConeSimpleDiagnostic -> when (kind) {
            DiagnosticKind.CannotInferParameterType -> {
                if (genericCallDiagnosticSource != null) {
                    CfirErrors.NEW_INFERENCE_ERROR.on(
                        genericCallDiagnosticSource,
                        "Inference error: ConstraintMismatch",
                        session,
                    )
                } else {
                    valueParameter
                        ?.typeParameters
                        ?.firstOrNull()
                        ?.symbol
                        ?.let { it as? CfirTypeParameterSymbol }
                        ?.let { CfirErrors.CANNOT_INFER_PARAMETER_TYPE.on(diagnosticSource, it, session) }
                }
            }

            DiagnosticKind.LoopInSupertype ->
                CfirErrors.INHERITANCE_CYCLE.on(diagnosticSource.inheritanceCycleDiagnosticSource(), session)

            DiagnosticKind.SupertypeSelfReference ->
                CfirErrors.SUPER_TYPES_SELF_REFERENCE.on(diagnosticSource, diagnosticSource.toApproxTypeName(), session)

            DiagnosticKind.DuplicateSupertype -> null

            DiagnosticKind.GenericTypeWithoutTypeArgument ->
                CfirErrors.GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT.on(
                    diagnosticSource,
                    diagnosticSource.toApproxTypeName(),
                    session,
                )

            DiagnosticKind.SuperNotAllowed ->
                CfirErrors.EXTEND_SUPER_NOT_ALLOWED.on(diagnosticSource, session)

            DiagnosticKind.JumpOutsideLoop ->
                CfirErrors.INVALID_LOOP_CONTROL.on(source ?: diagnosticSource, session)

            DiagnosticKind.ReturnNotAllowed ->
                CfirErrors.INVALID_RETURN.on(diagnosticSource, session)

            DiagnosticKind.ReturnInStaticInit ->
                CfirErrors.INVALID_RETURN_IN_STATIC_INIT.on(diagnosticSource, session)

            DiagnosticKind.CaptureBeforeInitialization ->
                // CaptureBeforeInitialization 需要变量名，但 ConeSimpleDiagnostic 不携带。
                // 使用 reason 字符串中提取的名称，或者使用结构化 Cone 诊断类。
                null

            DiagnosticKind.InvalidThisTypePosition ->
                CfirErrors.INVALID_POSITION_OF_THIS_TYPE.on(source ?: diagnosticSource, session)

            DiagnosticKind.EmptyArrayLiteralTypeUndefined ->
                CfirErrors.ARRAY_LITERAL_TYPE_CANNOT_BE_INFERRED.on(diagnosticSource, session)

            else -> null
        } ?: mapSimpleDiagnosticByReason(this, diagnosticSource, session)

        is ConeUnresolvedNameError -> CfirErrors.UNRESOLVED_REFERENCE.on(
            diagnosticSource,
            name.asString(),
            operator,
            session,
        )

        is ConeUnresolvedReferenceError -> CfirErrors.UNRESOLVED_REFERENCE.on(
            diagnosticSource,
            name.asString(),
            null,
            session,
        )

        is ConeUnresolvedSymbolError -> CfirErrors.UNRESOLVED_REFERENCE.on(
            diagnosticSource,
            classId.asString(),
            null,
            session,
        )

        is ConeUnresolvedTypeQualifierError -> {
            when {
                source?.kind == CjRealSourceElementKind -> {
                    val lastQualifier = this.qualifiers.last()
                    CfirErrors.UNDECLARED_TYPE_NAME.on(
                        lastQualifier.source.requireNotNull(),
                        lastQualifier.name.asString(),
                        session,
                    )
                }
                else -> {
                    CfirErrors.UNDECLARED_TYPE_NAME.on(source.requireNotNull(), this.qualifier, session)
                }
            }
        }

        is ConeNotATypeError -> CfirErrors.NOT_A_TYPE.on(
            diagnosticSource,
            name.asString(),
            session,
        )

        // ── resolve 管线补齐映射 ──

        is ConeCannotRefToPackageNameError -> CfirErrors.CANNOT_REF_TO_PKG_NAME.on(
            diagnosticSource, session,
        )

        is ConePackageNameConflictError -> CfirErrors.AMBIGUOUS_USE.on(
            diagnosticSource, packageName, session,
        )

        is ConeGenericTypeInconsistentError -> {
            // 官方把该诊断锚定在 nominal base expression，而不是被调用成员名。
            val receiverSource = candidate.callInfo.explicitReceiver?.source
            CfirErrors.GENERIC_TYPE_INCONSISTENT.on(
                receiverSource ?: diagnosticSource, typeParameterName, session,
            )
        }

        is ConeUnmatchedTypeArgumentsError -> {
            if (actualCount == 0) {
                CfirErrors.GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT.on(
                    diagnosticSource,
                    symbol.name,
                    session,
                )
            } else {
                CfirErrors.GENERIC_ARGUMENT_NO_MATCH.on(
                    diagnosticSource,
                    session,
                )
            }
        }

        is ConeGenericArgumentNoMatchError -> CfirErrors.GENERIC_ARGUMENT_NO_MATCH.on(
            diagnosticSource, session,
        )

        is ConeGenericTypeArgumentNotMatchConstraintError -> CfirErrors.GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT.on(
            diagnosticSource, actualType, upperBound, genericType, session,
        )

        is ConeGenericConstraintNotLooserError -> CfirErrors.GENERIC_CONSTRAINT_NOT_LOOSER.on(
            diagnosticSource, session,
        )

        is ConeGenericInstantiationCausesAmbiguousFunctionsError -> CfirErrors.GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS.on(
            diagnosticSource, instantiation, functionName, session,
        )

        is ConeMeetConstraintIndirectlyError -> CfirErrors.MEET_CONSTRAINT_INDIRECTLY.on(
            diagnosticSource, session,
        )

        is ConeNotMemberOfError -> CfirErrors.NOT_MEMBER_OF.on(
            diagnosticSource, memberName, kind, typeName, session,
        )

        is ConeMemberNotImportedError -> CfirErrors.MEMBER_NOT_IMPORTED.on(
            diagnosticSource, memberName, session,
        )

        is ConeInvalidUnaryExprError -> CfirErrors.INVALID_UNARY_EXPR.on(
            diagnosticSource, operator, type, session,
        )

        is ConeInvalidUnaryExprWithTargetError -> CfirErrors.INVALID_UNARY_EXPR_WITH_TARGET.on(
            diagnosticSource, operator, type, returnType, session,
        )

        is ConeOptionalChainNonOptionalError -> CfirErrors.OPTIONAL_CHAIN_NON_OPTIONAL.on(
            diagnosticSource, type, session,
        )

        is ConeUnableToInferGenericFuncError -> CfirErrors.UNABLE_TO_INFER_GENERIC_FUNC.on(
            genericInferenceAnchorSource, session,
        )

        is ConeGenericFunctionReferenceWithoutTypeArgumentsError ->
            CfirErrors.GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT.on(
                source ?: diagnosticSource,
                functionName,
                session,
            )

        is ConeUnableToInferExpressionTypeError -> CfirErrors.UNABLE_TO_INFER_EXPR.on(
            diagnosticSource, session,
        )

        is ConeInvalidNodeAfterCheckError -> CfirErrors.INVALID_NODE_AFTER_CHECK.on(
            diagnosticSource, session,
        )

        is ConeInconsistentArrayLiteralElementTypeError -> CfirErrors.INCONSISTENT_ARRAY_LITERAL_ELEMENT_TYPE.on(
            diagnosticSource, session,
        )

        is ConeTypeMismatchError -> typeMismatchDiagnostic(
            source = diagnosticSource,
            callOrAssignmentSource = callOrAssignmentSource,
            expectedType = expectedType,
            actualType = actualType,
            isMismatchDueToNullability = false,
            session = session,
            returnExpressionSource = returnExpressionSource,
        )

        is ConeMismatchedTypesBecauseError -> CfirErrors.MISMATCHED_TYPES_BECAUSE.on(
            diagnosticSource, expectedType, actualType, because, session,
        )

        is ConeMismatchedTypesMultipleAssignError -> CfirErrors.TYPE_MISMATCH.on(
            diagnosticSource, expectedType, actualType, false, session,
        )

        is ConeParamCountMismatchError -> CfirErrors.PARAM_COUNT_MISMATCH.on(
            diagnosticSource, expected, actual, session,
        )

        is ConeCaptureBeforeInitializationError -> CfirErrors.CAPTURE_BEFORE_INITIALIZATION.on(
            diagnosticSource, variableName, session,
        )

        else -> null
    }
}

/**
 * 对 `DiagnosticKind.Other` 的历史理由串做显式映射。
 *
 * 这些 reason 均来自 resolve/raw-cfir 中已有 producer，不是兜底策略：
 * - 只对已知、稳定的 reason 前缀进行映射；
 * - 未命中的 reason 仍保持原行为（不报告）。
 */
private fun mapSimpleDiagnosticByReason(
    diagnostic: ConeSimpleDiagnostic,
    diagnosticSource: CjSourceElement,
    session: CfirSession,
): CjDiagnostic? {
    val reason = diagnostic.reason
    return when {
        reason == "`return` must be used inside a function" ->
            CfirErrors.INVALID_RETURN.on(diagnosticSource, session)

        reason == "Unresolved return type" ||
            reason == "Unresolved function return type" ->
            CfirErrors.UNABLE_TO_INFER_RETURN_TYPE.on(diagnosticSource, session)

        reason == "unsupported declaration for implicit type" ||
            reason == "failed to resolve implicit type" ||
            reason == "recursive implicit type" ->
            CfirErrors.UNABLE_TO_INFER_DECL.on(diagnosticSource, session)

        reason == "type not resolved after transformation" ||
            reason == "No expected type" ->
            CfirErrors.UNABLE_TO_INFER_EXPR.on(diagnosticSource, session)

        reason.startsWith("Callee reference to candidate without return type:") ||
            reason == "non-name reference" ->
            CfirErrors.INVALID_CALLED_OBJECT.on(diagnosticSource, session)

        else -> null
    }
}

/**
 * 使用候选约束系统最终替换器替换类型变量。
 */
private fun ConeCangJieType.substituteTypeVariableTypes(
    candidate: AbstractCallCandidate<*>,
    session: CfirSession,
): ConeCangJieType {
    val substitutor = candidate.system.asReadOnlyStorage()
        .buildAbstractResultingSubstitutor(session.typeContext)
        .asCone()
    return substitutor.substituteOrSelf(this)
}

/**
 * 判断约束系统矛盾是否应归类为隐式泛型调用的约束不匹配。
 */
private fun ConeConstraintSystemHasContradiction.hasGenericInferenceConstraintMismatch(session: CfirSession): Boolean {
    if (candidate.symbol !is CfirCallableSymbol<*>) return false
    if (candidate.symbol is CfirEnumConstructorSymbol) return false
    if (candidate.genericInferenceDeclaredTypeParameters(session).isEmpty()) return false
    if (candidate.hasExplicitTypeArgumentsInCall()) return false

    return candidate.errors.any { it is ConstraintMismatch }
}

/**
 * 判断是否为隐式 enum constructor payload 约束导致的官方泛型函数推断失败。
 *
 * `Some(a): ??I` 这类调用的失败根因是 owner 泛型参数 `T` 同时受到 payload 下界
 * 和目标返回类型上界约束，官方诊断为 `sema_unable_to_infer_generic_func`，范围定位到 callee。
 */
private fun ConeConstraintSystemHasContradiction.isImplicitEnumConstructorPayloadInferenceMismatch(): Boolean {
    if (candidate.hasBareGenericFunctionReferencePayloadArgument()) return false
    val enumConstructor = (candidate.symbol as? CfirEnumConstructorSymbol)
        ?.takeIf { it.isBound }
        ?.cfir
        ?: return false
    if (enumConstructor.valueParameters.isEmpty()) return false
    if (candidate.hasExplicitTypeArgumentsInCall()) return false

    return candidate.errors.any { error ->
        error is ConstraintMismatch && error.position.from is ConeArgumentConstraintPosition
    }
}

/**
 * 判断候选是否为 typealias constructor 候选。
 */
private fun AbstractCallCandidate<*>.isTypeAliasConstructorCandidate(): Boolean {
    val constructorSymbol = symbol as? CfirConstructorSymbol ?: return false
    return constructorSymbol.typeAliasConstructorInfo != null
}

/**
 * 判断约束错误集合中是否包含显式类型实参约束不匹配。
 */
private fun List<ConstraintSystemError>.hasExplicitTypeArgumentConstraintMismatch(): Boolean =
    any { error ->
        error is ConstraintMismatch &&
            error.position.from is ConeExplicitTypeParameterConstraintPosition
    }

/**
 * 将显式类型实参产生的 constraint mismatch 映射为调用点上界诊断。
 *
 * solver 与类型使用 checker 必须消费同一组声明上界；当候选因该约束成为 error reference 时，
 * 由 constraint position 保存的原始 type argument source 负责精确定位，避免依赖完成后的 AST 形态。
 */
private fun AbstractCallCandidate<*>.explicitTypeArgumentConstraintDiagnostics(
    session: CfirSession,
    fallbackSource: CjSourceElement?,
): List<CjDiagnostic> {
    val typeParameters = explicitConstraintTypeParameters(session)
    val diagnostics = linkedMapOf<String, CjDiagnostic>()

    for (error in errors) {
        val mismatch = error as? ConstraintMismatch ?: continue
        val position = mismatch.position.from as? ConeExplicitTypeParameterConstraintPosition ?: continue
        val actualType = mismatch.lowerType as? ConeCangJieType ?: continue
        val upperBound = mismatch.upperType as? ConeCangJieType ?: continue
        if (actualType is ConeErrorType || upperBound is ConeErrorType) continue

        val argumentIndex = callInfo.typeArguments.indexOfFirst { argument ->
            argument === position.typeArgument || argument == position.typeArgument
        }
        val genericType = typeParameters.getOrNull(argumentIndex)
            ?.symbol
            ?.constructType()
            ?: upperBound
        val diagnosticSource = position.typeArgument.source ?: fallbackSource ?: continue
        val key = "${diagnosticSource.startOffset}:${diagnosticSource.endOffset}:$actualType:$upperBound:$genericType"
        val diagnostic = CfirErrors.GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT.on(
            diagnosticSource,
            actualType,
            upperBound,
            genericType,
            session,
        ) ?: continue
        diagnostics.putIfAbsent(key, diagnostic)
    }
    return diagnostics.values.toList()
}

/** 返回显式 callable/constructor type arguments 对应的声明类型参数序列。 */
private fun AbstractCallCandidate<*>.explicitConstraintTypeParameters(
    session: CfirSession,
): List<CfirTypeParameterRef> {
    val callable = symbol as? CfirCallableSymbol<*> ?: return emptyList()
    val declaration = callable.cfir
    if (declaration !is CfirConstructor) return declaration.typeParameters

    val ownerClassId = callable.callableId.classId ?: return declaration.typeParameters
    val owner = session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId)
        ?.cfir as? CfirTypeParameterRefsOwner
        ?: return declaration.typeParameters
    return owner.typeParameters + declaration.typeParameters
}

/**
 * 判断 typealias constructor 展开后是否触发实际构造类型的上界违例。
 */
private fun AbstractCallCandidate<*>.hasTypeAliasConstructorExpansionUpperBoundViolation(
    session: CfirSession,
): Boolean {
    val constructorSymbol = symbol as? CfirConstructorSymbol ?: return false
    val typeAliasConstructorInfo = constructorSymbol.typeAliasConstructorInfo ?: return false
    val typeArguments = resolvedExplicitTypeArgumentTypes()
    if (typeArguments.isEmpty()) return false

    val typeAlias = typeAliasConstructorInfo.typeAliasSymbol.cfir
    val expandedType = ConeTypeAliasType(
        classId = typeAliasConstructorInfo.typeAliasSymbol.classId,
        expandedType = typeAlias.expandedTypeRef.coneTypeOrNull,
        typeArguments = typeArguments,
    ).fullyExpandedType(session) as? ConeClassifierType ?: return false
    if (expandedType.typeArguments.isEmpty()) return false

    val expandedSymbol = expandedType.toSymbol(session) as? CfirClassLikeSymbol<*> ?: return false
    val typeParameters = expandedSymbol.cfir.typeParameters
    if (typeParameters.isEmpty()) return false

    val substitutor = createTypeSubstitutorByTypeConstructor(
        map = typeParameters
            .zip(expandedType.typeArguments.map { it.type })
            .associate { (typeParameter, argument) ->
                typeParameter.symbol.toLookupTag() as TypeConstructorMarker to argument
            },
        context = session.typeContext,
        approximateIntegerLiterals = false,
    )

    val count = minOf(typeParameters.size, expandedType.typeArguments.size)
    for (index in 0 until count) {
        val argumentType = expandedType.typeArguments[index].type
        if (argumentType is ConeErrorType) continue

        val upperBounds = typeParameters[index].symbol.resolvedBounds
            .map { it.coneType }
            .filterNot { it is ConeErrorType }
        if (upperBounds.isEmpty()) continue

        val upperBound = substitutor.substituteOrSelf(
            session.typeContext.intersectTypes(upperBounds) as ConeCangJieType,
        )
        if (upperBound !is ConeErrorType &&
            !AbstractTypeChecker.isSubtypeOfWithoutOptionBoxing(
                session.typeContext,
                argumentType,
                upperBound,
            )
        ) {
            return true
        }
    }
    return false
}

/**
 * 判断 typealias constructor 候选的约束系统中是否存在上界相关 mismatch。
 */
private fun AbstractCallCandidate<*>.hasTypeAliasConstructorUpperBoundConstraintMismatch(): Boolean {
    if (!isTypeAliasConstructorCandidate()) return false
    if (!hasExplicitTypeArgumentsInCall()) return false

    return errors.any { error ->
        error is ConstraintMismatch &&
            when (error.position.from) {
                is ConeExplicitTypeParameterConstraintPosition,
                is ConeDeclaredUpperBoundConstraintPosition,
                -> true

                else -> false
            }
    }
}

/**
 * 解析调用中显式类型实参的实际类型。
 */
private fun AbstractCallCandidate<*>.resolvedExplicitTypeArgumentTypes(): List<ConeCangJieType> {
    return callInfo.typeArguments.mapNotNull { it.coneTypeOrNull }
}

/**
 * 构造 callable 候选在约束系统中的 owner 类型视图。
 */
private fun AbstractCallCandidate<*>.callableConstraintOwnerType(session: CfirSession): ConeCangJieType? {
    val callable = symbol as? CfirCallableSymbol<*> ?: return null
    val declaration = callable.cfir
    return when (declaration) {
        is CfirFunction -> {
            val parameterTypes = declaration.valueParameters.map { parameter ->
                val parameterType = parameter.returnTypeRef.coneTypeOrNull ?: return null
                parameterType.substituteTypeVariableTypes(this, session)
            }
            val returnType = declaration.returnTypeRef.coneTypeOrNull ?: return null
            ConeFunctionType(parameterTypes, returnType.substituteTypeVariableTypes(this, session))
        }

        else -> declaration.returnTypeRef.coneTypeOrNull?.substituteTypeVariableTypes(this, session)
    }
}

/**
 * 判断隐式泛型调用是否因实参类型涉及声明类型参数而产生推断不匹配。
 */
private fun AbstractCallCandidate<*>.hasGenericInferenceArgumentMismatch(session: CfirSession): Boolean {
    val declaredTypeParameters = genericInferenceDeclaredTypeParameters(session)
    if (declaredTypeParameters.isEmpty()) return false
    if (hasExplicitTypeArgumentsInCall()) return false

    val argumentMismatches = diagnostics.filterIsInstance<ArgumentTypeMismatch>()
        .filter { it.argument !is CfirAnonymousFunctionExpression }
    return argumentMismatches.any { diagnostic ->
        diagnostic.expectedType.referencesDeclaredTypeParameter(declaredTypeParameters)
    } || argumentMismatches.size >= 2
}

/**
 * 构造泛型调用推断约束不匹配诊断。
 */
private fun ConeConstraintSystemHasContradiction.genericInferenceErrorDiagnostic(
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    if (candidate.symbol !is CfirCallableSymbol<*>) return null
    val declaredTypeParameters = candidate.genericInferenceDeclaredTypeParameters(session)
    val candidateTypeVariable = candidate.system.asReadOnlyStorage().allTypeVariables.values.firstOrNull { variable ->
        (variable as? ConeTypeParameterBasedTypeVariable)?.typeParameterSymbol in declaredTypeParameters
    }

    /**
     * 对齐官方仓颉 `DiagnoseForCallInference`：
     * 泛型调用推断失败的主诊断锚点应优先落在被调用函数名本身，
     * 只有拿不到 callee source 时，才退回到外围 source。
     */
    val diagnosticSource = candidateTypeVariable
        ?.let { candidate.sourceOfCallToSymbolWith(it) }
        ?: candidate.callInfo.callSite.genericInferenceCalleeSource()
        ?: source
        ?: qualifiedAccessSource
        ?: return null

    return CfirErrors.NEW_INFERENCE_ERROR.on(
        diagnosticSource,
        "Inference error: ConstraintMismatch",
        session,
    )
}

/**
 * 构造泛型函数类型实参无法推断诊断。
 */
private fun ConeConstraintSystemHasContradiction.unableToInferGenericFunctionDiagnostic(
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
    session: CfirSession,
): CjDiagnostic? {
    /**
     * 官方仓颉将泛型调用实参无法推断归一为
     * `unable to infer generic argument of this function`，并在
     * `DiagnoseForCallInference` 中把诊断锚定在 `ce.baseFunc`（callee 表达式）上，
     * 覆盖完整限定 callee 而不包含实参括号。这里保留 Kotlin FIR 的 constraint-system
     * 分层，只在诊断表面映射为仓颉诊断名与官方锚点。
     */
    val diagnosticSource = qualifiedAccessSource?.genericInferenceCalleeAnchorSource()
        ?: candidate.callInfo.callSite.source?.genericInferenceCalleeAnchorSource()
        ?: source?.genericInferenceCalleeAnchorSource()
        ?: return null

    return CfirErrors.UNABLE_TO_INFER_GENERIC_FUNC.on(
        diagnosticSource,
        session,
    )
}

/**
 * 从实参表达式自身恢复隐式泛型调用的推断不匹配诊断。
 */
private fun org.cangnova.cangjie.cfir.expressions.CfirExpression.genericInferenceArgumentMismatchDiagnostic(
    session: CfirSession,
): CjDiagnostic? {
    val qualifiedAccess = unwrapWrappedExpression() as? CfirQualifiedAccessExpression ?: return null
    if (qualifiedAccess !is CfirFunctionCall) return null
    if (qualifiedAccess.typeArguments.isNotEmpty()) return null

    val callableSymbol = qualifiedAccess.genericInferenceCallableSymbolOrNull()
    if (callableSymbol != null) {
        if (callableSymbol.cfir.typeParameters.isEmpty()) return null
        val source = qualifiedAccess.genericInferenceCalleeSource() ?: return null
        return CfirErrors.NEW_INFERENCE_ERROR.on(
            source,
            "Inference error: ConstraintMismatch",
            session,
        )
    }

    val errorDiagnostic = (qualifiedAccess.coneTypeOrNull as? ConeErrorType)?.diagnostic
    if (errorDiagnostic !is ConeCannotInferTypeParameterType) return null

    val source = qualifiedAccess.genericInferenceCalleeSource() ?: return null
    return CfirErrors.NEW_INFERENCE_ERROR.on(
        source,
        "Inference error: ConstraintMismatch",
        session,
    )
}

/**
 * 从调用表达式错误类型恢复隐式泛型调用的类型不匹配诊断。
 */
private fun org.cangnova.cangjie.cfir.expressions.CfirExpression.genericInferenceCallTypeMismatchDiagnostic(
    session: CfirSession,
): CjDiagnostic? {
    val qualifiedAccess = unwrapWrappedExpression() as? CfirQualifiedAccessExpression ?: return null
    if (qualifiedAccess.typeArguments.isNotEmpty()) return null

    val callableSymbol = qualifiedAccess.genericInferenceCallableSymbolOrNull() ?: return null
    if (callableSymbol.cfir.typeParameters.isEmpty()) return null

    val diagnostic = (qualifiedAccess.coneTypeOrNull as? ConeErrorType)?.diagnostic ?: return null
    if (diagnostic !is ConeCannotInferGenericFunctionTypeParameterType &&
        diagnostic !is ConeCannotInferTypeParameterType
    ) {
        return null
    }

    val source = qualifiedAccess.genericInferenceCalleeSource() ?: return null
    return CfirErrors.NEW_INFERENCE_ERROR.on(
        source,
        "Inference error: ConstraintMismatch",
        session,
    )
}

/**
 * 渲染 invalid binary operator 诊断中使用的类型文本。
 */
internal fun ConeCangJieType.renderInvalidBinaryOperatorType(session: CfirSession): String {
    val classId = classIdOrPrimitiveClassId ?: return toString()
    val declaration = runCatching { session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir }.getOrNull()
    val kind = when (declaration) {
        is CfirClass -> "Class"
        is CfirStruct -> "Struct"
        is CfirEnum -> "Enum"
        is CfirInterface -> "Interface"
        is CfirTypeAlias -> "TypeAlias"
        else -> null
    }
    return if (kind != null) "$kind-${classId.shortClassName.asString()}" else classId.shortClassName.asString()
}

/**
 * 根据 source 所在语义位置选择普通类型不匹配或返回类型不匹配诊断。
 */
private fun typeMismatchDiagnostic(
    source: CjSourceElement?,
    callOrAssignmentSource: CjSourceElement? = null,
    expectedType: ConeCangJieType,
    actualType: ConeCangJieType,
    isMismatchDueToNullability: Boolean,
    session: CfirSession,
    returnExpressionSource: AbstractCjSourceElement? = null,
): CjDiagnostic? {
    val diagnosticSource = source ?: return null
    if (expectedType.containsErrorType() || actualType.containsErrorType()) return null
    specificTypeMismatchDiagnostic(
        source = diagnosticSource,
        expectedType = expectedType,
        actualType = actualType,
        session = session,
    )?.let { return it }

    val typeMismatchTarget = returnExpressionSource?.let(TypeMismatchTarget::ReturnExpression)
        ?: diagnosticSource.typeMismatchTarget()
        ?: callOrAssignmentSource.typeMismatchTarget()
    return when (typeMismatchTarget) {
        is TypeMismatchTarget.ReturnExpression -> CfirErrors.RETURN_TYPE_MISMATCH.on(
            if (expectedType.rangeElementTypeOrNull() != null && actualType.rangeElementTypeOrNull() != null) {
                return CfirErrors.TYPE_MISMATCH.on(
                    diagnosticSource,
                    expectedType,
                    actualType,
                    isMismatchDueToNullability,
                    session,
                )
            } else typeMismatchTarget.expressionSource,
            expectedType,
            actualType,
            isMismatchDueToNullability,
            session,
        )

        TypeMismatchTarget.FieldInitializer -> null

        null -> CfirErrors.TYPE_MISMATCH.on(
            diagnosticSource,
            expectedType,
            actualType,
            isMismatchDueToNullability,
            session,
        )
    }
}

/**
 * 映射类型变量被推断为空交类型或可能为空交类型的诊断。
 */
private fun inferredIntoEmptyIntersection(
    source: CjSourceElement?,
    typeVariable: org.cangnova.cangjie.type.model.TypeVariableMarker,
    incompatibleTypes: Collection<ConeCangJieType>,
    causingTypes: Collection<ConeCangJieType>,
    kind: EmptyIntersectionTypeKind,
    isError: Boolean,
    session: CfirSession,
): CjDiagnostic? {
    val diagnosticSource = source ?: return null
    val typeVariableText = when (typeVariable) {
        is ConeTypeParameterBasedTypeVariable -> typeVariable.typeParameterSymbol.name.asString()
        else -> typeVariable.toString()
    }
    val causingTypesText = if (incompatibleTypes == causingTypes) {
        ""
    } else {
        ": ${causingTypes.joinToString()}"
    }
    val kindDescription = kind.toDiagnosticDescription()

    return if (isError) {
        CfirErrors.INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION.on(
            diagnosticSource,
            typeVariableText,
            incompatibleTypes,
            kindDescription,
            causingTypesText,
            session,
        )
    } else {
        CfirErrors.INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION.on(
            diagnosticSource,
            typeVariableText,
            incompatibleTypes,
            kindDescription,
            causingTypesText,
            session,
        )
    }
}

/**
 * 将空交类型种类渲染为诊断描述文本。
 */
private fun EmptyIntersectionTypeKind.toDiagnosticDescription(): String = when (this) {
    EmptyIntersectionTypeKind.MULTIPLE_CLASSES -> "multiple concrete class or struct bounds are incompatible"
    EmptyIntersectionTypeKind.FINAL_CLASS_AND_INTERFACE -> "a final concrete bound is combined with an interface bound"
}

/**
 * 在调用树中定位引用指定类型变量的被调用符号 source。
 */
private fun AbstractCallCandidate<*>.sourceOfCallToSymbolWith(typeVariable: org.cangnova.cangjie.type.model.TypeVariableMarker): CjSourceElement? {
    val declaredTypeParameter = (typeVariable as? ConeTypeParameterBasedTypeVariable)?.typeParameterSymbol ?: return null
    var narrowedSource: CjSourceElement? = null

    callInfo.callSite.acceptChildren(object : CfirVisitorVoid() {
        override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
            if (narrowedSource != null) return

            if (element is CfirQualifiedAccessExpression) {
                val symbol = (element.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol as? CfirCallableSymbol<*>
                if (symbol != null && symbol.cfir.typeParameters.any { it.symbol == declaredTypeParameter }) {
                    narrowedSource = element.calleeReference.source
                    return
                }
            }

            element.acceptChildren(this, null)
        }
    })

    return narrowedSource
}

/**
 * 获取泛型推断诊断应使用的 callee source。
 */
private fun org.cangnova.cangjie.cfir.CfirElement.genericInferenceCalleeSource(): CjSourceElement? {
    val qualifiedAccess = this as? CfirQualifiedAccessExpression ?: return source
    return qualifiedAccess.calleeReference.source ?: qualifiedAccess.source
}

/**
 * 从 PSI source 中提取调用表达式的 callee source。
 */
private fun CjSourceElement.genericInferenceCallCalleeSource(): CjSourceElement? {
    val psiSource = when (this) {
        is CjPsiSourceElement -> this
        // 纯 LightTree 路径没有 PSI 背书，必须在轻量树上取同一个 callee 子节点，
        // 否则 PSI 与非 PSI 两条 LLT 路径会给出不同的诊断范围。
        is CjLightSourceElement -> this.unwrapToCjPsiSourceElement() ?: return lightTreeCallCalleeSource()
        else -> null
    } ?: return this

    val callExpression = psiSource.psi as? CjCallExpression ?: return this
    return callExpression.calleeExpression?.toCjPsiSourceElement() ?: this
}

/**
 * 在轻量树上取调用表达式的 callee 子节点 source。
 *
 * 解析器先完成 callee 子树再 `done(CALL_EXPRESSION)`，因此 callee 是第一个有效子节点；
 * 类型实参列表与实参列表都排在其后，与 PSI 侧 `calleeExpression` 的范围一致。
 */
private fun CjLightSourceElement.lightTreeCallCalleeSource(): CjSourceElement {
    if (lighterASTNode.tokenType != CjNodeTypes.CALL_EXPRESSION) return this
    val childrenRef = Ref<Array<LighterASTNode>>()
    treeStructure.getChildren(lighterASTNode, childrenRef)
    val callee = childrenRef.get().firstOrNull { child ->
        child.tokenType != CjTokens.WHITE_SPACE && child.tokenType !in CjTokens.COMMENTS
    } ?: return this
    return callee.toCjLightSourceElement(
        tree = treeStructure,
        kind = kind,
        startOffset = treeStructure.getStartOffset(callee),
        endOffset = treeStructure.getEndOffset(callee),
    )
}

/**
 * 获取泛型推断诊断需要覆盖的完整调用表达式 source。
 *
 * 只有 source 落在调用表达式的 callee 表达式范围内时才上溯到外层调用；
 * 实参子树内部的错误位置（如 lambda 参数 typeRef）保持自身，与 LightTree
 * 路径（`lightTreeCallCalleeSource` 只在节点自身是 CALL_EXPRESSION 时取
 * callee 子节点）保持一致，避免把 lambda 参数推断失败错误地锚定到外层 callee。
 */
private fun CjSourceElement.genericInferenceWholeCallSource(): CjSourceElement {
    val psiSource = when (this) {
        is CjPsiSourceElement -> this
        is CjLightSourceElement -> this.unwrapToCjPsiSourceElement()
        else -> null
    } ?: return this

    val callExpression = psiSource.psi.containingCallExpressionOrNull() ?: return this
    val callee = callExpression.calleeExpression ?: return this
    val psi = psiSource.psi
    return if (psi == callee || PsiTreeUtil.isAncestor(callee, psi, false)) {
        callExpression.toCjPsiSourceElement() ?: this
    } else {
        this
    }
}

/**
 * 获取泛型推断失败诊断的官方锚点 source。
 *
 * 官方 `DiagnoseForCallInference` 把 `sema_unable_to_infer_generic_func` 报在
 * `ce.baseFunc` 上：先定位所在调用表达式，再取其 callee 表达式，因此范围覆盖完整
 * 限定 callee（`B.test`、`TypeClass`）而不包含实参括号；裸引用没有调用外壳时
 * callee 就是引用自身。
 */
private fun CjSourceElement.genericInferenceCalleeAnchorSource(): CjSourceElement =
    genericInferenceWholeCallSource().genericInferenceCallCalleeSource() ?: this

/**
 * 判断 source 所在调用是否为未写显式类型实参的隐式泛型调用。
 */
private fun CjSourceElement.isImplicitGenericCallWithoutTypeArguments(): Boolean {
    val psiSource = when (this) {
        is CjPsiSourceElement -> this
        is CjLightSourceElement -> this.unwrapToCjPsiSourceElement()
        else -> null
    } ?: return false

    val callExpression = psiSource.psi.containingCallExpressionOrNull() ?: return false
    return callExpression.typeArguments.isEmpty()
}

/**
 * 判断 source 所在调用是否包含显式类型实参。
 */
private fun CjSourceElement?.hasExplicitTypeArgumentsInSource(): Boolean {
    val psiSource = when (this) {
        is CjPsiSourceElement -> this
        is CjLightSourceElement -> this.unwrapToCjPsiSourceElement()
        else -> null
    } ?: return false

    val callExpression = psiSource.psi.containingCallExpressionOrNull() ?: return false
    return callExpression.typeArguments.isNotEmpty()
}

/**
 * 判断候选调用是否显式写出了类型实参。
 */
private fun AbstractCallCandidate<*>.hasExplicitTypeArgumentsInCall(): Boolean {
    return callInfo.hasExplicitTypeArguments ||
        callInfo.callSite.source.hasExplicitTypeArgumentsInSource() ||
        callInfo.explicitReceiver?.source.hasExplicitTypeArgumentsInSource()
}

/**
 * 判断当前候选是否通过 callable 自身的类型实参通道完成显式函数泛型实例化。
 *
 * owner/receiver 上的类型实参只负责实例化所属类型，不能把其中普通非泛型成员的参数错误
 * 改写成显式泛型函数的 `TYPE_MISMATCH`。因此这里只消费 `callInfo.typeArguments`，
 * 并要求目标 callable 自身确实声明函数类型参数。
 */
private fun AbstractCallCandidate<*>.hasExplicitCallableTypeArgumentsInCall(): Boolean {
    val callable = symbol.cfir as? CfirCallableDeclaration ?: return false
    if (callable.typeParameters.isEmpty()) return false
    return callInfo.typeArguments.isNotEmpty()
}

/**
 * Kotlin FIR 在错误类型已经来自被引用节点时跳过外层推断诊断。
 * 仓颉 `VArray<C, $N>(...)` 这类显式元素类型错误也应只报告 `C` 本身，
 * 不再把同一次 synthetic 构造调用折叠成泛型函数不可推断。
 */
private fun AbstractCallCandidate<*>.hasExplicitTypeArgumentError(): Boolean {
    if (!hasExplicitTypeArgumentsInCall()) return false
    return explicitTypeArgumentRefsInCall().any { typeRef ->
        (typeRef as? CfirResolvedTypeRef)?.coneType?.containsErrorType() == true ||
                typeRef is CfirErrorTypeRef
    }
}

/**
 * 枚举调用中显式类型实参 type refs。
 */
private fun AbstractCallCandidate<*>.explicitTypeArgumentRefsInCall(): Sequence<CfirTypeRef> =
    callInfo.typeArguments.asSequence()

/**
 * 从 PSI 元素向上查找所在调用表达式。
 */
private fun PsiElement.containingCallExpressionOrNull(): CjCallExpression? =
    this as? CjCallExpression ?: PsiTreeUtil.getParentOfType(this, CjCallExpression::class.java, false)

/**
 * 从 qualified access 的 callee reference 中提取可参与泛型推断诊断的 callable symbol。
 */
private fun CfirQualifiedAccessExpression.genericInferenceCallableSymbolOrNull(): CfirCallableSymbol<*>? {
    return when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirCallableSymbol<*>
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirCallableSymbol<*>
        is CfirErrorNamedReference ->
            (reference.diagnostic as? ConeDiagnosticWithSingleCandidate)?.candidateSymbol as? CfirCallableSymbol<*>
        else -> null
    }
}

/**
 * 判断匿名函数签名中是否含错误类型。
 */
private fun CfirAnonymousFunction.containsErrorType(): Boolean {
    return returnTypeRef is CfirErrorTypeRef ||
        valueParameters.any { it.returnTypeRef is CfirErrorTypeRef }
}

/**
 * 仓颉官方 InvalidTy 会阻断后续普通类型不匹配诊断；CFIR 在约束系统里保留
 * delegated type 时，也必须按错误类型处理，避免把已报告错误再次映射成 mismatch。
 */
private fun ConeCangJieType.containsErrorType(): Boolean {
    return contains { it is ConeErrorType || it.isError }
}

/**
 * 判断实参表达式子树中是否已经包含错误诊断。
 */
private fun org.cangnova.cangjie.cfir.expressions.CfirExpression.containsErrorDiagnosticInArgument(): Boolean {
    if (coneTypeOrNull?.containsErrorType() == true) return true
    if (this is CfirDiagnosticHolder) return true
    if (this is CfirResolvable && calleeReference is CfirDiagnosticHolder) return true

    var hasErrorDiagnostic = false
    acceptChildren(object : CfirVisitorVoid() {
        override fun visitElement(element: org.cangnova.cangjie.cfir.CfirElement) {
            if (hasErrorDiagnostic) return
            when {
                element is CfirDiagnosticHolder -> hasErrorDiagnostic = true
                element is CfirResolvable && element.calleeReference is CfirDiagnosticHolder -> hasErrorDiagnostic = true
                element is org.cangnova.cangjie.cfir.expressions.CfirExpression &&
                    element.coneTypeOrNull?.containsErrorType() == true -> hasErrorDiagnostic = true
                else -> element.acceptChildren(this, null)
            }
        }
    }, null)
    return hasErrorDiagnostic
}

/**
 * 官方 Cangjie Sema 对同一次调用中的实参类型错误只报告首个不匹配实参。
 * 这里按候选诊断顺序合并参数约束错误，保留后续非参数约束错误继续映射。
 */
private fun List<ConstraintSystemError>.coalesceArgumentConstraintMismatches(): List<ConstraintSystemError> {
    var reportedArgumentMismatch = false
    return filter { error ->
        if (error !is ConstraintMismatch || error.position.from !is ArgumentConstraintPosition<*>) return@filter true
        if (reportedArgumentMismatch) return@filter false
        reportedArgumentMismatch = true
        true
    }
}

/**
 * 官方 Cangjie Sema 对同一调用的期望类型约束只报告一个主类型不匹配。
 * 显式类型实参的 `Array<T>(...)` 等调用可能在约束系统内生成多个同源
 * expected-type mismatch，这里保留首个用户可见主错误。
 */
private fun List<ConstraintSystemError>.coalesceExpectedTypeConstraintMismatches(): List<ConstraintSystemError> {
    var reportedExpectedTypeMismatch = false
    return filter { error ->
        if (error !is ConstraintMismatch || error.position.from !is ConeExpectedTypeConstraintPosition) return@filter true
        if (reportedExpectedTypeMismatch) return@filter false
        reportedExpectedTypeMismatch = true
        true
    }
}

/**
 * 与约束系统错误保持一致：同一候选上的多个 [ArgumentTypeMismatch]
 * 是同一调用参数检查的级联结果，只保留首个作为用户可见主错误。
 */
private fun List<ResolutionDiagnostic>.coalesceArgumentTypeMismatches(): List<ResolutionDiagnostic> {
    if (any { it.isArgumentMappingDiagnostic }) {
        return filter { it !is ArgumentTypeMismatch }
    }

    var reportedArgumentMismatch = false
    return filter { diagnostic ->
        if (diagnostic !is ArgumentTypeMismatch) return@filter true
        if (reportedArgumentMismatch) return@filter false
        reportedArgumentMismatch = true
        true
    }
}

/**
 * 官方 Cangjie 调用检查在进入逐实参类型检查前先验证参数列表形状。
 * 同一候选已经有缺参、多参、命名参数等映射错误时，实参类型不匹配只是
 * 后续本地检查的派生噪声，不能压过用户可见的参数列表诊断。
 */
private val ResolutionDiagnostic.isArgumentMappingDiagnostic: Boolean
    get() = when (this) {
        is ArgumentPassedTwice,
        is MixingNamedAndPositionalArguments,
        is NamedArgumentsNotAllowed,
        is NamedParameterNotFound,
        is NeedNamedArgument,
        is TrailingLambdaCannotUsedForNonFunction,
        is NoValueForParameter,
        is TooManyArguments,
        is WrongNumberOfArguments,
        -> true

        else -> false
    }

/**
 * 将类型参数 marker 还原为声明侧 CFIR 类型参数符号。
 */
private fun TypeParameterMarker.asDeclaredTypeParameterSymbolOrNull(): CfirTypeParameterSymbol? = when (this) {
    is ConeTypeParameterLookupTag -> typeParameterSymbol
    else -> null
}

/**
 * 将类型变量 marker 还原为声明侧 CFIR 类型参数符号。
 */
private fun org.cangnova.cangjie.type.model.TypeVariableMarker.asDeclaredTypeParameterSymbolOrNull(): CfirTypeParameterSymbol? = when (this) {
    is ConeTypeParameterBasedTypeVariable -> typeParameterSymbol
    else -> null
}

/**
 * 获取符号对应的成员或 class-like 声明名称。
 */
private fun CfirBasedSymbol<*>.memberDeclarationNameOrNull(): Name? = when (this) {
    is CfirCallableSymbol<*> -> name
    is CfirClassLikeSymbol<*> -> classId.shortClassName
    else -> null
}

/**
 * 判断类型内部是否引用了指定声明类型参数集合。
 */
private fun ConeCangJieType.referencesDeclaredTypeParameter(
    declaredTypeParameters: Set<CfirTypeParameterSymbol>,
): Boolean {
    return when (this) {
        is ConeTypeParameterType -> lookupTag.typeParameterSymbol in declaredTypeParameters
        is ConeClassLikeType -> typeArguments.any { projection ->
            val nestedType = projection.type
            nestedType.referencesDeclaredTypeParameter(declaredTypeParameters)
        }
        is ConeTypeAliasType -> expandedType?.referencesDeclaredTypeParameter(declaredTypeParameters) == true
        else -> false
    }
}

/**
 * 判断候选是否存在泛型调用类型信息不足错误。
 */
private fun AbstractCallCandidate<*>.hasGenericCallNotEnoughTypeInformation(session: CfirSession): Boolean {
    val declaredTypeParameters = genericInferenceDeclaredTypeParameters(session)
    if (declaredTypeParameters.isEmpty()) return false
    if (hasExplicitTypeArgumentsInCall()) return false

    return errors.any { error ->
        val notEnough = error as? NotEnoughInformationForTypeParameter<*> ?: return@any false
        val typeParameterSymbol = notEnough.typeVariable.asDeclaredTypeParameterSymbolOrNull() ?: return@any false
        typeParameterSymbol in declaredTypeParameters
    }
}

/**
 * 收集候选调用可由泛型推断诊断负责的声明类型参数。
 */
private fun AbstractCallCandidate<*>.genericInferenceDeclaredTypeParameters(
    session: CfirSession,
): Set<CfirTypeParameterSymbol> {
    val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return emptySet()
    val result = linkedSetOf<CfirTypeParameterSymbol>()
    callableSymbol.cfir.typeParameters.mapTo(result) { it.symbol }

    if (callableSymbol is CfirEnumConstructorSymbol) {
        callableSymbol.getContainingClass()
            ?.cfir
            ?.typeParameters
            ?.mapTo(result) { it.symbol }
    }

    val typeAliasConstructorInfo = (callableSymbol as? CfirConstructorSymbol)?.typeAliasConstructorInfo
        ?: return result
    typeAliasConstructorInfo.originalConstructor.typeParameters.mapTo(result) { it.symbol }

    val expandedReturnType = typeAliasConstructorInfo.originalConstructor.returnTypeRef.coneTypeOrNull
        ?.fullyExpandedType(session) as? ConeLookupTagBasedType
        ?: return result
    val expandedSymbol = expandedReturnType.toSymbol(session) as? CfirClassLikeSymbol<*>
        ?: return result
    expandedSymbol.cfir.typeParameters.mapTo(result) { it.symbol }
    return result
}

/**
 * 判断候选是否为未写显式类型实参的泛型调用。
 */
private fun AbstractCallCandidate<*>.isImplicitGenericCallWithTypeParameters(): Boolean {
    val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return false
    return callableSymbol.cfir.typeParameters.isNotEmpty() && !hasExplicitTypeArgumentsInCall()
}

/**
 * 判断候选是否为隐式 builtin array constructor 调用。
 */
private fun AbstractCallCandidate<*>.isImplicitBuiltinArrayConstructorCall(): Boolean {
    val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return false
    return callableSymbol.cfir.origin == CfirDeclarationOrigin.Synthetic.BuiltinArrayConstructor &&
        !hasExplicitTypeArgumentsInCall()
}

/**
 * 根据 source 形态判断当前错误是否像隐式泛型调用推断失败。
 */
private fun AbstractCallCandidate<*>.looksLikeImplicitGenericCallInferenceFailure(
    source: CjSourceElement?,
    qualifiedAccessSource: CjSourceElement?,
): Boolean {
    val callableSymbol = symbol as? CfirCallableSymbol<*> ?: return false
    if (callableSymbol.cfir.typeParameters.isEmpty()) return false
    if (hasExplicitTypeArgumentsInCall()) return false
    val calleeSource = source ?: return false
    val callSource = qualifiedAccessSource ?: callInfo.callSite.source ?: return false
    if (calleeSource.startOffset == callSource.startOffset && calleeSource.endOffset == callSource.endOffset) return false
    return callSource.startOffset <= calleeSource.startOffset &&
        calleeSource.endOffset <= callSource.endOffset
}

/**
 * 去掉 CFIR wrapped expression，取得实际表达式。
 */
private tailrec fun org.cangnova.cangjie.cfir.expressions.CfirExpression.unwrapWrappedExpression():
    org.cangnova.cangjie.cfir.expressions.CfirExpression = when (this) {
    is CfirWrappedExpression -> expression.unwrapWrappedExpression()
    else -> this
}

/**
 * 从 source 文本近似提取类型名，用于错误恢复诊断参数。
 */
private fun CjSourceElement.toApproxTypeName(): Name {
    val rawText = text?.toString().orEmpty()
    val simplified = rawText.substringAfterLast('.').substringBefore('<').substringBefore('&').trim()
    return Name.identifierIfValid(simplified) ?: Name.ERROR_NAME
}

/**
 * 构造只覆盖 source 首字符的诊断 source。
 */
private fun CjSourceElement.firstCharacterDiagnosticSource(): CjOffsetsOnlySourceElement {
    return CjOffsetsOnlySourceElement(startOffset, (startOffset + 1).coerceAtMost(endOffset))
}

/**
 * 返回成员访问最左侧显式 receiver 的完整 token 范围。
 *
 * PSI 路径直接使用 receiver 语法节点；LightTree 路径用同一仓颉 lexer 取得首个实际 token，
 * 避免把官方单字符锚点扩展成整个访问表达式。
 */
private fun CjSourceElement.receiverTokenDiagnosticSource(): AbstractCjSourceElement {
    val receiverPsi = when (val sourcePsi = psi) {
        is CjQualifiedExpression -> sourcePsi.receiverExpression
        is CjCallExpression -> (sourcePsi.calleeExpression as? CjQualifiedExpression)?.receiverExpression
        is CjBinaryExpression -> (sourcePsi.left as? CjQualifiedExpression)?.receiverExpression
        else -> null
    }
    if (receiverPsi != null) return receiverPsi.toCjPsiSourceElement()

    val sourceText = text?.toString().orEmpty()
    if (sourceText.isEmpty()) return this
    val lexer = CangJieLexer()
    lexer.start(sourceText, 0, sourceText.length)
    if (lexer.tokenType == null) return this
    val tokenStart = lexer.tokenStart
    val tokenEnd = lexer.tokenEnd
    if (tokenEnd <= tokenStart) return this
    return CjOffsetsOnlySourceElement(
        startOffset + tokenStart,
        (startOffset + tokenEnd).coerceAtMost(endOffset),
    )
}

/**
 * 判断 source 是否表示赋值表达式。
 */
private fun CjSourceElement?.isAssignmentExpression(): Boolean {
    val psi = this?.psi
    return psi is CjBinaryExpression && CjPsiUtil.isAssignment(psi)
}

/**
 * 判断 source 是否位于赋值表达式左侧。
 */
private fun CjSourceElement?.isAssignmentLeftHandSide(): Boolean {
    val psiExpression = this?.psi as? CjExpression ?: return false
    if (psiExpression.getAssignmentByLHS() != null) return true

    val sourceRange = psiExpression.textRange ?: return false
    val assignment = PsiTreeUtil.getParentOfType(psiExpression, CjBinaryExpression::class.java, true) ?: return false
    if (!CjPsiUtil.isAssignment(assignment)) return false
    val lhsRange = assignment.left?.textRange ?: return false
    return lhsRange.startOffset <= sourceRange.startOffset && sourceRange.endOffset <= lhsRange.endOffset
}

/**
 * 类型不匹配诊断的语义目标位置。
 */
private sealed interface TypeMismatchTarget {
    /**
     * return 表达式中的返回值类型不匹配。
     *
     * @property expressionSource returned expression 的 source。
     */
    data class ReturnExpression(val expressionSource: AbstractCjSourceElement) : TypeMismatchTarget
    /** 字段初始化器类型不匹配，由字段初始化专用检查器负责。 */
    data object FieldInitializer : TypeMismatchTarget
}

/**
 * 在 PSI 树中定位当前 source 所属的类型不匹配语义目标。
 */
private fun CjPsiSourceElement.findTypeMismatchTarget(): TypeMismatchTarget? {
    var current: com.intellij.psi.PsiElement? = psi
    while (current != null) {
        when (current) {
            is CjReturnExpression -> {
                if (PsiTreeUtil.isAncestor(current.returnedExpression, psi, false)) {
                    val expressionSource = (current.returnedExpression as? CjExpression)?.toCjPsiSourceElement()
                        ?: return null
                    return TypeMismatchTarget.ReturnExpression(expressionSource)
                }
            }

            is CjFieldVariable -> if (PsiTreeUtil.isAncestor(current.initializer, psi, false)) {
                return TypeMismatchTarget.FieldInitializer
            }
        }
        current = current.parent
    }
    return null
}

/**
 * 在 LightTree 父链中定位当前 source 所属的类型不匹配语义目标。
 *
 * 非 PSI LLT 路径没有 PSI parent 可用，必须直接沿 lighter tree 识别
 * `return expr`，否则构造调用等根表达式的 expected-type mismatch 会退化成
 * 普通 TYPE_MISMATCH。
 */
private fun CjLightSourceElement.findTypeMismatchTarget(): TypeMismatchTarget? {
    var current: LighterASTNode? = lighterASTNode
    while (current != null) {
        if (current.tokenType == CjNodeTypes.RETURN) {
            val returnedExpression = current.returnedExpressionChildContaining(this) ?: return null
            return TypeMismatchTarget.ReturnExpression(
                returnedExpression.toCjLightSourceElement(
                    tree = treeStructure,
                    kind = kind,
                    startOffset = treeStructure.getStartOffset(returnedExpression),
                    endOffset = treeStructure.getEndOffset(returnedExpression),
                )
            )
        }
        current = treeStructure.getParent(current)
    }
    return null
}

/** 取得 return 节点中包含当前诊断 source 的返回值表达式 child。 */
private fun LighterASTNode.returnedExpressionChildContaining(source: CjLightSourceElement): LighterASTNode? {
    val childrenRef = Ref<Array<LighterASTNode>>()
    source.treeStructure.getChildren(this, childrenRef)
    return childrenRef.get().firstOrNull { child ->
        child.tokenType != CjTokens.RETURN_KEYWORD &&
            source.treeStructure.getStartOffset(child) <= source.startOffset &&
            source.endOffset <= source.treeStructure.getEndOffset(child)
    }
}

/**
 * 将 source 转换为类型不匹配语义目标。
 */
private fun CjSourceElement?.typeMismatchTarget(): TypeMismatchTarget? {
    return when (this) {
        is CjPsiSourceElement -> findTypeMismatchTarget()
        is CjLightSourceElement -> unwrapToCjPsiSourceElement()?.findTypeMismatchTarget()
            ?: findTypeMismatchTarget()
        else -> null
    }
}

/**
 * 约束不匹配中的实际类型 Cone 表示。
 */
private val ConstraintMismatch.lowerConeType: ConeCangJieType
    get() = lowerType.asCone()

/**
 * 约束不匹配中的期望上界类型 Cone 表示。
 */
private val ConstraintMismatch.upperConeType: ConeCangJieType
    get() = upperType.asCone()

/**
 * 为诊断工厂构造最小诊断上下文。
 */
private fun diagnosticContext(session: CfirSession): DiagnosticContext {
    return object : DiagnosticContext {
        override val languageVersionSettings = session.languageVersionSettings
        override val containingFilePath: String? = null
        override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean = false
    }
}

/**
 * 使用当前 session 上下文创建一参数诊断。
 */
@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A> CjDiagnosticFactory1<A>.on(
    source: AbstractCjSourceElement,
    a: A,
    session: CfirSession,
): CjDiagnostic? = on(source, a, null, diagnosticContext(session))

/**
 * 使用当前 session 上下文创建零参数诊断。
 */
@OptIn(InternalDiagnosticFactoryMethod::class)
private fun CjDiagnosticFactory0.on(
    source: AbstractCjSourceElement,
    session: CfirSession,
): CjDiagnostic? = on(source, null, diagnosticContext(session))

/**
 * 使用当前 session 上下文创建二参数诊断。
 */
@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A, B> CjDiagnosticFactory2<A, B>.on(
    source: AbstractCjSourceElement,
    a: A,
    b: B,
    session: CfirSession,
): CjDiagnostic? = on(source, a, b, null, diagnosticContext(session))

/**
 * 使用当前 session 上下文创建 unresolved-reference 风格的二参数字符串诊断。
 */
@OptIn(InternalDiagnosticFactoryMethod::class)
private fun CjDiagnosticFactory2<String, String?>.on(
    source: AbstractCjSourceElement,
    a: String,
    b: String?,
    session: CfirSession,
): CjDiagnostic? = on(source, a, b, null, diagnosticContext(session))

/**
 * 使用可空 source 创建二参数字符串诊断，source 必须在调用点已保证存在。
 */
@OptIn(InternalDiagnosticFactoryMethod::class)
private fun CjDiagnosticFactory2<String, String?>.createOn(
    source: AbstractCjSourceElement?,
    a: String,
    b: String?,
    session: CfirSession,
): CjDiagnostic? = on(source.requireNotNull(), a, b, null, diagnosticContext(session))

/**
 * 使用当前 session 上下文创建三参数诊断。
 */
@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A, B, C> CjDiagnosticFactory3<A, B, C>.on(
    source: AbstractCjSourceElement,
    a: A,
    b: B,
    c: C,
    session: CfirSession,
): CjDiagnostic? = on(source, a, b, c, null, diagnosticContext(session))

/**
 * 使用当前 session 上下文创建四参数诊断。
 */
@OptIn(InternalDiagnosticFactoryMethod::class)
private fun <A, B, C, D> CjDiagnosticFactory4<A, B, C, D>.on(
    source: AbstractCjSourceElement,
    a: A,
    b: B,
    c: C,
    d: D,
    session: CfirSession,
): CjDiagnostic? = on(source, a, b, c, d, null, diagnosticContext(session))
