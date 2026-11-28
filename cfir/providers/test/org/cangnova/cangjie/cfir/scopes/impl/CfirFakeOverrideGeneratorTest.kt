@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.containingClassForStaticMemberAttr
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.builder.*
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.*
import org.cangnova.cangjie.cfir.expressions.impl.CfirExpressionStub
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.providers.CfirLibrarySessionProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirEmptyExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProviderWithoutCallables
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.resolve.substitution.substitutorByMap
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.ProcessorAction
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cjmpDefaultParameterState
import org.cangnova.cangjie.cfir.session.declaredCjmpDefaultParameterState
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/** 通过消费者 lookup 验证签名副本的类型、来源、默认值及子声明所有权，不接受浅 copy 的共享正文。 */
class CfirFakeOverrideGeneratorTest {
    @Test
    fun functionLookupCreatesIndependentSignatureWithoutBodyOrDefaults() {
        val fixture = Fixture()
        val original = fixture.function
        val input = originalInput(original)
        val copy = fixture.functionCopy()

        assertSame(fixture.owner.symbol, fixture.session.cfirProvider.getContainingClass(original.symbol))
        assertSame(fixture.owner.symbol, fixture.session.symbolProvider.getClassLikeSymbolByClassId(fixture.owner.symbol.classId))
        assertFreshParameters(original, copy, listOf(ConePrimitiveType.INT64, fixture.callbackType, fixture.callbackType))
        assertLazyCopyStatus(original, copy)
        assertNull(copy.body)
        copy.valueParameters.forEach { parameter ->
            assertNull(parameter.defaultValue)
            assertNull(parameter.defaultValueMetadata)
            assertNull(parameter.defaultAssignmentSyntaxFacts)
        }
        assertOriginalUnchanged(original, input)
        assertSame(copy, fixture.functionCopy())

        original.replaceBody(fixture.block(fixture.literal(99L)))
        original.valueParameters[1].replaceDefaultValue(fixture.lambda(100L))
        copy.exportFacts = copy.exportFacts.copy(isToolAdded = true)

        assertNull(copy.body)
        assertNull(copy.valueParameters[1].defaultValue)
        assertSame(input.facts, original.exportFacts)
        assertNotSame(original.attributes, copy.attributes)
    }

    @Test
    fun constructorLookupKeepsOnlyTypedDefaultExistenceAndFreshOwners() {
        val fixture = Fixture()
        val original = fixture.constructor
        val input = originalInput(original)
        assertSame(fixture.owner.symbol, fixture.session.cfirProvider.getContainingClass(original.symbol))
        val copy = fixture.constructorCopy()
        assertFreshParameters(original, copy, listOf(ConePrimitiveType.INT64, fixture.callbackType))
        assertLazyCopyStatus(original, copy)
        assertNull(copy.body)
        assertNull(copy.valueParameters[0].defaultValue)
        val parameter = copy.valueParameters[1]
        val stub = requireNotNull(parameter.defaultValue)
        assertTrue(stub is CfirExpressionStub)
        assertEquals(parameter.returnTypeRef.coneTypeOrNull, stub.coneTypeOrNull)
        assertNotSame(original.valueParameters[1].defaultValue, stub)
        assertNull(parameter.defaultAssignmentSyntaxFacts)
        assertNull(requireNotNull(parameter.defaultValueMetadata).function)
        assertTrue(parameter.hasInitialDefaultValue)
        assertFalse(parameter.declaredCjmpDefaultParameterState().canBeReadThrough)
        assertSame(fixture.firstDefault.symbol, parameter.cjmpDefaultParameterState().desugaredDefaultSource)
        assertOriginalUnchanged(original, input)

        original.replaceBody(fixture.block(fixture.literal(102L), type = ConePrimitiveType.UNIT))
        original.valueParameters[1].replaceDefaultValue(fixture.lambda(103L))

        assertNull(copy.body)
        assertSame(stub, parameter.defaultValue)
        assertSame(copy, fixture.constructorCopy())
    }

    @Test
    fun enumConstructorCallableLookupCopiesOrderedParametersAndTypedStub() {
        val fixture = Fixture()
        val original = fixture.variant
        val input = originalInput(original)
        val copy = fixture.enumCopy()
        assertFreshParameters(original, copy, listOf(ConePrimitiveType.INT64, fixture.callbackType))
        assertLazyCopyStatus(original, copy)
        assertEquals(fixture.enumReceiver, copy.returnTypeRef.coneTypeOrNull)
        val ownerParameter = original.typeParameters.single().symbol
        assertTrue(copy.typeParameters.single() is CfirConstructedClassTypeParameterRef)
        assertSame(ownerParameter, copy.typeParameters.single().symbol)
        val enumOwner = fixture.session.symbolProvider.getClassLikeSymbolByClassId(fixture.enumReceiver.classId)
        assertTrue(enumOwner is CfirEnumSymbol)
        assertSame(enumOwner, ownerParameter.cfir.containingDeclarationSymbol)
        assertNull(copy.valueParameters[0].defaultValue)
        val stub = requireNotNull(copy.valueParameters[1].defaultValue)
        assertTrue(stub is CfirExpressionStub)
        assertEquals(fixture.callbackType, stub.coneTypeOrNull)
        assertNotSame(original.valueParameters[1].defaultValue, stub)
        assertSame(fixture.firstDefault.symbol, copy.valueParameters[1].cjmpDefaultParameterState().desugaredDefaultSource)
        assertOriginalUnchanged(original, input)

        original.valueParameters[1].replaceDefaultValue(fixture.lambda(104L))

        assertSame(stub, copy.valueParameters[1].defaultValue)
        assertSame(copy, fixture.enumCopy())
    }

    @Test
    fun propertyLookupRebindsAccessorsAndSetterToNewPropertyType() {
        val fixture = Fixture()
        val original = fixture.property
        val getter = requireNotNull(original.getter)
        val setter = requireNotNull(original.setter)
        val getterInput = originalInput(getter)
        val setterInput = originalInput(setter)
        val propertyType = original.returnTypeRef
        val propertyStatus = original.status
        val propertyAttributes = original.attributes
        val propertyFacts = original.exportFacts
        val copy = fixture.propertyCopy()
        val copiedGetter = requireNotNull(copy.getter)
        val copiedSetter = requireNotNull(copy.setter)

        assertLazyCopyStatus(original, copy)
        assertEquals(ConePrimitiveType.INT64, copy.returnTypeRef.coneTypeOrNull)
        assertNotSame(getter.symbol, copiedGetter.symbol)
        assertNotSame(setter.symbol, copiedSetter.symbol)
        assertSame(copy.symbol, copiedGetter.propertySymbol)
        assertSame(copy.symbol, copiedSetter.propertySymbol)
        assertSame(copy.returnTypeRef, copiedGetter.returnTypeRef)
        assertFreshParameters(setter, copiedSetter, listOf(ConePrimitiveType.INT64))
        assertSame(copy.returnTypeRef, copiedSetter.valueParameters.single().returnTypeRef)
        assertEquals(ConePrimitiveType.UNIT, copiedSetter.returnTypeRef.coneTypeOrNull)
        assertNull(copiedGetter.body)
        assertNull(copiedSetter.body)
        assertLazyCopyStatus(getter, copiedGetter)
        assertLazyCopyStatus(setter, copiedSetter)
        assertOriginalUnchanged(getter, getterInput)
        assertOriginalUnchanged(setter, setterInput)
        assertSame(propertyType, original.returnTypeRef)
        assertSame(propertyStatus, original.status)
        assertSame(propertyAttributes, original.attributes)
        assertSame(propertyFacts, original.exportFacts)
        assertSame(original.symbol, getter.propertySymbol)
        assertSame(original.symbol, setter.propertySymbol)

        getter.replaceBody(fixture.block(fixture.literal(105L)))
        setter.replaceBody(fixture.block(fixture.literal(106L), type = ConePrimitiveType.UNIT))

        assertNull(copiedGetter.body)
        assertNull(copiedSetter.body)
        assertSame(copy, fixture.propertyCopy())
    }

    /** 两个同类型默认参数有不同 helper，必须按原 callable 的 ordinal 读取，不能靠名称或 stub 归属。 */
    @Test
    fun copiedFunctionDefaultsFollowOriginalOrdinalsAndCheckCopyMembershipFirst() {
        val fixture = Fixture()
        val copy = fixture.functionCopy()
        val parameters = copy.valueParameters.toList()
        assertFalse(parameters[0].cjmpDefaultParameterState().hasInitial)
        assertSame(fixture.firstDefault.symbol, parameters[1].cjmpDefaultParameterState().desugaredDefaultSource)
        assertSame(fixture.secondDefault.symbol, parameters[2].cjmpDefaultParameterState().desugaredDefaultSource)
        assertFalse(parameters[1].declaredCjmpDefaultParameterState().hasInitial)
        assertTrue(parameters[1].cjmpDefaultParameterState().hasInitial)
        assertTrue(parameters[1].cjmpDefaultParameterState().canBeReadThrough)

        // 保留 owner 和列表长度，仅替换一个参数；provenance 的 ordinal 不能免除 membership 检查。
        val replacement = buildValueParameterCopy(parameters[1]) {
            symbol = CfirValueParameterSymbol(parameters[1].symbol.callableId)
        }
        copy.replaceValueParameters(listOf(parameters[0], replacement, parameters[2]))

        assertSame(copy.symbol, parameters[1].containingDeclarationSymbol)
        assertThrows(IllegalStateException::class.java) { parameters[1].cjmpDefaultParameterState() }
        assertSame(fixture.firstDefault.symbol, replacement.cjmpDefaultParameterState().desugaredDefaultSource)
        assertSame(fixture.secondDefault.symbol, parameters[2].cjmpDefaultParameterState().desugaredDefaultSource)
    }

    /** 原始 HAS_INITIAL=false 优先于表达式存在性；副本 stub 也不能授予原 assignment/helper 所有权。 */
    @Test
    fun explicitFalseDefaultMetadataRemainsAuthoritativeOverTypedStubs() {
        val fixture = Fixture()
        val original = fixture.constructor.valueParameters[1]
        original.defaultValueMetadata = CfirParameterDefaultValueMetadata(hasInitial = false, function = null)
        val originalStub = buildExpressionStub { coneTypeOrNull = fixture.callbackType }
        original.replaceDefaultValue(originalStub)
        assertFalse(original.hasInitialDefaultValue)
        val copy = fixture.constructorCopy().valueParameters[1]
        assertNull(copy.defaultValue)
        assertFalse(requireNotNull(copy.defaultValueMetadata).hasInitial)

        copy.replaceDefaultValue(buildExpressionStub { coneTypeOrNull = fixture.callbackType })

        assertFalse(copy.hasInitialDefaultValue)
        assertFalse(copy.declaredCjmpDefaultParameterState().hasInitial)
        assertFalse(copy.cjmpDefaultParameterState().hasInitial)
        assertNull(copy.cjmpDefaultParameterState().desugaredDefaultSource)
        assertFalse(copy.cjmpDefaultParameterState().canBeReadThrough)
        assertSame(originalStub, original.defaultValue)
        assertFalse(requireNotNull(original.defaultValueMetadata).hasInitial)
    }

    @Test
    fun nonLazyFactoryCopyKeepsResolvedStatusAndBodyResolvePhaseWithoutSourceContent() {
        val fixture = Fixture()
        val original = fixture.function
        val input = originalInput(original)
        val symbol = CfirNamedFunctionSymbol(original.symbol.callableId)
        val copy = CfirFakeOverrideGenerator.createCopyForCfirFunction(
            newSymbol = symbol,
            baseFunction = original,
            derivedClassLookupTag = fixture.owner.symbol.toLookupTag(),
            session = fixture.session,
            origin = CfirDeclarationOrigin.Library,
            newDispatchReceiverType = fixture.receiver,
            newParameterTypes = listOf(ConePrimitiveType.INT64, fixture.callbackType, fixture.callbackType),
            newReturnType = ConePrimitiveType.INT64,
            markAsOverride = false,
        )
        assertEquals(CfirResolvePhase.BODY_RESOLVE, copy.resolvePhase)
        assertTrue(copy.status is CfirResolvedDeclarationStatus)
        assertNotSame(original.status, copy.status)
        assertEquals(original.status.isOverride, copy.status.isOverride)
        assertEquals(original.status.isUnsafe, copy.status.isUnsafe)
        assertFreshParameters(original, copy, listOf(ConePrimitiveType.INT64, fixture.callbackType, fixture.callbackType))
        copy.valueParameters.forEach { assertEquals(CfirResolvePhase.BODY_RESOLVE, it.resolvePhase) }
        assertNull(copy.body)
        copy.valueParameters.forEach { assertNull(it.defaultValue) }
        assertOriginalUnchanged(original, input)
    }

    /** 原 class ref 借用 class 参数；scope copy 的参数声明及 owner 必须自有，不能回写 class 的参数。 */
    @Test
    fun constructorLookupSeparatesBorrowedClassReferencesFromCopyOwnedTypeParameters() {
        val fixture = Fixture()
        val borrowed = fixture.constructor.typeParameters.single()
        assertTrue(borrowed is CfirConstructedClassTypeParameterRef)
        assertSame(fixture.classParameter.symbol, borrowed.symbol)
        assertSame(fixture.owner.symbol, borrowed.symbol.cfir.containingDeclarationSymbol)
        val copy = fixture.constructorCopy()
        val owned = copy.typeParameters.single()
        assertTrue(owned is CfirTypeParameter)
        assertNotSame(borrowed.symbol, owned.symbol)
        assertSame(copy.symbol, owned.symbol.cfir.containingDeclarationSymbol)
        assertEquals(CfirResolvePhase.STATUS, owned.symbol.cfir.resolvePhase)
        assertSame(fixture.owner.symbol, fixture.classParameter.containingDeclarationSymbol)
        assertSame(fixture.classParameter.symbol, fixture.constructor.typeParameters.single().symbol)
        assertEquals(ConePrimitiveType.INT64, copy.valueParameters[0].returnTypeRef.coneTypeOrNull)
        assertEquals(ConePrimitiveType.INT64, fixture.scope.substitutedOwnerTypeParameterOrNull(fixture.classParameter.symbol))
    }

    /** 泛型 alias 的 class refs 引用 alias 原 symbol；默认值沿委托构造器再回原 callable 读取。 */
    @Test
    fun genericAliasLookupUsesAliasSymbolsFreshValueOwnersAndTypedDefaultStub() {
        val fixture = Fixture()
        val input = originalInput(fixture.constructor)
        val alias = fixture.alias(generic = true)
        val scope = fixture.session.cangjieScopeProvider.getTypealiasConstructorScope(alias, fixture.session, ScopeSession())
        val constructors = mutableListOf<CfirConstructorSymbol>()
        scope.processDeclaredConstructors(constructors::add)
        val copy = constructors.single().cfir
        val original = requireNotNull(copy.typeAliasConstructorInfo).originalConstructor
        val aliasParameter = alias.typeParameters.single()
        assertSame(alias.source, copy.source)
        assertSame(CfirDeclarationOrigin.Synthetic.TypeAliasConstructor, copy.origin)
        assertEquals(original.resolvePhase, copy.resolvePhase)
        assertSame(original.status, copy.status)
        assertNull(copy.body)
        assertTrue(copy.typeParameters.single() is CfirConstructedClassTypeParameterRef)
        assertSame(aliasParameter.symbol, copy.typeParameters.single().symbol)
        assertSame(alias.symbol, aliasParameter.containingDeclarationSymbol)
        assertFreshParameters(original, copy, listOf(aliasParameter.symbol.constructType(), fixture.callbackType))
        val stub = requireNotNull(copy.valueParameters[1].defaultValue)
        assertTrue(stub is CfirExpressionStub)
        assertEquals(copy.valueParameters[1].returnTypeRef.coneTypeOrNull, stub.coneTypeOrNull)
        assertNotSame(original.valueParameters[1].defaultValue, stub)
        assertNull(copy.valueParameters[1].defaultAssignmentSyntaxFacts)
        assertNull(requireNotNull(copy.valueParameters[1].defaultValueMetadata).function)
        assertSame(fixture.firstDefault.symbol, copy.valueParameters[1].cjmpDefaultParameterState().desugaredDefaultSource)
        assertOriginalUnchanged(fixture.constructor, input)
        val again = mutableListOf<CfirConstructorSymbol>()
        scope.processDeclaredConstructors(again::add)
        assertSame(copy.symbol, again.single())
        val parameters = copy.valueParameters.toList()
        copy.replaceValueParameters(listOf(parameters[0]))
        assertThrows(IllegalStateException::class.java) { parameters[1].cjmpDefaultParameterState() }
    }

    /** 无泛型替换时委托声明已在 BODY_RESOLVE，alias buildConstructor 不采用 lazy factory 的 STATUS 策略。 */
    @Test
    fun nonGenericAliasLookupPreservesOriginalPhaseAndStatusIdentity() {
        val fixture = Fixture()
        val input = originalInput(fixture.plainConstructor)
        val alias = fixture.alias(generic = false)
        val scope = fixture.session.cangjieScopeProvider.getTypealiasConstructorScope(alias, fixture.session, ScopeSession())
        val constructors = mutableListOf<CfirConstructorSymbol>()
        scope.processDeclaredConstructors(constructors::add)
        val copy = constructors.single().cfir
        assertSame(fixture.plainConstructor, requireNotNull(copy.typeAliasConstructorInfo).originalConstructor)
        assertEquals(CfirResolvePhase.BODY_RESOLVE, copy.resolvePhase)
        assertSame(fixture.plainConstructor.status, copy.status)
        assertTrue(copy.typeParameters.isEmpty())
        assertNull(copy.body)
        assertFreshParameters(fixture.plainConstructor, copy, listOf(ConePrimitiveType.INT64, fixture.callbackType))
        val stub = requireNotNull(copy.valueParameters[1].defaultValue)
        assertTrue(stub is CfirExpressionStub)
        assertEquals(fixture.callbackType, stub.coneTypeOrNull)
        assertOriginalUnchanged(fixture.plainConstructor, input)

        fixture.plainConstructor.valueParameters[1].replaceDefaultValue(fixture.lambda(110L))

        assertSame(stub, copy.valueParameters[1].defaultValue)
    }

    /** F-bound 中的 T 必须先经 owner 替换为 Int64；自有 U 才指向副本 U，constructed 映射最后应用。 */
    @Test
    fun selfBoundSubstitutionChainsOwnThenOwnerThenConstructedClassMappings() {
        val fixture = Fixture()
        val constructorSymbol = CfirConstructorSymbol(CallableId(fixture.owner.symbol.classId, Name.special("<init>")))
        val own = fixture.typeParameter("U", constructorSymbol)
        val boundClass = ClassId(FqName("fakeoverrides"), Name.identifier("Bound"))
        fun bound(type: ConeCangJieType): CfirResolvedTypeRef = fixture.typeRef(
            ConeClassLikeType(ConeClassLikeLookupTagImpl(boundClass), listOf(type)),
        )
        val classBound = bound(fixture.classParameter.symbol.constructType())
        fixture.classParameter.replaceBounds(listOf(classBound))
        val ownBounds = listOf(bound(fixture.classParameter.symbol.constructType()), bound(own.symbol.constructType()))
        own.replaceBounds(ownBounds)
        val original = buildConstructor {
            source = fixture.constructor.source
            moduleData = fixture.module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes()
            status = fixture.constructor.status
            isLocal = false
            symbol = constructorSymbol
            returnTypeRef = fixture.typeRef(requireNotNull(fixture.constructor.returnTypeRef.coneTypeOrNull))
            typeParameters += buildConstructedClassTypeParameterRef { symbol = fixture.classParameter.symbol }
            typeParameters += own
        }.apply { containingClassForStaticMemberAttr = fixture.owner.symbol.toLookupTag() }
        val newSymbol = CfirConstructorSymbol(constructorSymbol.callableId)
        val ownerSubstitution = substitutorByMap(mapOf(fixture.classParameter.symbol to ConePrimitiveType.INT64), fixture.session)
        val (parameters, substitution) = CfirFakeOverrideGenerator.createNewTypeParametersAndSubstitutor(
            fixture.session, original, newSymbol, ownerSubstitution, CfirDeclarationOrigin.SubstitutionOverride.CallSite,
        )
        assertEquals(2, parameters.size)
        val copiedClass = parameters[0].symbol.cfir
        val copiedOwn = parameters[1].symbol.cfir
        assertSame(newSymbol, copiedClass.containingDeclarationSymbol)
        assertSame(newSymbol, copiedOwn.containingDeclarationSymbol)
        assertNotSame(fixture.classParameter.symbol, copiedClass.symbol)
        assertNotSame(own.symbol, copiedOwn.symbol)
        fun argument(ref: CfirTypeRef): ConeCangJieType =
            (requireNotNull(ref.coneTypeOrNull) as ConeClassLikeType).typeArguments.single().type
        assertEquals(ConePrimitiveType.INT64, argument(copiedClass.bounds.single()))
        assertEquals(ConePrimitiveType.INT64, argument(copiedOwn.bounds[0]))
        assertEquals(copiedOwn.symbol.constructType(), argument(copiedOwn.bounds[1]))
        assertEquals(ConePrimitiveType.INT64, substitution.substituteOrSelf(fixture.classParameter.symbol.constructType()))
        assertEquals(copiedOwn.symbol.constructType(), substitution.substituteOrSelf(own.symbol.constructType()))
        assertSame(classBound, fixture.classParameter.bounds.single())
        assertEquals(ownBounds, own.bounds)
        assertSame(fixture.owner.symbol, fixture.classParameter.containingDeclarationSymbol)
        assertSame(constructorSymbol, own.containingDeclarationSymbol)
    }

    /** 显式传入原自有 T 作为返回类型，省略新参数向量时也必须与重新创建的自有 T 一起重绑定。 */
    @Test
    fun functionFactoryRebindsExplicitOriginalOwnReturnTypeToFreshTypeParameter() {
        val fixture = Fixture()
        val (original, parameter) = fixture.genericFunction()
        val input = originalInput(original)
        val copy = CfirFakeOverrideGenerator.createCopyForCfirFunction(
            newSymbol = CfirNamedFunctionSymbol(original.symbol.callableId),
            baseFunction = original,
            derivedClassLookupTag = fixture.owner.symbol.toLookupTag(),
            session = fixture.session,
            origin = CfirDeclarationOrigin.SubstitutionOverride.CallSite,
            newDispatchReceiverType = fixture.receiver,
            newTypeParameters = null,
            newReturnType = parameter.symbol.constructType(),
            markAsOverride = true,
        )
        assertReboundOwnReturnType(original, parameter, copy)
        assertSame(
            copy.typeParameters.single().symbol,
            (copy.valueParameters.single().returnTypeRef.coneTypeOrNull as ConeTypeParameterType).lookupTag.typeParameterSymbol,
        )
        assertSame(copy.symbol, copy.valueParameters.single().containingDeclarationSymbol)
        assertNotSame(original.valueParameters.single().symbol, copy.valueParameters.single().symbol)
        assertOriginalUnchanged(original, input)
    }

    @Test
    fun propertyFactoryRebindsExplicitOriginalOwnReturnTypeToFreshTypeParameter() {
        val fixture = Fixture()
        val (original, parameter) = fixture.genericProperty()
        val originalTypeRef = original.returnTypeRef
        val originalAttributes = original.attributes
        val copy = CfirFakeOverrideGenerator.createCopyForCfirProperty(
            newSymbol = CfirPropertySymbol(original.symbol.callableId),
            baseProperty = original,
            derivedClassLookupTag = fixture.owner.symbol.toLookupTag(),
            session = fixture.session,
            origin = CfirDeclarationOrigin.SubstitutionOverride.CallSite,
            newDispatchReceiverType = fixture.receiver,
            newTypeParameters = null,
            newReturnType = parameter.symbol.constructType(),
        )
        assertReboundOwnReturnType(original, parameter, copy)
        assertSame(originalTypeRef, original.returnTypeRef)
        assertSame(originalAttributes, original.attributes)
        assertNotSame(original.attributes, copy.attributes)
    }

    @Test
    fun fieldFactoryRebindsExplicitOriginalOwnReturnTypeToFreshTypeParameter() {
        val fixture = Fixture()
        val (original, parameter) = fixture.genericField()
        val originalTypeRef = original.returnTypeRef
        val originalAttributes = original.attributes
        val copy = CfirFakeOverrideGenerator.createCopyForCfirField(
            newSymbol = CfirFieldVariableSymbol(original.symbol.callableId),
            baseField = original,
            derivedClassLookupTag = fixture.owner.symbol.toLookupTag(),
            session = fixture.session,
            origin = CfirDeclarationOrigin.SubstitutionOverride.CallSite,
            newDispatchReceiverType = fixture.receiver,
            newTypeParameters = null,
            newReturnType = parameter.symbol.constructType(),
        )
        assertReboundOwnReturnType(original, parameter, copy)
        assertSame(originalTypeRef, original.returnTypeRef)
        assertSame(originalAttributes, original.attributes)
        assertNotSame(original.attributes, copy.attributes)
    }

    /** 返回类型与自有参数的身份一致；原声明的 T、owner、上界和 phase 保持不变。 */
    private fun assertReboundOwnReturnType(
        original: CfirCallableDeclaration,
        originalParameter: CfirTypeParameter,
        copy: CfirCallableDeclaration,
    ) {
        val copiedParameter = (copy as CfirTypeParameterRefsOwner).typeParameters.single().symbol.cfir
        val returnedType = requireNotNull(copy.returnTypeRef.coneTypeOrNull) as ConeTypeParameterType
        assertNotSame(originalParameter, copiedParameter)
        assertNotSame(originalParameter.symbol, copiedParameter.symbol)
        assertSame(copy.symbol, copiedParameter.containingDeclarationSymbol)
        assertSame(copiedParameter.symbol, returnedType.lookupTag.typeParameterSymbol)
        assertNotSame(original.returnTypeRef, copy.returnTypeRef)
        assertEquals(CfirResolvePhase.STATUS, copiedParameter.resolvePhase)
        assertSame(originalParameter, (original as CfirTypeParameterRefsOwner).typeParameters.single().symbol.cfir)
        assertSame(original.symbol, originalParameter.containingDeclarationSymbol)
        assertSame(
            originalParameter.symbol,
            (original.returnTypeRef.coneTypeOrNull as ConeTypeParameterType).lookupTag.typeParameterSymbol,
        )
        assertEquals(CfirResolvePhase.BODY_RESOLVE, originalParameter.resolvePhase)
        assertSame(CfirDeclarationOrigin.Library, originalParameter.origin)
        assertTrue(originalParameter.bounds.isEmpty())
    }

    private fun parametersOf(declaration: CfirCallableDeclaration): List<CfirValueParameter> = when (declaration) {
        is CfirFunction -> declaration.valueParameters
        is CfirEnumConstructor -> declaration.valueParameters
        else -> error("The test requires a function or enum constructor")
    }

    /** 消费者观察每个参数的身份、顺序、owner 与替换类型，不依赖工厂内部 symbol map。 */
    private fun assertFreshParameters(
        original: CfirCallableDeclaration,
        copy: CfirCallableDeclaration,
        expectedTypes: List<ConeCangJieType>,
    ) {
        assertNotSame(original, copy)
        assertNotSame(original.symbol, copy.symbol)
        assertNotSame(original.attributes, copy.attributes)
        val originals = parametersOf(original)
        val copies = parametersOf(copy)
        assertEquals(originals.size, copies.size)
        assertEquals(expectedTypes.size, copies.size)
        copies.forEachIndexed { index, parameter ->
            assertNotSame(originals[index], parameter)
            assertNotSame(originals[index].symbol, parameter.symbol)
            assertNotSame(originals[index].attributes, parameter.attributes)
            assertEquals(originals[index].name, parameter.name)
            assertEquals(copy.resolvePhase, parameter.resolvePhase)
            assertSame(copy.symbol, parameter.containingDeclarationSymbol)
            assertSame(original.symbol, originals[index].containingDeclarationSymbol)
            assertEquals(expectedTypes[index], parameter.returnTypeRef.coneTypeOrNull)
        }
    }

    private fun assertLazyCopyStatus(original: CfirCallableDeclaration, copy: CfirCallableDeclaration) {
        assertEquals(CfirResolvePhase.BODY_RESOLVE, original.resolvePhase)
        assertEquals(CfirResolvePhase.STATUS, copy.resolvePhase)
        assertTrue(copy.status is CfirResolvedDeclarationStatus)
        assertNotSame(original.status, copy.status)
        assertEquals(original.status.visibility, copy.status.visibility)
        assertEquals(original.status.modality, copy.status.modality)
        assertEquals(original.status.isUnsafe, copy.status.isUnsafe)
        assertFalse(original.status.isOverride)
    }

    private data class OriginalInput(
        val phase: CfirResolvePhase,
        val status: CfirDeclarationStatus,
        val attributes: CfirDeclarationAttributes,
        val facts: CfirDeclarationExportFacts,
        val typeRef: CfirTypeRef,
        val receiver: ConeSimpleCangJieType?,
        val body: CfirBlock?,
        val parameters: List<CfirValueParameter>,
        val parameterTypes: List<CfirTypeRef>,
        val defaults: List<CfirExpression?>,
        val metadata: List<CfirParameterDefaultValueMetadata?>,
        val syntaxFacts: List<CfirDefaultAssignmentSyntaxFacts?>,
    )

    private fun originalInput(declaration: CfirCallableDeclaration): OriginalInput {
        val parameters = parametersOf(declaration).toList()
        return OriginalInput(
            declaration.resolvePhase, declaration.status, declaration.attributes, declaration.exportFacts,
            declaration.returnTypeRef, declaration.dispatchReceiverType, (declaration as? CfirFunction)?.body,
            parameters, parameters.map { it.returnTypeRef }, parameters.map { it.defaultValue },
            parameters.map { it.defaultValueMetadata }, parameters.map { it.defaultAssignmentSyntaxFacts },
        )
    }

    private fun assertOriginalUnchanged(declaration: CfirCallableDeclaration, input: OriginalInput) {
        assertEquals(input.phase, declaration.resolvePhase)
        assertSame(input.status, declaration.status)
        assertSame(input.attributes, declaration.attributes)
        assertSame(input.facts, declaration.exportFacts)
        assertSame(input.typeRef, declaration.returnTypeRef)
        assertSame(input.receiver, declaration.dispatchReceiverType)
        assertSame(input.body, (declaration as? CfirFunction)?.body)
        val parameters = parametersOf(declaration)
        assertEquals(input.parameters.size, parameters.size)
        parameters.forEachIndexed { index, parameter ->
            assertSame(input.parameters[index], parameter)
            assertSame(input.parameterTypes[index], parameter.returnTypeRef)
            assertSame(input.defaults[index], parameter.defaultValue)
            assertSame(input.metadata[index], parameter.defaultValueMetadata)
            assertSame(input.syntaxFacts[index], parameter.defaultAssignmentSyntaxFacts)
            assertSame(declaration.symbol, parameter.containingDeclarationSymbol)
        }
    }

    /** 沿用 resolve scope fixture 的真实 session/建树方式；声明表只提供输入，不实现任何 copy 逻辑。 */
    private class Fixture {
        val session: CfirSession = object : CfirSession(Kind.Source) {}
        val module: CfirModuleData = CfirSourceModuleData(
            Name.identifier("fake-override-test"), emptyList(), emptyList(),
            CangJiePlatforms.defaultCangJiePlatform, CfirPlatform.DEFAULT,
        ).also {
            it.bindSession(session)
            session.register(CfirModuleData::class, it)
        }
        private val scopeProvider = CfirCangJieScopeProvider()
        private val packageName = FqName("fakeoverrides")
        private val ownerSymbol = CfirClassSymbol(ClassId(packageName, Name.identifier("Box")))
        private val enumSymbol = CfirEnumSymbol(ClassId(packageName, Name.identifier("Choice")))
        private val plainSymbol = CfirClassSymbol(ClassId(packageName, Name.identifier("Plain")))
        val classParameter = typeParameter("T", ownerSymbol)
        private val enumParameter = typeParameter("E", enumSymbol)
        private val originalOwnerType = ConeClassLikeType(
            ownerSymbol.toLookupTag(), listOf(classParameter.symbol.constructType()),
        )
        private val originalEnumType = ConeClassLikeType(enumSymbol.toLookupTag(), listOf(enumParameter.symbol.constructType()))
        val receiver = ConeClassLikeType(ownerSymbol.toLookupTag(), listOf(ConePrimitiveType.INT64))
        val enumReceiver = ConeClassLikeType(enumSymbol.toLookupTag(), listOf(ConePrimitiveType.INT64))
        val callbackType = ConeFunctionType(emptyList(), ConePrimitiveType.INT64)
        val firstDefault = function("defaultZeta", emptyList(), callbackType, block(lambda(7L), type = callbackType))
        val secondDefault = function("defaultAlpha", emptyList(), callbackType, block(lambda(8L), type = callbackType))
        val function = function(
            "convert", listOf(classParameter.symbol.constructType(), callbackType, callbackType),
            ConePrimitiveType.INT64, block(literal(1L)),
        ).also {
            default(it.valueParameters[1], firstDefault, 7L)
            default(it.valueParameters[2], secondDefault, 8L)
        }
        val constructor = constructor(ownerSymbol, originalOwnerType, classParameter)
        val plainConstructor = constructor(plainSymbol, ConeClassLikeType(plainSymbol.toLookupTag()), null)
        val variant: CfirEnumConstructor = buildEnumConstructor {
            source = TestSource("variant:Some")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            isLocal = false
            status = resolvedStatus()
            symbol = CfirEnumConstructorSymbol(CallableId(enumSymbol.classId, Name.identifier("Some")))
            name = Name.identifier("Some")
            returnTypeRef = typeRef(originalEnumType)
            typeParameters += buildConstructedClassTypeParameterRef { symbol = enumParameter.symbol }
            valueParameters += parameter(symbol, "value", enumParameter.symbol.constructType())
            valueParameters += parameter(symbol, "zeta", callbackType)
        }.also { default(it.valueParameters[1], firstDefault, 7L) }
        private val storage = buildFieldVariable {
            source = TestSource("field:storage")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            isLocal = false
            status = resolvedStatus()
            name = Name.identifier("storage")
            symbol = CfirFieldVariableSymbol(CallableId(ownerSymbol.classId, name))
            isVar = true
            returnTypeRef = typeRef(classParameter.symbol.constructType())
        }
        val property: CfirProperty = buildProperty {
            source = TestSource("property:item")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            isLocal = false
            status = resolvedStatus()
            name = Name.identifier("item")
            symbol = CfirPropertySymbol(CallableId(ownerSymbol.classId, name))
            returnTypeRef = typeRef(classParameter.symbol.constructType())
            dispatchReceiverType = originalOwnerType
            getter = accessor(symbol, getter = true)
            setter = accessor(symbol, getter = false)
        }
        val owner: CfirClass = buildClass {
            source = TestSource("class:Box")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            status = resolvedStatus()
            scopeProvider = this@Fixture.scopeProvider
            symbol = ownerSymbol
            name = symbol.classId.shortClassName
            typeParameters += classParameter
            declarations += listOf(function, constructor, storage, property)
        }
        private val enum = buildEnum {
            source = TestSource("enum:Choice")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            status = resolvedStatus()
            scopeProvider = this@Fixture.scopeProvider
            symbol = enumSymbol
            name = symbol.classId.shortClassName
            typeParameters += enumParameter
            declarations += variant
            isRefEnum = false
            isNonExhaustive = false
        }
        private val plain = buildClass {
            source = TestSource("class:Plain")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            status = resolvedStatus()
            scopeProvider = this@Fixture.scopeProvider
            symbol = plainSymbol
            name = symbol.classId.shortClassName
            declarations += plainConstructor
        }
        private val declarations = mutableListOf<CfirClassLikeSymbol<*>>(owner.symbol, enum.symbol, plain.symbol)
        init {
            session.register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
            session.register(CfirCangJieScopeProvider::class, scopeProvider)
            session.register(CfirExtendProvider::class, CfirEmptyExtendProvider())
            session.register(
                CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT),
            )
            session.register(TypeComponents::class, TypeComponents(session))
            val provider = DeclarationProvider(session, declarations)
            session.register(CfirSymbolProvider::class, provider)
            session.register(CfirProvider::class, CfirLibrarySessionProvider(provider))
        }
        val scope = CfirClassSubstitutionScope(session, DeclaredScope(owner.declarations), receiver)
        private val enumScope = CfirClassSubstitutionScope(session, DeclaredScope(enum.declarations), enumReceiver)

        fun functionCopy(): CfirNamedFunction = mutableListOf<CfirNamedFunctionSymbol>().also {
            scope.processFunctionsByName(function.name, it::add)
        }.single().cfir

        fun constructorCopy(): CfirConstructor = mutableListOf<CfirConstructorSymbol>().also {
            scope.processDeclaredConstructors(it::add)
        }.single().cfir

        fun enumCopy(): CfirEnumConstructor = mutableListOf<CfirCallableSymbol<*>>().also {
            enumScope.processCallablesByName(variant.name, it::add)
        }.single().let { (it as CfirEnumConstructorSymbol).cfir }

        fun propertyCopy(): CfirProperty = mutableListOf<CfirPropertySymbol>().also {
            scope.processPropertiesByName(property.name, it::add)
        }.single().cfir

        fun typeRef(type: ConeCangJieType): CfirResolvedTypeRef = buildResolvedTypeRef {
            source = TestSource("type")
            coneType = type
        }

        fun typeParameter(
            name: String,
            owner: CfirBasedSymbol<*>,
            origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
        ): CfirTypeParameter = buildTypeParameter {
            source = TestSource("type-parameter:$name")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            this.origin = origin
            attributes = CfirDeclarationAttributes()
            this.name = Name.identifier(name)
            symbol = CfirTypeParameterSymbol()
            containingDeclarationSymbol = owner
        }

        fun literal(value: Long): CfirExpression = buildLiteralExpression {
            source = TestSource("literal:$value")
            coneTypeOrNull = ConePrimitiveType.INT64
            kind = CfirLiteralKind.INT
            this.value = value
        }

        fun block(vararg expressions: CfirExpression, type: ConeCangJieType = ConePrimitiveType.INT64): CfirBlock = buildBlock {
            source = TestSource("body")
            coneTypeOrNull = type
            statements += expressions
        }

        /** 完整零参数 lambda 返回 Int64；默认 assignment 和 helper 正文各自构造其来源节点。 */
        fun lambda(value: Long): CfirAnonymousFunctionExpression = buildAnonymousFunctionExpression {
            source = TestSource("lambda:$value")
            isTrailingLambda = false
            anonymousFunction = buildAnonymousFunction {
                source = TestSource("lambda-function:$value")
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                isLocal = true
                symbol = CfirAnonymousFunctionSymbol()
                hasExplicitParameterList = true
                isLambda = true
                returnTypeRef = this@Fixture.typeRef(ConePrimitiveType.INT64)
                typeRef = this@Fixture.typeRef(callbackType)
                body = block(literal(value))
            }
        }

        private fun parameter(
            owner: CfirBasedSymbol<*>,
            name: String,
            type: ConeCangJieType,
            origin: CfirDeclarationOrigin = CfirDeclarationOrigin.Source,
        ): CfirValueParameter = buildValueParameter {
            source = TestSource("parameter:$name")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            this.origin = origin
            attributes = CfirDeclarationAttributes()
            isLocal = false
            isNamed = name != "value"
            this.name = Name.identifier(name)
            symbol = CfirValueParameterSymbol(CallableId(this.name))
            containingDeclarationSymbol = owner
            returnTypeRef = typeRef(type)
        }

        private fun default(parameter: CfirValueParameter, helper: CfirNamedFunction, value: Long) {
            parameter.replaceDefaultValue(lambda(value))
            parameter.defaultValueMetadata = CfirParameterDefaultValueMetadata(hasInitial = true, function = helper)
            parameter.defaultAssignmentSyntaxFacts = CfirDefaultAssignmentSyntaxFacts(isLiteral = false, literalConst = false)
        }

        private fun function(
            name: String,
            types: List<ConeCangJieType>,
            result: ConeCangJieType,
            body: CfirBlock,
        ): CfirNamedFunction = buildNamedFunction {
            source = TestSource("function:$name")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            isLocal = false
            status = resolvedStatus()
            this.name = Name.identifier(name)
            symbol = CfirNamedFunctionSymbol(CallableId(ownerSymbol.classId, this.name))
            isMut = false
            dispatchReceiverType = originalOwnerType
            returnTypeRef = typeRef(result)
            types.forEachIndexed { index, type ->
                valueParameters += parameter(symbol, listOf("value", "zeta", "alpha")[index], type)
            }
            this.body = body
        }

        private fun constructor(
            owner: CfirClassSymbol,
            result: ConeClassLikeType,
            borrowed: CfirTypeParameter?,
        ): CfirConstructor = buildConstructor {
            source = TestSource("constructor:${owner.classId}")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            isLocal = false
            status = resolvedStatus()
            symbol = CfirConstructorSymbol(CallableId(owner.classId, Name.special("<init>")))
            returnTypeRef = typeRef(result)
            borrowed?.let { parameter ->
                typeParameters += buildConstructedClassTypeParameterRef { symbol = parameter.symbol }
            }
            valueParameters += parameter(symbol, "value", borrowed?.symbol?.constructType() ?: ConePrimitiveType.INT64)
            valueParameters += parameter(symbol, "zeta", callbackType)
            body = block(literal(1L), type = ConePrimitiveType.UNIT)
        }.also {
            // 与 ConversionUtils.initContainingClassAttr 和 loader 相同，构造器的 owner 独立于 receiver。
            it.containingClassForStaticMemberAttr = owner.toLookupTag()
            default(it.valueParameters[1], firstDefault, 7L)
        }

        private fun accessor(property: CfirPropertySymbol, getter: Boolean): CfirPropertyAccessor = buildPropertyAccessor {
            source = TestSource(if (getter) "getter" else "setter")
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Source
            attributes = CfirDeclarationAttributes()
            isLocal = false
            status = resolvedStatus()
            symbol = CfirPropertyAccessorSymbol()
            propertySymbol = property
            isGetter = getter
            dispatchReceiverType = originalOwnerType
            returnTypeRef = typeRef(if (getter) classParameter.symbol.constructType() else ConePrimitiveType.UNIT)
            if (getter) {
                body = block(buildNamedAccessExpression {
                    source = TestSource("storage-access")
                    coneTypeOrNull = classParameter.symbol.constructType()
                    calleeReference = buildResolvedNamedReference {
                        source = TestSource("storage-reference")
                        name = storage.name
                        resolvedSymbol = storage.symbol
                    }
                }, type = classParameter.symbol.constructType())
            } else {
                valueParameters += parameter(symbol, "value", classParameter.symbol.constructType())
                body = block(type = ConePrimitiveType.UNIT)
            }
        }

        fun alias(generic: Boolean): CfirTypeAlias {
            val symbol = CfirTypeAliasSymbol(ClassId(packageName, Name.identifier(if (generic) "AliasBox" else "AliasPlain")))
            val parameter = if (generic) typeParameter("A", symbol) else null
            return buildTypeAlias {
                source = TestSource("alias:${symbol.classId}")
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                scopeProvider = this@Fixture.scopeProvider
                status = resolvedStatus()
                this.symbol = symbol
                name = symbol.classId.shortClassName
                parameter?.let { typeParameters += it }
                expandedTypeRef = typeRef(if (generic) {
                    ConeClassLikeType(ownerSymbol.toLookupTag(), listOf(requireNotNull(parameter).symbol.constructType()))
                } else ConeClassLikeType(plainSymbol.toLookupTag()))
            }.also { declarations += it.symbol }
        }

        /** 库签名直接建树到 BODY_RESOLVE；自有 T 归各自 callable，与 class 的 T 是不同 symbol。 */
        fun genericFunction(): Pair<CfirNamedFunction, CfirTypeParameter> {
            val symbol = CfirNamedFunctionSymbol(CallableId(ownerSymbol.classId, Name.identifier("genericFunction")))
            val parameter = typeParameter("T", symbol, CfirDeclarationOrigin.Library)
            val function = buildNamedFunction {
                source = null
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes()
                isLocal = false
                status = resolvedStatus()
                this.symbol = symbol
                name = symbol.name
                isMut = false
                dispatchReceiverType = originalOwnerType
                typeParameters += parameter
                returnTypeRef = typeRef(parameter.symbol.constructType())
                valueParameters += this@Fixture.parameter(
                    symbol, "value", parameter.symbol.constructType(), CfirDeclarationOrigin.Library,
                )
            }
            return function to parameter
        }

        fun genericProperty(): Pair<CfirProperty, CfirTypeParameter> {
            val symbol = CfirPropertySymbol(CallableId(ownerSymbol.classId, Name.identifier("genericProperty")))
            val parameter = typeParameter("T", symbol, CfirDeclarationOrigin.Library)
            val property = buildProperty {
                source = null
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes()
                isLocal = false
                status = resolvedStatus()
                this.symbol = symbol
                name = symbol.name
                dispatchReceiverType = originalOwnerType
                typeParameters += parameter
                returnTypeRef = typeRef(parameter.symbol.constructType())
            }
            return property to parameter
        }

        fun genericField(): Pair<CfirFieldVariable, CfirTypeParameter> {
            val symbol = CfirFieldVariableSymbol(CallableId(ownerSymbol.classId, Name.identifier("genericField")))
            val parameter = typeParameter("T", symbol, CfirDeclarationOrigin.Library)
            val field = buildFieldVariable {
                source = null
                moduleData = module
                resolvePhase = CfirResolvePhase.BODY_RESOLVE
                origin = CfirDeclarationOrigin.Library
                attributes = CfirDeclarationAttributes()
                isLocal = false
                status = resolvedStatus()
                this.symbol = symbol
                name = symbol.name
                isVar = false
                dispatchReceiverType = originalOwnerType
                typeParameters += parameter
                returnTypeRef = typeRef(parameter.symbol.constructType())
            }
            return field to parameter
        }

        private fun resolvedStatus(): CfirResolvedDeclarationStatus = buildResolvedDeclarationStatus {
            visibility = Visibilities.Public
            modality = Modality.FINAL
            visibilityExplicitness = CfirModifierExplicitness.EXPLICIT
            modalityExplicitness = CfirModifierExplicitness.IMPLICIT
            bodyPresence = CfirBodyPresence.UNKNOWN
            isDeclarationFile = false
            isFromCommonPart = false
            isVisibilityExplicit = true
            isModalityExplicit = false
            isAbstractExplicit = false
            isOverride = false
            isOperator = false
            isStatic = false
            isConst = false
            isMut = false
            isUnsafe = true
            isForeign = false
            isC = false
            isCommon = false
            isSpecific = false
            isCommonWithDefault = false
            isRedef = false
            isDefault = false
            isAbstract = false
            isOpen = false
            isSealed = false
        }
    }

    /** 只列出声明输入，所有替换及缓存行为均由公开 CfirClassSubstitutionScope 执行。 */
    private class DeclaredScope(private val declarations: List<CfirDeclaration>) : CfirTypeScope() {
        override fun getCallableNames(): Set<Name> =
            declarations.mapNotNull { (it.symbol as? CfirCallableSymbol<*>)?.name }.toSet()
        override fun getClassifierNames(): Set<Name> = emptySet()
        override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
            declarations.filterIsInstance<CfirNamedFunction>().filter { it.name == name }.forEach { processor(it.symbol) }
        }
        override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
            declarations.filterIsInstance<CfirProperty>().filter { it.name == name }.forEach { processor(it.symbol) }
        }
        override fun processDeclaredConstructors(processor: (CfirConstructorSymbol) -> Unit) {
            declarations.filterIsInstance<CfirConstructor>().forEach { processor(it.symbol) }
        }
        override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
            declarations.mapNotNull { it.symbol as? CfirCallableSymbol<*> }.filter { it.name == name }.forEach(processor)
        }
        override fun processDirectOverriddenFunctionsWithBaseScope(
            functionSymbol: CfirNamedFunctionSymbol, processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction,
        ): ProcessorAction = ProcessorAction.NONE
        override fun processDirectOverriddenPropertiesWithBaseScope(
            propertySymbol: CfirPropertySymbol, processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction,
        ): ProcessorAction = ProcessorAction.NONE
        override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirTypeScope =
            DeclaredScope(declarations)
    }

    /** 类与 alias 的声明表；不伪造 owner/substitution/default 查询结果。 */
    @OptIn(CfirSymbolProviderInternals::class)
    private class DeclarationProvider(
        session: CfirSession,
        private val declarations: List<CfirClassLikeSymbol<*>>,
    ) : CfirSymbolProvider(session) {
        override val symbolNamesProvider = object : CfirSymbolNamesProviderWithoutCallables() {
            override val hasSpecificClassifierPackageNamesComputation = false
            override fun getPackageNames(): Set<String> = declarations.map { it.classId.packageFqName.asString() }.toSet()
            override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> =
                declarations.filter { it.classId.packageFqName == packageFqName }.map { it.classId.shortClassName }.toSet()
        }
        override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
            declarations.singleOrNull { it.classId == classId }
        override fun hasPackage(fqName: FqName): Boolean = declarations.any { it.classId.packageFqName == fqName }
        override fun getTopLevelCallableSymbolsTo(
            destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name,
        ) {}
        override fun getTopLevelFunctionSymbolsTo(
            destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name,
        ) {}
        override fun getTopLevelPropertySymbolsTo(
            destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name,
        ) {}
    }

    private class TestSource(identity: String) : CjBinarySourceElement(identity, null, identity)
}
