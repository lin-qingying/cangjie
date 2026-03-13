package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.CfirSessionHolder
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangjieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.name.Name

/**
 * 调用解析器。
 *
 * 本阶段仅支持**单候选**调用解析（无重载决议）：
 * - 通过 [towerResolver] 在 scope 塔中按名称查找函数符号
 * - 单候选 → 成功
 * - 多候选 → Ambiguity
 * - 零候选 → NoCandidate
 *
 * 后续 Phase 3+ 将引入完整的重载解析和参数类型匹配。
 *
 * 参考 K2 FirCallResolver(components, towerResolver)。
 */
class CfirCallResolver(
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val towerResolver: CfirTowerResolver =
        CfirTowerResolver(components, components.resolutionStageRunner),
) : CfirSessionHolder {

    override val session: CfirSession get() = components.session

    /**
     * 在 scope 塔中查找名称，返回调用解析结果。
     *
     * @param name 函数名
     * @param arguments 实参列表（本阶段仅用于未来扩展，不做类型匹配）
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
                CfirCallResolutionResult.Success(symbol, returnType)
            }
            else -> CfirCallResolutionResult.Ambiguity(candidates)
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

    /** 解析成功：单一候选 */
    class Success(
        val symbol: CfirFunctionSymbol,
        val returnType: ConeCangjieType,
    ) : CfirCallResolutionResult()

    /** 多候选歧义 */
    class Ambiguity(
        val candidates: List<CfirFunctionSymbol>,
    ) : CfirCallResolutionResult()

    /** 无候选 */
    object NoCandidate : CfirCallResolutionResult()
}
