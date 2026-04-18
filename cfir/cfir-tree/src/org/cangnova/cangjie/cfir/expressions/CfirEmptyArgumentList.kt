package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.source.CjSourceElement

object CfirEmptyArgumentList :CfirAbstractArgumentList() {
    override val arguments: List<CfirExpression>
        get() = emptyList()

    override val source: CjSourceElement?
        get() = null
}
