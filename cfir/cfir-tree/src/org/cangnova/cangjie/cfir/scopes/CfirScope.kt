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

package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.name.Name

/**
 * 名称解析 scope 接口。
 *
 * scope 用于按名称查找符号，是名称解析的核心抽象。
 * 参考 K2 FirScope。
 */
abstract class CfirScope {
    /**
     * 按名称处理 classifier 符号，并同时暴露该符号对应的类型替换器。
     */
    open fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (CfirClassifierSymbol<*>, ConeSubstitutor) -> Unit
    ) {
        processClassifiersByName(name) { classifier ->
            processor(classifier, ConeSubstitutor.Empty)
        }
    }

    /**
     * 当前 scope owner 的查找名集合，用于调试、缓存 key 或 scope 链追踪。
     */
    open val scopeOwnerLookupNames: List<String> get() = emptyList()

    /**
     * 按名称处理类、接口、结构体、枚举等 class-like 符号。
     */
    open fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {}

    /**
     * 处理当前 scope 直接声明的构造器符号。
     */
    open fun processDeclaredConstructors(
        processor: (CfirConstructorSymbol) -> Unit
    ) {
    }

    /**
     * 按名称处理函数符号。
     */
    open fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {}

    /**
     * 按名称处理变量类符号。
     */
    open fun processVariablesByName(
        name: Name,
        processor: (CfirVariableSymbol<*>) -> Unit
    ) {
    }

    /**
     * 当前 scope 是否可能包含指定名称。
     *
     * 返回 `false` 可让调用方跳过更昂贵的按名查找。
     */
    open fun mayContainName(name: Name): Boolean = true

    /**
     * 按名称处理属性符号。
     */
    open fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {}

    /**
     * 在新 session / scope session 下替换当前 scope。
     *
     * 返回 `null` 表示该 scope 不能安全迁移到新的 session 上。
     */
    abstract fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): CfirScope?

    /**
     * 按名称处理所有 callable 符号，包括函数、属性和其他 callable 声明。
     */
    open fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {}
}
