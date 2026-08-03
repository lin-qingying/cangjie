/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.LLCfirReturnTypeCalculatorWithJump
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.LLImplicitBodyResolveComputationSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContextForProvider
import org.cangnova.cangjie.cfir.analysis.checkers.context.PersistentCheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration

/**
 * 为 low-level diagnostics 恢复和快照持久 checker context 的工厂，
 * 对齐 Kotlin 那边由 low-level analysis 直接构造 `PersistentCheckerContext` 的写法。
 *
 * 不再传 `reporter` 字段——仓颉 checker 走 context receiver 注入 reporter，
 * `CheckerContext` 不背 reporter（对齐 Kotlin `CheckerContext` 字段构成）。
 */
internal object PersistentCheckerContextFactory {
    fun createEmptyPersistenceCheckerContext(sessionHolder: SessionAndScopeSessionHolder): PersistentCheckerContext {
        val returnTypeCalculator = LLCfirReturnTypeCalculatorWithJump(
            scopeSession = sessionHolder.scopeSession,
            implicitBodyResolveComputationSession = LLImplicitBodyResolveComputationSession(),
        )

        return PersistentCheckerContext(sessionHolder, returnTypeCalculator)
    }


    /**
     * 复制已有 checker context，并可追加当前目标声明到 containing declarations。
     *
     * 走 [PersistentCheckerContext] 的 copy-on-write `addDeclaration`——
     * 即便上游 mutable context 继续压栈弹栈，已发出的快照引用不变。
     */
    fun createPersistenceCheckerContextSnapshot(
        context: CheckerContextForProvider,
        additionalDeclaration: CfirDeclaration? = null,
    ): CheckerContextForProvider {
        val snapshot = PersistentCheckerContext(
            sessionHolder = context.sessionHolder,
            returnTypeCalculator = context.returnTypeCalculator,
        ).let { persistent ->
            // 先把上游 mutable context 的栈快照复制进 persistent——靠 copy-on-write add 一遍。
            context.containingDeclarations.fold(persistent) { acc, symbol ->
                acc.addDeclaration(symbol.cfir)
            }
        }

        return additionalDeclaration?.let(snapshot::addDeclaration) ?: snapshot
    }
}
