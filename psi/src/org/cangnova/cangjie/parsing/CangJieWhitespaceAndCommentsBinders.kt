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

import org.cangnova.cangjie.lexer.CjTokens
import com.intellij.lang.WhitespacesAndCommentsBinder
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.tree.IElementType

/**
 * 提供 `PrecedingCommentsBinder` 单例，集中承载仓颉语法解析的共享状态、工厂或工具行为。
 */
object PrecedingCommentsBinder : WhitespacesAndCommentsBinder {
    /**
     * 实现 `getEdgePosition` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getEdgePosition(
        tokens: List<IElementType>,
        atStreamEdge: Boolean,
        getter: WhitespacesAndCommentsBinder.TokenTextGetter,
    ): Int {
        if (tokens.isEmpty()) return 0

        // 1. 绑定文档注释
        for (idx in tokens.indices.reversed()) {
            if (tokens[idx] == CjTokens.DOC_COMMENT) return idx
        }

        // 2.绑定注释
        var result = tokens.size
        tokens@ for (idx in tokens.indices.reversed()) {
            val tokenType = tokens[idx]
            when (tokenType) {
                CjTokens.WHITE_SPACE -> if (StringUtil.getLineBreakCount(getter[idx]) > 1) break@tokens

                in CjTokens.COMMENTS -> {
                    if (idx == 0 || tokens[idx - 1] == CjTokens.WHITE_SPACE && StringUtil.containsLineBreak(getter[idx - 1])) {
                        result = idx
                    }
                }

                else -> break@tokens
            }
        }

        return result
    }
}

/**
 * 提供 `PrecedingDocCommentsBinder` 单例，集中承载仓颉语法解析的共享状态、工厂或工具行为。
 */
object PrecedingDocCommentsBinder : WhitespacesAndCommentsBinder {
    /**
     * 实现 `getEdgePosition` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getEdgePosition(
        tokens: List<IElementType>,
        atStreamEdge: Boolean,
        getter: WhitespacesAndCommentsBinder.TokenTextGetter,
    ): Int {
        if (tokens.isEmpty()) return 0

        for (idx in tokens.indices.reversed()) {
            if (tokens[idx] == CjTokens.DOC_COMMENT) return idx
        }

        return tokens.size
    }
}

// 绑定行注释
/**
 * 提供 `TrailingCommentsBinder` 单例，集中承载仓颉语法解析的共享状态、工厂或工具行为。
 */
object TrailingCommentsBinder : WhitespacesAndCommentsBinder {
    /**
     * 实现 `getEdgePosition` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getEdgePosition(
        tokens: List<IElementType>,
        atStreamEdge: Boolean,
        getter: WhitespacesAndCommentsBinder.TokenTextGetter,
    ): Int {
        if (tokens.isEmpty()) return 0

        var result = 0
        tokens@ for (idx in tokens.indices) {
            val tokenType = tokens[idx]
            when (tokenType) {
                CjTokens.WHITE_SPACE -> if (StringUtil.containsLineBreak(getter[idx])) break@tokens

                CjTokens.EOL_COMMENT, CjTokens.BLOCK_COMMENT -> result = idx + 1

                else -> break@tokens
            }
        }

        return result
    }
}

/**
 * 表示 `AllCommentsBinder`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
 */
private class AllCommentsBinder(val isTrailing: Boolean) : WhitespacesAndCommentsBinder {
    /**
     * 实现 `getEdgePosition` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getEdgePosition(
        tokens: List<IElementType>,
        atStreamEdge: Boolean,
        getter: WhitespacesAndCommentsBinder.TokenTextGetter,
    ): Int {
        if (tokens.isEmpty()) return 0

        val size = tokens.size

        val endToken = tokens[if (isTrailing) size - 1 else 0]
        val shift = if (endToken == CjTokens.WHITE_SPACE) 1 else 0

        return if (isTrailing) size - shift else shift
    }
}

/**
 * 保存 `PRECEDING_ALL_COMMENTS_BINDER`，供仓颉语法解析流程读取节点结构或语义信息。
 */
@JvmField
val PRECEDING_ALL_COMMENTS_BINDER: WhitespacesAndCommentsBinder = AllCommentsBinder(false)

/**
 * 保存 `TRAILING_ALL_COMMENTS_BINDER`，供仓颉语法解析流程读取节点结构或语义信息。
 */
@JvmField
val TRAILING_ALL_COMMENTS_BINDER: WhitespacesAndCommentsBinder = AllCommentsBinder(true)

/**
 * 提供 `DoNotBindAnything` 单例，集中承载仓颉语法解析的共享状态、工厂或工具行为。
 */
object DoNotBindAnything : WhitespacesAndCommentsBinder {
    /**
     * 实现 `getEdgePosition` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getEdgePosition(
        tokens: List<IElementType>,
        atStreamEdge: Boolean,
        getter: WhitespacesAndCommentsBinder.TokenTextGetter,
    ): Int {
        return 0
    }
}

/**
 * 提供 `BindFirstShebangWithWhitespaceOnly` 单例，集中承载仓颉语法解析的共享状态、工厂或工具行为。
 */
object BindFirstShebangWithWhitespaceOnly : WhitespacesAndCommentsBinder {
    /**
     * 实现 `getEdgePosition` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getEdgePosition(
        tokens: List<IElementType>,
        atStreamEdge: Boolean,
        getter: WhitespacesAndCommentsBinder.TokenTextGetter,
    ): Int {
        if (tokens.firstOrNull() == CjTokens.SHEBANG_COMMENT) {
            return if (tokens.getOrNull(1) == CjTokens.WHITE_SPACE) 2 else 1
        }

        return 0
    }
}

/**
 * 表示 `BindAll`，承载仓颉语法解析中的语法节点、索引桩或辅助模型。
 */
class BindAll(val isTrailing: Boolean) : WhitespacesAndCommentsBinder {
    /**
     * 实现 `getEdgePosition` 的仓颉语法解析协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getEdgePosition(
        tokens: List<IElementType>,
        atStreamEdge: Boolean,
        getter: WhitespacesAndCommentsBinder.TokenTextGetter,
    ): Int {
        return if (!isTrailing) 0 else tokens.size
    }
}

/**
 * 保存 `PRECEDING_ALL_BINDER`，供仓颉语法解析流程读取节点结构或语义信息。
 */
@JvmField
val PRECEDING_ALL_BINDER: WhitespacesAndCommentsBinder = BindAll(false)

/**
 * 保存 `TRAILING_ALL_BINDER`，供仓颉语法解析流程读取节点结构或语义信息。
 */
@JvmField
val TRAILING_ALL_BINDER: WhitespacesAndCommentsBinder = BindAll(true)
