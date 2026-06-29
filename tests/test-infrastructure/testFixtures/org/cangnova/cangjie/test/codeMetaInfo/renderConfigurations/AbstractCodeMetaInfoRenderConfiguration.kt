/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.test.codeMetaInfo.renderConfigurations

import com.intellij.openapi.util.text.StringUtil
import org.cangnova.cangjie.test.codeMetaInfo.model.CodeMetaInfo

/**
 * 表示 `AbstractCodeMetaInfoRenderConfiguration`，承载代码元信息测试中的配置数据、测试产物或处理步骤。
 */
abstract class AbstractCodeMetaInfoRenderConfiguration(var renderParams: Boolean = true) {
    /**
     * 保存 `clickOrPressRegex`，供代码元信息测试在测试执行期间读取或传递。
     */
    private val clickOrPressRegex = "(Click or press|Press).*(to navigate)".toRegex() // We have different hotkeys on different platforms
    /**
     * 提供 `asString` 对应的代码元信息测试流程，维持测试框架的阶段契约。
     */
    open fun asString(codeMetaInfo: CodeMetaInfo): String = codeMetaInfo.tag + getAttributesString(codeMetaInfo)

    /**
     * 提供 `getAdditionalParams` 对应的代码元信息测试流程，维持测试框架的阶段契约。
     */
    open fun getAdditionalParams(codeMetaInfo: CodeMetaInfo) = ""

    /**
     * 提供 `postProcessAttributes` 对应的代码元信息测试流程，维持测试框架的阶段契约。
     */
    open fun postProcessAttributes(codeMetaInfo: CodeMetaInfo) {}

    /**
     * 提供 `sanitizeLineMarkerTooltip` 对应的代码元信息测试流程，维持测试框架的阶段契约。
     */
    protected fun sanitizeLineMarkerTooltip(originalText: String?): String {
        if (originalText == null) return "null"
        val noHtmlTags = StringUtil.removeHtmlTags(originalText)
            .replace(" ", "")
            .replace(clickOrPressRegex, "$1 ... $2")
            .trim()
        return sanitizeLineBreaks(noHtmlTags)
    }

    /**
     * 提供 `sanitizeLineBreaks` 对应的代码元信息测试流程，维持测试框架的阶段契约。
     */
    protected fun sanitizeLineBreaks(originalText: String): String {
        var sanitizedText = originalText
        sanitizedText = StringUtil.replace(sanitizedText, "\r\n", " ")
        sanitizedText = StringUtil.replace(sanitizedText, "\n", " ")
        sanitizedText = StringUtil.replace(sanitizedText, "\r", " ")
        return sanitizedText
    }

    /**
     * 提供 `getAttributesString` 对应的代码元信息测试流程，维持测试框架的阶段契约。
     */
    protected fun getAttributesString(codeMetaInfo: CodeMetaInfo): String {
        if (codeMetaInfo.attributes.isEmpty()) return ""
        return "{${codeMetaInfo.attributes.joinToString(";")}}"
    }
}
