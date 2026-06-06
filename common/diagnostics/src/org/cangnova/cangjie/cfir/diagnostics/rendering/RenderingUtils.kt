

package org.cangnova.cangjie.cfir.diagnostics.rendering

import org.cangnova.cangjie.LanguageFeature

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

fun StringBuilder.appendDeprecationWarningSuffix(deprecatingFeature: LanguageFeature) {
    append("This will become an error ")
    appendVersion(deprecatingFeature)
    append(".")
}

fun StringBuilder.appendVersion(deprecatingFeature: LanguageFeature) {
    append("in a future release")
}
