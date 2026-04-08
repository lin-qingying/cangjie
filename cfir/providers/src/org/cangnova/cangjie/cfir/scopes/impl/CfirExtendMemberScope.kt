package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.scopes.CfirExtendScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.toPrimitiveTypeKindOrNull
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * Extend-member scope. Primitive targets are indexed through their synthetic
 * builtin ClassId, so builtin and ordinary targets share the same lookup path.
 */
class CfirExtendMemberScope(
    private val targetClassId: ClassId,
    private val extendProvider: CfirExtendProvider,
) : CfirExtendScope() {

    private val memberIndex: MemberIndex by lazy { buildIndex() }

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
        memberIndex.functions[name]?.forEach(processor)
        memberIndex.properties[name]?.forEach(processor)
        memberIndex.variables[name]?.forEach(processor)
    }

    private class MemberIndex(
        val classifiers: Map<Name, List<CfirClassLikeSymbol<*>>>,
        val functions: Map<Name, List<CfirFunctionSymbol<*>>>,
        val properties: Map<Name, List<CfirPropertySymbol>>,
        val variables: Map<Name, List<CfirVariableSymbol<*>>>,
    )

    private fun buildIndex(): MemberIndex {
        val classifiers = HashMap<Name, MutableList<CfirClassLikeSymbol<*>>>()
        val functions = HashMap<Name, MutableList<CfirFunctionSymbol<*>>>()
        val properties = HashMap<Name, MutableList<CfirPropertySymbol>>()
        val variables = HashMap<Name, MutableList<CfirVariableSymbol<*>>>()

        val extends = buildList {
            addAll(extendProvider.getExtendsForClass(targetClassId))
            targetClassId.toPrimitiveTypeKindOrNull()?.let { kind ->
                addAll(extendProvider.getExtendsForBuiltinType(kind))
            }
        }
        for (extend in extends) {
            if (!extendProvider.isExtendAccessible(extend)) continue
            for (declaration in extend.declarations) {
                indexDeclaration(
                    declaration = declaration,
                    classifiers = classifiers,
                    functions = functions,
                    properties = properties,
                    variables = variables,
                )
            }
        }
        return MemberIndex(classifiers, functions, properties, variables)
    }

    private fun indexDeclaration(
        declaration: CfirDeclaration,
        classifiers: HashMap<Name, MutableList<CfirClassLikeSymbol<*>>>,
        functions: HashMap<Name, MutableList<CfirFunctionSymbol<*>>>,
        properties: HashMap<Name, MutableList<CfirPropertySymbol>>,
        variables: HashMap<Name, MutableList<CfirVariableSymbol<*>>>,
    ) {
        when (declaration) {
            is CfirClassLikeDeclaration -> {
                val symbol = declaration.symbol as? CfirClassLikeSymbol<*> ?: return
                classifiers.getOrPut(symbol.name) { mutableListOf() }.add(symbol)
            }

            is CfirFunction -> {
                val symbol = declaration.symbol as? CfirFunctionSymbol<*> ?: return
                val callableName = declaration.callableNameOrNull() ?: return
                functions.getOrPut(callableName) { mutableListOf() }.add(symbol)
            }

            is CfirProperty -> {
                val symbol = declaration.symbol as? CfirPropertySymbol ?: return
                properties.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
            }

            is CfirFieldVariable -> {
                val symbol = declaration.symbol as? CfirVariableSymbol<*> ?: return
                variables.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
            }

            else -> Unit
        }
    }
}
