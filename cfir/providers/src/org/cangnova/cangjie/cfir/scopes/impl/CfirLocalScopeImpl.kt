package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.scopes.CfirLocalScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.Name

/**
 * 局部作用域。
 *
 * 这里保存函数体、代码块等局部区域里逐步引入的符号。
 * `classifier` 入口保留给解析流程内部使用，但当前仓颉实现不会把局部 class-like
 * 当作公开语义能力向 Analysis / Provider 层暴露。
 */
class CfirLocalScopeImpl : CfirLocalScope() {

    private val variables = HashMap<Name, MutableList<CfirCallableSymbol<*>>>()
    private val functions = HashMap<Name, MutableList<CfirFunctionSymbol<*>>>()
    private val classifiers = HashMap<Name, MutableList<CfirClassSymbol>>()

    /** 添加局部变量。支持字段变量和模式变量。 */
    fun addVariable(name: Name, symbol: CfirCallableSymbol<*>) {
        variables.getOrPut(name) { mutableListOf() }.add(symbol)
    }

    /** 添加局部函数。 */
    fun addFunction(name: Name, symbol: CfirFunctionSymbol<*>) {
        functions.getOrPut(name) { mutableListOf() }.add(symbol)
    }

    /** 添加局部 classifier 符号。 */
    fun addClassifier(name: Name, symbol: CfirClassSymbol) {
        classifiers.getOrPut(name) { mutableListOf() }.add(symbol)
    }

    fun withVariable(name: Name, symbol: CfirCallableSymbol<*>): CfirLocalScopeImpl =
        snapshot().also { it.addVariable(name, symbol) }

    fun withFunction(name: Name, symbol: CfirFunctionSymbol<*>): CfirLocalScopeImpl =
        snapshot().also { it.addFunction(name, symbol) }

    fun snapshot(): CfirLocalScopeImpl {
        val copy = CfirLocalScopeImpl()
        variables.forEach { (name, symbols) ->
            symbols.forEach { copy.addVariable(name, it) }
        }
        functions.forEach { (name, symbols) ->
            symbols.forEach { copy.addFunction(name, it) }
        }
        classifiers.forEach { (name, symbols) ->
            symbols.forEach { copy.addClassifier(name, it) }
        }
        return copy
    }

    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        classifiers[name]?.forEach(processor)
    }

    override fun processFunctionsByName(name: Name, processor: (CfirFunctionSymbol<*>) -> Unit) {
        functions[name]?.forEach(processor)
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        // 局部变量通过 processVariablesByName 查询；
        // CfirPropertySymbol 只用于类成员属性。
    }

    /** 按名称查询局部变量。 */
    fun processVariablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        variables[name]?.forEach(processor)
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        variables[name]?.forEach(processor)
        functions[name]?.forEach(processor)
    }
}
