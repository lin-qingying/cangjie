package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirClassKind
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.diagnostics.CfirResolveRuleCatalog
import org.cangnova.cangjie.cfir.scopes.CfirScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession

private val RULE_EXTEND_DUPLICATE_INTERFACE = CfirResolveRuleCatalog.EXTENSIONS_DUPLICATE_INTERFACE
private val RULE_EXTEND_NOT_INTERFACE = CfirResolveRuleCatalog.EXTENSIONS_NOT_INTERFACE
private val RULE_ILLEGAL_EXTENDED_TYPE = CfirResolveRuleCatalog.EXTENSIONS_ILLEGAL_EXTENDED_TYPE

internal class CfirExtensionsResolveProcessor(
    private val diagnosticReporter: CfirDiagnosticReporter,
    session: CfirSession,
    scopeSession: CfirScopeSession,
) : CfirTransformerBasedResolveProcessor(
    session = session,
    scopeSession = scopeSession,
    phase = CfirResolvePhase.EXTENSIONS,
) {
    override val transformer: CfirExtensionsResolveTransformer =
        CfirExtensionsResolveTransformer(session, diagnosticReporter)
}

internal class CfirExtensionsResolveTransformer(
    override val session: CfirSession,
    private val diagnosticReporter: CfirDiagnosticReporter,
) : CfirAbstractTreeTransformer<Nothing?>(CfirResolvePhase.EXTENSIONS) {
    override fun transformDeclaration(declaration: CfirDeclaration, data: Nothing?): CfirDeclaration {
        if (declaration.resolvePhase < CfirResolvePhase.STATUS || declaration.resolvePhase >= CfirResolvePhase.EXTENSIONS) {
            return declaration
        }

        if (declaration is CfirExtend) {
            val resolver = CfirTypeRefResolver(session)

            if (declaration.extendedTypeRef.isDefinitelyIllegalExtendedType()) {
                diagnosticReporter.reportOn(
                    source = declaration.extendedTypeRef.source,
                    factory = CfirErrors.ILLEGAL_EXTENDED_TYPE,
                    a = RULE_ILLEGAL_EXTENDED_TYPE.id,
                    b = "illegal extended type '${declaration.extendedTypeRef.renderStableKey()}' (${RULE_ILLEGAL_EXTENDED_TYPE.officialReference})",
                    context = DiagnosticContext.Default,
                )
            }

            resolver.resolveClass(declaration.extendedTypeRef)?.let { resolved ->
                if (resolved.classKind == CfirClassKind.INTERFACE) {
                    diagnosticReporter.reportOn(
                        source = declaration.extendedTypeRef.source,
                        factory = CfirErrors.ILLEGAL_EXTENDED_TYPE,
                        a = RULE_ILLEGAL_EXTENDED_TYPE.id,
                        b = "extend target cannot be interface '${resolved.name}' (${RULE_ILLEGAL_EXTENDED_TYPE.officialReference})",
                        context = DiagnosticContext.Default,
                    )
                }
            }

            val seen = linkedSetOf<String>()
            for (superTypeRef in declaration.superTypeRefs) {
                val key = superTypeRef.renderStableKey()
                if (!seen.add(key)) {
                    diagnosticReporter.reportOn(
                        source = superTypeRef.source,
                        factory = CfirErrors.EXTEND_DUPLICATE_INTERFACE,
                        a = RULE_EXTEND_DUPLICATE_INTERFACE.id,
                        b = "duplicate extend interface '$key' (${RULE_EXTEND_DUPLICATE_INTERFACE.officialReference})",
                        context = DiagnosticContext.Default,
                    )
                }

                if (superTypeRef.isDefinitelyNotInterfaceType()) {
                    diagnosticReporter.reportOn(
                        source = superTypeRef.source,
                        factory = CfirErrors.EXTEND_NOT_INTERFACE,
                        a = RULE_EXTEND_NOT_INTERFACE.id,
                        b = "inherited type '$key' in extend declaration is not an interface (${RULE_EXTEND_NOT_INTERFACE.officialReference})",
                        context = DiagnosticContext.Default,
                    )
                    continue
                }

                resolver.resolveClass(superTypeRef)?.let { resolved ->
                    if (resolved.classKind != CfirClassKind.INTERFACE) {
                        diagnosticReporter.reportOn(
                            source = superTypeRef.source,
                            factory = CfirErrors.EXTEND_NOT_INTERFACE,
                            a = RULE_EXTEND_NOT_INTERFACE.id,
                            b = "inherited type '$key' in extend declaration resolves to non-interface '${resolved.name}' (${RULE_EXTEND_NOT_INTERFACE.officialReference})",
                            context = DiagnosticContext.Default,
                        )
                    }
                }
            }
        }

        declaration.resolvePhase = CfirResolvePhase.EXTENSIONS
        return declaration
    }
}
