/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirClassSpecificMembersResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.resolve
import org.cangnova.cangjie.cfir.correspondingProperty
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.impl.CfirPrimaryConstructor
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase

internal object FileElementFactory {
    fun createFileStructureElement(
        cfirDeclaration: CfirDeclaration,
        cfirFile: CfirFile,
        moduleComponents: LLCfirModuleResolveComponents,
    ): FileStructureElement = when (cfirDeclaration) {
        is CfirClass -> {
            cfirDeclaration.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE.previous)

            lazyResolveClassGeneratedMembers(cfirDeclaration)
            ClassDeclarationStructureElement(cfirFile, cfirDeclaration, moduleComponents)
        }

        is CfirExtend -> {
            cfirDeclaration.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
            cfirDeclaration.declarations.forEach { declaration ->
                declaration.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
            }
            ExtendDeclarationStructureElement(cfirFile, cfirDeclaration, moduleComponents)
        }

        else -> {
            if (cfirDeclaration is CfirPrimaryConstructor) {
                cfirDeclaration.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
                cfirDeclaration.valueParameters.forEach { parameter ->
                    parameter.correspondingProperty?.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
                }
            } else {
                /** Reserve the [CfirResolvePhase.BODY_RESOLVE] for partial body analysis. */
                cfirDeclaration.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE.previous)
            }

            DeclarationStructureElement(cfirFile, cfirDeclaration, moduleComponents)
        }
    }

    private fun lazyResolveClassGeneratedMembers(cfirClass: CfirClass) {
        val classMembersToResolve = cfirClass.declarations.filter(CfirDeclaration::isPartOfClassStructureElement)

        if (classMembersToResolve.isEmpty()) return
        val cfirClassDesignation = cfirClass.collectDesignation()
        val designationWithMembers = LLCfirClassSpecificMembersResolveTarget(
            cfirClassDesignation,
            classMembersToResolve,
        )

        designationWithMembers.resolve(CfirResolvePhase.BODY_RESOLVE)
    }
}
