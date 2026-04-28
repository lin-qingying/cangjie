package org.cangnova.cangjie.analysis.api.cfir.scopes

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.scopes.CfirClassScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.Name

/**
 * 单个 extend 声明的 declared-member CFIR 作用域。
 *
 * Analysis API 的 ScopeProvider 只负责把底层 CFIR scope 映射为公开 scope；
 * extend 声明自身的成员索引必须停留在 CFIR scope 层，保持与 Kotlin ScopeProvider 的职责边界一致。
 */
internal class CfirSingleExtendDeclaredMemberScope(
    private val extend: CfirExtend,
) : CfirClassScope() {
    private val classifiers = linkedMapOf<Name, MutableList<CfirClassLikeSymbol<*>>>()
    private val functions = linkedMapOf<Name, MutableList<CfirNamedFunctionSymbol>>()
    private val properties = linkedMapOf<Name, MutableList<CfirPropertySymbol>>()
    private val variables = linkedMapOf<Name, MutableList<CfirVariableSymbol<*>>>()

    init {
        for (declaration in extend.declarations) {
            when (declaration) {
                is CfirClassLikeDeclaration -> {
                    val symbol = declaration.symbol as? CfirClassLikeSymbol<*> ?: continue
                    classifiers.getOrPut(symbol.name) { mutableListOf() }.add(symbol)
                }

                is CfirNamedFunction -> {
                    val symbol = declaration.symbol ?: continue
                    val callableName = declaration.callableNameOrNull() ?: continue
                    functions.getOrPut(callableName) { mutableListOf() }.add(symbol)
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
    }

    override fun getCallableNames(): Set<Name> = functions.keys + properties.keys + variables.keys

    override fun getClassifierNames(): Set<Name> = classifiers.keys

    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        classifiers[name]?.forEach(processor)
    }

    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        functions[name]?.forEach(processor)
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        properties[name]?.forEach(processor)
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        functions[name]?.forEach(processor)
        properties[name]?.forEach(processor)
        variables[name]?.forEach(processor)
    }
}
