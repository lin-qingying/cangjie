package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.scopes.CfirLocalScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.Name

/**
 * 局部变量 scope。
 *
 * 可变 scope（与其他只读 scope 不同），支持动态添加局部声明。
 * 用于函数体解析时逐语句构建局部符号表。
 *
 * 参考 K2 FirLocalScope。
 */
class CfirLocalScopeImpl : CfirLocalScope() {

    private val variables = HashMap<Name, MutableList<CfirCallableSymbol<*>>>()
    private val functions = HashMap<Name, MutableList<CfirFunctionSymbol>>()
    private val classifiers = HashMap<Name, MutableList<CfirClassSymbol>>()

    /** 添加局部变量（支持 CfirFieldVariableSymbol 和 CfirPatternVariableSymbol） */
    fun addVariable(name: Name, symbol: CfirCallableSymbol<*>) {
        variables.getOrPut(name) { mutableListOf() }.add(symbol)
    }

    /** 添加局部函数 */
    fun addFunction(name: Name, symbol: CfirFunctionSymbol) {
        functions.getOrPut(name) { mutableListOf() }.add(symbol)
    }

    /** 添加局部类 */
    fun addClassifier(name: Name, symbol: CfirClassSymbol) {
        classifiers.getOrPut(name) { mutableListOf() }.add(symbol)
    }

    override fun processClassifiersByName(name: Name, processor: (CfirClassSymbol) -> Unit) {
        classifiers[name]?.forEach(processor)
    }

    override fun processFunctionsByName(name: Name, processor: (CfirFunctionSymbol) -> Unit) {
        functions[name]?.forEach(processor)
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        // 局部变量通过 processVariablesByName 查找
        // CfirPropertySymbol 用于类成员属性，局部变量使用 CfirFieldVariableSymbol
    }

    /** 按名称查找局部变量符号 */
    fun processVariablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        variables[name]?.forEach(processor)
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        variables[name]?.forEach(processor)
        functions[name]?.forEach(processor)
    }
}
