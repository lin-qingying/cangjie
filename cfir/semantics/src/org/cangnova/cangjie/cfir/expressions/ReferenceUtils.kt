package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * Returns the [FirReference] of this [FirElement], if available.
 * The reference is resolved in the context of a use-site [session], which may be required to find a symbol for an ID-based expression.
 */
fun CfirElement.toReference(session: CfirSession): CfirReference? {
    return when (this) {
        is CfirExpression -> toReferenceImpl(session)
//        is CfirVariableAssignment -> calleeReference
        is CfirResolvable -> calleeReference
        else -> null
    }
}

private fun CfirExpression.toReferenceImpl(session: CfirSession?): CfirReference? {
    return when (this) {
//        is CfirEnumEntryDeserializedAccessExpression -> {
//            requireNotNull(session)
//            toReference(session)
//        }
//        is CfirWrappedArgumentExpression -> expression.toResolvedCallableReferenceImpl(session)
        is CfirSmartCastExpression -> originalExpression.toReferenceImpl(session)
//        is CfirDesugaredAssignmentValueReferenceExpression -> expressionRef.value.toReferenceImpl(session)
        is CfirResolvable -> calleeReference
        else -> null
    }
}