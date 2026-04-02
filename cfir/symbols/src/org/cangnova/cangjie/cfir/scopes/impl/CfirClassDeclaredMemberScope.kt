package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.scopes.CfirClassScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.Name

/**
 * Declared member scope for class-like symbols, mirroring Kotlin FIR's
 * declared-member scope model.
 */
class CfirClassDeclaredMemberScope(
    private val classSymbol: CfirClassLikeSymbol<*>,
) : CfirClassScope() {

    private val memberIndex: MemberIndex by lazy { buildIndex(classSymbol.cfir.declarations) }

    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        memberIndex.classifiers[name]?.forEach(processor)
    }

    override fun processFunctionsByName(name: Name, processor: (CfirFunctionSymbol<*>) -> Unit) {
        memberIndex.functions[name]?.forEach(processor)
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        memberIndex.properties[name]?.forEach(processor)
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        memberIndex.enumConstructors[name]?.forEach(processor)
        memberIndex.functions[name]?.forEach(processor)
        memberIndex.properties[name]?.forEach(processor)
        memberIndex.variables[name]?.forEach(processor)
    }

    private class MemberIndex(
        val classifiers: Map<Name, List<CfirClassLikeSymbol<*>>>,
        val enumConstructors: Map<Name, List<CfirEnumConstructorSymbol>>,
        val functions: Map<Name, List<CfirFunctionSymbol<*>>>,
        val properties: Map<Name, List<CfirPropertySymbol>>,
        val variables: Map<Name, List<CfirVariableSymbol<*>>>,
    )

    private fun buildIndex(declarations: List<CfirDeclaration>): MemberIndex {
        val classifiers = HashMap<Name, MutableList<CfirClassLikeSymbol<*>>>()
        val enumConstructors = HashMap<Name, MutableList<CfirEnumConstructorSymbol>>()
        val functions = HashMap<Name, MutableList<CfirFunctionSymbol<*>>>()
        val properties = HashMap<Name, MutableList<CfirPropertySymbol>>()
        val variables = HashMap<Name, MutableList<CfirVariableSymbol<*>>>()

        for (declaration in declarations) {
            when (declaration) {
                is CfirClassLikeDeclaration -> {
                    val symbol = declaration.symbol as? CfirClassLikeSymbol<*> ?: continue
                    classifiers.getOrPut(symbol.name) { mutableListOf() }.add(symbol)
                }

                is CfirFunction -> {
                    val symbol = declaration.symbol as? CfirFunctionSymbol<*> ?: continue
                    val callableName = declaration.callableNameOrNull() ?: continue
                    functions.getOrPut(callableName) { mutableListOf() }.add(symbol)
                }

                is CfirEnumConstructor -> {
                    val symbol = declaration.symbol as? CfirEnumConstructorSymbol ?: continue
                    enumConstructors.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
                }

                is CfirProperty -> {
                    val symbol = declaration.symbol as? CfirPropertySymbol ?: continue
                    properties.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
                }

                is CfirFieldVariable -> {
                    val symbol = declaration.symbol as? CfirVariableSymbol<*> ?: continue
                    variables.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
                }

                else -> Unit
            }
        }

        return MemberIndex(classifiers, enumConstructors, functions, properties, variables)
    }

    override fun toString(): String {
        return "Declared member scope of ${classSymbol.classId}"
    }
}
