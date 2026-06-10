package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRefsOwner
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory1
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirForInExpression
import org.cangnova.cangjie.cfir.patterns.bindingVariables
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
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.name.Name
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
                    checkLocalRedeclarationsInFunctionBody(declaration)
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
            if (conflictingDeclaration.hasLaterFunctionConflictRepresentative(declarationConflictingSymbols)) {
                return@forEach
            }

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

context(reporter: DiagnosticReporter, context: CheckerContext)
private fun checkLocalRedeclarationsInFunctionBody(function: CfirFunction) {
    val body = function.body ?: return
    val visitor = LocalRedeclarationVisitor(reporter, context)
    visitor.withFunctionBodyScope(function) {
        body.statements.forEach { it.accept(visitor) }
    }
}

private fun CfirBasedSymbol<*>.boundSourceOrNull(): CjSourceElement? =
    if (isBound) cfir.source else null

/**
 * 仓颉官方编译器对同签名函数冲突只选择冲突簇中的一个声明承载
 * `sema_overload_conflicts`，其他声明作为 note。这里保留冲突图用于渲染，
 * 但避免把同一函数签名簇中的较早声明也报告为独立诊断。
 */
private fun CfirBasedSymbol<*>.hasLaterFunctionConflictRepresentative(
    declarationConflictingSymbols: Map<CfirBasedSymbol<*>, SmartSet<CfirBasedSymbol<*>>>,
): Boolean {
    if (!isFunctionLikeRedeclaration()) return false

    val sourceOffset = boundSourceOrNull()?.startOffset ?: return false
    val presentation = CfirRedeclarationPresenter.represent(this) ?: return false

    return declarationConflictingSymbols.any { (otherDeclaration, otherConflicts) ->
        otherDeclaration != this &&
            otherDeclaration.isFunctionLikeRedeclaration() &&
            otherConflicts.contains(this) &&
            otherDeclaration.boundSourceOrNull()?.startOffset?.let { it > sourceOffset } == true &&
            CfirRedeclarationPresenter.represent(otherDeclaration) == presentation
    }
}

private fun Collection<CfirBasedSymbol<*>>.renderNames(): List<String> =
    asSequence().mapNotNull(CfirRedeclarationPresenter::diagnosticName).distinct().sorted().toList()

private fun CfirBasedSymbol<*>.isFunctionLikeRedeclaration(): Boolean =
    this is CfirConstructorSymbol || this is CfirFunctionSymbol<*> || this is CfirEnumConstructorSymbol

private class LocalRedeclarationVisitor(
    private val reporter: DiagnosticReporter,
    private val context: CheckerContext,
) : CfirDefaultVisitorVoid() {
    private val scopes = ArrayDeque<MutableMap<Name, MutableList<CfirBasedSymbol<*>>>>()

    fun withFunctionBodyScope(function: CfirFunction, body: () -> Unit) {
        withScope {
            function.valueParameters.forEach { declare(it, report = false) }
            body()
        }
    }

    override fun visitElement(element: CfirElement) {
        element.acceptChildren(this)
    }

    override fun visitBlock(block: CfirBlock) {
        withScope {
            block.statements.forEach { it.accept(this) }
        }
    }

    override fun visitForInExpression(forInExpression: CfirForInExpression) {
        forInExpression.iterable.accept(this)
        withScope {
            declarePatternVariable(forInExpression.variable)
            forInExpression.body.statements.forEach { it.accept(this) }
        }
    }

    override fun visitFunction(function: CfirFunction) {
        if (function.isLocal) {
            declare(function.symbol)
        }
    }

    override fun visitProperty(property: CfirProperty) {
        if (property.isLocal) {
            declare(property.symbol)
        }
    }

    override fun visitVariable(variable: CfirVariable) {
        variable.initializer?.accept(this)
        if (variable is CfirPatternVariable) {
            declarePatternVariable(variable)
        } else {
            declare(variable)
        }
    }

    private fun declare(variable: CfirVariable, report: Boolean = true) {
        declare(variable.symbol, report)
    }

    private fun declarePatternVariable(variable: CfirPatternVariable, report: Boolean = true) {
        variable.pattern.bindingVariables().forEach { bindingVariable ->
            declare(bindingVariable.symbol, report)
        }
    }

    private fun declare(symbol: CfirBasedSymbol<*>, report: Boolean = true) {
        val name = CfirRedeclarationPresenter.diagnosticName(symbol)?.let(Name::identifier) ?: return
        if (name.isSpecial) return

        val currentScope = scopes.lastOrNull() ?: return
        val declarations = currentScope.getOrPut(name, ::mutableListOf)
        val previousConflicts = declarations.filter { symbol.conflictsWithLocalDeclaration(it) }
        declarations += symbol
        if (previousConflicts.isNotEmpty() && report) {
            val source = symbol.boundSourceOrNull() ?: return
            val conflicts = previousConflicts + symbol
            val factory = if (symbol.isFunctionLikeRedeclaration() && previousConflicts.any { it.isFunctionLikeRedeclaration() }) {
                CfirErrors.CONFLICTING_OVERLOADS
            } else {
                CfirErrors.REDECLARATION
            }
            reporter.reportOn(source, factory, conflicts.renderNames(), context)
        }
    }

    private fun CfirBasedSymbol<*>.conflictsWithLocalDeclaration(other: CfirBasedSymbol<*>): Boolean {
        if (this is CfirFunctionSymbol<*> && other is CfirFunctionSymbol<*>) {
            return CfirRedeclarationPresenter.represent(this) == CfirRedeclarationPresenter.represent(other)
        }
        return true
    }

    private inline fun withScope(body: () -> Unit) {
        scopes.addLast(linkedMapOf())
        try {
            body()
        } finally {
            scopes.removeLast()
        }
    }
}
