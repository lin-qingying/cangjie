/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.diagnostics.cfir

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContextForProvider
import org.cangnova.cangjie.cfir.analysis.checkers.context.MutableCheckerContext
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve

internal object PersistentCheckerContextFactory {
    fun createEmptyPersistenceCheckerContext(sessionHolder: SessionAndScopeSessionHolder): CheckerContextForProvider {
        return MutableCheckerContext(
            sessionHolder = sessionHolder,
            returnTypeCalculator = ReturnTypeCalculatorForFullBodyResolve.Default,
            reporter = CfirDiagnosticCollector(),
            containingFileSymbol = null,
        )
    }

    fun createPersistenceCheckerContextSnapshot(
        context: CheckerContextForProvider,
        additionalDeclaration: CfirDeclaration? = null,
    ): CheckerContextForProvider {
        return MutableCheckerContext(
            sessionHolder = context.sessionHolder,
            returnTypeCalculator = context.returnTypeCalculator,
            reporter = context.reporter,
            containingFileSymbol = context.containingFileSymbol,
            mutableDeclarations = context.containingDeclarations.toMutableList().apply {
                additionalDeclaration?.let { declaration ->
                    add(declaration)
                }
            },
            mutableStatements = context.containingStatements.toMutableList(),
            mutableElements = context.containingElements.toMutableList(),
            mutableCallsOrAssignments = context.callsOrAssignments.toMutableList(),
            mutableAnnotationContainers = context.annotationContainers.toMutableList(),
            suppressedDiagnostics = context.suppressedDiagnostics,
            allInfosSuppressed = context.allInfosSuppressed,
            allWarningsSuppressed = context.allWarningsSuppressed,
            allErrorsSuppressed = context.allErrorsSuppressed,
        )
    }
}
