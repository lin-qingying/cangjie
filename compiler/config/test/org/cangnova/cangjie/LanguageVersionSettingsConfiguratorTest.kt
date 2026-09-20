package org.cangnova.cangjie

import org.cangnova.cangjie.config.ApiVersion
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

/** Verifies that language/API version arguments are owned by compiler config. */
class LanguageVersionSettingsConfiguratorTest {
    @Test
    fun explicitApiVersionIsAppliedWithoutRebuildingFeatures() {
        val messages = RecordingMessageCollector()
        val configuration = CompilerConfiguration.create(messageCollector = messages)
        configuration.languageVersionSettings = LanguageVersionSettingsImpl(
            languageVersion = LanguageVersion.CANGJIE_1_1_3,
            apiVersion = ApiVersion.CANGJIE_1_1_3,
            specificFeatures = mapOf(
                LanguageFeature.ObjCInteropAnnotations to LanguageFeature.State.DISABLED,
            ),
        )

        assertTrue(configuration.configureLanguageVersionSettings("1.1.3", "1.1.0"))
        assertEquals(ApiVersion.CANGJIE_1_1_0, configuration.languageVersionSettings.apiVersion)
        assertEquals(
            LanguageFeature.State.DISABLED,
            configuration.languageVersionSettings.getFeatureSupport(LanguageFeature.ObjCInteropAnnotations),
        )
        assertFalse(messages.hasErrors())
    }

    @Test
    fun apiVersionCannotExceedLanguageVersion() {
        val messages = RecordingMessageCollector()
        val configuration = CompilerConfiguration.create(messageCollector = messages)

        assertFalse(configuration.configureLanguageVersionSettings("1.0.5", "1.1.0"))
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
