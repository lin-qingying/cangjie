package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.resolve.providers.CfirDirectSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.toPrimitiveTypeKindOrNull
import org.cangnova.cangjie.name.Name

/**
 * Use-site member scope for class-like receivers. Declared members win over
 * extend members for classifier/property hiding, while functions are merged.
 */
enum class CfirClassMemberScopeKind {
    /**
     * 接收者 use-site scope。
     *
     * 用于调用解析，允许看到 extend 注入成员以及类型感知后的父类型链。
     */
    USE_SITE,

    /**
     * 声明检查 scope。
     *
     * 仅反映源码显式声明的继承关系，不把 extend 注入的父接口/成员视为类本体义务。
     */
    DECLARATION_SITE,
}

class CfirClassUseSiteMemberScope(
    private val session: CfirSession,
    private val classSymbol: CfirClassLikeSymbol<*>,
    private val symbolProvider: CfirSymbolProvider,
    private val extendProvider: CfirExtendProvider? = null,
    private val directSupertypeProvider: CfirDirectSupertypeProvider? = null,
    private val ownerType: ConeCangJieType? = declarationSelfType(classSymbol),
    private val scopeKind: CfirClassMemberScopeKind = CfirClassMemberScopeKind.USE_SITE,
) : CfirTypeScope() {
    constructor(
        classSymbol: CfirClassLikeSymbol<*>,
        symbolProvider: CfirSymbolProvider,
        extendProvider: CfirExtendProvider? = null,
        directSupertypeProvider: CfirDirectSupertypeProvider? = null,
    ) : this(
        session = symbolProvider.session,
        classSymbol = classSymbol,
        symbolProvider = symbolProvider,
        extendProvider = extendProvider,
        directSupertypeProvider = directSupertypeProvider,
        ownerType = declarationSelfType(classSymbol),
        scopeKind = CfirClassMemberScopeKind.USE_SITE,
    )

    private val declaredScope = CfirClassDeclaredMemberScope(classSymbol)
    private val extendScope = takeIf { scopeKind == CfirClassMemberScopeKind.USE_SITE }
        ?.let { extendProvider?.let { provider -> CfirExtendMemberScope(classSymbol.classId, provider) } }
    private val parentScopes: List<CfirTypeScope> by lazy { buildParentScopes() }

    override fun getCallableNames(): Set<Name> = buildSet {
        addDeclaredCallableNames(classSymbol.cfir.declarations, this)
        addAll(extendCallableNames())
        addAll(parentCallableNames())
    }

    override fun getClassifierNames(): Set<Name> = buildSet {
        for (declaration in classSymbol.cfir.declarations) {
            if (declaration is CfirClassLikeDeclaration) add((declaration.symbol as CfirClassLikeSymbol<*>).name)
        }
        addAll(extendClassifierNames())
        addAll(parentClassifierNames())
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        for (parent in parentScopes) {
            val candidates = mutableListOf<CfirNamedFunctionSymbol>()
            parent.processFunctionsByName(functionSymbol.name) { candidates += it }
            for (candidate in candidates) {
                if (processor(candidate, parent) == ProcessorAction.STOP) {
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
                if (processor(candidate, parent) == ProcessorAction.STOP) {
                    return ProcessorAction.STOP
                }
            }
        }
        return ProcessorAction.NEXT
    }

    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession,
    ): CfirTypeScope? = CfirClassUseSiteMemberScope(
        session = newSession,
        classSymbol = classSymbol,
        symbolProvider = newSession.symbolProvider,
        extendProvider = newSession.extendProviderOrNull,
        directSupertypeProvider = newSession.directSupertypeProviderOrNull,
        ownerType = ownerType,
        scopeKind = scopeKind,
    )

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

    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
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

    private fun buildParentScopes(): List<CfirTypeScope> {
        val rootType = ownerType ?: return emptyList()
        return directParentTypesOf(rootType).mapNotNull { supertype ->
            val classId = supertype.classIdOrPrimitiveClassId ?: return@mapNotNull null
            val parentSymbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: return@mapNotNull null
            CfirClassUseSiteMemberScope(
                session = session,
                classSymbol = parentSymbol,
                symbolProvider = symbolProvider,
                extendProvider = extendProvider,
                directSupertypeProvider = directSupertypeProvider,
                ownerType = supertype,
                scopeKind = scopeKind,
            )
        }
    }

    private fun extendCallableNames(): Set<Name> = buildSet {
        for (extend in extendsForCurrentClass()) {
            for (declaration in extend.declarations) {
                when (declaration) {
                    is CfirFunction -> declaration.callableNameOrNull()?.let(::add)
                    is org.cangnova.cangjie.cfir.declarations.CfirProperty -> add(declaration.name)
                    is org.cangnova.cangjie.cfir.declarations.CfirFieldVariable -> add(declaration.name)
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
        if (scopeKind != CfirClassMemberScopeKind.USE_SITE) return@buildList
        val provider = extendProvider ?: return@buildList
        addAll(provider.getExtendsForClass(classSymbol.classId))
        classSymbol.classId.toPrimitiveTypeKindOrNull()?.let { kind ->
            addAll(provider.getExtendsForBuiltinType(kind))
        }
    }.filter { extendProvider?.isExtendAccessible(it) != false }

    private fun addDeclaredCallableNames(
        declarations: List<CfirDeclaration>,
        destination: MutableSet<Name>,
    ) {
        for (declaration in declarations) {
            when (declaration) {
                is CfirFunction -> declaration.callableNameOrNull()?.let(destination::add)
                is CfirProperty -> destination += declaration.name
                is CfirFieldVariable -> destination += declaration.name
                is CfirEnumConstructor -> destination += declaration.name
                else -> Unit
            }
        }
    }

    private fun parentCallableNames(): Set<Name> = buildSet {
        parentScopes.flatMapTo(this) { it.getCallableNames() }
    }

    private fun parentClassifierNames(): Set<Name> = buildSet {
        parentScopes.flatMapTo(this) { it.getClassifierNames() }
    }

    private fun directParentTypesOf(type: ConeCangJieType): List<ConeCangJieType> {
        if (scopeKind == CfirClassMemberScopeKind.DECLARATION_SITE) {
            return classSymbol.cfir.superTypeRefs.mapNotNull { superTypeRef ->
                val resolvedRef = superTypeRef as? CfirResolvedTypeRef ?: return@mapNotNull null
                resolvedRef.coneType
            }
        }

        val classId = type.classIdOrPrimitiveClassId ?: classSymbol.classId
        return session.typeAwareSupertypeProviderOrNull
            ?.getDirectSupertypes(type)
            ?.takeIf { it.isNotEmpty() }
            ?: directSupertypeProvider
                ?.getDirectSuperTypes(classId)
                ?.map { it.coneType }
                ?.takeIf { it.isNotEmpty() }
            ?: classSymbol.cfir.superTypeRefs.mapNotNull { superTypeRef ->
                val resolvedRef = superTypeRef as? CfirResolvedTypeRef ?: return@mapNotNull null
                resolvedRef.coneType
            }
    }

    override fun toString(): String {
        return "Use site scope of ${classSymbol.classId}"
    }
}

private fun declarationSelfType(symbol: CfirClassLikeSymbol<*>): ConeCangJieType? {
    return symbol.takeIf { it.isBound }?.constructType()
}
