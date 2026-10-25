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

package org.cangnova.cangjie.parsing

/**
 * 官方 `ScopeKind`（`external/cangjie_compiler/include/cangjie/Parse/Parser.h`）的宿主子集。
 *
 * 只有能承载成员声明的值参与缺体判定；`FUNC_BODY` / `MAIN_BODY` / `PROP_*_BODY` 等
 * 官方值在本仓 parser 路径上不承载声明，不予建模。
 *
 * 该枚举是缺体判定与 CFIR 隐式 abstract 推导之间**唯一**的作用域载体，
 * 替代原先 `topTokenId: Int?` 的魔法值编码（`0` = 顶层、`null` = 未知）。
 */
enum class CjScopeKind {
    /** 顶层作用域（官方 `ScopeKind::TOPLEVEL`）。 */
    TOPLEVEL,

    /** `class` 体（官方 `ScopeKind::CLASS_BODY`）。 */
    CLASS_BODY,

    /** `interface` 体（官方 `ScopeKind::INTERFACE_BODY`）。 */
    INTERFACE_BODY,

    /** `struct` 体（官方 `ScopeKind::STRUCT_BODY`）。 */
    STRUCT_BODY,

    /** `extend` 体（官方 `ScopeKind::EXTEND_BODY`）。 */
    EXTEND_BODY,

    /** `enum` 体（官方 `ScopeKind::ENUM_BODY`）。 */
    ENUM_BODY,

    /**
     * 宿主未知（官方 `ScopeKind::UNKNOWN_SCOPE`）。
     *
     * 典型来源是宏输入：宏调用可能嵌套在括号里，parser 无法判定其最终作用域。
     * 官方 1.0.x 对该作用域的函数缺体保持沉默、对属性缺体按隐式 abstract 处理 —— 两者不对称。
     * （1.1.3 `CanBeAbstract` 对两者都不再标记；本仓 parser 路径只消费诊断，CFIR 在
     * MACRO_EXPAND 后以已知作用域重建。）
     */
    UNKNOWN_SCOPE,
}
