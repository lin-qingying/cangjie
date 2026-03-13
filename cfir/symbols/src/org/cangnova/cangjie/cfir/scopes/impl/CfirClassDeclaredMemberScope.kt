package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.scopes.CfirClassScope
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.Name

/**
 * 类成员声明 scope。
 *
 * 遍历 [classSymbol] 绑定的类声明中的 [CfirClass.declarations]，
 * 按名称索引函数、属性和嵌套类。使用懒初始化缓存。
 *
 * 参考 K2 FirClassDeclaredMemberScope。
 */
class CfirClassDeclaredMemberScope(
    private val classSymbol: CfirClassSymbol,
) : CfirClassScope {

    private val memberIndex: MemberIndex by lazy { buildIndex(classSymbol.fir.declarations) }

    override fun processClassifiersByName(name: Name, processor: (CfirClassSymbol) -> Unit) {
        memberIndex.classifiers[name]?.forEach(processor)
    }

    override fun processFunctionsByName(name: Name, processor: (CfirFunctionSymbol) -> Unit) {
        memberIndex.functions[name]?.forEach(processor)
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        memberIndex.properties[name]?.forEach(processor)
    }

    private class MemberIndex(
        val classifiers: Map<Name, List<CfirClassSymbol>>,
        val functions: Map<Name, List<CfirFunctionSymbol>>,
        val properties: Map<Name, List<CfirPropertySymbol>>,
    )

    private fun buildIndex(declarations: List<CfirDeclaration>): MemberIndex {
        val classifiers = HashMap<Name, MutableList<CfirClassSymbol>>()
        val functions = HashMap<Name, MutableList<CfirFunctionSymbol>>()
        val properties = HashMap<Name, MutableList<CfirPropertySymbol>>()

        for (decl in declarations) {
            when (decl) {
                is CfirClass -> {
                    val sym = decl.symbol as? CfirClassSymbol ?: continue
                    classifiers.getOrPut(decl.name) { mutableListOf() }.add(sym)
                }
                is CfirFunction -> {
                    val sym = decl.symbol as? CfirFunctionSymbol ?: continue
                    functions.getOrPut(decl.name) { mutableListOf() }.add(sym)
                }
                is CfirProperty -> {
                    val sym = decl.symbol as? CfirPropertySymbol ?: continue
                    properties.getOrPut(decl.name) { mutableListOf() }.add(sym)
                }
                else -> { /* 忽略其他声明类型 */ }
            }
        }
        return MemberIndex(classifiers, functions, properties)
    }
}
