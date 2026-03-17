package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.replaceResolvePhase
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.resolve.CfirSuperTypeChecker
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.superTypeGraphStoreOrNull
import org.cangnova.cangjie.cfir.session.typeResolver

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

internal class CfirSupertypeResolveTransformer(
    override val session: CfirSession,
    private val diagnosticReporter: CfirDiagnosticReporter,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.SUPER_TYPES) {
    override fun <E : CfirElement> transformElement(element: E, data: Nothing?): E {
        if (element is CfirDeclaration) {
            @Suppress("UNCHECKED_CAST")
            return transformDeclaration(element, data) as E
        }
        return super.transformElement(element, data)
    }

    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration.resolvePhase < CfirResolvePhase.IMPORTS || declaration.resolvePhase >= CfirResolvePhase.SUPER_TYPES) {
            return declaration
        }

        declaration.transformChildren(this, data)

        if (declaration is CfirClass) {
            val checker = CfirSuperTypeChecker(
                diagnosticReporter = diagnosticReporter,
                resolver = session.typeResolver,
            )
            val checkResult = checker.collectAndCheck(declaration)

            session.superTypeGraphStoreOrNull?.record(declaration, checkResult.graphEdges)

            if (declaration.classKind == CfirClassKind.INTERFACE) {
                declaration.superTypeRefs.forEach { superTypeRef ->
                    val resolved = session.typeResolver.resolveClass(superTypeRef) ?: return@forEach
                    if (resolved.classKind != CfirClassKind.INTERFACE) {
                        diagnosticReporter.reportOn(
                            source = superTypeRef.source,
                            factory = CfirErrors.INTERFACE_CANNOT_INHERIT_CLASS,
                            a = declaration.name,
                            b = resolved.name,
                            context = DiagnosticContext.Default,
                        )
                    }
                }
            } else {
                val resolvedClassSupers = checkResult.classLikeSupers.filter { it.classKind != CfirClassKind.INTERFACE }
                if (resolvedClassSupers.size > 1) {
                    val classSupers = resolvedClassSupers.map { it.name }
                    diagnosticReporter.reportOn(
                        source = declaration.superTypeRefs.firstOrNull()?.source,
                        factory = CfirErrors.MULTIPLE_CLASS_SUPER_TYPES,
                        a = declaration.name,
                        b = classSupers,
                        context = DiagnosticContext.Default,
                    )
                }
            }
        }

        declaration.replaceResolvePhase(CfirResolvePhase.SUPER_TYPES)
        return declaration
    }
}

