package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.scopes.CfirTypeParameterScope
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.name.Name

/**
 * 类型参数 scope。
 *
 * 在泛型类或泛型函数的作用域中注入类型参数名称。
 * 例如 `class Foo<T>` 内部，`T` 通过此 scope 解析为对应的 [CfirTypeParameterSymbol]。
 *
 * 注意：类型参数不是 classifiers/functions/properties，但为了简化 scope 查找，
 * 通过 processClassifiersByName 查找时如果存在同名的类型参数，也不会冲突，
 * 因为类型参数在仓颉中通过单独的名称解析路径处理。
 * 这里提供独立的 [processTypeParametersByName] 方法用于类型参数查找。
 */
class CfirTypeParameterScopeImpl(
    typeParameters: List<CfirTypeParameter>,
) : CfirTypeParameterScope() {

    private val typeParametersByName: Map<Name, List<CfirTypeParameterSymbol>>

    init {
        val map = HashMap<Name, MutableList<CfirTypeParameterSymbol>>()
        for (tp in typeParameters) {
            val sym = tp.symbol as? CfirTypeParameterSymbol ?: continue
            map.getOrPut(tp.name) { mutableListOf() }.add(sym)
        }
        typeParametersByName = map
    }

    /** 按名称查找类型参数符号 */
    fun processTypeParametersByName(name: Name, processor: (CfirTypeParameterSymbol) -> Unit) {
        typeParametersByName[name]?.forEach(processor)
    }

    // CfirScope 的默认方法保持空实现，因为类型参数不是 classifiers/functions/properties
    override fun processClassifiersByName(name: Name, processor: (CfirClassSymbol) -> Unit) {}
    override fun processFunctionsByName(name: Name, processor: (CfirFunctionSymbol) -> Unit) {}
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {}
}
