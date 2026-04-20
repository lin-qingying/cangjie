package org.cangnova.cangjie.cfir.resolve.transformers.body.resolve

import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.builtinTypes
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.resolvedType

internal inline var CfirExpression.resultType: ConeCangJieType
    get() = resolvedType
    set(type) {
        replaceConeTypeOrNull(type)
    }

/**
 * 对位 Kotlin `FirBlock.writeResultType`。
 */
fun CfirBlock.writeResultType(session: CfirSession) {
    val resultExpression = statements.lastOrNull() as? CfirExpression
    resultType = if (resultExpression == null) {
        session.builtinTypes.unitType
    } else {
        resultExpression.coneTypeOrNull ?: ConeErrorType(ConeSimpleDiagnostic("Postponed inference"))
    }
}
