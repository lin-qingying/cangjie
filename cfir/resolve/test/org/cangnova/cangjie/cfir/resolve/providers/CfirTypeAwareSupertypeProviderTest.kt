@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirInterfaceImpl
import org.cangnova.cangjie.cfir.declarations.initDefaultResolveState
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.services.CfirTypeAwareSupertypeProviderImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CfirTypeAwareSupertypeProviderTest {
    @Test
    fun `generic extend is instantiated for concrete type and subtype traversal`() {
        val packageFqName = FqName("sample.generic")
        val boxClassId = ClassId(packageFqName, Name.identifier("Box"))
        val iterableClassId = ClassId(packageFqName, Name.identifier("Iterable"))

        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val typeParameter = ExtendTestFixtures.newTypeParameter(moduleData, "T")
        val boxClass = newClass(
            moduleData = moduleData,
            classId = boxClassId,
            typeParameters = listOf(typeParameter),
        )
        val iterableInterface = newInterface(
            moduleData = moduleData,
            classId = iterableClassId,
            typeParameters = listOf(typeParameter),
        )
        val boxExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(typeParameter),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(
                classId = boxClassId,
                typeArguments = listOf(typeParameterType(typeParameter)),
            ),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = iterableClassId,
                    typeArguments = listOf(typeParameterType(typeParameter)),
                    isInterface = true,
                )
            ),
        )

        val typeContext = session.registerTestTypeContext(
            declarations = listOf(boxClass, iterableInterface),
            extends = listOf(boxExtend),
        )

        val boxInt = classType(boxClassId, ConePrimitiveType.INT32)
        val iterableInt = interfaceType(iterableClassId, ConePrimitiveType.INT32)

        assertIterableEquals(
            listOf(iterableInt),
            session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(boxInt),
        )
        assertTrue(AbstractTypeChecker.isSubtypeOf(typeContext, boxInt, iterableInt))
        assertTrue(
            with(typeContext) {
                boxInt.anySuperTypeConstructor { constructor ->
                    constructor is org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag &&
                            constructor.classId == iterableClassId
                }
            }
        )
    }

    @Test
    fun `extends on superclass are inherited by concrete child type`() {
        val packageFqName = FqName("sample.inherited")
        val baseClassId = ClassId(packageFqName, Name.identifier("Base"))
        val childClassId = ClassId(packageFqName, Name.identifier("Child"))
        val markerClassId = ClassId(packageFqName, Name.identifier("Marker"))

        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val typeParameter = ExtendTestFixtures.newTypeParameter(moduleData, "T")
        val baseClass = newClass(
            moduleData = moduleData,
            classId = baseClassId,
            typeParameters = listOf(typeParameter),
        )
        val childClass = newClass(
            moduleData = moduleData,
            classId = childClassId,
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = baseClassId,
                    typeArguments = listOf(ConePrimitiveType.INT32),
                )
            ),
        )
        val markerInterface = newInterface(
            moduleData = moduleData,
            classId = markerClassId,
            typeParameters = listOf(typeParameter),
        )
        val baseExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(typeParameter),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(
                classId = baseClassId,
                typeArguments = listOf(typeParameterType(typeParameter)),
            ),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = markerClassId,
                    typeArguments = listOf(typeParameterType(typeParameter)),
                    isInterface = true,
                )
            ),
        )

        val typeContext = session.registerTestTypeContext(
            declarations = listOf(baseClass, childClass, markerInterface),
            extends = listOf(baseExtend),
        )

        val childType = classType(childClassId)
        val baseInt = classType(baseClassId, ConePrimitiveType.INT32)
        val markerInt = interfaceType(markerClassId, ConePrimitiveType.INT32)

        assertEquals(
            listOf(baseInt, markerInt),
            session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(childType),
        )
        assertTrue(AbstractTypeChecker.isSubtypeOf(typeContext, childType, markerInt))
    }

    @Test
    fun `type comparison uses extend provider instead of declaration graph`() {
        val packageFqName = FqName("sample.crossfile")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val parentInterfaceId = ClassId(packageFqName, Name.identifier("Parent"))

        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val targetClass = newClass(moduleData, targetClassId)
        val parentInterface = newInterface(moduleData, parentInterfaceId)
        val extend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(parentInterfaceId, isInterface = true)),
        )

        val typeContext = session.registerTestTypeContext(
            declarations = listOf(targetClass, parentInterface),
            extends = listOf(extend),
        )

        assertTrue(
            AbstractTypeChecker.isSubtypeOf(
                typeContext,
                classType(targetClassId),
                interfaceType(parentInterfaceId),
            )
        )
    }
}

private fun CfirSession.registerTestTypeContext(
    declarations: List<CfirClassLikeDeclaration>,
    extends: List<CfirExtend>,
): ConeInferenceContext {
    register(CfirSymbolProvider::class, TestSymbolProvider(this, declarations))
    register(CfirExtendProvider::class, TestExtendProvider(extends))
    register(CfirTypeAwareSupertypeProvider::class, CfirTypeAwareSupertypeProviderImpl(this))
    return object : ConeInferenceContext {
        override val session: CfirSession
            get() = this@registerTestTypeContext
    }
}

private class TestSymbolProvider(
    session: CfirSession,
    declarations: List<CfirClassLikeDeclaration>,
) : CfirSymbolProvider(session) {
    private val declarationsByClassId = declarations.associateBy { it.symbol.classId }

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
        declarationsByClassId[classId]?.symbol

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> =
        emptyList()

    override fun hasPackage(fqName: FqName): Boolean =
        declarationsByClassId.keys.any { it.packageFqName == fqName }
}

private class TestExtendProvider(
    extends: List<CfirExtend>,
) : CfirExtendProvider {
    private val extendsByClassId: Map<ClassId, List<CfirExtend>> = extends.groupBy { extend ->
        val extendedType = extend.extendedTypeRef as? org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
            ?: error("extend target must already be resolved in test fixtures")
        extendedType.coneType.classIdOrPrimitiveClassId
            ?: error("extend target must be classifier type in test fixtures")
    }

    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> =
        extendsByClassId[classId].orEmpty()

    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> =
        extendsByClassId
            .filterKeys { it.packageFqName == packageFqName }
            .values
            .flatten()

    override fun getExtendsForBuiltinType(kind: org.cangnova.cangjie.cfir.types.PrimitiveTypeKind): List<CfirExtend> =
        extendsByClassId[kind.classId].orEmpty()
}

private fun newClass(
    moduleData: CfirModuleData,
    classId: ClassId,
    typeParameters: List<CfirTypeParameter> = emptyList(),
    superTypeRefs: List<CfirTypeRef> = emptyList(),
): CfirClassImpl {
    val symbol = CfirClassSymbol(classId)
    return CfirClassImpl(
        source = null,
        moduleData = moduleData,
        resolvePhase = CfirResolvePhase.BODY_RESOLVE,
        annotations = MutableOrEmptyList.empty(),
        origin = CfirDeclarationOrigin.Library,
        attributes = CfirDeclarationAttributes.EMPTY,
        isLocal = false,
        status = CfirDeclarationStatusImpl(),
        typeParameters = typeParameters.toMutableList(),
        symbol = symbol,
        superTypeRefs = superTypeRefs.toMutableList(),
        declarations = mutableListOf(),
        name = classId.shortClassName,
    ).also { it.initDefaultResolveState() }
}

private fun newInterface(
    moduleData: CfirModuleData,
    classId: ClassId,
    typeParameters: List<CfirTypeParameter> = emptyList(),
    superTypeRefs: List<CfirTypeRef> = emptyList(),
): CfirInterfaceImpl {
    val symbol = CfirInterfaceSymbol(classId)
    return CfirInterfaceImpl(
        source = null,
        moduleData = moduleData,
        resolvePhase = CfirResolvePhase.BODY_RESOLVE,
        annotations = MutableOrEmptyList.empty(),
        origin = CfirDeclarationOrigin.Library,
        attributes = CfirDeclarationAttributes.EMPTY,
        isLocal = false,
        declarations = mutableListOf<CfirDeclaration>(),
        status = CfirDeclarationStatusImpl(),
        typeParameters = typeParameters.toMutableList(),
        symbol = symbol,
        superTypeRefs = superTypeRefs.toMutableList(),
        name = classId.shortClassName,
    ).also { it.initDefaultResolveState() }
}

private fun typeParameterType(typeParameter: CfirTypeParameter): ConeCangJieType =
    ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())

private fun classType(classId: ClassId, vararg typeArguments: ConeCangJieType): ConeClassLikeType =
    ConeClassLikeType(
        lookupTag = classId.toLookupTag(),
        typeArguments = typeArguments.map(::ConeTypeProjection),
    )

private fun interfaceType(classId: ClassId, vararg typeArguments: ConeCangJieType): ConeClassLikeType =
    ConeClassLikeType(
        lookupTag = classId.toLookupTag(),
        typeArguments = typeArguments.map(::ConeTypeProjection),
        isInterface = true,
    )
