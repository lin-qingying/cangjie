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
import org.cangnova.cangjie.platform.CangJiePlatforms

/**
 * extend 相关测试共享的 CFIR 声明与 session 构造工具。
 */
internal object ExtendTestFixtures {
    /**
     * extend 测试使用的最小 source session。
     */
    class TestSession : CfirSession(Kind.Source) {
        /**
         * 返回稳定的调试名称。
         */
        override fun toString(): String = "ExtendTestSession"
    }

    /**
     * 创建绑定了 module data 的测试 session。
     */
    fun newSessionAndModule(moduleName: String = "extend-test"): Pair<TestSession, CfirModuleData> {
        val session = TestSession()
        val moduleData = CfirSourceModuleData(
            name = Name.identifier(moduleName),
            dependencies = emptyList(),
            refinementDependencies = emptyList(),
            targetPlatform = CangJiePlatforms.defaultCangJiePlatform,
            platform = CfirPlatform.DEFAULT,
        )
        moduleData.bindSession(session)
        session.register(CfirModuleData::class, moduleData)
        return session to moduleData
    }

    /**
     * 构造无类型实参的 class-like resolved type ref。
     */
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

    /**
     * 构造带类型实参的 class-like resolved type ref。
     */
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

    /**
     * 构造类型参数类型。
     */
    fun typeParameterType(name: String): ConeTypeParameterType {
        return ConeTypeParameterType(ConeTypeParameterLookupTag(name))
    }

    /**
     * 构造并绑定测试用 extend 声明。
     */
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

    /**
     * 构造并绑定测试用类型参数声明。
     */
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

    /**
     * 构造包含给定声明的测试 CFIR 文件。
     */
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

    /**
     * 构造测试 class 声明。
     */
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

    /**
     * 构造测试 interface 声明。
     */
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
