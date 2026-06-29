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


/**
 * 表示 `LastBefore`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
 */
class LastBefore private constructor(
    /**
     * 保存 `lookFor` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val lookFor: TokenStreamPredicate,
    /**
     * 保存 `stopAt` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val stopAt: TokenStreamPredicate,
    /**
     * 保存 `dontStopRightAfterOccurrence` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private val dontStopRightAfterOccurrence: Boolean
) : AbstractTokenStreamPattern() {
    /**
     * 保存 `previousLookForResult` 的内部状态，供仓颉语法解析实现维护节点缓存或解析上下文。
     */
    private var previousLookForResult = false

    constructor(lookFor: TokenStreamPredicate, stopAt: TokenStreamPredicate) : this(lookFor, stopAt, false)

    /**
     * 实现 `processToken` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun processToken(offset: Int, topLevel: Boolean): Boolean {
        val lookForResult = lookFor.matching(topLevel)
        if (lookForResult) {
            lastOccurrence = offset
        }
        if (stopAt.matching(topLevel)) {
            if (topLevel
                && (!dontStopRightAfterOccurrence
                        || !previousLookForResult)
            ) return true
        }
        previousLookForResult = lookForResult
        return false
    }

    /**
     * 实现 `reset` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun reset() {
        super.reset()
        previousLookForResult = false
    }
}
