package org.cangnova.cangjie.cfir.render

import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection

class ConeTypeRendererForDebugging() : ConeTypeRenderer() {

    constructor(builder: StringBuilder) : this() {
        this.builder = builder
        this.idRenderer = ConeIdRendererForDebugging()
        idRenderer.builder = builder
    }


}
