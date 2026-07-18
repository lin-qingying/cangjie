package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostic.InapplicableCandidate
import org.cangnova.cangjie.cfir.diagnostic.AmbiguousArgumentType
import org.cangnova.cangjie.cfir.diagnostic.UnsuccessfulCallableReferenceArgument
import org.cangnova.cangjie.cfir.diagnostic.CallableReferenceFailureKind
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldDiagnostic
import org.cangnova.cangjie.cfir.resolve.body.CallableReferenceResolutionResult
import org.cangnova.cangjie.cfir.resolve.expectedType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.semantics.ErrorTypeInArguments
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.approximateThisTypeForDeclaration
import org.cangnova.cangjie.cfir.types.asCone
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.resolve.calls.inference.buildCurrentSubstitutor
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 用期望函数类型检查函数名作为值使用的候选。
 *
 * 对齐 Kotlin FIR `CheckCallableReferenceExpectedType`：在候选自己的约束系统中构造
 * 函数引用的结果函数类型，并加入 `resultingType <: expectedType`。仓颉函数引用语法是
 * 普通名字访问（如 `foo` / `obj.foo`），因此阶段挂在 `NamedValueAccess` 序列上。
 */
object CfirCheckCallableReferenceExpectedType : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    /** 在候选约束系统中加入 callable reference 结果函数类型到 expected type 的子类型约束。 */
    override suspend fun check(candidate: Candidate) {
        val expectedType = candidate.callInfo.resolutionMode.expectedType ?: return
        val expectedFunctionType = expectedType.fullyExpandedType(context.session) as? ConeFunctionType
        val function = candidate.symbol.takeIf { it.isBound }?.cfir as? CfirFunction ?: return
        if (expectedFunctionType != null && !candidate.hasCompatibleParameterShape(function, expectedFunctionType)) {
            sink.yieldDiagnostic(InapplicableCandidate)
            return
        }
        val resultingType = candidate.buildResultingCallableReferenceType(function, context)
        if (resultingType.hasRecursiveImplicitReturnType()) {
            sink.yieldDiagnostic(InapplicableCandidate)
            return
        }

        candidate.initializeCallableReferenceAdaptation(
            callableReferenceAdaptation = null,
            resultingTypeForCallableReference = resultingType,
        )
        val currentStorage = candidate.system.currentStorage()
        val currentResultingType = currentStorage
            .buildCurrentSubstitutor(context.typeContext, emptyMap())
            .asCone()
            .substituteOrSelf(resultingType)
        val hasNotFixedVariables = currentResultingType.contains { type ->
            type is ConeTypeVariableType && type.typeConstructor in currentStorage.notFixedTypeVariables
        }
        if (
            !hasNotFixedVariables &&
            expectedFunctionType != null &&
            !expectedFunctionType.contains { it is ConeTypeVariableType } &&
            !AbstractTypeChecker.isSubtypeOfForFunctionReference(
                context.typeContext,
                currentResultingType,
                expectedFunctionType,
            )
        ) {
            sink.yieldDiagnostic(InapplicableCandidate)
            return
        }
        candidate.system.addSubtypeConstraint(
            resultingType,
            expectedType,
            ConeArgumentConstraintPosition(candidate.callInfo.callSite),
        )
        if (candidate.system.hasContradiction) {
            sink.yieldDiagnostic(InapplicableCandidate)
        }
    }

    /** 根据函数声明与候选替换器构造 callable reference 表达式的结果函数类型。 */
    private fun Candidate.buildResultingCallableReferenceType(
        function: CfirFunction,
        context: ResolutionContext,
    ): ConeCangJieType {
        val parameterTypes = function.valueParameters.map { parameter ->
            val parameterType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                ?: return ConeErrorType(ConeSimpleDiagnostic("Unresolved function parameter type"))
            substitutor.substituteOrSelf(parameterType)
        }

        val calculatedReturnType = context.returnTypeCalculator.tryCalculateReturnType(function).coneType
        val returnType = substitutedReturnType(calculatedReturnType).approximateThisTypeForDeclaration()
        return ConeFunctionType(parameterTypes, returnType)
    }

    /** 先检查参数形状，避免明显不匹配的 overload 触发隐式返回类型计算。 */
    private fun Candidate.hasCompatibleParameterShape(
        function: CfirFunction,
        expectedType: ConeFunctionType,
    ): Boolean = function.valueParameters.size == expectedType.parameterTypes.size

    /** 判断函数引用结果类型是否依赖正在计算的隐式返回类型。 */
    private fun ConeCangJieType.hasRecursiveImplicitReturnType(): Boolean =
        contains { type ->
            type is ConeErrorType && type.diagnostic.unwrapUnreportedDuplicateDiagnostic()
                .let { diagnostic ->
                    diagnostic is ConeSimpleDiagnostic &&
                        diagnostic.kind == DiagnosticKind.RecursionInImplicitTypes
                }
        }

    /** 解开用于抑制级联诊断的 wrapper，读取原始递归类型错误。 */
    private fun ConeDiagnostic.unwrapUnreportedDuplicateDiagnostic(): ConeDiagnostic =
        (this as? ConeUnreportedDuplicateDiagnostic)?.original ?: this
}

/**
 * 在外层调用候选完成前解析函数引用实参。
 *
 * 对齐 Kotlin FIR `EagerResolveOfCallableReferences`：`CheckArguments` 创建 postponed
 * callable-reference atom 后，本阶段用外层候选的约束系统解析这些 atom。
 */
object CfirEagerResolveOfCallableReferences : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    /** 提前解析候选内尚未分析的 postponed callable-reference atom。 */
    override suspend fun check(candidate: Candidate) {
        val callableReferenceAtoms = candidate.postponedAtoms
            .filterIsInstance<org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedCallableReferenceAtom>()
            .filterNot { it.analyzed }
        if (callableReferenceAtoms.isEmpty()) return

        when (context.bodyResolveComponents.callResolver.resolveCallableReferenceArguments(candidate, callableReferenceAtoms)) {
            CallableReferenceResolutionResult.RESOLVED,
            CallableReferenceResolutionResult.POSTPONED -> return
            CallableReferenceResolutionResult.FAILURE -> {
                val failureDiagnostics = callableReferenceAtoms
                    .filter { atom -> atom.failureKind != null }
                    .map { atom ->
                        when (atom.failureKind) {
                            CallableReferenceFailureKind.AMBIGUOUS_ARGUMENT_TYPE ->
                                AmbiguousArgumentType(candidate.callInfo.callSite, atom.expression)

                            CallableReferenceFailureKind.GENERIC_TYPE_ARGUMENT_REQUIRED ->
                                ErrorTypeInArguments

                            else ->
                                UnsuccessfulCallableReferenceArgument(
                                    atom.expression,
                                    atom.failureKind ?: CallableReferenceFailureKind.NO_MATCH,
                                )
                        }
                    }
                check(failureDiagnostics.isNotEmpty()) {
                    "Callable-reference resolution failed without a classified failing atom"
                }
                // 具体 callable-reference 失败诊断同时拥有 INAPPLICABLE 适用性和错误归属。
                // 最后一个诊断负责终止当前阶段，避免再附加通用 InapplicableCandidate 产生外层调用级联。
                failureDiagnostics.dropLast(1).forEach(sink::reportDiagnostic)
                sink.yieldDiagnostic(failureDiagnostics.last())
            }
        }
    }
}
