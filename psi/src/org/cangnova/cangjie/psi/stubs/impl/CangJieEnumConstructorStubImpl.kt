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
import org.cangnova.cangjie.name.*

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.stubs.CangJieEnumConstructorStub
import org.cangnova.cangjie.psi.stubs.elements.CjEnumConstructorElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef

/**
 * 枚举构造器的 Stub 实现
 *
 * 只存储名称和参数类型数量，参数类型详情由 PSI 子树（TYPE_LIST）提供。
 * 存储所属枚举的 FqName 用于索引。
 */
 class CangJieEnumConstructorStubImpl(
    type: CjEnumConstructorElementType,
    parent: StubElement<out PsiElement>?,
    /**
     * 保存 `name` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val name: StringRef?,       // 枚举构造器名称 (例如: Red, Green)
    /**
     * 保存 `typeCount` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val typeCount: Int,         // 参数类型数量
    /**
     * 保存 `enumFqName` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val enumFqName: StringRef?, // 所属枚举的完全限定名
) : CangJieStubBaseImpl<CjEnumConstructor>(parent, type), CangJieEnumConstructorStub {

    /**
     * 实现 `getName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName() = StringRef.toString(name)

    /**
     * 实现 `getTypeCount` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getTypeCount(): Int = typeCount

    /**
     * 实现 `getEnumFqName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getEnumFqName(): FqName? {
        val fqNameStr = StringRef.toString(enumFqName) ?: return null
        return FqName(fqNameStr)
    }

    /**
     * 实现 `copyInto` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun copyInto(newParent: StubElement<*>?): CangJieEnumConstructorStubImpl = CangJieEnumConstructorStubImpl(
        type = stubType as CjEnumConstructorElementType,
        parent = newParent,
        name = name,
        typeCount = typeCount,
        enumFqName = enumFqName,
    )
}
