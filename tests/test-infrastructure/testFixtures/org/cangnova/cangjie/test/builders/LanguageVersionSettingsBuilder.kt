package org.cangnova.cangjie.test.builders

import org.cangnova.cangjie.config.AnalysisFlag
import org.cangnova.cangjie.config.AnalysisFlags
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersion
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.config.LanguageVersions
import org.cangnova.cangjie.config.WarningLevel
import org.cangnova.cangjie.test.directives.LanguageSettingsDirectives
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.singleOrZeroValue
import org.cangnova.cangjie.test.services.AbstractEnvironmentConfigurator
import org.cangnova.cangjie.test.services.DefaultsDsl

@DefaultsDsl
class LanguageVersionSettingsBuilder {
    companion object {
        fun fromExistingSettings(builder: LanguageVersionSettingsBuilder): LanguageVersionSettingsBuilder {
            return LanguageVersionSettingsBuilder().apply {
                languageVersion = builder.languageVersion
                enabledFeatures += builder.enabledFeatures
                analysisFlags += builder.analysisFlags
            }
        }
    }

    var languageVersion: LanguageVersion = LanguageVersions.LATEST_STABLE

    private val enabledFeatures: MutableSet<LanguageFeature> = mutableSetOf()
    private val analysisFlags: MutableMap<AnalysisFlag<*>, Any?> = mutableMapOf()

    fun enable(feature: LanguageFeature) {
        enabledFeatures += feature
    }

    fun disable(feature: LanguageFeature) {
        enabledFeatures -= feature
    }

    fun <T> withFlag(flag: AnalysisFlag<T>, value: T) {
        if (value == flag.defaultValue) {
            analysisFlags.remove(flag)
        } else {
            analysisFlags[flag] = value
        }
    }

    fun configureUsingDirectives(
        directives: RegisteredDirectives,
        environmentConfigurators: List<AbstractEnvironmentConfigurator>,
        @Suppress("UNUSED_PARAMETER") useK2: Boolean,
    ) {
        directives.singleOrZeroValue(LanguageSettingsDirectives.LANGUAGE_VERSION)?.let { value ->
            languageVersion = LanguageVersion.parse(value)
                ?: error("Invalid LANGUAGE_VERSION '$value'. Expected format: major.minor.patch")
        }

        directives[LanguageSettingsDirectives.SUPPRESS_WARNINGS]
            .takeIf { it.isNotEmpty() }
            ?.let { warningNames ->
                withFlag(AnalysisFlags.warningLevels, warningNames.associateWith { WarningLevel.Disabled })
            }

        directives[LanguageSettingsDirectives.LANGUAGE].forEach { featureDirective ->
            parseLanguageFeature(featureDirective)
        }

        environmentConfigurators.forEach { configurator ->
            configurator.provideAdditionalAnalysisFlags(directives, languageVersion).forEach { (flag, value) ->
                @Suppress("UNCHECKED_CAST")
                withFlag(flag as AnalysisFlag<Any?>, value)
            }
        }
    }

    private fun parseLanguageFeature(featureString: String) {
        val trimmed = featureString.trim()
        if (trimmed.isEmpty()) return

        val mode = trimmed.first()
        val featureName = if (mode == '+' || mode == '-') trimmed.drop(1) else trimmed
        val feature = LanguageFeature.fromName(featureName)
            ?: error("Unknown language feature '$featureName' in directive '$featureString'")

        if (mode == '-') {
            disable(feature)
        } else {
            enable(feature)
        }
    }

    fun build(): LanguageVersionSettings {
        return LanguageVersionSettings(
            languageVersion = languageVersion,
            enabledFeatures = enabledFeatures.toSet(),
            analysisFlags = analysisFlags.toMap(),
        )
    }
}
