package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedTypeQualifierError
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.extendRuleQueryServiceOrNull
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.abbreviatedType
import org.cangnova.cangjie.cfir.types.arrayElementType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions

object CfirExtendTargetLegalityChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
        val targetTypeRef = extend.extendedTypeRef

        val targetConeType = targetTypeRef.extendSemanticType()
        if (targetTypeRef.isDefinitelyIllegalExtendedType() && targetConeType == null) {
            reporter.reportOn(
                source = targetTypeRef.source,
                factory = CfirErrors.ILLEGAL_EXTENDED_TYPE,
                a = targetTypeRef.toApproxName(),
            )
            return
        }

        targetConeType ?: return
        if (CfirExtendSemantics.isForeignInteropBoundaryTarget(context, extend)) {
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
            val superClassId = CfirExtendSemantics.run { superTypeRef.toClassIdOrNull() }
            if (CfirExtendSemantics.isProtectedInterface(superClassId)) {
                reporter.reportOn(
                    source = superTypeRef.source,
                    factory = CfirErrors.EXTEND_INTERFACE_NOT_EXTENDABLE,
                    a = superTypeRef.toApproxName(),
                )
                if (CfirExtendSemantics.isCType(superClassId)) {
                    val targetName = CfirExtendSemantics.run {
                        extend.extendedTypeRef.toClassIdOrNull()?.shortClassName
                    } ?: extend.extendedTypeRef.toApproxName()
                    reporter.reportOn(
                        source = superTypeRef.source,
                        factory = CfirErrors.CANNOT_INHERIT_SEALED,
                        a = "implement",
                        b = targetName.asString(),
                        c = "sealed interface",
                        d = superTypeRef.toApproxName(),
                    )
                }
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
                is ConePrimitiveType,
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
    override fun check(declaration: CfirExtend) {
        val query = context.session.extendRuleQueryServiceOrNull ?: return
        val targetKey = query.targetKeyOf(declaration) ?: return

        val localSeen = linkedSetOf<String>()
        val localSemanticKeys = query.inheritedInterfaceSemanticKeysOf(declaration)
        val localInterfaces = query.inheritedInterfacesOf(declaration)
        val interfacesInOtherExtends = query
            .inheritedInterfaceSemanticKeysForTarget(targetKey, excludingDeclaration = declaration)
            .toSet()
        val targetOwnInterfaces = targetKey.classIdOrNull?.let(query::targetClassOwnInterfaceClassIds).orEmpty()
        val reportCrossExtendDuplicate = declaration.superTypeRefs.size == 1 && query.isFirstExtendForTarget(declaration, targetKey)

        for ((index, superTypeRef) in declaration.superTypeRefs.withIndex()) {
            val key = localSemanticKeys.getOrNull(index) ?: superTypeRef.toSemanticStableKey()
            val firstInDeclaration = localSeen.add(key)
            val duplicatedInsideDeclaration = !firstInDeclaration &&
                localSemanticKeys.drop(index + 1).none { laterKey -> laterKey == key }
            val duplicatedAcrossExtends = reportCrossExtendDuplicate && key in interfacesInOtherExtends
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
        if (CfirExtendSemantics.isTargetDeclaredInPackage(context, extend, declarationPackage)) return

        val currentInterfaceClosure = query.inheritedInterfaceClosureClassIdsOf(extend)
        val currentExternalInterfaces = currentInterfaceClosure
            .filterTo(linkedSetOf()) { interfaceClassId -> interfaceClassId.packageFqName != declarationPackage }
        if (currentExternalInterfaces.isEmpty()) return

        val otherPackageClosure = query.otherPackageExtendedInterfaceClassIds(targetClassId, declarationPackage)
        val newlyIntroducedExternalInterfaces = currentExternalInterfaces - otherPackageClosure
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
                source = extend.extendedTypeRef.source,
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
        if (!CfirExtendSemantics.isImmutableTarget(targetConeType)) return

        for (superTypeRef in extend.superTypeRefs) {
            val interfaceClassId = CfirExtendSemantics.run { superTypeRef.toClassIdOrNull() } ?: continue
            val leak = CfirExtendSemantics.findMutPropertyLeak(context, interfaceClassId) ?: continue
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
        if (!CfirExtendSemantics.isImmutableTarget(targetConeType)) return

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
                    CfirExtendSemantics.isImmutableNonEnumTarget(targetConeType) &&
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
        val targetKey = query.targetKeyOf(extend) ?: return
        val localInterfaces = query.inheritedInterfacesOf(extend)
        val foreignInterfaces = query.inheritedInterfacesForTarget(targetKey, excludingDeclaration = extend)

        for ((index, localInterface) in localInterfaces.withIndex()) {
            val localClassId = localInterface.classId ?: continue
            val conflict = foreignInterfaces.any { other ->
                other.classId == localClassId && other.semanticKey != localInterface.semanticKey
            }
            if (!conflict) continue

            val sourceTypeRef = extend.superTypeRefs.getOrNull(index)
            val localUsesTypeParameter = sourceTypeRef?.containsAnyExtendTypeParameter(extend) == true
            val foreignGenericConflict = localUsesTypeParameter &&
                foreignInterfaces.any { other ->
                    other.classId == localClassId &&
                        other.semanticKey != localInterface.semanticKey &&
                        other.semanticKey.contains("__EXT_TP_")
                }
            if (localUsesTypeParameter && !foreignGenericConflict) continue
            if (foreignGenericConflict && query.isFirstExtendForTarget(extend, targetKey)) continue
            reporter.reportOn(
                source = sourceTypeRef?.source,
                factory = CfirErrors.EXTEND_DUPLICATE_INTERFACE,
                a = localClassId.shortClassName,
            )
        }
    }
}

object CfirExtendDefaultImplementationConflictChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
        val query = context.session.extendRuleQueryServiceOrNull ?: return
        val targetKey = query.targetKeyOf(extend) ?: return
        val localInterfaces = query.inheritedInterfacesOf(extend)
        if (localInterfaces.isEmpty()) return

        val foreignInterfaceIds = query
            .inheritedInterfacesForTarget(targetKey, excludingDeclaration = extend)
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
    is CfirImplicitTypeRef -> true
    is CfirErrorTypeRef -> this.diagnostic !is ConeUnresolvedTypeQualifierError
    else -> false
}

context(context: CheckerContext)
private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.extendSemanticType(): ConeCangJieType? {
    val coneType = (this as? CfirResolvedTypeRef)?.coneType ?: return null
    val recoverableType = (coneType as? ConeErrorType)?.delegatedType ?: coneType
    return recoverableType.fullyExpandedType(context.session)
}

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.toSemanticStableKey(): String {
    val coneType = (this as? CfirResolvedTypeRef)?.coneType
    return coneType?.toString() ?: toString()
}

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.containsTypeParameter(parameterName: String): Boolean {
    val coneType = (this as? CfirResolvedTypeRef)?.coneType ?: return false
    return coneType.containsTypeParameter(parameterName)
}

private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.containsAnyExtendTypeParameter(extend: CfirExtend): Boolean {
    val parameterNames = extend.typeParameters.mapTo(linkedSetOf()) { it.name.asString() }
    if (parameterNames.isEmpty()) return false
    val coneType = (this as? CfirResolvedTypeRef)?.coneType ?: return false
    return parameterNames.any { parameterName -> coneType.containsTypeParameter(parameterName) }
}

private fun ConeCangJieType.containsTypeParameter(parameterName: String): Boolean =
    abbreviatedType?.containsTypeParameter(parameterName) == true || containsTypeParameterInConstructor(parameterName)

private fun ConeCangJieType.containsTypeParameterInConstructor(parameterName: String): Boolean = when (this) {
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
    val classId = CfirExtendSemantics.run { toClassIdOrNull() }
    if (classId != null) return classId.shortClassName

    val raw = toString().substringAfterLast('.').substringBefore('<')
    return Name.identifierIfValid(raw) ?: Name.ERROR_NAME
}
