

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjFakeSourceElementKind.*
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbolOfType
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.cfir.declarations.isLazyResolvable
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement

/**
 * 基于 CFIR source 与 PSI 结构计算符号所属 class-like 符号的工具。
 */
internal object LLContainingClassCalculator {
    /**
     * 返回 [symbol] 的外围 class-like 符号。
     *
     * 该方法只使用 source 信息和 CFIR 节点内部信息，不依赖额外索引。
     */
    fun getContainingClassSymbol(symbol: CfirBasedSymbol<*>): CfirClassLikeSymbol<*>? {
        if (!symbol.origin.isLazyResolvable) {
            // Handle only source or source-based declarations for now as below we use the PSI tree
            return null
        }

        if (!canHaveContainingClassSymbol(symbol)) {
            return null
        }

        // 仓颉没有局部名义类型；源码 owner 按 PSI 身份定位，不能让同 ClassId 的首个符号抢先命中。
        val source = symbol.cfir.source as? CjPsiSourceElement ?: return null
        when (val kind = source.kind) {
            is CjFakeSourceElementKind -> {
                if (symbol is CfirPropertyAccessorSymbol && kind == DefaultAccessor) {
                    val propertyOrParameter = source.psi as? CjDeclaration
                    return computeContainingClass(symbol, propertyOrParameter?.containingTypeStatement)
                }

                if (symbol is CfirPropertyAccessorSymbol && kind == PropertyFromParameter) {
                    val containingParameter = source.psi as? CjParameter
                    return computeContainingClass(symbol, containingParameter?.containingTypeStatement)
                }

                if (symbol is CfirConstructorSymbol && kind == ImplicitConstructor) {
                    return computeContainingClass(symbol, source.psi as? CjTypeStatement)
                }

                if (symbol is CfirPropertySymbol && kind == PropertyFromParameter) {
                    val containingParameter = source.psi as? CjParameter
                    return computeContainingClass(symbol, containingParameter?.containingTypeStatement)
                }
            }
            else -> if (symbol is CfirCallableSymbol<*>) {
                return when (val selfCallable = source.psi) {
                    is CjCallableDeclaration -> computeContainingClass(symbol, selfCallable.containingTypeStatement)
                    is CjEnumConstructor -> computeContainingClass(symbol, selfCallable.parentEnum)
                    is CjPropertyAccessor -> computeContainingClass(symbol, selfCallable.property.containingTypeStatement)
                    else -> null
                }
            }
        }

        return null
    }

    /**
     * 判断 [symbol] 这种符号类别是否可能拥有外围 class-like。
     */
    private fun canHaveContainingClassSymbol(symbol: CfirBasedSymbol<*>): Boolean = when (symbol) {
        is CfirValueParameterSymbol, is CfirAnonymousFunctionSymbol -> false
        is CfirPropertySymbol -> true
        is CfirNamedFunctionSymbol -> symbol.rawStatus.visibility != Visibilities.Local
        is CfirCallableSymbol -> true
        else -> false
    }

    /**
     * 根据 [psi] 所属模块解析外围 class-like 符号。
     */
    private fun computeContainingClass(symbol: CfirBasedSymbol<*>, psi: CjTypeStatement?): CfirClassLikeSymbol<*>? {
        if (psi == null || psi is CjExtend) {
            return null
        }

        val module = symbol.llCfirModuleData.caModule
        val resolutionFacade = module.getResolutionFacade(module.project)
        return psi.resolveToCfirSymbolOfType<CfirClassLikeSymbol<*>>(resolutionFacade)
    }
}
