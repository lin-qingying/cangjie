package org.cangjie.cfir.resolve.providers

import org.cangjie.cfir.CfirImplementationDetail
import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.declarations.CfirClassKind
import org.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
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
 * 鍐呭缓鍘熷绫诲瀷绗﹀彿鎻愪緵鑰呫€? *
 * 涓轰粨棰夊唴寤哄師濮嬬被鍨嬶紙Int8, Bool, Float64 绛夛級鎻愪緵棰勬瀯寤虹殑鍚堟垚绫荤鍙枫€? * 杩欎簺绫诲瀷鐢?[PrimitiveTypeKind] 鏋氫妇瀹氫箟锛屼笉灞炰簬鏍囧噯搴?std.core 鐨勭被澹版槑锛? * 鑰屾槸缂栬瘧鍣ㄥ唴寤虹殑鍘熷绫诲瀷銆? *
 * 瀵归綈 Kotlin K2 鐨?FirBuiltinSymbolProvider锛堜粎鍐呭缓閮ㄥ垎锛夈€? */
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
        // Builtin primitive types do not provide top-level callables.
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
                    status = CfirDeclarationStatusImpl.DEFAULT,
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

