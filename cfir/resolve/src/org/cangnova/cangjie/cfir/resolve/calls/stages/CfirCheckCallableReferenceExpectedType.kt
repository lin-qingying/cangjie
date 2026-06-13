package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostic.InapplicableCandidate
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldDiagnostic
import org.cangnova.cangjie.cfir.resolve.expectedType
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.approximateThisTypeForDeclaration

/**
 * 用期望函数类型检查函数名作为值使用的候选。
 *
 * 对齐 Kotlin FIR `CheckCallableReferenceExpectedType`：在候选自己的约束系统中构造
 * 函数引用的结果函数类型，并加入 `resultingType <: expectedType`。仓颉函数引用语法是
 * 普通名字访问（如 `foo` / `obj.foo`），因此阶段挂在 `NamedValueAccess` 序列上。
 */
object CfirCheckCallableReferenceExpectedType : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        val expectedType = candidate.callInfo.resolutionMode.expectedType ?: return
        val function = candidate.symbol.takeIf { it.isBound }?.cfir as? CfirFunction ?: return
        val resultingType = candidate.buildResultingCallableReferenceType(function, context)

        candidate.initializeCallableReferenceAdaptation(
            callableReferenceAdaptation = null,
            resultingTypeForCallableReference = resultingType,
        )
        candidate.system.addSubtypeConstraint(
            resultingType,
            expectedType,
            ConeArgumentConstraintPosition(candidate.callInfo.callSite),
        )
        if (candidate.system.hasContradiction) {
            sink.yieldDiagnostic(InapplicableCandidate)
        }
    }

    private fun Candidate.buildResultingCallableReferenceType(
        function: CfirFunction,
        context: ResolutionContext,
    ): ConeCangJieType {
        val parameterTypes = function.valueParameters.map { parameter ->
            val parameterType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType
                ?: return ConeErrorType(ConeSimpleDiagnostic("Unresolved function parameter type"))
            substitutor.substituteOrSelf(parameterType)
        }

        context.returnTypeCalculator.tryCalculateReturnType(function)
        val returnType = substitutedReturnType().approximateThisTypeForDeclaration()
        return ConeFunctionType(parameterTypes, returnType)
    }
}

/**
 * 在外层调用候选完成前解析函数引用实参。
 *
 * 对齐 Kotlin FIR `EagerResolveOfCallableReferences`：`CheckArguments` 创建 postponed
 * callable-reference atom 后，本阶段用外层候选的约束系统解析这些 atom。
 */
object CfirEagerResolveOfCallableReferences : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        val callableReferenceAtoms = candidate.postponedAtoms
            .filterIsInstance<org.cangnova.cangjie.cfir.resolve.calls.ConeResolvedCallableReferenceAtom>()
            .filterNot { it.analyzed }
        if (callableReferenceAtoms.isEmpty()) return

        if (!context.bodyResolveComponents.callResolver.resolveCallableReferenceArguments(candidate, callableReferenceAtoms)) {
            sink.yieldDiagnostic(InapplicableCandidate)
        }
    }
}
