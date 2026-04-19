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
        firDeclaration: CfirDeclaration,
        firFile: CfirFile,
        moduleComponents: LLCfirModuleResolveComponents,
    ): FileStructureElement = when (firDeclaration) {
        is CfirClass -> {
            firDeclaration.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE.previous)

            lazyResolveClassGeneratedMembers(firDeclaration)
            ClassDeclarationStructureElement(firFile, firDeclaration, moduleComponents)
        }

        else -> {
            if (firDeclaration is CfirPrimaryConstructor) {
                firDeclaration.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
                firDeclaration.valueParameters.forEach { parameter ->
                    parameter.correspondingProperty?.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
                }
            } else {
                /** Reserve the [CfirResolvePhase.BODY_RESOLVE] for partial body analysis. */
                firDeclaration.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE.previous)
            }

            DeclarationStructureElement(firFile, firDeclaration, moduleComponents)
        }
    }

    private fun lazyResolveClassGeneratedMembers(firClass: CfirClass) {
        val classMembersToResolve = firClass.declarations.filter(CfirDeclaration::isPartOfClassStructureElement)

        if (classMembersToResolve.isEmpty()) return
        val firClassDesignation = firClass.collectDesignation()
        val designationWithMembers = LLCfirClassSpecificMembersResolveTarget(
            firClassDesignation,
            classMembersToResolve,
        )

        designationWithMembers.resolve(CfirResolvePhase.BODY_RESOLVE)
    }
}
