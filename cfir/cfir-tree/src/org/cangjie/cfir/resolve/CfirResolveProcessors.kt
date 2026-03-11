package org.cangjie.cfir.resolve

import org.cangjie.cfir.declarations.CfirClass
import org.cangjie.cfir.declarations.CfirClassKind
import org.cangjie.cfir.declarations.CfirDeclaration
import org.cangjie.cfir.declarations.CfirExtend
import org.cangjie.cfir.declarations.CfirFile
import org.cangjie.cfir.declarations.CfirInvalidDeclaration
import org.cangjie.cfir.declarations.CfirImport
import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.diagnostics.CfirDiagnosticFactory
import org.cangjie.cfir.diagnostics.CfirDiagnosticReporter
import org.cangjie.cfir.diagnostics.CfirDiagnosticSeverity
import org.cangjie.cfir.resolve.diagnostics.CfirResolveRuleCatalog
import org.cangjie.cfir.resolve.diagnostics.CfirResolveRuleDefinition
import org.cangjie.cfir.resolve.services.CfirResolvedImportBinding
import org.cangjie.cfir.resolve.services.CfirResolvedImportTarget
import org.cangjie.cfir.resolve.services.CfirSuperTypeGraphEdge
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.session.cfirProvider
import org.cangjie.cfir.session.importBindingStoreOrNull
import org.cangjie.cfir.session.symbolProvider
import org.cangjie.cfir.session.superTypeGraphStoreOrNull
import org.cangjie.cfir.symbols.CfirClassSymbol
import org.cangjie.cfir.types.CfirBasicTypeRef
import org.cangjie.cfir.types.CfirErrorTypeRef
import org.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangjie.cfir.types.CfirTypeRef
import org.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

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

private val IMPORT_TARGET_NOT_FOUND_DIAGNOSTIC = CfirDiagnosticFactory(
    name = "CFIR_IMPORT_TARGET_NOT_FOUND",
    defaultSeverity = CfirDiagnosticSeverity.ERROR,
)

private val IMPORT_CONFLICT_DIAGNOSTIC = CfirDiagnosticFactory(
    name = "CFIR_IMPORT_CONFLICT",
    defaultSeverity = CfirDiagnosticSeverity.ERROR,
)

private val IMPORT_ALIAS_CONFLICT_DIAGNOSTIC = CfirDiagnosticFactory(
    name = "CFIR_IMPORT_ALIAS_CONFLICT",
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
private val RULE_IMPORTS_BINDING: CfirResolveRuleDefinition =
    CfirResolveRuleCatalog.IMPORTS_BINDING
private val RULE_IMPORTS_CONFLICT: CfirResolveRuleDefinition =
    CfirResolveRuleCatalog.IMPORTS_CONFLICT

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
    registry.registerProcessor(CfirResolvePhase.IMPORTS, CfirImportsResolveProcessor(diagnosticReporter))
    registry.registerProcessor(CfirResolvePhase.SUPER_TYPES, CfirSuperTypesResolveProcessor(diagnosticReporter))
    registry.registerProcessor(CfirResolvePhase.TYPES, CfirTypesResolveProcessor())
    registry.registerProcessor(CfirResolvePhase.STATUS, CfirStatusResolveProcessor())
    registry.registerProcessor(CfirResolvePhase.EXTENSIONS, CfirExtensionsResolveProcessor(diagnosticReporter))
    registry.registerProcessor(CfirResolvePhase.IMPLICIT_TYPES, CfirImplicitTypesResolveProcessor())
    registry.registerProcessor(CfirResolvePhase.BODY_RESOLVE, CfirBodyResolveProcessor())
    registry.registerProcessor(CfirResolvePhase.CHECKERS, CfirCheckersResolveProcessor(diagnosticReporter))
}

private abstract class CfirPhaseResolveProcessor(
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

private class CfirImportsResolveProcessor(
    private val diagnosticReporter: CfirDiagnosticReporter,
) : CfirPhaseResolveProcessor(
    fromPhase = CfirResolvePhase.RAW_CFIR,
    toPhase = CfirResolvePhase.IMPORTS,
) {
    override fun doProcess(target: CfirDeclaration, session: CfirSession) {
        if (target !is CfirFile) return
        val store = session.importBindingStoreOrNull ?: return
        val resolvedImports = target.imports.map { resolveImportBinding(it, session) }
        resolvedImports
            .filter { it.targets.isEmpty() }
            .forEach { unresolved ->
                diagnosticReporter.report(
                    IMPORT_TARGET_NOT_FOUND_DIAGNOSTIC.on(
                        source = unresolved.importDirective.source,
                        message = "[${RULE_IMPORTS_BINDING.id}] unresolved import target '${unresolved.importDirective.importedFqName.asString()}' (${RULE_IMPORTS_BINDING.officialReference})",
                    ),
                )
            }
        reportImportConflicts(resolvedImports)
        store.record(target, resolvedImports)
    }

    private fun reportImportConflicts(resolvedImports: List<CfirResolvedImportBinding>) {
        resolvedImports
            .groupBy { it.effectiveName }
            .forEach { (effectiveName, sameNameBindings) ->
                if (sameNameBindings.size < 2) return@forEach
                val signatures = sameNameBindings.map { it.stableTargetSignature() }.toSet()
                if (signatures.size > 1) {
                    diagnosticReporter.report(
                        IMPORT_CONFLICT_DIAGNOSTIC.on(
                            source = sameNameBindings.first().importDirective.source,
                            message = "[${RULE_IMPORTS_CONFLICT.id}] conflicting imports for name '${effectiveName.asString()}' (${RULE_IMPORTS_CONFLICT.officialReference})",
                        ),
                    )
                }
            }

        resolvedImports
            .filter { it.importDirective.aliasName != null }
            .groupBy { it.importDirective.aliasName!! }
            .forEach { (aliasName, sameAliasBindings) ->
                if (sameAliasBindings.size < 2) return@forEach
                val signatures = sameAliasBindings.map { it.stableTargetSignature() }.toSet()
                if (signatures.size > 1) {
                    diagnosticReporter.report(
                        IMPORT_ALIAS_CONFLICT_DIAGNOSTIC.on(
                            source = sameAliasBindings.first().importDirective.source,
                            message = "[${RULE_IMPORTS_CONFLICT.id}] alias conflict for '${aliasName.asString()}' (${RULE_IMPORTS_CONFLICT.officialReference})",
                        ),
                    )
                }
            }
    }

    private fun resolveImportBinding(importDirective: CfirImport, session: CfirSession): CfirResolvedImportBinding {
        val importedFqName = importDirective.importedFqName
        val effectiveName = importDirective.aliasName ?: importedFqName.shortNameAsIdentifier()
        val targets = mutableListOf<CfirResolvedImportTarget>()

        if (session.symbolProvider.hasPackage(importedFqName)) {
            targets += CfirResolvedImportTarget.Package(importedFqName)
        }

        val memberName = importedFqName.shortNameAsIdentifier()
        val packageFqName = importedFqName.parentOrRoot()

        if (!importDirective.isAllUnder) {
            val classId = ClassId(packageFqName, memberName)
            session.symbolProvider.getClassLikeSymbolByClassId(classId)?.let { symbol ->
                targets += CfirResolvedImportTarget.ClassLike(
                    classId = classId,
                    symbol = symbol,
                )
            }

            val callableSymbols = session.symbolProvider.getTopLevelCallableSymbols(packageFqName, memberName)
            if (callableSymbols.isNotEmpty()) {
                targets += CfirResolvedImportTarget.Callable(
                    packageFqName = packageFqName,
                    name = memberName,
                    symbols = callableSymbols,
                )
            }
        }

        return CfirResolvedImportBinding(
            importDirective = importDirective,
            effectiveName = effectiveName,
            targets = targets,
        )
    }
}

private class CfirTypesResolveProcessor : CfirPhaseResolveProcessor(
    fromPhase = CfirResolvePhase.SUPER_TYPES,
    toPhase = CfirResolvePhase.TYPES,
)

private class CfirStatusResolveProcessor : CfirPhaseResolveProcessor(
    fromPhase = CfirResolvePhase.TYPES,
    toPhase = CfirResolvePhase.STATUS,
)

private class CfirImplicitTypesResolveProcessor : CfirPhaseResolveProcessor(
    fromPhase = CfirResolvePhase.EXTENSIONS,
    toPhase = CfirResolvePhase.IMPLICIT_TYPES,
)

private class CfirBodyResolveProcessor : CfirPhaseResolveProcessor(
    fromPhase = CfirResolvePhase.IMPLICIT_TYPES,
    toPhase = CfirResolvePhase.BODY_RESOLVE,
)

private class CfirCheckersResolveProcessor(
    private val diagnosticReporter: CfirDiagnosticReporter,
) : CfirPhaseResolveProcessor(
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
) : CfirPhaseResolveProcessor(
    fromPhase = CfirResolvePhase.IMPORTS,
    toPhase = CfirResolvePhase.SUPER_TYPES,
) {
    override fun doProcess(target: CfirDeclaration, session: CfirSession) {
        if (target !is CfirClass) return
        val resolver = CfirTypeRefResolver(session)
        val seen = linkedSetOf<String>()
        val seenResolvedInterfaceSymbols = linkedSetOf<CfirClassSymbol>()
        val classLikeSupers = mutableListOf<CfirClass>()
        val graphEdges = mutableListOf<CfirSuperTypeGraphEdge>()

        for (superTypeRef in target.superTypeRefs) {
            val key = superTypeRef.renderStableKey()
            val resolvedClass = resolver.resolveClass(superTypeRef)
            graphEdges += CfirSuperTypeGraphEdge(
                renderedType = key,
                resolvedClassSymbol = resolvedClass?.symbol,
            )
            if (!seen.add(key)) {
                diagnosticReporter.report(
                    SUPER_TYPES_DUPLICATE_DIAGNOSTIC.on(
                        source = superTypeRef.source,
                        message = "[${RULE_SUPER_TYPES_DUPLICATE.id}] duplicate super type '$key' for type '${target.name}' (${RULE_SUPER_TYPES_DUPLICATE.officialReference})",
                    ),
                )
            }
            if (resolvedClass?.classKind == CfirClassKind.INTERFACE && !seenResolvedInterfaceSymbols.add(resolvedClass.symbol)) {
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
            resolvedClass?.let { classLikeSupers += it }
        }
        session.superTypeGraphStoreOrNull?.record(target, graphEdges)

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
) : CfirPhaseResolveProcessor(
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

private fun FqName.parentOrRoot(): FqName {
    val fqNameString = asString()
    val parentString = fqNameString.substringBeforeLast('.', missingDelimiterValue = "")
    return if (parentString.isEmpty()) FqName.ROOT else FqName(parentString)
}

private fun FqName.shortNameAsIdentifier(): Name {
    val fqNameString = asString()
    val shortNameString = fqNameString.substringAfterLast('.')
    return Name.identifier(shortNameString)
}

private fun CfirResolvedImportBinding.stableTargetSignature(): String {
    val targetSignatures = targets.map { target ->
        when (target) {
            is CfirResolvedImportTarget.Package -> "pkg:${target.fqName.asString()}"
            is CfirResolvedImportTarget.ClassLike -> "class:${target.classId.asString()}"
            is CfirResolvedImportTarget.Callable -> {
                val callableOwner = "${target.packageFqName.asString()}.${target.name.asString()}"
                "callable:$callableOwner#${target.symbols.size}"
            }
        }
    }.sorted()
    return buildString {
        append(importDirective.importedFqName.asString())
        append('|')
        append(importDirective.isAllUnder)
        append('|')
        append(targetSignatures.joinToString(";"))
    }
}

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
