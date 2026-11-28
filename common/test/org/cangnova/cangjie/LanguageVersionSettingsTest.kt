package org.cangnova.cangjie

import org.cangnova.cangjie.annotations.AnnotationVersionSupportStatus
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.requiredLanguageFeature
import org.cangnova.cangjie.annotations.versionSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 锁定语言版本的三段式身份与 Kotlin 同构的显式 feature 覆盖优先级。
 */
class LanguageVersionSettingsTest {
    /** 固定本地官方46个tag，加上原先支持但无对应本地tag的三个稳定版本。 */
    @Test
    fun officialVersionIdentitiesRemainUniqueAndRoundTripWithoutLosingSuffixSpelling() {
        val expectedVersions = setOf(
            "1.0.0", "1.0.1", "1.0.2", "1.0.3-beta", "1.0.3", "1.0.4", "1.0.5",
            "1.1.0-alpha.34", "1.1.0-alpha.35", "1.1.0-alpha.36", "1.1.0-alpha.37",
            "1.1.0-alpha.44", "1.1.0-alpha.55", "1.1.0-alpha.65", "1.1.0-alpha.66",
            "1.1.0-alpha.68", "1.1.0-alpha.69", "1.1.0-alpha.70", "1.1.0.beta.0",
            "1.1.0-beta.1", "1.1.0-beta.20", "1.1.0-beta.21", "1.1.0-beta.23",
            "1.1.0-beta.24", "1.1.0-beta.25", "1.1.0", "1.1.1", "1.1.2", "1.1.3",
            "1.2.0-alpha.06", "1.2.0-alpha.07", "1.2.0-alpha.08", "1.2.0-alpha.16",
            "1.2.0-alpha.17", "1.2.0-alpha.18", "1.2.0-alpha.19", "1.2.0-alpha.20",
            "1.2.0-alpha.21", "1.2.0-alpha.22", "1.2.0-beta.01", "1.2.0-beta.02",
            "1.2.0-beta.03", "1.2.0-beta.rc", "1.2.0-beta.rc1", "1.2.0-beta.rc2",
            "1.3.0-alpha.02", "1.3.0-alpha.03", "1.3.0-alpha.04", "1.3.0-alpha.05",
        )
        assertEquals(49, expectedVersions.size)
        assertEquals(49, LanguageVersion.entries.size)
        assertEquals(expectedVersions, LanguageVersion.entries.map { it.versionString }.toSet())
        for (version in LanguageVersion.entries) {
            assertEquals(version, LanguageVersion.parse(version.versionString), version.name)
            assertEquals(version.versionString, version.toString(), version.name)
        }
    }

    @Test
    fun commonSpecificDeclarationsStartAtTheStableLanguageVersion() {
        val feature = LanguageFeature.CommonSpecificDeclarations
        assertEquals(LanguageVersion.CANGJIE_1_1_0, feature.sinceVersion)
        assertFalse(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_0_5).supportsFeature(feature))
        assertFalse(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_65).supportsFeature(feature))
        assertFalse(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_66).supportsFeature(feature))
        assertTrue(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0).supportsFeature(feature))

        val explicitlyDisabled = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_1_0_ALPHA_65,
            specificFeatures = mapOf(feature to LanguageFeature.State.DISABLED),
        )
        assertFalse(explicitlyDisabled.supportsFeature(feature))
    }

    @Test
    fun prereleaseCjmpKeywordSurfacesHaveIndependentVersionRanges() {
        val earlyPlatform = CjmpLanguageFeature.CommonPlatformDeclarations
        val prereleaseSpecific = CjmpLanguageFeature.PrereleaseSpecificDeclarations

        assertTrue(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_34).supportsCjmpFeature(earlyPlatform))
        assertTrue(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_65).supportsCjmpFeature(earlyPlatform))
        assertFalse(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_BETA_20).supportsCjmpFeature(earlyPlatform))

        assertFalse(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_BETA_1).supportsCjmpFeature(prereleaseSpecific))
        assertTrue(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_BETA_20).supportsCjmpFeature(prereleaseSpecific))
        assertFalse(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0).supportsCjmpFeature(prereleaseSpecific))
    }

    @Test
    fun prereleaseBoundaryIdentitiesKeepLeadingZeroesAndHistoricalSeparator() {
        assertEquals("1.2.0-alpha.06", LanguageVersion.CANGJIE_1_2_0_ALPHA_06.versionString)
        assertEquals("1.2.0-beta.01", LanguageVersion.CANGJIE_1_2_0_BETA_01.versionString)
        assertEquals("1.3.0-alpha.04", LanguageVersion.CANGJIE_1_3_0_ALPHA_04.versionString)
        assertEquals("1.3.0-alpha.05", LanguageVersion.CANGJIE_1_3_0_ALPHA_05.versionString)
        assertEquals("1.1.0.beta.0", LanguageVersion.CANGJIE_1_1_0_BETA_0.versionString)
        assertEquals("1.2.0-beta.rc", LanguageVersion.CANGJIE_1_2_0_BETA_RC.versionString)
        assertEquals("1.2.0-beta.rc1", LanguageVersion.CANGJIE_1_2_0_BETA_RC1.versionString)
        assertEquals("1.2.0-beta.rc2", LanguageVersion.CANGJIE_1_2_0_BETA_RC2.versionString)
    }

    /** 只锁定已取证的数值预发布顺序，不借rc身份推导额外语言语义。 */
    @Test
    fun prereleaseOrderingKeepsNumericAndStableReleaseBoundaries() {
        val orderedVersions = listOf(
            LanguageVersion.CANGJIE_1_0_2,
            LanguageVersion.CANGJIE_1_0_3_BETA,
            LanguageVersion.CANGJIE_1_0_3,
            LanguageVersion.CANGJIE_1_0_5,
            LanguageVersion.CANGJIE_1_1_0_ALPHA_34,
            LanguageVersion.CANGJIE_1_1_0_ALPHA_70,
            LanguageVersion.CANGJIE_1_1_0_BETA_0,
            LanguageVersion.CANGJIE_1_1_0_BETA_1,
            LanguageVersion.CANGJIE_1_1_0_BETA_20,
            LanguageVersion.CANGJIE_1_1_0,
            LanguageVersion.CANGJIE_1_1_1,
            LanguageVersion.CANGJIE_1_1_2,
            LanguageVersion.CANGJIE_1_1_3,
            LanguageVersion.CANGJIE_1_2_0_ALPHA_08,
            LanguageVersion.CANGJIE_1_2_0_ALPHA_16,
            LanguageVersion.CANGJIE_1_2_0_ALPHA_19,
            LanguageVersion.CANGJIE_1_2_0_ALPHA_20,
            LanguageVersion.CANGJIE_1_2_0_ALPHA_22,
            LanguageVersion.CANGJIE_1_2_0_BETA_01,
            LanguageVersion.CANGJIE_1_3_0_ALPHA_04,
            LanguageVersion.CANGJIE_1_3_0_ALPHA_05,
        )
        for ((earlier, later) in orderedVersions.zipWithNext()) {
            assertTrue(earlier < later, "$earlier must precede $later")
        }
    }

    @Test
    fun unknownPrereleaseIdentitiesAreRejectedInsteadOfTruncatedOrNormalized() {
        for (unknownVersion in listOf(
            "1.1.0-alpha.33",
            "1.1.0-beta.0",
            "1.2.0-alpha.6",
            "1.2.0-alpha.020",
            "1.2.0-alpha.23",
            "1.2.0-beta.1",
            "1.2.0-beta.rc3",
            "1.3.0-alpha.4",
            "1.3.0-alpha.06",
            "1.1.3-alpha.01",
            "1.1.3.extra",
        )) {
            assertFailsWith<IllegalStateException>(unknownVersion) { LanguageVersion.parse(unknownVersion) }
        }
        assertEquals(LanguageVersion.CANGJIE_1_0_0, LanguageVersion.parse("1.0"))
        assertEquals(LanguageVersion.CANGJIE_1_1_0, LanguageVersion.parse("1.1"))
    }

    @Test
    fun historicalPrereleasesStayUnstableEvenWhenEarlierThanLatestStable() {
        val prereleases = LanguageVersion.entries.filter { it.preReleaseTag != null }
        assertEquals(39, prereleases.size)
        for (version in prereleases) {
            assertFalse(version.isStable, version.versionString)
            assertTrue(version.isPreRelease(), version.versionString)
            assertTrue(LanguageVersionSettingsImpl(version).isPreRelease(), version.versionString)
        }
        assertTrue(LanguageVersion.CANGJIE_1_1_0.isStable)
        assertFalse(LanguageVersion.CANGJIE_1_1_0.isPreRelease())
        assertEquals(LanguageVersion.CANGJIE_1_1_3, LanguageVersion.LATEST_STABLE)
        assertEquals(LanguageVersion.CANGJIE_1_1_3, LanguageVersionSettingsImpl.DEFAULT.languageVersion)
    }

    /** 此门只表达alpha.20起已取证的混合文件规则，不推定早期generic等历史语义。 */
    @Test
    fun mixedCommonSpecificDeclarationsStartAtExactAlphaTwentyBoundary() {
        val feature = LanguageFeature.CjmpMixedCommonSpecificDeclarations
        assertEquals(LanguageVersion.CANGJIE_1_2_0_ALPHA_20, feature.sinceVersion)
        val oldSettings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_2_0_ALPHA_19)
        val introducedSettings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_2_0_ALPHA_20)
        assertFalse(oldSettings.supportsFeature(feature))
        assertEquals(LanguageFeature.State.DISABLED, oldSettings.getFeatureSupport(feature))
        assertTrue(introducedSettings.supportsFeature(feature))
        assertEquals(LanguageFeature.State.ENABLED, introducedSettings.getFeatureSupport(feature))
        assertFalse(LanguageVersionSettingsImpl.DEFAULT.supportsFeature(feature))
        assertTrue(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_3_0_ALPHA_05).supportsFeature(feature))
    }

    @Test
    fun commonMembersInSpecificOutersHaveAnIndependentAlphaTwentyFeature() {
        val feature = LanguageFeature.CjmpCommonMemberInSpecificOuter
        assertEquals(LanguageVersion.CANGJIE_1_2_0_ALPHA_20, feature.sinceVersion)
        val alpha19 = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_2_0_ALPHA_19)
        val alpha20 = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_2_0_ALPHA_20)
        assertFalse(alpha19.supportsCjmpFeature(CjmpLanguageFeature.CommonMemberInSpecificOuter))
        assertTrue(alpha20.supportsCjmpFeature(CjmpLanguageFeature.CommonMemberInSpecificOuter))

        val alpha19WithOverride = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_2_0_ALPHA_19,
            specificFeatures = mapOf(feature to LanguageFeature.State.ENABLED),
        )
        val alpha20WithOverride = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_2_0_ALPHA_20,
            specificFeatures = mapOf(feature to LanguageFeature.State.DISABLED),
        )
        assertTrue(alpha19WithOverride.supportsCjmpFeature(CjmpLanguageFeature.CommonMemberInSpecificOuter))
        assertFalse(alpha20WithOverride.supportsCjmpFeature(CjmpLanguageFeature.CommonMemberInSpecificOuter))
        assertFalse(alpha19WithOverride.supportsCjmpFeature(CjmpLanguageFeature.MixedCommonSpecificDeclarations))
        assertTrue(alpha20WithOverride.supportsCjmpFeature(CjmpLanguageFeature.MixedCommonSpecificDeclarations))
    }

    /** 三项早期 CJMP 行为分别按本地官方 tag 的首次源码证据建立独立版本门。 */
    @Test
    fun cjmpGenericDefaultAndPostMatchFeaturesStartAtTheirVerifiedPrereleaseBoundaries() {
        val alpha65 = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_65)
        val alpha66 = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_66)
        val alpha68 = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_68)

        assertFalse(alpha65.supportsCjmpFeature(CjmpLanguageFeature.GenericDeclarations))
        assertTrue(alpha66.supportsCjmpFeature(CjmpLanguageFeature.GenericDeclarations))
        assertTrue(LanguageVersionSettingsImpl.DEFAULT.supportsCjmpFeature(CjmpLanguageFeature.GenericDeclarations))

        assertFalse(alpha65.supportsCjmpFeature(CjmpLanguageFeature.ParameterDefaultValueRules))
        assertTrue(alpha66.supportsCjmpFeature(CjmpLanguageFeature.ParameterDefaultValueRules))
        assertTrue(LanguageVersionSettingsImpl.DEFAULT.supportsCjmpFeature(CjmpLanguageFeature.ParameterDefaultValueRules))

        assertTrue(alpha65.supportsCjmpFeature(CjmpLanguageFeature.PrimaryConstructorRoleModifiers))
        assertFalse(alpha66.supportsCjmpFeature(CjmpLanguageFeature.PrimaryConstructorRoleModifiers))
        assertFalse(alpha68.supportsCjmpFeature(CjmpLanguageFeature.PrimaryConstructorRoleModifiers))
        for (version in listOf(LanguageVersion.CANGJIE_1_1_0_BETA_0, LanguageVersion.CANGJIE_1_1_0_BETA_1)) {
            assertTrue(LanguageVersionSettingsImpl(version).supportsCjmpFeature(CjmpLanguageFeature.PrimaryConstructorRoleModifiers))
        }
        assertFalse(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_BETA_20)
            .supportsCjmpFeature(CjmpLanguageFeature.PrimaryConstructorRoleModifiers))

        assertTrue(alpha65.supportsCjmpFeature(CjmpLanguageFeature.PrimaryConstructorDefaultImplementation))
        assertTrue(alpha66.supportsCjmpFeature(CjmpLanguageFeature.PrimaryConstructorDefaultImplementation))
        assertFalse(alpha68.supportsCjmpFeature(CjmpLanguageFeature.PrimaryConstructorDefaultImplementation))
        for (version in listOf(LanguageVersion.CANGJIE_1_1_0_BETA_0, LanguageVersion.CANGJIE_1_1_0_BETA_1)) {
            assertTrue(LanguageVersionSettingsImpl(version).supportsCjmpFeature(CjmpLanguageFeature.PrimaryConstructorDefaultImplementation))
        }
        assertFalse(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_BETA_20)
            .supportsCjmpFeature(CjmpLanguageFeature.PrimaryConstructorDefaultImplementation))

        assertFalse(alpha66.supportsCjmpFeature(CjmpLanguageFeature.PostMatchReturnTypeCheck))
        assertTrue(alpha68.supportsCjmpFeature(CjmpLanguageFeature.PostMatchReturnTypeCheck))
        assertTrue(LanguageVersionSettingsImpl.DEFAULT.supportsCjmpFeature(CjmpLanguageFeature.PostMatchReturnTypeCheck))
    }

    @Test
    fun cjmpParameterRoleMatchingFollowsTheCompleteNonMonotonicVersionMatrix() {
        val feature = LanguageFeature.CjmpParameterRoleMatching
        val enabledVersions = setOf(
            LanguageVersion.CANGJIE_1_1_0_ALPHA_34,
            LanguageVersion.CANGJIE_1_1_0_ALPHA_35,
            LanguageVersion.CANGJIE_1_1_0_ALPHA_36,
            LanguageVersion.CANGJIE_1_1_0_ALPHA_37,
            LanguageVersion.CANGJIE_1_1_0_ALPHA_44,
            LanguageVersion.CANGJIE_1_1_0_ALPHA_55,
            LanguageVersion.CANGJIE_1_1_0_ALPHA_65,
            LanguageVersion.CANGJIE_1_1_0_ALPHA_68,
            LanguageVersion.CANGJIE_1_1_0_ALPHA_69,
            LanguageVersion.CANGJIE_1_1_0_ALPHA_70,
            LanguageVersion.CANGJIE_1_1_0_BETA_0,
            LanguageVersion.CANGJIE_1_1_0_BETA_1,
            LanguageVersion.CANGJIE_1_1_0_BETA_20,
            LanguageVersion.CANGJIE_1_1_0_BETA_21,
            LanguageVersion.CANGJIE_1_1_0_BETA_23,
            LanguageVersion.CANGJIE_1_1_0_BETA_24,
            LanguageVersion.CANGJIE_1_1_0_BETA_25,
            LanguageVersion.CANGJIE_1_1_0,
            LanguageVersion.CANGJIE_1_1_1,
            LanguageVersion.CANGJIE_1_1_2,
            LanguageVersion.CANGJIE_1_1_3,
            LanguageVersion.CANGJIE_1_2_0_ALPHA_06,
            LanguageVersion.CANGJIE_1_2_0_ALPHA_07,
            LanguageVersion.CANGJIE_1_2_0_ALPHA_08,
            LanguageVersion.CANGJIE_1_2_0_ALPHA_16,
            LanguageVersion.CANGJIE_1_2_0_ALPHA_17,
            LanguageVersion.CANGJIE_1_2_0_ALPHA_18,
            LanguageVersion.CANGJIE_1_2_0_ALPHA_19,
        )
        assertEquals(28, enabledVersions.size)

        for (version in LanguageVersion.entries) {
            val settings = LanguageVersionSettingsImpl(version)
            val expected = version in enabledVersions
            assertEquals(expected, settings.supportsCjmpFeature(CjmpLanguageFeature.ParameterRoleMatching), version.versionString)
            assertEquals(
                when {
                    expected -> LanguageFeatureSupportStatus.SUPPORTED
                    version < feature.sinceVersion!! -> LanguageFeatureSupportStatus.UNSUPPORTED_LANGUAGE_VERSION
                    else -> LanguageFeatureSupportStatus.DISABLED
                },
                settings.featureSupportStatus(feature),
                version.versionString,
            )
        }
    }

    @Test
    fun cjmpParameterRoleMatchingHonorsOverridesAcrossRangeGaps() {
        val feature = LanguageFeature.CjmpParameterRoleMatching
        val beforeIntroduction = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_0_5,
            specificFeatures = mapOf(feature to LanguageFeature.State.ENABLED),
        )
        assertTrue(beforeIntroduction.supportsCjmpFeature(CjmpLanguageFeature.ParameterRoleMatching))

        val alpha66Disabled = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_66)
        assertEquals(LanguageFeature.State.DISABLED, alpha66Disabled.getFeatureSupport(feature))
        val alpha66Enabled = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_1_0_ALPHA_66,
            specificFeatures = mapOf(feature to LanguageFeature.State.ENABLED),
        )
        assertTrue(alpha66Enabled.supportsCjmpFeature(CjmpLanguageFeature.ParameterRoleMatching))
        assertEquals(setOf(feature), alpha66Enabled.getCustomizedEffectivelyEnabledLanguageFeatures())

        val alpha65Disabled = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_1_0_ALPHA_65,
            specificFeatures = mapOf(feature to LanguageFeature.State.DISABLED),
        )
        assertEquals(LanguageFeatureSupportStatus.DISABLED, alpha65Disabled.featureSupportStatus(feature))
        assertEquals(setOf(feature), alpha65Disabled.getCustomizedEffectivelyDisabledLanguageFeatures())

        val alpha20Disabled = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_2_0_ALPHA_20)
        assertEquals(LanguageFeatureSupportStatus.DISABLED, alpha20Disabled.featureSupportStatus(feature))
    }

    @Test
    fun languageVersionRangesRequireAnIncreasingExclusiveEnd() {
        assertFailsWith<IllegalArgumentException> {
            LanguageVersionRange(
                sinceInclusive = LanguageVersion.CANGJIE_1_1_0_ALPHA_66,
                untilExclusive = LanguageVersion.CANGJIE_1_1_0_ALPHA_65,
            )
        }
        val range = LanguageVersionRange(
            sinceInclusive = LanguageVersion.CANGJIE_1_1_0_ALPHA_65,
            untilExclusive = LanguageVersion.CANGJIE_1_1_0_ALPHA_66,
        )
        assertTrue(range.contains(LanguageVersion.CANGJIE_1_1_0_ALPHA_65))
        assertFalse(range.contains(LanguageVersion.CANGJIE_1_1_0_ALPHA_66))
    }

    @Test
    fun multipleCommonParentsStartAtExactAlphaTwentyBoundary() {
        val feature = LanguageFeature.CjmpMultipleCommonParents
        assertEquals(LanguageVersion.CANGJIE_1_2_0_ALPHA_20, feature.sinceVersion)
        assertFalse(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_2_0_ALPHA_19).supportsFeature(feature))
        assertTrue(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_2_0_ALPHA_20).supportsFeature(feature))
        assertTrue(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_3_0_ALPHA_05).supportsFeature(feature))
    }

    @Test
    fun dedicatedCommonPartChirInputsHaveTheirOwnAlphaTwentyFeature() {
        val feature = LanguageFeature.CjmpDedicatedCommonPartChirInputs
        assertEquals(LanguageVersion.CANGJIE_1_2_0_ALPHA_20, feature.sinceVersion)
        assertFalse(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_2_0_ALPHA_19).supportsFeature(feature))
        assertTrue(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_2_0_ALPHA_20).supportsFeature(feature))
        assertTrue(LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_3_0_ALPHA_05).supportsFeature(feature))
    }

    @Test
    fun cjmpLanguageFeatureGateOwnsTheCommonLoadAndCjoFormatBoundaries() {
        val alpha19 = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_2_0_ALPHA_19)
        val alpha20 = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_2_0_ALPHA_20)
        assertFalse(alpha19.supportsCjmpFeature(CjmpLanguageFeature.CommonPartFeaturesAndOptionsGate))
        assertTrue(alpha20.supportsCjmpFeature(CjmpLanguageFeature.CommonPartFeaturesAndOptionsGate))

        val alpha04 = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_3_0_ALPHA_04)
        val alpha05 = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_3_0_ALPHA_05)
        assertFalse(alpha04.supportsCjmpFeature(CjmpLanguageFeature.CjoFormatVersionCompatibility))
        assertTrue(alpha05.supportsCjmpFeature(CjmpLanguageFeature.CjoFormatVersionCompatibility))
    }

    @Test
    fun mixedDeclarationsExplicitOverridesAndCustomizedDisabledProjectionShareOneState() {
        val feature = LanguageFeature.CjmpMixedCommonSpecificDeclarations
        val explicitlyEnabled = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_2_0_ALPHA_19,
            specificFeatures = mapOf(feature to LanguageFeature.State.ENABLED),
        )
        val explicitlyDisabled = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_2_0_ALPHA_20,
            specificFeatures = mapOf(feature to LanguageFeature.State.DISABLED),
        )
        val oldExplicitlyDisabled = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_2_0_ALPHA_19,
            specificFeatures = mapOf(feature to LanguageFeature.State.DISABLED),
        )
        assertTrue(explicitlyEnabled.supportsFeature(feature))
        assertEquals(LanguageFeature.State.ENABLED, explicitlyEnabled.getFeatureSupport(feature))
        assertEquals(emptySet(), explicitlyEnabled.getCustomizedEffectivelyDisabledLanguageFeatures())
        assertFalse(explicitlyDisabled.supportsFeature(feature))
        assertEquals(LanguageFeature.State.DISABLED, explicitlyDisabled.getFeatureSupport(feature))
        assertEquals(setOf(feature), explicitlyDisabled.getCustomizedEffectivelyDisabledLanguageFeatures())
        assertEquals(
            mapOf(feature to LanguageFeature.State.DISABLED),
            oldExplicitlyDisabled.getCustomizedLanguageFeatures(),
        )
        assertEquals(emptySet(), oldExplicitlyDisabled.getCustomizedEffectivelyDisabledLanguageFeatures())
        assertTrue(explicitlyEnabled.toString().startsWith("Language = 1.2.0-alpha.19"))
        assertTrue("+CjmpMixedCommonSpecificDeclarations" in explicitlyEnabled.toString())
        assertTrue(explicitlyDisabled.toString().startsWith("Language = 1.2.0-alpha.20"))
        assertTrue("-CjmpMixedCommonSpecificDeclarations" in explicitlyDisabled.toString())
    }

    @Test
    fun patchVersionsRemainDistinctAcrossStringRoundTrip() {
        assertEquals("1.0.0", LanguageVersion.CANGJIE_1_0_0.versionString)
        assertEquals("1.0.5", LanguageVersion.CANGJIE_1_0_5.versionString)
        assertEquals("1.1.3", LanguageVersion.CANGJIE_1_1_3.versionString)
        assertEquals(LanguageVersion.CANGJIE_1_0_5, LanguageVersion.parse("1.0.5"))
        assertEquals(LanguageVersion.CANGJIE_1_1_3, LanguageVersion.LATEST_STABLE)
        assertFailsWith<IllegalStateException> { LanguageVersion.parse("1.0.0.extra") }
    }

    @Test
    fun featureIdentityLookupUsesExactEnumSpelling() {
        assertEquals(
            LanguageFeature.ObjCInteropAnnotations,
            LanguageFeature.fromName("ObjCInteropAnnotations"),
        )
        assertEquals(null, LanguageFeature.fromName("objcinteropannotations"))
    }

    @Test
    fun builtinAstKindsUseTheCentralVersionFeatureMapping() {
        assertEquals(
            LanguageFeature.JavaBuiltinAnnotations,
            BuiltInAnnotationKind.JAVA.requiredLanguageFeature,
        )
        assertEquals(
            LanguageFeature.JavaInteropAnnotations,
            BuiltInAnnotationKind.JAVA_MIRROR.requiredLanguageFeature,
        )
        assertEquals(
            LanguageFeature.ObjCInteropAnnotations,
            BuiltInAnnotationKind.OBJ_C_IMPL.requiredLanguageFeature,
        )
        assertEquals(null, BuiltInAnnotationKind.C.requiredLanguageFeature)
    }

    @Test
    fun featureMetadataUsesKotlinPresentableNameContract() {
        assertEquals("api level since parameter", LanguageFeature.ApiLevelSinceParameter.presentableName)
        assertFalse(LanguageFeature.EnableDfaWarnings.actuallyEnabledInProgressiveMode)
        assertEquals("allow intersection types in inference", LanguageFeature.AllowIntersectionTypesInInference.presentableText)
    }

    @Test
    fun apiLevelSinceParameterStartsInOneZeroFive() {
        val legacy = LanguageVersionSettingsImpl(
            LanguageVersion.CANGJIE_1_0_0,
        )
        val introduced = LanguageVersionSettingsImpl(
            LanguageVersion.CANGJIE_1_0_5,
        )
        assertEquals(
            LanguageFeatureSupportStatus.UNSUPPORTED_LANGUAGE_VERSION,
            legacy.featureSupportStatus(LanguageFeature.ApiLevelSinceParameter),
        )
        assertTrue(introduced.supportsFeature(LanguageFeature.ApiLevelSinceParameter))
    }

    @Test
    fun explicitFeatureStateOverridesVersionDefaultInBothDirections() {
        val feature = LanguageFeature.ObjCInteropAnnotations
        val oldVersion = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_0_0,
            specificFeatures = mapOf(feature to LanguageFeature.State.ENABLED),
        )
        val newVersionDisabled = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_1_3,
            specificFeatures = mapOf(feature to LanguageFeature.State.DISABLED),
        )

        assertTrue(oldVersion.supportsFeature(feature))
        assertFalse(newVersionDisabled.supportsFeature(feature))
    }

    @Test
    fun settingsToStringIsStableAndIncludesSemanticConfiguration() {
        val settings = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_1_0,
            analysisFlags = mapOf(AnalysisFlags.noPrelude to true),
            specificFeatures = mapOf(
                LanguageFeature.ObjCInteropAnnotations to LanguageFeature.State.ENABLED,
                LanguageFeature.JavaInteropAnnotations to LanguageFeature.State.DISABLED,
            ),
        )

        val rendered = settings.toString()
        assertTrue(rendered.startsWith("Language = 1.1.0"))
        assertTrue("+ObjCInteropAnnotations" in rendered)
        assertTrue("-JavaInteropAnnotations" in rendered)
        assertTrue("noPrelude:true" in rendered)
    }

    @Test
    fun annotationVersionGateUsesTheSameFeatureOverrideAsSettings() {
        val oldSettings = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_0_0,
        )
        val explicitlyEnabled = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_0_0,
            specificFeatures = mapOf(
                LanguageFeature.ObjCInteropAnnotations to LanguageFeature.State.ENABLED,
            ),
        )
        val descriptor = requireNotNull(
            BuiltInAnnotationRegistry.findPlatformAnnotation(
                org.cangnova.cangjie.name.FqName("objc.lang.ObjCMirror"),
            ),
        )

        assertEquals(
            AnnotationVersionSupportStatus.UNSUPPORTED_LANGUAGE_VERSION,
            descriptor.versionSupport(oldSettings),
        )
        assertEquals(
            AnnotationVersionSupportStatus.SUPPORTED,
            descriptor.versionSupport(explicitlyEnabled),
        )
        assertEquals(
            AnnotationVersionSupportStatus.SUPPORTED,
            LanguageFeature.ObjCInteropAnnotations.versionSupport(explicitlyEnabled),
        )
        assertEquals(
            AnnotationVersionSupportStatus.SUPPORTED,
            LanguageFeature.JavaBuiltinAnnotations.versionSupport(oldSettings),
        )
    }

    @Test
    fun featureSupportStatusKeepsLanguageReasonInOneOwner() {
        val languageTooOld = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_0_0,
        )

        assertEquals(
            LanguageFeatureSupportStatus.UNSUPPORTED_LANGUAGE_VERSION,
            languageTooOld.featureSupportStatus(LanguageFeature.ObjCInteropAnnotations),
        )
    }

    @Test
    fun explicitDisabledUsesVersionReasonBeforeDisabledReason() {
        val oldSettings = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_0_0,
            specificFeatures = mapOf(
                LanguageFeature.ObjCInteropAnnotations to LanguageFeature.State.DISABLED,
            ),
        )
        val currentSettings = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_1_3,
            specificFeatures = mapOf(
                LanguageFeature.ObjCInteropAnnotations to LanguageFeature.State.DISABLED,
            ),
        )

        assertEquals(
            LanguageFeatureSupportStatus.UNSUPPORTED_LANGUAGE_VERSION,
            oldSettings.featureSupportStatus(LanguageFeature.ObjCInteropAnnotations),
        )
        assertEquals(
            LanguageFeatureSupportStatus.DISABLED,
            currentSettings.featureSupportStatus(LanguageFeature.ObjCInteropAnnotations),
        )
        assertEquals(
            LanguageFeature.State.DISABLED,
            currentSettings.getFeatureSupport(LanguageFeature.ObjCInteropAnnotations),
        )
        assertEquals(
            AnnotationVersionSupportStatus.DISABLED,
            LanguageFeature.ObjCInteropAnnotations.versionSupport(currentSettings),
        )
    }

    @Test
    fun effectivelyDisabledFeaturesFollowKotlinStateProjectionWithoutLifecycleFiltering() {
        val settings = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_1_3,
            specificFeatures = mapOf(
                LanguageFeature.ObjCInteropAnnotations to LanguageFeature.State.DISABLED,
                LanguageFeature.BuiltInAnnotations to LanguageFeature.State.DISABLED,
            ),
        )

        assertEquals(
            setOf(LanguageFeature.ObjCInteropAnnotations, LanguageFeature.BuiltInAnnotations),
            settings.getCustomizedEffectivelyDisabledLanguageFeatures(),
        )
    }

    @Test
    fun experimentalFeatureIsReportedAsExperimental() {
        val settings = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_1_3,
        )

        assertEquals(
            LanguageFeatureSupportStatus.EXPERIMENTAL,
            settings.featureSupportStatus(LanguageFeature.AllowIntersectionTypesInInference),
        )
        assertEquals(
            AnnotationVersionSupportStatus.EXPERIMENTAL,
            LanguageFeature.AllowIntersectionTypesInInference.versionSupport(settings),
        )
    }

    @Test
    fun explicitlyEnabledExperimentalFeatureIsSupported() {
        val settings = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_0_0,
            specificFeatures = mapOf(
                LanguageFeature.AllowIntersectionTypesInInference to LanguageFeature.State.ENABLED,
            ),
        )

        assertTrue(settings.supportsFeature(LanguageFeature.AllowIntersectionTypesInInference))
        assertEquals(
            LanguageFeatureSupportStatus.SUPPORTED,
            settings.featureSupportStatus(LanguageFeature.AllowIntersectionTypesInInference),
        )
        assertEquals(
            AnnotationVersionSupportStatus.SUPPORTED,
            LanguageFeature.AllowIntersectionTypesInInference.versionSupport(settings),
        )
    }
}
