package org.cangnova.cangjie.resolve.calls.mpp

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.descriptors.Visibilities
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AbstractCjmpCheckerTest {
    private data class TypeParameter(val name: String) : CjmpTypeParameterSymbolMarker

    private enum class Trait {
        CLASS_LIKE,
        CLASS,
        ENUM,
        NAMED_FUNCTION,
        PAYLOAD_ENUM_CONSTRUCTOR,
        PROPERTY,
        VARIABLE,
    }

    private data class Declaration(
        val traits: Set<Trait> = emptySet(),
        val typeParameters: List<TypeParameter> = emptyList(),
        val modifiers: CjmpDeclarationModifiers = defaultModifiers,
        val superTypes: List<String> = emptyList(),
        val nonExhaustive: Boolean = false,
        val deprecatedAnnotation: String? = null,
        val annotationMatches: List<CjmpAnnotationMatch<String, String>> = emptyList(),
        val annotationAttributes: List<CjmpAnnotationAttribute<String>> = emptyList(),
        val genericConstraintIndices: List<Int> = emptyList(),
        val type: String? = null,
        val mutable: Boolean = false,
        val propertyMutable: Boolean = false,
        val postMatchReturnCompatibility: CjmpTypeCompatibility = CjmpTypeCompatibility.COMPATIBLE,
    ) : CjmpDeclarationSymbolMarker

    private class Context :
        CjmpCompatibilityContext<Declaration, TypeParameter, String>,
        CjmpAnnotationMatchingContext<Declaration, String, String> {
        override fun classLikeKind(declaration: Declaration): CjmpClassLikeKind? =
            if (Trait.CLASS_LIKE in declaration.traits) CjmpClassLikeKind.CLASS else null

        override fun isExtend(declaration: Declaration): Boolean = false

        override fun haveSameExtendKey(first: Declaration, second: Declaration): CjmpTypeCompatibility =
            CjmpTypeCompatibility.INCOMPATIBLE

        override fun callableKind(declaration: Declaration): CjmpCallableKind? = when {
            Trait.NAMED_FUNCTION in declaration.traits || Trait.PAYLOAD_ENUM_CONSTRUCTOR in declaration.traits ->
                CjmpCallableKind.FUNCTION
            Trait.PROPERTY in declaration.traits -> CjmpCallableKind.PROPERTY
            Trait.VARIABLE in declaration.traits -> CjmpCallableKind.VARIABLE
            else -> null
        }

        override fun isFunction(declaration: Declaration): Boolean =
            Trait.NAMED_FUNCTION in declaration.traits || Trait.PAYLOAD_ENUM_CONSTRUCTOR in declaration.traits

        override fun isConstructor(declaration: Declaration): Boolean = false

        override fun areEnumConstructors(specific: Declaration, common: Declaration): Boolean =
            Trait.PAYLOAD_ENUM_CONSTRUCTOR in specific.traits && Trait.PAYLOAD_ENUM_CONSTRUCTOR in common.traits

        override fun areEnumConstructorTypesEquivalent(
            specific: Declaration,
            common: Declaration,
            typeParameterMapping: Map<TypeParameter, TypeParameter>,
        ): CjmpTypeCompatibility = error("The compatibility checker tests do not match enum constructor signatures")

        override fun typeParameters(declaration: Declaration): List<TypeParameter> = declaration.typeParameters

        override fun valueParameters(declaration: Declaration): List<CjmpMatchingValueParameter<String>> = emptyList()

        override fun checkParameterDefaultValueConflict(): Boolean = true

        override fun areReturnTypesCompatible(
            specific: Declaration,
            common: Declaration,
            typeParameterMapping: Map<TypeParameter, TypeParameter>,
        ): CjmpTypeCompatibility = specific.postMatchReturnCompatibility

        override fun areTypesEquivalent(
            specific: String?,
            common: String?,
            typeParameterMapping: Map<TypeParameter, TypeParameter>,
        ): CjmpTypeCompatibility = when {
            specific == null || common == null || specific == unresolvedType || common == unresolvedType -> CjmpTypeCompatibility.UNRESOLVED
            specific == common -> CjmpTypeCompatibility.COMPATIBLE
            else -> CjmpTypeCompatibility.INCOMPATIBLE
        }

        override fun needToReportMissingBody(common: Declaration, specific: Declaration): Boolean = false

        override fun isClassLike(declaration: Declaration): Boolean = Trait.CLASS_LIKE in declaration.traits

        override fun isClass(declaration: Declaration): Boolean = Trait.CLASS in declaration.traits

        override fun isEnum(declaration: Declaration): Boolean = Trait.ENUM in declaration.traits

        override fun isNamedFunction(declaration: Declaration): Boolean =
            Trait.NAMED_FUNCTION in declaration.traits

        override fun isPayloadEnumConstructor(declaration: Declaration): Boolean =
            Trait.PAYLOAD_ENUM_CONSTRUCTOR in declaration.traits

        override fun isProperty(declaration: Declaration): Boolean = Trait.PROPERTY in declaration.traits

        override fun isVariable(declaration: Declaration): Boolean = Trait.VARIABLE in declaration.traits

        override fun isMutableProperty(declaration: Declaration): Boolean = declaration.propertyMutable

        override fun modifiers(declaration: Declaration): CjmpDeclarationModifiers = declaration.modifiers

        override fun superTypes(declaration: Declaration): List<String> = declaration.superTypes

        override fun isNonExhaustiveEnum(declaration: Declaration): Boolean = declaration.nonExhaustive

        override fun deprecatedAnnotation(declaration: Declaration): String? = declaration.deprecatedAnnotation

        override fun annotations(declaration: Declaration): List<CjmpAnnotationMatch<String, String>> =
            declaration.annotationMatches

        override fun nonSerializedAnnotationAttributes(declaration: Declaration): List<CjmpAnnotationAttribute<String>> =
            declaration.annotationAttributes

        override fun areAnnotationArgumentsEqual(specific: String, common: String): Boolean = specific == common

        override fun incompatibleGenericConstraintIndices(
            specific: Declaration,
            common: Declaration,
            typeParameterMapping: Map<TypeParameter, TypeParameter>,
        ): List<Int> = specific.genericConstraintIndices

        override fun propertyOrVariableType(declaration: Declaration): String? = declaration.type

        override fun isMutableVariable(declaration: Declaration): Boolean = declaration.mutable

        override fun postMatchReturnTypeCompatibility(
            specific: Declaration,
            common: Declaration,
            typeParameterMapping: Map<TypeParameter, TypeParameter>,
        ): CjmpTypeCompatibility = specific.postMatchReturnCompatibility
    }

    @Test
    fun `equal supertype counts do not conceal different types`() {
        val common = Declaration(traits = setOf(Trait.CLASS_LIKE, Trait.CLASS), superTypes = listOf("I"))
        val specific = common.copy(superTypes = listOf("J"))

        assertEquals(
            listOf(CjmpCompatibilityIssue.Supertypes),
            AbstractCjmpChecker.checkMatched(specific, common, emptyMap(), Context()),
        )
    }

    @Test
    fun `supertype declaration order does not affect compatibility`() {
        val common = Declaration(traits = setOf(Trait.CLASS_LIKE, Trait.CLASS), superTypes = listOf("I", "J"))
        val specific = common.copy(superTypes = listOf("J", "I"))

        assertEquals(emptyList<CjmpCompatibilityIssue>(), AbstractCjmpChecker.checkMatched(specific, common, emptyMap(), Context()))
    }

    @Test
    fun `one specific supertype cannot satisfy multiple common entries`() {
        val common = Declaration(traits = setOf(Trait.CLASS_LIKE, Trait.CLASS), superTypes = listOf("I", "I"))
        val specific = common.copy(superTypes = listOf("I", "J"))

        assertEquals(
            listOf(CjmpCompatibilityIssue.Supertypes),
            AbstractCjmpChecker.checkMatched(specific, common, emptyMap(), Context()),
        )
    }

    @Test
    fun `unresolved supertypes do not add a compatibility diagnostic`() {
        val common = Declaration(traits = setOf(Trait.CLASS_LIKE, Trait.CLASS), superTypes = listOf("I"))
        val specific = common.copy(superTypes = listOf(unresolvedType))

        assertEquals(emptyList<CjmpCompatibilityIssue>(), AbstractCjmpChecker.checkMatched(specific, common, emptyMap(), Context()))
    }

    @Test
    fun `unresolved entries cannot conceal a provable mismatch in either direction or order`() {
        val nominal = Declaration(traits = setOf(Trait.CLASS_LIKE, Trait.CLASS))
        for ((commonTypes, specificTypes) in listOf(
            listOf(unresolvedType, "I") to listOf("J", "K"),
            listOf("I", "J") to listOf(unresolvedType, "K"),
        )) {
            for (commonOrder in listOf(commonTypes, commonTypes.reversed())) {
                for (specificOrder in listOf(specificTypes, specificTypes.reversed())) {
                    assertEquals(
                        listOf(CjmpCompatibilityIssue.Supertypes),
                        AbstractCjmpChecker.checkMatched(
                            nominal.copy(superTypes = specificOrder),
                            nominal.copy(superTypes = commonOrder),
                            emptyMap(),
                            Context(),
                        ),
                    )
                }
            }
        }
    }

    @Test
    fun `different positive nominal generic arities are diagnosed after matching`() {
        val specific = Declaration(
            traits = setOf(Trait.CLASS_LIKE, Trait.CLASS),
            typeParameters = listOf(TypeParameter("U"), TypeParameter("V")),
            genericConstraintIndices = listOf(0),
        )
        val common = Declaration(
            traits = setOf(Trait.CLASS_LIKE, Trait.CLASS),
            typeParameters = listOf(TypeParameter("T")),
        )

        val issues = AbstractCjmpChecker.checkMatched(specific, common, emptyMap(), Context())

        assertEquals(listOf(CjmpCompatibilityIssue.GenericParameterCount), issues)
    }

    @Test
    fun `type compatibility precedes the independent annotation checker`() {
        val specific = Declaration(
            traits = setOf(Trait.NAMED_FUNCTION),
            deprecatedAnnotation = "Deprecated",
            annotationMatches = listOf(CjmpAnnotationMatch("specific", "Extra")),
            genericConstraintIndices = listOf(2),
            postMatchReturnCompatibility = CjmpTypeCompatibility.INCOMPATIBLE,
        )
        val common = Declaration(
            traits = setOf(Trait.NAMED_FUNCTION),
            annotationMatches = listOf(CjmpAnnotationMatch("common", "Inherited")),
        )

        val issues = AbstractCjmpChecker.checkMatched(specific, common, emptyMap(), Context())

        assertEquals(
            listOf(
                CjmpCompatibilityIssue.ReturnType,
                CjmpCompatibilityIssue.GenericConstraint(2),
            ),
            issues,
        )
        assertEquals(
            listOf(
                CjmpAnnotationMatchingIssue.DeprecatedAnnotation("Deprecated"),
                CjmpAnnotationMatchingIssue.AnnotationMismatch("Inherited", CjmpAnnotationMismatchSide.COMMON),
                CjmpAnnotationMatchingIssue.AnnotationMismatch("Extra", CjmpAnnotationMismatchSide.SPECIFIC),
            ),
            AbstractCjmpAnnotationMatchChecker.checkAnnotations(specific, common, Context()),
        )
    }

    @Test
    fun `variable type mutability generic and annotation checks retain their order`() {
        val specific = Declaration(
            traits = setOf(Trait.VARIABLE),
            annotationMatches = listOf(CjmpAnnotationMatch("specific", "Extra")),
            genericConstraintIndices = listOf(1),
            type = "SpecificType",
            mutable = true,
        )
        val common = Declaration(
            traits = setOf(Trait.VARIABLE),
            annotationMatches = listOf(CjmpAnnotationMatch("common", "Inherited")),
            type = "CommonType",
        )

        val issues = AbstractCjmpChecker.checkMatched(specific, common, emptyMap(), Context())

        assertEquals(
            listOf(
                CjmpCompatibilityIssue.DeclarationType,
                CjmpCompatibilityIssue.Mutability,
                CjmpCompatibilityIssue.GenericConstraint(1),
            ),
            issues,
        )
        assertEquals(
            listOf(
                CjmpAnnotationMatchingIssue.AnnotationMismatch("Inherited", CjmpAnnotationMismatchSide.COMMON),
                CjmpAnnotationMatchingIssue.AnnotationMismatch("Extra", CjmpAnnotationMismatchSide.SPECIFIC),
            ),
            AbstractCjmpAnnotationMatchChecker.checkAnnotations(specific, common, Context()),
        )
    }

    @Test
    fun `nominal annotations are compared after nominal compatibility checks`() {
        val common = Declaration(
            traits = setOf(Trait.CLASS_LIKE, Trait.CLASS),
            annotationMatches = listOf(CjmpAnnotationMatch("Tag", "level=1")),
        )
        val specific = common.copy(annotationMatches = listOf(CjmpAnnotationMatch("Tag", "level=2")))

        assertEquals(
            listOf(
                CjmpAnnotationMatchingIssue.AnnotationMismatch("level=1", CjmpAnnotationMismatchSide.COMMON),
                CjmpAnnotationMatchingIssue.AnnotationMismatch("level=2", CjmpAnnotationMismatchSide.SPECIFIC),
            ),
            AbstractCjmpAnnotationMatchChecker.checkAnnotations(specific, common, Context()),
        )
    }

    @Test
    fun `annotation matching compares arguments and preserves duplicate multiplicity`() {
        val common = Declaration(
            traits = setOf(Trait.NAMED_FUNCTION),
            annotationMatches = listOf(
                CjmpAnnotationMatch("Tag", "level=1"),
                CjmpAnnotationMatch("Tag", "level=1"),
            ),
        )
        val exactSpecific = common.copy()
        val differentArguments = common.copy(
            annotationMatches = listOf(
                CjmpAnnotationMatch("Tag", "level=1"),
                CjmpAnnotationMatch("Tag", "level=2"),
            ),
        )

        assertEquals(
            emptyList<CjmpAnnotationMatchingIssue.AnnotationMismatch<String>>(),
            AbstractCjmpAnnotationMatchChecker.checkAnnotations(exactSpecific, common, Context()),
        )
        assertEquals(
            listOf(
                CjmpAnnotationMatchingIssue.AnnotationMismatch("level=1", CjmpAnnotationMismatchSide.COMMON),
                CjmpAnnotationMatchingIssue.AnnotationMismatch("level=2", CjmpAnnotationMismatchSide.SPECIFIC),
            ),
            AbstractCjmpAnnotationMatchChecker.checkAnnotations(differentArguments, common, Context()),
        )
    }

    @Test
    fun `nonserialized attribute mismatches retain kind and specific annotation identity`() {
        val common = Declaration(
            annotationAttributes = listOf(CjmpAnnotationAttribute(BuiltInAnnotationKind.C, annotation = null)),
        )
        val specificWithoutAttribute = Declaration()
        assertEquals(
            listOf(
                CjmpAnnotationMatchingIssue.AnnotationAttributeMismatch(
                    BuiltInAnnotationKind.C,
                    annotation = null,
                ),
            ),
            AbstractCjmpAnnotationMatchChecker.checkAnnotations(specificWithoutAttribute, common, Context()),
        )

        val specificWithAttribute = Declaration(
            annotationAttributes = listOf(CjmpAnnotationAttribute(BuiltInAnnotationKind.C, annotation = "@C")),
        )
        assertEquals(
            listOf(
                CjmpAnnotationMatchingIssue.AnnotationAttributeMismatch(
                    BuiltInAnnotationKind.C,
                    annotation = "@C",
                ),
            ),
            AbstractCjmpAnnotationMatchChecker.checkAnnotations(specificWithAttribute, Declaration(), Context()),
        )
    }

    @Test
    fun `property mutability is reported before visibility while static remains first`() {
        val common = Declaration(
            traits = setOf(Trait.PROPERTY),
            propertyMutable = false,
        )
        val specificMutable = common.copy(
            modifiers = defaultModifiers.copy(visibility = Visibilities.Private, isMut = true),
            propertyMutable = true,
        )

        assertEquals(
            listOf(CjmpCompatibilityIssue.PropertyMutability(specificIsMutable = true)),
            AbstractCjmpChecker.checkMatched(specificMutable, common, emptyMap(), Context()),
        )

        val specificStatic = specificMutable.copy(
            modifiers = specificMutable.modifiers.copy(isStatic = true),
        )
        assertEquals(
            listOf(CjmpCompatibilityIssue.Modifiers),
            AbstractCjmpChecker.checkMatched(specificStatic, common, emptyMap(), Context()),
        )
    }

    @Test
    fun `common mutable property reports the mut requirement at the specific side`() {
        val common = Declaration(
            traits = setOf(Trait.PROPERTY),
            propertyMutable = true,
        )
        val specific = common.copy(propertyMutable = false)

        assertEquals(
            listOf(CjmpCompatibilityIssue.PropertyMutability(specificIsMutable = false)),
            AbstractCjmpChecker.checkMatched(specific, common, emptyMap(), Context()),
        )
    }

    @Test
    fun `nominal C modifier remains part of declaration compatibility`() {
        val common = Declaration(
            traits = setOf(Trait.CLASS_LIKE, Trait.CLASS),
        )
        val specific = common.copy(
            modifiers = common.modifiers.copy(isC = true),
        )

        assertEquals(
            listOf(CjmpCompatibilityIssue.Modifiers),
            AbstractCjmpChecker.checkMatched(specific, common, emptyMap(), Context()),
        )
    }

    @Test
    fun `open abstract compatibility stop does not suppress independent annotation checks`() {
        val specific = Declaration(
            traits = setOf(Trait.NAMED_FUNCTION),
            modifiers = defaultModifiers.copy(isAbstract = true),
            annotationMatches = listOf(CjmpAnnotationMatch("specific", "Extra")),
            genericConstraintIndices = listOf(1),
        )
        val common = Declaration(
            traits = setOf(Trait.NAMED_FUNCTION),
            modifiers = defaultModifiers.copy(isOpen = true),
        )

        val issues = AbstractCjmpChecker.checkMatched(specific, common, emptyMap(), Context())

        assertEquals(listOf(CjmpCompatibilityIssue.OpenAbstractMember), issues)
        assertEquals(
            listOf(CjmpAnnotationMatchingIssue.AnnotationMismatch("Extra", CjmpAnnotationMismatchSide.SPECIFIC)),
            AbstractCjmpAnnotationMatchChecker.checkAnnotations(specific, common, Context()),
        )
    }

    @Test
    fun `classlike supertype mismatch stops enum exhaustiveness check`() {
        val specific = Declaration(
            traits = setOf(Trait.CLASS_LIKE, Trait.ENUM),
            superTypes = listOf("I"),
            nonExhaustive = true,
        )
        val common = Declaration(
            traits = setOf(Trait.CLASS_LIKE, Trait.ENUM),
            nonExhaustive = false,
        )

        val issues = AbstractCjmpChecker.checkMatched(specific, common, emptyMap(), Context())

        assertEquals(listOf(CjmpCompatibilityIssue.Supertypes), issues)
    }

    private companion object {
        const val unresolvedType = "<unresolved>"

        val defaultModifiers = CjmpDeclarationModifiers(
            visibility = Visibilities.Public,
            isAbstract = false,
            isOpen = false,
            isSealed = false,
            isC = false,
            isStatic = false,
            isMut = false,
            isForeign = false,
            isUnsafe = false,
        )
    }
}
