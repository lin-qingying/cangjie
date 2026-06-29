

package org.cangnova.cangjie.cfir.diagnostics.rendering

import org.cangnova.cangjie.LanguageFeature

/**
 * 将普通诊断消息扩展为语言特性废弃警告消息。
 */
fun String.toDeprecationWarningMessage(deprecatingFeature: LanguageFeature): String {
    return buildString {
        append(this@toDeprecationWarningMessage)
        when {
            endsWith(".") -> append(" ")
            lastOrNull()?.isWhitespace() == true -> {}
            else -> append(". ")
        }
        appendDeprecationWarningSuffix(deprecatingFeature)
    }
}

/**
 * 向当前字符串构造器追加废弃警告的版本后缀。
 */
fun StringBuilder.appendDeprecationWarningSuffix(deprecatingFeature: LanguageFeature) {
    append("This will become an error ")
    appendVersion(deprecatingFeature)
    append(".")
}

/**
 * 追加废弃特性未来变为错误的版本描述。
 */
fun StringBuilder.appendVersion(deprecatingFeature: LanguageFeature) {
    append("in a future release")
}
