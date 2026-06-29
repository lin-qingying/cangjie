/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirPartialBodyResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkCanceled
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkPhase
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment

/**
 * 负责指定 CFIR 阶段的懒解析入口。
 *
 * 该基类把 [LLCfirResolveTarget] 转换为具体 [LLCfirTargetResolver]，执行 designation 解析，并在解析后检查目标及其嵌套声明
 * 是否已经达到 [resolverPhase]。各阶段只需要提供目标解析器和阶段特有的完成性校验。
 *
 * @see LLCfirLazyResolverRunner
 * @see LLCfirTargetResolver
 */
internal sealed class LLCfirLazyResolver(val resolverPhase: CfirResolvePhase) {
    /**
     * 对 [target] 执行当前 [resolverPhase] 的懒解析。
     */
    fun resolve(target: LLCfirResolveTarget) {
        val resolver = createTargetResolver(target)
        requireWithAttachment(
            resolverPhase == resolver.resolverPhase,
            {
                """
                Phase mismatch between ${this::class.simpleName} and ${resolver::class.simpleName}.
                The resolver phase is ${resolver.resolverPhase}, but $resolverPhase is expected
                """.trimIndent()
            },
        )

        resolver.resolveDesignation()

        if (target !is LLCfirPartialBodyResolveTarget) {
            target.forEachTarget { declaration -> checkIsResolved(declaration) }
        }

        checkCanceled()
    }

    /**
     * 为 [target] 创建当前阶段的具体目标解析器。
     */
    protected abstract fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver

    /**
     * 校验 [target] 已经解析到 [resolverPhase]，并递归检查嵌套声明。
     */
    fun checkIsResolved(target: CfirElementWithResolveState) {
        target.checkPhase(resolverPhase)
        phaseSpecificCheckIsResolved(target)
        checkNestedDeclarationsAreResolved(target)
    }

    /**
     * 校验当前阶段的专有完成条件。
     *
     * 该校验会应用到已解析声明及其嵌套声明。
     *
     * @see checkNestedDeclarationsAreResolved
     */
    protected abstract fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState)

    /**
     * 递归检查 [target] 下属参数、访问器、类型参数等子声明的解析状态。
     */
    private fun checkNestedDeclarationsAreResolved(target: CfirElementWithResolveState) {
        if (target !is CfirDeclaration) return

        checkFunctionParametersAreResolved(target)
        checkVariableSubDeclarationsAreResolved(target)
        checkTypeParametersAreResolved(target)
        checkReceiversAreResolved(target)
    }

    /**
     * 检查声明接收者的解析状态。
     *
     * 当前 CFIR 模型没有在该入口下需要额外递归的 receiver 子声明，因此实现为空。
     */
    private fun checkReceiversAreResolved(declaration: CfirDeclaration) {
        return
    }

    /**
     * 检查属性访问器等变量附属声明的解析状态。
     */
    private fun checkVariableSubDeclarationsAreResolved(declaration: CfirDeclaration) {
        if (declaration !is CfirProperty) return

        declaration.getter?.let { checkIsResolved(it) }
        declaration.setter?.let { checkIsResolved(it) }
    }

    /**
     * 检查函数值参数的解析状态。
     */
    private fun checkFunctionParametersAreResolved(declaration: CfirDeclaration) {
        if (declaration !is CfirFunction) return

        declaration.valueParameters.forEach(::checkIsResolved)
    }

    /**
     * 检查声明类型参数的解析状态。
     */
    private fun checkTypeParametersAreResolved(declaration: CfirDeclaration) {
        if (declaration !is CfirTypeParameterRefsOwner) return

        for (parameter in declaration.typeParameters) {
            if (parameter !is CfirTypeParameter) continue
            checkIsResolved(parameter)
        }
    }
}
