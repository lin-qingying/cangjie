@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.scopes

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.initDefaultResolveState
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirFunctionImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirPropertyImpl
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.providers.CfirSessionExtendProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
import org.cangnova.cangjie.cfir.scopes.impl.CfirExtendMemberScope
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CfirExtendMemberScopeTest {
    @Test
    fun `scope exposes extend members for target class only`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val otherClassId = ClassId(packageFqName, Name.identifier("Other"))

        val targetNestedClass = newClassDeclaration(moduleData, "TargetNested")
        val targetFunction = newFunctionDeclaration(moduleData, "extFun")
        val targetProperty = newPropertyDeclaration(moduleData, "extProp")
        val targetExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = emptyList(),
            declarations = listOf(targetNestedClass, targetFunction, targetProperty),
        )

        val otherFunction = newFunctionDeclaration(moduleData, "extFun")
        val otherProperty = newPropertyDeclaration(moduleData, "extProp")
        val otherExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(otherClassId),
            superTypeRefs = emptyList(),
            declarations = listOf(otherFunction, otherProperty),
        )

        val file = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(targetExtend, otherExtend))
        val store = CfirExtendIndexStore().also { it.rebuild(listOf(file), NoopTypeResolver) }
        val provider = CfirSessionExtendProvider(store)
        val scope = CfirExtendMemberScope(targetClassId, provider)

        val functions = mutableListOf<CfirFunctionSymbol>()
        scope.processFunctionsByName(Name.identifier("extFun"), functions::add)
        assertEquals(listOf(targetFunction.symbol as CfirFunctionSymbol), functions)

        val properties = mutableListOf<CfirPropertySymbol>()
        scope.processPropertiesByName(Name.identifier("extProp"), properties::add)
        assertEquals(listOf(targetProperty.symbol as CfirPropertySymbol), properties)

        val classifiers = mutableListOf<CfirClassSymbol>()
        scope.processClassifiersByName(Name.identifier("TargetNested"), classifiers::add)
        assertEquals(listOf(targetNestedClass.symbol as CfirClassSymbol), classifiers)
    }
}

private fun newClassDeclaration(moduleData: CfirModuleData, name: String): CfirClassImpl {
    val symbol = CfirClassSymbol()
    return CfirClassImpl(
        source = null,
        moduleData = moduleData,
        annotations = emptyList(),
        symbol = symbol,
        origin = CfirDeclarationOrigin.Source,
        attributes = CfirDeclarationAttributes.EMPTY,
        status = CfirDeclarationStatusImpl(),
        typeParameters = emptyList(),
        superTypeRefs = emptyList(),
        declarations = emptyList(),
        name = Name.identifier(name),
        classKind = CfirClassKind.CLASS,
    ).also {
        it.initDefaultResolveState()
        symbol.bind(it)
    }
}

private fun newFunctionDeclaration(moduleData: CfirModuleData, name: String): CfirFunctionImpl {
    val symbol = CfirFunctionSymbol()
    return CfirFunctionImpl(
        source = null,
        moduleData = moduleData,
        annotations = emptyList(),
        symbol = symbol,
        origin = CfirDeclarationOrigin.Source,
        attributes = CfirDeclarationAttributes.EMPTY,
        status = CfirDeclarationStatusImpl(),
        typeParameters = emptyList(),
        returnTypeRef = org.cangnova.cangjie.cfir.types.impl.CfirImplicitTypeRefImpl(emptyList()),
        name = Name.identifier(name),
        valueParameters = emptyList(),
        body = null,
        isMut = false,
    ).also {
        it.initDefaultResolveState()
        symbol.bind(it)
    }
}

private fun newPropertyDeclaration(moduleData: CfirModuleData, name: String): CfirPropertyImpl {
    val symbol = CfirPropertySymbol()
    return CfirPropertyImpl(
        source = null,
        moduleData = moduleData,
        annotations = emptyList(),
        symbol = symbol,
        origin = CfirDeclarationOrigin.Source,
        attributes = CfirDeclarationAttributes.EMPTY,
        status = CfirDeclarationStatusImpl(),
        typeParameters = emptyList(),
        returnTypeRef = org.cangnova.cangjie.cfir.types.impl.CfirImplicitTypeRefImpl(emptyList()),
        name = Name.identifier(name),
        initializer = null,
        getter = null,
        setter = null,
    ).also {
        it.initDefaultResolveState()
        symbol.bind(it)
    }
}

private object NoopTypeResolver : CfirTypeResolver() {
    override fun resolveType(
        typeRef: org.cangnova.cangjie.cfir.types.CfirTypeRef,
        configuration: org.cangnova.cangjie.cfir.resolve.TypeResolutionConfiguration,
        areBareTypesAllowed: Boolean,
        isOperandOfIsOperator: Boolean,
        resolveDeprecations: Boolean,
        supertypeSupplier: org.cangnova.cangjie.cfir.resolve.SupertypeSupplier,
        expandTypeAliases: Boolean,
    ): org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionResult {
        return org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionResult(
            type = org.cangnova.cangjie.cfir.types.ConeErrorType("NoopTypeResolver"),
            diagnostic = null,
        )
    }

    override fun resolveClass(typeRef: org.cangnova.cangjie.cfir.types.CfirTypeRef): org.cangnova.cangjie.cfir.declarations.CfirClass? = null

    override fun resolveClass(classId: ClassId): org.cangnova.cangjie.cfir.declarations.CfirClass? = null
}

