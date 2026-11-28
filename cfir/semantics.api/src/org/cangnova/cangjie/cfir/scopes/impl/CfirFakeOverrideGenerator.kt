package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.common.nullableModuleData
import org.cangnova.cangjie.cfir.containingClassForStaticMemberAttr
import org.cangnova.cangjie.cfir.copy
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.expressions.builder.buildExpressionStub
import org.cangnova.cangjie.cfir.originalForSubstitutionOverrideAttr
import org.cangnova.cangjie.cfir.resolve.substitution.ChainedSubstitutor
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.substitution.substitutorByMap
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.scopes.DeferredCallableCopyReturnType
import org.cangnova.cangjie.cfir.scopes.deferredCallableCopyReturnType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 对位 Kotlin FirFakeOverrideGenerator 的签名副本工厂。
 * scope 计算 use-site 替换，工厂创建独立签名和参数归属；正文、默认 assignment 与 helper 不成为副本拥有的子图。
 */
object CfirFakeOverrideGenerator {
    fun createSymbolForSubstitutionOverride(baseSymbol: CfirNamedFunctionSymbol, derivedClassId: ClassId? = null): CfirNamedFunctionSymbol =
        CfirNamedFunctionSymbol(derivedClassId?.let { CallableId(it, baseSymbol.name) } ?: baseSymbol.callableId)

    fun createSymbolForSubstitutionOverride(baseSymbol: CfirPropertySymbol, derivedClassId: ClassId? = null): CfirPropertySymbol =
        CfirPropertySymbol(derivedClassId?.let { CallableId(it, baseSymbol.name) } ?: baseSymbol.callableId)

    fun createSubstitutionOverrideFunction(
        session: CfirSession,
        symbolForSubstitutionOverride: CfirNamedFunctionSymbol,
        baseFunction: CfirNamedFunction,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        newDispatchReceiverType: ConeSimpleCangJieType?,
        origin: CfirDeclarationOrigin.SubstitutionOverride,
        newReturnType: ConeCangJieType? = null,
        newParameterTypes: List<ConeCangJieType?>? = null,
        newTypeParameters: List<CfirTypeParameter>? = null,
        callableCopySubstitutionForTypeUpdater: DeferredCallableCopyReturnType? = null,
    ): CfirNamedFunctionSymbol {
        createCopyForCfirFunction(
            symbolForSubstitutionOverride, baseFunction, derivedClassLookupTag, session, origin,
            newDispatchReceiverType, newParameterTypes, newTypeParameters, newReturnType,
            deferredReturnTypeCalculation = callableCopySubstitutionForTypeUpdater, markAsOverride = true,
        ).originalForSubstitutionOverrideAttr = baseFunction
        return symbolForSubstitutionOverride
    }

    fun createCopyForCfirFunction(
        newSymbol: CfirNamedFunctionSymbol,
        baseFunction: CfirNamedFunction,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        session: CfirSession,
        origin: CfirDeclarationOrigin,
        newDispatchReceiverType: ConeSimpleCangJieType?,
        newParameterTypes: List<ConeCangJieType?>? = null,
        newTypeParameters: List<CfirTypeParameterRef>? = null,
        newReturnType: ConeCangJieType? = null,
        newModality: Modality? = null,
        newVisibility: Visibility? = null,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType? = null,
        newSource: CjSourceElement? = derivedClassLookupTag?.toSymbol(session)?.source ?: baseFunction.source,
        markAsOverride: Boolean,
    ): CfirNamedFunction = buildNamedFunction {
        source = newSource
        moduleData = session.nullableModuleData ?: baseFunction.moduleData
        this.origin = origin
        name = baseFunction.name
        status = baseFunction.status.copy(newVisibility, newModality, isOverride = if (markAsOverride) true else baseFunction.status.isOverride)
        isLocal = baseFunction.isLocal
        isMut = baseFunction.isMut
        symbol = newSymbol
        resolvePhase = origin.resolvePhaseForCopy
        dispatchReceiverType = newDispatchReceiverType
        attributes = baseFunction.attributes.copy()
        hasVariableLenArg = baseFunction.hasVariableLenArg
        typeParameters += configureAnnotationsTypeParametersAndSignature(
            session, baseFunction, newParameterTypes, newTypeParameters, newReturnType,
            deferredReturnTypeCalculation, newSymbol, copyDefaultValues = false,
        ).filterIsInstance<CfirTypeParameter>()
        deprecationsProvider = baseFunction.deprecationsProvider
    }.apply {
        containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseFunction) }
    }

    fun createCopyForCfirConstructor(
        fakeOverrideSymbol: CfirConstructorSymbol,
        session: CfirSession,
        baseConstructor: CfirConstructor,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        origin: CfirDeclarationOrigin,
        newDispatchReceiverType: ConeSimpleCangJieType?,
        newReturnType: ConeCangJieType?,
        newParameterTypes: List<ConeCangJieType?>?,
        newTypeParameters: List<CfirTypeParameterRef>?,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType?,
        newSource: CjSourceElement? = null,
    ): CfirConstructor = buildConstructor {
        source = newSource ?: derivedClassLookupTag?.toSymbol(session)?.source ?: baseConstructor.source
        moduleData = session.nullableModuleData ?: baseConstructor.moduleData
        this.origin = origin
        status = baseConstructor.status.copy()
        isLocal = baseConstructor.isLocal
        symbol = fakeOverrideSymbol
        attributes = baseConstructor.attributes.copy()
        typeParameters += configureAnnotationsTypeParametersAndSignature(
            session, baseConstructor, newParameterTypes, newTypeParameters, newReturnType,
            deferredReturnTypeCalculation, fakeOverrideSymbol, copyDefaultValues = true,
        )
        dispatchReceiverType = newDispatchReceiverType
        resolvePhase = origin.resolvePhaseForCopy
        hasVariableLenArg = baseConstructor.hasVariableLenArg
        deprecationsProvider = baseConstructor.deprecationsProvider
    }.apply {
        originalForSubstitutionOverrideAttr = baseConstructor
        containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseConstructor) }
    }

    private fun CfirFunctionBuilder.configureAnnotationsTypeParametersAndSignature(
        useSiteSession: CfirSession,
        baseFunction: CfirFunction,
        newParameterTypes: List<ConeCangJieType?>?,
        newTypeParameters: List<CfirTypeParameterRef>?,
        newReturnType: ConeCangJieType?,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType?,
        symbolForOverride: CfirBasedSymbol<*>,
        copyDefaultValues: Boolean = false,
    ): List<CfirTypeParameterRef> = when {
        baseFunction.typeParameters.isEmpty() -> {
            configureAnnotationsAndSignature(baseFunction, symbolForOverride, newParameterTypes, newReturnType, deferredReturnTypeCalculation, origin, copyDefaultValues)
            emptyList()
        }
        newTypeParameters == null -> {
            val (copiedParameters, substitutor) = createNewTypeParametersAndSubstitutor(useSiteSession, baseFunction, symbolForOverride, ConeSubstitutor.Empty, origin)
            val copiedParameterTypes = baseFunction.valueParameters.map { substitutor.substituteOrNull(checkNotNull(it.returnTypeRef.coneTypeOrNull)) }
            val baseReturnType = baseFunction.returnTypeRef.coneTypeOrNull
            val copiedReturnType = (newReturnType ?: baseReturnType)?.let(substitutor::substituteOrSelf)
            val deferred = if (baseReturnType == null) DeferredReturnTypeOfSubstitution(substitutor, baseFunction.symbol) else null
            configureAnnotationsAndSignature(baseFunction, symbolForOverride, copiedParameterTypes, copiedReturnType, deferred, origin, copyDefaultValues)
            copiedParameters
        }
        else -> {
            configureAnnotationsAndSignature(baseFunction, symbolForOverride, newParameterTypes, newReturnType, deferredReturnTypeCalculation, origin, copyDefaultValues)
            newTypeParameters
        }
    }

    private fun CfirFunctionBuilder.configureAnnotationsAndSignature(
        baseFunction: CfirFunction,
        fakeFunctionSymbol: CfirBasedSymbol<*>,
        newParameterTypes: List<ConeCangJieType?>?,
        newReturnType: ConeCangJieType?,
        deferredTypeCalculation: DeferredCallableCopyReturnType?,
        origin: CfirDeclarationOrigin,
        copyDefaultValues: Boolean = false,
    ) {
        checkStatusIsResolved(baseFunction)
        annotations += baseFunction.annotations
        val calculation = deferredTypeCalculation ?: if (baseFunction.returnTypeRef is CfirImplicitTypeRef) {
            DeferredReturnTypeOfSubstitution(ConeSubstitutor.Empty, baseFunction.symbol)
        } else null
        if (calculation != null) {
            returnTypeRef = buildImplicitTypeRef()
            attributes.deferredCallableCopyReturnType = calculation
        } else {
            returnTypeRef = baseFunction.returnTypeRef.withReplacedReturnType(newReturnType)
        }
        require(newParameterTypes == null || newParameterTypes.size == baseFunction.valueParameters.size) {
            "A copied signature must retain every ordered parameter"
        }
        valueParameters += baseFunction.valueParameters.zip(newParameterTypes ?: List(baseFunction.valueParameters.size) { null }) { parameter, type ->
            buildCopyForValueParameter(parameter, parameter.returnTypeRef.withReplacedConeType(type), origin, fakeFunctionSymbol, source ?: parameter.source, copyDefaultValues)
        }
    }

    /** 默认值存在性与 assignment 所有权分开：构造器 stub 不承载原表达式、源码事实或 helper。 */
    private fun buildCopyForValueParameter(
        original: CfirValueParameter,
        returnTypeRef: CfirTypeRef,
        origin: CfirDeclarationOrigin,
        containingDeclarationSymbol: CfirBasedSymbol<*>,
        source: CjSourceElement?,
        copyDefaultValues: Boolean = true,
    ): CfirValueParameter = buildValueParameterCopy(original) {
        this.origin = origin
        this.source = source
        this.returnTypeRef = returnTypeRef
        symbol = CfirValueParameterSymbol(original.symbol.callableId)
        this.containingDeclarationSymbol = containingDeclarationSymbol
        defaultValue = if (copyDefaultValues && original.hasInitialDefaultValue) buildExpressionStub {
            coneTypeOrNull = returnTypeRef.coneTypeOrNull
        } else null
        resolvePhase = origin.resolvePhaseForCopy
    }.apply {
        originalForSubstitutionOverrideAttr = original
        defaultAssignmentSyntaxFacts = null
        defaultValueMetadata = if (copyDefaultValues) original.defaultValueMetadata?.copy(function = null) else null
    }

    fun createSubstitutionOverrideProperty(
        session: CfirSession,
        symbolForSubstitutionOverride: CfirPropertySymbol,
        baseProperty: CfirProperty,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        newDispatchReceiverType: ConeSimpleCangJieType?,
        origin: CfirDeclarationOrigin.SubstitutionOverride,
        newReturnType: ConeCangJieType? = null,
        newTypeParameters: List<CfirTypeParameter>? = null,
        callableCopySubstitutionForTypeUpdater: DeferredCallableCopyReturnType? = null,
    ): CfirPropertySymbol {
        createCopyForCfirProperty(
            symbolForSubstitutionOverride, baseProperty, derivedClassLookupTag, session, origin,
            newDispatchReceiverType, newTypeParameters, newReturnType,
            deferredReturnTypeCalculation = callableCopySubstitutionForTypeUpdater,
        ).originalForSubstitutionOverrideAttr = baseProperty
        return symbolForSubstitutionOverride
    }

    fun createCopyForCfirProperty(
        newSymbol: CfirPropertySymbol,
        baseProperty: CfirProperty,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        session: CfirSession,
        origin: CfirDeclarationOrigin,
        newDispatchReceiverType: ConeSimpleCangJieType?,
        newTypeParameters: List<CfirTypeParameter>? = null,
        newReturnType: ConeCangJieType? = null,
        newModality: Modality? = null,
        newVisibility: Visibility? = null,
        newSetterVisibility: Visibility? = null,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType? = null,
        newSource: CjSourceElement? = derivedClassLookupTag?.toSymbol(session)?.source,
    ): CfirProperty = buildProperty {
        source = newSource ?: baseProperty.source
        moduleData = session.nullableModuleData ?: baseProperty.moduleData
        this.origin = origin
        symbol = newSymbol
        name = baseProperty.name
        status = baseProperty.status.copy(newVisibility, newModality, isOverride = true)
        isLocal = baseProperty.isLocal
        resolvePhase = origin.resolvePhaseForCopy
        dispatchReceiverType = newDispatchReceiverType
        attributes = baseProperty.attributes.copy()
        typeParameters += configureAnnotationsTypeParametersAndSignature(session, baseProperty, newTypeParameters, newReturnType, deferredReturnTypeCalculation)
        deprecationsProvider = baseProperty.deprecationsProvider
        getter = baseProperty.getter?.buildCopyIfNeeded(moduleData, origin, returnTypeRef, newSymbol, dispatchReceiverType, derivedClassLookupTag, baseProperty,
            newSource ?: baseProperty.getter?.source, newVisibility ?: baseProperty.getter?.status?.visibility, newModality)
        setter = baseProperty.setter?.buildCopyIfNeeded(moduleData, origin, returnTypeRef, newSymbol, dispatchReceiverType, derivedClassLookupTag, baseProperty,
            newSource ?: baseProperty.setter?.source, newSetterVisibility ?: baseProperty.setter?.status?.visibility, newModality)
    }.apply {
        containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseProperty) }
    }

    private fun CfirPropertyBuilder.configureAnnotationsTypeParametersAndSignature(
        useSiteSession: CfirSession,
        baseProperty: CfirProperty,
        newTypeParameters: List<CfirTypeParameter>?,
        newReturnType: ConeCangJieType?,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType?,
    ): List<CfirTypeParameter> {
        checkStatusIsResolved(baseProperty)
        annotations += baseProperty.annotations
        val (parameters, substitutor) = if (newTypeParameters == null) {
            createNewTypeParametersAndSubstitutor(useSiteSession, baseProperty, symbol, ConeSubstitutor.Empty, origin)
        } else newTypeParameters to ConeSubstitutor.Empty
        val baseReturnType = baseProperty.returnTypeRef.coneTypeOrNull
        val calculation = deferredReturnTypeCalculation ?: if (baseReturnType == null) DeferredReturnTypeOfSubstitution(substitutor, baseProperty.symbol) else null
        if (calculation != null) {
            returnTypeRef = buildImplicitTypeRef()
            attributes.deferredCallableCopyReturnType = calculation
        } else {
            returnTypeRef = baseProperty.returnTypeRef.withReplacedReturnType((newReturnType ?: baseReturnType)?.let(substitutor::substituteOrSelf))
        }
        return parameters.filterIsInstance<CfirTypeParameter>()
    }

    private fun CfirPropertyAccessor.buildCopyIfNeeded(
        moduleData: org.cangnova.cangjie.cfir.common.CfirModuleData,
        origin: CfirDeclarationOrigin,
        propertyReturnTypeRef: CfirTypeRef,
        propertySymbol: CfirPropertySymbol,
        dispatchReceiverType: ConeSimpleCangJieType?,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        baseProperty: CfirProperty,
        newSource: CjSourceElement? = source,
        newVisibility: Visibility? = status.visibility,
        newModality: Modality? = status.modality,
    ): CfirPropertyAccessor = buildCopy(moduleData, origin, propertyReturnTypeRef, propertySymbol, dispatchReceiverType,
        derivedClassLookupTag, baseProperty, newSource, newVisibility, newModality)

    private fun CfirPropertyAccessor.buildCopy(
        moduleData: org.cangnova.cangjie.cfir.common.CfirModuleData,
        origin: CfirDeclarationOrigin,
        propertyReturnTypeRef: CfirTypeRef,
        propertySymbol: CfirPropertySymbol,
        dispatchReceiverType: ConeSimpleCangJieType?,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        baseProperty: CfirProperty,
        newSource: CjSourceElement?,
        newVisibility: Visibility?,
        newModality: Modality?,
    ): CfirPropertyAccessor {
        checkStatusIsResolved(this)
        return buildPropertyAccessorCopy(this) {
            source = newSource
            symbol = CfirPropertyAccessorSymbol()
            this.moduleData = moduleData
            this.origin = origin
            this.propertySymbol = propertySymbol
            this.dispatchReceiverType = dispatchReceiverType
            body = null
            resolvePhase = origin.resolvePhaseForCopy
            status = status.copy(newVisibility, newModality)
        }.also { copy ->
            if (copy.isGetter) copy.replaceReturnTypeRef(propertyReturnTypeRef) else {
                check(copy.valueParameters.size == 1) { "A setter signature must have exactly one value parameter" }
                val parameter = copy.valueParameters.single()
                copy.replaceValueParameters(listOf(buildCopyForValueParameter(parameter, propertyReturnTypeRef, origin, copy.symbol, copy.source ?: parameter.source)))
            }
            copy.originalForSubstitutionOverrideAttr = this
            copy.containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseProperty) }
        }
    }

    fun createCopyForCfirField(
        newSymbol: CfirFieldVariableSymbol,
        baseField: CfirFieldVariable,
        derivedClassLookupTag: ConeClassLikeLookupTag?,
        session: CfirSession,
        origin: CfirDeclarationOrigin,
        newDispatchReceiverType: ConeSimpleCangJieType?,
        newTypeParameters: List<CfirTypeParameter>? = null,
        newReturnType: ConeCangJieType? = null,
        newModality: Modality? = null,
        newVisibility: Visibility? = null,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType? = null,
    ): CfirFieldVariable = buildFieldVariable {
        source = baseField.source
        moduleData = session.nullableModuleData ?: baseField.moduleData
        this.origin = origin
        symbol = newSymbol
        name = baseField.name
        status = baseField.status.copy(newVisibility, newModality)
        isLocal = baseField.isLocal
        isVar = baseField.isVar
        isTypeImplicit = baseField.isTypeImplicit
        resolvePhase = origin.resolvePhaseForCopy
        dispatchReceiverType = newDispatchReceiverType
        attributes = baseField.attributes.copy()
        checkStatusIsResolved(baseField)
        annotations += baseField.annotations
        val (parameters, substitutor) = if (newTypeParameters == null) {
            createNewTypeParametersAndSubstitutor(session, baseField, newSymbol, ConeSubstitutor.Empty, origin)
        } else newTypeParameters to ConeSubstitutor.Empty
        typeParameters += parameters.filterIsInstance<CfirTypeParameter>()
        val calculation = deferredReturnTypeCalculation ?: if (baseField.returnTypeRef is CfirImplicitTypeRef) DeferredReturnTypeOfSubstitution(substitutor, baseField.symbol) else null
        if (calculation != null) {
            returnTypeRef = buildImplicitTypeRef()
            attributes.deferredCallableCopyReturnType = calculation
        } else returnTypeRef = baseField.returnTypeRef.withReplacedReturnType((newReturnType ?: baseField.returnTypeRef.coneTypeOrNull)?.let(substitutor::substituteOrSelf))
        deprecationsProvider = baseField.deprecationsProvider
    }.apply {
        originalForSubstitutionOverrideAttr = baseField
        containingClassForStaticMemberAttr = derivedClassLookupTag.takeIf { shouldOverrideSetContainingClass(baseField) }
    }

    /** enum constructor 是仓颉声明种类；沿同一参数工厂保留默认存在性和新 callable 归属。 */
    fun createCopyForCfirEnumConstructor(
        newSymbol: CfirEnumConstructorSymbol,
        baseConstructor: CfirEnumConstructor,
        session: CfirSession,
        origin: CfirDeclarationOrigin,
        newDispatchReceiverType: ConeSimpleCangJieType?,
        newReturnType: ConeCangJieType?,
        newParameterTypes: List<ConeCangJieType?>,
        newTypeParameters: List<CfirTypeParameterRef>,
        deferredReturnTypeCalculation: DeferredCallableCopyReturnType? = null,
    ): CfirEnumConstructor = buildEnumConstructor {
        checkStatusIsResolved(baseConstructor)
        source = baseConstructor.source
        moduleData = session.nullableModuleData ?: baseConstructor.moduleData
        this.origin = origin
        symbol = newSymbol
        name = baseConstructor.name
        status = baseConstructor.status.copy()
        isLocal = baseConstructor.isLocal
        resolvePhase = origin.resolvePhaseForCopy
        dispatchReceiverType = newDispatchReceiverType
        attributes = baseConstructor.attributes.copy()
        annotations += baseConstructor.annotations
        returnTypeRef = if (deferredReturnTypeCalculation == null) {
            baseConstructor.returnTypeRef.withReplacedReturnType(newReturnType)
        } else {
            buildImplicitTypeRef()
        }
        attributes.deferredCallableCopyReturnType = deferredReturnTypeCalculation
        typeParameters += newTypeParameters
        require(newParameterTypes.size == baseConstructor.valueParameters.size)
        valueParameters += baseConstructor.valueParameters.zip(newParameterTypes) { parameter, type ->
            buildCopyForValueParameter(parameter, parameter.returnTypeRef.withReplacedConeType(type), origin, newSymbol, source ?: parameter.source, copyDefaultValues = true)
        }
        deprecationsProvider = baseConstructor.deprecationsProvider
    }.apply { originalForSubstitutionOverrideAttr = baseConstructor }

    /** 自有映射 → owner 映射 → constructed-class 映射；顺序避免把 owner 上界捕获成副本的自循环。 */
    fun createNewTypeParametersAndSubstitutor(
        useSiteSession: CfirSession,
        original: CfirTypeParameterRefsOwner,
        symbolForOverride: CfirBasedSymbol<*>,
        substitutor: ConeSubstitutor,
        origin: CfirDeclarationOrigin,
        forceTypeParametersRecreation: Boolean = true,
    ): Pair<List<CfirTypeParameterRef>, ConeSubstitutor> {
        if (original.typeParameters.isEmpty()) return original.typeParameters to substitutor
        val newParameters = original.typeParameters.map { ref ->
            val parameter = ref.symbol.cfir
            CfirTypeParameterBuilder().apply {
                source = parameter.source
                moduleData = parameter.moduleData
                this.origin = origin
                resolvePhase = origin.resolvePhaseForCopy
                attributes = CfirDeclarationAttributes()
                name = parameter.name
                symbol = CfirTypeParameterSymbol()
                annotations += parameter.annotations
                containingDeclarationSymbol = symbolForOverride
            }
        }
        val (own, constructed) = original.typeParameters.zip(newParameters).partition { it.first !is CfirConstructedClassTypeParameterRef }
        fun substitutorFrom(pairs: List<Pair<CfirTypeParameterRef, CfirTypeParameterBuilder>>): ConeSubstitutor =
            substitutorByMap(pairs.associate { (originalParameter, copy) -> originalParameter.symbol to copy.symbol.constructType() }, useSiteSession)
        val combined = ChainedSubstitutor(substitutorFrom(own), ChainedSubstitutor(substitutor, substitutorFrom(constructed)))
        var changed = forceTypeParametersRecreation
        newParameters.zip(original.typeParameters).forEach { (copy, originalParameter) ->
            originalParameter.symbol.resolvedBounds.forEach { bound ->
                val type = checkNotNull(bound.coneTypeOrNull)
                val substituted = combined.substituteOrNull(type)
                if (substituted != null) changed = true
                copy.bounds += buildResolvedTypeRef {
                    source = bound.source
                    coneType = substituted ?: type
                }
            }
        }
        if (!changed) return original.typeParameters to substitutor
        return newParameters.map(CfirTypeParameterBuilder::build) to combined
    }

    private fun shouldOverrideSetContainingClass(base: CfirCallableDeclaration): Boolean =
        base is CfirConstructor || base.status.isStatic || base.containingClassForStaticMemberAttr != null

    private fun checkStatusIsResolved(member: CfirCallableDeclaration) {
        check(member.status is CfirResolvedDeclarationStatus) {
            "A fake override requires resolved declaration status: ${member.symbol}"
        }
    }

    private val CfirDeclarationOrigin.resolvePhaseForCopy: CfirResolvePhase
        get() = if (isLazyResolvable) CfirResolvePhase.STATUS else CfirResolvePhase.BODY_RESOLVE
}
