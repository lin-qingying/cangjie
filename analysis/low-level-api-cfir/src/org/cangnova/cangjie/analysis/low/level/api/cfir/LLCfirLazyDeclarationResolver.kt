/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirResolvableSession
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.ThreadSafeMutableState
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.util.superConeTypes
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.resolve.toClassSymbol

@ThreadSafeMutableState
internal class LLCfirLazyDeclarationResolver : CfirLazyDeclarationResolver() {
    override fun startResolvingPhase(phase: CfirResolvePhase) {}
    override fun finishResolvingPhase(phase: CfirResolvePhase) {}

    override fun lazyResolveToPhase(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
        assertLazyResolveAllowed()
        val session = element.llCfirResolvableSession ?: return
        session.moduleComponents.cfirModuleLazyDeclarationResolver.lazyResolve(
            target = element,
            toPhase = toPhase,
        )
    }

    override fun lazyResolveToPhaseWithCallableMembers(clazz: CfirClass, toPhase: CfirResolvePhase) {
        assertLazyResolveAllowed()
        val cfirClass = clazz as? CfirClass ?: return
        val session = cfirClass.llCfirResolvableSession ?: return
        session.moduleComponents.cfirModuleLazyDeclarationResolver.lazyResolveWithCallableMembers(
            target = cfirClass,
            toPhase = toPhase,
        )

        if (toPhase == CfirResolvePhase.STATUS && cfirClass.declarations.none { it is CfirCallableDeclaration }) {
            for (superType in cfirClass.superConeTypes) {
                val classSymbol = superType.toClassSymbol(session) ?: continue
                lazyResolveToPhaseWithCallableMembers(classSymbol.cfir, toPhase)
            }
        }
    }

    override fun lazyResolveToPhaseRecursively(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
        assertLazyResolveAllowed()
        val session = element.llCfirResolvableSession ?: return
        session.moduleComponents.cfirModuleLazyDeclarationResolver.lazyResolveRecursively(
            target = element,
            toPhase = toPhase,
        )
    }
}
