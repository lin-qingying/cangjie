package org.cangnova.cangjie.chir.cfir2chir

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilySnapshot
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import java.util.IdentityHashMap

/**
 * 为跨包 actualization 提供稳定的声明身份。
 *
 * CJMP 驱动可将 CFIR family matcher 的 counterpart 与其 CJO 来源映射到同一身份；普通源码声明由
 * 转换器按现有 CFIR 语义 ID 规则标识。该边界不依赖 CJO 存储或官方 CHIR wire 类型。
 */
fun interface Cfir2ChirSemanticIdentityProvider {
    /** 返回声明的跨包身份；当前编译图中没有外部身份时返回 null。 */
    fun identityOf(symbol: CfirBasedSymbol<*>): ChirSemanticId?

    /**
     * 返回转换器实际用于该声明的身份：先取跨包稳定身份，否则沿用 CFIR 声明身份。
     *
     * 调用方需要在转换器外构造引用计划时也必须复用此规则，避免计划中的 owner 身份与 CHIR 图分叉。
     */
    fun semanticIdentityOf(symbol: CfirBasedSymbol<*>): ChirSemanticId = identityOf(symbol) ?: when (val declaration = symbol.cfir) {
        is CfirCallableDeclaration -> Cfir2ChirIds.callableId(declaration)
        else -> Cfir2ChirIds.declarationId(symbol)
    }

    companion object {
        /** 类型参数在所属声明中的位置，与 CHIR 转换器使用的身份规则一致。 */
        fun typeParameterIndex(symbol: CfirTypeParameterSymbol): Int = Cfir2ChirIds.typeParameterIndex(symbol)

        /**
         * 将已完成的 CFIR family matcher 关系映射为 CHIR 跨包身份。
         *
         * [identityOfStableBinaryDeclaration] 只能从声明的稳定二进制来源解析身份；未匹配的 CJO
         * 符号也通过该函数按需解析，因此 Specific 函数体可以引用没有 specific 对应声明的 common 函数。
         */
        fun fromCjmpFamilies(
            families: Iterable<CfirCjmpFamilySnapshot>,
            identityOfStableBinaryDeclaration: (CfirBasedSymbol<*>) -> ChirSemanticId?,
        ): Cfir2ChirSemanticIdentityProvider {
            val relations = buildList {
                families.forEach { family ->
                    family.specificDeclarations.forEach { specific ->
                        family.commonCounterpartsFor(specific).forEach { common ->
                            add(specific to common)
                        }
                    }
                }
            }
            return fromCjmpCounterparts(relations, identityOfStableBinaryDeclaration)
        }

        internal fun fromCjmpCounterparts(
            specificToCommon: Iterable<Pair<CfirDeclaration, CfirDeclaration>>,
            identityOfStableBinaryDeclaration: (CfirBasedSymbol<*>) -> ChirSemanticId?,
        ): Cfir2ChirSemanticIdentityProvider {
            val identitiesBySymbol = IdentityHashMap<CfirBasedSymbol<*>, ChirSemanticId>()

            fun bind(symbol: CfirBasedSymbol<*>, semanticId: ChirSemanticId, declaration: CfirDeclaration) {
                val previous = identitiesBySymbol[symbol]
                if (previous != null && previous != semanticId) {
                    throw Cfir2ChirConversionException(
                        "CFIR declaration has conflicting CHIR semantic identities: $previous and $semanticId",
                        declaration,
                    )
                }
                identitiesBySymbol[symbol] = semanticId
            }

            specificToCommon.forEach { (specific, common) ->
                val semanticId = identityOfStableBinaryDeclaration(common.symbol)
                    ?: throw Cfir2ChirConversionException(
                        "matched common declaration has no stable CHIR semantic identity",
                        common,
                    )
                bind(common.symbol, semanticId, common)
                bind(specific.symbol, semanticId, specific)
            }

            return Cfir2ChirSemanticIdentityProvider { symbol ->
                identitiesBySymbol[symbol] ?: identityOfStableBinaryDeclaration(symbol)
            }
        }
    }
}
