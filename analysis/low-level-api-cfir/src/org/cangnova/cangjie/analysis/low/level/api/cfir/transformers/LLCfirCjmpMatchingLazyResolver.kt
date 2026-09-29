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

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchRunner
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpResolver
import org.cangnova.cangjie.cfir.session.cjmpMappingStorage
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.resolve.transformers.CfirCjmpMatcherTransformer

/**
 * CJMP_MATCHING 阶段的低阶懒解析入口（G16：LL 分析管线配对入口）。
 *
 * 对齐 Kotlin `LLFirExpectActualMatcherLazyResolver`：
 * - LL 路径按声明级锁把目标推进到 [CfirResolvePhase.CJMP_MATCHING]，并就地执行配对写存储；
 * - 外层 class-like 在内部声明之前先完成配对（内部声明的候选查找依赖外层配对结果）；
 * - 版本门关闭或 session 不处于 specific 编译模式时不做任何事（不写存储、不推相位）。
 *
 * 配对结果落在 session 存储（`CfirCjmpMappingStorage`）；该阶段后置条件据此检查 specific 声明已有处理结果。
 */
internal object LLCfirCjmpMatchingLazyResolver : LLCfirLazyResolver(CfirResolvePhase.CJMP_MATCHING) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver =
        LLCfirCjmpMatchingTargetResolver(target)

    /** 以 session 存储的不变量验证 specific 声明都经过配对判定。 */
    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        val declaration = target as? CfirMemberDeclaration ?: return
        if (!declaration.status.isSpecific || !CfirCjmpMatchRunner.canHaveCommonCounterpart(declaration)) return
        if (declaration is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor) return
        check(declaration.moduleData.session.cjmpMappingStorage.hasResolutionResult(declaration)) {
            "CJMP_MATCHING did not record a result for ${declaration::class.simpleName}"
        }
    }

    override fun shouldCheckIsResolved(target: CfirElementWithResolveState): Boolean =
        target.cjmpMatchingSession()?.isCjmpSpecificCompilationEnabled() ?: false
}

private class LLCfirCjmpMatchingTargetResolver(
    target: LLCfirResolveTarget,
) : LLCfirTargetResolver(target, CfirResolvePhase.CJMP_MATCHING) {
    private val matcherTransformer = CfirCjmpMatcherTransformer(resolveTargetSession, resolveTargetScopeSession)

    /** LL 配对只在 specific 编译中执行。 */
    private fun isCjmpMatchingEnabled(): Boolean = resolveTargetSession.isCjmpSpecificCompilationEnabled()

    /**
     * 进入外围 class-like 前先推进其到前一阶段并完成配对，再继续内部声明。
     */
    @Deprecated("Should never be called directly, only for override purposes, please use withClassLike", level = DeprecationLevel.ERROR)
    override fun withContainingClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit) {
        if (isCjmpMatchingEnabled()) {
            cfirClassLike.lazyResolveToPhase(resolverPhase.previous)
            performResolve(cfirClassLike)
        }
        action()
    }

    /** 进入 extend 成员前先完成外层 extend 配对，使成员类型参数映射可供成员匹配使用。 */
    @Deprecated("Should never be called directly, only for override purposes, please use withExtend", level = DeprecationLevel.ERROR)
    override fun withContainingExtend(extend: org.cangnova.cangjie.cfir.declarations.CfirExtend, action: () -> Unit) {
        if (isCjmpMatchingEnabled()) {
            extend.lazyResolveToPhase(resolverPhase.previous)
            performResolve(extend)
        }
        action()
    }

    /**
     * 在目标锁之外先推进到前一阶段，再在目标锁内完成配对与相位推进。
     */
    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean {
        if (!isCjmpMatchingEnabled()) return true
        target.lazyResolveToPhase(resolverPhase.previous)
        when (target) {
            is CfirPatternVariable ->
                CfirCjmpResolver.findSpecificTopLevelPatternVariablesInMatchOrder(target, resolveTargetSession)
                    .filter { it !== target }
                    .forEach { it.lazyResolveToPhase(resolverPhase.previous) }

            is CfirCallableDeclaration ->
            CfirCjmpResolver.findSpecificCallablesInMatchOrder(target, resolveTargetSession)
                .asSequence()
                .filter { it !== target }
                .forEach { it.lazyResolveToPhase(resolverPhase.previous) }
        }
        performCustomResolveUnderLock(target) {
            doLazyResolveUnderLock(target)
        }
        return true
    }

    /**
     * 目标锁内的配对执行（声明级）。
     */
    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        if (isCjmpMatchingEnabled() && target is CfirMemberDeclaration) {
            if (CfirCjmpMatchRunner.canHaveCommonCounterpart(target)) {
                matcherTransformer.transformMemberDeclaration(target)
            }
        }
        if (target.resolvePhase < CfirResolvePhase.CJMP_MATCHING) {
            target.replaceResolvePhase(CfirResolvePhase.CJMP_MATCHING)
        }
    }
}

private fun CfirElementWithResolveState.cjmpMatchingSession() = when (this) {
    is CfirDeclaration -> moduleData.session
    is CfirFile -> moduleData.session
    else -> null
}
