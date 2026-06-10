package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.type.CfirTypeProjectionModifierChecker
import org.cangnova.cangjie.cfir.analysis.checkers.type.CfirUpperBoundViolatedTypeChecker
import org.cangnova.cangjie.cfir.analysis.checkers.type.TypeCheckers

object CommonTypeCheckers : TypeCheckers() {
    override val typeRefCheckers
        get() = setOf(CfirTypeProjectionModifierChecker)

    override val resolvedTypeRefCheckers
        get() = setOf(CfirUpperBoundViolatedTypeChecker)
}
