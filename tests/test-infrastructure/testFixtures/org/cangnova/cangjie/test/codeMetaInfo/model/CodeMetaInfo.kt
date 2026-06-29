/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.test.codeMetaInfo.model

import org.cangnova.cangjie.test.codeMetaInfo.renderConfigurations.AbstractCodeMetaInfoRenderConfiguration

/**
 * 定义 `CodeMetaInfo` 接口，约束测试模型参与者需要暴露的协作能力。
 */
interface CodeMetaInfo {
    /**
     * 保存 `start`，供测试模型在测试执行期间读取或传递。
     */
    val start: Int
    /**
     * 保存 `end`，供测试模型在测试执行期间读取或传递。
     */
    val end: Int
    /**
     * 保存 `tag`，供测试模型在测试执行期间读取或传递。
     */
    val tag: String
    /**
     * 保存 `renderConfiguration`，供测试模型在测试执行期间读取或传递。
     */
    val renderConfiguration: AbstractCodeMetaInfoRenderConfiguration
    /**
     * 保存 `attributes`，供测试模型在测试执行期间读取或传递。
     */
    val attributes: MutableList<String>

    /**
     * 保存 `tagPrefix`，供测试模型在测试执行期间读取或传递。
     */
    val tagPrefix: String get() = "<!"
    /**
     * 保存 `tagPostfix`，供测试模型在测试执行期间读取或传递。
     */
    val tagPostfix: String get() = "!>"
    /**
     * 保存 `closingTag`，供测试模型在测试执行期间读取或传递。
     */
    val closingTag: String get() = "<!>"

    /**
     * 执行 `asString` 对应的测试模型流程，维持测试框架的阶段契约。
     */
    fun asString(): String
}
