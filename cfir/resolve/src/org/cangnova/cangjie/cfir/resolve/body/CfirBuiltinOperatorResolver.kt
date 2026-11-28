package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperatorMatch
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperators
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperatorSignature
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedBuiltinOperatorReference
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirResolvedBuiltinOperatorReference
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.diagnostic.ConeNoMatchOperatorFunctionCallError
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.name.Name

/** 内建基础类型运算符的调用解析入口。 */
object CfirBuiltinOperatorResolver {
    /** 唯一成功路径统一提交签名、内建引用和结果类型；不创建虚构的运算符声明。 */
    fun publishResult(
        call: CfirFunctionCall,
        signature: BuiltinPrimitiveOperatorSignature,
        resultType: ConeCangJieType,
    ): CfirFunctionCall {
        val reference = call.calleeReference as CfirNamedReference
        val resultKind = checkNotNull(BuiltinPrimitiveOperators.primitiveOperandKind(resultType)) {
            "A successful primitive operator must have a primitive result"
        }
        val completedSignature = signature.copy(returnKind = resultKind)
        if (reference !is CfirResolvedBuiltinOperatorReference || reference.signature != completedSignature) {
            call.replaceCalleeReference(buildResolvedBuiltinOperatorReference {
                source = reference.source
                name = signature.name
                this.signature = completedSignature
            })
        }
        if (call.coneTypeOrNull != resultType) call.replaceConeTypeOrNull(resultType)
        return call
    }

    /** 子运算类型替换／Ideal 定型后同步重建绑定，不能保留旧参数类型的成功引用。 */
    fun completeResolution(call: CfirFunctionCall): Boolean {
        val reference = call.calleeReference as? CfirResolvedBuiltinOperatorReference ?: return false
        // 根诊断与内建绑定独立；近似子输入不能把已发布的失败改回成功表达式。
        if (call.coneTypeOrNull is ConeErrorType) return true
        val receiver = call.explicitReceiver ?: call.dispatchReceiver ?: return false
        val receiverType = receiver.coneTypeOrNull ?: return false
        val arguments = call.argumentList.arguments.map { it.coneTypeOrNull ?: return false }
        val operandError = (listOf(receiverType) + arguments).filterIsInstance<ConeErrorType>().firstOrNull()
        if (operandError != null) {
            publishFailure(call, operandError, propagated = true)
            return true
        }
        // PCLA 尚未落地的变量继续保留在 completion 域，不冒充 FINAL 成功输入。
        if ((listOf(receiverType) + arguments).any { BuiltinPrimitiveOperators.primitiveOperandKind(it) == null }) return false
        val match = tryResolveBuiltinOperator(reference.name, receiverType, arguments)
        if (match == null) publishFailure(call, ConeErrorType(ConeNoMatchOperatorFunctionCallError))
        else publishResult(call, match.signature, match.returnType)
        return true
    }

    /** 错误也是明确的绑定终态；已有 operand 诊断不在 operator 引用上重复报告。 */
    fun publishFailure(call: CfirFunctionCall, error: ConeErrorType, propagated: Boolean = false): CfirFunctionCall {
        val reference = call.calleeReference as CfirNamedReference
        call.replaceCalleeReference(buildErrorNamedReference {
            source = reference.source
            name = reference.name
            diagnostic = if (propagated) ConeUnreportedDuplicateDiagnostic(error.diagnostic) else error.diagnostic
        })
        call.replaceConeTypeOrNull(error)
        return call
    }

    /** 尝试按名称、接收者类型和参数类型匹配内建基础类型运算符。 */
    fun tryResolveBuiltinOperator(
        name: Name,
        receiverType: ConeCangJieType?,
        argumentTypes: List<ConeCangJieType>,
    ): BuiltinPrimitiveOperatorMatch? =
        BuiltinPrimitiveOperators.resolve(name, receiverType, argumentTypes)
}
