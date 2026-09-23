package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 对位 Kotlin FIR LanguageVersionUtils 的 session-owned 查询契约。 */
class LanguageVersionUtilsTest {
    @Test
    fun isEnabledReadsSessionFeatureSupport() {
        val session = session(
            LanguageVersionSettingsImpl(
                languageVersion = LanguageVersion.CANGJIE_1_0_0,
                specificFeatures = mapOf(
                    LanguageFeature.ObjCInteropAnnotations to LanguageFeature.State.ENABLED,
                ),
            ),
        )

        withSession(session) {
            assertTrue(LanguageFeature.ObjCInteropAnnotations.isEnabled())
        }
    }

    @Test
    fun isDisabledIsExactNegationOfIsEnabled() {
        val session = session(LanguageVersionSettingsImpl.DEFAULT)

        withSession(session) {
            assertFalse(LanguageFeature.BuiltInAnnotations.isDisabled())
            assertTrue(LanguageFeature.AllowIntersectionTypesInInference.isDisabled())
        }
    }

    @Test
    fun isSetReadsConfiguredAndDefaultAnalysisFlags() {
        val configured = session(
            LanguageVersionSettingsImpl(
                languageVersion = LanguageVersion.LATEST_STABLE,
                analysisFlags = mapOf(AnalysisFlags.noPrelude to true),
            ),
        )
        val defaults = session(LanguageVersionSettingsImpl.DEFAULT)

        withSession(configured) {
            assertTrue(AnalysisFlags.noPrelude.isSet())
            assertTrue(AnalysisFlags.expandTypeAliasesInTypeResolution.isSet())
        }
        withSession(defaults) {
            assertFalse(AnalysisFlags.noPrelude.isSet())
            assertTrue(AnalysisFlags.expandTypeAliasesInTypeResolution.isSet())
        }
    }

    private fun session(settings: LanguageVersionSettingsImpl): CfirSession =
        object : CfirSession(CfirSession.Kind.Source) {
            init {
                register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(settings))
            }
        }
}
