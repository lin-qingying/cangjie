/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.forEachDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass

/**
 * [LLCfirResolveTarget] representing a class with all callable members (functions and properties).
 */
internal class LLCfirClassWithAllCallablesResolveTarget(designation: CfirDesignation) : LLCfirClassResolveTarget(designation) {
    override val visitClass: Boolean get() = true
    override fun visitMembers(visitor: LLCfirResolveTargetVisitor, firClass: CfirClass) {
        firClass.forEachDeclaration {
            if (it is CfirCallableDeclaration) {
                visitor.performAction(it)
            }
        }
    }
}
