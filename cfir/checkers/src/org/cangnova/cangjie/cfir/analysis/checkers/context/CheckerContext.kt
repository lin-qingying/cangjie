package org.cangnova.cangjie.cfir.analysis.checkers.context

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator

abstract class CheckerContext : DiagnosticContext, SessionAndScopeSessionHolder {
    abstract val reporter: DiagnosticReporter
    abstract val sessionHolder: SessionAndScopeSessionHolder
    abstract val returnTypeCalculator: ReturnTypeCalculator

    abstract val containingDeclarations: List<CfirDeclaration>
    abstract val containingStatements: List<CfirStatement>
    abstract val containingElements: List<CfirElement>
    abstract val callsOrAssignments: List<CfirElement>
    abstract val annotationContainers: List<CfirAnnotationContainer>

    abstract val suppressedDiagnostics: Set<String>
    abstract val allInfosSuppressed: Boolean
    abstract val allWarningsSuppressed: Boolean
    abstract val allErrorsSuppressed: Boolean

    override val session
        get() = sessionHolder.session

    override val scopeSession
        get() = sessionHolder.scopeSession

    override val languageVersionSettings: LanguageVersionSettings
        get() = session.languageVersionSettings
    abstract val containingFileSymbol: CfirFileSymbol?

    override val containingFilePath: String?
        get() = containingFileSymbol?.sourceFile?.path

    override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean {
        val suppressedByAll = when (diagnostic.severity) {
            Severity.INFO -> allInfosSuppressed
            Severity.WARNING, Severity.STRONG_WARNING, Severity.FIXED_WARNING -> allWarningsSuppressed
            Severity.ERROR -> allErrorsSuppressed
        }
        return suppressedByAll || diagnostic.factoryName in suppressedDiagnostics
    }
}

class MutableCheckerContext(
    override val sessionHolder: SessionAndScopeSessionHolder,
    override val returnTypeCalculator: ReturnTypeCalculator,
    override val reporter: DiagnosticReporter,
    override var containingFileSymbol: CfirFileSymbol?,
    private val mutableDeclarations: MutableList<CfirDeclaration> = mutableListOf(),
    private val mutableStatements: MutableList<CfirStatement> = mutableListOf(),
    private val mutableElements: MutableList<CfirElement> = mutableListOf(),
    private val mutableCallsOrAssignments: MutableList<CfirElement> = mutableListOf(),
    private val mutableAnnotationContainers: MutableList<CfirAnnotationContainer> = mutableListOf(),
    override val suppressedDiagnostics: Set<String> = emptySet(),
    override val allInfosSuppressed: Boolean = false,
    override val allWarningsSuppressed: Boolean = false,
    override val allErrorsSuppressed: Boolean = false,


) : CheckerContextForProvider(
    sessionHolder = sessionHolder,
    returnTypeCalculator = returnTypeCalculator,
    allInfosSuppressed = allInfosSuppressed,
    allWarningsSuppressed = allWarningsSuppressed,
    allErrorsSuppressed = allErrorsSuppressed,
) {
    override val containingDeclarations: List<CfirDeclaration>
        get() = mutableDeclarations

    override val containingStatements: List<CfirStatement>
        get() = mutableStatements

    override val containingElements: List<CfirElement>
        get() = mutableElements
    override val callsOrAssignments: List<CfirElement>
        get() = mutableCallsOrAssignments
    override val annotationContainers: List<CfirAnnotationContainer>
        get() = mutableAnnotationContainers

    override fun addSuppressedDiagnostics(
        diagnosticNames: Collection<String>,
        allInfosSuppressed: Boolean,
        allWarningsSuppressed: Boolean,
        allErrorsSuppressed: Boolean,
    ): CheckerContextForProvider {
        if (diagnosticNames.isEmpty()) return this
        return MutableCheckerContext(
            sessionHolder = sessionHolder,
            returnTypeCalculator = returnTypeCalculator,
            containingFileSymbol = containingFileSymbol,
            reporter = reporter,
            mutableDeclarations = mutableDeclarations,
            mutableStatements = mutableStatements,
            mutableElements = mutableElements,
            mutableCallsOrAssignments = mutableCallsOrAssignments,
            mutableAnnotationContainers = mutableAnnotationContainers,
            suppressedDiagnostics = suppressedDiagnostics + diagnosticNames,
            allInfosSuppressed = this.allInfosSuppressed || allInfosSuppressed,
            allWarningsSuppressed = this.allWarningsSuppressed || allWarningsSuppressed,
            allErrorsSuppressed = this.allErrorsSuppressed || allErrorsSuppressed,
        )
    }

    override fun addDeclaration(declaration: CfirDeclaration): CheckerContextForProvider {
        mutableDeclarations += declaration
        return this
    }

    override fun dropDeclaration() {
        if (mutableDeclarations.isNotEmpty()) {
            mutableDeclarations.removeLast()
        }
    }

    override fun addStatement(statement: CfirStatement): CheckerContextForProvider {
        mutableStatements += statement
        return this
    }

    override fun dropStatement() {
        if (mutableStatements.isNotEmpty()) {
            mutableStatements.removeLast()
        }
    }

    override fun addCallOrAssignment(qualifiedAccessOrAnnotationCall: CfirStatement): CheckerContextForProvider {
        mutableCallsOrAssignments += qualifiedAccessOrAnnotationCall
        return this
    }

    override fun dropCallOrAssignment() {
        if (mutableCallsOrAssignments.isNotEmpty()) {
            mutableCallsOrAssignments.removeLast()
        }
    }

    override fun addAnnotationContainer(annotationContainer: CfirAnnotationContainer): CheckerContextForProvider {
        mutableAnnotationContainers += annotationContainer
        return this
    }

    override fun dropAnnotationContainer() {
        if (mutableAnnotationContainers.isNotEmpty()) {
            mutableAnnotationContainers.removeLast()
        }
    }

    override fun enterContractBody(): CheckerContextForProvider = this

    override fun exitContractBody(): CheckerContextForProvider = this

    override fun enterFile(file: CfirFile): CheckerContextForProvider {
        containingFileSymbol = file.symbol
        return this
    }

    override fun exitFile(file: CfirFile): CheckerContextForProvider {
        containingFileSymbol = file.symbol
        return this
    }
    override fun addElement(element: CfirElement): CheckerContextForProvider {
        if (mutableElements.lastOrNull() !== element) {
            mutableElements += element
            if (element.isCallOrAssignmentCandidate()) {
                mutableCallsOrAssignments += element
            }
        }
        return this
    }

    override fun dropElement() {
        if (mutableElements.isNotEmpty()) {
            val removed = mutableElements.removeLast()
            if (removed.isCallOrAssignmentCandidate()) {
                mutableCallsOrAssignments.removeLast()
            }
        }
    }

    private fun CfirElement.isCallOrAssignmentCandidate(): Boolean {
        return this is CfirFunctionCall ||
                this is CfirNamedAccessExpression ||
                this is CfirQualifiedAccessExpression ||
                this is CfirAssignment
    }
}

inline fun <reified T : CfirDeclaration> CheckerContext.findClosestDeclaration(noinline check: (T) -> Boolean = { true }): T? {
    for (declaration in containingDeclarations.asReversed()) {
        val typed = declaration as? T ?: continue
        if (check(typed)) {
            return typed
        }
    }
    return null
}
