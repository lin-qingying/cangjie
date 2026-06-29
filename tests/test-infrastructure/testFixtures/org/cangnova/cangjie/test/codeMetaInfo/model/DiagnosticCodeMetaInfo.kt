/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.test.codeMetaInfo.model

import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.test.codeMetaInfo.renderConfigurations.DiagnosticCodeMetaInfoRenderConfiguration

/**
 * 表示 `DiagnosticCodeMetaInfo`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
class DiagnosticCodeMetaInfo(
    /**
     * 保存 `start`，供测试模型在测试执行期间读取或传递。
     */
    override val start: Int,
    /**
     * 保存 `end`，供测试模型在测试执行期间读取或传递。
     */
    override val end: Int,
    renderConfiguration: DiagnosticCodeMetaInfoRenderConfiguration,
    /**
     * 保存 `diagnostic`，供测试模型在测试执行期间读取或传递。
     */
    val diagnostic: CjDiagnostic
) : CodeMetaInfo {
    constructor(
        range: TextRange,
        renderConfiguration: DiagnosticCodeMetaInfoRenderConfiguration,
        diagnostic: CjDiagnostic
    ) : this(range.startOffset, range.endOffset, renderConfiguration, diagnostic)

    /**
     * 维护 `renderConfiguration`，供测试模型在测试执行期间读取或传递。
     */
    override var renderConfiguration: DiagnosticCodeMetaInfoRenderConfiguration = renderConfiguration
        private set

    /**
     * 执行 `replaceRenderConfiguration` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    fun replaceRenderConfiguration(renderConfiguration: DiagnosticCodeMetaInfoRenderConfiguration) {
        this.renderConfiguration = renderConfiguration
    }

    /**
     * 保存 `tag`，供测试模型在测试执行期间读取或传递。
     */
    override val tag: String
        get() = renderConfiguration.getTag(this)

    /**
     * 保存 `attributes`，供测试模型在测试执行期间读取或传递。
     */
    override val attributes: MutableList<String> = mutableListOf()

    /**
     * 执行 `asString` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    override fun asString(): String = renderConfiguration.asString(this)
}
