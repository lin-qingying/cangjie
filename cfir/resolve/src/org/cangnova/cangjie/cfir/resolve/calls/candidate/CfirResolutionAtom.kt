package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.semantics.AbstractConeResolutionAtom

class CfirResolutionAtom(
    override val expression: CfirExpression,
) : AbstractConeResolutionAtom()

