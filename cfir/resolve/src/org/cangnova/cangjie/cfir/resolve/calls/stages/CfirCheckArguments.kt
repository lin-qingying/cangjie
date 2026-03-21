package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.candidate.ArgumentTypeMismatch
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.inference.CfirConstraintPosition
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType

/**
 * 参数类型检查阶段：逐个验证实参与形参的类型兼容性。
 *
 * 对齐 K2 的 `CheckArguments`：
 * - 泛型调用：通过约束系统事务性添加约束（`addSubtypeConstraintIfCompatible`），
 *   失败时报 `ArgumentTypeMismatch`。
 * - 非泛型调用：直接使用 `isSubtypeOf` 检查。
 */
object CfirCheckArguments : CfirResolutionStage() {

    override fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    ) {
        val constraintSystem = candidate.constraintSystem
        if (constraintSystem != null) {
            checkWithConstraintSystem(candidate, sink, constraintSystem)
        } else {
            checkWithoutConstraintSystem(candidate, sink, context)
        }
    }

    /**
     * 泛型调用：通过约束系统事务性添加约束。
     */
    private fun checkWithConstraintSystem(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        constraintSystem: org.cangnova.cangjie.cfir.resolve.inference.CfirConstraintSystem,
    ) {
        for ((argumentAtom, parameter) in candidate.argumentMapping) {
            if (sink.shouldStop) return

            val argType = argumentAtom.expression.coneTypeOrNull ?: continue
            val paramType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (argType is ConeErrorType || paramType is ConeErrorType) continue

            val position = CfirConstraintPosition.ArgumentPosition(
                candidate.argumentMapping.keys.indexOf(argumentAtom)
            )

            if (!constraintSystem.addSubtypeConstraintIfCompatible(argType, paramType, position)) {
                sink.reportDiagnostic(
                    ArgumentTypeMismatch(
                        expectedType = paramType,
                        actualType = argType,
                        parameterIndex = candidate.argumentMapping.keys.indexOf(argumentAtom),
                    ),
                )
            }
        }
    }

    /**
     * 非泛型调用：直接使用 isSubtypeOf 检查（保留原有逻辑）。
     */
    private fun checkWithoutConstraintSystem(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    ) {
        for ((argumentAtom, parameter) in candidate.argumentMapping) {
            if (sink.shouldStop) return

            val argument = argumentAtom.expression
            val argType = argument.coneTypeOrNull ?: continue
            val paramType = (parameter.returnTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            val substitutedParamType = candidate.substitutor.substituteOrSelf(paramType)

            if (argType is ConeErrorType || substitutedParamType is ConeErrorType) continue
            if (!context.subtypeChecker.isSubtypeOf(argType, substitutedParamType)) {
                sink.reportDiagnostic(
                    ArgumentTypeMismatch(
                        expectedType = substitutedParamType,
                        actualType = argType,
                        parameterIndex = candidate.argumentMapping.keys.indexOf(argumentAtom),
                    ),
                )
            }
        }
    }
}
