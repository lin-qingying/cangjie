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

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure

/**
 * 提供 `CangJieLightParser` 单例，集中承载仓颉语法解析的共享状态、工厂或工具行为。
 */
object CangJieLightParser {
    /**
     * 提供 `parse` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
     */
    fun parse(
        builder: PsiBuilder,
        errorListener: LightTreeParsingErrorListener? = null,
    ): FlyweightCapableTreeStructure<LighterASTNode> {
        val cjParsing: CangJieParsing = CangJieParsing.createForTopLevelNonLazy(
            SemanticWhitespaceAwarePsiBuilderImpl(builder),
        )
        cjParsing.parseFile()
        return builder.lightTree.also { lightTree ->
            if (errorListener != null) {
                reportErrors(lightTree.root, lightTree, errorListener)
            }
        }
    }

    /**
     * 提供 `interface` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
     */
    fun interface LightTreeParsingErrorListener {
        fun onError(startOffset: Int, endOffset: Int, message: String?)
    }

    /**
     * 执行 `reportErrors` 内部辅助逻辑，支撑仓颉语法解析节点的结构解析与访问。
     */
    private fun reportErrors(
        node: LighterASTNode,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
        errorListener: LightTreeParsingErrorListener,
        ref: Ref<Array<LighterASTNode?>> = Ref(),
    ) {
        tree.getChildren(node, ref)
        val children = ref.get() ?: return

        for (child in children) {
            if (child == null) break
            if (child.tokenType == TokenType.ERROR_ELEMENT) {
                errorListener.onError(child.startOffset, child.endOffset, PsiBuilderImpl.getErrorMessage(child))
            }

            ref.set(null)
            reportErrors(child, tree, errorListener, ref)
        }
    }
}
