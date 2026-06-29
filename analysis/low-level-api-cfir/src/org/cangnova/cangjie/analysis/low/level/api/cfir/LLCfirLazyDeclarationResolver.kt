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

/**
 * low-level CFIR session 暴露给 CFIR 树的 lazy declaration resolver 实现。
 */
@ThreadSafeMutableState
internal class LLCfirLazyDeclarationResolver : CfirLazyDeclarationResolver() {
    /**
     * low-level resolver 的阶段开始回调由模块级 lazy resolver 处理，这里保持空实现。
     */
    override fun startResolvingPhase(phase: CfirResolvePhase) {}

    /**
     * low-level resolver 的阶段结束回调由模块级 lazy resolver 处理，这里保持空实现。
     */
    override fun finishResolvingPhase(phase: CfirResolvePhase) {}

    /**
     * 将单个 CFIR 元素推进到指定解析阶段。
     */
    override fun lazyResolveToPhase(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
        assertLazyResolveAllowed()
        val session = element.llCfirResolvableSession ?: return
        session.moduleComponents.cfirModuleLazyDeclarationResolver.lazyResolve(
            target = element,
            toPhase = toPhase,
        )
    }

    /**
     * 将 class 本身和 callable 成员推进到指定解析阶段。
     */
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

    /**
     * 递归推进目标元素及其声明子树到指定解析阶段。
     */
    override fun lazyResolveToPhaseRecursively(element: CfirElementWithResolveState, toPhase: CfirResolvePhase) {
        assertLazyResolveAllowed()
        val session = element.llCfirResolvableSession ?: return
        session.moduleComponents.cfirModuleLazyDeclarationResolver.lazyResolveRecursively(
            target = element,
            toPhase = toPhase,
        )
    }
}
