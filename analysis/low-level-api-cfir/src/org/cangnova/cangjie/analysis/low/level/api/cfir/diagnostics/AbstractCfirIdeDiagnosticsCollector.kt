/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import com.intellij.util.SmartFMap
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.DiagnosticCheckerFilter
import org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.LLCheckersFactory.Provider.Companion.filterToCheckersMapUpdater
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.cfir.analysis.CheckersComponentInternal
import org.cangnova.cangjie.cfir.analysis.checkers.*
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.ComposedDeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.DeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.DeclarationCheckersDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.checkers.expression.ComposedExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.expression.ExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.expression.ExpressionCheckersDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.checkers.type.ComposedTypeCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.type.TypeCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.type.TypeCheckersDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.collectors.AbstractDiagnosticCollector
import org.cangnova.cangjie.cfir.analysis.collectors.DiagnosticCollectorComponents
import org.cangnova.cangjie.cfir.analysis.collectors.components.ControlFlowAnalysisDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.collectors.components.ErrorNodeDiagnosticCollectorComponent
import org.cangnova.cangjie.cfir.analysis.collectors.components.ReportCommitterDiagnosticComponent
import org.cangnova.cangjie.cfir.analysis.extensions.CfirAdditionalCheckersExtension
import org.cangnova.cangjie.cfir.analysis.extensions.additionalCheckers
import org.cangnova.cangjie.cfir.diagnostics.PendingDiagnosticReporter
import org.cangnova.cangjie.cfir.extensions.extensionService
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater

internal abstract class AbstractLLCfirDiagnosticsCollector(
    session: CfirSession,
    filter: DiagnosticCheckerFilter,
) : AbstractDiagnosticCollector(
    session,
    createComponents = { reporter ->
        session.checkersFactory.createComponents(filter, reporter)
    }
)

private val CfirSession.checkersFactory: LLCheckersFactory by CfirSession.sessionComponentAccessor()

/**
 * In the CLI mode all checkers are created once during the session initialization phase.
 * In the Analysis API checkers depend on [DiagnosticCheckerFilter].
 *
 * This factory provides an efficient way to get checkers for a given filter.
 *
 * @see org.cangnova.cangjie.cfir.analysis.CheckersComponent
 */
internal class LLCheckersFactory(val session: LLCfirSession) : CfirSessionComponent {
    private val declarationCheckersProvider = Provider(session, ::createDeclarationCheckers)
    private val expressionCheckersProvider = Provider(session, ::createExpressionCheckers)
    private val typeCheckersProvider = Provider(session, ::createTypeCheckers)

    fun createComponents(filter: DiagnosticCheckerFilter, reporter: PendingDiagnosticReporter): DiagnosticCollectorComponents {
        val declarationCheckers = declarationCheckersProvider.getOrCreateCheckers(filter)
        val expressionCheckers = expressionCheckersProvider.getOrCreateCheckers(filter)
        val typeCheckers = typeCheckersProvider.getOrCreateCheckers(filter)

        val regularComponents = buildList {
            if (!filter.runExtraCheckers && !filter.runExperimentalCheckers) {
                add(ErrorNodeDiagnosticCollectorComponent(session, reporter))
            }
            add(DeclarationCheckersDiagnosticComponent(session, reporter, declarationCheckers))
            add(ExpressionCheckersDiagnosticComponent(session, reporter, expressionCheckers))
            add(TypeCheckersDiagnosticComponent(session, reporter, typeCheckers))
            add(ControlFlowAnalysisDiagnosticComponent(session, reporter, declarationCheckers))
        }.toTypedArray()

        return DiagnosticCollectorComponents(regularComponents, ReportCommitterDiagnosticComponent(session, reporter))
    }

    /**
     * This provider allows creating checkers lazily based on a given [filter][DiagnosticCheckerFilter].
     */
    private class Provider<T>(
        private val session: CfirSession,
        private val checkersFactory: (
            filter: DiagnosticCheckerFilter,
            additionalCheckers: List<CfirAdditionalCheckersExtension>,
        ) -> T,
    ) {
        /** @see filterToCheckersMapUpdater */
        @Volatile
        private var filterToCheckersMap: SmartFMap<DiagnosticCheckerFilter, T> = SmartFMap.emptyMap()

        fun getOrCreateCheckers(filter: DiagnosticCheckerFilter): T {
            // Happy-path to avoid checkers recreation
            filterToCheckersMap[filter]?.let { return it }

            val checkers = createCheckers(filter)
            do {
                val oldMap = filterToCheckersMap
                oldMap[filter]?.let { return it }

                val newMap = oldMap.plus(filter, checkers)
            } while (!filterToCheckersMapUpdater.compareAndSet(/* obj = */ this, /* expect = */ oldMap, /* update = */ newMap))

            return checkers
        }

        private fun createCheckers(filter: DiagnosticCheckerFilter): T {
            val additionalCheckers = session.extensionService.additionalCheckers
            return checkersFactory(filter, additionalCheckers)
        }

        companion object {
            private val filterToCheckersMapUpdater = AtomicReferenceFieldUpdater.newUpdater(
                /* tclass = */ Provider::class.java,
                /* vclass = */ SmartFMap::class.java,
                /* fieldName = */ "filterToCheckersMap",
            )
        }
    }

    private fun createDeclarationCheckers(
        filter: DiagnosticCheckerFilter,
        extensionCheckers: List<CfirAdditionalCheckersExtension>
    ) = createDeclarationCheckers {
        if (filter.runDefaultCheckers) {
            add(CommonDeclarationCheckers)
            add(CommonIdeOnlyDeclarationCheckers)
            addAll(extensionCheckers.map { it.declarationCheckers })
        }

        if (filter.runExtraCheckers) {
            add(ExtraDeclarationCheckers)
        }
    }

    private fun createExpressionCheckers(
        filter: DiagnosticCheckerFilter,
        extensionCheckers: List<CfirAdditionalCheckersExtension>
    ) = createExpressionCheckers {
        if (filter.runDefaultCheckers) {
            add(CommonExpressionCheckers)
            addAll(extensionCheckers.map { it.expressionCheckers })
        }

        if (filter.runExtraCheckers) {
            add(ExtraExpressionCheckers)
        }

        if (filter.runExperimentalCheckers) {
            add(ExperimentalExpressionCheckers)
        }
    }

    private fun createTypeCheckers(
        filter: DiagnosticCheckerFilter,
        extensionCheckers: List<CfirAdditionalCheckersExtension>,
    ) = createTypeCheckers {
        if (filter.runDefaultCheckers) {
            add(CommonTypeCheckers)
            addAll(extensionCheckers.map { it.typeCheckers })
        }

        if (filter.runExperimentalCheckers) {
            add(ExperimentalTypeCheckers)
        }
    }


    private inline fun createDeclarationCheckers(
        createDeclarationCheckers: MutableList<DeclarationCheckers>.() -> Unit
    ): DeclarationCheckers =
        createDeclarationCheckers(buildList(createDeclarationCheckers))


    @OptIn(CheckersComponentInternal::class)
    private fun createDeclarationCheckers(declarationCheckers: List<DeclarationCheckers>): DeclarationCheckers {
        return when (declarationCheckers.size) {
            1 -> declarationCheckers.single()
            else -> ComposedDeclarationCheckers().apply {
                declarationCheckers.forEach(::register)
            }
        }
    }

    private inline fun createExpressionCheckers(
        createExpressionCheckers: MutableList<ExpressionCheckers>.() -> Unit
    ): ExpressionCheckers = createExpressionCheckers(buildList(createExpressionCheckers))

    @OptIn(CheckersComponentInternal::class)
    private fun createExpressionCheckers(expressionCheckers: List<ExpressionCheckers>): ExpressionCheckers {
        return when (expressionCheckers.size) {
            1 -> expressionCheckers.single()
            else -> ComposedExpressionCheckers().apply {
                expressionCheckers.forEach(::register)
            }
        }
    }

    private inline fun createTypeCheckers(
        createTypeCheckers: MutableList<TypeCheckers>.() -> Unit
    ): TypeCheckers = createTypeCheckers(buildList(createTypeCheckers))

    @OptIn(CheckersComponentInternal::class)
    private fun createTypeCheckers(typeCheckers: List<TypeCheckers>): TypeCheckers {
        return when (typeCheckers.size) {
            1 -> typeCheckers.single()
            else -> ComposedTypeCheckers().apply {
                typeCheckers.forEach(::register)
            }
        }
    }
}
