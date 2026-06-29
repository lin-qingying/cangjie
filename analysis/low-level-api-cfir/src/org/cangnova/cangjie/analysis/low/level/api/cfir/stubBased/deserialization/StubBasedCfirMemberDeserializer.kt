

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.cfir.containingClassForStaticMemberAttr
import org.cangnova.cangjie.cfir.copyWithNewSource
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirPropertyBodyResolveState
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.builder.buildConstructor
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPrimaryConstructor
import org.cangnova.cangjie.cfir.declarations.builder.buildProperty
import org.cangnova.cangjie.cfir.declarations.builder.buildPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeAlias
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyExpression
import org.cangnova.cangjie.cfir.expressions.withCfirSymbolEntry
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.CjPsiUtil
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeParameterListOwner
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import org.cangnova.cangjie.source.CjFakePsiSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjRealPsiSourceElement
import org.cangnova.cangjie.source.SuspiciousFakeSourceCheck
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/**
 * stub-based CFIR 反序列化过程中共享的上下文。
 */
internal class StubBasedCfirDeserializationContext(
    /**
     * 反序列化声明绑定的 module data。
     */
    val moduleData: org.cangnova.cangjie.cfir.common.CfirModuleData,

    /**
     * 当前容器包名。
     */
    val packageFqName: FqName,

    /**
     * 当前类内上下文的相对类名；顶层 callable 可为 `null`。
     */
    val relativeClassName: FqName?,

    /**
     * 当前上下文使用的类型反序列化器。
     */
    val typeDeserializer: StubBasedCfirTypeDeserializer,

    /**
     * 当前上下文使用的注解反序列化器。
     */
    val annotationDeserializer: StubBasedAnnotationDeserializer,

    /**
     * 当前声明所属的反序列化 container source。
     */
    val containerSource: DeserializedContainerSource?,

    /**
     * 当前外层类 symbol。
     */
    val outerClassSymbol: CfirClassLikeSymbol<*>?,

    /**
     * 外层上下文捕获到的类型参数 symbol。
     */
    val outerTypeParameters: List<CfirTypeParameterSymbol>,

    /**
     * 当前反序列化声明使用的初始 origin。
     */
    val initialOrigin: CfirDeclarationOrigin,

    /**
     * 当前上下文绑定的 class-like PSI 声明。
     */
    val classLikeDeclaration: org.cangnova.cangjie.psi.CjClassLikeDeclaration? = null,
) {
    /**
     * 当前 module data 绑定的 CFIR session。
     */
    val session: CfirSession get() = moduleData.session

    /**
     * 当前上下文可见的全部类型参数 symbol。
     */
    val allTypeParameters: List<CfirTypeParameterSymbol> =
        typeDeserializer.ownTypeParameters + outerTypeParameters

    /**
     * 为 [owner] 创建子反序列化上下文。
     */
    fun childContext(
        owner: CjTypeParameterListOwner,
        relativeClassName: FqName? = this.relativeClassName,
        containerSource: DeserializedContainerSource? = this.containerSource,
        outerClassSymbol: CfirClassLikeSymbol<*>? = this.outerClassSymbol,
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

    /**
     * 返回绑定 [classLikeDeclaration] 的上下文副本。
     */
    fun withClassLikeDeclaration(
        classLikeDeclaration: org.cangnova.cangjie.psi.CjClassLikeDeclaration,
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

    /**
     * 当前上下文的 member deserializer。
     */
    val memberDeserializer: StubBasedCfirMemberDeserializer = StubBasedCfirMemberDeserializer(this, initialOrigin)

    /**
     * 当前类上下文的 dispatch receiver 类型。
     */
    val dispatchReceiver = relativeClassName?.let {
        ConeClassLikeType(
            ClassId(packageFqName, it).toLookupTag(),
            allTypeParameters.map { typeParameter -> ConeTypeParameterTypeImpl(typeParameter.toLookupTag()) },
        )
    }

    companion object {
        fun createForClass(
            classId: ClassId,
            classOrObject: CjTypeStatement,
            moduleData: org.cangnova.cangjie.cfir.common.CfirModuleData,
            annotationDeserializer: StubBasedAnnotationDeserializer,
            containerSource: DeserializedContainerSource?,
            outerClassSymbol: CfirClassLikeSymbol<*>,
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
            moduleData: org.cangnova.cangjie.cfir.common.CfirModuleData,
            annotationDeserializer: StubBasedAnnotationDeserializer,
            packageFqName: FqName,
            relativeClassName: FqName?,
            owner: CjTypeParameterListOwner,
            containerSource: DeserializedContainerSource?,
            outerClassSymbol: CfirClassLikeSymbol<*>?,
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
            moduleData: org.cangnova.cangjie.cfir.common.CfirModuleData,
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

/**
 * 从 compiled PSI/stub 中反序列化 callable、属性、构造函数、类型别名和参数声明。
 */
internal class StubBasedCfirMemberDeserializer(
    /**
     * 当前成员反序列化上下文。
     */
    private val c: StubBasedCfirDeserializationContext,

    /**
     * 反序列化声明使用的初始 origin。
     */
    private val initialOrigin: CfirDeclarationOrigin,
) {
    @Suppress("UNUSED_PARAMETER")
    /**
     * 反序列化类型别名声明。
     */
    fun loadTypeAlias(typeAlias: CjTypeAlias, aliasSymbol: CfirTypeAliasSymbol, scopeProvider: CfirScopeProvider): CfirTypeAlias {
        val name = typeAlias.nameAsSafeName
        val local = c.childContext(typeAlias, containingDeclarationSymbol = aliasSymbol)
        return buildTypeAlias {
            source = CjRealPsiSourceElement(typeAlias)
            moduleData = c.moduleData
            origin = initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            this.scopeProvider = scopeProvider
            this.name = name
            status = buildResolvedStatus(typeAlias.visibility, Modality.FINAL)
            annotations += c.annotationDeserializer.loadAnnotations(typeAlias, aliasSymbol)
            symbol = aliasSymbol
            expandedTypeRef = typeAlias.getTypeReference()?.toTypeRef(local)
                ?: errorWithAttachment("Type alias doesn't have type reference") {
                    withPsiEntry("property", typeAlias)
                }
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.cfir }
            deprecationsProvider = EmptyDeprecationsProvider
        }
    }

    /**
     * 反序列化属性 getter。
     */
    private fun loadPropertyGetter(
        getter: CjPropertyAccessor?,
        classSymbol: CfirClassLikeSymbol<*>?,
        returnTypeRef: CfirTypeRef,
        propertySymbol: CfirPropertySymbol,
    ): CfirPropertyAccessor? {
        if (getter == null) return null
        val accessor = buildPropertyAccessor {
            source = CjRealPsiSourceElement(getter)
            moduleData = c.moduleData
            origin = initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            this.returnTypeRef = returnTypeRef.copyWithNewSource(source!!)
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            isGetter = true
            status = buildResolvedStatus(getter.visibility, getter.modality)
            this.symbol = CfirPropertyAccessorSymbol()
            dispatchReceiverType = c.dispatchReceiver
            this.propertySymbol = propertySymbol
            deprecationsProvider = EmptyDeprecationsProvider
        }
        return accessor.apply {
            replaceAnnotations(c.annotationDeserializer.loadAnnotations(getter, accessor.symbol))
            replaceDeprecationsProvider(EmptyDeprecationsProvider)
            containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
        }
    }

    /**
     * 反序列化属性 setter。
     */
    private fun loadPropertySetter(
        setter: CjPropertyAccessor?,
        classSymbol: CfirClassLikeSymbol<*>?,
        propertySymbol: CfirPropertySymbol,
        local: StubBasedCfirDeserializationContext,
    ): CfirPropertyAccessor? {
        if (setter == null) return null
        val accessor = buildPropertyAccessor {
            source = CjRealPsiSourceElement(setter)
            moduleData = c.moduleData
            origin = initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            this.returnTypeRef = c.session.builtinTypes.unitType.toCfirResolvedTypeRef(source)
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            isGetter = false
            status = buildResolvedStatus(setter.visibility, setter.modality)
            this.symbol = CfirPropertyAccessorSymbol()
            dispatchReceiverType = c.dispatchReceiver
            valueParameters += local.memberDeserializer.valueParameters(setter.valueParameters, symbol)
            this.propertySymbol = propertySymbol
            deprecationsProvider = EmptyDeprecationsProvider
        }
        return accessor.apply {
            replaceAnnotations(c.annotationDeserializer.loadAnnotations(setter, accessor.symbol))
            replaceDeprecationsProvider(EmptyDeprecationsProvider)
            containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
        }
    }

    /**
     * 反序列化属性声明。
     */
    fun loadProperty(
        property: CjProperty,
        classSymbol: CfirClassLikeSymbol<*>? = null,
        existingSymbol: CfirPropertySymbol? = null,
    ): CfirProperty {
        val callableName = property.nameAsSafeName
        val callableId = CallableId(c.packageFqName, c.relativeClassName, callableName)
        val symbol = existingSymbol ?: CfirPropertySymbol(callableId)
        val local = c.childContext(property, containingDeclarationSymbol = symbol)
        val returnTypeRef = property.typeReference?.toTypeRef(local)
            ?: errorWithAttachment("Property doesn't have type reference") {
                withPsiEntry("property", property)
            }

        return buildProperty {
            source = CjRealPsiSourceElement(property)
            moduleData = c.moduleData
            origin = initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            this.returnTypeRef = returnTypeRef
            name = callableName
            this.symbol = symbol
            dispatchReceiverType = c.dispatchReceiver
            status = buildResolvedStatus(property.visibility, property.modality).apply {
                isOverride = false
                isConst = property.hasModifier(CjTokens.CONST_KEYWORD)
                isMut = property.isVar
            }
            isLocal = false
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.cfir }
            val allAnnotations = c.annotationDeserializer.loadAnnotations(property, symbol)
            annotations += allAnnotations
            getter = loadPropertyGetter(property.getter, classSymbol, returnTypeRef, symbol)
            setter = loadPropertySetter(property.setter, classSymbol, symbol, local)
            bodyResolveState = CfirPropertyBodyResolveState.ALL_BODIES_RESOLVED
            deprecationsProvider = EmptyDeprecationsProvider
        }.apply {
            replaceDeprecationsProvider(EmptyDeprecationsProvider)
        }
    }

    /**
     * 反序列化命名函数声明。
     */
    fun loadFunction(
        function: CjNamedFunction,
        classSymbol: CfirClassLikeSymbol<*>? = null,
        session: CfirSession,
        existingSymbol: CfirNamedFunctionSymbol? = null,
    ) = buildNamedFunction {
        val callableName = function.nameAsSafeName
        val callableId = CallableId(c.packageFqName, c.relativeClassName, callableName)
        val symbol = existingSymbol ?: CfirNamedFunctionSymbol(callableId)
        val local = c.childContext(function, containingDeclarationSymbol = symbol)
        moduleData = c.moduleData
        origin = initialOrigin
        source = CjRealPsiSourceElement(function)
        attributes = CfirDeclarationAttributes.EMPTY
        returnTypeRef = function.typeReference?.toTypeRef(local) ?: session.builtinTypes.unitType.toCfirResolvedTypeRef(null)
        name = callableName
        status = buildResolvedStatus(function.visibility, function.modality).apply {
            isOverride = false
            isOperator = function.hasModifier(CjTokens.OPERATOR_KEYWORD)
        }
        isLocal = false
        this.symbol = symbol
        dispatchReceiverType = c.dispatchReceiver
        resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
        typeParameters += local.typeDeserializer.ownTypeParameters.map { it.cfir }
        valueParameters += local.memberDeserializer.valueParameters(function.valueParameters, symbol)
        annotations += c.annotationDeserializer.loadAnnotations(function, symbol)
        deprecationsProvider = EmptyDeprecationsProvider
        isMut = false
    }

    @OptIn(SuspiciousFakeSourceCheck::class)
    /**
     * 反序列化构造函数声明。
     */
    fun loadConstructor(
        constructor: CjConstructor<*>,
        classOrObject: CjTypeStatement,
        classSymbol: CfirClassLikeSymbol<*>,
        typeParameters: List<CfirTypeParameter>,
    ) = if (constructor is CjPrimaryConstructor) {
        buildPrimaryConstructor {
            val relativeClassName = c.relativeClassName!!
            val callableId = CallableId(c.packageFqName, relativeClassName, relativeClassName.shortName())
            val symbol = CfirConstructorSymbol(callableId)
            val local = c.childContext(constructor, containingDeclarationSymbol = symbol)
            moduleData = c.moduleData
            source = CjRealPsiSourceElement(constructor)
            origin = initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            returnTypeRef = ConeClassLikeType(
                classSymbol.toLookupTag(),
                typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag())},
            ).toCfirResolvedTypeRef(CjFakePsiSourceElement(classOrObject, CjFakeSourceElementKind.ClassSelfTypeRef))
            status = buildResolvedStatus(constructor.visibility, Modality.FINAL)
            isLocal = false
            this.symbol = symbol
            dispatchReceiverType = null
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            this.typeParameters += typeParameters
            valueParameters += local.memberDeserializer.valueParameters(constructor.valueParameters, symbol)
            annotations += c.annotationDeserializer.loadAnnotations(constructor, symbol)
            deprecationsProvider = EmptyDeprecationsProvider
        }
    } else {
        buildConstructor {
            val relativeClassName = c.relativeClassName!!
            val callableId = CallableId(c.packageFqName, relativeClassName, relativeClassName.shortName())
            val symbol = CfirConstructorSymbol(callableId)
            val local = c.childContext(constructor, containingDeclarationSymbol = symbol)
            moduleData = c.moduleData
            source = CjRealPsiSourceElement(constructor)
            origin = initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            returnTypeRef = ConeClassLikeType(
                classSymbol.toLookupTag(),
                typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag()) },
            ).toCfirResolvedTypeRef(CjFakePsiSourceElement(classOrObject, CjFakeSourceElementKind.ClassSelfTypeRef))
            status = buildResolvedStatus(constructor.visibility, Modality.FINAL)
            isLocal = false
            this.symbol = symbol
            dispatchReceiverType = null
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            this.typeParameters += typeParameters
            valueParameters += local.memberDeserializer.valueParameters(constructor.valueParameters, symbol)
            annotations += c.annotationDeserializer.loadAnnotations(constructor, symbol)
            deprecationsProvider = EmptyDeprecationsProvider
        }
    }.apply {
        containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
    }

    /**
     * 反序列化函数或构造函数的值参数列表。
     */
    private fun valueParameters(
        valueParameters: List<CjParameter>,
        functionSymbol: CfirFunctionSymbol<*>,
        forceDefaultValue: Boolean = false,
    ): List<CfirValueParameter> = valueParameters.map { parameter ->
        loadValueParameter(parameter, functionSymbol, forceDefaultValue)
    }

    /**
     * 反序列化单个值参数。
     */
    private fun loadValueParameter(
        parameter: CjParameter,
        containingSymbol: CfirCallableSymbol<*>,
        forceDefaultValue: Boolean = false,
    ): CfirValueParameter = buildValueParameter {
        source = CjRealPsiSourceElement(parameter)
        moduleData = c.moduleData
        containingDeclarationSymbol = containingSymbol
        origin = initialOrigin
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = false
        deprecationsProvider = EmptyDeprecationsProvider
        returnTypeRef = parameter.typeReference?.toTypeRef(c)
            ?: errorWithAttachment("CjParameter doesn't have type") {
                withPsiEntry("parameter", parameter)
                withCfirSymbolEntry("containingSymbol", containingSymbol)
            }
        val parameterName = parameter.name
        name = if (parameterName == "_") {
            SpecialNames.UNDERSCORE_FOR_UNUSED_VAR
        } else {
            CjPsiUtil.safeName(parameterName)
        }
        symbol = CfirValueParameterSymbol(CallableId(c.packageFqName, c.relativeClassName, name))
        resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
        isNamed = parameter.isNamed
        status = buildResolvedStatus(Visibilities.Local, Modality.FINAL)
        defaultValue = if (forceDefaultValue || parameter.hasDefaultValue()) buildLazyExpression() else null
        annotations += c.annotationDeserializer.loadAnnotations(parameter, containingSymbol)
    }

    /**
     * 使用 [context] 的类型反序列化器把 PSI 类型引用转换为 CFIR type ref。
     */
    private fun CjTypeReference.toTypeRef(context: StubBasedCfirDeserializationContext): CfirTypeRef =
        context.typeDeserializer.typeRef(this)

    /**
     * 构建已解析声明状态。
     */
    private fun buildResolvedStatus(visibility: Visibility, modality: Modality): CfirDeclarationStatusImpl {
        return CfirDeclarationStatusImpl(visibility, modality).apply {
            isVisibilityExplicit = visibility != Visibilities.Public
            isModalityExplicit = modality != Modality.FINAL
            isAbstract = modality == Modality.ABSTRACT
            isOpen = modality == Modality.OPEN
            isSealed = modality == Modality.SEALED
        }
    }
}
