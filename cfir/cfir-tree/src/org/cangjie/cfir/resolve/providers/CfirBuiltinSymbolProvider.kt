package org.cangjie.cfir.resolve.providers

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.declarations.CfirClassKind
import org.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangjie.cfir.symbols.CfirClassSymbol
import org.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 内建原始类型符号提供者。
 *
 * 为仓颉内建原始类型（Int8, Bool, Float64 等）提供预构建的合成类符号。
 * 这些类型由 [PrimitiveTypeKind] 枚举定义，不属于标准库 std.core 的类声明，
 * 而是编译器内建的原始类型。
 *
 * 对齐 Kotlin K2 的 FirBuiltinSymbolProvider（仅内建部分）。
 */
class CfirBuiltinSymbolProvider(
    private val session: CfirSession,
) : CfirSymbolProvider() {

    override val symbolNamesProvider: CfirSymbolNamesProvider = BuiltinNamesProvider

    private val builtinClassSymbols: Map<ClassId, CfirClassSymbol> by lazy {
        buildBuiltinClassSymbols()
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassSymbol? {
        return builtinClassSymbols[classId]
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> {
        // 内建原始类型没有顶级可调用符号（方法通过 extend 机制添加）
        return emptyList()
    }

    override fun hasPackage(fqName: FqName): Boolean {
        return fqName == StandardNames.BASIC_PACKAGE_FQ_NAME
    }

    @OptIn(CfirImplementationDetail::class)
    private fun buildBuiltinClassSymbols(): Map<ClassId, CfirClassSymbol> {
        val moduleData = CfirModuleData(Name.identifier("builtins"))
        return buildMap {
            for (kind in PrimitiveTypeKind.entries) {
                val name = Name.identifier(kind.typeName)
                val classId = ClassId(StandardNames.BASIC_PACKAGE_FQ_NAME, name)
                val symbol = CfirClassSymbol()
                val cfirClass = CfirClassImpl(
                    symbol = symbol,
                    origin = CfirDeclarationOrigin.Synthetic,
                    annotations = emptyList(),
                    moduleData = moduleData,
                    resolvePhase = CfirResolvePhase.RAW_CFIR,
                    attributes = CfirDeclarationAttributes.EMPTY,
                    status = CfirDeclarationStatus.DEFAULT,
                    typeParameters = emptyList(),
                    superTypeRefs = emptyList(),
                    declarations = emptyList(),
                    name = name,
                    classKind = CfirClassKind.STRUCT,
                )
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
