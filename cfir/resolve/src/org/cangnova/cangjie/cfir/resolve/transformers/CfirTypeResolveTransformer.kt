/*
 * Copyright 2010-2026. cangjie.
 */

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildConstructor
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionConfiguration
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.SpecialNames

/**
 * TYPES 阶段处理器。
 * 它负责把声明头中的显式类型引用解析成 `CfirResolvedTypeRef`。
 */
class CfirTypeResolveProcessor(
    session: CfirSession,
    scopeSession: ScopeSession,
) : CfirTransformerBasedResolveProcessor(session, scopeSession, CfirResolvePhase.TYPES) {
    private val typeResolveTransformer = CfirTypeResolveTransformer(session)

    @Suppress("UNCHECKED_CAST")
    override val transformer get() = typeResolveTransformer as org.cangnova.cangjie.cfir.visitors.CfirTransformer<Nothing?>

    override fun processFile(file: CfirFile) {
        typeResolveTransformer.transformFile(file, CfirTypeResolutionConfiguration.EMPTY)
    }
}

/**
 * TYPES 阶段转换器。
 * 它遍历 CFIR 树，只处理声明头中的类型引用，不触碰函数体和隐式类型。
 */
class CfirTypeResolveTransformer(
    override val session: CfirSession,
) : CfirAbstractTreeTransformer<CfirTypeResolutionConfiguration>(CfirResolvePhase.TYPES) {

    private val typeResolverTransformer = CfirSpecificTypeResolverTransformer(session)

    // ---- 声明遍历 ----

    override fun transformFile(file: CfirFile, data: CfirTypeResolutionConfiguration): CfirFile {
        checkSessionConsistency(file)
        val configuration = data.withUseSiteFile(file).withTopContainer(file)
        file.transformDeclarations(this, configuration)
        return file
    }

    override fun transformClass(klass: CfirClass, data: CfirTypeResolutionConfiguration): CfirClass {
        ensureImplicitDefaultConstructorIfNeeded(klass)

        transformClassLikeHeader(klass, data)
        bumpPhase(klass)
        return klass
    }

    override fun transformInterface(interfaceDeclaration: CfirInterface, data: CfirTypeResolutionConfiguration): CfirInterface {
        transformClassLikeHeader(interfaceDeclaration, data)
        bumpPhase(interfaceDeclaration)
        return interfaceDeclaration
    }

    override fun transformStruct(struct: CfirStruct, data: CfirTypeResolutionConfiguration): CfirStruct {
        transformClassLikeHeader(struct, data)
        bumpPhase(struct)
        return struct
    }

    override fun transformEnum(enum: CfirEnum, data: CfirTypeResolutionConfiguration): CfirEnum {
        transformClassLikeHeader(enum, data)
        bumpPhase(enum)
        return enum
    }

    private fun transformClassLikeHeader(
        declaration: CfirClassLikeDeclaration,
        data: CfirTypeResolutionConfiguration,
    ) {
        val typeParameters = when (declaration) {
            is CfirClass -> declaration.typeParameters
            is CfirInterface -> declaration.typeParameters
            is CfirStruct -> declaration.typeParameters
            is CfirEnum -> declaration.typeParameters
            else -> emptyList()
        }
        val configuration = data
            .withTopContainer(declaration)
            .withAdditionalTypeParameters(typeParameters)
        declaration.transformTypeParameters(this, configuration)
        declaration.transformSuperTypeRefs(this, configuration)
        declaration.transformDeclarations(this, configuration)
    }

    override fun transformExtend(extend: CfirExtend, data: CfirTypeResolutionConfiguration): CfirExtend {
        val configuration = data
            .withTopContainer(extend)
            .withAdditionalTypeParameters(extend.typeParameters)
        extend.transformTypeParameters(this, configuration)
        extend.transformExtendedTypeRef(this, configuration)
        extend.transformSuperTypeRefs(this, configuration)
        extend.transformDeclarations(this, configuration)
        bumpPhase(extend)
        return extend
    }

    override fun transformFunction(function: CfirFunction, data: CfirTypeResolutionConfiguration): CfirFunction {
        val configuration = data
            .withTopContainer(function)
            .withAdditionalTypeParameters(function.typeParameters)
        function.transformTypeParameters(this, configuration)
        function.transformReturnTypeRef(this, configuration)
        function.transformValueParameters(this, configuration)
        // TYPES 阶段不解析函数体
        bumpPhase(function)
        return function
    }

    override fun transformConstructor(constructor: CfirConstructor, data: CfirTypeResolutionConfiguration): CfirConstructor {
        val configuration = data
            .withTopContainer(constructor)
            .withAdditionalTypeParameters(constructor.typeParameters)
        constructor.transformTypeParameters(this, configuration)
        if (constructor.returnTypeRef is CfirImplicitTypeRef) {
            val ownerClass = data.topContainer as? CfirClassLikeDeclaration
            val ownerType = ownerClass?.let(::buildConstructedTypeForConstructorOwner)
                ?: ConeErrorType(ConeSimpleDiagnostic("cannot resolve constructor owner type"))
            constructor.replaceReturnTypeRef(
                constructor.returnTypeRef.resolvedTypeFromPrototype(ownerType, constructor.returnTypeRef.source),
            )
        } else {
            constructor.transformReturnTypeRef(this, configuration)
        }
        constructor.transformValueParameters(this, configuration)
        bumpPhase(constructor)
        return constructor
    }

    override fun transformProperty(property: CfirProperty, data: CfirTypeResolutionConfiguration): CfirProperty {
        val configuration = data
            .withTopContainer(property)
            .withAdditionalTypeParameters(property.typeParameters)
        property.transformTypeParameters(this, configuration)
        property.transformReturnTypeRef(this, configuration)
        bumpPhase(property)
        return property
    }

    override fun transformVariable(variable: CfirVariable, data: CfirTypeResolutionConfiguration): CfirVariable {
        bumpPhase(variable)
        return variable
    }

    override fun transformFieldVariable(
        fieldVariable: CfirFieldVariable,
        data: CfirTypeResolutionConfiguration,
    ): CfirFieldVariable {
        fieldVariable.transformReturnTypeRef(this, data)
        bumpPhase(fieldVariable)
        return fieldVariable
    }

    override fun transformPatternBindingVariable(
        patternBindingVariable: CfirPatternBindingVariable,
        data: CfirTypeResolutionConfiguration,
    ): CfirPatternBindingVariable {
        patternBindingVariable.transformReturnTypeRef(this, data)
        bumpPhase(patternBindingVariable)
        return patternBindingVariable
    }

    override fun transformPatternVariable(patternVariable: CfirPatternVariable, data: CfirTypeResolutionConfiguration): CfirPatternVariable {
        patternVariable.transformReturnTypeRef(this, data)
        bumpPhase(patternVariable)
        return patternVariable
    }

    override fun transformValueParameter(valueParameter: CfirValueParameter, data: CfirTypeResolutionConfiguration): CfirValueParameter {
        valueParameter.transformReturnTypeRef(this, data)
        return valueParameter
    }

    override fun transformTypeParameter(typeParameter: CfirTypeParameter, data: CfirTypeResolutionConfiguration): CfirTypeParameter {
        typeParameter.transformBounds(this, data)
        return typeParameter
    }

    override fun transformTypeAlias(typeAlias: CfirTypeAlias, data: CfirTypeResolutionConfiguration): CfirTypeAlias {
        val configuration = data
            .withTopContainer(typeAlias)
            .withAdditionalTypeParameters(typeAlias.typeParameters)
        typeAlias.transformTypeParameters(this, configuration)
        typeAlias.transformExpandedTypeRef(this, configuration)
        bumpPhase(typeAlias)
        return typeAlias
    }

    // ---- 类型解析 ----

    override fun transformTypeRef(typeRef: CfirTypeRef, data: CfirTypeResolutionConfiguration): CfirTypeRef {
        return typeResolverTransformer.transformTypeRef(typeRef, data)
    }

    override fun transformUserTypeRef(userTypeRef: CfirUserTypeRef, data: CfirTypeResolutionConfiguration): CfirTypeRef {
        return transformTypeRef(userTypeRef, data)
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: CfirTypeResolutionConfiguration): CfirTypeRef {
        return resolvedTypeRef
    }

    override fun transformImplicitTypeRef(implicitTypeRef: CfirImplicitTypeRef, data: CfirTypeResolutionConfiguration): CfirTypeRef {
        // 隐式类型留给 IMPLICIT_TYPES 阶段处理
        return implicitTypeRef
    }

    override fun transformBasicTypeRef(basicTypeRef: CfirBasicTypeRef, data: CfirTypeResolutionConfiguration): CfirTypeRef {
        // 基础类型也统一走类型引用解析逻辑
        return transformTypeRef(basicTypeRef, data)
    }

    // ---- 跳过函数体 ----

    override fun transformBlock(block: CfirBlock, data: CfirTypeResolutionConfiguration): CfirExpression {
        return block
    }

    /**
     * 推进声明的 resolve phase。
     */
    private fun bumpPhase(declaration: CfirDeclaration) {
        declaration.replaceResolvePhase(CfirResolvePhase.TYPES)
    }

    private fun ensureImplicitDefaultConstructorIfNeeded(klass: CfirClass) {
        if (klass.declarations.any { it is CfirConstructor }) return

        val classImpl = klass as? CfirClassImpl ?: return
        val symbol = CfirConstructorSymbol(CallableId(SpecialNames.INIT))
        val constructor = buildConstructor {
            source = klass.source
            moduleData = klass.moduleData
            this.symbol = symbol
            origin = CfirDeclarationOrigin.ImplicitDefault
            attributes = CfirDeclarationAttributes.EMPTY
            status = klass.status
            returnTypeRef = buildImplicitTypeRef {}
            body = null
        }

        classImpl.declarations += constructor
    }

    /**
     * 构造器返回类型在仓颉 CFIR 中直接建模为 `returnTypeRef`。
     * 它不依赖 body 推断，TYPES 阶段就应回填为所属 class-like 的构造后类型，
     * 否则 low-level 的 IMPLICIT_TYPES 校验会把 constructor 误判为未完成。
     */
    private fun buildConstructedTypeForConstructorOwner(owner: CfirClassLikeDeclaration): ConeCangJieType {
        val ownerSymbol = owner.symbol as? CfirClassLikeSymbol<*>
            ?: return ConeErrorType(ConeSimpleDiagnostic("constructor owner has no class-like symbol"))
        val typeArguments = owner.typeParameters.map { parameter ->
            ConeTypeProjection(ConeTypeParameterTypeImpl(parameter.symbol.toLookupTag()))
        }
        return ownerSymbol.constructType(typeArguments)
    }
}
