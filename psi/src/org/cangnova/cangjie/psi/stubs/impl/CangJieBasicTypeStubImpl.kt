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

package org.cangnova.cangjie.psi.stubs.impl

import org.cangnova.cangjie.psi.CjBasicType
import org.cangnova.cangjie.psi.stubs.CangJieBasicTypeStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement

/**
 * 表示 `CangJieBasicTypeStubImpl`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
class CangJieBasicTypeStubImpl(
    parent: StubElement<out PsiElement>?,
    /**
     * 暴露 `basicType`，实现PSI Stub节点对上层接口的属性契约。
     */
    override val basicType: String
) : CangJieStubBaseImpl<CjBasicType>(parent, CjStubElementTypes.BASIC_TYPE),
    CangJieBasicTypeStub {
    /**
     * 实现 `copyInto` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun copyInto(newParent: StubElement<*>?): CangJieBasicTypeStubImpl = CangJieBasicTypeStubImpl(
        parent = newParent,
        basicType = basicType,
    )
}
