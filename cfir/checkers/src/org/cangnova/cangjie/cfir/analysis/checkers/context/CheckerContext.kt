package org.cangnova.cangjie.cfir.analysis.checkers.context

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirPropertyAccess
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccess
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

    private val mutableDeclarations = mutableListOf<CfirDeclaration>()
    private val mutableStatements = mutableListOf<CfirStatement>()
    private val mutableElements = mutableListOf<CfirElement>()
    private val mutableCallsOrAssignments = mutableListOf<CfirElement>()

    override val containingDeclarations: List<CfirDeclaration>
        get() = mutableDeclarations

    override val containingStatements: List<CfirStatement>
        get() = mutableStatements

    override val containingElements: List<CfirElement>
        get() = mutableElements
    override val callsOrAssignments: List<CfirElement>
        get() = mutableCallsOrAssignments

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

    override fun addAnnotationContainer(annotationContainer: org.cangnova.cangjie.cfir.CfirAnnotationContainer): CheckerContextForProvider = this

    override fun dropAnnotationContainer() {}

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
                this is CfirPropertyAccess ||
                this is CfirQualifiedAccess ||
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
