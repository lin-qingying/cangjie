package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.resolve.CfirSuperTypeChecker
import org.cangnova.cangjie.cfir.resolve.CfirTypeRefResolver
import org.cangnova.cangjie.cfir.resolve.diagnostics.CfirResolveRuleCatalog
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.superTypeGraphStoreOrNull

internal class CfirSupertypeResolverProcessor(
    private val diagnosticReporter: CfirDiagnosticReporter,
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.SUPER_TYPES,
) {
    override val transformer: CfirSupertypeResolveTransformer =
        CfirSupertypeResolveTransformer(session, diagnosticReporter)
}


private val RULE_SUPER_TYPES_INTERFACE_CANNOT_INHERIT_CLASS =
    CfirResolveRuleCatalog.SUPER_TYPES_INTERFACE_CANNOT_INHERIT_CLASS
private val RULE_SUPER_TYPES_MULTIPLE_CLASS_SUPER_TYPES =
    CfirResolveRuleCatalog.SUPER_TYPES_MULTIPLE_CLASS_SUPER_TYPES

internal class CfirSupertypeResolveTransformer(
    override val session: CfirSession,
    private val diagnosticReporter: CfirDiagnosticReporter,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.SUPER_TYPES) {
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration.resolvePhase < CfirResolvePhase.IMPORTS || declaration.resolvePhase >= CfirResolvePhase.SUPER_TYPES) {
            return declaration
        }

        if (declaration is CfirClass) {
            val resolver = CfirTypeRefResolver(session)
            val checker = CfirSuperTypeChecker(
                diagnosticReporter = diagnosticReporter,
                resolver = resolver,
            )
            val checkResult = checker.collectAndCheck(declaration)

            session.superTypeGraphStoreOrNull?.record(declaration, checkResult.graphEdges)

            if (declaration.classKind == CfirClassKind.INTERFACE) {
                checkResult.classLikeSupers
                    .filter { it.classKind != CfirClassKind.INTERFACE }
                    .forEach { nonInterfaceSuper ->
                        diagnosticReporter.reportOn(
                            source = declaration.source,
                            factory = CfirErrors.INTERFACE_CANNOT_INHERIT_CLASS,
                            a = RULE_SUPER_TYPES_INTERFACE_CANNOT_INHERIT_CLASS.id,
                            b = "interface '${declaration.name}' cannot inherit non-interface type '${nonInterfaceSuper.name}' (${RULE_SUPER_TYPES_INTERFACE_CANNOT_INHERIT_CLASS.officialReference})",
                            context = DiagnosticContext.Default,
                        )
                    }
            } else {
                val resolvedClassSupers = checkResult.classLikeSupers.filter { it.classKind != CfirClassKind.INTERFACE }
                if (resolvedClassSupers.size > 1) {
                    val classSupers = resolvedClassSupers.joinToString { it.name.asString() }
                    diagnosticReporter.reportOn(
                        source = declaration.source,
                        factory = CfirErrors.MULTIPLE_CLASS_SUPER_TYPES,
                        a = RULE_SUPER_TYPES_MULTIPLE_CLASS_SUPER_TYPES.id,
                        b = "type '${declaration.name}' has multiple class supertypes: $classSupers (${RULE_SUPER_TYPES_MULTIPLE_CLASS_SUPER_TYPES.officialReference})",
                        context = DiagnosticContext.Default,
                    )
                }
            }
        }

        declaration.resolvePhase = CfirResolvePhase.SUPER_TYPES
        return declaration
    }
}
