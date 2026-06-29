/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.CfirLazyBodiesCalculator
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformerDispatcher
import org.cangnova.cangjie.cfir.visitors.transformSingle

/**
 * body 相关懒解析目标解析器的公共基类。
 *
 * 该类型封装 body 解析阶段通用的容器上下文进入逻辑、返回类型计算器创建逻辑，以及通过 [StateKeeper] 保护目标声明状态的
 * 原子解析流程。具体 body 阶段由子类提供实际 [transformer]。
 */
internal sealed class LLCfirAbstractBodyTargetResolver(
    resolveTarget: LLCfirResolveTarget,
    resolvePhase: CfirResolvePhase,
    /**
     * 隐式 body 解析计算会话，用于在返回类型推断和 body 解析之间传递递归检测状态。
     */
    protected val llImplicitBodyResolveComputationSession: LLImplicitBodyResolveComputationSession = LLImplicitBodyResolveComputationSession(),
) : LLCfirTargetResolver(resolveTarget, resolvePhase) {
    /**
     * 创建允许跳转到 LL 懒解析的返回类型计算器。
     */
    protected fun createReturnTypeCalculator(): LLCfirReturnTypeCalculatorWithJump = LLCfirReturnTypeCalculatorWithJump(
        resolveTargetSession,
        resolveTargetScopeSession,
        llImplicitBodyResolveComputationSession,
    )

    /**
     * 当前 body 解析阶段使用的 CFIR transformer。
     */
    abstract val transformer: CfirAbstractBodyResolveTransformerDispatcher

    /**
     * 校验解析器阶段与 transformer 阶段一致。
     */
    override fun checkResolveConsistency() {
        check(resolverPhase == transformer.transformerPhase) {
            "Inconsistent Resolver($resolverPhase) and Transformer(${transformer.transformerPhase}) phases"
        }
    }

    /**
     * 在 [cfirFile] 文件上下文中执行 [action]。
     */
    @Deprecated("Should never be called directly, only for override purposes, please use withFile", level = DeprecationLevel.ERROR)
    override fun withContainingFile(cfirFile: CfirFile, action: () -> Unit) {
        transformer.declarationsTransformer.withFile(cfirFile) {
            action()
            cfirFile
        }
    }

    /**
     * 在 [cfirClassLike] class-like 声明上下文和类作用域中执行 [action]。
     */
    @Deprecated("Should never be called directly, only for override purposes, please use withClassLike", level = DeprecationLevel.ERROR)
    override fun withContainingClassLike(cfirClassLike: CfirClassLikeDeclaration, action: () -> Unit) {
        val declarationsTransformer = transformer.declarationsTransformer
        val context = declarationsTransformer.context
        val actionWithScopes = {
            context.withScopesForClass(cfirClassLike, declarationsTransformer.components) {
                action()
            }
        }

        if (cfirClassLike is CfirClass) {
            context.withContainingClass(cfirClassLike, actionWithScopes)
        } else {
            actionWithScopes()
        }
    }

    /**
     * 在 [cfirExtend] 扩展声明容器上下文中执行 [action]。
     */
    @Deprecated("Should never be called directly, only for override purposes, please use withExtend", level = DeprecationLevel.ERROR)
    override fun withContainingExtend(cfirExtend: CfirExtend, action: () -> Unit) {
        transformer.declarationsTransformer.context.withContainer(cfirExtend) {
            action()
        }
    }

    /**
     * 使用 [keeper] 保存 [target] 当前状态，准备懒 body 后执行 raw 解析。
     */
    protected fun <T : CfirElementWithResolveState> resolve(target: T, keeper: StateKeeper<T, CfirDesignation>) {
        val cfirDesignation = CfirDesignation(containingDeclarations, target)
        resolveWithKeeper(target, cfirDesignation, keeper, { CfirLazyBodiesCalculator.calculateBodies(cfirDesignation) }) {
            rawResolve(target)
        }
    }

    /**
     * 对 [target] 执行当前 body transformer 的实际转换。
     */
    protected open fun rawResolve(target: CfirElementWithResolveState) {
        target.transformSingle(transformer, ResolutionMode.ContextIndependent)
    }

}
