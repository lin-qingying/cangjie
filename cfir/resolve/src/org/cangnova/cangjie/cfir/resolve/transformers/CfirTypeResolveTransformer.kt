/*
 * Copyright 2010-2026. cangjie.
 */

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
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
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef

/**
 * TYPES 阶段处理器。
 * 它负责把声明头中的显式类型引用解析成 `CfirResolvedTypeRef`。
 */
class CfirTypeResolveProcessor(
    session: CfirSession,
    scopeSession: CfirScopeSession,
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

        val configuration = data
            .withTopContainer(klass)
            .withAdditionalTypeParameters(klass.typeParameters)
        klass.transformTypeParameters(this, configuration)
        klass.transformSuperTypeRefs(this, configuration)
        klass.transformDeclarations(this, configuration)
        bumpPhase(klass)
        return klass
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
        constructor.transformReturnTypeRef(this, configuration)
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
        if (klass.classKind == CfirClassKind.INTERFACE) return
        if (klass.declarations.any { it is CfirConstructor }) return

        val classImpl = klass as? CfirClassImpl ?: return
        val symbol = CfirConstructorSymbol()
        val constructor = buildConstructor {
            source = klass.source
            moduleData = klass.moduleData
            this.symbol = symbol
            origin = CfirDeclarationOrigin.ImplicitDefault
            attributes = CfirDeclarationAttributes.EMPTY
            status = klass.status
            returnTypeRef = buildImplicitTypeRef()
            body = null
        }
        symbol.bind(constructor)
        classImpl.declarations = classImpl.declarations + constructor
    }
}


