package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** 解析调用配置必须保留每项相互独立的 CJMP 语言门投影。 */
class CjmpParsingContextTest {
    @Test
    fun commonMemberNestingFeatureIsProjectedIndependentlyFromMixedFileFeature() {
        val settingsComponent = CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.SPECIFIC)

        val alpha19 = settingsComponent.toParsingContext(
            LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_2_0_ALPHA_19),
        )
        assertTrue(alpha19.compileSpecific)
        assertFalse(alpha19.commonMemberInSpecificOuter)
        assertFalse(alpha19.mixedCommonSpecificDeclarations)
        assertTrue(alpha19.parameterRoleMatching)

        val alpha20 = settingsComponent.toParsingContext(
            LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_2_0_ALPHA_20),
        )
        assertTrue(alpha20.commonMemberInSpecificOuter)
        assertTrue(alpha20.mixedCommonSpecificDeclarations)
        assertFalse(alpha20.parameterRoleMatching)

        val alpha66 = settingsComponent.toParsingContext(
            LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_66),
        )
        assertTrue(alpha66.enabled)
        assertFalse(alpha66.parameterRoleMatching)
        assertTrue(alpha66.genericDeclarationsAvailable)
        assertTrue(alpha66.parameterDefaultValueRulesAvailable)
        assertFalse(alpha66.modifierSyntaxAvailable)
        assertTrue(alpha66.platformModifierSyntaxAvailable)
        assertFalse(alpha66.primaryConstructorRoleModifiersAvailable)
        val alpha68 = settingsComponent.toParsingContext(
            LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_68),
        )
        assertTrue(alpha68.enabled)
        assertTrue(alpha68.parameterRoleMatching)
        assertTrue(alpha68.genericDeclarationsAvailable)
        assertTrue(alpha68.parameterDefaultValueRulesAvailable)
        assertFalse(alpha68.modifierSyntaxAvailable)
        assertTrue(alpha68.platformModifierSyntaxAvailable)

        val alpha19WithNestingOverride = settingsComponent.toParsingContext(
            LanguageVersionSettingsImpl(
                languageVersion = LanguageVersion.CANGJIE_1_2_0_ALPHA_19,
                specificFeatures = mapOf(
                    LanguageFeature.CjmpCommonMemberInSpecificOuter to LanguageFeature.State.ENABLED,
                ),
            ),
        )
        assertTrue(alpha19WithNestingOverride.commonMemberInSpecificOuter)
        assertFalse(alpha19WithNestingOverride.mixedCommonSpecificDeclarations)
        assertTrue(alpha19WithNestingOverride.parameterRoleMatching)

        val alpha20WithNestingDisabled = settingsComponent.toParsingContext(
            LanguageVersionSettingsImpl(
                languageVersion = LanguageVersion.CANGJIE_1_2_0_ALPHA_20,
                specificFeatures = mapOf(
                    LanguageFeature.CjmpCommonMemberInSpecificOuter to LanguageFeature.State.DISABLED,
                ),
            ),
        )
        assertFalse(alpha20WithNestingDisabled.commonMemberInSpecificOuter)
        assertTrue(alpha20WithNestingDisabled.mixedCommonSpecificDeclarations)
        assertFalse(alpha20WithNestingDisabled.parameterRoleMatching)

        val alpha65WithParameterOverride = settingsComponent.toParsingContext(
            LanguageVersionSettingsImpl(
                languageVersion = LanguageVersion.CANGJIE_1_1_0_ALPHA_65,
                specificFeatures = mapOf(
                    LanguageFeature.CjmpParameterRoleMatching to LanguageFeature.State.DISABLED,
                ),
            ),
        )
        assertFalse(alpha65WithParameterOverride.parameterRoleMatching)
        assertFalse(alpha65WithParameterOverride.commonMemberInSpecificOuter)
        assertFalse(alpha65WithParameterOverride.mixedCommonSpecificDeclarations)

        val alpha65 = settingsComponent.toParsingContext(
            LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_65),
        )
        assertTrue(alpha65.enabled)
        assertFalse(alpha65.genericDeclarationsAvailable)
        assertFalse(alpha65.parameterDefaultValueRulesAvailable)
        assertFalse(alpha65.modifierSyntaxAvailable)
        assertTrue(alpha65.platformModifierSyntaxAvailable)
        assertTrue(alpha65.primaryConstructorRoleModifiersAvailable)

        for (version in listOf(LanguageVersion.CANGJIE_1_1_0_BETA_0, LanguageVersion.CANGJIE_1_1_0_BETA_1)) {
            val beta = settingsComponent.toParsingContext(LanguageVersionSettingsImpl(version))
            assertTrue(beta.platformModifierSyntaxAvailable)
            assertTrue(beta.primaryConstructorRoleModifiersAvailable)
        }

        val beta20 = settingsComponent.toParsingContext(
            LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_BETA_20),
        )
        assertTrue(beta20.enabled)
        assertTrue(beta20.modifierSyntaxAvailable)
        assertFalse(beta20.platformModifierSyntaxAvailable)
        assertFalse(beta20.primaryConstructorRoleModifiersAvailable)

        val alpha65WithCjmpDisabled = settingsComponent.toParsingContext(
            LanguageVersionSettingsImpl(
                languageVersion = LanguageVersion.CANGJIE_1_1_0_ALPHA_65,
                specificFeatures = mapOf(
                    LanguageFeature.CommonSpecificDeclarations to LanguageFeature.State.DISABLED,
                ),
            ),
        )
        assertFalse(alpha65WithCjmpDisabled.enabled)

    }
}
