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

import org.cangnova.cangjie.cfir.resolve.substitution.asCone

import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.interopInfo
import org.cangnova.cangjie.cfir.diagnostic.*
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.providers.findExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.providers.semanticExtendedType
import org.cangnova.cangjie.cfir.session.accessibilityChecker
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
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
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintMismatch
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.resolve.calls.tower.CandidateApplicability
import org.cangnova.cangjie.type.model.safeSubstitute
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * 仓颉 class 与 extend 都只有一个运行时对象槽。
 * extend 的 chosen extension 是实际对象，candidate dispatch 可以只是查找宿主；
 * 普通成员和函数值 invoke 保留其正式 dispatch，不改变推断阶段 D/E 两种角色。
 */
internal fun Candidate.selectedInstanceReceiverExpression(): CfirExpression? {
    // 外层 invoke 的 symbol 仍指向内层成员；对象是已取得的函数值，不能再取内层 extend E。
    if (callInfo.candidateForCommonInvokeReceiver != null) return dispatchReceiverExpression()
    return if (instanceExtendOwnerOrNull() != null) chosenExtensionReceiverExpression() else dispatchReceiverExpression()
}

private fun Candidate.instanceExtendOwnerOrNull(): CfirExtend? {
    val declaration = symbol.cfir as? CfirCallableDeclaration ?: return null
    val isInstanceMember = !declaration.status.isStatic && !declaration.isLocal &&
        (declaration is CfirFieldVariable || declaration is CfirProperty || declaration is CfirNamedFunction ||
            declaration is CfirPropertyAccessor)
    return if (isInstanceMember) (symbol as? CfirCallableSymbol<*>)?.getContainingExtend() else null
}

/** 成员检查、fresh receiver 规约与 completion 共用正式 extend 声明/用点代换期望类型。 */
internal fun Candidate.expectedExtensionReceiverType(session: CfirSession): ConeCangJieType? {
    val owner = (symbol as? CfirCallableSymbol<*>)?.getContainingExtend() ?: return null
    val access = CfirAccessContext(callInfo.containingFile, callInfo.containingDeclarations,
        receiverType = givenExtensionReceiver?.expression?.coneTypeOrNull, kind = CfirAccessKind.EXTEND)
    if (session.accessibilityChecker.checkExtend(owner, access) !is CfirAccessibilityResult.Accessible) return null
    val actualType = givenExtensionReceiver?.expression?.coneTypeOrNull
    if (actualType != null) {
        findExtendDeclarationSubstitution(session, owner, actualType)?.substitutedReceiverType?.let { return it }
    }
    // 尚未取得实例化代换时，约束检查的输入仍是该已绑定 extend 自己的声明类型。
    return owner.semanticExtendedType(session)
}

/** 物理 receiver 的正式期望类型独立于其当前表达式类型；函数值调用消费已取得的 callee 类型。 */
internal fun Candidate.selectedInstanceReceiverExpectedType(session: CfirSession): ConeCangJieType? = when {
    callInfo.callKind == CallKind.Function && isFunctionTypeCallableValueCall -> dispatchReceiverExpression()?.coneTypeOrNull
    instanceExtendOwnerOrNull() != null -> if (isSuccessful) expectedExtensionReceiverType(session) else null
    else -> (symbol as? CfirCallableSymbol<*>)?.dispatchReceiverType
}

/** 同一次候选完成发布对象；成功显式成员访问同步到同一个完成后的对象 identity。 */
internal fun CfirQualifiedAccessExpression.replaceCandidateReceivers(receiver: CfirExpression?, candidate: Candidate) {
    replaceDispatchReceiver(receiver)
    if (candidate.isSuccessful && receiver != null &&
        (candidate.explicitReceiverKind == ExplicitReceiverKind.DISPATCH_RECEIVER ||
            candidate.explicitReceiverKind == ExplicitReceiverKind.EXTENSION_RECEIVER)) {
        replaceExplicitReceiver(receiver)
    }
}

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
        val declaredResultType = candidate.substitutedReturnTypeFromCalculator()
        // 类型投影只读取固定绑定；FULL 已冻结的系统不能再进入 builder/analyzer 上下文。
        return system.captureStorage().buildCurrentSubstitutor(system, emptyMap())
            .safeSubstitute(system, declaredResultType)
            .asCone()
    }
    val type = typeFromSymbol(candidate.symbol)
    return type.initialTypeOfCandidate(candidate)
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
                return candidate.substituteExplicitTypeArgumentConstraints(candidate.substitutedReturnTypeFromCalculator())
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
                return candidate.substitutedReturnTypeFromCalculator()
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
            if (calleeReference.isFunctionValue) {
                calleeReference.callSiteType
                    ?: ConeErrorType(ConeSimpleDiagnostic("Applied function value has no function type"))
            } else if (calleeReference.resolvedSymbol.cfir is CfirEnumConstructor) {
                calleeReference.callSiteType
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

/** 收集候选的显式类型实参：优先取 callInfo，qualified access 调用点回退到其自身 typeArguments。 */
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
            return candidate.substitutedReturnTypeFromCalculator()
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
    /*
     * 函数值的 CFunc 标记属于声明级 ABI 事实，producer 是 STATUS，而不是
     * 当前候选的函数值类型投影。名字查找可以在声明尚未显式推进 STATUS 时
     * 直接进入 body resolve，因此这里必须先走声明自己的 lazy phase owner。
     * 不能在缺少 interop snapshot 时按 `foreign` 或函数名兜底推断。
     */
    declaration.symbol.lazyResolveToPhase(CfirResolvePhase.STATUS)
    val parameterTypes = declaration.valueParameters.map { parameter ->
        val resolvedType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType
            ?: return ConeErrorType(ConeSimpleDiagnostic("Unresolved function parameter type", DiagnosticKind.Other))
        candidate.substitutor.substituteOrSelf(resolvedType)
    }

    val calculatedReturnType = returnTypeCalculator.tryCalculateReturnType(declaration).coneType
    val substitutedReturnType = candidate.substitutedReturnType(calculatedReturnType).approximateThisTypeForDeclaration()

    return ConeFunctionType(
        parameterTypes = parameterTypes,
        returnType = substitutedReturnType,
        isCFunc = declaration.interopInfo?.resolvedAbi?.isCFunction == true,
        hasVariableLenArg = declaration.hasVariableLenArg,
    )
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
    // 保留语义入口原来的 parent 同步，替换计算自身只消费同一实际系统的只读快照。
    val resultingSubstitutor = system.captureStorage().buildCurrentSubstitutor(system, emptyMap())
    return resultingSubstitutor.safeSubstitute(system, candidate.substitutor.substituteOrSelf(this)).asCone()
}
