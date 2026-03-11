package org.cangjie.cfir.resolve

import org.cangjie.cfir.declarations.CfirClass
import org.cangjie.cfir.declarations.CfirClassKind
import org.cangjie.cfir.declarations.CfirDeclaration
import org.cangjie.cfir.declarations.CfirExtend
import org.cangjie.cfir.declarations.CfirInvalidDeclaration
import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.diagnostics.CfirDiagnosticFactory
import org.cangjie.cfir.diagnostics.CfirDiagnosticReporter
import org.cangjie.cfir.diagnostics.CfirDiagnosticSeverity
import org.cangjie.cfir.resolve.diagnostics.CfirResolveRuleCatalog
import org.cangjie.cfir.resolve.diagnostics.CfirResolveRuleDefinition
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.session.cfirProvider
import org.cangjie.cfir.types.CfirBasicTypeRef
import org.cangjie.cfir.types.CfirErrorTypeRef
import org.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

private const val SYNTHETIC_INVALID_DECLARATION_PREFIX = "<synthetic>"

private val INVALID_DECLARATION_DIAGNOSTIC = CfirDiagnosticFactory(
    name = "CFIR_INVALID_DECLARATION",
    defaultSeverity = CfirDiagnosticSeverity.ERROR,
)

private val SUPER_TYPES_DUPLICATE_DIAGNOSTIC = CfirDiagnosticFactory(
    name = "CFIR_SUPER_TYPES_DUPLICATE",
    defaultSeverity = CfirDiagnosticSeverity.ERROR,
)

private val SUPER_TYPES_SELF_REFERENCE_DIAGNOSTIC = CfirDiagnosticFactory(
    name = "CFIR_SUPER_TYPES_SELF_REFERENCE",
    defaultSeverity = CfirDiagnosticSeverity.ERROR,
)

private val INTERFACE_CANNOT_INHERIT_CLASS_DIAGNOSTIC = CfirDiagnosticFactory(
    name = "CFIR_INTERFACE_CANNOT_INHERIT_CLASS",
    defaultSeverity = CfirDiagnosticSeverity.ERROR,
)

private val MULTIPLE_CLASS_SUPER_TYPES_DIAGNOSTIC = CfirDiagnosticFactory(
    name = "CFIR_MULTIPLE_CLASS_SUPER_TYPES",
    defaultSeverity = CfirDiagnosticSeverity.ERROR,
)

private val EXTEND_DUPLICATE_INTERFACE_DIAGNOSTIC = CfirDiagnosticFactory(
    name = "CFIR_EXTEND_DUPLICATE_INTERFACE",
    defaultSeverity = CfirDiagnosticSeverity.ERROR,
)

private val EXTEND_NOT_INTERFACE_DIAGNOSTIC = CfirDiagnosticFactory(
    name = "CFIR_EXTEND_NOT_INTERFACE",
    defaultSeverity = CfirDiagnosticSeverity.ERROR,
)

private val ILLEGAL_EXTENDED_TYPE_DIAGNOSTIC = CfirDiagnosticFactory(
    name = "CFIR_ILLEGAL_EXTENDED_TYPE",
    defaultSeverity = CfirDiagnosticSeverity.ERROR,
)

private val RULE_SUPER_TYPES_DUPLICATE: CfirResolveRuleDefinition =
    CfirResolveRuleCatalog.SUPER_TYPES_DUPLICATE_INTERFACE
private val RULE_EXTEND_DUPLICATE_INTERFACE: CfirResolveRuleDefinition =
    CfirResolveRuleCatalog.EXTENSIONS_DUPLICATE_INTERFACE
private val RULE_EXTEND_NOT_INTERFACE: CfirResolveRuleDefinition =
    CfirResolveRuleCatalog.EXTENSIONS_NOT_INTERFACE
private val RULE_ILLEGAL_EXTENDED_TYPE: CfirResolveRuleDefinition =
    CfirResolveRuleCatalog.EXTENSIONS_ILLEGAL_EXTENDED_TYPE

/**
 * Register CFIR resolve processors.
 *
 * The framework follows FIR phase-pipeline style, while semantic rules and diagnostics
 * are aligned to the official Cangjie compiler references listed in [CfirRuleReference].
 */
fun registerResolveProcessors(
    registry: CfirPhaseResolverRegistry,
    diagnosticReporter: CfirDiagnosticReporter,
) {
    registry.registerProcessor(CfirResolvePhase.IMPORTS, CfirMinimalImportsResolveProcessor())
    registry.registerProcessor(CfirResolvePhase.SUPER_TYPES, CfirSuperTypesResolveProcessor(diagnosticReporter))
    registry.registerProcessor(CfirResolvePhase.TYPES, CfirMinimalTypesResolveProcessor())
    registry.registerProcessor(CfirResolvePhase.STATUS, CfirMinimalStatusResolveProcessor())
    registry.registerProcessor(CfirResolvePhase.EXTENSIONS, CfirExtensionsResolveProcessor(diagnosticReporter))
    registry.registerProcessor(CfirResolvePhase.IMPLICIT_TYPES, CfirMinimalImplicitTypesResolveProcessor())
    registry.registerProcessor(CfirResolvePhase.BODY_RESOLVE, CfirMinimalBodyResolveProcessor())
    registry.registerProcessor(CfirResolvePhase.CHECKERS, CfirMinimalCheckersResolveProcessor(diagnosticReporter))
}

@Deprecated(
    message = "Use registerResolveProcessors for the formal CFIR_RESOLVE pipeline.",
    replaceWith = ReplaceWith("registerResolveProcessors(registry, diagnosticReporter)"),
)
fun registerMinimalResolveProcessors(
    registry: CfirPhaseResolverRegistry,
    diagnosticReporter: CfirDiagnosticReporter,
) {
    registerResolveProcessors(registry, diagnosticReporter)
}

private abstract class CfirMinimalPhaseResolveProcessor(
    final override val fromPhase: CfirResolvePhase,
    final override val toPhase: CfirResolvePhase,
) : CfirResolveProcessor {
    final override fun process(target: CfirDeclaration, session: CfirSession) {
        if (target.resolvePhase < fromPhase || target.resolvePhase >= toPhase) return
        doProcess(target, session)
        target.resolvePhase = toPhase
    }

    protected open fun doProcess(target: CfirDeclaration, session: CfirSession) {}
}

private class CfirMinimalImportsResolveProcessor : CfirMinimalPhaseResolveProcessor(
    fromPhase = CfirResolvePhase.RAW_CFIR,
    toPhase = CfirResolvePhase.IMPORTS,
)

private class CfirMinimalTypesResolveProcessor : CfirMinimalPhaseResolveProcessor(
    fromPhase = CfirResolvePhase.SUPER_TYPES,
    toPhase = CfirResolvePhase.TYPES,
)

private class CfirMinimalStatusResolveProcessor : CfirMinimalPhaseResolveProcessor(
    fromPhase = CfirResolvePhase.TYPES,
    toPhase = CfirResolvePhase.STATUS,
)

private class CfirMinimalImplicitTypesResolveProcessor : CfirMinimalPhaseResolveProcessor(
    fromPhase = CfirResolvePhase.EXTENSIONS,
    toPhase = CfirResolvePhase.IMPLICIT_TYPES,
)

private class CfirMinimalBodyResolveProcessor : CfirMinimalPhaseResolveProcessor(
    fromPhase = CfirResolvePhase.IMPLICIT_TYPES,
    toPhase = CfirResolvePhase.BODY_RESOLVE,
)

private class CfirMinimalCheckersResolveProcessor(
    private val diagnosticReporter: CfirDiagnosticReporter,
) : CfirMinimalPhaseResolveProcessor(
    fromPhase = CfirResolvePhase.BODY_RESOLVE,
    toPhase = CfirResolvePhase.CHECKERS,
) {
    override fun doProcess(target: CfirDeclaration, session: CfirSession) {
        if (target is CfirInvalidDeclaration && !target.reason.startsWith(SYNTHETIC_INVALID_DECLARATION_PREFIX)) {
            diagnosticReporter.report(
                INVALID_DECLARATION_DIAGNOSTIC.on(
                    source = target.source,
                    message = target.reason,
                ),
            )
        }
    }
}

private class CfirSuperTypesResolveProcessor(
    private val diagnosticReporter: CfirDiagnosticReporter,
) : CfirMinimalPhaseResolveProcessor(
    fromPhase = CfirResolvePhase.IMPORTS,
    toPhase = CfirResolvePhase.SUPER_TYPES,
) {
    override fun doProcess(target: CfirDeclaration, session: CfirSession) {
        if (target !is CfirClass) return
        val resolver = CfirTypeRefResolver(session)
        val seen = linkedSetOf<String>()
        val classLikeSupers = mutableListOf<CfirClass>()

        for (superTypeRef in target.superTypeRefs) {
            val key = superTypeRef.renderStableKey()
            if (!seen.add(key)) {
                diagnosticReporter.report(
                    SUPER_TYPES_DUPLICATE_DIAGNOSTIC.on(
                        source = superTypeRef.source,
                        message = "[${RULE_SUPER_TYPES_DUPLICATE.id}] duplicate super type '$key' for type '${target.name}' (${RULE_SUPER_TYPES_DUPLICATE.officialReference})",
                    ),
                )
            }
            if (key == target.name.asString()) {
                diagnosticReporter.report(
                    SUPER_TYPES_SELF_REFERENCE_DIAGNOSTIC.on(
                        source = superTypeRef.source,
                        message = "type '${target.name}' cannot inherit from itself",
                    ),
                )
            }
            resolver.resolveClass(superTypeRef)?.let { classLikeSupers += it }
        }

        if (target.classKind == CfirClassKind.INTERFACE) {
            classLikeSupers
                .filter { it.classKind != CfirClassKind.INTERFACE }
                .forEach { nonInterfaceSuper ->
                    diagnosticReporter.report(
                        INTERFACE_CANNOT_INHERIT_CLASS_DIAGNOSTIC.on(
                            source = target.source,
                            message = "interface '${target.name}' cannot inherit non-interface type '${nonInterfaceSuper.name}'",
                        ),
                    )
                }
            return
        }

        val resolvedClassSupers = classLikeSupers.filter { it.classKind != CfirClassKind.INTERFACE }
        if (resolvedClassSupers.size > 1) {
            val classSupers = resolvedClassSupers.joinToString { it.name.asString() }
            diagnosticReporter.report(
                MULTIPLE_CLASS_SUPER_TYPES_DIAGNOSTIC.on(
                    source = target.source,
                    message = "type '${target.name}' has multiple class supertypes: $classSupers",
                ),
            )
        }
    }
}

private class CfirExtensionsResolveProcessor(
    private val diagnosticReporter: CfirDiagnosticReporter,
) : CfirMinimalPhaseResolveProcessor(
    fromPhase = CfirResolvePhase.STATUS,
    toPhase = CfirResolvePhase.EXTENSIONS,
) {
    override fun doProcess(target: CfirDeclaration, session: CfirSession) {
        if (target !is CfirExtend) return
        val resolver = CfirTypeRefResolver(session)

        if (target.extendedTypeRef.isDefinitelyIllegalExtendedType()) {
            diagnosticReporter.report(
                ILLEGAL_EXTENDED_TYPE_DIAGNOSTIC.on(
                    source = target.extendedTypeRef.source,
                    message = "[${RULE_ILLEGAL_EXTENDED_TYPE.id}] illegal extended type '${target.extendedTypeRef.renderStableKey()}' (${RULE_ILLEGAL_EXTENDED_TYPE.officialReference})",
                ),
            )
        }

        resolver.resolveClass(target.extendedTypeRef)?.let { resolved ->
            if (resolved.classKind == CfirClassKind.INTERFACE) {
                diagnosticReporter.report(
                    ILLEGAL_EXTENDED_TYPE_DIAGNOSTIC.on(
                        source = target.extendedTypeRef.source,
                        message = "[${RULE_ILLEGAL_EXTENDED_TYPE.id}] extend target cannot be interface '${resolved.name}' (${RULE_ILLEGAL_EXTENDED_TYPE.officialReference})",
                    ),
                )
            }
        }

        val seen = linkedSetOf<String>()
        for (superTypeRef in target.superTypeRefs) {
            val key = superTypeRef.renderStableKey()
            if (!seen.add(key)) {
                diagnosticReporter.report(
                    EXTEND_DUPLICATE_INTERFACE_DIAGNOSTIC.on(
                        source = superTypeRef.source,
                        message = "[${RULE_EXTEND_DUPLICATE_INTERFACE.id}] duplicate extend interface '$key' (${RULE_EXTEND_DUPLICATE_INTERFACE.officialReference})",
                    ),
                )
            }

            if (superTypeRef.isDefinitelyNotInterfaceType()) {
                diagnosticReporter.report(
                    EXTEND_NOT_INTERFACE_DIAGNOSTIC.on(
                        source = superTypeRef.source,
                        message = "[${RULE_EXTEND_NOT_INTERFACE.id}] inherited type '$key' in extend declaration is not an interface (${RULE_EXTEND_NOT_INTERFACE.officialReference})",
                    ),
                )
                continue
            }

            resolver.resolveClass(superTypeRef)?.let { resolved ->
                if (resolved.classKind != CfirClassKind.INTERFACE) {
                    diagnosticReporter.report(
                        EXTEND_NOT_INTERFACE_DIAGNOSTIC.on(
                            source = superTypeRef.source,
                            message = "[${RULE_EXTEND_NOT_INTERFACE.id}] inherited type '$key' in extend declaration resolves to non-interface '${resolved.name}' (${RULE_EXTEND_NOT_INTERFACE.officialReference})",
                        ),
                    )
                }
            }
        }
    }
}

private class CfirTypeRefResolver(
    private val session: CfirSession,
) {
    fun resolveClass(typeRef: CfirTypeRef): CfirClass? {
        val userTypeRef = typeRef as? CfirUserTypeRef ?: return null
        if (userTypeRef.qualifier.isEmpty()) return null

        val className = userTypeRef.qualifier.last()
        val packageName = userTypeRef.qualifier.dropLast(1).joinToString(".") { it.asString() }
        val packageFqName = if (packageName.isEmpty()) FqName.ROOT else FqName(packageName)
        val classId = ClassId(packageFqName, className)
        return session.cfirProvider.getClassByClassId(classId)
    }
}

private fun CfirTypeRef.renderStableKey(): String = toString()

private fun CfirTypeRef.isDefinitelyNotInterfaceType(): Boolean = when (this) {
    is CfirBasicTypeRef,
    is CfirImplicitTypeRef,
    is CfirErrorTypeRef -> true
    else -> false
}

private fun CfirTypeRef.isDefinitelyIllegalExtendedType(): Boolean = when (this) {
    is CfirImplicitTypeRef,
    is CfirErrorTypeRef -> true
    else -> false
}
