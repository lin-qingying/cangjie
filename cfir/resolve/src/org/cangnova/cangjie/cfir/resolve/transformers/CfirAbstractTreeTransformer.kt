/*
 * Copyright 2010-2026. cangjie.
 */

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase

abstract class CfirAbstractTreeTransformer<D>(phase: CfirResolvePhase) : CfirAbstractPhaseTransformer<D>(phase) {
    override fun <E : CfirElement> transformElement(element: E, data: D): E {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E)
    }
}

