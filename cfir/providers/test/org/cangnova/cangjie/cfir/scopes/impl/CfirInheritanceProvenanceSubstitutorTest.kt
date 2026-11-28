package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.cfir.types.ConeCStringType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.name.ClassId
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CfirInheritanceProvenanceSubstitutorTest {
    private val typeContext = object : ConeInferenceContext {}
    @Test
    fun mapsNominalCpointerParameterToItsStructuralConeRepresentation() {
        val parameter = typeParameter()
        val provenance = ConeClassLikeType(
            lookupTag = ConeClassLikeLookupTagImpl(StdlibClassIds.CPointer),
            typeArguments = listOf(parameter),
        )
        val concrete = ConePointerType(ConePrimitiveType.INT8)

        val substitutor = provenance.provenanceToConcreteSubstitutor(concrete, typeContext)

        assertEquals(ConePrimitiveType.INT8, substitutor.substituteOrSelf(parameter))
    }

    @Test
    fun mapsStructuralCpointerParameterToItsNominalConeRepresentation() {
        val parameter = typeParameter()
        val provenance = ConePointerType(parameter)
        val concrete = ConeClassLikeType(
            lookupTag = ConeClassLikeLookupTagImpl(StdlibClassIds.CPointer),
            typeArguments = listOf(ConePrimitiveType.INT8),
        )

        val substitutor = provenance.provenanceToConcreteSubstitutor(concrete, typeContext)

        assertEquals(ConePrimitiveType.INT8, substitutor.substituteOrSelf(parameter))
    }

    @Test
    fun mapsOwnerParametersFromOrdinaryClassToThisView() {
        val parameter = typeParameter()
        val provenance = boxed(parameter)
        val concrete = boxed(ConePrimitiveType.INT64).copyAsThisType()

        val substitutor = provenance.provenanceToConcreteSubstitutor(concrete, typeContext)

        assertEquals(ConePrimitiveType.INT64, substitutor.substituteOrSelf(parameter))
    }

    @Test
    fun mapsOwnerParametersFromThisViewToOrdinaryClass() {
        val parameter = typeParameter()
        val provenance = boxed(parameter).copyAsThisType()
        val concrete = boxed(ConePrimitiveType.INT64)

        val substitutor = provenance.provenanceToConcreteSubstitutor(concrete, typeContext)

        assertEquals(ConePrimitiveType.INT64, substitutor.substituteOrSelf(parameter))
    }

    @Test
    fun mapsStructOwnerParametersAcrossItsClassLikeUseSiteView() {
        val parameter = typeParameter()
        val classId = StdlibClassIds.Array
        val provenance = ConeStructType(
            lookupTag = ConeClassLikeLookupTagImpl(classId),
            typeArguments = listOf(parameter),
        )
        val concrete = ConeClassLikeType(
            lookupTag = ConeClassLikeLookupTagImpl(classId),
            typeArguments = listOf(ConePrimitiveType.INT64),
        )

        val substitutor = provenance.provenanceToConcreteSubstitutor(concrete, typeContext)

        assertEquals(ConePrimitiveType.INT64, substitutor.substituteOrSelf(parameter))
    }

    @Test
    fun mapsEnumOwnerParametersAcrossItsClassLikeUseSiteView() {
        val parameter = typeParameter()
        val classId = StdlibClassIds.Option
        val provenance = ConeEnumType(
            lookupTag = ConeClassLikeLookupTagImpl(classId),
            typeArguments = listOf(parameter),
            isRefEnum = false,
        )
        val concrete = ConeClassLikeType(
            lookupTag = ConeClassLikeLookupTagImpl(classId),
            typeArguments = listOf(ConePrimitiveType.INT64),
        )

        val substitutor = provenance.provenanceToConcreteSubstitutor(concrete, typeContext)

        assertEquals(ConePrimitiveType.INT64, substitutor.substituteOrSelf(parameter))
    }

    @Test
    fun acceptsCStringBuiltinOwnerThroughItsClassLikeDeclarationView() {
        val provenance = ConeClassLikeType(
            lookupTag = ConeClassLikeLookupTagImpl(StdlibClassIds.CString),
        )

        assertDoesNotThrow { provenance.provenanceToConcreteSubstitutor(ConeCStringType(), typeContext) }
    }

    @Test
    fun rejectsStructAndEnumViewsForOneOwnerIdentity() {
        val classId = ClassId.fromString("sample/Declaration")
        val provenance = ConeStructType(
            lookupTag = ConeClassLikeLookupTagImpl(classId),
            typeArguments = listOf(ConePrimitiveType.INT64),
        )
        val concrete = ConeEnumType(
            lookupTag = ConeClassLikeLookupTagImpl(classId),
            typeArguments = listOf(ConePrimitiveType.INT64),
            isRefEnum = false,
        )

        assertThrows(IllegalStateException::class.java) {
            provenance.provenanceToConcreteSubstitutor(concrete, typeContext)
        }
    }

    @Test
    fun rejectsDifferentRefEnumOwnerViews() {
        val classId = ClassId.fromString("sample/ReferenceEnum")
        val provenance = ConeEnumType(
            lookupTag = ConeClassLikeLookupTagImpl(classId),
            typeArguments = listOf(ConePrimitiveType.INT64),
            isRefEnum = false,
        )
        val concrete = ConeEnumType(
            lookupTag = ConeClassLikeLookupTagImpl(classId),
            typeArguments = listOf(ConePrimitiveType.INT64),
            isRefEnum = true,
        )

        assertThrows(IllegalStateException::class.java) {
            provenance.provenanceToConcreteSubstitutor(concrete, typeContext)
        }
    }

    @Test
    fun acceptsTheNominalBuiltinClassifierFormOfAPrimitiveOwner() {
        val provenance = ConePrimitiveType.INT8
        val concrete = ConeClassLikeType(
            lookupTag = ConeClassLikeLookupTagImpl(ConePrimitiveType.INT8.kind.classId),
        )

        assertDoesNotThrow { provenance.provenanceToConcreteSubstitutor(concrete, typeContext) }
    }

    @Test
    fun mapsUnionComponentsByStructureInsteadOfSetIterationOrder() {
        val parameter = typeParameter()
        val provenance = boxed(ConeUnionType(linkedSetOf(parameter, ConePrimitiveType.INT64)))
        val concrete = boxed(ConeUnionType(linkedSetOf(ConePrimitiveType.INT64, ConePrimitiveType.BOOLEAN)))

        val substitutor = provenance.provenanceToConcreteSubstitutor(concrete, typeContext)

        assertEquals(ConePrimitiveType.BOOLEAN, substitutor.substituteOrSelf(parameter))
        assertEquals(concrete.typeArguments.single().type, substitutor.substituteOrSelf(provenance.typeArguments.single().type))
    }

    @Test
    fun rejectsAnAmbiguousUnionParameterMapping() {
        val firstParameter = typeParameter()
        val secondParameter = typeParameter()
        val provenance = boxed(ConeUnionType(linkedSetOf(firstParameter, secondParameter)))
        val concrete = boxed(ConeUnionType(linkedSetOf(ConePrimitiveType.INT64, ConePrimitiveType.BOOLEAN)))

        assertThrows(IllegalStateException::class.java) {
            provenance.provenanceToConcreteSubstitutor(concrete, typeContext)
        }
    }

    @Test
    fun mapsIntersectionComponentsWithoutDependingOnCollectionOrder() {
        val parameter = typeParameter()
        val provenance = boxed(ConeIntersectionType(listOf(parameter, ConePrimitiveType.INT64)))
        val concrete = boxed(ConeIntersectionType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.BOOLEAN)))

        val substitutor = provenance.provenanceToConcreteSubstitutor(concrete, typeContext)

        assertEquals(ConePrimitiveType.BOOLEAN, substitutor.substituteOrSelf(parameter))
    }

    @Test
    fun rejectsAnAmbiguousIntersectionParameterMapping() {
        val firstParameter = typeParameter()
        val secondParameter = typeParameter()
        val provenance = boxed(ConeIntersectionType(listOf(firstParameter, secondParameter)))
        val concrete = boxed(ConeIntersectionType(listOf(ConePrimitiveType.INT64, ConePrimitiveType.BOOLEAN)))

        assertThrows(IllegalStateException::class.java) {
            provenance.provenanceToConcreteSubstitutor(concrete, typeContext)
        }
    }

    @Test
    fun rejectsDifferentNominalOwnerShapes() {
        val provenance = boxed(ConePrimitiveType.INT64)
        val concrete = ConeClassLikeType(
            lookupTag = ConeClassLikeLookupTagImpl(ClassId.fromString("sample/Other")),
            typeArguments = listOf(ConePrimitiveType.INT64),
        )

        assertThrows(IllegalStateException::class.java) {
            provenance.provenanceToConcreteSubstitutor(concrete, typeContext)
        }
    }

    private fun typeParameter() = ConeTypeParameterTypeImpl(CfirTypeParameterSymbol().toLookupTag())

    private fun boxed(type: org.cangnova.cangjie.cfir.types.ConeCangJieType) = ConeClassLikeType(
        lookupTag = ConeClassLikeLookupTagImpl(ClassId.fromString("sample/Box")),
        typeArguments = listOf(type),
    )

    private fun ConeClassLikeType.copyAsThisType() = ConeClassLikeType(
        lookupTag = lookupTag,
        typeArguments = typeArguments,
        attributes = attributes,
        isInterface = isInterface,
        isThisType = true,
    )
}
