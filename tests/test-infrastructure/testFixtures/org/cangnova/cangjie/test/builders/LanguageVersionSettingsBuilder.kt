package org.cangnova.cangjie.test.builders

import org.cangnova.cangjie.AnalysisFlag
import org.cangnova.cangjie.AnalysisFlags
import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.WarningLevel
import org.cangnova.cangjie.config.ApiVersion
import org.cangnova.cangjie.test.directives.LanguageSettingsDirectives
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.singleOrZeroValue
import org.cangnova.cangjie.test.services.AbstractEnvironmentConfigurator
import org.cangnova.cangjie.test.services.DefaultsDsl

/**
 * 表示 `LanguageVersionSettingsBuilder`，承载测试配置构建中的配置数据、测试产物或处理步骤。
 */
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

    /**
     * 维护 `languageVersion`，供测试配置构建在测试执行期间读取或传递。
     */
    var languageVersion: LanguageVersion = LanguageVersion.LATEST_STABLE

    /**
     * 保存 `enabledFeatures`，供测试配置构建在测试执行期间读取或传递。
     */
    private val enabledFeatures: MutableSet<LanguageFeature> = mutableSetOf()
    /**
     * 保存 `analysisFlags`，供测试配置构建在测试执行期间读取或传递。
     */
    private val analysisFlags: MutableMap<AnalysisFlag<*>, Any?> = mutableMapOf()

    /**
     * 执行 `enable` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun enable(feature: LanguageFeature) {
        enabledFeatures += feature
    }

    /**
     * 执行 `disable` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun disable(feature: LanguageFeature) {
        enabledFeatures -= feature
    }

    /**
     * 执行 `withFlag` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun <T> withFlag(flag: AnalysisFlag<T>, value: T) {
        if (value == flag.defaultValue) {
            analysisFlags.remove(flag)
        } else {
            analysisFlags[flag] = value
        }
    }

    /**
     * 执行 `configureUsingDirectives` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
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

    /**
     * 提供 `parseLanguageFeature` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
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

    /**
     * 执行 `build` 对应的测试配置构建流程，维持测试框架的阶段契约。
     */
    fun build(): LanguageVersionSettings {
        return LanguageVersionSettingsImpl(
            languageVersion = languageVersion,
            apiVersion = ApiVersion.LATEST_STABLE,
            analysisFlags = analysisFlags.toMap(),
            specificFeatures = enabledFeatures.toSet().associateWith { LanguageFeature.State.ENABLED },
        )
    }
}
