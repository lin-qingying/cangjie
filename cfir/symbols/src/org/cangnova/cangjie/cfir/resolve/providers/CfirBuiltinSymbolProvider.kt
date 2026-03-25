package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 内建类型符号提供器（仓颉语义版）。
 *
 * 设计约束：
 * 1. 原始类型（Int64/Bool/Float64/...）不是 class/struct 声明；
 * 2. 原始类型通过 `session.builtinTypes` 提供，不通过 `CfirClassSymbol` 暴露；
 * 3. 因此这里不再为 primitive 构造合成 `CfirClassSymbol`。
 */
class CfirBuiltinSymbolProvider(
    session: CfirSession,
) : CfirSymbolProvider(session) {

    override val symbolNamesProvider: CfirSymbolNamesProvider = BuiltinNamesProvider

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassSymbol? {
        // Primitive types are compiler-builtins, not class-like declarations.
        return null
    }

    override fun getClassIdBySymbol(classSymbol: CfirClassSymbol): ClassId? {
        return null
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> {
        // Builtin primitive types do not provide top-level callables.
        return emptyList()
    }

    override fun hasPackage(fqName: FqName): Boolean {
        // 保留 root package 可见性，避免包遍历逻辑回退。
        return fqName == StandardNames.BASIC_PACKAGE_FQ_NAME
    }

    private object BuiltinNamesProvider : CfirSymbolNamesProvider {
        override fun getPackageNames(): Set<FqName> =
            setOf(StandardNames.BASIC_PACKAGE_FQ_NAME)

        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? = emptySet()

        override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? = emptySet()
    }
}
