package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.WrongArgumentCount
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol

/**
 * 参数映射阶段：将调用的实参映射到函数的形参。
 *
 * 核心逻辑：
 * 1. 位置参数按序映射
 * 2. 带默认值的形参可跳过（计入 numDefaults）
 * 3. 实参数量必须在 [minRequired, totalParams] 范围内
 *
 * Phase 3 仅支持位置参数，不支持命名参数（仓颉暂无命名参数语法）。
 *
 * 对齐 K2 MapArguments + FirArgumentsToParametersMapper。
 */
object CfirMapArguments : CfirResolutionStage() {

    override fun check(
        candidate: CfirCandidate,
        sink: CfirCheckerSink,
        context: CfirResolutionContext,
    ) {
        val valueParameters = extractValueParameters(candidate.symbol) ?: return
        val arguments = candidate.callInfo.arguments

        val totalParams = valueParameters.size
        val requiredParams = valueParameters.count { it.defaultValue == null }
        val actualArgs = arguments.size

        // 检查参数数量
        if (actualArgs < requiredParams || actualArgs > totalParams) {
            sink.reportDiagnostic(
                WrongArgumentCount(
                    expectedCount = if (requiredParams == totalParams) totalParams else requiredParams,
                    actualCount = actualArgs,
                )
            )
            return
        }

        // 位置参数映射：argIndex → paramIndex
        val mapping = mutableMapOf<Int, Int>()
        for (i in 0 until actualArgs) {
            mapping[i] = i
        }
        candidate.argumentMapping = mapping

        // 计算使用的默认值参数数量
        candidate.numDefaults = totalParams - actualArgs
    }

    /** 从候选符号中提取值参数列表 */
    private fun extractValueParameters(symbol: CfirCallableSymbol<*>): List<CfirValueParameter>? {
        if (!symbol.isBound) return null
        return when (val decl = symbol.cfir) {
            is CfirFunction -> decl.valueParameters
            is CfirConstructor -> decl.valueParameters
            else -> null
        }
    }
}
