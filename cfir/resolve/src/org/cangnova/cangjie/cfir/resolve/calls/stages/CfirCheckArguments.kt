package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.diagnostic.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 参数类型检查阶段：逐个验证实参与形参的类型兼容性。
 *
 * 对齐 K2 的 `CheckArguments`：
 * - 泛型调用：通过约束系统事务性添加约束（`addSubtypeConstraintIfCompatible`），
 *   失败时报 `ArgumentTypeMismatch`。
 * - 非泛型调用：直接使用 `isSubtypeOf` 检查。
 */
object CfirCheckArguments :  ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        if (!candidate.argumentMappingInitialized) return

        candidate.argumentMapping.entries.forEach { (argumentAtom, parameter) ->
            val argument = argumentAtom.expression
            val actualType = argument.resolvedType() ?: return@forEach
            val expectedType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: return@forEach

            if (actualType is ConeErrorType || expectedType is ConeErrorType) return@forEach

            if (shouldUseConstraintSystem(actualType, expectedType, candidate)) {
                candidate.constraintSystem.addSubtypeConstraint(
                    actualType,
                    expectedType,
                    ConeArgumentConstraintPosition(argument),
                )

                if (candidate.constraintSystem.hasContradiction) {
                    sink.reportDiagnostic(
                        ArgumentTypeMismatch(
                            expectedType = expectedType,
                            actualType = actualType,
                            argument = argument,
                            isMismatchDueToNullability = false,
                            systemHadContradiction = true,
                        )
                    )
                }
                return@forEach
            }

            if (!AbstractTypeChecker.isSubtypeOf(context.typeContext, actualType, expectedType)) {
                sink.reportDiagnostic(
                    ArgumentTypeMismatch(
                        expectedType = expectedType,
                        actualType = actualType,
                        argument = argument,
                        isMismatchDueToNullability = false,
                    )
                )
            }
        }
    }

    private fun shouldUseConstraintSystem(
        actualType: ConeCangJieType,
        expectedType: ConeCangJieType,
        candidate: Candidate,
    ): Boolean {
        return candidate.hasFreshTypeVariables() ||
            actualType is ConeTypeParameterType ||
            expectedType is ConeTypeParameterType
    }

    private fun Candidate.hasFreshTypeVariables(): Boolean =
        freshVariables.isNotEmpty()

    private fun CfirExpression.resolvedType(): ConeCangJieType? =
        coneTypeOrNull
}
