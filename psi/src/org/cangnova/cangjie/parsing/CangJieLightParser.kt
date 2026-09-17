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
import org.cangnova.cangjie.CjSourceKind

/**
 * 提供 `CangJieLightParser` 单例，集中承载仓颉语法解析的共享状态、工厂或工具行为。
 */
object CangJieLightParser {
    /**
     * 按完整仓颉文件语法构造 LightTree；宏片段可传入源模块，显式 package 会覆盖它。
     *
     * @param sourceKind 被解析代码所属源文件的种类；`.cj.d` 传 [CjSourceKind.DECLARATION]。
     *   默认 [CjSourceKind.SOURCE]，既有调用点行为不变。
     */
    fun parse(
        builder: PsiBuilder,
        errorListener: LightTreeParsingErrorListener? = null,
        languageModuleName: String = "",
        sourceKind: CjSourceKind = CjSourceKind.SOURCE,
    ): FlyweightCapableTreeStructure<LighterASTNode> =
        parseWith(builder, errorListener, languageModuleName, sourceKind) { parseFile() }

    /**
     * 按 annotation-only 语法构造 LightTree。
     *
     * languageModuleName 来自源 package，保证无 package header 的片段保留模块限制。
     * 该入口供 custom annotation 宏展开结果重解析使用，保证参数列表生成标准
     * `ANNOTATION -> VALUE_ARGUMENT_LIST -> VALUE_ARGUMENT` 结构，禁止退化为 macro-expression token 容器。
     *
     * @param sourceKind 该片段**来源文件**的种类。片段不是文件，种类由它的归属文件决定
     *   （宏展开产物属于展开它的那个文件），因此这里不按片段自身内容推断。
     */
    fun parseAnnotationOnly(
        builder: PsiBuilder,
        errorListener: LightTreeParsingErrorListener? = null,
        languageModuleName: String = "",
        sourceKind: CjSourceKind = CjSourceKind.SOURCE,
    ): FlyweightCapableTreeStructure<LighterASTNode> =
        parseWith(builder, errorListener, languageModuleName, sourceKind) { parseOnlyAnnotationFile() }

    /** 统一创建 parser、执行指定语法入口并完成 LightTree 错误上报。 */
    private inline fun parseWith(
        builder: PsiBuilder,
        errorListener: LightTreeParsingErrorListener?,
        languageModuleName: String,
        sourceKind: CjSourceKind,
        parseAction: CangJieParsing.() -> Unit,
    ): FlyweightCapableTreeStructure<LighterASTNode> {
        val cjParsing: CangJieParsing = CangJieParsing.createForTopLevelNonLazy(
            SemanticWhitespaceAwarePsiBuilderImpl(builder),
            languageModuleName,
            sourceKind,
        )
        cjParsing.parseAction()
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
