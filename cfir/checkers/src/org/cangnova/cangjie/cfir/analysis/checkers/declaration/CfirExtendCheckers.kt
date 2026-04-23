package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemanticsSupport
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.session.extendRuleQueryServiceOrNull
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.arrayElementType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

object CfirExtendTargetLegalityChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
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
        if (CfirExtendSemanticsSupport.isForeignInteropBoundaryTarget(context, extend)) {
            reporter.reportOn(
                source = targetTypeRef.source,
                factory = CfirErrors.EXTEND_C_TYPE_NOT_ALLOWED,
                a = targetTypeRef.toApproxName(),
            )
            return
        }

        val targetIsIllegal = when (targetConeType) {
            is ConeClassLikeType -> targetConeType.isInterface
            is ConeFunctionType,
            is ConeTupleType,
            is ConeVArrayType,
            is ConePointerType,
            is ConeIntersectionType,
            is ConeUnionType -> true
            else -> false
        }
        if (!targetIsIllegal) return

        reporter.reportOn(
            source = targetTypeRef.source,
            factory = CfirErrors.ILLEGAL_EXTENDED_TYPE,
            a = targetTypeRef.toApproxName(),
        )
    }
}

object CfirExtendInterfaceKindChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
        for (superTypeRef in extend.superTypeRefs) {
            val superClassId = CfirExtendSemanticsSupport.run { superTypeRef.toClassIdOrNull() }
            if (CfirExtendSemanticsSupport.isProtectedInterface(superClassId)) {
                reporter.reportOn(
                    source = superTypeRef.source,
                    factory = CfirErrors.EXTEND_INTERFACE_NOT_EXTENDABLE,
                    a = superTypeRef.toApproxName(),
                )
                continue
            }

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
            if (!definitelyNotInterface) continue

            reporter.reportOn(
                source = superTypeRef.source,
                factory = CfirErrors.EXTEND_NOT_INTERFACE,
                a = superTypeRef.toApproxName(),
            )
        }
    }
}

object CfirExtendDuplicateInterfaceChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
        val query = context.session.extendRuleQueryServiceOrNull ?: return
        val targetClassId = query.targetClassIdOf(extend) ?: return

        val localSeen = linkedSetOf<String>()
        val localSemanticKeys = query.inheritedInterfaceSemanticKeysOf(extend)
        val localInterfaces = query.inheritedInterfacesOf(extend)
        val interfacesInOtherExtends = query
            .inheritedInterfaceSemanticKeysForTarget(targetClassId, excludingDeclaration = extend)
            .toSet()
        val targetOwnInterfaces = query.targetClassOwnInterfaceClassIds(targetClassId)

        for ((index, superTypeRef) in extend.superTypeRefs.withIndex()) {
            val key = localSemanticKeys.getOrNull(index) ?: superTypeRef.toSemanticStableKey()
            val duplicatedInsideDeclaration = !localSeen.add(key)
            val duplicatedAcrossExtends = key in interfacesInOtherExtends
            val duplicatedWithTarget = localInterfaces.getOrNull(index)?.classId in targetOwnInterfaces
            if (!duplicatedInsideDeclaration && !duplicatedAcrossExtends && !duplicatedWithTarget) continue

            reporter.reportOn(
                source = superTypeRef.source,
                factory = CfirErrors.EXTEND_DUPLICATE_INTERFACE,
                a = superTypeRef.toApproxName(),
            )
        }
    }
}

object CfirExtendOrphanRuleChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
        val query = context.session.extendRuleQueryServiceOrNull ?: return
        val declarationPackage = query.packageFqNameOf(extend) ?: return
        val targetClassId = query.targetClassIdOf(extend) ?: return
        if (CfirExtendSemanticsSupport.isTargetDeclaredInPackage(context, extend, declarationPackage)) return

        val currentInterfaceClosure = query.inheritedInterfaceClosureClassIdsOf(extend)
        if (currentInterfaceClosure.any { interfaceClassId -> interfaceClassId.packageFqName == declarationPackage }) {
            return
        }

        val otherPackageClosure = query.otherPackageExtendedInterfaceClassIds(targetClassId, declarationPackage)
        val newlyIntroducedExternalInterfaces = currentInterfaceClosure - otherPackageClosure
        if (newlyIntroducedExternalInterfaces.isEmpty()) return

        reporter.reportOn(
            source = extend.extendedTypeRef.source,
            factory = CfirErrors.EXTEND_ORPHAN_RULE,
            a = targetClassId.shortClassName,
        )
    }
}

object CfirExtendGenericUsageChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
        if (extend.typeParameters.isEmpty()) return
        val allTypeRefs = listOf(extend.extendedTypeRef)

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

object CfirExtendImmutableMutInterfaceChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
        val targetConeType = (extend.extendedTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        if (!CfirExtendSemanticsSupport.isImmutableTarget(targetConeType)) return

        for (superTypeRef in extend.superTypeRefs) {
            val interfaceClassId = CfirExtendSemanticsSupport.run { superTypeRef.toClassIdOrNull() } ?: continue
            val leak = CfirExtendSemanticsSupport.findMutPropertyLeak(context, interfaceClassId) ?: continue
            reporter.reportOn(
                source = superTypeRef.source,
                factory = CfirErrors.EXTEND_IMMUTABLE_MUT_INTERFACE,
                a = leak.interfaceClassId.shortClassName,
                b = leak.propertyName,
            )
        }
    }
}

object CfirExtendImmutableMemberChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
        val targetConeType = (extend.extendedTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        if (!CfirExtendSemanticsSupport.isImmutableTarget(targetConeType)) return

        for (declaration in extend.declarations) {
            when {
                declaration is CfirProperty && declaration.status.isMut -> {
                    reporter.reportOn(
                        source = declaration.source,
                        factory = CfirErrors.EXTEND_IMMUTABLE_MUT_PROPERTY,
                        a = declaration.name,
                    )
                }

                declaration is CfirNamedFunction &&
                    CfirExtendSemanticsSupport.isImmutableNonEnumTarget(targetConeType) &&
                    declaration.status.isOperator &&
                    declaration.name == OperatorNameConventions.SET -> {
                    reporter.reportOn(
                        source = declaration.source,
                        factory = CfirErrors.EXTEND_IMMUTABLE_INDEX_ASSIGNMENT,
                        a = declaration.name,
                    )
                }
            }
        }
    }
}

object CfirExtendSpecializationConflictChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
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

object CfirExtendDefaultImplementationConflictChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
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

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.toSemanticStableKey(): String {
    val coneType = (this as? CfirResolvedTypeRef)?.coneType
    return coneType?.toString() ?: toString()
}

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.containsTypeParameter(parameterName: String): Boolean {
    val coneType = (this as? CfirResolvedTypeRef)?.coneType ?: return false
    return coneType.containsTypeParameter(parameterName)
}

private fun ConeCangJieType.containsTypeParameter(parameterName: String): Boolean = when (this) {
    is ConeTypeParameterType -> lookupTag.name.asString() == parameterName
    is ConeClassLikeType -> typeArguments.any { it.type.containsTypeParameter(parameterName) }
    is ConeStructType -> typeArguments.any { it.type.containsTypeParameter(parameterName) }
    is ConeEnumType -> typeArguments.any { it.type.containsTypeParameter(parameterName) }
    is ConeTypeAliasType -> typeArguments.any { it.type.containsTypeParameter(parameterName) } ||
        (expandedType?.containsTypeParameter(parameterName) == true)
    is ConeFunctionType -> parameterTypes.any { it.containsTypeParameter(parameterName) } ||
        returnType.containsTypeParameter(parameterName)
    is ConeTupleType -> elementTypes.any { it.containsTypeParameter(parameterName) }
    is ConeVArrayType -> elementType.containsTypeParameter(parameterName)
    is ConePointerType -> pointeeType.containsTypeParameter(parameterName)
    is ConeIntersectionType -> intersectedTypes.any { it.containsTypeParameter(parameterName) }
    is ConeUnionType -> unionTypes.any { it.containsTypeParameter(parameterName) }
    else -> arrayElementType?.containsTypeParameter(parameterName) == true
}

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.toApproxName(): Name {
    val classId = CfirExtendSemanticsSupport.run { toClassIdOrNull() }
    if (classId != null) return classId.shortClassName

    val raw = toString().substringAfterLast('.').substringBefore('<')
    return Name.identifierIfValid(raw) ?: Name.ERROR_NAME
}
