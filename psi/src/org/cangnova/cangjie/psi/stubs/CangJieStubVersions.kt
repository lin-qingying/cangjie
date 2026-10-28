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

package org.cangnova.cangjie.psi.stubs

/**
 * 提供 `CangJieStubVersions` 单例，集中承载PSI Stub的共享状态、工厂或工具行为。
 */
object CangJieStubVersions {

    /**
     * 214:
     * - 宏调用 `@Macro(...)` 的 Tokens 输入补齐 `<<`/`>>`、`b'0'` 等运算符与字面量 token，
     *   宏输入边界变化会改变 PSI 树与 stub 数量；与 213 是两次独立解析变更，必须再次重建索引
     *
     * 213:
     * - 二元表达式换行续接对齐官方 `ParserImpl::SeeingExprOperator`：行首运算符（如 `+`）继续上一行表达式，
     *   条件里换行后的 `let` 不再被当作语句边界。PSI 树形状发生变化，必须重建 stub 索引，
     *   否则旧索引（例如把续行 `let` 索引成 VARIABLE）与新 AST（ENUM_PATTERN）会触发 stub 绑定断言。
     */

    /**
     * 217:
     * - `.cjo` type stub 为 CPointer 保存结构化类型实参，并按包段/顶层声明段恢复限定名
     */

    /**
     * 212:
     * - extend stub 不再持久化 `ClassId`（extend 不属于公开类型标识体系，身份统一由 extendId 表达）
     *
     * 210:
     * - common/specific 修饰符 token 进入 MODIFIER_KEYWORDS_ARRAY（尾部追加，存根掩码新增 2 位）
     */
    /**
     * 209:
     * - 枚举声明宏的输入保留 ENUM_CONSTRUCTOR 和 payload 类型列表
     * - macro stub 持久化声明输入与括号 token 输入的边界
     *
     * 208:
     * - annotation stub 持久化 classId、builtin kind 和 `@!` provenance
     *
     * 206:
     * - features directive stub 持久化文件级 feature id，供 compiled/decompiled PSI 恢复
     *
     * 205:
     * - pattern variable stub 持久化 const 声明关键字状态
     *
     * 204:
     * - `.cjo` 顶层 callable stub 持久化 package facade origin
     * - property stub 持久化 origin，供 stub-based library 反序列化恢复 container source
     *
     * 203:
     * - package directive stub 持久化 macro package 标记
     *
     * 202:
     * - extend stub 显式持久化稳定 extendId
     * - decompiled compiled stub 与 source stub 统一使用同一套 extend identity 协议
     */
    const val SOURCE_STUB_VERSION = 214

    /**
     * 保存 `BINARY_STUB_VERSION`，供PSI Stub流程读取节点结构或语义信息。
     */
    private const val BINARY_STUB_VERSION = SOURCE_STUB_VERSION

    /**
     * 保存 `CLASSFILE_STUB_VERSION`，供PSI Stub流程读取节点结构或语义信息。
     */
    const val CLASSFILE_STUB_VERSION = BINARY_STUB_VERSION + 0

    /**
     * 218:
     * - `.cjo` 错误类型不再把 `<error>` 占位名物化为用户类型（改为 delegatedType 或 `std.core.Any`），
     *   保证反编译文本可被重新解析，stub 树与 AST 的 stubbed spine 节点数一致
     */
    const val BUILTIN_STUB_VERSION = BINARY_STUB_VERSION + 6


}
