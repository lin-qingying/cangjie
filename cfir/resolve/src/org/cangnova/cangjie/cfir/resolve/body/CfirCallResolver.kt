package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirSessionHolder
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.resolve.calls.overloads.CfirCallConflictResolver
import org.cangnova.cangjie.cfir.resolve.calls.stages.CfirResolutionContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.name.Name

/**
 * 璋冪敤瑙ｆ瀽鍣ㄣ€? *
 * Phase 3 鏀寔瀹屾暣鐨勪笁闃舵璋冪敤瑙ｆ瀽锛? * 1. Tower 閬嶅巻鏀堕泦鍊欓€夛紙閫氳繃 towerResolver.runResolver锛? * 2. 鍙栨渶浣冲€欓€夐泦锛堥€氳繃 collector.bestCandidates锛? * 3. 閲嶈浇娑堟锛堥€氳繃 conflictResolver.chooseMaximallySpecificCandidates锛? *
 * 鍚屾椂淇濈暀鏃х増 resolveCall 鏂规硶鍚戝悗鍏煎銆? *
 * 鍙傝€?K2 FirCallResolver(components, towerResolver)銆? */
class CfirCallResolver(
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val towerResolver: CfirTowerResolver =
        CfirTowerResolver(components, components.resolutionStageRunner),
) : CfirSessionHolder {

    override val session: CfirSession get() = components.session

    /** 娉ㄥ叆鐨勫啿绐佽В鏋愬櫒 鈥?鐢?BodyResolveTransformerComponents 鎻愪緵 */
    var conflictResolver: CfirCallConflictResolver? = null

    /**
     * Phase 3 瀹屾暣璋冪敤瑙ｆ瀽娴佺▼銆?     *
     * @param callInfo 璋冪敤淇℃伅
     * @param context 瑙ｆ瀽涓婁笅鏂?     * @return 璋冪敤瑙ｆ瀽缁撴灉
     */
    fun resolveCallAndSelectCandidate(
        callInfo: CfirCallInfo,
        context: CfirResolutionContext,
    ): CfirCallResolutionResult {
        // 1. Tower 閬嶅巻鏀堕泦鍊欓€?
        towerResolver.runResolver(callInfo, context)

        // 2. 鍙栨渶浣冲€欓€夐泦
        val bestCandidates = towerResolver.collector.bestCandidates()
        if (bestCandidates.isEmpty()) {
            return CfirCallResolutionResult.NoCandidate
        }

        // 杩囨护鍑烘垚鍔熷€欓€?
        val successCandidates = bestCandidates.filter { it.isSuccessful }

        if (successCandidates.isEmpty()) {
            // 鎵€鏈夊€欓€夐兘澶辫触 鈥?杩斿洖鏈€浣崇殑澶辫触鍊欓€夛紙鐢ㄤ簬閿欒鎶ュ憡锛?
            return if (bestCandidates.size == 1) {
                val candidate = bestCandidates.single()
                CfirCallResolutionResult.ResolvedWithErrors(candidate)
            } else {
                CfirCallResolutionResult.NoCandidate
            }
        }

        if (successCandidates.size == 1) {
            return CfirCallResolutionResult.Success(successCandidates.single())
        }

        // 3. 閲嶈浇娑堟
        val resolver = conflictResolver
        if (resolver != null) {
            val disambiguated = resolver.chooseMaximallySpecificCandidates(successCandidates)
            return when (disambiguated.size) {
                0 -> CfirCallResolutionResult.NoCandidate
                1 -> CfirCallResolutionResult.Success(disambiguated.single())
                else -> CfirCallResolutionResult.Ambiguity(disambiguated.toList())
            }
        }

        // 鏃犳秷姝у櫒 鈥?鐩存帴杩斿洖姝т箟
        return CfirCallResolutionResult.Ambiguity(successCandidates)
    }

    // ---- 鏃х増 API锛堝悜鍚庡吋瀹癸級 ----

    /**
     * 鏃х増璋冪敤瑙ｆ瀽锛圥hase 2 鍏煎锛夈€?     *
     * @param name 鍑芥暟鍚?     * @param arguments 瀹炲弬鍒楄〃锛堜笉鍋氱被鍨嬪尮閰嶏級
     */
    fun resolveCall(
        name: Name,
        arguments: List<CfirExpression>,
    ): CfirCallResolutionResult {
        val candidates = towerResolver.findFunctions(name)

        return when {
            candidates.isEmpty() -> CfirCallResolutionResult.NoCandidate
            candidates.size == 1 -> {
                val symbol = candidates.single()
                val returnType = extractReturnType(symbol)
                CfirCallResolutionResult.LegacySuccess(symbol, returnType)
            }
            else -> CfirCallResolutionResult.LegacyAmbiguity(candidates)
        }
    }

    /** 浠庡嚱鏁扮鍙蜂腑鎻愬彇杩斿洖绫诲瀷 */
    private fun extractReturnType(symbol: CfirFunctionSymbol): ConeCangjieType {
        if (!symbol.isBound) return ConeErrorType("unbound function symbol")
        val function = symbol.cfir
        val typeRef = function.returnTypeRef
        return if (typeRef is CfirResolvedTypeRef) {
            typeRef.coneType
        } else {
            ConeErrorType("unresolved return type")
        }
    }
}

/** 璋冪敤瑙ｆ瀽缁撴灉 */
sealed class CfirCallResolutionResult {

    /** Phase 3 瑙ｆ瀽鎴愬姛锛氬崟涓€鍊欓€夛紙鎼哄甫瀹屾暣 CfirCandidate锛?*/
    class Success(
        val candidate: CfirCandidate,
    ) : CfirCallResolutionResult()

    /** 瑙ｆ瀽鎴愬姛浣嗘湁閿欒锛堝敮涓€鍊欓€変絾楠岃瘉鏈畬鍏ㄩ€氳繃锛?*/
    class ResolvedWithErrors(
        val candidate: CfirCandidate,
    ) : CfirCallResolutionResult()

    /** 澶氬€欓€夋涔夛紙Phase 3锛?*/
    class Ambiguity(
        val candidates: List<CfirCandidate>,
    ) : CfirCallResolutionResult()

    /** 鏃犲€欓€?*/
    data object NoCandidate : CfirCallResolutionResult()

    // ---- 鏃х増缁撴灉绫诲瀷锛堝悜鍚庡吋瀹癸級 ----

    /** 鏃х増鍗曞€欓€夋垚鍔?*/
    class LegacySuccess(
        val symbol: CfirFunctionSymbol,
        val returnType: ConeCangjieType,
    ) : CfirCallResolutionResult()

    /** 鏃х増澶氬€欓€夋涔?*/
    class LegacyAmbiguity(
        val candidates: List<CfirFunctionSymbol>,
    ) : CfirCallResolutionResult()
}

