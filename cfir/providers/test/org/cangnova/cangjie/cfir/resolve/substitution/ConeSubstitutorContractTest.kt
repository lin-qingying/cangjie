package org.cangnova.cangjie.cfir.resolve.substitution

import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/** 通过共同替换 API 验证使用点属性、参数身份及完整结构；不读取替换器内部映射。 */
class ConeSubstitutorContractTest {
    private val session = object : CfirSession(Kind.Source) {}.also {
        it.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
        it.register(TypeComponents::class, TypeComponents(it))
    }

    @Test
    fun constructorMapPreservesTargetAndUseSiteAttributes() {
        val parameter = CfirTypeParameterSymbol()
        val source = ConeTypeParameterTypeImpl(parameter.toLookupTag(), attributes("source"))
        val target = ConePrimitiveType.INT64.withAttributes(attributes("target"))
        val result = createTypeSubstitutorByTypeConstructor(mapOf(source.lookupTag to target), session.typeContext, false)
            .substituteOrSelf(source)
        assertEquals(listOf("target", "source"), trace(result))
        assertEquals(listOf("source"), trace(source))
        assertEquals(listOf("target"), trace(target))
    }

    @Test
    fun symbolMapPreservesTheSameAttributeContract() {
        val parameter = CfirTypeParameterSymbol()
        val source = ConeTypeParameterTypeImpl(parameter.toLookupTag(), attributes("source"))
        val target = ConePrimitiveType.INT64.withAttributes(attributes("target"))
        val result = substitutorByMap(mapOf(parameter to target), session).substituteOrSelf(source)
        assertEquals(listOf("target", "source"), trace(result))
        assertEquals(listOf("source"), trace(source))
        assertEquals(listOf("target"), trace(target))
    }

    @Test
    fun declarationsWithIndependentTypeParametersRetainIndependentIdentity() {
        val selected = CfirTypeParameterSymbol()
        val external = CfirTypeParameterSymbol()
        val selectedType = ConeTypeParameterTypeImpl(selected.toLookupTag())
        val externalType = ConeTypeParameterTypeImpl(external.toLookupTag())
        val substitutor = createTypeSubstitutorByTypeConstructor(mapOf(selectedType.lookupTag to ConePrimitiveType.INT64), session.typeContext, false)
        val input = ConeTupleType(listOf(selectedType, externalType))
        val result = substitutor.substituteOrSelf(input) as ConeTupleType
        assertSame(ConePrimitiveType.INT64, result.elementTypes[0])
        assertSame(externalType, result.elementTypes[1])
        assertSame(selectedType, input.elementTypes[0])
    }

    @Test
    fun substitutionTraversesFunctionTuplePointerAndVarrayWithoutLosingTheirRoles() {
        val parameter = CfirTypeParameterSymbol()
        val source = ConeTypeParameterTypeImpl(parameter.toLookupTag())
        val pointer = ConePointerType(source, attributes("pointer"))
        val array = ConeVArrayType(source, 4, attributes("array"))
        val resultType = ConeTupleType(listOf(pointer, array), attributes("tuple"))
        val input = ConeFunctionType(listOf(source), resultType, isCFunc = true, hasVariableLenArg = true, attributes = attributes("function"))
        val output = createTypeSubstitutorByTypeConstructor(mapOf(source.lookupTag to ConePrimitiveType.INT64), session.typeContext, false)
            .substituteOrSelf(input) as ConeFunctionType
        assertTrue(output.isCFunc)
        assertTrue(output.hasVariableLenArg)
        assertEquals(listOf("function"), trace(output))
        assertSame(ConePrimitiveType.INT64, output.parameterTypes.single())
        val tuple = output.returnType as ConeTupleType
        assertEquals(listOf("tuple"), trace(tuple))
        assertSame(ConePrimitiveType.INT64, (tuple.elementTypes[0] as ConePointerType).pointeeType)
        assertEquals(listOf("pointer"), trace(tuple.elementTypes[0]))
        assertSame(ConePrimitiveType.INT64, (tuple.elementTypes[1] as ConeVArrayType).elementType)
        assertEquals(4L, (tuple.elementTypes[1] as ConeVArrayType).size)
        assertEquals(listOf("array"), trace(tuple.elementTypes[1]))
        assertSame(source, input.parameterTypes.single())
    }

    @Test
    fun aTypeStoredInsideAnAttributeIsSubstitutedWithItsCarrier() {
        val parameter = CfirTypeParameterSymbol()
        val source = ConeTypeParameterTypeImpl(parameter.toLookupTag())
        val input = ConeClassLikeType(
            ConeClassLikeLookupTagImpl(ClassId.topLevel(FqName("sample.Carrier"))),
            attributes = ConeAttributes.create(listOf(AbbreviatedTypeAttribute(ConeTupleType(listOf(source))))),
        )
        val result = createTypeSubstitutorByTypeConstructor(mapOf(source.lookupTag to ConePrimitiveType.INT64), session.typeContext, false)
            .substituteOrSelf(input)
        val updated = result.attributes.filterIsInstance<AbbreviatedTypeAttribute>().single().coneType as ConeTupleType
        assertSame(ConePrimitiveType.INT64, updated.elementTypes.single())
        val original = input.attributes.filterIsInstance<AbbreviatedTypeAttribute>().single().coneType as ConeTupleType
        assertSame(source, original.elementTypes.single())
    }

    @Test
    fun remappingOriginalAttributeTypesPreservesReplacementAttributes() {
        val parameter = CfirTypeParameterSymbol()
        val bareParameter = ConeTypeParameterTypeImpl(parameter.toLookupTag())
        val source = ConeTypeParameterTypeImpl(
            parameter.toLookupTag(),
            ConeAttributes.create(listOf(TraceAttribute(listOf("source")), AbbreviatedTypeAttribute(ConeTupleType(listOf(bareParameter))))),
        )
        val target = ConePrimitiveType.INT64.withAttributes(attributes("target"))
        val result = createTypeSubstitutorByTypeConstructor(mapOf(source.lookupTag to target), session.typeContext, false)
            .substituteOrSelf(source)
        assertEquals(listOf("target", "source"), trace(result))
        val abbreviation = result.attributes.filterIsInstance<AbbreviatedTypeAttribute>().single().coneType as ConeTupleType
        assertEquals(PrimitiveTypeKind.INT64, (abbreviation.elementTypes.single() as ConePrimitiveType).kind)
        assertEquals(listOf("target"), trace(abbreviation.elementTypes.single()))
        assertSame(bareParameter, (source.attributes.filterIsInstance<AbbreviatedTypeAttribute>().single().coneType as ConeTupleType).elementTypes.single())
        assertEquals(listOf("target"), trace(target))
    }

    @Test
    fun replacementAttributesCanRetainTheOriginalParameterWithoutRecursiveExpansion() {
        val parameter = CfirTypeParameterSymbol()
        val source = ConeTypeParameterTypeImpl(parameter.toLookupTag())
        val target = ConePrimitiveType.INT64.withAttributes(
            ConeAttributes.create(listOf(AbbreviatedTypeAttribute(ConeTupleType(listOf(source))))),
        )
        val result = createTypeSubstitutorByTypeConstructor(mapOf(source.lookupTag to target), session.typeContext, false)
            .substituteOrSelf(source)
        assertSame(target, result)
        val abbreviation = result.attributes.filterIsInstance<AbbreviatedTypeAttribute>().single().coneType as ConeTupleType
        assertSame(source, abbreviation.elementTypes.single())
    }

    @Test
    fun emptyMapsPublishTheCanonicalEmptySubstitutor() {
        assertSame(ConeSubstitutor.Empty, createTypeSubstitutorByTypeConstructor(emptyMap(), session.typeContext, false))
        assertSame(ConeSubstitutor.Empty, substitutorByMap(emptyMap(), session))
    }

    @Test
    fun inferenceContextUsesTheSameStubConstructorAndAttributeContract() {
        val variable = ConeTypeVariable("E")
        val source = ConeStubType(variable.typeConstructor, ConeStubType.Kind.FOR_SUBTYPING, attributes("source"))
        val target = ConePrimitiveType.INT64.withAttributes(attributes("target"))
        val substitutor = session.typeContext.typeSubstitutorByTypeConstructor(mapOf(variable.typeConstructor to target)).asCone()
        assertEquals(listOf("target", "source"), trace(substitutor.substituteOrSelf(source)))
        assertEquals(listOf("source"), trace(source))
        assertEquals(listOf("target"), trace(target))
    }

    @Test
    fun variablesWithTheSameDebugNameRetainDistinctConstructorIdentity() {
        val selected = ConeTypeVariable("E")
        val external = ConeTypeVariable("E")
        val substitutor = session.typeContext.typeSubstitutorByTypeConstructor(mapOf(selected.typeConstructor to ConePrimitiveType.INT64)).asCone()
        assertSame(ConePrimitiveType.INT64, substitutor.substituteOrSelf(selected.defaultType))
        assertSame(external.defaultType, substitutor.substituteOrSelf(external.defaultType))
    }

    @Test
    fun explicitIdentityMappingCanRetainThePublicMapContract() {
        val parameter = CfirTypeParameterSymbol()
        val source = ConeTypeParameterTypeImpl(parameter.toLookupTag())
        assertSame(ConeSubstitutor.Empty, substitutorByMap(mapOf(parameter to source), session))
        val explicit = substitutorByMap(mapOf(parameter to source), session, allowIdenticalSubstitution = true)
        assertNotSame(ConeSubstitutor.Empty, explicit)
        assertSame(source, explicit.substituteOrSelf(source))
    }

    @Test
    fun structuralConstructorMarkersDoNotReplaceAnUnrelatedPrimitiveHead() {
        val input = ConePrimitiveType.INT64
        val substitutor = createTypeSubstitutorByTypeConstructor(mapOf(input to ConePrimitiveType.BOOLEAN), session.typeContext, false)
        assertSame(input, substitutor.substituteOrSelf(input))
    }

    private fun attributes(vararg labels: String) = ConeAttributes.create(listOf(TraceAttribute(labels.toList())))
    private fun trace(type: ConeCangJieType): List<String> = type.attributes.filterIsInstance<TraceAttribute>().single().labels

    /** 两个语义来源可独立观察；合并顺序对应目标类型和原使用点。 */
    private data class TraceAttribute(val labels: List<String>) : ConeAttribute<TraceAttribute>() {
        override fun union(other: TraceAttribute?): TraceAttribute? = other?.let { TraceAttribute((labels + it.labels).distinct()) }
        override fun intersect(other: TraceAttribute?): TraceAttribute? = other?.let { TraceAttribute(labels.intersect(it.labels.toSet()).toList()) }
        override fun add(other: TraceAttribute?): TraceAttribute = other?.let { TraceAttribute((labels + it.labels).distinct()) } ?: this
        override fun isSubtypeOf(other: TraceAttribute?): Boolean = other == null || labels.containsAll(other.labels)
        override val key: KClass<out TraceAttribute> get() = TraceAttribute::class
        override val keepInInferredDeclarationType: Boolean get() = true
        override val implementsEquality: Boolean get() = true
    }
}
