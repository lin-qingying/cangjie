package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.ResolveStateAccess
import org.cangnova.cangjie.cfir.declarations.asResolveState
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 内建原始类型符号提供器。
 * 为仓颉的原始类型（如 `Int8`、`Bool`、`Float64`）提供预构建的合成类符号。
 * 这些类型由 [PrimitiveTypeKind] 定义，不属于标准库 `std.core` 中的普通类声明，
 * 而是编译器内建的原始类型。
 * 对齐 Kotlin K2 的 `FirBuiltinSymbolProvider`（仅覆盖内建部分）。
 */
class CfirBuiltinSymbolProvider(
    session: CfirSession,
) : CfirSymbolProvider(session) {

    override val symbolNamesProvider: CfirSymbolNamesProvider = BuiltinNamesProvider

    private val builtinClassSymbols: Map<ClassId, CfirClassSymbol> by lazy {
        buildBuiltinClassSymbols()
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassSymbol? {
        return builtinClassSymbols[classId]
    }

    override fun getClassIdBySymbol(classSymbol: CfirClassSymbol): ClassId? {
        return builtinClassSymbols.entries.firstOrNull { it.value == classSymbol }?.key
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> {
        // Builtin primitive types do not provide top-level callables.
        return emptyList()
    }

    override fun hasPackage(fqName: FqName): Boolean {
        return fqName == StandardNames.BASIC_PACKAGE_FQ_NAME
    }

    @OptIn(CfirImplementationDetail::class, ResolveStateAccess::class)
    private fun buildBuiltinClassSymbols(): Map<ClassId, CfirClassSymbol> {
        val moduleData = CfirBinaryDependenciesModuleData(Name.identifier("builtins")).apply {
            bindSession(this@CfirBuiltinSymbolProvider.session)
        }
        return buildMap {
            for (kind in PrimitiveTypeKind.entries) {
                val name = Name.identifier(kind.typeName)
                val classId = ClassId(StandardNames.BASIC_PACKAGE_FQ_NAME, name)
                val symbol = CfirClassSymbol()
                val cfirClass = CfirClassImpl(
                    source = null,
                    symbol = symbol,
                    origin = CfirDeclarationOrigin.Synthetic,
                    annotations = emptyList(),
                    moduleData = moduleData,
                    attributes = CfirDeclarationAttributes.EMPTY,
                    status = CfirDeclarationStatusImpl.DEFAULT,
                    typeParameters = emptyList(),
                    superTypeRefs = emptyList(),
                    declarations = emptyList(),
                    name = name,
                    classKind = CfirClassKind.STRUCT,
                )
                cfirClass.resolveState = CfirResolvePhase.RAW_CFIR.asResolveState()
                symbol.bind(cfirClass)
                put(classId, symbol)
            }
        }
    }

    private object BuiltinNamesProvider : CfirSymbolNamesProvider {
        override fun getPackageNames(): Set<FqName> =
            setOf(StandardNames.BASIC_PACKAGE_FQ_NAME)

        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? {
            if (packageFqName != StandardNames.BASIC_PACKAGE_FQ_NAME) return emptySet()
            return BUILTIN_TYPE_NAMES
        }

        override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? = emptySet()

        private val BUILTIN_TYPE_NAMES: Set<Name> =
            PrimitiveTypeKind.entries.mapTo(mutableSetOf()) { Name.identifier(it.typeName) }
    }
}
