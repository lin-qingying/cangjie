package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.toPrimitiveTypeKindOrNull
import org.cangnova.cangjie.name.Name

/**
 * Use-site member scope for class-like receivers. Declared members win over
 * extend members for classifier/property hiding, while functions are merged.
 */
class CfirClassUseSiteMemberScope(
    private val classSymbol: CfirClassLikeSymbol<*>,
    private val symbolProvider: CfirSymbolProvider,
    private val extendProvider: CfirExtendProvider? = null,
) : CfirTypeScope() {

    private val declaredScope = CfirClassDeclaredMemberScope(classSymbol)
    private val extendScope = extendProvider?.let { CfirExtendMemberScope(classSymbol.classId, it) }
    private val parentScopes: List<CfirClassDeclaredMemberScope> by lazy { buildParentScopes() }

    override fun getCallableNames(): Set<Name> = buildSet {
        for (declaration in classSymbol.cfir.declarations) {
            when (declaration) {
                is CfirFunction -> declaration.callableNameOrNull()?.let(::add)
                is org.cangnova.cangjie.cfir.declarations.CfirProperty -> add(declaration.name)
                is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor -> add(declaration.name)
                else -> Unit
            }
        }
        addAll(extendCallableNames())
    }

    override fun getClassifierNames(): Set<Name> = buildSet {
        for (declaration in classSymbol.cfir.declarations) {
            if (declaration is CfirClassLikeDeclaration) add((declaration.symbol as CfirClassLikeSymbol<*>).name)
        }
        addAll(extendClassifierNames())
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirFunctionSymbol<*>,
        processor: (CfirFunctionSymbol<*>, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        for (parent in parentScopes) {
            val candidates = mutableListOf<CfirFunctionSymbol<*>>()
            parent.processFunctionsByName(functionSymbol.name) { candidates += it }
            for (candidate in candidates) {
                if (processor(candidate, this) == ProcessorAction.STOP) {
                    return ProcessorAction.STOP
                }
            }
        }
        return ProcessorAction.NEXT
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        for (parent in parentScopes) {
            val candidates = mutableListOf<CfirPropertySymbol>()
            parent.processPropertiesByName(propertySymbol.name) { candidates += it }
            for (candidate in candidates) {
                if (processor(candidate, this) == ProcessorAction.STOP) {
                    return ProcessorAction.STOP
                }
            }
        }
        return ProcessorAction.NEXT
    }

    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession,
    ): CfirTypeScope? = null

    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        val local = mutableListOf<CfirClassLikeSymbol<*>>()
        declaredScope.processClassifiersByName(name) { local += it }
        if (local.isEmpty()) {
            extendScope?.processClassifiersByName(name) { local += it }
        }
        if (local.isNotEmpty()) {
            local.forEach(processor)
            return
        }
        for (parent in parentScopes) {
            parent.processClassifiersByName(name, processor)
        }
    }

    override fun processFunctionsByName(name: Name, processor: (CfirFunctionSymbol<*>) -> Unit) {
        declaredScope.processFunctionsByName(name, processor)
        extendScope?.processFunctionsByName(name, processor)
        for (parent in parentScopes) {
            parent.processFunctionsByName(name, processor)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        val local = mutableListOf<CfirPropertySymbol>()
        declaredScope.processPropertiesByName(name) { local += it }
        if (local.isEmpty()) {
            extendScope?.processPropertiesByName(name) { local += it }
        }
        if (local.isNotEmpty()) {
            local.forEach(processor)
            return
        }
        for (parent in parentScopes) {
            parent.processPropertiesByName(name, processor)
        }
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        declaredScope.processCallablesByName(name, processor)
        extendScope?.processCallablesByName(name, processor)
        for (parent in parentScopes) {
            parent.processCallablesByName(name, processor)
        }
    }

    private fun buildParentScopes(): List<CfirClassDeclaredMemberScope> =
        buildParentScopesRecursive(classSymbol, 0)

    private fun extendCallableNames(): Set<Name> = buildSet {
        for (extend in extendsForCurrentClass()) {
            for (declaration in extend.declarations) {
                when (declaration) {
                    is CfirFunction -> declaration.callableNameOrNull()?.let(::add)
                    is org.cangnova.cangjie.cfir.declarations.CfirProperty -> add(declaration.name)
                    is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor -> add(declaration.name)
                    else -> Unit
                }
            }
        }
    }

    private fun extendClassifierNames(): Set<Name> = buildSet {
        for (extend in extendsForCurrentClass()) {
            for (declaration in extend.declarations) {
                if (declaration is CfirClassLikeDeclaration) {
                    add((declaration.symbol as CfirClassLikeSymbol<*>).name)
                }
            }
        }
    }

    private fun extendsForCurrentClass() = buildList {
        val provider = extendProvider ?: return@buildList
        addAll(provider.getExtendsForClass(classSymbol.classId))
        classSymbol.classId.toPrimitiveTypeKindOrNull()?.let { kind ->
            addAll(provider.getExtendsForBuiltinType(kind))
        }
    }

    private fun buildParentScopesRecursive(
        symbol: CfirClassLikeSymbol<*>,
        depth: Int,
    ): List<CfirClassDeclaredMemberScope> {
        if (depth >= MAX_DEPTH) return emptyList()
        val declaration = symbol.cfir
        val result = mutableListOf<CfirClassDeclaredMemberScope>()

        for (superTypeRef in declaration.superTypeRefs) {
            val resolvedRef = superTypeRef as? CfirResolvedTypeRef ?: continue
            val classId = resolvedRef.coneType.classIdOrPrimitiveClassId ?: continue
            val parentSymbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: continue
            result += CfirClassDeclaredMemberScope(parentSymbol)
            result += buildParentScopesRecursive(parentSymbol, depth + 1)
        }
        return result
    }

    companion object {
        private const val MAX_DEPTH = 32
    }
}
