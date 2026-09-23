package org.cangnova.cangjie

import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.configureLanguageVersionSettings
import org.cangnova.cangjie.config.create
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Verifies that the language version argument is owned by compiler config. */
class LanguageVersionSettingsConfiguratorTest {
    @Test
    fun explicitLanguageVersionIsAppliedWithoutRebuildingFeatures() {
        val messages = RecordingMessageCollector()
        val configuration = CompilerConfiguration.create(messageCollector = messages)
        configuration.languageVersionSettings = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_0_0,
            specificFeatures = mapOf(
                LanguageFeature.ObjCInteropAnnotations to LanguageFeature.State.DISABLED,
            ),
        )

        assertTrue(configuration.configureLanguageVersionSettings("1.1.3"))
        assertEquals(LanguageVersion.CANGJIE_1_1_3, configuration.languageVersionSettings.languageVersion)
        assertEquals(
            LanguageFeature.State.DISABLED,
            configuration.languageVersionSettings.getFeatureSupport(LanguageFeature.ObjCInteropAnnotations),
        )
        assertFalse(messages.hasErrors())
    }

    @Test
    fun unknownLanguageVersionIsReportedAsError() {
        val messages = RecordingMessageCollector()
        val configuration = CompilerConfiguration.create(messageCollector = messages)

        assertFalse(configuration.configureLanguageVersionSettings("9.9.9"))
        assertTrue(messages.hasErrors())
    }

    private class RecordingMessageCollector : MessageCollector {
        private var errors = false

        override fun clear() {
            errors = false
        }

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?,
        ) {
            if (severity == CompilerMessageSeverity.ERROR) errors = true
        }

        override fun hasErrors(): Boolean = errors
    }
}
