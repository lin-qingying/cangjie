package org.cangnova.cangjie.cfir.render

import org.cangnova.cangjie.cfir.types.ConeCangJieType

class ConeTypeRendererForDebugging() : ConeTypeRenderer() {

    constructor(builder: StringBuilder) : this() {
        this.builder = builder
        this.idRenderer = ConeIdRendererForDebugging()
        idRenderer.builder = builder
    }

    override fun ConeCangJieType.renderAttributes() {
        renderNonCompilerAttributes()
    }
}
