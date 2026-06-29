/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.lexer.cdoc

import com.intellij.lang.documentation.DocumentationMarkup.*

/**
 * 表示 `CDocTemplate`，承载仓颉词法与文档注释中的语法节点、索引桩或辅助模型。
 */
open class CDocTemplate : Template<StringBuilder> {
    /**
     * 保存 `definition`，供仓颉词法与文档注释流程读取节点结构或语义信息。
     */
    val definition = Placeholder<StringBuilder>()

    /**
     * 保存 `description`，供仓颉词法与文档注释流程读取节点结构或语义信息。
     */
    val description = Placeholder<StringBuilder>()

    /**
     * 保存 `deprecation`，供仓颉词法与文档注释流程读取节点结构或语义信息。
     */
    val deprecation = Placeholder<StringBuilder>()

    /**
     * 保存 `containerInfo`，供仓颉词法与文档注释流程读取节点结构或语义信息。
     */
    val containerInfo = Placeholder<StringBuilder>()

    /**
     * 实现 `apply` 的仓颉词法与文档注释协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun StringBuilder.apply() {
        append(DEFINITION_START)
        insert(definition)
        append(DEFINITION_END)

        if (!deprecation.isEmpty()) {
            append(SECTIONS_START)
            insert(deprecation)
            append(SECTIONS_END)
        }

        insert(description)

        if (!containerInfo.isEmpty()) {
            append("<div class='bottom'>")
            insert(containerInfo)
            append("</div>")
        }
    }

    /**
     * 表示 `DescriptionBodyTemplate`，承载仓颉词法与文档注释中的语法节点、索引桩或辅助模型。
     */
    sealed class DescriptionBodyTemplate : Template<StringBuilder> {
        /**
         * 表示 `CangJie`，承载仓颉词法与文档注释中的语法节点、索引桩或辅助模型。
         */
        class CangJie : DescriptionBodyTemplate() {
            /**
             * 保存 `content`，供仓颉词法与文档注释流程读取节点结构或语义信息。
             */
            val content = Placeholder<StringBuilder>()
            /**
             * 保存 `sections`，供仓颉词法与文档注释流程读取节点结构或语义信息。
             */
            val sections = Placeholder<StringBuilder>()
            /**
             * 实现 `apply` 的仓颉词法与文档注释协议回调，保持与 IntelliJ PSI 访问契约一致。
             */
            override fun StringBuilder.apply() {
                val computedContent = buildString { insert(content) }
                if (computedContent.isNotBlank()) {
                    append(CONTENT_START)
                    append(computedContent)
                    append(CONTENT_END)
                }

                append(SECTIONS_START)
                insert(sections)
                append(SECTIONS_END)
            }
        }
    }

    /**
     * 表示 `NoDocTemplate`，承载仓颉词法与文档注释中的语法节点、索引桩或辅助模型。
     */
    class NoDocTemplate : CDocTemplate() {

        /**
         * 保存 `error`，供仓颉词法与文档注释流程读取节点结构或语义信息。
         */
        val error = Placeholder<StringBuilder>()

        /**
         * 实现 `apply` 的仓颉词法与文档注释协议回调，保持与 IntelliJ PSI 访问契约一致。
         */
        override fun StringBuilder.apply() {
            insert(error)
        }
    }
}
