package org.cangnova.cangjie

import org.cangnova.cangjie.config.ApiVersion
import org.cangnova.cangjie.annotations.AnnotationVersionSupportStatus
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.annotations.versionSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 锁定语言/API 版本的三段式身份与 Kotlin 同构的显式 feature 覆盖优先级。
 */
class LanguageVersionSettingsTest {
    @Test
    fun patchVersionsRemainDistinctAcrossStringAndApiRoundTrip() {
        assertEquals("1.0.0", LanguageVersion.CANGJIE_1_0_0.versionString)
        assertEquals("1.0.5", LanguageVersion.CANGJIE_1_0_5.versionString)
        assertEquals("1.1.3", LanguageVersion.CANGJIE_1_1_3.versionString)
        assertEquals(LanguageVersion.CANGJIE_1_0_5, LanguageVersion.parse("1.0.5"))
        assertEquals(ApiVersion.CANGJIE_1_0_5, ApiVersion.parse("1.0.5"))
    }

    @Test
    fun explicitFeatureStateOverridesVersionDefaultInBothDirections() {
        val feature = LanguageFeature.ObjCInteropAnnotations
        val oldVersion = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_0_0,
            apiVersion = ApiVersion.CANGJIE_1_0_0,
            specificFeatures = mapOf(feature to LanguageFeature.State.ENABLED),
        )
        val newVersionDisabled = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_1_3,
            apiVersion = ApiVersion.CANGJIE_1_1_3,
            specificFeatures = mapOf(feature to LanguageFeature.State.DISABLED),
        )

        assertTrue(oldVersion.supportsFeature(feature))
        assertFalse(newVersionDisabled.supportsFeature(feature))
    }

    @Test
    fun settingsToStringIsStableAndIncludesSemanticConfiguration() {
        val settings = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_1_0,
            apiVersion = ApiVersion.CANGJIE_1_1_0,
            analysisFlags = mapOf(AnalysisFlags.noPrelude to true),
            specificFeatures = mapOf(
                LanguageFeature.ObjCInteropAnnotations to LanguageFeature.State.ENABLED,
                LanguageFeature.JavaInteropAnnotations to LanguageFeature.State.DISABLED,
            ),
        )

        val rendered = settings.toString()
        assertTrue(rendered.startsWith("Language = 1.1.0, API = 1.1.0"))
        assertTrue("+ObjCInteropAnnotations" in rendered)
        assertTrue("-JavaInteropAnnotations" in rendered)
        assertTrue("noPrelude:true" in rendered)
    }

    @Test
    fun annotationVersionGateUsesTheSameFeatureOverrideAsSettings() {
        val oldSettings = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_0_0,
            apiVersion = ApiVersion.CANGJIE_1_0_0,
        )
        val explicitlyEnabled = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_0_0,
            apiVersion = ApiVersion.CANGJIE_1_0_0,
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
    }
}
