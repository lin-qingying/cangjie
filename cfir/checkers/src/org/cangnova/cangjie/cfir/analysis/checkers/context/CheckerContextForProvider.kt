package org.cangnova.cangjie.cfir.analysis.checkers.context

import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator

/**
 * This class should be invisible for checkers, but should be only used by DiagnosticCollectorVisitor who runs those checkers
 * It contains all context-modification-related methods unlike CheckerContext that is assumed to be read-only
 */
abstract class CheckerContextForProvider(
    override val sessionHolder: SessionAndScopeSessionHolder,
    override val returnTypeCalculator: ReturnTypeCalculator,
    override val allInfosSuppressed: Boolean,
    override val allWarningsSuppressed: Boolean,
    override val allErrorsSuppressed: Boolean
) : CheckerContext() {
    abstract fun addSuppressedDiagnostics(
        diagnosticNames: Collection<String>,
        allInfosSuppressed: Boolean,
        allWarningsSuppressed: Boolean,
        allErrorsSuppressed: Boolean
    ): CheckerContextForProvider

    abstract fun addDeclaration(declaration: CfirDeclaration): CheckerContextForProvider

    abstract fun dropDeclaration()

    abstract fun addStatement(statement: CfirStatement): CheckerContextForProvider

    abstract fun dropStatement()

    abstract fun addCallOrAssignment(qualifiedAccessOrAnnotationCall: CfirStatement): CheckerContextForProvider

    abstract fun dropCallOrAssignment()

    abstract fun addAnnotationContainer(annotationContainer: CfirAnnotationContainer): CheckerContextForProvider

    abstract fun dropAnnotationContainer()

    abstract fun enterContractBody(): CheckerContextForProvider

    abstract fun exitContractBody(): CheckerContextForProvider



    abstract fun enterFile(file: CfirFile): CheckerContextForProvider

    abstract fun exitFile(file: CfirFile): CheckerContextForProvider

    abstract fun addElement(element: CfirElement): CheckerContextForProvider

    abstract fun dropElement()
}
