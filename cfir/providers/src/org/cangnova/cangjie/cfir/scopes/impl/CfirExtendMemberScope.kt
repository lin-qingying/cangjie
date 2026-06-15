package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.scopes.CfirExtendScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
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
    private val session: CfirSession,
    private val receiverType: ConeCangJieType,
) : CfirExtendScope() {

    private val memberIndex: MemberIndex by lazy { buildIndex() }

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
        memberIndex.functions[name]?.forEach(processor)
        memberIndex.properties[name]?.forEach(processor)
        memberIndex.variables[name]?.forEach(processor)
    }

    private class MemberIndex(
        val classifiers: Map<Name, List<CfirClassLikeSymbol<*>>>,
        val functions: Map<Name, List<CfirNamedFunctionSymbol>>,
        val properties: Map<Name, List<CfirPropertySymbol>>,
        val variables: Map<Name, List<CfirVariableSymbol<*>>>,
    )

    private fun buildIndex(): MemberIndex {
        val classifiers = HashMap<Name, MutableList<CfirClassLikeSymbol<*>>>()
        val functions = HashMap<Name, MutableList<CfirNamedFunctionSymbol>>()
        val properties = HashMap<Name, MutableList<CfirPropertySymbol>>()
        val variables = HashMap<Name, MutableList<CfirVariableSymbol<*>>>()

        val extends = buildList {
            addAll(extendProvider.getExtendsForClass(targetClassId))
            targetClassId.toPrimitiveTypeKindOrNull()?.let { kind ->
                addAll(extendProvider.getExtendsForBuiltinType(kind))
            }
        }
        for (extend in extends) {
            if (!extend.isApplicableAtReceiver()) continue
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

    /**
     * extend member scope 必须和 use-site substitution/receiver 检查共享同一适用性规则。
     *
     * 只按目标 ClassId 建索引会把不满足泛型约束的 extend 成员暴露给调用解析；
     * 官方成员访问流程会在候选阶段删除这些目标。
     */
    private fun CfirExtend.isApplicableAtReceiver(): Boolean {
        if (!extendProvider.isExtendAccessible(this)) return false
        val targetPattern = extendedTypeRef.coneTypeOrNull ?: return false
        return createExtendDeclarationSubstitution(
            session = session,
            extend = this,
            targetPattern = targetPattern,
            concreteReceiverType = receiverType,
        ) != null
    }

    private fun indexDeclaration(
        declaration: CfirDeclaration,
        classifiers: HashMap<Name, MutableList<CfirClassLikeSymbol<*>>>,
        functions: HashMap<Name, MutableList<CfirNamedFunctionSymbol>>,
        properties: HashMap<Name, MutableList<CfirPropertySymbol>>,
        variables: HashMap<Name, MutableList<CfirVariableSymbol<*>>>,
    ) {
        when (declaration) {
            is CfirClassLikeDeclaration -> {
                val symbol = declaration.symbol as? CfirClassLikeSymbol<*> ?: return
                classifiers.getOrPut(symbol.name) { mutableListOf() }.add(symbol)
            }

            is CfirNamedFunction -> {
                val symbol = declaration.symbol ?: return
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
