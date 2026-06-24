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

package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConePlaceholderType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection

/**
 * 显式类型实参到声明类型参数序号的映射。
 *
 * 候选构造阶段通过该结构统一读取用户显式提供的类型实参；
 * 未提供或越界的位置会返回占位投影，让后续约束系统继续推断。
 */
sealed class TypeArgumentMapping {
    /**
     * 返回指定类型参数序号对应的类型投影。
     */
    abstract operator fun get(typeParameterIndex: Int): ConeTypeProjection

    /**
     * 返回类型实参在源码中的已解析类型引用。
     */
    open fun sourceTypeRef(typeParameterIndex: Int): CfirResolvedTypeRef? = null

    /**
     * 表示当前调用没有显式类型实参。
     */
    data object NoExplicitArguments : TypeArgumentMapping() {
        /**
         * 无显式类型实参时始终返回占位投影。
         */
        override fun get(typeParameterIndex: Int): ConeTypeProjection = buildPlaceholderProjection()
    }

    /**
     * 按声明类型参数顺序保存的显式类型实参映射。
     */
    class Mapped(
        /**
         * 源码中显式提供并已解析的类型实参列表。
         */
        private val ordered: List<CfirResolvedTypeRef>,
    ) : TypeArgumentMapping() {
        /**
         * 返回指定序号的显式类型实参；缺失时返回占位投影继续推断。
         */
        override fun get(typeParameterIndex: Int): ConeTypeProjection {
            return ordered.getOrNull(typeParameterIndex)?.coneType ?: buildPlaceholderProjection()
        }

        /**
         * 返回指定序号的源码类型引用，缺失时为空。
         */
        override fun sourceTypeRef(typeParameterIndex: Int): CfirResolvedTypeRef? =
            ordered.getOrNull(typeParameterIndex)
    }
}

/**
 * 创建一个用于触发类型推断的占位类型投影。
 */
private fun buildPlaceholderProjection(): ConeTypeProjection {
    return ConePlaceholderType()
}
