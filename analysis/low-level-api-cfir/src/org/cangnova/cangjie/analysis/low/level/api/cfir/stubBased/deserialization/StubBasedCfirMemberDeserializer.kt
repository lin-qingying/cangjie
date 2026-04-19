/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.*
import org.cangnova.cangjie.descriptors.EffectiveVisibility
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.declarations.impl.*
import org.cangnova.cangjie.cfir.declarations.utils.sourceElement
import org.cangnova.cangjie.cfir.deserialization.toLazyEffectiveVisibility
import org.cangnova.cangjie.cfir.expressions.builder.buildExpressionStub
import org.cangnova.cangjie.cfir.resolve.defaultType
import org.cangnova.cangjie.cfir.resolve.transformers.setLazyPublishedVisibility
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.impl.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.impl.ConeClassLikeTypeImpl
import org.cangnova.cangjie.cfir.types.impl.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.impl.CfirImplicitUnitTypeRef
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirSymbolEntry
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.*
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

internal class StubBasedCfirDeserializationContext(
    val moduleData: CfirModuleData,
    val packageFqName: FqName,
    val relativeClassName: FqName?,
    val typeDeserializer: StubBasedCfirTypeDeserializer,
    val annotationDeserializer: StubBasedAnnotationDeserializer,
    val containerSource: DeserializedContainerSource?,
    val outerClassSymbol: CfirRegularClassSymbol?,
    val outerTypeParameters: List<CfirTypeParameterSymbol>,
    val initialOrigin: CfirDeclarationOrigin,
    val classLikeDeclaration: CjClassLikeDeclaration? = null,
) {
    val session: CfirSession get() = moduleData.session

    val allTypeParameters: List<CfirTypeParameterSymbol> =
        typeDeserializer.ownTypeParameters + outerTypeParameters

    fun childContext(
        owner: CjTypeParameterListOwner,
        relativeClassName: FqName? = this.relativeClassName,
        containerSource: DeserializedContainerSource? = this.containerSource,
        outerClassSymbol: CfirRegularClassSymbol? = this.outerClassSymbol,
        annotationDeserializer: StubBasedAnnotationDeserializer = this.annotationDeserializer,
        capturesTypeParameters: Boolean = true,
        containingDeclarationSymbol: CfirBasedSymbol<*>? = outerClassSymbol,
    ): StubBasedCfirDeserializationContext = StubBasedCfirDeserializationContext(
        moduleData = moduleData,
        packageFqName = packageFqName,
        relativeClassName = relativeClassName,
        typeDeserializer = StubBasedCfirTypeDeserializer(
            moduleData,
            annotationDeserializer,
            typeDeserializer,
            containingDeclarationSymbol,
            owner,
            initialOrigin
        ),
        annotationDeserializer = annotationDeserializer,
        containerSource = containerSource,
        outerClassSymbol = outerClassSymbol,
        outerTypeParameters = if (capturesTypeParameters) allTypeParameters else emptyList(),
        initialOrigin = initialOrigin
    )

    fun withClassLikeDeclaration(
        classLikeDeclaration: CjClassLikeDeclaration,
    ): StubBasedCfirDeserializationContext = StubBasedCfirDeserializationContext(
        moduleData = moduleData,
        packageFqName = packageFqName,
        relativeClassName = relativeClassName,
        typeDeserializer = typeDeserializer,
        annotationDeserializer = annotationDeserializer,
        containerSource = containerSource,
        outerClassSymbol = outerClassSymbol,
        outerTypeParameters = outerTypeParameters,
        initialOrigin = initialOrigin,
        classLikeDeclaration = classLikeDeclaration,
    )

    val memberDeserializer: StubBasedCfirMemberDeserializer = StubBasedCfirMemberDeserializer(this, initialOrigin)
    val dispatchReceiver = relativeClassName?.let { ClassId(packageFqName, it, isLocal = false).defaultType(allTypeParameters) }

    companion object {

        fun createForClass(
            classId: ClassId,
            classOrObject: CjTypeStatement,
            moduleData: CfirModuleData,
            annotationDeserializer: StubBasedAnnotationDeserializer,
            containerSource: DeserializedContainerSource?,
            outerClassSymbol: CfirRegularClassSymbol,
            initialOrigin: CfirDeclarationOrigin,
        ): StubBasedCfirDeserializationContext = createRootContext(
            moduleData,
            annotationDeserializer,
            classId.packageFqName,
            classId.relativeClassName,
            classOrObject,
            containerSource,
            outerClassSymbol,
            outerClassSymbol,
            initialOrigin
        )

        fun createRootContext(
            moduleData: CfirModuleData,
            annotationDeserializer: StubBasedAnnotationDeserializer,
            packageFqName: FqName,
            relativeClassName: FqName?,
            owner: CjTypeParameterListOwner,
            containerSource: DeserializedContainerSource?,
            outerClassSymbol: CfirRegularClassSymbol?,
            containingDeclarationSymbol: CfirBasedSymbol<*>?,
            initialOrigin: CfirDeclarationOrigin,
        ): StubBasedCfirDeserializationContext = StubBasedCfirDeserializationContext(
            moduleData,
            packageFqName,
            relativeClassName,
            StubBasedCfirTypeDeserializer(
                moduleData,
                annotationDeserializer,
                parent = null,
                containingDeclarationSymbol,
                owner,
                initialOrigin
            ),
            annotationDeserializer,
            containerSource,
            outerClassSymbol,
            outerTypeParameters = emptyList(),
            initialOrigin
        )

        fun createRootContext(
            session: CfirSession,
            moduleData: CfirModuleData,
            callableId: CallableId,
            parameterListOwner: CjTypeParameterListOwner,
            symbol: CfirBasedSymbol<*>,
            initialOrigin: CfirDeclarationOrigin,
            containerSource: DeserializedContainerSource?,
        ): StubBasedCfirDeserializationContext {
            return createRootContext(
                moduleData,
                StubBasedAnnotationDeserializer(session),
                callableId.packageName,
                callableId.className,
                parameterListOwner,
                containerSource = containerSource,
                outerClassSymbol = null,
                symbol,
                initialOrigin
            )
        }
    }
}

internal class StubBasedCfirMemberDeserializer(
    private val c: StubBasedCfirDeserializationContext,
    private val initialOrigin: CfirDeclarationOrigin,
) {

    fun loadTypeAlias(typeAlias: CjTypeAlias, aliasSymbol: CfirTypeAliasSymbol, scopeProvider: CfirScopeProvider): CfirTypeAlias {
        val name = typeAlias.nameAsSafeName
        val local = c.childContext(typeAlias, containingDeclarationSymbol = aliasSymbol)
        return buildTypeAlias {
            source = CjRealPsiSourceElement(typeAlias)
            moduleData = c.moduleData
            origin = initialOrigin
            this.scopeProvider = scopeProvider
            this.name = name
            val visibility = typeAlias.visibility
            status = CfirResolvedDeclarationStatusWithLazyEffectiveVisibility(
                visibility,
                Modality.FINAL,
                visibility.toLazyEffectiveVisibility(owner = null)
            ).apply {
            }

            annotations += c.annotationDeserializer.loadAnnotations(typeAlias)
            symbol = aliasSymbol
            expandedTypeRef = typeAlias.getTypeReference()?.toTypeRef(local)
                ?: errorWithAttachment("Type alias doesn't have type reference") {
                    withPsiEntry("property", typeAlias)
                }
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
            deprecationsProvider = annotations.getDeprecationsProviderFromAnnotations(c.session, fromJava = false)
        }.apply {
            sourceElement = c.containerSource
        }
    }

    private fun loadPropertyGetter(
        getter: CjPropertyAccessor?,
        classSymbol: CfirClassSymbol<*>?,
        returnTypeRef: CfirTypeRef,
        propertySymbol: CfirPropertySymbol,
    ): CfirPropertyAccessor? {
        val accessor = if (getter != null) {
            buildPropertyAccessor {
                source = CjRealPsiSourceElement(getter)
                moduleData = c.moduleData
                origin = initialOrigin
                this.returnTypeRef = returnTypeRef
                resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
                isGetter = true
                status = CfirResolvedDeclarationStatusWithLazyEffectiveVisibility(
                    getter.visibility,
                    getter.modality,
                    getter.visibility.toLazyEffectiveVisibility(classSymbol),
                )
                this.symbol = CfirPropertyAccessorSymbol()
                dispatchReceiverType = c.dispatchReceiver
                this.propertySymbol = propertySymbol
            }
        } else {
            return null
        }

        return accessor.apply {
            if (getter != null) {
                replaceAnnotations(c.annotationDeserializer.loadAnnotations(getter))
            }

            replaceDeprecationsProvider(getDeprecationsProvider(c.session))
            containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
        }
    }

    private fun loadPropertySetter(
        setter: CjPropertyAccessor?,
        classSymbol: CfirClassSymbol<*>?,
        returnTypeRef: CfirTypeRef,
        propertySymbol: CfirPropertySymbol,
        local: StubBasedCfirDeserializationContext,
    ): CfirPropertyAccessor? {
        val accessor = if (setter != null) {
            buildPropertyAccessor {
                source = CjRealPsiSourceElement(setter)
                moduleData = c.moduleData
                origin = initialOrigin
                this.returnTypeRef = CfirImplicitUnitTypeRef(source)
                resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
                isGetter = false
                status = CfirResolvedDeclarationStatusWithLazyEffectiveVisibility(
                    setter.visibility,
                    setter.modality,
                    setter.visibility.toLazyEffectiveVisibility(classSymbol),
                )
                this.symbol = CfirPropertyAccessorSymbol()
                dispatchReceiverType = c.dispatchReceiver
                valueParameters += local.memberDeserializer.valueParameters(
                    setter.valueParameters,
                    symbol
                )

                this.propertySymbol = propertySymbol
            }
        } else {
            return null
        }

        return accessor.apply {
            if (setter != null) {
                replaceAnnotations(c.annotationDeserializer.loadAnnotations(setter))
            }

            replaceDeprecationsProvider(getDeprecationsProvider(c.session))
            containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
        }
    }

    fun loadProperty(
        property: CjProperty,
        classSymbol: CfirClassSymbol<*>? = null,
        existingSymbol: CfirPropertySymbol? = null,
    ): CfirProperty {
        val callableName = property.nameAsSafeName
        val callableId = CallableId(c.packageFqName, c.relativeClassName, callableName)
        val symbol = existingSymbol ?: CfirRegularPropertySymbol(callableId)
        val local = c.childContext(property, containingDeclarationSymbol = symbol)

        val returnTypeRef = property.typeReference?.toTypeRef(local)
            ?: errorWithAttachment("Property doesn't have type reference") {
                withPsiEntry("property", property)
            }

        val propertyModality = property.modality

        val isVar = property.isVar
        return buildProperty {
            source = CjRealPsiSourceElement(property)
            moduleData = c.moduleData
            origin = initialOrigin
            this.returnTypeRef = returnTypeRef
            name = callableName
            this.isVar = isVar
            this.symbol = symbol
            dispatchReceiverType = c.dispatchReceiver
            val visibility = property.visibility
            val resolvedStatus = CfirResolvedDeclarationStatusWithLazyEffectiveVisibility(
                visibility,
                propertyModality,
                visibility.toLazyEffectiveVisibility(classSymbol)
            ).apply {
                isOverride = false
                isConst = property.hasModifier(CjTokens.CONST_KEYWORD)
            }

            status = resolvedStatus
            isLocal = false

            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
            val allAnnotations = c.annotationDeserializer.loadAnnotations(property)
            annotations += allAnnotations.filter { it.useSiteTarget == null }

            this.getter = loadPropertyGetter(
                getter = property.getter,
                classSymbol = classSymbol,
                returnTypeRef = returnTypeRef,
                propertySymbol = symbol,
            )

            val setter = property.setter
            this.setter = if (setter != null) {
                loadPropertySetter(
                    setter = setter,
                    classSymbol = classSymbol,
                    returnTypeRef = returnTypeRef,
                    propertySymbol = symbol,
                    local = local,
                )
            } else {
                null
            }

            this.containerSource = c.containerSource
            this.initializer = c.annotationDeserializer.loadConstant(
                property,
                isUnsigned = returnTypeRef.coneType.isUnsignedType,
            )

        }.apply {
            setLazyPublishedVisibility(c.session)

            this.getter?.setLazyPublishedVisibility(annotations, this, c.session)
            this.setter?.setLazyPublishedVisibility(annotations, this, c.session)

            replaceDeprecationsProvider(getDeprecationsProvider(c.session))
        }
    }

    fun loadFunction(
        function: CjNamedFunction,
        classSymbol: CfirClassSymbol<*>? = null,
        session: CfirSession,
        existingSymbol: CfirNamedFunctionSymbol? = null,
    ): CfirNamedFunction {
        val callableName = function.nameAsSafeName
        val callableId = CallableId(c.packageFqName, c.relativeClassName, callableName)
        val symbol = existingSymbol ?: CfirNamedFunctionSymbol(callableId)
        val local = c.childContext(function, containingDeclarationSymbol = symbol)

        val simpleFunction = buildNamedFunction {
            moduleData = c.moduleData
            origin = initialOrigin
            source = CjRealPsiSourceElement(function)
            returnTypeRef = function.typeReference?.toTypeRef(local) ?: session.builtinTypes.unitType
            name = callableName
            val visibility = function.visibility
            status = CfirResolvedDeclarationStatusWithLazyEffectiveVisibility(
                visibility,
                function.modality,
                visibility.toLazyEffectiveVisibility(classSymbol)
            ).apply {
                isOverride = false
                isOperator = function.hasModifier(CjTokens.OPERATOR_KEYWORD)
            }
            isLocal = false
            this.symbol = symbol
            dispatchReceiverType = c.dispatchReceiver
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.fir }
            valueParameters += local.memberDeserializer.valueParameters(
                function.valueParameters,
                symbol
            )
            annotations += c.annotationDeserializer.loadAnnotations(function)
            deprecationsProvider = annotations.getDeprecationsProviderFromAnnotations(c.session, fromJava = false)
            this.containerSource = c.containerSource

        }.apply {
            setLazyPublishedVisibility(c.session)
        }

        return simpleFunction
    }

    @OptIn(SuspiciousFakeSourceCheck::class)
    fun loadConstructor(
        constructor: CjConstructor<*>,
        classOrObject: CjTypeStatement,
        classBuilder: CfirRegularClassBuilder,
    ): CfirConstructor {
        val relativeClassName = c.relativeClassName!!
        val callableId = CallableId(c.packageFqName, relativeClassName, relativeClassName.shortName())
        val symbol = CfirConstructorSymbol(callableId)
        val local = c.childContext(constructor, containingDeclarationSymbol = symbol)
        val isPrimary = constructor is CjPrimaryConstructor

        val typeParameters = classBuilder.typeParameters

        val delegatedSelfType = buildResolvedTypeRef {
            coneType = ConeClassLikeTypeImpl(
                classBuilder.symbol.toLookupTag(),
                typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }.toTypedArray(),
                false
            )
            source = CjFakePsiSourceElement(classOrObject, CjFakeSourceElementKind.ClassSelfTypeRef)
        }

        return if (isPrimary) {
            CfirPrimaryConstructorBuilder()
        } else {
            CfirConstructorBuilder()
        }.apply {
            moduleData = c.moduleData
            source = CjRealPsiSourceElement(constructor)
            origin = initialOrigin
            returnTypeRef = delegatedSelfType
            val visibility = constructor.visibility
            status = CfirResolvedDeclarationStatusWithLazyEffectiveVisibility(
                visibility,
                Modality.FINAL,
                visibility.toLazyEffectiveVisibility(classBuilder.symbol)
            ).apply {
                isOverride = false
            }
            isLocal = false
            this.symbol = symbol
            dispatchReceiverType = null
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            this.typeParameters +=
                typeParameters.filterIsInstance<CfirTypeParameter>()
                    .map { buildConstructedClassTypeParameterRef { this.symbol = it.symbol } }
            valueParameters += local.memberDeserializer.valueParameters(
                constructor.valueParameters,
                symbol,
                forceDefaultValue = classBuilder.symbol.classId == StandardClassIds.Enum
            )
            annotations +=
                c.annotationDeserializer.loadAnnotations(constructor)
            containerSource = c.containerSource
            deprecationsProvider = annotations.getDeprecationsProviderFromAnnotations(c.session, fromJava = false)

        }.build().apply {
            containingClassForStaticMemberAttr = c.dispatchReceiver!!.lookupTag
            setLazyPublishedVisibility(c.session)
        }
    }

    private fun valueParameters(
        valueParameters: List<CjParameter>,
        functionSymbol: CfirFunctionSymbol<*>,
        forceDefaultValue: Boolean = false,
    ): List<CfirValueParameter> = valueParameters.map { parameter ->
        loadValueParameter(
            parameter = parameter,
            containingSymbol = functionSymbol,
            kind = CfirValueParameterKind.Regular,
            forceDefaultValue = forceDefaultValue,
        )
    }

    private fun loadValueParameter(
        parameter: CjParameter,
        containingSymbol: CfirCallableSymbol<*>,
        kind: CfirValueParameterKind,
        forceDefaultValue: Boolean = false,
    ): CfirValueParameter = buildValueParameter {
        valueParameterKind = kind
        source = CjRealPsiSourceElement(parameter)
        moduleData = c.moduleData
        containingDeclarationSymbol = containingSymbol
        origin = initialOrigin
        returnTypeRef = parameter.typeReference?.toTypeRef(c)
            ?: errorWithAttachment("CjParameter doesn't have type") {
                withPsiEntry("parameter", parameter)
                withCfirSymbolEntry("containingSymbol", containingSymbol)
            }

        isVararg = parameter.isVarArg
        if (isVararg) {
            returnTypeRef = returnTypeRef.withReplacedReturnType(returnTypeRef.coneType.createOutArrayType())
        }

        val name = parameter.name
        this.name = if (name == "_") {
            SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
        } else {
            CjPsiUtil.safeName(name)
        }
        symbol = CfirValueParameterSymbol()
        resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
        defaultValue = if (forceDefaultValue || parameter.hasDefaultValue()) {
            buildExpressionStub()
        } else {
            null
        }

        annotations += c.annotationDeserializer.loadAnnotations(parameter)
    }

    private fun CjTypeReference.toTypeRef(context: StubBasedCfirDeserializationContext): CfirTypeRef =
        context.typeDeserializer.typeRef(this)

    fun loadEnumEntry(
        declaration: CjEnumEntry,
        symbol: CfirRegularClassSymbol,
        classId: ClassId,
    ): CfirEnumEntry {
        val enumEntryName = declaration.name
            ?: errorWithAttachment("Enum entry doesn't provide name") {
                withPsiEntry("declaration", declaration)
            }

        val enumType = ConeClassLikeTypeImpl(symbol.toLookupTag(), ConeTypeProjection.EMPTY_ARRAY, false)
        val enumEntry = buildEnumEntry {
            source = CjRealPsiSourceElement(declaration)
            this.moduleData = c.moduleData
            this.origin = initialOrigin
            returnTypeRef = buildResolvedTypeRef { coneType = enumType }
            name = Name.identifier(enumEntryName)
            this.symbol = CfirEnumEntrySymbol(CallableId(classId, name))
            this.status = CfirResolvedDeclarationStatusImpl(
                Visibilities.Public,
                Modality.FINAL,
                EffectiveVisibility.Public
            ).apply {
                isStatic = true
            }
            isLocal = false
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
        }.apply {
            containingClassForStaticMemberAttr = c.dispatchReceiver!!.lookupTag
        }
        return enumEntry
    }

    private fun Visibility.toLazyEffectiveVisibility(owner: CfirClassLikeSymbol<*>?): Lazy<EffectiveVisibility> {
        return this.toLazyEffectiveVisibility(owner, c.session, forClass = false)
    }
}
