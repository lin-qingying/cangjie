package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.SourceElementPositioningStrategies
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.descriptors.Visibilities

/**
 * Alignment target: Kotlin FIR `FirNotImplementedOverrideChecker` core behavior.
 *
 * For non-abstract class/struct declarations, report when inherited abstract members
 * remain without concrete implementation.
 */
object CfirNotImplementedOverrideChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration !is CfirClass && declaration !is CfirStruct) return
        if (declaration.status.isAbstract || declaration.status.isSealed) return

        val classScope = context.createUseSiteMemberScope(declaration)
        if (!classScope.hasUnimplementedAbstractMember(declaration, context)) return

        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.ABSTRACT_MEMBER_NOT_IMPLEMENTED,
            a = declaration.name,
            positioningStrategy = SourceElementPositioningStrategies.DECLARATION_START_TO_NAME,
        )
    }
}

private fun CfirTypeScope.hasUnimplementedAbstractMember(
    ownerDeclaration: CfirClassLikeDeclaration,
    context: CheckerContext,
): Boolean {
    for (name in getCallableNames()) {
        val functionSymbols = mutableListOf<CfirFunctionSymbol<*>>()
        processFunctionsByName(name) { functionSymbols += it }
        if (ownerDeclaration.name.asString() == "B") {
            System.err.println(
                buildString {
                    appendLine("DEBUG not-implemented owner=${ownerDeclaration.name} function=$name")
                    functionSymbols.forEach { symbol ->
                        append("  symbol=").append(symbol)
                        append(" sig=").append(symbol.stableSignatureKey())
                        append(" bound=").append(symbol.isBound)
                        append(" visible=").append(symbol.isVisibleIn(ownerDeclaration, context))
                        append(" abstract=").append(symbol.isAbstractLike(context))
                        appendLine()
                    }
                }
            )
        }
        if (functionSymbols.hasUnimplementedAbstractBySignature(ownerDeclaration, context)) {
            return true
        }

        val propertySymbols = mutableListOf<CfirPropertySymbol>()
        processPropertiesByName(name) { propertySymbols += it }
        if (propertySymbols.hasUnimplementedAbstractBySignature(ownerDeclaration, context)) {
            return true
        }
    }
    return false
}

private fun <S : CfirCallableSymbol<*>> List<S>.hasUnimplementedAbstractBySignature(
    ownerDeclaration: CfirClassLikeDeclaration,
    context: CheckerContext,
): Boolean {
    if (isEmpty()) return false

    val visibleGroups = this
        .asSequence()
        .filter { it.isBound }
        .filter { it.isVisibleIn(ownerDeclaration, context) }
        .groupBy { it.stableSignatureKey() }

    for ((_, symbols) in visibleGroups) {
        val abstractSymbols = symbols.filter { it.isAbstractLike(context) }
        if (abstractSymbols.isEmpty()) continue

        for (abstractSymbol in abstractSymbols) {
            val hasConcreteImplementation = symbols.any { candidate ->
                candidate !== abstractSymbol &&
                    !candidate.isAbstractLike(context) &&
                    candidate.canImplementAbstractMember(abstractSymbol)
            }
            if (!hasConcreteImplementation) {
                return true
            }
        }
    }

    return false
}

private fun CfirCallableSymbol<*>.canImplementAbstractMember(abstractSymbol: CfirCallableSymbol<*>): Boolean {
    val compareResult = Visibilities.compare(cfir.status.visibility, abstractSymbol.cfir.status.visibility)
    return compareResult != null && compareResult >= 0
}
