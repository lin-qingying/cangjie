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
        is CfirClassLikeDeclaration -> {
            cfirDeclaration.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE.previous)

            lazyResolveClassGeneratedMembersIfNeeded(cfirDeclaration)
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

    /**
     * 只有 regular class 目前会在结构单元上挂 synthetic 成员。
     * interface / 其他 class-like 共享同一 file-structure 路径，但不引入额外生成成员解析。
     */
    private fun lazyResolveClassGeneratedMembersIfNeeded(cfirClassLike: CfirClassLikeDeclaration) {
        val cfirClass = cfirClassLike as? CfirClass ?: return
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
