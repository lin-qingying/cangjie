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
    private val designation: CfirDesignation,
    private val contextKind: PersistenceContextCollector.ContextKind,
) : AbstractDiagnosticCollectorVisitor(
    PersistentCheckerContextFactory.createEmptyPersistenceCheckerContext(sessionHolder)
) {
    private var nestedDeclarationsContext: CheckerContextForProvider? = null
    /**
     * 根据 designation 路径推进 visitor 并在目标位置快照 checker context。
     */
    private val contextCollector = object : ContextByDesignationCollector<CheckerContextForProvider>(designation) {
        override fun getCurrentContext(): CheckerContextForProvider = context

        override fun goToNestedDeclaration(target: CfirElementWithResolveState) {
            target.accept(this@ContextCollectingDiagnosticCollectorVisitor, null)
        }


    }

    /**
     * 进入嵌套声明时推进 designation collector，否则继续普通子节点遍历。
     */
    override fun visitNestedElements(element: CfirElement) {
        if (element is CfirDeclaration) {
            // 声明入口快照不包含自身；内部快照必须在 withDeclaration 压栈后采集，
            // 不能依赖是否有子声明，否则空 interface/class 会丢失这一层。
            if (element === designation.target && contextKind == PersistenceContextCollector.ContextKind.NESTED_DECLARATIONS) {
                check(nestedDeclarationsContext == null)
                nestedDeclarationsContext = context
            }
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

        return when (contextKind) {
            PersistenceContextCollector.ContextKind.DECLARATION_ENTRY -> contextCollector.getCollectedContext()
            PersistenceContextCollector.ContextKind.NESTED_DECLARATIONS -> checkNotNull(nestedDeclarationsContext) {
                "Target declaration nested context was not visited"
            }
        }
    }

    companion object {
        fun collect(
            sessionHolder: SessionAndScopeSessionHolder,
            designation: CfirDesignation,
            contextKind: PersistenceContextCollector.ContextKind,
        ): CheckerContextForProvider {
            requireWithAttachment(designation.fileOrNull != null, { "${CfirFile::class.simpleName} is missed" }) {
                withCfirDesignationEntry("designation", designation)
            }

            return ContextCollectingDiagnosticCollectorVisitor(sessionHolder, designation, contextKind).collect()
        }
    }
}

/**
 * 对外提供持久 checker context 收集能力的入口。
 */
internal object PersistenceContextCollector {
    /** 与 diagnostic visitor 生命周期一致的两种采样边界。 */
    enum class ContextKind {
        /** 进入目标声明前，供 structure-element diagnostics 初始化 visitor。 */
        DECLARATION_ENTRY,
        /** 目标声明已经压栈、即将遍历其内部，包含目标本身及其上下文。 */
        NESTED_DECLARATIONS,
    }

    /**
     * 收集指定非局部声明在给定 CFIR 文件中的 checker context。
     */
    fun collectContext(
        sessionHolder: SessionAndScopeSessionHolder,
        cfirFile: CfirFile,
        declaration: CfirDeclaration,
        contextKind: ContextKind = ContextKind.DECLARATION_ENTRY,
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

        return ContextCollectingDiagnosticCollectorVisitor.collect(sessionHolder, designation, contextKind)
    }
}
