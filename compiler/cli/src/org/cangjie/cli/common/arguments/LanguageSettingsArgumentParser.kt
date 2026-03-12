package org.cangjie.cli.common.arguments

import org.cangjie.config.LanguageFeature
import org.cangjie.config.LanguageVersion
import org.cangjie.config.LanguageVersionSettings
import org.cangjie.config.LanguageVersions

object LanguageSettingsArgumentParser {
    fun parse(arguments: CommonCompilerArguments): LanguageVersionSettings {
        val languageVersion = parseLanguageVersion(arguments.languageVersion)
        val enabledFeatures = parseLanguageFeatures(arguments.languageFeatures)
        return LanguageVersionSettings(
            languageVersion = languageVersion,
            enabledFeatures = enabledFeatures,
        )
    }

    private fun parseLanguageVersion(rawVersion: String?): LanguageVersion {
        val text = rawVersion?.trim().orEmpty()
        if (text.isEmpty()) return LanguageVersions.LATEST_STABLE

        return LanguageVersion.parse(text)
            ?: throw IllegalArgumentException("Invalid language version '$text'. Expected format: major.minor.patch")
    }

    private fun parseLanguageFeatures(rawFeatures: List<String>): Set<LanguageFeature> {
        if (rawFeatures.isEmpty()) return emptySet()

        val parsed = linkedSetOf<LanguageFeature>()
        val unknown = mutableListOf<String>()

        for (token in rawFeatures.flatMap(::splitFeatureTokens)) {
            val feature = LanguageFeature.fromName(token)
            if (feature == null) {
                unknown += token
            } else {
                parsed += feature
            }
        }

        if (unknown.isNotEmpty()) {
            throw IllegalArgumentException("Unknown language feature(s): ${unknown.joinToString(", ")}")
        }

        return parsed
    }

    private fun splitFeatureTokens(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}

data class CangjieCompilerConfiguration(
    val languageVersionSettings: LanguageVersionSettings = LanguageVersionSettings.DEFAULT,
)

object CangjieCompilerConfigurationFactory {
    fun fromArguments(arguments: CommonCompilerArguments = CommonCompilerArguments()): CangjieCompilerConfiguration {
        return CangjieCompilerConfiguration(
            languageVersionSettings = LanguageSettingsArgumentParser.parse(arguments),
        )
    }
}
