/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.forEachDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.isDeclarationContainer
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile

/**
 * [LLCfirResolveTarget] representing all declarations in [target] recursively.
 * All of them are going to be resolved.
 */
internal class LLCfirWholeElementResolveTarget(designation: CfirDesignation) : LLCfirResolveTarget(designation) {
    override fun visitTargetElement(
        element: CfirElementWithResolveState,
        visitor: LLCfirResolveTargetVisitor,
    ) {
        if (element !is CfirFile) {
            visitor.performAction(element)
        }

        when {
            element !is CfirDeclaration || !element.isDeclarationContainer -> {}

            element is CfirClass -> visitor.withClass(element) {
                element.forEachDeclaration {
                    visitTargetElement(it, visitor)
                }
            }

            element is CfirExtend -> visitor.withExtend(element) {
                element.forEachDeclaration {
                    visitTargetElement(it, visitor)
                }
            }

            element is CfirFile -> visitor.withFile(element) {
                element.forEachDeclaration {
                    visitTargetElement(it, visitor)
                }
            }

            else -> errorWithCfirSpecificEntries("Unexpected declaration: ${element::class.simpleName}", fir = element)
        }
    }

    override fun toStringAdditionalSuffix(): String = "*"
}
