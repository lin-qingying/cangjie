package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.extendRuleQueryServiceOrNull
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeArrayType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.renderSemanticKey
import org.cangnova.cangjie.name.Name

abstract class CfirExtendChecker(
    dispatchKind: CheckerDispatchKind,
) : CfirClassLikeChecker(dispatchKind) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    final override fun check(declaration: CfirClassLikeDeclaration) {
        val extend = declaration as? CfirExtend ?: return
        checkExtend(extend)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    protected abstract fun checkExtend(extend: CfirExtend)
}

object CfirExtendTargetLegalityChecker : CfirExtendChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkExtend(extend: CfirExtend) {
        val targetTypeRef = extend.extendedTypeRef

        if (targetTypeRef.isDefinitelyIllegalExtendedType()) {
            reporter.reportOn(
                source = targetTypeRef.source,
                factory = CfirErrors.ILLEGAL_EXTENDED_TYPE,
                a = targetTypeRef.toApproxName(),
            )
            return
        }

        val targetConeType = (targetTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        val targetIsIllegal = when (targetConeType) {
            is ConeClassLikeType -> targetConeType.isInterface
            is ConeStructType,
            is ConeEnumType -> true
            else -> false
        }
        if (targetIsIllegal) {
            reporter.reportOn(
                source = targetTypeRef.source,
                factory = CfirErrors.ILLEGAL_EXTENDED_TYPE,
                a = targetTypeRef.toApproxName(),
            )
        }
    }
}

object CfirExtendInterfaceKindChecker : CfirExtendChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkExtend(extend: CfirExtend) {
        for (superTypeRef in extend.superTypeRefs) {
            if (superTypeRef.isDefinitelyNotInterfaceType()) {
                reporter.reportOn(
                    source = superTypeRef.source,
                    factory = CfirErrors.EXTEND_NOT_INTERFACE,
                    a = superTypeRef.toApproxName(),
                )
                continue
            }

            val superConeType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            val definitelyNotInterface = when (superConeType) {
                is ConeClassLikeType -> !superConeType.isInterface
                is ConeStructType,
                is ConeEnumType -> true
                else -> false
            }
            if (definitelyNotInterface) {
                reporter.reportOn(
                    source = superTypeRef.source,
                    factory = CfirErrors.EXTEND_NOT_INTERFACE,
                    a = superTypeRef.toApproxName(),
                )
            }
        }
    }
}

object CfirExtendDuplicateInterfaceChecker : CfirExtendChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkExtend(extend: CfirExtend) {
        val query = context.session.extendRuleQueryServiceOrNull ?: return
        val targetClassId = query.targetClassIdOf(extend) ?: return

        val localSeen = linkedSetOf<String>()
        val localSemanticKeys = query.inheritedInterfaceSemanticKeysOf(extend)
        val interfacesInOtherExtends = query
            .inheritedInterfaceSemanticKeysForTarget(targetClassId, excludingDeclaration = extend)
            .toSet()

        for ((index, superTypeRef) in extend.superTypeRefs.withIndex()) {
            val key = localSemanticKeys.getOrNull(index) ?: superTypeRef.toSemanticStableKey()
            val duplicatedInsideDeclaration = !localSeen.add(key)
            val duplicatedAcrossExtends = key in interfacesInOtherExtends
            if (!duplicatedInsideDeclaration && !duplicatedAcrossExtends) continue

            reporter.reportOn(
                source = superTypeRef.source,
                factory = CfirErrors.EXTEND_DUPLICATE_INTERFACE,
                a = superTypeRef.toApproxName(),
            )
        }
    }
}

object CfirExtendOrphanRuleChecker : CfirExtendChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkExtend(extend: CfirExtend) {
        val query = context.session.extendRuleQueryServiceOrNull ?: return
        val declarationPackage = query.packageFqNameOf(extend) ?: return
        val targetClassId = query.targetClassIdOf(extend) ?: return

        val targetIsLocal = targetClassId.packageFqName == declarationPackage
        val anyInterfaceIsLocal = query.inheritedInterfacesOf(extend).any { inherited ->
            inherited.classId?.packageFqName == declarationPackage
        }
        if (targetIsLocal || anyInterfaceIsLocal) return

        reporter.reportOn(
            source = extend.extendedTypeRef.source,
            factory = CfirErrors.EXTEND_ORPHAN_RULE,
            a = targetClassId.shortClassName,
        )
    }
}

object CfirExtendGenericUsageChecker : CfirExtendChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkExtend(extend: CfirExtend) {
        if (extend.typeParameters.isEmpty()) return
        val allTypeRefs = buildList {
            add(extend.extendedTypeRef)
            addAll(extend.superTypeRefs)
        }

        for (typeParameter in extend.typeParameters) {
            val used = allTypeRefs.any { typeRef -> typeRef.containsTypeParameter(typeParameter.name.asString()) }
            if (used) continue
            reporter.reportOn(
                source = typeParameter.source,
                factory = CfirErrors.EXTEND_GENERIC_USAGE,
                a = typeParameter.name,
            )
        }
    }
}

object CfirExtendSpecializationConflictChecker : CfirExtendChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkExtend(extend: CfirExtend) {
        val query = context.session.extendRuleQueryServiceOrNull ?: return
        val targetClassId = query.targetClassIdOf(extend) ?: return
        val localInterfaces = query.inheritedInterfacesOf(extend)
        val foreignInterfaces = query.inheritedInterfacesForTarget(targetClassId, excludingDeclaration = extend)

        for ((index, localInterface) in localInterfaces.withIndex()) {
            val localClassId = localInterface.classId ?: continue
            val conflict = foreignInterfaces.any { other ->
                other.classId == localClassId && other.semanticKey != localInterface.semanticKey
            }
            if (!conflict) continue

            val sourceTypeRef = extend.superTypeRefs.getOrNull(index)
            reporter.reportOn(
                source = sourceTypeRef?.source,
                factory = CfirErrors.EXTEND_SPECIALIZATION_CONFLICT,
                a = localClassId.shortClassName,
            )
        }
    }
}

object CfirExtendDefaultImplementationConflictChecker : CfirExtendChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun checkExtend(extend: CfirExtend) {
        val query = context.session.extendRuleQueryServiceOrNull ?: return
        val targetClassId = query.targetClassIdOf(extend) ?: return
        val localInterfaces = query.inheritedInterfacesOf(extend)
        if (localInterfaces.isEmpty()) return

        val foreignInterfaceIds = query
            .inheritedInterfacesForTarget(targetClassId, excludingDeclaration = extend)
            .mapNotNull { it.classId }
            .toSet()
        if (foreignInterfaceIds.isEmpty()) return

        for ((index, localInterface) in localInterfaces.withIndex()) {
            val interfaceClassId = localInterface.classId ?: continue
            if (interfaceClassId !in foreignInterfaceIds) continue
            val conflictMembers = query.defaultIndependentMembersOfInterface(interfaceClassId)
            if (conflictMembers.isEmpty()) continue

            val sourceTypeRef = extend.superTypeRefs.getOrNull(index)
            for (memberName in conflictMembers) {
                reporter.reportOn(
                    source = sourceTypeRef?.source,
                    factory = CfirErrors.EXTEND_DEFAULT_IMPLEMENTATION_CONFLICT,
                    a = memberName,
                    b = interfaceClassId.shortClassName,
                )
            }
        }
    }
}

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.isDefinitelyNotInterfaceType(): Boolean = when (this) {
    is CfirBasicTypeRef,
    is CfirImplicitTypeRef,
    is CfirErrorTypeRef -> true
    else -> false
}

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.isDefinitelyIllegalExtendedType(): Boolean = when (this) {
    is CfirImplicitTypeRef,
    is CfirErrorTypeRef -> true
    else -> false
}

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.toClassIdOrNull(): org.cangnova.cangjie.name.ClassId? {
    val coneType = (this as? CfirResolvedTypeRef)?.coneType ?: return null
    return when (coneType) {
        is ConeClassLikeType -> coneType.classId
        is ConeStructType -> coneType.classId
        is ConeEnumType -> coneType.classId
        else -> null
    }
}

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.toSemanticStableKey(): String {
    val coneType = (this as? CfirResolvedTypeRef)?.coneType
    return coneType?.renderSemanticKey() ?: toString()
}

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.containsTypeParameter(parameterName: String): Boolean {
    val coneType = (this as? CfirResolvedTypeRef)?.coneType ?: return false
    return coneType.containsTypeParameter(parameterName)
}

private fun ConeCangJieType.containsTypeParameter(parameterName: String): Boolean = when (this) {
    is ConeTypeParameterType -> lookupTag.name == parameterName
    is ConeClassLikeType -> typeArguments.any { it.containsTypeParameter(parameterName) }
    is ConeStructType -> typeArguments.any { it.containsTypeParameter(parameterName) }
    is ConeEnumType -> typeArguments.any { it.containsTypeParameter(parameterName) }
    is ConeTypeAliasType -> typeArguments.any { it.containsTypeParameter(parameterName) } ||
        (expandedType?.containsTypeParameter(parameterName) == true)
    is ConeFuncType -> parameterTypes.any { it.containsTypeParameter(parameterName) } ||
        returnType.containsTypeParameter(parameterName)
    is ConeTupleType -> elementTypes.any { it.containsTypeParameter(parameterName) }
    is ConeArrayType -> elementType.containsTypeParameter(parameterName)
    is ConeVArrayType -> elementType.containsTypeParameter(parameterName)
    is ConePointerType -> pointeeType.containsTypeParameter(parameterName)
    is ConeIntersectionType -> intersectedTypes.any { it.containsTypeParameter(parameterName) }
    is ConeUnionType -> unionTypes.any { it.containsTypeParameter(parameterName) }
    else -> false
}

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.toApproxName(): Name {
    val classId = toClassIdOrNull()
    if (classId != null) return classId.shortClassName
    val raw = toString().substringAfterLast('.').substringBefore('<')
    return Name.identifierIfValid(raw) ?: Name.ERROR_NAME
}

