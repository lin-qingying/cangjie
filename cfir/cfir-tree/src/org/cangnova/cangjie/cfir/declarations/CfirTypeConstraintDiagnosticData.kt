package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

data class CfirTypeConstraintDiagnosticData(
    val typeConstraints: List<CfirTypeConstraintReference>,
) {
    companion object {
        val EMPTY = CfirTypeConstraintDiagnosticData(
            typeConstraints = emptyList(),
        )
    }
}

data class CfirTypeConstraintReference(
    val parameterName: Name,
    val source: CjSourceElement,
)

private object TypeConstraintDiagnosticDataKey : CfirDeclarationDataKey()

var CfirDeclarationAttributes.typeConstraintDiagnosticData: CfirTypeConstraintDiagnosticData? by CfirDeclarationDataRegistry.attributesAccessor(
    TypeConstraintDiagnosticDataKey
)
