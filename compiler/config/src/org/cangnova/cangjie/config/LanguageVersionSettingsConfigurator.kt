package org.cangnova.cangjie.config

import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.messages.CompilerMessageSeverity

/**
 * 编译配置阶段的唯一语言/API 版本 settings owner。
 *
 * Kotlin 在进入 FIR session 前由 CLI configurator 完成同一项工作；前端
 * pipeline 只消费已经配置好的 settings，不在 phase 内重新解释参数文本。
 */
fun CompilerConfiguration.configureLanguageVersionSettings(
    rawLanguageVersion: String?,
    rawApiVersion: String?,
): Boolean {
    val current = languageVersionSettings
    val languageVersion = rawLanguageVersion?.let { raw ->
        try {
            LanguageVersion.parse(raw)
        } catch (error: IllegalStateException) {
            messageCollector.report(
                CompilerMessageSeverity.ERROR,
                "Invalid language version '$raw': ${error.message ?: "unknown version"}",
            )
            return false
        }
    } ?: current.languageVersion

    if (languageVersion.isUnsupported) {
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            "Language version '${languageVersion.versionString}' is not supported",
        )
        return false
    }

    val languageMaximumApi = ApiVersion.createByLanguageVersion(languageVersion)
    val apiVersion = rawApiVersion?.let { raw ->
        ApiVersion.parse(raw) ?: run {
            messageCollector.report(CompilerMessageSeverity.ERROR, "Invalid API version '$raw'")
            return false
        }
    } ?: if (rawLanguageVersion != null) languageMaximumApi else current.apiVersion

    if (apiVersion > languageMaximumApi) {
        messageCollector.report(
            CompilerMessageSeverity.ERROR,
            "API version '${apiVersion.versionString}' cannot be used with " +
                "language version '${languageVersion.versionString}'",
        )
        return false
    }

    val versioned = LanguageVersionSettingsImpl(
        languageVersion = languageVersion,
        apiVersion = apiVersion,
        specificFeatures = current.getCustomizedLanguageFeatures(),
    )
    languageVersionSettings = object : LanguageVersionSettings by versioned {
        override fun <T> getFlag(flag: org.cangnova.cangjie.AnalysisFlag<T>): T = current.getFlag(flag)
    }
    return true
}
