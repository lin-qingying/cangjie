

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.cfir.containingClassForStaticMemberAttr
import org.cangnova.cangjie.cfir.copyWithNewSource
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.impl.CfirPatternVariableImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirPatternBindingVariableImpl
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirPropertyBodyResolveState
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.resolvedWithEffectiveModality
import org.cangnova.cangjie.cfir.declarations.enumConstructorPayloadParameterName
import org.cangnova.cangjie.cfir.declarations.builder.buildConstructedClassTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.builder.buildEnumConstructor
import org.cangnova.cangjie.cfir.declarations.builder.buildFieldVariable
import org.cangnova.cangjie.cfir.declarations.builder.buildConstructor
import org.cangnova.cangjie.cfir.declarations.utils.constructedClassTypeParameterRefs
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildPrimaryConstructor
import org.cangnova.cangjie.cfir.declarations.builder.buildProperty
import org.cangnova.cangjie.cfir.declarations.builder.buildPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeAlias
import org.cangnova.cangjie.cfir.declarations.builder.buildValueParameter
import org.cangnova.cangjie.cfir.declarations.utils.hasSerializedInitializer
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.expressions.builder.buildLazyExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildExpressionStub
import org.cangnova.cangjie.cfir.patterns.builder.buildBindingPattern
import org.cangnova.cangjie.cfir.expressions.withCfirSymbolEntry
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.CjPsiUtil
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeParameterListOwner
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.stubs.CangJieCompiledDeclarationStatus
import org.cangnova.cangjie.psi.stubs.CangJieCompiledSetterParameter
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCallCopy
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.cfir.serialization.deserialize.CjoDeserializationMode
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeclDeserializer
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirTypeDeserializer
import com.intellij.openapi.util.io.FileUtil
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
            status = typeAlias.buildResolvedStatus()
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
    @OptIn(SuspiciousFakeSourceCheck::class)
    private fun loadPropertyGetter(
        getter: CjPropertyAccessor?,
        classSymbol: CfirClassLikeSymbol<*>?,
        returnTypeRef: CfirTypeRef,
        propertySymbol: CfirPropertySymbol,
        serializedStatus: CangJieCompiledDeclarationStatus?,
        property: CjProperty,
        binaryGetter: CfirPropertyAccessor?,
    ): CfirPropertyAccessor? {
        if (serializedStatus == null) {
            check(getter == null && binaryGetter == null) { "CJO getter has no own serialized status" }
            return null
        }
        val accessorStatus = serializedStatus.toResolvedStatus()
        // abstract/foreign 的合法语法投影省略 accessor；存在性与状态仍由真实 CJO header 决定。
        val accessorSource = if (getter != null) CjRealPsiSourceElement(getter)
            else CjFakePsiSourceElement(property, CjFakeSourceElementKind.DefaultAccessor)
        val accessor = buildPropertyAccessor {
            source = accessorSource
            moduleData = c.moduleData
            origin = initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            this.returnTypeRef = returnTypeRef.copyWithNewSource(source!!)
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            isGetter = true
            status = accessorStatus
            this.symbol = CfirPropertyAccessorSymbol()
            // 对齐 binary accessor 的 dispatchReceiverTypeForCurrentOwner：static accessor 没有接收者。
            dispatchReceiverType = c.dispatchReceiver.takeUnless { accessorStatus.isStatic }
            this.propertySymbol = propertySymbol
            deprecationsProvider = EmptyDeprecationsProvider
        }
        return accessor.apply {
            replaceAnnotations(copyBinaryAnnotations(checkNotNull(binaryGetter).annotations, accessor.symbol))
            replaceDeprecationsProvider(EmptyDeprecationsProvider)
            containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
        }
    }

    /**
     * 反序列化属性 setter。
     */
    @OptIn(SuspiciousFakeSourceCheck::class)
    private fun loadPropertySetter(
        setter: CjPropertyAccessor?,
        classSymbol: CfirClassLikeSymbol<*>?,
        propertyTypeRef: CfirTypeRef,
        propertySymbol: CfirPropertySymbol,
        local: StubBasedCfirDeserializationContext,
        serializedStatus: CangJieCompiledDeclarationStatus?,
        property: CjProperty,
        binarySetter: CfirPropertyAccessor?,
        parameterFacts: CangJieCompiledSetterParameter?,
    ): CfirPropertyAccessor? {
        if (serializedStatus == null) {
            check(setter == null && binarySetter == null && parameterFacts == null) { "CJO setter has no own serialized status" }
            return null
        }
        val accessorStatus = serializedStatus.toResolvedStatus()
        val accessorSource = if (setter != null) CjRealPsiSourceElement(setter)
            else CjFakePsiSourceElement(property, CjFakeSourceElementKind.DefaultAccessor)
        val accessorSymbol = CfirPropertyAccessorSymbol()
        val accessor = buildPropertyAccessor {
            source = accessorSource
            moduleData = c.moduleData
            origin = initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            this.returnTypeRef = c.session.builtinTypes.unitType.toCfirResolvedTypeRef(source)
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            isGetter = false
            status = accessorStatus
            this.symbol = accessorSymbol
            // 对齐 binary accessor 的 dispatchReceiverTypeForCurrentOwner：static accessor 没有接收者。
            dispatchReceiverType = c.dispatchReceiver.takeUnless { accessorStatus.isStatic }
            // 仓颉 setter 的参数类型由属性声明确定，反编译 PSI 合法地只保存参数名。
            // 对齐官方 TypeChecker 对 PropDecl setter 的类型赋值，不把省略类型当作损坏参数。
            val facts = checkNotNull(parameterFacts) { "CJO setter is missing its parameter facts" }
            val originalParameter = checkNotNull(binarySetter).valueParameters.single()
            val parameterSource = setter?.valueParameters?.single()?.let(::CjRealPsiSourceElement) ?: accessorSource
            val parameter = buildValueParameter {
                source = parameterSource
                moduleData = c.moduleData
                containingDeclarationSymbol = accessorSymbol
                origin = initialOrigin
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                deprecationsProvider = EmptyDeprecationsProvider
                returnTypeRef = propertyTypeRef.copyWithNewSource(parameterSource)
                name = if (facts.isNameSpecial) Name.special(facts.name) else Name.identifier(facts.name)
                symbol = CfirValueParameterSymbol(CallableId(c.packageFqName, c.relativeClassName, name))
                resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
                isNamed = facts.isNamed
                status = facts.status.toResolvedStatus()
                defaultValue = if (facts.hasDefaultValue) buildExpressionStub { coneTypeOrNull = propertyTypeRef.coneType } else null
            }
            parameter.replaceAnnotations(copyBinaryAnnotations(originalParameter.annotations, parameter.symbol))
            valueParameters += parameter
            this.propertySymbol = propertySymbol
            deprecationsProvider = EmptyDeprecationsProvider
        }
        return accessor.apply {
            replaceAnnotations(copyBinaryAnnotations(checkNotNull(binarySetter).annotations, accessor.symbol))
            replaceDeprecationsProvider(EmptyDeprecationsProvider)
            containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
        }
    }

    /** 同一 binary、module 和 pool index 的只读签名；状态仍来自 stub，不按名字重配对。 */
    private fun loadBinaryPropertyHeader(
        property: CjProperty,
        metadata: org.cangnova.cangjie.psi.stubs.CangJieCompiledDeclarationMetadata,
    ): CfirProperty {
        val ownerSession = c.session as? LLCfirSession ?: error("Compiled property requires its actual library session")
        val binaryContext = checkNotNull(ownerSession.cjoSymbolProvider.getOwnPackageContext(
            c.packageFqName.asString(), CjoDeserializationMode.SIGNATURE_ONLY,
        )) { "Compiled property binary metadata is unavailable" }
        check(binaryContext.moduleData === c.moduleData) { "Compiled property has a different binary module owner" }
        check(FileUtil.pathsEqual(binaryContext.sourcePath?.toString(), property.containingCjFile.virtualFile.path)) {
            "Compiled property header belongs to another binary file"
        }
        val index = checkNotNull(metadata.declarationIndex) { "Compiled property has no binary declaration index" }
        val deserializer = CfirDeclDeserializer(binaryContext, CfirTypeDeserializer(binaryContext))
        val binary = checkNotNull(deserializer.deserializeDecl(index) as? CfirProperty)
            { "Compiled property index does not identify a property" }
        check((metadata.getterStatus != null) == (binary.getter != null))
        check((metadata.setterStatus != null) == (binary.setter != null))
        binary.setter?.let { setter ->
            val facts = checkNotNull(metadata.setterParameter)
            val parameter = setter.valueParameters.single()
            check(parameter.name.asString() == facts.name && parameter.name.isSpecial == facts.isNameSpecial)
            check(parameter.isNamed == facts.isNamed && (parameter.defaultValue != null) == facts.hasDefaultValue)
            check(parameter.returnTypeRef.coneType == binary.returnTypeRef.coneType)
        }
        return binary
    }

    /** signature annotations 保留原解析身份和参数树，仅把所属声明绑定到当前 accessor/parameter。 */
    private fun copyBinaryAnnotations(annotations: List<CfirAnnotation>, owner: CfirBasedSymbol<*>): List<CfirAnnotation> =
        annotations.map { annotation ->
            check(annotation is CfirAnnotationCall) { "CJO annotations must be represented by annotation calls" }
            buildAnnotationCallCopy(annotation) { containingDeclarationSymbol = owner }
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
        val metadata = property.compiledDeclarationMetadata
        val binaryHeader = loadBinaryPropertyHeader(property, metadata)
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
            status = metadata.status.toResolvedStatus()
            // 对齐 binary `dispatchReceiverTypeForCurrentOwner`：static 成员没有 dispatch receiver。
            dispatchReceiverType = c.dispatchReceiver.takeUnless { status.isStatic }
            isLocal = false
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            typeParameters += local.typeDeserializer.ownTypeParameters.map { it.cfir }
            val allAnnotations = c.annotationDeserializer.loadAnnotations(property, symbol)
            annotations += allAnnotations
            getter = loadPropertyGetter(property.getter, classSymbol, returnTypeRef, symbol, metadata.getterStatus, property, binaryHeader.getter)
            setter = loadPropertySetter(property.setter, classSymbol, returnTypeRef, symbol, local, metadata.setterStatus, property,
                binaryHeader.setter, metadata.setterParameter)
            bodyResolveState = CfirPropertyBodyResolveState.ALL_BODIES_RESOLVED
            deprecationsProvider = EmptyDeprecationsProvider
        }.apply {
            // 对齐 binary `publishDeclarationMetadata`：static callable 必须显式记录名义 owner。
            if (status.isStatic) {
                containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
            }
            replaceDeprecationsProvider(EmptyDeprecationsProvider)
        }
    }

    /**
     * 反序列化 CJO 全局 VarDecl 的单名模式投影。
     *
     * 容器只拥有声明结构和 initializer；包级查询返回拥有名字、类型与状态的独立叶子。
     */
    @OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)
    fun loadPatternVariable(declaration: CjPatternVariable, ownerSymbol: CfirPatternVariableSymbol): CfirPatternVariable {
        val psiBinding = checkNotNull(declaration.pattern as? CjBindingPattern) {
            "A CJO global VarDecl must have a single projected binding"
        }
        val name = psiBinding.nameAsSafeName
        val type = checkNotNull(declaration.typeReference) { "A compiled global binding must have a signature type" }.toTypeRef(c)
        val status = declaration.buildResolvedStatus()
        val bindingSymbol = CfirPatternBindingSymbol(CallableId(c.packageFqName, name))
        val binding = CfirPatternBindingVariableImpl(
            source = CjRealPsiSourceElement(psiBinding), moduleData = c.moduleData,
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES, annotations = MutableOrEmptyList.empty(),
            origin = initialOrigin, attributes = CfirDeclarationAttributes.EMPTY, isLocal = false,
            deprecationsProvider = EmptyDeprecationsProvider, dispatchReceiverType = null, status = status,
            isVar = declaration.isVar, isTypeImplicit = false, symbol = bindingSymbol, typeParameters = mutableListOf(),
            returnTypeRef = type, name = name,
        )
        bindingSymbol.bind(binding)
        binding.hasSerializedInitializer = declaration.hasInitializer()
        val owner = CfirPatternVariableImpl(
            source = CjRealPsiSourceElement(declaration), moduleData = c.moduleData,
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES, annotations = MutableOrEmptyList.empty(),
            origin = initialOrigin, attributes = CfirDeclarationAttributes.EMPTY, isLocal = false,
            dispatchReceiverType = null, status = status, deprecationsProvider = EmptyDeprecationsProvider,
            initializer = if (declaration.hasInitializer()) buildExpressionStub { coneTypeOrNull = type.coneTypeOrNull } else null,
            isVar = declaration.isVar, isTypeImplicit = false, symbol = ownerSymbol, typeParameters = mutableListOf(),
            returnTypeRef = type,
            pattern = buildBindingPattern { source = CjRealPsiSourceElement(psiBinding); this.name = name; typeRef = type; bindingVariable = binding },
        )
        ownerSymbol.bind(owner)
        return owner
    }

    /** 恢复成员字段的独立身份，字段不会进入模式声明容器。 */
    fun loadFieldVariable(field: CjFieldVariable): CfirFieldVariable {
        val callableName = field.nameAsSafeName
        val callableId = CallableId(c.packageFqName, c.relativeClassName, callableName)
        val symbol = CfirFieldVariableSymbol(callableId)
        val returnTypeRef = field.typeReference?.toTypeRef(c)
            ?: errorWithAttachment("Field variable doesn't have type reference") {
                withPsiEntry("field", field)
            }
        val status = field.buildResolvedStatus()

        return buildFieldVariable {
            source = CjRealPsiSourceElement(field)
            moduleData = c.moduleData
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            origin = initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            deprecationsProvider = EmptyDeprecationsProvider
            dispatchReceiverType = c.dispatchReceiver.takeUnless { status.isStatic }
            this.status = status
            initializer = field.initializer?.let { buildLazyExpression() }
            isVar = field.isVar
            isTypeImplicit = false
            this.symbol = symbol
            this.returnTypeRef = returnTypeRef
            name = callableName
            annotations += c.annotationDeserializer.loadAnnotations(field, symbol)
        }.also { fieldVariable ->
            // 对齐 binary `publishDeclarationMetadata`：static 字段没有 dispatch receiver，必须记录名义 owner。
            if (status.isStatic) {
                fieldVariable.containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
            }
            fieldVariable.hasSerializedInitializer = field.hasInitializer()
        }
    }

    /**
     * 反序列化枚举构造项及其 payload 类型。
     *
     * CJO stub 以 CjEnumConstructor.typeReferences 保存 payload，不会伪造源码参数名；CFIR 使用
     * 与 raw builder 相同的稳定合成名，并把返回类型绑定到所属泛型 enum。
     */
    fun loadEnumConstructor(
        enumConstructor: CjEnumConstructor,
        enumSymbol: CfirEnumSymbol,
        typeParameters: List<CfirTypeParameter>,
    ): CfirEnumConstructor {
        val callableName = enumConstructor.name?.let(Name::identifier)
            ?: errorWithAttachment("Enum constructor doesn't have a name") {
                withPsiEntry("enumConstructor", enumConstructor)
            }
        val enumConstructorSymbol = CfirEnumConstructorSymbol(CallableId(enumSymbol.classId, callableName))
        val valueParameters = enumConstructor.typeReferences.mapIndexed { index, typeReference ->
            val name = enumConstructorPayloadParameterName(index)
            val source = CjRealPsiSourceElement(typeReference)
            val returnTypeRef = typeReference.toTypeRef(c)
            buildValueParameter {
                this.source = source
                moduleData = c.moduleData
                resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
                origin = initialOrigin
                attributes = CfirDeclarationAttributes.EMPTY
                isLocal = false
                deprecationsProvider = EmptyDeprecationsProvider
                isNamed = false
                symbol = CfirValueParameterSymbol(CallableId(enumSymbol.classId, name))
                containingDeclarationSymbol = enumConstructorSymbol
                status = DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
                this.returnTypeRef = returnTypeRef
                this.name = name
                defaultValue = null
            }
        }
        val source = CjRealPsiSourceElement(enumConstructor)
        val returnTypeRef = ConeEnumType(
            lookupTag = enumSymbol.classId.toLookupTag(),
            typeArguments = typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag()) },
            isRefEnum = enumSymbol.isRefEnum,
        ).toCfirResolvedTypeRef(source)

        return buildEnumConstructor {
            this.source = source
            moduleData = c.moduleData
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            origin = initialOrigin
            attributes = CfirDeclarationAttributes.EMPTY
            isLocal = false
            deprecationsProvider = EmptyDeprecationsProvider
            dispatchReceiverType = null
            this.symbol = enumConstructorSymbol
            status = enumConstructor.buildResolvedStatus()
            this.returnTypeRef = returnTypeRef
            this.typeParameters += typeParameters.map { parameter ->
                buildConstructedClassTypeParameterRef { this.symbol = parameter.symbol }
            }
            this.valueParameters += valueParameters
            name = callableName
            annotations += c.annotationDeserializer.loadAnnotations(enumConstructor, enumConstructorSymbol)
        }.apply {
            // 对齐 binary `publishDeclarationMetadata`：enum constructor 无 dispatch receiver，必须记录 owner enum。
            containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag ?: enumSymbol.classId.toLookupTag()
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
        status = function.buildResolvedStatus()
        isLocal = false
        this.symbol = symbol
        // 对齐 binary `dispatchReceiverTypeForCurrentOwner`：static 成员没有 dispatch receiver。
        dispatchReceiverType = c.dispatchReceiver.takeUnless { status.isStatic }
        resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
        typeParameters += local.typeDeserializer.ownTypeParameters.map { it.cfir }
        valueParameters += local.memberDeserializer.valueParameters(function.valueParameters, symbol)
        annotations += c.annotationDeserializer.loadAnnotations(function, symbol)
        deprecationsProvider = EmptyDeprecationsProvider
        isMut = status.isMut
    }.apply {
        // 对齐 binary `publishDeclarationMetadata`：static callable 必须显式记录名义 owner。
        if (status.isStatic) {
            containingClassForStaticMemberAttr = c.dispatchReceiver?.lookupTag
        }
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
            status = constructor.buildResolvedStatus()
            isLocal = false
            this.symbol = symbol
            dispatchReceiverType = null
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            // 构造器引用所属名义类型的参数 symbol，不重复持有 owner 的参数声明。
            this.typeParameters += constructedClassTypeParameterRefs(typeParameters.map { it.symbol })
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
            status = constructor.buildResolvedStatus()
            isLocal = false
            this.symbol = symbol
            dispatchReceiverType = null
            resolvePhase = CfirResolvePhase.ANALYZED_DEPENDENCIES
            // 构造器引用所属名义类型的参数 symbol，不重复持有 owner 的参数声明。
            this.typeParameters += constructedClassTypeParameterRefs(typeParameters.map { it.symbol })
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
        declaredTypeRef: CfirTypeRef? = null,
    ): CfirValueParameter = buildValueParameter {
        source = CjRealPsiSourceElement(parameter)
        moduleData = c.moduleData
        containingDeclarationSymbol = containingSymbol
        origin = initialOrigin
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = false
        deprecationsProvider = EmptyDeprecationsProvider
        returnTypeRef = declaredTypeRef?.copyWithNewSource(source!!) ?: parameter.typeReference?.toTypeRef(c)
            ?: errorWithAttachment("CjParameter doesn't have type") {
                withPsiEntry("parameter", parameter)
                if (containingSymbol.isBound) {
                    withCfirSymbolEntry("containingSymbol", containingSymbol)
                } else {
                    withEntry("containingSymbolKind", containingSymbol::class.java.name)
                }
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
        // 值参数按既有 statusless 约定保存 Local/FINAL，不参与成员状态推导。
        status = CfirDeclarationStatusImpl(Visibilities.Local, Modality.FINAL).resolvedWithEffectiveModality()
        defaultValue = if (forceDefaultValue || parameter.hasDefaultValue()) buildLazyExpression() else null
        annotations += c.annotationDeserializer.loadAnnotations(parameter, containingSymbol)
    }

    /**
     * 使用 [context] 的类型反序列化器把 PSI 类型引用转换为 CFIR type ref。
     */
    private fun CjTypeReference.toTypeRef(context: StubBasedCfirDeserializationContext): CfirTypeRef =
        context.typeDeserializer.typeRef(this)
}
