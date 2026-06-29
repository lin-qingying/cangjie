/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.asResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkCanceled
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.body.ReturnTypeCalculatorWithJump
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef

/**
 * low-level 专用的隐式返回类型计算器。
 *
 * 对齐 Kotlin `LLFirReturnTypeCalculatorWithJump`：当按需计算返回类型时，
 * 不走主编译器的 file-level designated transformer，而是收集 LL designation 后交给
 * [LLCfirImplicitBodyTargetResolver]，确保锁、缓存与局部 LL lazy resolve 状态一致。
 */
internal class LLCfirReturnTypeCalculatorWithJump(
    session: CfirSession,
    scopeSession: ScopeSession,
    implicitBodyResolveComputationSession: LLImplicitBodyResolveComputationSession,
) : ReturnTypeCalculatorWithJump(session, scopeSession, implicitBodyResolveComputationSession) {
    /**
     * 解析 [declaration] 的返回类型，并在需要隐式类型计算时跳转到 LL 懒解析流程。
     */
    override fun resolveDeclaration(declaration: CfirCallableDeclaration): CfirResolvedTypeRef {
        val hasSomethingToResolveOnImplicitTypePhase = when (declaration) {
            is CfirProperty -> declaration.shouldBeResolvedOnImplicitTypePhase
            else -> declaration.returnTypeRef is CfirImplicitTypeRef
        }

        if (!hasSomethingToResolveOnImplicitTypePhase) {
            return declaration.symbol.resolvedReturnTypeRef
        }

        declaration.lazyResolveToPhase(CfirResolvePhase.IMPLICIT_TYPES.previous)

        val designation = declaration.collectDesignation().asResolveTarget()
        val computationSession = implicitBodyResolveComputationSession
        val resolver = LLCfirImplicitBodyTargetResolver(
            designation,
            llImplicitBodyResolveComputationSessionParameter = computationSession,
        )

        resolver.resolveDesignation()

        if (computationSession.popCycledSymbolIfExists() == declaration.symbol) {
            return recursionInImplicitTypeRef(declaration)
        }

        LLCfirImplicitTypesLazyResolver.checkIsResolved(declaration)
        return declaration.returnTypeRef as CfirResolvedTypeRef
    }

    /**
     * 尝试计算 [declaration] 的返回类型，并在委托父类前执行取消检查。
     */
    override fun tryCalculateReturnTypeOrNull(declaration: CfirCallableDeclaration): CfirResolvedTypeRef {
        checkCanceled()
        return super.tryCalculateReturnTypeOrNull(declaration)
    }
}
