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
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * low-level 的隐式类型阶段解析器。
 *
 * 设计上对齐 Kotlin LL FIR 的职责边界：这里只负责把目标推进到 IMPLICIT_TYPES，
 * 不引入仓颉不存在的 declaration shape（匿名初始化块、field、enum entry 等）。
 */
internal object LLCfirImplicitTypesLazyResolver : LLCfirLazyResolver(CfirResolvePhase.IMPLICIT_TYPES) {
    /**
     * 为 [target] 创建 IMPLICIT_TYPES 阶段的 body 目标解析器。
     */
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver =
        LLCfirImplicitBodyTargetResolver(target)

    /**
     * 校验 callable 的返回类型已经在隐式类型阶段完成解析。
     */
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

/**
 * IMPLICIT_TYPES 阶段的 low-level body 目标解析器。
 *
 * 该解析器只处理需要隐式返回类型或隐式变量类型计算的声明，并在 raw resolve 后通知声明修改服务 body 已解析。
 */
internal class LLCfirImplicitBodyTargetResolver(
    target: LLCfirResolveTarget,
    llImplicitBodyResolveComputationSessionParameter: LLImplicitBodyResolveComputationSession? = null,
) : LLCfirAbstractBodyTargetResolver(
    resolveTarget = target,
    resolvePhase = CfirResolvePhase.IMPLICIT_TYPES,
        llImplicitBodyResolveComputationSession =
        llImplicitBodyResolveComputationSessionParameter ?: LLImplicitBodyResolveComputationSession(),
) {
    /**
     * 只执行隐式类型求解的 body transformer。
     */
    override val transformer = object : CfirImplicitAwareBodyResolveTransformer(
        session = resolveTargetSession,
        scopeSession = resolveTargetScopeSession,
        implicitBodyResolveComputationSession = llImplicitBodyResolveComputationSession,
        phase = resolverPhase,
        implicitTypeOnly = true,
        returnTypeCalculator = createReturnTypeCalculator(),
    ) {
        /**
         * IMPLICIT_TYPES 阶段不保留类 CFG。
         */
        override val preserveCFGForClasses: Boolean get() = false
        /**
         * IMPLICIT_TYPES 阶段不构建文件级 CFG。
         */
        override val buildCfgForFiles: Boolean get() = false
    }

    /**
     * 与 [LLCfirReturnTypeCalculatorWithJump.resolveDeclaration] 保持同步：
     * jumping resolve 检测到递归时先记录符号，随后由返回类型计算器统一产出递归错误类型。
     */
    override fun handleCycleInResolution(target: CfirElementWithResolveState) {
        requireWithAttachment(target is CfirCallableDeclaration, { "Resolution cycle is supposed to be only for callable declaration" }) {
            withCfirEntry("target", target)
        }

        llImplicitBodyResolveComputationSession.pushCycledSymbol((target as CfirCallableDeclaration).symbol)
    }

    /**
     * 在目标锁内推进 [target] 的隐式类型解析。
     */
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

    /**
     * 执行父类 raw body 解析，并记录 body 已推进到当前阶段。
     */
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
