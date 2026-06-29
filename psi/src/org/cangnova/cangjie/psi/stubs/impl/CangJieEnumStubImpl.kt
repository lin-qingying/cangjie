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
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.stubs.CangJieEnumStub
import org.cangnova.cangjie.psi.stubs.elements.CjEnumElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import java.util.ArrayList

/**
 * 表示 `CangJieEnumStubImpl`，承载PSI Stub中的语法节点、索引桩或辅助模型。
 */
open class CangJieEnumStubImpl(
    type: CjEnumElementType,
    parent: StubElement<out PsiElement>?,
    /**
     * 保存 `qualifiedName` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val qualifiedName: StringRef?,
    /**
     * 保存 `classId` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val classId: ClassId?,
    /**
     * 保存 `name` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val name: StringRef?,
    /**
     * 保存 `superNames` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val superNames: Array<StringRef>,
    /**
     * 保存 `isNonExhaustive` 的内部状态，供PSI Stub实现维护节点缓存或解析上下文。
     */
    private val isNonExhaustive: Boolean
) : CangJieStubBaseImpl<CjEnum>(parent, type), CangJieEnumStub {

    /**
     * 实现 `getFqName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getFqName(): FqName? {
        val stringRef = StringRef.toString(qualifiedName) ?: return null
        return FqName(stringRef)
    }

    /**
     * 实现 `getName` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName() = StringRef.toString(name)
    /**
     * 实现 `isNonExhaustive` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isNonExhaustive(): Boolean {
        return isNonExhaustive
    }
    /**
     * 实现 `getSuperNames` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getSuperNames(): List<String> {
        val result = ArrayList<String>()
        for (ref in superNames) {
            result.add(ref.toString())
        }
        return result
    }

    /**
     * 实现 `getClassId` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getClassId(): ClassId? = classId

    /**
     * 实现 `copyInto` 的PSI Stub协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun copyInto(newParent: StubElement<*>?): CangJieEnumStubImpl = CangJieEnumStubImpl(
        type = stubType as CjEnumElementType,
        parent = newParent,
        qualifiedName = qualifiedName,
        classId = classId,
        name = name,
        superNames = superNames,
        isNonExhaustive = isNonExhaustive,
    )
}
