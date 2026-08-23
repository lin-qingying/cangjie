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

package org.cangnova.cangjie.cfir.diagnostics

/**
 * 诊断分类，对齐 K2 `DiagnosticKind`。
 */
enum class DiagnosticKind {
    /**
     * 非法 const 表达式。
     */
    IllegalConstExpression,

    /**
     * 反序列化过程中产生的错误。
     */
    DeserializationError,

    /**
     * 类型推断错误。
     */
    InferenceError,

    /**
     * 隐式类型求解中的递归错误。
     */
    RecursionInImplicitTypes,

    /**
     * 父类型图中发现循环。
     */
    LoopInSupertype,

    /**
     * 父类型列表直接引用声明自身。
     */
    SupertypeSelfReference,

    /**
     * typealias 展开图中发现递归引用。
     */
    RecursiveTypealiasExpansion,

    /**
     * 重复父类型错误。
     */
    DuplicateSupertype,

    /**
     * 当前位置不允许 return。
     */
    ReturnNotAllowed,

    /**
     * 跳转语句跳出允许范围。
     */
    JumpOutsideLoop,

    /**
     * 标签目标不是循环。
     */
    NotLoopLabel,

    /**
     * 父类型无法解析。
     */
    UnresolvedSupertype,

    /**
     * 无法推断参数类型。
     */
    CannotInferParameterType,

    /**
     * enum initializer 相关错误。
     */
    EnumInitializerError,

    /**
     * 标签解析出现歧义。
     */
    AmbiguousLabel,

    /**
     * 标签无法解析。
     */
    UnresolvedLabel,

    /**
     * 标签名称冲突。
     */
    LabelNameClash,

    /**
     * 当前位置不允许 super。
     */
    SuperNotAllowed,

    /**
     * 泛型类型缺失类型实参。
     */
    GenericTypeWithoutTypeArgument,

    /**
     * static initializer 中出现 return。
     */
    ReturnInStaticInit,

    /**
     * 初始化前捕获了尚不可用的值。
     */
    CaptureBeforeInitialization,

    /**
     * this 类型出现的位置非法。
     */
    InvalidThisTypePosition,

    /**
     * 空数组字面量缺少可确定元素类型。
     */
    EmptyArrayLiteralTypeUndefined,

    /**
     * 未归入专门分类的其他诊断。
     */
    Other,
}
