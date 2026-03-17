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
 * 鍙傛暟鏄犲皠闃舵锛氬皢璋冪敤鐨勫疄鍙傛槧灏勫埌鍑芥暟鐨勫舰鍙傘€? *
 * 鏍稿績閫昏緫锛? * 1. 浣嶇疆鍙傛暟鎸夊簭鏄犲皠
 * 2. 甯﹂粯璁ゅ€肩殑褰㈠弬鍙烦杩囷紙璁″叆 numDefaults锛? * 3. 瀹炲弬鏁伴噺蹇呴』鍦?[minRequired, totalParams] 鑼冨洿鍐? *
 * Phase 3 浠呮敮鎸佷綅缃弬鏁帮紝涓嶆敮鎸佸懡鍚嶅弬鏁帮紙浠撻鏆傛棤鍛藉悕鍙傛暟璇硶锛夈€? *
 * 瀵归綈 K2 MapArguments + FirArgumentsToParametersMapper銆? */
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

        // 妫€鏌ュ弬鏁版暟閲?
        if (actualArgs < requiredParams || actualArgs > totalParams) {
            sink.reportDiagnostic(
                WrongArgumentCount(
                    expectedCount = if (requiredParams == totalParams) totalParams else requiredParams,
                    actualCount = actualArgs,
                )
            )
            return
        }

        // 浣嶇疆鍙傛暟鏄犲皠锛歛rgIndex 鈫?paramIndex
        val mapping = mutableMapOf<Int, Int>()
        for (i in 0 until actualArgs) {
            mapping[i] = i
        }
        candidate.argumentMapping = mapping

        // 璁＄畻浣跨敤鐨勯粯璁ゅ€煎弬鏁版暟閲?
        candidate.numDefaults = totalParams - actualArgs
    }

    /** 浠庡€欓€夌鍙蜂腑鎻愬彇鍊煎弬鏁板垪琛?*/
    private fun extractValueParameters(symbol: CfirCallableSymbol<*>): List<CfirValueParameter>? {
        if (!symbol.isBound) return null
        return when (val decl = symbol.cfir) {
            is CfirFunction -> decl.valueParameters
            is CfirConstructor -> decl.valueParameters
            else -> null
        }
    }
}

