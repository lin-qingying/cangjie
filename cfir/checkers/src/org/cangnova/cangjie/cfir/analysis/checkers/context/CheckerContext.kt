package org.cangnova.cangjie.cfir.analysis.checkers.context

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.config.LanguageVersionSettings

abstract class CheckerContext : DiagnosticContext {
    abstract val file: CfirFile?
    abstract val session: CfirSession
    abstract val reporter: DiagnosticReporter

    abstract val containingDeclarations: List<CfirDeclaration>
    abstract val containingStatements: List<CfirStatement>
    abstract val containingElements: List<CfirElement>

    abstract val suppressedDiagnostics: Set<String>
    abstract val allInfosSuppressed: Boolean
    abstract val allWarningsSuppressed: Boolean
    abstract val allErrorsSuppressed: Boolean

    override val languageVersionSettings: LanguageVersionSettings
        get() = LanguageVersionSettings.DEFAULT
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
    override val file: CfirFile?,
    override val session: CfirSession,
    override val reporter: DiagnosticReporter,
    override val suppressedDiagnostics: Set<String> = emptySet(),
    override val allInfosSuppressed: Boolean = false,
    override val allWarningsSuppressed: Boolean = false,
    override val allErrorsSuppressed: Boolean = false,
) : CheckerContext() {
    override val containingFileSymbol: CfirFileSymbol?
        get() = file?.symbol as? CfirFileSymbol
    private val mutableDeclarations = mutableListOf<CfirDeclaration>()
    private val mutableStatements = mutableListOf<CfirStatement>()
    private val mutableElements = mutableListOf<CfirElement>()

    override val containingDeclarations: List<CfirDeclaration>
        get() = mutableDeclarations

    override val containingStatements: List<CfirStatement>
        get() = mutableStatements

    override val containingElements: List<CfirElement>
        get() = mutableElements

    fun addDeclaration(declaration: CfirDeclaration) {
        mutableDeclarations += declaration
    }

    fun dropDeclaration() {
        if (mutableDeclarations.isNotEmpty()) {
            mutableDeclarations.removeLast()
        }
    }

    fun addStatement(statement: CfirStatement) {
        mutableStatements += statement
    }

    fun dropStatement() {
        if (mutableStatements.isNotEmpty()) {
            mutableStatements.removeLast()
        }
    }

    fun addElement(element: CfirElement) {
        if (mutableElements.lastOrNull() !== element) {
            mutableElements += element
        }
    }

    fun dropElement() {
        if (mutableElements.isNotEmpty()) {
            mutableElements.removeLast()
        }
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

