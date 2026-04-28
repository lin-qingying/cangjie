/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLCfirResolveTarget
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.throwUnexpectedCfirElementError
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.LLCfirDeclarationModificationService
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkReturnTypeRefIsResolved
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.canHaveDeferredReturnTypeCalculation
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirVariable

import org.cangnova.cangjie.cfir.resolve.body.CfirImplicitAwareBodyResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirImplicitBodyResolveComputationSession
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef

/**
 * low-level 的隐式类型阶段解析器。
 *
 * 设计上对齐 Kotlin LL FIR 的职责边界：这里只负责把目标推进到 IMPLICIT_TYPES，
 * 不引入仓颉不存在的 declaration shape（匿名初始化块、field、enum entry 等）。
 */
internal object LLCfirImplicitTypesLazyResolver : LLCfirLazyResolver(CfirResolvePhase.IMPLICIT_TYPES) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver =
        LLCfirImplicitBodyTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        if (target !is CfirCallableDeclaration) return
        checkReturnTypeRefIsResolved(target)
    }
}

/**
 * 与主干 `CfirImplicitBodyResolveComputationSession` 同构复用。
 * low-level 不再自造一套独立状态机类型。
 */
internal typealias LLImplicitBodyResolveComputationSession = CfirImplicitBodyResolveComputationSession

private class LLCfirImplicitBodyTargetResolver(
    target: LLCfirResolveTarget,
    llImplicitBodyResolveComputationSessionParameter: LLImplicitBodyResolveComputationSession? = null,
) : LLCfirAbstractBodyTargetResolver(
    resolveTarget = target,
    resolvePhase = CfirResolvePhase.IMPLICIT_TYPES,
    llImplicitBodyResolveComputationSession =
        llImplicitBodyResolveComputationSessionParameter ?: LLImplicitBodyResolveComputationSession(),
) {
    override val transformer = CfirImplicitAwareBodyResolveTransformer(
        session = resolveTargetSession,
        scopeSession = resolveTargetScopeSession,
        implicitBodyResolveComputationSession = llImplicitBodyResolveComputationSession,
        phase = resolverPhase,
        implicitTypeOnly = true,
        returnTypeCalculator = createReturnTypeCalculator(),
    )

    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        when (target) {
            is CfirCallableDeclaration if target.canHaveDeferredReturnTypeCalculation -> {
                transformer.context.returnTypeCalculator.callableCopyTypeCalculator.computeReturnType(target)
            }

            is CfirFunction -> {
                if (target.returnTypeRef is CfirImplicitTypeRef) {
                    resolve(target, BodyStateKeepers.FUNCTION)
                }
            }

            is CfirProperty -> {
                if (target.shouldBeResolvedOnImplicitTypePhase) {
                    resolve(target, BodyStateKeepers.PROPERTY)
                }
            }

            is CfirVariable -> {
                if (target.returnTypeRef is CfirImplicitTypeRef) {
                    resolve(target, BodyStateKeepers.VARIABLE)
                }
            }

            is CfirClassLikeDeclaration, is CfirExtend, is CfirTypeAlias, is CfirFile, is CfirCodeFragment -> {
                // 这些声明在仓颉 IMPLICIT_TYPES 阶段无 body 级隐式类型求解入口
            }

            else -> throwUnexpectedCfirElementError(target)
        }
    }

    override fun rawResolve(target: CfirElementWithResolveState) {
        super.rawResolve(target)
        LLCfirDeclarationModificationService.bodyResolved(target, resolverPhase)
    }
}

/**
 * 属性在 IMPLICIT_TYPES 阶段需要推进的判定。
 */
internal val CfirProperty.shouldBeResolvedOnImplicitTypePhase: Boolean
    get() = returnTypeRef is CfirImplicitTypeRef
