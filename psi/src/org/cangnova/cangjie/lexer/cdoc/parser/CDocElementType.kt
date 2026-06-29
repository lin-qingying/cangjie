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

package org.cangnova.cangjie.lexer.cdoc.parser

import org.cangnova.cangjie.lang.CangJieLanguage
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import java.lang.reflect.Constructor

/**
 * 表示 `CDocElementType`，承载仓颉词法与文档注释中的语法节点、索引桩或辅助模型。
 */
class CDocElementType(debugName: String, psiClass: Class<out PsiElement?>) :
    IElementType(debugName, CangJieLanguage) {
    /**
     * 保存 `psiFactory` 的内部状态，供仓颉词法与文档注释实现维护节点缓存或解析上下文。
     */
    private var psiFactory: Constructor<out PsiElement?>? = null

    init {
        psiFactory = try {
            psiClass.getConstructor(ASTNode::class.java)
        } catch (e: NoSuchMethodException) {
            throw RuntimeException("Must have a constructor with ASTNode")
        }
    }

    /**
     * 提供 `createPsi` 操作，封装仓颉词法与文档注释节点的访问、构造或判断逻辑。
     */
    fun createPsi(node: ASTNode): PsiElement {
        assert(node.elementType === this)
        return try {
            psiFactory?.newInstance(node)!!
        } catch (e: Exception) {
            throw RuntimeException("Error creating psi element for node", e)
        }
    }
}
