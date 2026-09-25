/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchRunner
import org.cangnova.cangjie.cfir.session.cjmpMappingStorageOrNull
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase

/**
 * CJMP_MATCHING 阶段的低阶懒解析入口（G16：LL 分析管线配对入口）。
 *
 * 对齐 Kotlin `LLFirExpectActualMatcherLazyResolver`：
 * - LL 路径按声明级锁把目标推进到 [CfirResolvePhase.CJMP_MATCHING]，并就地执行配对写存储；
 * - 外层 class-like 在内部声明之前先完成配对（内部声明的候选查找依赖外层配对结果）；
 * - 版本门（`LanguageFeature.CommonSpecificDeclarations`）关闭时不做任何事（不写存储、不推相位）。
 *
 * 配对结果落在 session 存储（`CfirCjmpMappingStorage`），不是声明树状态，故
 * [phaseSpecificCheckIsResolved] 无需树级完成性校验。
 */
internal object LLCfirCjmpMatchingLazyResolver : LLCfirLazyResolver(CfirResolvePhase.CJMP_MATCHING) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver =
        LLCfirCjmpMatchingTargetResolver(target)

    /**
     * 配对结果写 session 存储，无声明树完成性可校验。
     */
    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        // 存储态由 CfirCjmpMappingStorage 承载；此处无可校验的树级不变量。
    }
}

private class LLCfirCjmpMatchingTargetResolver(
    target: LLCfirResolveTarget,
) : LLCfirTargetResolver(target, CfirResolvePhase.CJMP_MATCHING) {
    /** 版本门判定（对齐 Kotlin `LanguageFeature.MultiPlatformProjects.isEnabled()`）。 */
    private val enabled: Boolean =
        resolveTargetSession.languageVersionSettings.supportsFeature(LanguageFeature.CommonSpecificDeclarations)

    /**
     * 进入外围 class-like 前先推进其到前一阶段并完成配对，再继续内部声明。
     */
    @Deprecated("Should never be called directly, only for override purposes, please use withClassLike", level = DeprecationLevel.ERROR)
    override fun withContainingClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit) {
        if (enabled) {
            cfirClassLike.lazyResolveToPhase(resolverPhase.previous)
            performResolve(cfirClassLike)
        }
        action()
    }

    /**
     * 在目标锁之外先推进到前一阶段，再在目标锁内完成配对与相位推进。
     */
    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean {
        if (!enabled) return true
        target.lazyResolveToPhase(resolverPhase.previous)
        performCustomResolveUnderLock(target) {
            doLazyResolveUnderLock(target)
        }
        return true
    }

    /**
     * 目标锁内的配对执行（声明级）。
     */
    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        if (enabled && target is CfirMemberDeclaration) {
            if (CfirCjmpMatchRunner.canHaveCommonCounterpart(target)) {
                // 轻量会话（平台 common/桩会话）可能未装配存储：按"无配对数据"处理
                resolveTargetSession.cjmpMappingStorageOrNull?.let { storage ->
                    CfirCjmpMatchRunner.matchDeclaration(target, resolveTargetSession, storage)
                }
            }
        }
        if (target.resolvePhase < CfirResolvePhase.CJMP_MATCHING) {
            target.replaceResolvePhase(CfirResolvePhase.CJMP_MATCHING)
        }
    }
}
