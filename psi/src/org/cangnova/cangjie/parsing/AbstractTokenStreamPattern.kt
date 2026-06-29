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
package org.cangnova.cangjie.parsing

import com.intellij.psi.tree.IElementType


/**
 * 表示 `AbstractTokenStreamPattern`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
 */
abstract class AbstractTokenStreamPattern : TokenStreamPattern {
    /**
     * 保存 `lastOccurrence`，供仓颉语法解析流程读取节点结构或语义信息。
     */
    protected var lastOccurrence: Int = -1

    /**
     * 提供 `fail` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
     */
    protected fun fail() {
        lastOccurrence = -1
    }

    /**
     * 实现 `result` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun result(): Int {
        return lastOccurrence
    }

    /**
     * 实现 `isTopLevel` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isTopLevel(openAngleBrackets: Int, openBrackets: Int, openBraces: Int, openParentheses: Int): Boolean {
        return openBraces == 0 && openBrackets == 0 && openParentheses == 0 && openAngleBrackets == 0
    }

    /**
     * 实现 `handleUnmatchedClosing` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun handleUnmatchedClosing(token: IElementType?): Boolean {
        return false
    }

    /**
     * 提供 `reset` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
     */
    open fun reset() {
        lastOccurrence = -1
    }
}
