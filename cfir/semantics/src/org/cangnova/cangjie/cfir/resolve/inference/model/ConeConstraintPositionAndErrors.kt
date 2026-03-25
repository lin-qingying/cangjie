package org.cangnova.cangjie.cfir.resolve.inference.model

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.resolve.calls.inference.model.DeclaredUpperBoundConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.ExpectedTypeConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.ExplicitTypeParameterConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.FixVariableConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.ReceiverConstraintPosition
import org.cangnova.cangjie.resolve.calls.inference.model.RegularArgumentConstraintPosition
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
    typeArgument: ConeTypeProjection,
) : ExplicitTypeParameterConstraintPosition<ConeTypeProjection>(typeArgument)

class ConeReceiverConstraintPosition(
    receiver: CfirExpression,
    @Suppress("UNUSED_PARAMETER") source: CjSourceElement? = null,
) : ReceiverConstraintPosition<CfirExpression>(receiver)
