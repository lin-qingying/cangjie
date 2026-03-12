/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.cfir.diagnostics.rendering

import org.cangjie.config.LanguageFeature

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
