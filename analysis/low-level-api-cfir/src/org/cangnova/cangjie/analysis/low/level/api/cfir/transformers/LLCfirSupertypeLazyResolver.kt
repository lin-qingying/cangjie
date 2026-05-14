/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkTypeRefIsResolved
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.resolve.transformers.runSupertypeResolvePhaseForNonLocalClassLikeDeclaration
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase

internal object LLCfirSupertypeLazyResolver : LLCfirLazyResolver(CfirResolvePhase.SUPER_TYPES) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver = LLCfirSuperTypeTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        when (target) {
            is CfirClass -> {
                for (superTypeRef in target.superTypeRefs) {
                    checkTypeRefIsResolved(superTypeRef, "class super type", target)
                }
            }

            is CfirTypeAlias -> {
                checkTypeRefIsResolved(target.expandedTypeRef, typeRefName = "type alias expanded type", target)
            }
        }
    }
}

/**
 * SUPER_TYPES 阶段在 low-level 中直接复用主干 supertype resolve 流水线，
 * 不再跨模块访问编译器 internal visitor。
 */
private class LLCfirSuperTypeTargetResolver(
    target: LLCfirResolveTarget,
) : LLCfirTargetResolver(target, CfirResolvePhase.SUPER_TYPES) {
    @Deprecated("Should never be called directly, only for override purposes, please use withClassLike", level = DeprecationLevel.ERROR)
    override fun withContainingClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit) {
        if (cfirClassLike.resolvePhase < resolverPhase) {
            doResolveWithoutLock(cfirClassLike)
        }
        action()
    }

    @Deprecated("Should never be called directly, only for override purposes, please use withExtend", level = DeprecationLevel.ERROR)
    override fun withContainingExtend(cfirExtend: CfirExtend, action: () -> Unit) {
        if (cfirExtend.resolvePhase < resolverPhase) {
            doResolveWithoutLock(cfirExtend)
        }
        action()
    }

    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean {
        when (target) {
            is CfirClassLikeDeclaration -> {
                target.lazyResolveToPhase(resolverPhase.previous)
                performCustomResolveUnderLock(target) {
                    target.runSupertypeResolvePhaseForNonLocalClassLikeDeclaration(
                        session = resolveTargetSession,
                        scopeSession = resolveTargetScopeSession,
                        useSiteFile = containingFile(),
                        containingDeclarations = containingDeclarations,
                    )
                }
            }

            is CfirExtend -> {
                target.lazyResolveToPhase(resolverPhase.previous)
                performCustomResolveUnderLock(target) {
                    target.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
                }
            }

            is CfirFile -> {
                target.lazyResolveToPhase(resolverPhase.previous)
                performCustomResolveUnderLock(target) {
                    // file 自身没有可应用的 supertype 结果，目标声明会单独推进
                }
            }

            else -> {
                performCustomResolveUnderLock(target) {
                    // 对齐 Kotlin：非 class-like target 在 SUPER_TYPES 阶段只需要推进相位。
                }
            }
        }

        return true
    }

    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        error("Should be resolved without lock in ${::doResolveWithoutLock.name}")
    }

    private fun containingFile(): CfirFile? {
        return containingDeclarations.lastOrNull { it is CfirFile } as? CfirFile ?: resolveTarget.cfirFile
    }
}
