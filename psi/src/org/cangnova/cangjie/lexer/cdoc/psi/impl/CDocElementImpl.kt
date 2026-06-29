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

package org.cangnova.cangjie.lexer.cdoc.psi.impl

import org.cangnova.cangjie.lexer.cdoc.psi.CDocElement
import org.cangnova.cangjie.lang.CangJieLanguage
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.lang.Language
import org.jetbrains.annotations.NotNull

/**
 * 表示 `CDocElementImpl`，承载仓颉词法与文档注释中的语法节点、索引桩或辅助模型。
 */
abstract class CDocElementImpl(node: ASTNode) : ASTWrapperPsiElement(node), CDocElement {

    /**
     * 实现 `getLanguage` 的仓颉词法与文档注释协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    @NotNull
    override fun getLanguage(): Language = CangJieLanguage

    /**
     * 实现 `toString` 的仓颉词法与文档注释协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String = node.elementType.toString()
}
