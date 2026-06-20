package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.scopes.CfirClassScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
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

    override fun getCallableNames(): Set<Name> = memberIndex.callableNames

    override fun getClassifierNames(): Set<Name> = memberIndex.classifierNames

    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        memberIndex.classifiers[name]?.forEach(processor)
    }

    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
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

    override fun processDeclaredConstructors(processor: (CfirConstructorSymbol) -> Unit) {
        memberIndex.constructors.forEach(processor)
    }

    private class MemberIndex(
        val classifiers: Map<Name, List<CfirClassLikeSymbol<*>>>,
        val constructors: List<CfirConstructorSymbol>,
        val enumConstructors: Map<Name, List<CfirEnumConstructorSymbol>>,
        val functions: Map<Name, List<CfirNamedFunctionSymbol>>,
        val properties: Map<Name, List<CfirPropertySymbol>>,
        val variables: Map<Name, List<CfirVariableSymbol<*>>>,
    ) {
        val callableNames: Set<Name> = buildSet {
            addAll(enumConstructors.keys)
            addAll(functions.keys)
            addAll(properties.keys)
            addAll(variables.keys)
        }

        val classifierNames: Set<Name> = classifiers.keys
    }

    private fun buildIndex(declarations: List<CfirDeclaration>): MemberIndex {
        val classifiers = HashMap<Name, MutableList<CfirClassLikeSymbol<*>>>()
        val constructors = mutableListOf<CfirConstructorSymbol>()
        val enumConstructors = HashMap<Name, MutableList<CfirEnumConstructorSymbol>>()
        val functions = HashMap<Name, MutableList<CfirNamedFunctionSymbol>>()
        val properties = HashMap<Name, MutableList<CfirPropertySymbol>>()
        val variables = HashMap<Name, MutableList<CfirVariableSymbol<*>>>()

        for (declaration in declarations) {
            when (declaration) {
                is CfirClassLikeDeclaration -> {
                    val symbol = declaration.symbol as? CfirClassLikeSymbol<*> ?: continue
                    classifiers.getOrPut(symbol.name) { mutableListOf() }.add(symbol)
                }

                is CfirConstructor -> {
                    constructors += declaration.symbol
                }

                is CfirNamedFunction -> {
                    val symbol = declaration.symbol ?: continue
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

        val (visibleProperties, visibleVariables) = filterShadowedValueDeclarations(properties, variables)

        return MemberIndex(
            classifiers = classifiers,
            constructors = constructors,
            enumConstructors = enumConstructors,
            functions = functions,
            properties = visibleProperties,
            variables = visibleVariables,
        )
    }

    /**
     * 仓颉官方 PreCheck 对同名值成员先报告重定义，然后普通引用解析仍以先声明成员为目标。
     *
     * CFIR 仍保留完整 declarations 供冲突检查遍历；这里仅收窄 declared member scope
     * 暴露给普通 lookup 的值成员集合，避免后续 overload resolver 把已重定义成员建模为歧义使用。
     */
    private fun filterShadowedValueDeclarations(
        properties: Map<Name, List<CfirPropertySymbol>>,
        variables: Map<Name, List<CfirVariableSymbol<*>>>,
    ): Pair<Map<Name, List<CfirPropertySymbol>>, Map<Name, List<CfirVariableSymbol<*>>>> {
        val visibleProperties = linkedMapOf<Name, List<CfirPropertySymbol>>()
        val visibleVariables = linkedMapOf<Name, List<CfirVariableSymbol<*>>>()

        for (name in properties.keys + variables.keys) {
            val valueMembers = buildList {
                properties[name].orEmpty().forEachIndexed { index, symbol ->
                    add(IndexedValueMember(symbol, ValueMemberKind.PROPERTY, index))
                }
                variables[name].orEmpty().forEachIndexed { index, symbol ->
                    add(IndexedValueMember(symbol, ValueMemberKind.VARIABLE, index))
                }
            }
            if (valueMembers.isEmpty()) continue

            val selected = valueMembers.minWith(
                compareBy<IndexedValueMember> { it.symbol.declarationStartOffset() }
                    .thenBy { it.kind.ordinal }
                    .thenBy { it.index },
            )
            when (selected.kind) {
                ValueMemberKind.PROPERTY -> visibleProperties[name] = listOf(selected.symbol as CfirPropertySymbol)
                ValueMemberKind.VARIABLE -> visibleVariables[name] = listOf(selected.symbol as CfirVariableSymbol<*>)
            }
        }

        return visibleProperties to visibleVariables
    }

    private data class IndexedValueMember(
        val symbol: CfirCallableSymbol<*>,
        val kind: ValueMemberKind,
        val index: Int,
    )

    private enum class ValueMemberKind {
        PROPERTY,
        VARIABLE,
    }

    private fun CfirCallableSymbol<*>.declarationStartOffset(): Int =
        if (isBound) cfir.source?.startOffset ?: Int.MAX_VALUE else Int.MAX_VALUE

    override fun toString(): String {
        return "Declared member scope of ${classSymbol.classId}"
    }
}
