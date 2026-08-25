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

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostic.*
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.*
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedErrorReference
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirErrorReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.calls.substituteExplicitTypeArgumentConstraints
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintMismatch
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.safeSubstitute
import org.cangnova.cangjie.type.model.typeConstructor
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * 取得候选在当前调用语法中的初始表达式类型，并应用候选当前 substitutor。
 *
 * 同一变量在 `f` 与 `f(...)` 中分别表示函数值和调用结果。expected-return 阶段、
 * 重载规约及 lambda body completion 必须共享这一投影，否则会把 `(P) -> R` 错当作
 * `f(...)` 的结果类型并提前淘汰合法候选。
 */
fun BodyResolveComponents.initialTypeOfCandidate(candidate: Candidate): ConeCangJieType {
    if (candidate.usesCallResultTypeForInitialProjection()) {
        // enum constructor 的表达式结果是 owner enum 类型；函数值变量的调用结果是函数类型的
        // return type。candidate.substitutedReturnType 已应用声明类型参数替换，这里只继续应用
        // 当前约束系统，供 expected-return 过滤和嵌套调用目标类型选择读取真实调用结果类型。
        val system = candidate.system
        val declaredResultType = candidate.localLambdaInitializerFinalResultTypeOrNull()
            ?: candidate.substitutedReturnType()
        return system.buildCurrentSubstitutor()
            .safeSubstitute(system, declaredResultType)
            .asCone()
    }
    val type = typeFromSymbol(candidate.symbol)
    return type.initialTypeOfCandidate(candidate)
}

/**
 * 无期望类型的局部 lambda 初始化器完成 body 重算后，最终函数类型写在 initializer 的
 * 匿名函数表达式上；变量声明自身的 returnTypeRef 不会回写（synthetic 路径的
 * `applyCompletionResult` 以 `variable = null` 运行）。函数值调用投影若继续读声明类型，
 * 未固定的形参/返回占位会以 TypeVariable 泄漏给外层重载集合，使 `println(f19(...))`
 * 这类调用误报 AMBIGUOUS_FUNCTION_CALL。这里取重算后的真实返回类型。
 */
private fun Candidate.localLambdaInitializerFinalResultTypeOrNull(): ConeCangJieType? {
    val variable = symbol.cfir as? CfirVariable ?: return null
    val lambdaExpression = variable.initializer as? org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
        ?: return null
    if (variable.localLambdaInitializerInferenceDataOrNull() == null) return null
    val finalFunctionType = lambdaExpression.coneTypeOrNull as? org.cangnova.cangjie.cfir.types.ConeFunctionType
        ?: return null
    return finalFunctionType.returnType
}

/** 需要使用调用结果类型而非符号作为值时的声明类型。 */
private fun Candidate.usesCallResultTypeForInitialProjection(): Boolean =
    symbol.cfir is CfirEnumConstructor ||
            (callInfo.callKind == CallKind.Function && symbol.cfir is CfirVariable)

/**
 * 类型参数符号对应的默认类型。
 */
val CfirTypeParameterSymbol.defaultType: ConeTypeParameterType
    get() = ConeTypeParameterTypeImpl(toLookupTag())

/**
 * 查找当前类型到目标 class/interface 的唯一已实例化父类型视图。
 *
 * 该查询统一交给类型系统的 corresponding-supertype 算法，使继承链上的泛型替换、
 * extend 超类型和多路径继承都使用与 subtype 检查相同的语义。
 */
fun ConeCangJieType.findCorrespondingClassLikeSupertype(
    session: CfirSession,
    targetClassId: ClassId,
): ConeClassLikeType? {
    val sourceType = fullyExpandedType(session) as? ConeRigidType ?: return null
    val targetType = session.symbolProvider.getClassLikeSymbolByClassId(targetClassId)
        ?.constructType() as? ConeRigidType
        ?: return null
    val targetConstructor = with(session.typeContext) { targetType.typeConstructor() }
    val typeCheckerState = session.typeContext.newTypeCheckerState(
        errorTypesEqualToAnything = false,
        stubTypesEqualToAnything = false,
    )
    val correspondingTypes = AbstractTypeChecker.findCorrespondingSupertypes(
        typeCheckerState,
        sourceType,
        targetConstructor,
    ).mapNotNull { type -> type as? ConeClassLikeType }
    val first = correspondingTypes.firstOrNull() ?: return null
    return first.takeIf { candidate ->
        correspondingTypes.all { current ->
            AbstractTypeChecker.equalTypes(session.typeContext, candidate, current)
        }
    }
}

/**
 * 从可解析访问表达式中提取表达式类型。
 */
fun <T : CfirResolvable> BodyResolveComponents.typeFromCallee(access: T): ConeCangJieType {
    if (access is CfirQualifiedAccessExpression && access.typeArguments.isNotEmpty()) {
        val classifierSymbol = (access.calleeReference as? CfirResolvedNamedReference)
            ?.resolvedSymbol as? CfirClassifierSymbol<*>
        if (classifierSymbol != null) {
            val typeArguments = access.typeArguments.map { typeArgument ->
                typeArgument.coneTypeOrNull ?: return ConeErrorType(
                    ConeSimpleDiagnostic("Unresolved qualifier type argument", DiagnosticKind.Other)
                )
            }
            val expectedCount = (classifierSymbol.cfir as? CfirTypeParameterRefsOwner)?.typeParameters?.size ?: 0
            if (expectedCount != typeArguments.size) {
                return ConeErrorType(
                    diagnostic = ConeGenericArgumentNoMatchError(
                        expectedCount = expectedCount,
                        actualCount = typeArguments.size,
                    ),
                    delegatedType = classifierSymbol.constructTypeForQualifiedAccess(emptyList()),
                    typeArguments = typeArguments,
                )
            }
            return classifierSymbol.constructTypeForQualifiedAccess(typeArguments)
        }
    }
    return typeFromCallee(access.calleeReference)
}

/**
 * 从 callee reference 中提取表达式类型。
 */
fun BodyResolveComponents.typeFromCallee(calleeReference: CfirReference): ConeCangJieType {
    return when (calleeReference) {
        is CfirErrorReferenceWithCandidate -> {
            val candidate = calleeReference.candidate
            if (candidate.hasExplicitTypeArgumentConstraintMismatchForNominalRecovery()) {
                return candidate.substituteExplicitTypeArgumentConstraints(candidate.substitutedReturnType())
            }
            // 其他失败候选只承载根诊断，不能把声明的名义返回类型暴露给外围表达式。
            ConeErrorType(ConeUnreportedDuplicateDiagnostic(calleeReference.diagnostic))
        }

        is CfirNamedReferenceWithCandidate -> {
            val candidate = calleeReference.candidate
            // 函数类型变量以调用形式使用时，对齐 synthetic invoke：表达式类型是函数类型的返回值。
            if (candidate.callInfo.callKind == CallKind.Function && candidate.symbol.cfir is CfirVariable) {
                return candidate.substitutedReturnType()
            }
            if (candidate.symbol.cfir is CfirEnumConstructor) {
                return candidate.substitutedReturnType()
            }
            when (candidate.callInfo.callKind) {
                CallKind.NamedValueAccess -> typeFromNamedValueCandidate(candidate)
                // 声明返回类型进入调用表达式时必须先把 `This` 视图绑定到调用点接收者，
                // 否则 `recv.f()` 会停留在声明所属类的 `This` 上，后续成员解析看不到子类成员。
                else -> candidate.bindThisTypeToCallSite(typeFromSymbol(candidate.symbol))
            }
        }

        is CfirErrorNamedReference -> {

            ConeErrorType(ConeUnreportedDuplicateDiagnostic(calleeReference.diagnostic))
        }

        is CfirResolvedAppliedCallableReference -> {
            if (calleeReference.resolvedSymbol.cfir is CfirEnumConstructor) {
                calleeReference.substitutedReturnType
                    ?: typeFromSymbol(calleeReference.resolvedSymbol)
            } else {
                typeFromSymbol(calleeReference.resolvedSymbol)
            }
        }

        is CfirResolvedNamedReference -> {
            typeFromSymbol(calleeReference.resolvedSymbol)
        }

        is CfirThisReference -> {
            val possibleImplicitReceivers = implicitValueStorage[null]
            when {
                possibleImplicitReceivers.size >= 2 -> ConeErrorType(
                    ConeSimpleDiagnostic("Ambiguous implicit this", DiagnosticKind.Other)
                )

                possibleImplicitReceivers.isEmpty() -> ConeErrorType(
                    ConeSimpleDiagnostic("Unresolved this", DiagnosticKind.Other)
                )

                else -> possibleImplicitReceivers.single().type
            }
        }

        is CfirSuperReference -> {
            calleeReference.superTypeRef.coneTypeOrNull
                ?: ConeErrorType(ConeSimpleDiagnostic("Unresolved super type", DiagnosticKind.Other))
        }

        else -> errorWithAttachment("Failed to extract type from: ${calleeReference::class.simpleName}") {
            withCfirEntry("reference", calleeReference)
        }
    }
}

/**
 * 显式类型实参的上界违例不应抹掉构造调用的 nominal 结果类型。
 * 具体诊断由 upper-bound checker 报告，外围表达式仍需使用候选声明返回类型完成成员解析。
 */
private fun Candidate.hasExplicitTypeArgumentConstraintMismatchForNominalRecovery(): Boolean =
    explicitTypeArgumentsForNominalRecovery().isNotEmpty() &&
        (errors.any { it is ConstraintMismatch } || system.hasContradiction)

private fun Candidate.explicitTypeArgumentsForNominalRecovery(): List<CfirTypeRef> =
    callInfo.typeArguments.ifEmpty {
        (callInfo.callSite as? CfirQualifiedAccessExpression)?.typeArguments.orEmpty()
    }

/**
 * 构造命名值候选作为表达式时的类型。
 */
private fun BodyResolveComponents.typeFromNamedValueCandidate(candidate: Candidate): ConeCangJieType {
    val declaration = candidate.symbol.cfir
    if (declaration !is CfirFunction) {
        if (declaration is CfirEnumConstructor) {
            return candidate.substitutedReturnType()
        }
        return typeFromSymbol(candidate.symbol)
    }

    return functionTypeForFunctionValueCandidate(candidate, declaration)
}

/**
 * 构造“函数名作为值”时的函数类型。
 *
 * 仓颉函数是一等值，`let f: (Int64) -> Unit = g` 这类引用不能使用
 * `g` 的返回值类型，而必须使用完整函数签名参与后续期望类型与重载解析。
 */
fun BodyResolveComponents.functionTypeForFunctionValueCandidate(
    candidate: Candidate,
    declaration: CfirFunction = candidate.symbol.cfir as CfirFunction,
): ConeCangJieType {
    val parameterTypes = declaration.valueParameters.map { parameter ->
        val resolvedType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            ?: return ConeErrorType(ConeSimpleDiagnostic("Unresolved function parameter type", DiagnosticKind.Other))
        candidate.substitutor.substituteOrSelf(resolvedType)
    }

    val calculatedReturnType = returnTypeCalculator.tryCalculateReturnType(declaration).coneType
    val substitutedReturnType = candidate.substitutedReturnType(calculatedReturnType).approximateThisTypeForDeclaration()

    return ConeFunctionType(parameterTypes, substitutedReturnType)
}

/**
 * 从已解析符号中提取默认表达式类型。
 */
private fun BodyResolveComponents.typeFromSymbol(symbol: CfirBasedSymbol<*>): ConeCangJieType {
    return when (symbol) {
        is CfirCallableSymbol<*> -> {
            val returnTypeRef = returnTypeCalculator.tryCalculateReturnType(symbol.cfir)
            returnTypeRef.coneType
        }

        is CfirClassifierSymbol<*> -> {
            symbol.constructDefaultQualifierType()
        }

        else -> errorWithAttachment("Failed to extract type from symbol: ${symbol::class.java}") {
            withCfirEntry("declaration", symbol.cfir)
        }
    }
}

/**
 * 裸 classifier 作为 qualifier 时需要保留声明侧类型参数。
 *
 * `C.test(1, 2)` 中 `type C<T> = A<T, Int16>` 的 qualifier 语义不是 `C<>`，
 * 而是携带 `C<T>`，后续 owner use-site substitution 才能把 `A<T, Int16>`
 * 与 static 成员 `A.test` 的外层类型参数接通并交给调用推断。
 */
private fun CfirClassifierSymbol<*>.constructDefaultQualifierType(): ConeCangJieType = when (this) {
    is CfirTypeParameterSymbol -> constructType()
    is CfirTypeAliasSymbol -> {
        val typeArguments = cfir.typeParameters.map { typeParameter ->
            ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())
        }
        ConeTypeAliasType(classId, typeArguments = typeArguments)
    }

    is CfirClassLikeSymbol<*> -> {
        val typeArguments = (cfir as? CfirTypeParameterRefsOwner)
            ?.typeParameters
            ?.map { typeParameter -> ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag()) }
            .orEmpty()
        constructType(typeArguments)
    }
}

/**
 * 使用显式类型实参构造限定访问中的分类器类型。
 */
private fun CfirClassifierSymbol<*>.constructTypeForQualifiedAccess(
    typeArguments: List<ConeTypeProjection>,
): ConeCangJieType = when (this) {
    is CfirTypeAliasSymbol -> ConeTypeAliasType(classId, typeArguments = typeArguments)
    else -> constructType(typeArguments)
}

/**
 * 根据候选适用性错误构造 cone 诊断。
 */
fun createConeDiagnosticForCandidateWithError(
    applicability: CandidateApplicability,
    candidate: Candidate,
): org.cangnova.cangjie.cfir.types.ConeDiagnostic {
    val objectCannotAccessStaticMember =
        candidate.diagnostics.firstOrNull { it is ObjectCannotAccessStaticMember } as? ObjectCannotAccessStaticMember
    if (objectCannotAccessStaticMember != null) {
        return ConeObjectCannotAccessStaticMemberError(objectCannotAccessStaticMember.memberName, candidate)
    }

    val illegalAccessNonStaticMember =
        candidate.diagnostics.firstOrNull { it is IllegalAccessNonStaticMember } as? IllegalAccessNonStaticMember
    if (illegalAccessNonStaticMember != null) {
        return ConeIllegalAccessNonStaticMemberError(illegalAccessNonStaticMember.memberName, candidate)
    }

    val visibilityError = candidate.diagnostics.firstOrNull { it is VisibilityError } as? VisibilityError
    if (visibilityError != null) {
        return ConeVisibilityError(visibilityError.symbol)
    }
    if (candidate.system.hasContradiction) {
        return ConeConstraintSystemHasContradiction(candidate)
    }

    return when (applicability) {
        CandidateApplicability.HIDDEN -> ConeHiddenCandidateError(candidate)
        CandidateApplicability.VISIBILITY_ERROR -> {
            ConeVisibilityError(visibilityError?.symbol ?: candidate.symbol)
        }

        else -> ConeInapplicableCandidateError(applicability, candidate)
    }
}

/**
 * 判断解析结果是否带有“保留兼容性时覆盖其他结果”的诊断标记。
 */
internal fun Candidate.doesResolutionResultOverrideOtherToPreserveCompatibility(): Boolean =
    diagnostics.any { it === ResolutionResultOverridesOtherToPreserveCompatibility }

/**
 * 返回 typealias 完整展开后的 class-like 符号。
 */
fun CfirTypeAliasSymbol.fullyExpandedClass(session: CfirSession): CfirClassLikeSymbol<*>? {
    if (!isBound) return null
    val expandedType = cfir.expandedTypeRef.coneTypeOrNull ?: return null
    val classId = when (expandedType) {
        is ConeClassLikeType -> expandedType.classId
        is ConeStructType -> expandedType.classId
        is ConeEnumType -> expandedType.classId
        is ConeTypeAliasType -> expandedType.expandedType?.let { nested ->
            when (nested) {
                is ConeClassLikeType -> nested.classId
                is ConeStructType -> nested.classId
                is ConeEnumType -> nested.classId
                else -> expandedType.classId
            }
        } ?: expandedType.classId

        else -> return null
    }
    return session.symbolProvider.getClassLikeSymbolByClassId(classId)
}

/**
 * 将携带候选的命名引用转换为错误引用。
 */
fun CfirNamedReferenceWithCandidate.toErrorReference(diagnostic: ConeDiagnostic): CfirNamedReference {
    val calleeReference = this
    val errorSource = calleeReference.source ?: calleeReference.candidate.callInfo.callSite.source
    return when (calleeReference.candidateSymbol) {
        is CfirErrorFunctionSymbol,
        is CfirErrorNamedValueSymbol -> buildErrorNamedReference {
            source = errorSource
            name = calleeReference.name
            this.diagnostic = diagnostic
        }

        else -> buildResolvedErrorReference {
            source = errorSource
            name = calleeReference.name
            resolvedSymbol = calleeReference.candidateSymbol
            this.diagnostic = diagnostic
        }
    }
}

/**
 * 对候选初始类型应用当前约束系统 substitutor 和候选 substitutor。
 */
fun ConeCangJieType.initialTypeOfCandidate(candidate: Candidate): ConeCangJieType {
    val system = candidate.system
    val resultingSubstitutor = system.buildCurrentSubstitutor()
    return resultingSubstitutor.safeSubstitute(system, candidate.substitutor.substituteOrSelf(this)).asCone()
}
