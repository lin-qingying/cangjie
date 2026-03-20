package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirClassScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.name.Name

/**
 * 类使用点成员 scope，合并本类声明和继承的父类成员。
 *
 * 查询顺序：先查本类直接声明（遮蔽父类同名成员），再查所有父类成员。
 * 递归深度受 [MAX_DEPTH] 限制，防止循环继承导致栈溢出。
 *
 * 参考 K2 FirClassUseSiteMemberScope。
 */
class CfirClassUseSiteMemberScope(
    private val classSymbol: CfirClassSymbol,
    private val symbolProvider: CfirSymbolProvider,
) : CfirClassScope {

    private val declaredScope = CfirClassDeclaredMemberScope(classSymbol)
    private val parentScopes: List<CfirClassDeclaredMemberScope> by lazy { buildParentScopes() }

    override fun processClassifiersByName(name: Name, processor: (CfirClassSymbol) -> Unit) {
        val found = mutableListOf<CfirClassSymbol>()
        declaredScope.processClassifiersByName(name) { found += it }
        if (found.isNotEmpty()) {
            found.forEach(processor)
            return
        }
        for (parent in parentScopes) {
            parent.processClassifiersByName(name, processor)
        }
    }

    override fun processFunctionsByName(name: Name, processor: (CfirFunctionSymbol) -> Unit) {
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
        symbol: CfirClassSymbol,
        depth: Int,
    ): List<CfirClassDeclaredMemberScope> {
        if (depth >= MAX_DEPTH) return emptyList()
        val klass = symbol.cfir
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
