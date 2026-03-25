package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.Name

/**
 * Mirrors Kotlin FIR's class static scope slot for tower construction.
 *
 * Cangjie has no companion object, so this scope contains:
 * - nested classifiers
 * - enum constructors
 * - static functions
 * - static properties
 */
class CfirClassStaticScope(
    owner: CfirClassLikeDeclaration,
) : CfirContainingNamesAwareScope() {
    private val memberIndex: MemberIndex by lazy(LazyThreadSafetyMode.PUBLICATION) { buildIndex(owner.declarations) }

    override val hasDefinitelyNoStaticMembers: Boolean
        get() = memberIndex.isEmpty

    override fun getCallableNames(): Set<Name> = memberIndex.callableNames

    override fun getClassifierNames(): Set<Name> = memberIndex.classifierNames

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
    }

    override fun withReplacedSessionOrNull(
        newSession: org.cangnova.cangjie.cfir.session.CfirSession,
        newScopeSession: org.cangnova.cangjie.cfir.ScopeSession,
    ): CfirContainingNamesAwareScope = this

    private data class MemberIndex(
        val classifiers: Map<Name, List<CfirClassSymbol>>,
        val enumConstructors: Map<Name, List<CfirEnumConstructorSymbol>>,
        val functions: Map<Name, List<CfirFunctionSymbol<*>>>,
        val properties: Map<Name, List<CfirPropertySymbol>>,
    ) {
        val callableNames: Set<Name> = buildSet {
            addAll(enumConstructors.keys)
            addAll(functions.keys)
            addAll(properties.keys)
        }

        val classifierNames: Set<Name> = classifiers.keys

        val isEmpty: Boolean
            get() = classifiers.isEmpty() && enumConstructors.isEmpty() && functions.isEmpty() && properties.isEmpty()
    }

    private fun buildIndex(declarations: List<CfirDeclaration>): MemberIndex {
        val classifiers = linkedMapOf<Name, MutableList<CfirClassSymbol>>()
        val enumConstructors = linkedMapOf<Name, MutableList<CfirEnumConstructorSymbol>>()
        val functions = linkedMapOf<Name, MutableList<CfirFunctionSymbol<*>>>()
        val properties = linkedMapOf<Name, MutableList<CfirPropertySymbol>>()

        for (declaration in declarations) {
            when (declaration) {
                is CfirClass -> {
                    val symbol = declaration.symbol as? CfirClassSymbol ?: continue
                    classifiers.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
                }

                is CfirEnumConstructor -> {
                    val symbol = declaration.symbol as? CfirEnumConstructorSymbol ?: continue
                    enumConstructors.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
                }

                is CfirFunction -> {
                    if (!declaration.status.isStatic) continue
                    val symbol = declaration.symbol as? CfirFunctionSymbol<*> ?: continue
                    val callableName = declaration.callableNameOrNull() ?: continue
                    functions.getOrPut(callableName) { mutableListOf() }.add(symbol)
                }

                is CfirProperty -> {
                    if (!declaration.status.isStatic) continue
                    val symbol = declaration.symbol as? CfirPropertySymbol ?: continue
                    properties.getOrPut(declaration.name) { mutableListOf() }.add(symbol)
                }

                else -> continue
            }
        }

        return MemberIndex(classifiers, enumConstructors, functions, properties)
    }
}
