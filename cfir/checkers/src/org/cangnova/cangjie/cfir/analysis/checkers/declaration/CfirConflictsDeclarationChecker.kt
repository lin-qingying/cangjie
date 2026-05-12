package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory1
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.patterns.bindingOccurrences
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.utils.SmartSet

interface CfirPlatformConflictDeclarationsDiagnosticDispatcher : CfirSessionComponent {
    context(context: CheckerContext)
    fun getDiagnostic(
        conflictingDeclaration: CfirBasedSymbol<*>,
        symbols: SmartSet<CfirBasedSymbol<*>>,
    ): CjDiagnosticFactory1<Collection<String>>?

    object DEFAULT : CfirPlatformConflictDeclarationsDiagnosticDispatcher {
        context(context: CheckerContext)
        override fun getDiagnostic(
            conflictingDeclaration: CfirBasedSymbol<*>,
            symbols: SmartSet<CfirBasedSymbol<*>>,
        ): CjDiagnosticFactory1<Collection<String>> {
            return when (conflictingDeclaration) {
                is CfirConstructorSymbol,
                is CfirFunctionSymbol<*>,

                is CfirEnumConstructorSymbol,
                -> if (symbols.any { it.isFunctionLikeRedeclaration() }) {
                    CfirErrors.CONFLICTING_OVERLOADS
                } else {
                    CfirErrors.REDECLARATION
                }

                is CfirClassLikeSymbol<*>
                    if symbols.any { it is CfirClassLikeSymbol<*> } ->
                    CfirErrors.CLASSIFIER_REDECLARATION

                else -> CfirErrors.REDECLARATION
            }
        }
    }
}

val CfirSession.conflictDeclarationsDiagnosticDispatcher: CfirPlatformConflictDeclarationsDiagnosticDispatcher?
    by CfirSession.nullableSessionComponentAccessor()

object CfirConflictsDeclarationChecker : CfirBasicDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        when (declaration) {
            is CfirFile -> {
                val inspector = CfirDeclarationCollector<CfirBasedSymbol<*>>(context)
                checkFile(declaration, inspector)
                reportConflicts(inspector.declarationConflictingSymbols, declaration)
            }

            is CfirClassLikeDeclaration -> {
                if (declaration.source?.kind !is CjFakeSourceElementKind) {
                    checkForLocalRedeclarations(declaration.typeParameters)
                }

                val inspector = CfirDeclarationCollector<CfirBasedSymbol<*>>(context)
                inspector.collectClassMembers(declaration)
                reportConflicts(inspector.declarationConflictingSymbols, declaration)
            }

            else -> {
                if (declaration.source?.kind !is CjFakeSourceElementKind && declaration is CfirTypeParameterRefsOwner) {
                    checkForLocalRedeclarations(declaration.typeParameters)
                }

                if (declaration is CfirFunction) {
                    checkForLocalRedeclarations(declaration.valueParameters)
                }

                if (declaration is CfirProperty) {
                    declaration.setter?.let { setter ->
                        checkForLocalRedeclarations(setter.valueParameters)
                    }
                }
            }
        }
    }

    context(reporter: DiagnosticReporter, context: CheckerContext)
    private fun reportConflicts(
        declarationConflictingSymbols: Map<CfirBasedSymbol<*>, SmartSet<CfirBasedSymbol<*>>>,
        container: CfirDeclaration,
    ) {
        declarationConflictingSymbols.forEach { (conflictingDeclaration, symbols) ->
            val source = when {
                conflictingDeclaration !is CfirCallableSymbol<*> -> conflictingDeclaration.boundSourceOrNull()
                !conflictingDeclaration.isBound -> container.source
                conflictingDeclaration.origin == CfirDeclarationOrigin.Source -> conflictingDeclaration.boundSourceOrNull()
                conflictingDeclaration.origin == CfirDeclarationOrigin.Library -> return@forEach
                else -> container.source
            }

            if (
                symbols.isEmpty() ||
                (conflictingDeclaration as? CfirConstructorSymbol)?.cfir?.isPrimary == true &&
                symbols.all { (it as? CfirConstructorSymbol)?.cfir?.isPrimary == true }
            ) {
                return@forEach
            }

            val dispatcher = context.session.conflictDeclarationsDiagnosticDispatcher
                ?: CfirPlatformConflictDeclarationsDiagnosticDispatcher.DEFAULT
            val factory = dispatcher.getDiagnostic(conflictingDeclaration, symbols) ?: return@forEach
            val renderedNames = symbols.renderNames()
            val patternVariable = (conflictingDeclaration as? CfirCallableSymbol<*>)?.cfir as? CfirPatternVariable
            if (patternVariable != null) {
                val conflictingNameSet = renderedNames.toSet()
                val bindingSources = patternVariable.pattern.bindingOccurrences()
                    .asSequence()
                    .filter { it.name.asString() in conflictingNameSet }
                    .mapNotNull { it.source }
                    .distinct()
                    .toList()
                if (bindingSources.isNotEmpty()) {
                    bindingSources.forEach { bindingSource ->
                        reporter.reportOn(bindingSource, factory, renderedNames)
                    }
                    return@forEach
                }
            }

            source ?: return@forEach
            reporter.reportOn(source, factory, renderedNames)
        }
    }

    context(context: CheckerContext)
    private fun checkFile(file: CfirFile, inspector: CfirDeclarationCollector<CfirBasedSymbol<*>>) {
        val packageMemberScope = context.session.cangjieScopeProvider.getPackageMemberScope(
            packageFqName = file.packageDirective.packageFqName,
            symbolProvider = context.session.symbolProvider,
            useSiteSession = context.session,
            scopeSession = context.scopeSession,
        )
        inspector.collectTopLevel(file, packageMemberScope)
    }
}

private fun CfirBasedSymbol<*>.boundSourceOrNull(): CjSourceElement? =
    if (isBound) cfir.source else null

private fun Collection<CfirBasedSymbol<*>>.renderNames(): List<String> =
    asSequence().mapNotNull(CfirRedeclarationPresenter::diagnosticName).distinct().sorted().toList()

private fun CfirBasedSymbol<*>.isFunctionLikeRedeclaration(): Boolean =
    this is CfirConstructorSymbol || this is CfirFunctionSymbol<*> || this is CfirEnumConstructorSymbol
