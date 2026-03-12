package org.cangjie.cli.common.arguments

import org.cangjie.config.LanguageVersion
import org.cangjie.config.LanguageVersions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class LanguageSettingsArgumentParserTest {
    @Test
    fun `defaults to 1_0_5 and empty feature set when arguments are absent`() {
        val settings = LanguageSettingsArgumentParser.parse(CommonCompilerArguments())

        assertEquals(LanguageVersions.V_1_0_5, settings.languageVersion)
        assertTrue(settings.enabledFeatures.isEmpty())
    }

    @Test
    fun `parses language version from command arguments`() {
        val settings = LanguageSettingsArgumentParser.parse(
            CommonCompilerArguments(languageVersion = "1.0.5"),
        )

        assertEquals(LanguageVersion(1, 0, 5), settings.languageVersion)
    }

    @Test
    fun `configuration factory injects default language settings`() {
        val configuration = CangjieCompilerConfigurationFactory.fromArguments()

        assertEquals(LanguageVersions.V_1_0_5, configuration.languageVersionSettings.languageVersion)
        assertTrue(configuration.languageVersionSettings.enabledFeatures.isEmpty())
    }

    @Test
    fun `fails for unknown language feature`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            LanguageSettingsArgumentParser.parse(
                CommonCompilerArguments(languageFeatures = listOf("UnknownFeature")),
            )
        }

        assertTrue(error.message?.contains("Unknown language feature(s)") == true)
    }
}
