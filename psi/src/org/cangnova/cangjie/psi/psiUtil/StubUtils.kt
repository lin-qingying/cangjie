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

package org.cangnova.cangjie.psi.psiUtil

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.stubs.CangJieFileStub
import org.cangnova.cangjie.psi.stubs.CangJiePlaceHolderStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes

/**
 * Stub 序列化工具。
 *
 * 这里统一封装两类基础能力：
 * 1. `ClassId` 的序列化与反序列化。
 * 2. 仅为允许进入公开索引的顶层 class-like 声明构造稳定 `ClassId`。
 *
 * 仓颉当前不建模类型声明的层级语义，`ClassId` 只服务于顶层 class-like 声明。
 * 因此类体内声明、局部声明、匿名声明都不会在这里获得稳定 `ClassId`。
 */
object StubUtils {
    /**
     * 从 Stub 输入流中恢复 `ClassId`。
     */
    @JvmStatic
    fun deserializeClassId(dataStream: StubInputStream): ClassId? {
        val classId = dataStream.readName() ?: return null
        return ClassId.fromString(classId.string)
    }

    /**
     * 将 `ClassId` 写入 Stub 输出流。
     */
    @JvmStatic
    fun serializeClassId(dataStream: StubOutputStream, classId: ClassId?) {
        dataStream.writeName(classId?.asString())
    }

    /**
     * 为顶层 class-like 声明创建稳定的 Stub `ClassId`。
     *
     * 这里遵循仓颉的公开索引语义：
     * - 只有文件级顶层 class-like 声明拥有稳定 `ClassId`
     * - 其他声明位置一律返回 `null`
     */
    @JvmStatic
    fun createClassId(parentStub: StubElement<*>, currentDeclaration: CjClassLikeDeclaration): ClassId? = when {
        // 文件级顶层 class-like 声明。
        parentStub is CangJieFileStub -> ClassId(parentStub.getPackageFqName(), currentDeclaration.nameAsSafeName)

        // 类体中的声明不进入顶层类索引。
        parentStub is CangJiePlaceHolderStub<*> && parentStub.stubType == CjStubElementTypes.CLASS_BODY -> null

        // 其余位置同样不产生稳定 ClassId。
        else -> null
    }
}
