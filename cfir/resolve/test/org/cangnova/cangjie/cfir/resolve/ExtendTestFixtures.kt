@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.initDefaultResolveState
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirExtendImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirFileImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirInterfaceImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirPackageDirectiveImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirTypeParameterImpl
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.toMutableOrEmpty
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms

/**
 * extend 相关测试工具：CFIR 声明与 session 构造工具。
 */
internal object ExtendTestFixtures {
    /**
     * extend 测试使用的最小 source session。
     */
    class TestSession : CfirSession(Kind.Source) {
        /**
         * 稳定的调试名称。
         */
        override fun toString(): String = "ExtendTestSession"
    }

    /**
     * 构造带 module data 的测试 session。
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
            annotations = emptyList<CfirAnnotation>().toMutableOrEmpty(),
            customRenderer = false,
            coneType = ConeClassLikeType(
                lookupTag = ConeClassLikeLookupTagImpl(classId),
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
            annotations = emptyList<CfirAnnotation>().toMutableOrEmpty(),
            customRenderer = false,
            coneType = ConeClassLikeType(
                lookupTag = ConeClassLikeLookupTagImpl(classId),
                typeArguments = typeArguments,
                isInterface = isInterface,
            ),
            delegatedTypeRef = null,
        )
    }

    /**
     * 构造绑定了具名类型参数符号的类型参数类型。
     */
    fun typeParameterType(moduleData: CfirModuleData, name: String): ConeTypeParameterType {
        val symbol = CfirTypeParameterSymbol()
        val declaration = CfirTypeParameterImpl(
            source = null,
            moduleData = moduleData,
            resolvePhase = org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            containingDeclarationSymbol = symbol,
            symbol = symbol,
            name = Name.identifier(name),
            bounds = mutableListOf(),
        )
        symbol.bind(declaration)
        return ConeTypeParameterTypeImpl(lookupTag = symbol.toLookupTag(), attributes = ConeAttributes.Empty)
    }

    /**
     * 构造引用已绑定类型参数符号的类型参数类型。
     *
     * 语义 key 归一化按声明身份替换类型参数，因此测试的类型实参必须与
     * extend 声明自身的 [CfirTypeParameter] 使用同一个符号，否则归一化无法命中。
     */
    fun typeParameterType(typeParameter: CfirTypeParameter): ConeTypeParameterType =
        ConeTypeParameterTypeImpl(
            lookupTag = typeParameter.symbol.toLookupTag(),
            attributes = ConeAttributes.Empty,
        )

    /**
     * 构造并绑定参数化的 extend 声明。
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
            resolvePhase = org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.BODY_RESOLVE,
            annotations = emptyList<CfirAnnotation>().toMutableOrEmpty(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            status = CfirDeclarationStatusImpl(),
            typeParameters = typeParameters.toMutableList(),
            extendedTypeRef = extendedTypeRef,
            superTypeRefs = superTypeRefs.toMutableList(),
            declarations = declarations.toMutableList(),
        )
        declaration.initDefaultResolveState()
        symbol.bind(declaration)
        return declaration
    }

    /**
     * 构造并绑定参数化的类型参数声明。
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
            annotations = emptyList<CfirAnnotation>().toMutableOrEmpty(),
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
     * 构造带指定声明列表的测试 CFIR 文件。
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
            resolvePhase = org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.BODY_RESOLVE,
            annotations = emptyList<CfirAnnotation>().toMutableOrEmpty(),
            symbol = symbol,
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            name = fileName ?: "test_${packageFqName.asString().replace('.', '_')}.cj",
            sourceFile = null,
            packageDirective = CfirPackageDirectiveImpl(null, packageFqName, false),
            imports = mutableListOf(),
            sourceFileLinesMapping = null,
            declarations = declarations.toMutableList(),
        )
        file.initDefaultResolveState()
        symbol.bind(file)
        return file
    }

    /**
     * 构造参数化的 class 声明。
     */
    fun newClass(
        moduleData: CfirModuleData,
        name: String,
        classId: ClassId,
        superTypeRefs: List<CfirTypeRef> = emptyList(),
        declarations: List<CfirDeclaration> = emptyList(),
    ): CfirClassImpl {
        val symbol = CfirClassSymbol(classId)
        val klass = CfirClassImpl(
            source = null,
            moduleData = moduleData,
            resolvePhase = org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.BODY_RESOLVE,
            annotations = emptyList<CfirAnnotation>().toMutableOrEmpty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = EmptyDeprecationsProvider,
            scopeProvider = CfirCangJieScopeProvider(),
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
     * 构造参数化的 interface 声明。
     */
    fun newInterface(
        moduleData: CfirModuleData,
        name: String,
        classId: ClassId,
        superTypeRefs: List<CfirTypeRef> = emptyList(),
        declarations: List<CfirDeclaration> = emptyList(),
    ): CfirInterface {
        val symbol = CfirInterfaceSymbol(classId)
        val interfaceDeclaration = CfirInterfaceImpl(
            source = null,
            moduleData = moduleData,
            resolvePhase = org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.BODY_RESOLVE,
            annotations = emptyList<CfirAnnotation>().toMutableOrEmpty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            deprecationsProvider = EmptyDeprecationsProvider,
            scopeProvider = CfirCangJieScopeProvider(),
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