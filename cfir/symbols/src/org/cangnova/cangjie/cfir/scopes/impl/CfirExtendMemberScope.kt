package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.scopes.CfirExtendScope
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * extend 成员注入 scope。
 *
 * 通过 [CfirExtendProvider] 获取目标类的所有 extend 声明，
 * 将其 declarations 暴露为 scope 内的符号。
 *
 * 仓颉的 extend 机制允许在类定义之外为其添加新的成员和接口实现，
 * 此 scope 使得在成员查找时能包含 extend 引入的声明。
 *
 * 参考 K2 FirExtensionFunctionMemberScope。
 */
class CfirExtendMemberScope(
    private val targetClassId: ClassId,
    private val extendProvider: CfirExtendProvider,
) : CfirExtendScope {

    private val memberIndex: MemberIndex by lazy { buildIndex() }

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

    private fun buildIndex(): MemberIndex {
        val classifiers = HashMap<Name, MutableList<CfirClassSymbol>>()
        val functions = HashMap<Name, MutableList<CfirFunctionSymbol>>()
        val properties = HashMap<Name, MutableList<CfirPropertySymbol>>()

        val extends = extendProvider.getExtendsForClass(targetClassId)
        for (extend in extends) {
            for (decl in extend.declarations) {
                indexDeclaration(decl, classifiers, functions, properties)
            }
        }
        return MemberIndex(classifiers, functions, properties)
    }

    private fun indexDeclaration(
        decl: CfirDeclaration,
        classifiers: HashMap<Name, MutableList<CfirClassSymbol>>,
        functions: HashMap<Name, MutableList<CfirFunctionSymbol>>,
        properties: HashMap<Name, MutableList<CfirPropertySymbol>>,
    ) {
        when (decl) {
            is CfirClass -> {
                val sym = decl.symbol as? CfirClassSymbol ?: return
                classifiers.getOrPut(decl.name) { mutableListOf() }.add(sym)
            }
            is CfirFunction -> {
                val sym = decl.symbol as? CfirFunctionSymbol ?: return
                functions.getOrPut(decl.name) { mutableListOf() }.add(sym)
            }
            is CfirProperty -> {
                val sym = decl.symbol as? CfirPropertySymbol ?: return
                properties.getOrPut(decl.name) { mutableListOf() }.add(sym)
            }
            else -> { /* 忽略其他声明类型 */ }
        }
    }
}
