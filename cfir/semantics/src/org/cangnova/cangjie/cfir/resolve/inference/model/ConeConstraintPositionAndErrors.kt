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

package org.cangnova.cangjie.cfir.resolve.inference.model

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.model.TypeVariableMarker

/**
 * 固定类型变量时使用的约束位置。
 *
 * @param variable 需要固定的类型变量。
 */
class ConeFixVariableConstraintPosition(
    variable: TypeVariableMarker,
) : FixVariableConstraintPosition<Nothing?>(variable, null)

/**
 * 普通调用实参产生的约束位置。
 *
 * @param argument 触发类型约束的 CFIR 元素。
 */
class ConeArgumentConstraintPosition(
    argument: CfirElement,
) : RegularArgumentConstraintPosition<CfirElement>(argument)

/**
 * 期望类型产生的约束位置。
 */
object ConeExpectedTypeConstraintPosition : ExpectedTypeConstraintPosition<Nothing?>(null)

/**
 * 声明侧上界产生的约束位置。
 */
object ConeDeclaredUpperBoundConstraintPosition : DeclaredUpperBoundConstraintPosition<Nothing?>(null)

/**
 * 显式类型实参产生的约束位置。
 *
 * @param typeArgument 源码中写出的类型实参引用。
 */
class ConeExplicitTypeParameterConstraintPosition(
    typeArgument: CfirTypeRef,
) : ExplicitTypeParameterConstraintPosition<CfirTypeRef>(typeArgument)

/**
 * 接收者类型约束位置。
 *
 * @param receiver 触发约束的接收者表达式。
 * @param source 保留的源码位置参数，当前实现仅用于保持与公共约束模型签名对齐。
 */
class ConeReceiverConstraintPosition(
    receiver: CfirExpression,
    @Suppress("UNUSED_PARAMETER") source: CjSourceElement? = null,
) : ReceiverConstraintPosition<CfirExpression>(receiver)
