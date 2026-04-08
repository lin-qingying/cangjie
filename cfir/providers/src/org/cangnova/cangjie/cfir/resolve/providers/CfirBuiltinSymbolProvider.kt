package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.common.CfirBinaryDependenciesModuleData
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.common.nullableModuleData
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.initDefaultResolveState
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.BuiltinPrimitiveOperators
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.isExposedBuiltinClassifier
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Exposes builtin primitive types as synthetic class-like declarations so that
 * provider, scope and resolver code can use the same architecture as regular
 * classifiers, mirroring Kotlin FIR's builtin provider design.
 */
class CfirBuiltinSymbolProvider(
    session: CfirSession,
) : CfirSymbolProvider(session) {

    private val builtinModuleData by lazy(LazyThreadSafetyMode.PUBLICATION) {
        session.nullableModuleData
            ?: CfirBinaryDependenciesModuleData(Name.identifier("<builtins>")).also { it.bindSession(session) }
    }

    private val primitiveDeclarationsByClassId: Map<ClassId, CfirPrimitiveTypeDeclaration> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        PrimitiveTypeKind.entries.associateBy(
            keySelector = { it.classId },
            valueTransform = ::buildPrimitiveDeclaration,
        )
    }

    private val containingClassIdsByCallable: Map<CfirCallableSymbol<*>, ClassId> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildMap {
            for ((classId, declaration) in primitiveDeclarationsByClassId) {
                declaration.declarations.forEach { nested ->
                    val callable = (nested as? CfirCallableDeclaration)?.symbol as? CfirCallableSymbol<*> ?: return@forEach
                    put(callable, classId)
                }
            }
        }
    }

    override val symbolNamesProvider: CfirSymbolNamesProvider = BuiltinNamesProvider

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
        primitiveDeclarationsByClassId[classId]?.symbol

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> =
        emptyList()

    override fun hasPackage(fqName: FqName): Boolean =
        fqName == StandardNames.BASIC_PACKAGE_FQ_NAME

    override fun getContainingClassId(symbol: CfirCallableSymbol<*>): ClassId? =
        containingClassIdsByCallable[symbol.unwrapCallableForDeclarationMetadataLookup()]

    private fun buildPrimitiveDeclaration(kind: PrimitiveTypeKind): CfirPrimitiveTypeDeclaration {
        val symbol = CfirPrimitiveTypeSymbol(kind.classId, kind)
        val declaration = CfirPrimitiveTypeDeclaration(
            moduleData = builtinModuleData,
            symbol = symbol,
            name = kind.classId.shortClassName,
            kind = kind,
            origin = CfirDeclarationOrigin.Synthetic.Default,
            attributes = CfirDeclarationAttributes.EMPTY,
            declarations = buildPrimitiveMembers(kind).toMutableList(),
            superTypeRefs = mutableListOf(),
        )
        declaration.initDefaultResolveState()

        return declaration
    }

    private fun buildPrimitiveMembers(kind: PrimitiveTypeKind): List<CfirDeclaration> =
        BuiltinPrimitiveOperators.signaturesFor(kind).map { signature ->
            val functionSymbol = CfirNamedFunctionSymbol(CallableId(kind.classId, signature.name))
            val parameters = signature.parameterKinds.mapIndexed { index, parameterKind ->
                val parameterSymbol = CfirValueParameterSymbol(CallableId(signature.name))
                buildValueParameter {
                    moduleData = builtinModuleData
                    resolvePhase = CfirResolvePhase.BODY_RESOLVE
                    origin = CfirDeclarationOrigin.Synthetic.FakeFunction
                    attributes = CfirDeclarationAttributes.EMPTY
                    isLocal = false
                    dispatchReceiverType = null
                    symbol = parameterSymbol
                    isNamed = false
                    status = CfirDeclarationStatusImpl()
                    returnTypeRef = buildResolvedTypeRef {
                        coneType = ConePrimitiveType(parameterKind)
                    }
                    name = Name.identifier("p$index")
                }
            }

            val status = CfirDeclarationStatusImpl().apply {
                isOperator = true
            }
            buildNamedFunction {
                moduleData = builtinModuleData
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Synthetic.FakeFunction
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                dispatchReceiverType = null
                this.status = status
                returnTypeRef = buildResolvedTypeRef {
                    coneType = ConePrimitiveType(signature.returnKind)
                }
                valueParameters += parameters
                symbol = functionSymbol
                name = signature.name
                isMut = false
            }
        }

    private object BuiltinNamesProvider : CfirSymbolNamesProvider {
        private val builtinClassifierNames: Set<Name> = PrimitiveTypeKind.entries
            .filter(PrimitiveTypeKind::isExposedBuiltinClassifier)
            .mapTo(linkedSetOf()) { Name.identifier(it.typeName) }

        override fun getPackageNames(): Set<FqName> =
            setOf(StandardNames.BASIC_PACKAGE_FQ_NAME)

        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
            if (packageFqName == StandardNames.BASIC_PACKAGE_FQ_NAME) builtinClassifierNames else emptySet()

        override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? = emptySet()
    }
}
