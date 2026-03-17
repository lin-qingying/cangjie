/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.test.codeMetaInfo.renderConfigurations

import org.cangnova.cangjie.cfir.diagnostics.Diagnostic
import org.cangnova.cangjie.cfir.diagnostics.rendering.DiagnosticRenderer
import org.cangnova.cangjie.test.codeMetaInfo.model.CodeMetaInfo
import org.cangnova.cangjie.test.codeMetaInfo.model.DiagnosticCodeMetaInfo

open class DiagnosticCodeMetaInfoRenderConfiguration(
    val withNewInference: Boolean = true,
    val renderSeverity: Boolean = false
) : AbstractCodeMetaInfoRenderConfiguration() {
    private val crossPlatformLineBreak = """\r?\n""".toRegex()

    override fun asString(codeMetaInfo: CodeMetaInfo): String {
        if (codeMetaInfo !is DiagnosticCodeMetaInfo) return ""
        return (getTag(codeMetaInfo)
                + getAttributesString(codeMetaInfo)
                + getParamsString(codeMetaInfo))
            .replace(crossPlatformLineBreak, "")
    }

    private fun getParamsString(codeMetaInfo: DiagnosticCodeMetaInfo): String {
        if (!renderParams) return ""
        val params = mutableListOf<String>()

        @Suppress("UNCHECKED_CAST")
        val renderer = codeMetaInfo.diagnostic.factory.defaultRenderer as? DiagnosticRenderer<Diagnostic>
        if (renderer != null) {
            renderer.renderParameters(codeMetaInfo.diagnostic).mapTo(params) {
                it.toString().replace("\"", "\\\"")
            }
        }
        if (renderSeverity)
            params.add("severity='${codeMetaInfo.diagnostic.severity}'")

        params.add(getAdditionalParams(codeMetaInfo))

        return "(\"${params.filter { it.isNotEmpty() }.joinToString("; ")}\")"
    }

    fun getTag(codeMetaInfo: DiagnosticCodeMetaInfo): String {
        return codeMetaInfo.diagnostic.factory.name
    }
}
