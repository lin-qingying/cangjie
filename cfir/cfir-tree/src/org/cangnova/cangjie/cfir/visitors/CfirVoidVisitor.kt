package org.cangjie.cfir.visitors

import org.cangjie.cfir.CfirElement

abstract class CfirVoidVisitor : CfirVisitor<Unit, Nothing?>() {
    fun visitAll(elements: List<CfirElement>) {
        elements.forEach { it.accept(this, null) }
    }
}