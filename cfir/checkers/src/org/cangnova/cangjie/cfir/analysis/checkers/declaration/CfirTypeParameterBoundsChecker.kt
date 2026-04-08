package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeKind

object CfirTypeParameterBoundsChecker : CfirTypeParameterChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirTypeParameter) {
        val nonErrorBounds = declaration.symbol.resolvedBounds.filterNot { it.coneType is ConeErrorType }
        if (nonErrorBounds.isEmpty()) return

        val uniqueBounds = linkedMapOf<String, CfirResolvedTypeRef>()
        nonErrorBounds.forEach { bound ->
            val key = bound.stableBoundKey()
            if (uniqueBounds.putIfAbsent(key, bound) != null) {
                reporter.reportOn(bound.source, CfirErrors.REPEATED_BOUND)
            }
        }

        var seenConcreteBound = false
        uniqueBounds.values.forEach { bound ->
            if (!bound.hasConcreteUpperBound(context)) return@forEach
            if (seenConcreteBound) {
                reporter.reportOn(bound.source, CfirErrors.ONLY_ONE_CLASS_BOUND_ALLOWED)
            } else {
                seenConcreteBound = true
            }
        }

        if (uniqueBounds.size > 1) {
            val emptyIntersection = context.session.typeContext.computeEmptyIntersectionTypeKind(
                uniqueBounds.values.map { it.coneType.fullyExpandTypeAlias() },
            )
            if (emptyIntersection?.kind == EmptyIntersectionTypeKind.MULTIPLE_CLASSES) {
                reporter.reportOn(declaration.source, CfirErrors.CONFLICTING_UPPER_BOUNDS)
            }
        }
    }
}

private fun CfirResolvedTypeRef.stableBoundKey(): String = coneType
    .fullyExpandTypeAlias()
    .renderForDebugging()

private fun CfirResolvedTypeRef.hasConcreteUpperBound(context: CheckerContext): Boolean {
    val expandedType = coneType.fullyExpandTypeAlias()
    return when (expandedType) {
        is ConePrimitiveType, is ConeStructType, is ConeEnumType -> true
        is ConeClassLikeType -> expandedType.toResolvedClassLikeDeclaration(context) !is CfirInterface
        else -> false
    }
}

private fun ConeCangJieType.toResolvedClassLikeDeclaration(context: CheckerContext) =
    when (this) {
        is ConePrimitiveType -> context.session.cfirProvider.getClassByClassId(kind.classId)
        is ConeClassLikeType -> context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
        is ConeStructType -> context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
        is ConeEnumType -> context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir
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

private val org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.classId
    get() = org.cangnova.cangjie.name.ClassId.fromString(typeName)
