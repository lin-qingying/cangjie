package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.name.Name

/**
 * 类使用点成员 scope，合并本类声明和继承的父类成员。
 *
 * 查询顺序：先查本类直接声明（遮蔽父类同名成员），再查所有父类成员。
 * 递归深度受 [MAX_DEPTH] 限制，防止循环继承导致栈溢出。
 *
 * 继承 [CfirTypeScope] 以支持 override 追踪（对标 K2 FirClassUseSiteMemberScope）。
 */
class CfirClassUseSiteMemberScope(
    private val classSymbol: CfirClassLikeSymbol<*>,
    private val symbolProvider: CfirSymbolProvider,
) : CfirTypeScope() {

    private val declaredScope = CfirClassDeclaredMemberScope(classSymbol)
    private val parentScopes: List<CfirClassDeclaredMemberScope> by lazy { buildParentScopes() }

    override fun getCallableNames(): Set<Name> {
        val names = mutableSetOf<Name>()
        // 收集本类和父类中的所有 callable 名称
        for (decl in classSymbol.cfir.declarations) {
            when (decl) {
                is CfirFunction -> decl.callableNameOrNull()?.let(names::add)
                is org.cangnova.cangjie.cfir.declarations.CfirProperty -> names += decl.name
                is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor -> names += decl.name
                else -> {}
            }
        }
        return names
    }

    override fun getClassifierNames(): Set<Name> {
        val names = mutableSetOf<Name>()
        for (decl in classSymbol.cfir.declarations) {
            if (decl is CfirClass) names += decl.name
        }
        return names
    }

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirFunctionSymbol<*>,
        processor: (CfirFunctionSymbol<*>, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        // 在父类 scope 中查找同名函数作为被 override 的候选
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
        val found = mutableListOf<CfirClassLikeSymbol<*>>()
        declaredScope.processClassifiersByName(name) { found += it }
        if (found.isNotEmpty()) {
            found.forEach(processor)
            return
        }
        for (parent in parentScopes) {
            parent.processClassifiersByName(name, processor)
        }
    }

    override fun processFunctionsByName(name: Name, processor: (CfirFunctionSymbol<*>) -> Unit) {
        // 函数不遮蔽：本类和父类均可能有同名重载
        declaredScope.processFunctionsByName(name, processor)
        for (parent in parentScopes) {
            parent.processFunctionsByName(name, processor)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        val found = mutableListOf<CfirPropertySymbol>()
        declaredScope.processPropertiesByName(name) { found += it }
        if (found.isNotEmpty()) {
            found.forEach(processor)
            return
        }
        for (parent in parentScopes) {
            parent.processPropertiesByName(name, processor)
        }
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        declaredScope.processCallablesByName(name, processor)
        for (parent in parentScopes) {
            parent.processCallablesByName(name, processor)
        }
    }

    private fun buildParentScopes(): List<CfirClassDeclaredMemberScope> {
        return buildParentScopesRecursive(classSymbol, 0)
    }

    private fun buildParentScopesRecursive(
        symbol: CfirClassLikeSymbol<*>,
        depth: Int,
    ): List<CfirClassDeclaredMemberScope> {
        if (depth >= MAX_DEPTH) return emptyList()
        val klass = symbol.cfir as CfirClassLikeDeclaration
        val result = mutableListOf<CfirClassDeclaredMemberScope>()

        for (superTypeRef in klass.superTypeRefs) {
            val resolvedRef = superTypeRef as? CfirResolvedTypeRef ?: continue
            val classId = when (val coneType = resolvedRef.coneType) {
                is ConeClassLikeType -> coneType.classId
                is ConeStructType -> coneType.classId
                is ConeEnumType -> coneType.classId
                else -> continue
            }
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
