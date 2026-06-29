/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.test.codeMetaInfo.model

import org.cangnova.cangjie.test.codeMetaInfo.renderConfigurations.ParsedCodeMetaInfoRenderConfiguration

/**
 * 表示 `ParsedCodeMetaInfo`，承载测试模型中的配置数据、测试产物或处理步骤。
 */
class ParsedCodeMetaInfo(
    /**
     * 保存 `start`，供测试模型在测试执行期间读取或传递。
     */
    override val start: Int,
    /**
     * 保存 `end`，供测试模型在测试执行期间读取或传递。
     */
    override val end: Int,
    /**
     * 保存 `attributes`，供测试模型在测试执行期间读取或传递。
     */
    override val attributes: MutableList<String>,
    /**
     * 保存 `tag`，供测试模型在测试执行期间读取或传递。
     */
    override val tag: String,
    /**
     * 保存 `description`，供测试模型在测试执行期间读取或传递。
     */
    val description: String?
) : CodeMetaInfo {
    /**
     * 保存 `renderConfiguration`，供测试模型在测试执行期间读取或传递。
     */
    override val renderConfiguration = ParsedCodeMetaInfoRenderConfiguration

    /**
     * 执行 `asString` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    override fun asString(): String = renderConfiguration.asString(this)

    /**
     * 执行 `equals` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    override fun equals(other: Any?): Boolean {
        if (other == null || other !is CodeMetaInfo) return false
        return this.tag == other.tag && this.start == other.start && this.end == other.end
    }

    /**
     * 执行 `hashCode` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    override fun hashCode(): Int {
        var result = start
        result = 31 * result + end
        result = 31 * result + tag.hashCode()
        return result
    }

    /**
     * 执行 `copy` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    fun copy(): ParsedCodeMetaInfo {
        return ParsedCodeMetaInfo(start, end, attributes.toMutableList(), tag, description)
    }
}
