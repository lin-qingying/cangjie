/*
 * Copyright 2010-2026. cangjie.
 */

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.withFileAnalysisExceptionWrapping

abstract class CfirAbstractTreeTransformer<D>(phase: CfirResolvePhase) : CfirAbstractPhaseTransformer<D>(phase) {
    override fun <E : CfirElement> transformElement(element: E, data: D): E {
        @Suppress("UNCHECKED_CAST")
        return (element.transformChildren(this, data) as E)
    }
    override fun transformFile(file: CfirFile, data: D): CfirFile {
        checkSessionConsistency(file)
        return withFileAnalysisExceptionWrapping(file) {
            super.transformFile(file, data)
        }
    }
}

