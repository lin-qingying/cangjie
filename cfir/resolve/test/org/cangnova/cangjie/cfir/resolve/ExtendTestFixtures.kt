@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.CfirAnnotation
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.initDefaultResolveState
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirExtendImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirFileImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirInterfaceImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirPackageDirectiveImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirTypeParameterImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

internal object ExtendTestFixtures {
    class TestSession : CfirSession(Kind.Source) {
        override fun toString(): String = "ExtendTestSession"
    }

    fun newSessionAndModule(moduleName: String = "extend-test"): Pair<TestSession, CfirModuleData> {
        val session = TestSession()
        val moduleData = CfirSourceModuleData(
            name = Name.identifier(moduleName),
            dependencies = emptyList(),
            refinementDependencies = emptyList(),
            platform = CfirPlatform.DEFAULT,
        )
        moduleData.bindSession(session)
        session.register(CfirModuleData::class, moduleData)
        return session to moduleData
    }

    fun classTypeRef(classId: ClassId, isInterface: Boolean = false): CfirResolvedTypeRefImpl {
        return CfirResolvedTypeRefImpl(
            source = null,
            annotations = emptyList(),
            coneType = ConeClassLikeType(
                lookupTag = ConeClassLookupTagImpl(classId),
                isInterface = isInterface,
            ),
            delegatedTypeRef = null,
        )
    }

    fun classTypeRef(
        classId: ClassId,
        typeArguments: List<org.cangnova.cangjie.cfir.types.ConeCangJieType>,
        isInterface: Boolean = false,
    ): CfirResolvedTypeRefImpl {
        return CfirResolvedTypeRefImpl(
            source = null,
            annotations = emptyList(),
            coneType = ConeClassLikeType(
                lookupTag = ConeClassLookupTagImpl(classId),
                typeArguments = typeArguments,
                isInterface = isInterface,
            ),
            delegatedTypeRef = null,
        )
    }

    fun typeParameterType(name: String): ConeTypeParameterType {
        return ConeTypeParameterType(ConeTypeParameterLookupTag(name))
    }

    fun newExtend(
        moduleData: CfirModuleData,
        extendedTypeRef: CfirTypeRef,
        superTypeRefs: List<CfirTypeRef>,
        typeParameters: List<CfirTypeParameter> = emptyList(),
        declarations: List<CfirDeclaration> = emptyList(),
    ): CfirExtendImpl {
        val symbol = CfirExtendSymbol()
        val declaration = CfirExtendImpl(
            source = null,
            moduleData = moduleData,
            annotations = emptyList<CfirAnnotation>(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Source,
            attributes = CfirDeclarationAttributes.EMPTY,
            status = CfirDeclarationStatusImpl(),
            typeParameters = typeParameters,
            extendedTypeRef = extendedTypeRef,
            superTypeRefs = superTypeRefs,
            declarations = declarations,
        )
        declaration.initDefaultResolveState()
        symbol.bind(declaration)
        return declaration
    }

    fun newTypeParameter(
        moduleData: CfirModuleData,
        name: String,
        bounds: List<CfirTypeRef> = emptyList(),
    ): CfirTypeParameter {
        val symbol = CfirTypeParameterSymbol()
        val typeParameter = CfirTypeParameterImpl(
            source = null,
            moduleData = moduleData,
            resolvePhase = org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.BODY_RESOLVE,
            annotations = emptyList(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            containingDeclarationSymbol = symbol,
            symbol = symbol,
            name = Name.identifier(name),
            bounds = bounds.toMutableList(),
        )
        typeParameter.initDefaultResolveState()
        symbol.bind(typeParameter)
        return typeParameter
    }

    fun newFile(
        moduleData: CfirModuleData,
        packageFqName: FqName,
        declarations: List<CfirDeclaration>,
        fileName: String? = null,
    ): CfirFile {
        val symbol = CfirFileSymbol()
        val file = CfirFileImpl(
            source = null,
            moduleData = moduleData,
            annotations = emptyList(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Source,
            attributes = CfirDeclarationAttributes.EMPTY,
            name = fileName ?: "test_${packageFqName.asString().replace('.', '_')}.cj",
            sourceFile = null,
            packageDirective = CfirPackageDirectiveImpl(null, packageFqName, false),
            imports = emptyList(),
            declarations = declarations,
        )
        file.initDefaultResolveState()
        symbol.bind(file)
        return file
    }

    fun newClass(
        moduleData: CfirModuleData,
        name: String,
        superTypeRefs: List<CfirTypeRef> = emptyList(),
        declarations: List<CfirDeclaration> = emptyList(),
    ): CfirClassImpl {
        val symbol = CfirClassSymbol()
        val klass = CfirClassImpl(
            source = null,
            moduleData = moduleData,
            resolvePhase = org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.BODY_RESOLVE,
            annotations = emptyList<CfirAnnotation>().toMutableList(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            status = CfirDeclarationStatusImpl(),
            typeParameters = mutableListOf(),
            symbol = symbol,
            superTypeRefs = superTypeRefs.toMutableList(),
            declarations = declarations.toMutableList(),
            name = Name.identifier(name),
        )
        klass.initDefaultResolveState()
        return klass
    }

    fun newInterface(
        moduleData: CfirModuleData,
        name: String,
        superTypeRefs: List<CfirTypeRef> = emptyList(),
        declarations: List<CfirDeclaration> = emptyList(),
    ): CfirInterface {
        val symbol = CfirInterfaceSymbol()
        val interfaceDeclaration = CfirInterfaceImpl(
            source = null,
            moduleData = moduleData,
            resolvePhase = org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.BODY_RESOLVE,
            annotations = emptyList<CfirAnnotation>().toMutableList(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            isLocal = false,
            declarations = declarations.toMutableList(),
            status = CfirDeclarationStatusImpl(),
            typeParameters = mutableListOf(),
            symbol = symbol,
            superTypeRefs = superTypeRefs.toMutableList(),
            name = Name.identifier(name),
        )
        interfaceDeclaration.initDefaultResolveState()
        return interfaceDeclaration
    }
}
