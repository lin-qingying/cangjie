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

package org.cangnova.cangjie.cfir.types

/**
 * 仓颉中的 `Array<T>` 是标准库名义类型，在 Cone 层直接用 [ConeClassLikeType] 表示即可。
 * 这里只有需要编译器显式建模的 `VArray<T, N>`，因为它携带编译期固定长度语义。
 */
class ConeVArrayType(
    val elementType: ConeCangJieType,
    /** 数组大小，必须是编译期常量。 */
    val size: Long,
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeRigidType(), ConeTypeConstructorMarker {
    /**
     * CHIR `VArrayType::GetSize()` 将 Sema 层 `Int64` 长度转换成 C++ `unsigned int`。
     * CFIR 需要同时保留 Sema 长度和 CHIR 有效长度，才能区分 `sema_builtin_index_in_bound`
     * 与 `chir_idx_out_of_bounds` 两个官方诊断层。
     */
    val chirEffectiveSize: Long
        get() = size and 0xFFFF_FFFFL

    /**
     * VArray 在官方类型系统中以元素类型作为唯一 typeArg，size 属于类型头部。
     * 这里暴露给通用约束系统，保证 `VArray<α, N>` 能拆解出元素类型约束。
     */
    override val typeArguments: List<ConeTypeProjection> = listOf(elementType)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConeVArrayType) return false
        return elementType == other.elementType && size == other.size
    }

    override fun hashCode(): Int {
        var result = elementType.hashCode()
        result = 31 * result + size.hashCode()
        return result
    }

}
