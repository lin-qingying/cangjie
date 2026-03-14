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
 * 调用解析器。
 *
 * Phase 3 支持完整的三阶段调用解析：
 * 1. Tower 遍历收集候选（通过 towerResolver.runResolver）
 * 2. 取最佳候选集（通过 collector.bestCandidates）
 * 3. 重载消歧（通过 conflictResolver.chooseMaximallySpecificCandidates）
 *
 * 同时保留旧版 resolveCall 方法向后兼容。
 *
 * 参考 K2 FirCallResolver(components, towerResolver)。
 */
class CfirCallResolver(
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val towerResolver: CfirTowerResolver =
        CfirTowerResolver(components, components.resolutionStageRunner),
) : CfirSessionHolder {

    override val session: CfirSession get() = components.session

    /** 注入的冲突解析器 — 由 BodyResolveTransformerComponents 提供 */
    var conflictResolver: CfirCallConflictResolver? = null

    /**
     * Phase 3 完整调用解析流程。
     *
     * @param callInfo 调用信息
     * @param context 解析上下文
     * @return 调用解析结果
     */
    fun resolveCallAndSelectCandidate(
        callInfo: CfirCallInfo,
        context: CfirResolutionContext,
    ): CfirCallResolutionResult {
        // 1. Tower 遍历收集候选
        towerResolver.runResolver(callInfo, context)

        // 2. 取最佳候选集
        val bestCandidates = towerResolver.collector.bestCandidates()
        if (bestCandidates.isEmpty()) {
            return CfirCallResolutionResult.NoCandidate
        }

        // 过滤出成功候选
        val successCandidates = bestCandidates.filter { it.isSuccessful }

        if (successCandidates.isEmpty()) {
            // 所有候选都失败 — 返回最佳的失败候选（用于错误报告）
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

        // 3. 重载消歧
        val resolver = conflictResolver
        if (resolver != null) {
            val disambiguated = resolver.chooseMaximallySpecificCandidates(successCandidates)
            return when (disambiguated.size) {
                0 -> CfirCallResolutionResult.NoCandidate
                1 -> CfirCallResolutionResult.Success(disambiguated.single())
                else -> CfirCallResolutionResult.Ambiguity(disambiguated.toList())
            }
        }

        // 无消歧器 — 直接返回歧义
        return CfirCallResolutionResult.Ambiguity(successCandidates)
    }

    // ---- 旧版 API（向后兼容） ----

    /**
     * 旧版调用解析（Phase 2 兼容）。
     *
     * @param name 函数名
     * @param arguments 实参列表（不做类型匹配）
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

    /** 从函数符号中提取返回类型 */
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

/** 调用解析结果 */
sealed class CfirCallResolutionResult {

    /** Phase 3 解析成功：单一候选（携带完整 CfirCandidate） */
    class Success(
        val candidate: CfirCandidate,
    ) : CfirCallResolutionResult()

    /** 解析成功但有错误（唯一候选但验证未完全通过） */
    class ResolvedWithErrors(
        val candidate: CfirCandidate,
    ) : CfirCallResolutionResult()

    /** 多候选歧义（Phase 3） */
    class Ambiguity(
        val candidates: List<CfirCandidate>,
    ) : CfirCallResolutionResult()

    /** 无候选 */
    data object NoCandidate : CfirCallResolutionResult()

    // ---- 旧版结果类型（向后兼容） ----

    /** 旧版单候选成功 */
    class LegacySuccess(
        val symbol: CfirFunctionSymbol,
        val returnType: ConeCangjieType,
    ) : CfirCallResolutionResult()

    /** 旧版多候选歧义 */
    class LegacyAmbiguity(
        val candidates: List<CfirFunctionSymbol>,
    ) : CfirCallResolutionResult()
}
