@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics

import com.intellij.openapi.diagnostic.thisLogger
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.resolveToCfirSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkCanceled
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.forEachDeclaration
import org.cangnova.cangjie.analysis.utils.printer.parentsOfType
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContextForProvider
import org.cangnova.cangjie.cfir.analysis.collectors.CheckerRunningDiagnosticCollectorVisitor
import org.cangnova.cangjie.cfir.analysis.collectors.DiagnosticCollectorComponents
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.utils.exceptions.shouldIjPlatformExceptionBeRethrown

internal open class LLCfirDiagnosticVisitor(
    context: CheckerContextForProvider,
    components: DiagnosticCollectorComponents,
) : CheckerRunningDiagnosticCollectorVisitor(context, components) {
    private val beforeElementDiagnosticCollectionHandler = context.session.beforeElementDiagnosticCollectionHandler

    override fun visitNestedElements(element: CfirElement) {
        if (element is CfirDeclaration) {
            beforeElementDiagnosticCollectionHandler?.beforeGoingNestedDeclaration(element, context)
        }

        super.visitNestedElements(element)
    }

    override fun checkElement(element: CfirElement) {
        beforeElementDiagnosticCollectionHandler?.beforeCollectingForElement(element)
        components.regularComponents.forEach { diagnosticVisitor ->
            checkCanceled()
            suppressAndLogExceptions {
                element.accept(diagnosticVisitor, context)
            }
        }

        checkCanceled()
        suppressAndLogExceptions {
            element.accept(components.reportCommitter, context)
        }

        suppressAndLogExceptions {
            commitPendingDiagnosticsOnNestedDeclarations(element)
        }
    }

    override fun visitCodeFragment(codeFragment: CfirCodeFragment, data: Nothing?) {
        val cjCodeFragment = codeFragment.psi as CjCodeFragment

        val contextElement = cjCodeFragment.context
        if (contextElement != null) {
            fun process(containingSymbols: List<CfirDeclaration>) {
                if (containingSymbols.isEmpty()) {
                    super.visitCodeFragment(codeFragment, data)
                } else {
                    withDeclaration(containingSymbols.first()) {
                        process(containingSymbols.subList(1, containingSymbols.size))
                    }
                }
            }

            val project = contextElement.project
            val module = CangJieProjectStructureProvider.getModule(project, contextElement, useSiteModule = null)
            val resolutionFacade = module.getResolutionFacade(project)

            // Register containing declarations of a context element
            contextElement.parentsOfType<CjDeclaration>().toList().asReversed()
                .map { declaration: CjDeclaration -> declaration.resolveToCfirSymbol(resolutionFacade).cfir }
                .run(::process)

            return
        }

        super.visitCodeFragment(codeFragment, data)
    }

    /**
     * File and class checkers may report diagnostics on top-level declarations and class members, such as conflicting overload errors.
     * Because we are collecting diagnostics for each structure element separately, this visitor will not visit these nested declarations by
     * default, as the file/class and its nested declarations are different structure elements. Instead, all diagnostics produced during the
     * visitor run will be committed at the end (see [FileStructureElementDiagnosticsCollector.collectForStructureElement]).
     *
     * Skipping nested declarations circumvents error suppression with `@Suppress` on top-level declarations and class members. This is
     * because suppression usually works as such: When a diagnostic is first reported on an element `E`, it is "pending". Once element `E`
     * is visited by the diagnostic visitor, it commits all pending diagnostics for `E`, including those reported by a file/class checker.
     * Diagnostics which are suppressed in the current context are instead removed. Without committing pending diagnostics on each element
     * `E`, suppression cannot take effect.
     *
     * [commitPendingDiagnosticsOnNestedDeclarations] commits pending diagnostics for directly nested elements, allowing the report
     * committer to take suppression into account.
     *
     * It suffices to commit pending diagnostics for directly nested declarations, because checkers can only report diagnostics on directly
     * accessible children. For example, a file checker can report a diagnostic on a top-level class, but not its member function.
     */
    private fun commitPendingDiagnosticsOnNestedDeclarations(element: CfirElement) {
        val declarationContainer = when (element) {
            is CfirFile, is CfirClass, is CfirExtend -> element
            else -> return
        }

        // Casting to `CfirDeclaration` is required in K1.
        @Suppress("USELESS_CAST")
        (declarationContainer as CfirDeclaration).forEachDeclaration { declaration ->
            withAnnotationContainer(declaration) {
                declaration.accept(components.reportCommitter, context)
            }
        }
    }

    companion object {
        /**
         * We don't want to throw exceptions right away to not interrupt other checkers there possible.
         * It is better to report as much as possible and not crash the entire visitor.
         *
         * By default, a logger throws exceptions, but it is up to the user code to provide
         * an alternative handler.
         *
         * For instance, the IntelliJ plugin reports such exceptions and doesn't interrupt the execution flow.
         */
        inline fun <T> suppressAndLogExceptions(block: () -> T): T? = try {
            block()
        } catch (e: Throwable) {
            if (shouldIjPlatformExceptionBeRethrown(e)) {
                throw e
            }

            thisLogger().error("The diagnostic collector has been interrupted by an exception. The result may be incomplete", e)
            null
        }
    }
}
