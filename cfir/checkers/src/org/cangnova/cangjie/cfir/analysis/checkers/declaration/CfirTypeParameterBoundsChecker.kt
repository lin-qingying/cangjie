package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.ConeAnyType
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker

object CfirTypeParameterBoundsChecker : CfirTypeParameterChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirTypeParameter) {
        val nonErrorBounds = declaration.symbol.resolvedBounds.filterNot { it.coneType is ConeErrorType }
        if (nonErrorBounds.isEmpty()) return

        val uniqueBounds = linkedMapOf<String, CfirResolvedTypeRef>()
        nonErrorBounds.forEach { bound ->
            uniqueBounds.putIfAbsent(bound.stableBoundKey(), bound)
        }

        val invalidBounds = uniqueBounds.values
            .mapNotNull { bound -> bound.takeIf { it.upperBoundKind() == UpperBoundKind.INVALID } }
        invalidBounds.forEach { bound ->
            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE,
                a = bound.coneType.fullyExpandTypeAlias(),
                b = declaration.name,
            )
        }
        if (invalidBounds.isNotEmpty()) return

        val classBounds = uniqueBounds.values
            .filter { it.upperBoundKind() == UpperBoundKind.CLASS }
            .map { it.coneType.fullyExpandTypeAlias() }

        if (classBounds.size > 1 && !classBounds.areInOneInheritanceChain()) {
            reporter.reportOn(declaration.source, CfirErrors.CONFLICTING_UPPER_BOUNDS)
        }
    }
}

private fun CfirResolvedTypeRef.stableBoundKey(): String = coneType
    .fullyExpandTypeAlias()
    .renderForDebugging()

context(context: CheckerContext)
private fun CfirResolvedTypeRef.upperBoundKind(): UpperBoundKind {
    val expandedType = coneType.fullyExpandTypeAlias()
    return when (expandedType) {
        ConeAnyType -> UpperBoundKind.IGNORED_TOP_OR_CTYPE
        is ConeClassLikeType -> {
            val classId = expandedType.classId
            when {
                classId == StdlibClassIds.Any || CfirExtendSemantics.isCType(classId) ->
                    UpperBoundKind.IGNORED_TOP_OR_CTYPE
                expandedType.toResolvedClassLikeDeclaration() is CfirInterface ->
                    UpperBoundKind.INTERFACE
                expandedType.toResolvedClassLikeDeclaration() is CfirClass ->
                    UpperBoundKind.CLASS
                expandedType.isInterface ->
                    UpperBoundKind.INTERFACE
                else ->
                    UpperBoundKind.CLASS
            }
        }
        else -> {
            val classId = expandedType.classIdOrPrimitiveClassId
            if (classId == StdlibClassIds.Any || CfirExtendSemantics.isCType(classId)) {
                UpperBoundKind.IGNORED_TOP_OR_CTYPE
            } else {
                UpperBoundKind.INVALID
            }
        }
    }
}

context(context: CheckerContext)
private fun ConeCangJieType.toResolvedClassLikeDeclaration(): CfirClassLikeDeclaration? =
    when (this) {
        is ConeClassLikeType -> context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
        is ConeTypeAliasType -> context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
        else -> null
    }

private fun ConeCangJieType.fullyExpandTypeAlias(): ConeCangJieType {
    var current = this
    while (current is ConeTypeAliasType && current.expandedType != null) {
        current = current.expandedType ?: break
    }
    return current
}

context(context: CheckerContext)
private fun List<ConeCangJieType>.areInOneInheritanceChain(): Boolean {
    for (leftIndex in indices) {
        for (rightIndex in leftIndex + 1 until size) {
            if (!this[leftIndex].isRelatedTo(this[rightIndex])) return false
        }
    }
    return true
}

context(context: CheckerContext)
private fun ConeCangJieType.isRelatedTo(other: ConeCangJieType): Boolean =
    AbstractTypeChecker.isSubtypeOf(context.session.typeContext, this, other) ||
            AbstractTypeChecker.isSubtypeOf(context.session.typeContext, other, this)

private enum class UpperBoundKind {
    IGNORED_TOP_OR_CTYPE,
    CLASS,
    INTERFACE,
    INVALID,
}
