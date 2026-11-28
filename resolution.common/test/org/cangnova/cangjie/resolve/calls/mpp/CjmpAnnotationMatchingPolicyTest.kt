package org.cangnova.cangjie.resolve.calls.mpp

import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CjmpAnnotationMatchingPolicyTest {
    @Test
    fun `v1 1 3 classification separates serialized attributes matching and prohibited annotations`() {
        val version = LanguageVersion.CANGJIE_1_1_3
        val unsupported = listOf(
            BuiltInAnnotationKind.JAVA,
            BuiltInAnnotationKind.CALLING_CONV,
            BuiltInAnnotationKind.CONSTSAFE,
            BuiltInAnnotationKind.ENSURE_PREPARED_TO_MOCK,
        )
        unsupported.forEach { kind ->
            assertTrue(CjmpAnnotationMatchingPolicy.isDisallowedOnCommonSpecific(identity(kind), version), "$kind")
            assertTrue(
                CjmpAnnotationMatchingPolicy.isHandledOutsideAnnotationMatching(kind, version),
                "$kind must not enter ordinary annotation matching",
            )
        }

        val allowed = listOf(
            BuiltInAnnotationKind.C,
            BuiltInAnnotationKind.JAVA_MIRROR,
            BuiltInAnnotationKind.JAVA_IMPL,
        )
        allowed.forEach { kind ->
            assertFalse(CjmpAnnotationMatchingPolicy.isDisallowedOnCommonSpecific(identity(kind), version), "$kind")
        }

        assertTrue(
            CjmpAnnotationMatchingPolicy.isDisallowedOnCommonSpecific(CangjieAnnotationIdentity.Unknown, version),
        )
        assertTrue(
            CjmpAnnotationMatchingPolicy.isHandledOutsideAnnotationMatching(CangjieAnnotationIdentity.Unknown, version),
        )
    }

    @Test
    fun `alpha twenty extends the prohibited declaration annotation set`() {
        val before = LanguageVersion.CANGJIE_1_2_0_ALPHA_19
        val alphaTwenty = LanguageVersion.CANGJIE_1_2_0_ALPHA_20
        val addedKinds = listOf(
            BuiltInAnnotationKind.FOREIGN_GETTER_NAME,
            BuiltInAnnotationKind.FOREIGN_SETTER_NAME,
            BuiltInAnnotationKind.NON_PRODUCT,
        )

        addedKinds.forEach { kind ->
            assertFalse(CjmpAnnotationMatchingPolicy.isDisallowedOnCommonSpecific(identity(kind), before), "$kind before alpha.20")
            assertTrue(CjmpAnnotationMatchingPolicy.isDisallowedOnCommonSpecific(identity(kind), alphaTwenty), "$kind at alpha.20")
        }
        assertTrue(
            CjmpAnnotationMatchingPolicy.isDisallowedOnCommonSpecific(
                CangjieAnnotationIdentity.PackageDirective(
                    org.cangnova.cangjie.annotations.CangjiePackageDirectiveKind.NON_PRODUCT,
                    "NonProduct",
                ),
                alphaTwenty,
            ),
        )
    }

    @Test
    fun `JavaMirror becomes a regularly matched annotation at alpha 02`() {
        assertTrue(
            CjmpAnnotationMatchingPolicy.isHandledOutsideAnnotationMatching(
                BuiltInAnnotationKind.JAVA_MIRROR,
                LanguageVersion.CANGJIE_1_1_3,
            ),
        )
        assertTrue(
            CjmpAnnotationMatchingPolicy.isHandledOutsideAnnotationMatching(
                BuiltInAnnotationKind.JAVA_MIRROR,
                LanguageVersion.CANGJIE_1_2_0_ALPHA_20,
            ),
        )
        assertFalse(
            CjmpAnnotationMatchingPolicy.isHandledOutsideAnnotationMatching(
                BuiltInAnnotationKind.JAVA_MIRROR,
                LanguageVersion.CANGJIE_1_3_0_ALPHA_02,
            ),
        )
    }

    @Test
    fun `ObjC declaration metadata follows its introduction boundary`() {
        val versionedKinds = listOf(
            BuiltInAnnotationKind.OBJ_C_INIT,
            BuiltInAnnotationKind.OBJ_C_OPTIONAL,
            BuiltInAnnotationKind.FOREIGN_GETTER_NAME,
            BuiltInAnnotationKind.FOREIGN_SETTER_NAME,
            BuiltInAnnotationKind.NON_PRODUCT,
        )
        versionedKinds.forEach { kind ->
            assertFalse(
                CjmpAnnotationMatchingPolicy.isHandledOutsideAnnotationMatching(
                    kind,
                    LanguageVersion.CANGJIE_1_2_0_ALPHA_19,
                ),
            )
            assertTrue(
                CjmpAnnotationMatchingPolicy.isHandledOutsideAnnotationMatching(
                    kind,
                    LanguageVersion.CANGJIE_1_2_0_ALPHA_20,
                ),
            )
        }
    }

    @Test
    fun `official unsupported kinds stay outside ordinary annotation matching`() {
        val unsupportedKinds = listOf(
            BuiltInAnnotationKind.JAVA,
            BuiltInAnnotationKind.CALLING_CONV,
            BuiltInAnnotationKind.CONSTSAFE,
            BuiltInAnnotationKind.ENSURE_PREPARED_TO_MOCK,
        )
        listOf(
            LanguageVersion.CANGJIE_1_1_3,
            LanguageVersion.CANGJIE_1_2_0_ALPHA_20,
            LanguageVersion.CANGJIE_1_3_0_ALPHA_02,
        ).forEach { version ->
            unsupportedKinds.forEach { kind ->
                assertTrue(CjmpAnnotationMatchingPolicy.isHandledOutsideAnnotationMatching(kind, version))
            }
        }
    }

    @Test
    fun `ordinary and serialization-owned kinds are not filtered by the official matcher policy`() {
        val ordinaryKinds = listOf(
            BuiltInAnnotationKind.JAVA_IMPL,
            BuiltInAnnotationKind.OBJ_C_IMPL,
            BuiltInAnnotationKind.FOREIGN_NAME,
            BuiltInAnnotationKind.NUMERIC_OVERFLOW,
            BuiltInAnnotationKind.INTRINSIC,
            BuiltInAnnotationKind.WHEN,
            BuiltInAnnotationKind.FASTNATIVE,
            BuiltInAnnotationKind.ANNOTATION,
            BuiltInAnnotationKind.FROZEN,
        )
        listOf(
            LanguageVersion.CANGJIE_1_1_3,
            LanguageVersion.CANGJIE_1_2_0_ALPHA_20,
            LanguageVersion.CANGJIE_1_3_0_ALPHA_02,
        ).forEach { version ->
            ordinaryKinds.forEach { kind ->
                assertFalse(CjmpAnnotationMatchingPolicy.isHandledOutsideAnnotationMatching(kind, version))
            }
        }
    }

    private fun identity(kind: BuiltInAnnotationKind): CangjieAnnotationIdentity =
        CangjieAnnotationIdentity.LanguageBuiltIn(kind, kind.name)
}
