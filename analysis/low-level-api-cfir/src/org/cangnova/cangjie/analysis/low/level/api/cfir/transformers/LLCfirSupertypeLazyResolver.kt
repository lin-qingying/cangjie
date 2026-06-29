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

/**
 * SUPER_TYPES 阶段的低阶懒解析入口。
 */
internal object LLCfirSupertypeLazyResolver : LLCfirLazyResolver(CfirResolvePhase.SUPER_TYPES) {
    /**
     * 为 [target] 创建 SUPER_TYPES 阶段目标解析器。
     */
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver = LLCfirSuperTypeTargetResolver(target)

    /**
     * 校验类的父类型和 typealias 展开类型已经完成类型解析。
     */
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
    /**
     * 进入外围 class-like 前确保该 class-like 已完成 SUPER_TYPES。
     */
    @Deprecated("Should never be called directly, only for override purposes, please use withClassLike", level = DeprecationLevel.ERROR)
    override fun withContainingClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit) {
        if (cfirClassLike.resolvePhase < resolverPhase) {
            doResolveWithoutLock(cfirClassLike)
        }
        action()
    }

    /**
     * 进入外围 extend 前确保该 extend 已完成 SUPER_TYPES。
     */
    @Deprecated("Should never be called directly, only for override purposes, please use withExtend", level = DeprecationLevel.ERROR)
    override fun withContainingExtend(cfirExtend: CfirExtend, action: () -> Unit) {
        if (cfirExtend.resolvePhase < resolverPhase) {
            doResolveWithoutLock(cfirExtend)
        }
        action()
    }

    /**
     * 在无目标锁阶段执行 SUPER_TYPES 解析并负责加自定义写锁。
     */
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

    /**
     * SUPER_TYPES 阶段必须通过 [doResolveWithoutLock] 完成解析，因此锁内入口不可调用。
     */
    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        error("Should be resolved without lock in ${::doResolveWithoutLock.name}")
    }

    /**
     * 返回当前 designation 中最近的文件容器，找不到时使用解析目标携带的文件。
     */
    private fun containingFile(): CfirFile? {
        return containingDeclarations.lastOrNull { it is CfirFile } as? CfirFile ?: resolveTarget.cfirFile
    }
}
