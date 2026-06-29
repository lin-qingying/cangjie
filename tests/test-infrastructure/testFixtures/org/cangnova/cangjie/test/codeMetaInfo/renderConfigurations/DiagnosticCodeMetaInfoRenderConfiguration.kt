/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.test.codeMetaInfo.renderConfigurations

import org.cangnova.cangjie.test.codeMetaInfo.model.CodeMetaInfo
import org.cangnova.cangjie.test.codeMetaInfo.model.DiagnosticCodeMetaInfo

/**
 * 表示 `DiagnosticCodeMetaInfoRenderConfiguration`，承载代码元信息测试中的配置数据、测试产物或处理步骤。
 */
open class DiagnosticCodeMetaInfoRenderConfiguration(
    /**
     * 保存 `withNewInference`，供代码元信息测试在测试执行期间读取或传递。
     */
    val withNewInference: Boolean = true,
    /**
     * 保存 `renderSeverity`，供代码元信息测试在测试执行期间读取或传递。
     */
    val renderSeverity: Boolean = false
) : AbstractCodeMetaInfoRenderConfiguration() {
    /**
     * 保存 `crossPlatformLineBreak`，供代码元信息测试在测试执行期间读取或传递。
     */
    private val crossPlatformLineBreak = """\r?\n""".toRegex()

    /**
     * 执行 `asString` 对应的代码元信息测试流程，维持测试框架的阶段契约。
     */
    override fun asString(codeMetaInfo: CodeMetaInfo): String {
        if (codeMetaInfo !is DiagnosticCodeMetaInfo) return ""
        return (getTag(codeMetaInfo)
                + getAttributesString(codeMetaInfo)
                + getParamsString(codeMetaInfo))
            .replace(crossPlatformLineBreak, "")
    }

    /**
     * 提供 `getParamsString` 对应的代码元信息测试流程，维持测试框架的阶段契约。
     */
    private fun getParamsString(codeMetaInfo: DiagnosticCodeMetaInfo): String {
        if (!renderParams) return ""
        val params = mutableListOf<String>()

        val renderer = codeMetaInfo.diagnostic.factory.cjRenderer
        renderer.renderParameters(codeMetaInfo.diagnostic).mapTo(params) {
            it.toString().replace("\"", "\\\"")
        }
        if (renderSeverity)
            params.add("severity='${codeMetaInfo.diagnostic.severity}'")

        params.add(getAdditionalParams(codeMetaInfo))

        return "(\"${params.filter { it.isNotEmpty() }.joinToString("; ")}\")"
    }

    /**
     * 提供 `getTag` 对应的代码元信息测试流程，维持测试框架的阶段契约。
     */
    open fun getTag(codeMetaInfo: DiagnosticCodeMetaInfo): String {
        return codeMetaInfo.diagnostic.factory.name.removePrefix("CFIR_")
    }
}
