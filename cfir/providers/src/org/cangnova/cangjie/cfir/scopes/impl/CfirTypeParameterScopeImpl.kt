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

package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.CfirTypeParameterScope
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.name.Name

/**
 * 类型参数 scope。
 *
 * 在泛型类或泛型函数的作用域中注入类型参数名称。
 * 例如 `class Foo<T>` 内部，`T` 通过此 scope 解析为对应的 [CfirTypeParameterSymbol]。
 *
 * 注意：类型参数不是 classifiers/functions/variables，但为了简化 scope 查找，
 * 通过 processClassifiersByName 查找时如果存在同名的类型参数，也不会冲突，
 * 因为类型参数在仓颉中通过单独的名称解析路径处理。
 * 这里提供独立的 [processTypeParametersByName] 方法用于类型参数查找。
 */
class CfirTypeParameterScopeImpl(
    typeParameters: List<CfirTypeParameter>,
) : CfirTypeParameterScope() {

    /**
     * 类型参数名称到 symbol 列表的索引。
     */
    private val typeParametersByName: Map<Name, List<CfirTypeParameterSymbol>>

    init {
        val map = HashMap<Name, MutableList<CfirTypeParameterSymbol>>()
        for (tp in typeParameters) {
            val sym = tp.symbol as? CfirTypeParameterSymbol ?: continue
            map.getOrPut(tp.name) { mutableListOf() }.add(sym)
        }
        typeParametersByName = map
    }

    /**
     * 按名称查找类型参数符号。
     */
    override fun processTypeParametersByName(name: Name, processor: (CfirTypeParameterSymbol) -> Unit) {
        typeParametersByName[name]?.forEach(processor)
    }

    /**
     * 对齐 Kotlin `FirTypeParameterScope`：类型参数通过 classifier 主入口进入 tower。
     * 仓颉不把类型参数当作 class-like，因此旧的 [processClassifiersByName] 仍保持空实现。
     */
    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (CfirClassifierSymbol<*>, ConeSubstitutor) -> Unit,
    ) {
        typeParametersByName[name]?.forEach { symbol ->
            processor(symbol, ConeSubstitutor.Empty)
        }
    }

    /**
     * 类型参数 scope 不包含 callable。
     */
    override fun getCallableNames(): Set<Name> = emptySet()

    /**
     * 返回类型参数名称集合。
     */
    override fun getClassifierNames(): Set<Name> = typeParametersByName.keys

    /**
     * 判断 scope 是否可能包含指定类型参数名。
     */
    override fun mayContainName(name: Name): Boolean = name in typeParametersByName

    // 类型参数不是 class-like/functions/variables。
    /**
     * 类型参数不是 class-like classifier，保持空实现。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {}

    /**
     * 类型参数 scope 不包含函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {}

    /**
     * 类型参数 scope 不包含属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {}
}
