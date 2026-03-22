/*
 * Copyright 2010-2026. cangjie.
 */

package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.visitors.CfirDefaultTransformer

abstract class CfirAbstractPhaseTransformer<D>(
    val baseTransformerPhase: CfirResolvePhase,
) : CfirDefaultTransformer<D>(), SessionHolder {
    abstract override val session: CfirSession

    init {
        require(baseTransformerPhase != CfirResolvePhase.RAW_CFIR) {
            "Raw CFIR building should not be done in phase transformer"
        }
    }

    open val transformerPhase: CfirResolvePhase
        get() = baseTransformerPhase

    override fun transformFile(file: CfirFile, data: D): CfirFile {
        checkSessionConsistency(file)
        return super.transformFile(file, data) as CfirFile
    }

    protected fun checkSessionConsistency(file: CfirFile) {
        require(session.moduleData == file.moduleData) {
            "File ${file.name} and transformer ${this::class} have inconsistent sessions"
        }
    }
}

