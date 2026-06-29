/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.test.codeMetaInfo.renderConfigurations

import org.cangnova.cangjie.test.codeMetaInfo.model.CodeMetaInfo
import org.cangnova.cangjie.test.codeMetaInfo.model.ParsedCodeMetaInfo

/**
 * 提供 `ParsedCodeMetaInfoRenderConfiguration` 单例，集中承载代码元信息测试的共享状态、常量或默认行为。
 */
object ParsedCodeMetaInfoRenderConfiguration : AbstractCodeMetaInfoRenderConfiguration() {
    /**
     * 执行 `asString` 对应的代码元信息测试流程，维持测试框架的阶段契约。
     */
    override fun asString(codeMetaInfo: CodeMetaInfo): String {
        require(codeMetaInfo is ParsedCodeMetaInfo)
        return super.asString(codeMetaInfo) + (codeMetaInfo.description?.let { "(\"$it\")" } ?: "")
    }
}
