/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.cfir

import org.cangnova.cangjie.analysis.low.level.api.cfir.ContextByDesignationCollector
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.collectDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.withCfirDesignationEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.isLocalForLazyResolutionPurposes
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContextForProvider
import org.cangnova.cangjie.cfir.analysis.collectors.AbstractDiagnosticCollectorVisitor
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * 沿 designation 路径运行 diagnostics visitor，以恢复目标声明处的 checker context。
 */
private class ContextCollectingDiagnosticCollectorVisitor private constructor(
    sessionHolder: SessionAndScopeSessionHolder,
    designation: CfirDesignation,
) : AbstractDiagnosticCollectorVisitor(
    PersistentCheckerContextFactory.createEmptyPersistenceCheckerContext(sessionHolder)
) {
    /**
     * 根据 designation 路径推进 visitor 并在目标位置快照 checker context。
     */
    private val contextCollector = object : ContextByDesignationCollector<CheckerContextForProvider>(designation) {
        override fun getCurrentContext(): CheckerContextForProvider =
            PersistentCheckerContextFactory.createPersistenceCheckerContextSnapshot(context)

        override fun getCurrentContextForTarget(target: CfirElementWithResolveState): CheckerContextForProvider =
            PersistentCheckerContextFactory.createPersistenceCheckerContextSnapshot(context, target as CfirDeclaration)

        override fun goToNestedDeclaration(target: CfirElementWithResolveState) {
            target.accept(this@ContextCollectingDiagnosticCollectorVisitor, null)
        }
    }

    /**
     * 进入嵌套声明时推进 designation collector，否则继续普通子节点遍历。
     */
    override fun visitNestedElements(element: CfirElement) {
        if (element is CfirDeclaration) {
            contextCollector.nextStep()
        } else {
            element.accept(this, null)
        }
    }

    /**
     * 上下文收集阶段不实际运行 checker。
     */
    override fun checkElement(element: CfirElement) {}

    /**
     * 触发 context collector 并返回目标声明处的 checker context。
     */
    fun collect(): CheckerContextForProvider {
        // Trigger the collector
        contextCollector.nextStep()

        return contextCollector.getCollectedContext()
    }

    companion object {
        fun collect(sessionHolder: SessionAndScopeSessionHolder, designation: CfirDesignation): CheckerContextForProvider {
            requireWithAttachment(designation.fileOrNull != null, { "${CfirFile::class.simpleName} is missed" }) {
                withCfirDesignationEntry("designation", designation)
            }

            return ContextCollectingDiagnosticCollectorVisitor(sessionHolder, designation).collect()
        }
    }
}

/**
 * 对外提供持久 checker context 收集能力的入口。
 */
internal object PersistenceContextCollector {
    /**
     * 收集指定非局部声明在给定 CFIR 文件中的 checker context。
     */
    fun collectContext(
        sessionHolder: SessionAndScopeSessionHolder,
        cfirFile: CfirFile,
        declaration: CfirDeclaration,
    ): CheckerContextForProvider {
        val isLocal = when (declaration) {
            is CfirClassLikeDeclaration -> false
            is CfirExtend -> false
            is CfirCallableDeclaration -> declaration.symbol.isLocalForLazyResolutionPurposes
            is CfirCodeFragment -> false
            else -> errorWithAttachment("Unsupported declaration ${declaration::class}") {
                withCfirEntry("declaration", declaration)
            }
        }

        requireWithAttachment(
            !isLocal,
            { "Cannot collect context for local declaration ${declaration::class.simpleName}" },
        ) {
            withCfirEntry("declaration", declaration)
        }

        val designation = declaration.collectDesignation(cfirFile)
        designation.path.asReversed().forEach {
            it.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
        }

        return ContextCollectingDiagnosticCollectorVisitor.collect(sessionHolder, designation)
    }
}
