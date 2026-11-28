package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.messages.ParsingDiagnosticSettings

/** 文件、惰性正文及宏重解析共用的不可变调用配置；警告选项与语言版本独立。 */
data class CjParserSettings(
    val languageVersion: LanguageVersion = LanguageVersion.LATEST_STABLE,
    val diagnostics: ParsingDiagnosticSettings = ParsingDiagnosticSettings(),
    val backend: CjParsingBackend = CjParsingBackend.CJNATIVE,
)

/** 官方 parser 的后端选择只影响真实 SetUnsafe 报告点，不由源文件角色决定。 */
enum class CjParsingBackend(val optionName: String) { CJNATIVE("cjnative"), UNKNOWN("unknown") }
