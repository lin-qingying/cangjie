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
 * 仓颉版约束位置类型，对齐 K2 FirConstraintPositionAndErrors。
 *
 * 将 resolution.common 中的通用约束位置抽象类具化为 CFIR 层使用的具体类型。
 */

class ConeFixVariableConstraintPosition(
    variable: TypeVariableMarker,
) : FixVariableConstraintPosition<Nothing?>(variable, null)

class ConeArgumentConstraintPosition(
    argument: CfirElement,
) : RegularArgumentConstraintPosition<CfirElement>(argument)

object ConeExpectedTypeConstraintPosition : ExpectedTypeConstraintPosition<Nothing?>(null)

object ConeDeclaredUpperBoundConstraintPosition : DeclaredUpperBoundConstraintPosition<Nothing?>(null)

class ConeExplicitTypeParameterConstraintPosition(
    typeArgument: CfirTypeRef,
) : ExplicitTypeParameterConstraintPosition<CfirTypeRef>(typeArgument)

class ConeReceiverConstraintPosition(
    receiver: CfirExpression,
    @Suppress("UNUSED_PARAMETER") source: CjSourceElement? = null,
) : ReceiverConstraintPosition<CfirExpression>(receiver)
