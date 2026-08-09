package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.firstCharacterDiagnosticSource
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.diagnostic.ConeUnmatchedTypeArgumentsError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedTypeQualifierError
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.fullyExpandedTypeUsingAbbreviation
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitutionForConstraintDerivation
import org.cangnova.cangjie.cfir.resolve.providers.semanticExtendType
import org.cangnova.cangjie.cfir.resolve.providers.semanticExtendedType
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendRuleQueryServiceOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.CfirBasicTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConePointerType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.cfir.types.abbreviatedType
import org.cangnova.cangjie.cfir.types.arrayElementType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * extend 目标类型合法性检查器。
 *
 * 该检查器负责过滤不能作为 extend 接收者的类型，包括接口、函数类型、元组、varray、
 * 交叉/联合类型以及外部互操作边界类型。
 */
object CfirExtendTargetLegalityChecker : CfirExtendChecker() {
    /**
     * 检查单个 extend 声明的目标类型是否合法。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
        val targetTypeRef = extend.extendedTypeRef

        val targetConeType = targetTypeRef.semanticExtendType(context.session)
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

/**
 * extend 所实现接口的类型种类检查器。
 */
object CfirExtendInterfaceKindChecker : CfirExtendChecker() {
    /**
     * 检查 extend super type 列表中的每个接口类型是否合法且可扩展。
     */
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

            if (superTypeRef.isDefinitelyNotInterfaceType(context.session)) {
                reporter.reportOn(
                    source = superTypeRef.source,
                    factory = CfirErrors.EXTEND_NOT_INTERFACE,
                    a = superTypeRef.toApproxName(),
                )
                continue
            }

        }
    }
}

/**
 * extend 重复实现接口检查器。
 *
 * 同一 extend 内、同一目标的多个 extend 之间、目标类型本身已继承接口之间都需要检查
 * 重复接口关系。
 */
object CfirExtendDuplicateInterfaceChecker : CfirExtendChecker() {
    /**
     * 检查当前 extend 声明是否重复引入接口。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirExtend) {
        val query = context.session.extendRuleQueryServiceOrNull ?: return

        val localSeen = linkedSetOf<String>()
        val localSemanticKeys = query.inheritedInterfaceSemanticKeysOf(declaration)
        val sameTargetDeclarations = query
            .extendDeclarationsForSameTarget(declaration)
            .filterIsInstance<CfirExtend>()

        for ((index, superTypeRef) in declaration.superTypeRefs.withIndex()) {
            val localInterface = query.inheritedInterfacesOf(declaration).getOrNull(index) ?: continue
            if (localInterface.classId == null) continue

            val key = localSemanticKeys.getOrNull(index) ?: superTypeRef.toSemanticStableKey()
            val firstInDeclaration = localSeen.add(key)
            val duplicatedInsideDeclaration = !firstInDeclaration &&
                localSemanticKeys.drop(index + 1).none { laterKey -> laterKey == key }
            val declarationsWithSameInterface = sameTargetDeclarations.filter { candidate ->
                key in query.inheritedInterfaceSemanticKeysOf(candidate)
            }
            val duplicatedAcrossExtends = declarationsWithSameInterface.size > 1 &&
                declarationsWithSameInterface.last() === declaration
            val duplicatedWithTarget = declaration.duplicatesInheritedTargetInterface(superTypeRef, index)
            if (!duplicatedInsideDeclaration && !duplicatedAcrossExtends && !duplicatedWithTarget) continue

            reporter.reportOn(
                source = superTypeRef.source ?: (superTypeRef as? CfirResolvedTypeRef)?.delegatedTypeRef?.source,
                factory = CfirErrors.EXTEND_DUPLICATE_INTERFACE,
                a = superTypeRef.toApproxName(),
            )
        }
    }
}

/**
 * 判断当前 extend 的某个接口是否已经由目标类型或同目标其它 extend 继承。
 */
context(context: CheckerContext)
internal fun CfirExtend.duplicatesInheritedTargetInterface(
    superTypeRef: CfirTypeRef,
    index: Int? = null,
): Boolean {
    val query = context.session.extendRuleQueryServiceOrNull ?: return false
    val semanticKey = index
        ?.let { query.inheritedInterfaceSemanticKeysOf(this).getOrNull(it) }
        ?: superTypeRef.toSemanticStableKey()

    val targetOwnSemanticKeys = query.targetOwnInterfacesOf(this).mapTo(linkedSetOf()) { it.semanticKey }
    if (semanticKey in targetOwnSemanticKeys) {
        return true
    }

    val localInterfaceType = superTypeRef.coneTypeOrNull
        ?.fullyExpandedTypeUsingAbbreviation(context.session) as? ConeClassifierType
    val localInterfaceKey = localInterfaceType?.instantiatedInterfaceKey()
    val targetInheritedInterfaceKeys = instantiatedTargetInheritedInterfaceKeys()
    if (localInterfaceKey != null && localInterfaceKey in targetInheritedInterfaceKeys) {
        return true
    }

    val targetType = extendedTypeRef.coneTypeOrNull
    val superClassId = (localInterfaceType as? ConeClassLikeType)?.classId
    if (
        targetType != null &&
        superClassId in CfirExtendSemantics.implicitPrimitiveInterfaceClassIds(context, targetType)
    ) {
        return true
    }

    return false
}

/**
 * 收集 extend 目标类型在实际类型实参替换后的已继承接口 key。
 */
context(context: CheckerContext)
private fun CfirExtend.instantiatedTargetInheritedInterfaceKeys(): Set<String> {
    val targetType = extendedTypeRef.coneTypeOrNull as? ConeClassifierType ?: return emptySet()
    val targetDeclaration = (targetType.toSymbol(context.session) as? CfirClassLikeSymbol<*>)
        ?.cfir as? CfirClassLikeDeclaration ?: return emptySet()
    val substitutor = targetDeclaration.createDeclarationTypeSubstitutor(targetType)
    val result = linkedSetOf<String>()
    val visiting = linkedSetOf<String>()

    for (superTypeRef in targetDeclaration.superTypeRefs) {
        val supertype = superTypeRef.coneTypeOrNull ?: continue
        collectInstantiatedInheritedInterfaceKeys(
            classLikeType = substitutor.substituteOrSelf(supertype),
            result = result,
            visiting = visiting,
        )
    }
    return result
}

/**
 * 递归收集 class-like 类型及其父类型中实例化后的接口 key。
 */
context(context: CheckerContext)
private fun collectInstantiatedInheritedInterfaceKeys(
    classLikeType: ConeCangJieType,
    result: MutableSet<String>,
    visiting: MutableSet<String>,
) {
    val classifierType = classLikeType as? ConeClassifierType ?: return
    val symbol = classifierType.toSymbol(context.session) as? CfirClassLikeSymbol<*> ?: return
    val declaration = symbol.cfir as? CfirClassLikeDeclaration ?: return
    val classLikeKey = classifierType.instantiatedInterfaceKey() ?: return
    if (!visiting.add(classLikeKey)) return
    if (declaration is CfirInterface) {
        result += classLikeKey
    }

    val substitutor = declaration.createDeclarationTypeSubstitutor(classifierType)
    for (superTypeRef in declaration.superTypeRefs) {
        val supertype = superTypeRef.coneTypeOrNull ?: continue
        collectInstantiatedInheritedInterfaceKeys(
            classLikeType = substitutor.substituteOrSelf(supertype),
            result = result,
            visiting = visiting,
        )
    }
    visiting.remove(classLikeKey)
}

/**
 * extend 检查顺序不可判定诊断检查器。
 */
object CfirExtendCheckSequenceChecker : CfirExtendChecker() {
    /**
     * 检查当前 extend 是否处于无法决定检查顺序的规则集合中。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
        val query = context.session.extendRuleQueryServiceOrNull ?: return
        if (!query.hasUndecidableExtendCheckSequence(extend)) return

        reporter.reportOn(
            source = extend.source?.firstCharacterDiagnosticSource() ?: extend.extendedTypeRef.source,
            factory = CfirErrors.EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE,
        )
    }
}

/**
 * extend orphan rule 检查器。
 *
 * 对齐官方 `TypeChecker::TypeCheckerImpl::CheckExtendOrphanRule`
 * （`external/cangjie_compiler/src/Sema/TypeCheckExtend.cpp:519`）：
 * 只有当「被扩展类型来自其他包或是 builtin/primitive」且「本次 extend 引入了
 * 目标尚未具备的其他包接口」时才报错。目标已经具备的接口有两个来源，
 * 二者都不算新引入：
 *
 * 1. 目标类型自身（含父类链）声明继承的接口；
 * 2. 其他包中对目标类型或其父类型所作 extend 引入的接口。
 *
 * 该检查是官方 orphan rule 在 CFIR 中的唯一实现，诊断名与官方
 * `sema_type_cannot_extend_imported_interface` 对齐。
 */
object CfirExtendOrphanRuleChecker : CfirExtendChecker() {
    /**
     * 检查当前 extend 是否违反 orphan rule。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
        val query = context.session.extendRuleQueryServiceOrNull ?: return
        val declarationPackage = query.packageFqNameOf(extend) ?: return

        val targetDeclaration = CfirExtendSemantics.targetDeclaration(context, extend)
        val targetType = extend.extendedTypeRef.coneTypeOrNull
            ?.fullyExpandedTypeUsingAbbreviation(context.session)
        val isPrimitiveTarget = targetType is ConePrimitiveType
        if (!isPrimitiveTarget) {
            if (targetDeclaration == null) return
            if (CfirExtendSemantics.isTargetDeclaredInPackage(context, extend, declarationPackage)) return
        }

        val alreadyAvailableKeys = linkedSetOf<String>()
        query.targetOwnInterfacesOf(extend).mapTo(alreadyAvailableKeys) { it.semanticKey }
        val targetClassId = query.targetClassIdOf(extend)
        if (targetClassId != null) {
            alreadyAvailableKeys += query.otherPackageExtendedInterfaceSemanticKeys(targetClassId, declarationPackage)
        }

        val introducedExternalInterfaces = query.inheritedInterfaceClosureOf(extend).filter { inheritedInterface ->
            val interfaceClassId = inheritedInterface.classId ?: return@filter false
            if (CfirExtendSemantics.isProtectedInterface(interfaceClassId)) return@filter false
            interfaceClassId.packageFqName != declarationPackage &&
                inheritedInterface.semanticKey !in alreadyAvailableKeys
        }
        if (introducedExternalInterfaces.isEmpty()) return

        val targetName = targetDeclaration?.symbol?.classId?.shortClassName
            ?: targetClassId?.shortClassName
            ?: return
        reporter.reportOn(
            source = extend.extendedTypeRef.source ?: extend.source,
            factory = CfirErrors.TYPE_CANNOT_EXTEND_IMPORTED_INTERFACE,
            a = if (isPrimitiveTarget) "primitive" else "imported",
            b = targetName,
        )
    }
}

/**
 * extend 泛型参数使用检查器。
 */
object CfirExtendGenericUsageChecker : CfirExtendChecker() {
    /**
     * 检查 extend 声明的类型参数是否至少出现在目标类型中。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(extend: CfirExtend) {
        if (extend.typeParameters.isEmpty()) return
        val allTypeRefs = listOf(extend.extendedTypeRef)
        val unusedTypeParameters = extend.typeParameters.filter { typeParameter ->
            allTypeRefs.none { typeRef -> typeRef.containsTypeParameter(typeParameter.name.asString()) }
        }
        if (unusedTypeParameters.isEmpty()) return

        reporter.reportOn(
            source = extend.extendedTypeRef.source,
            factory = CfirErrors.EXTEND_GENERIC_USAGE,
            a = unusedTypeParameters.joinToString(", ") { it.name.asString() },
        )
    }
}

/**
 * 不可变目标泄漏 mut 接口检查器。
 */
object CfirExtendImmutableMutInterfaceChecker : CfirExtendChecker() {
    /**
     * 检查不可变目标是否通过 extend super interface 泄漏 mut 接口能力。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirExtend) {
        for (superTypeRef in declaration.superTypeRefs) {
            val leak = CfirExtendSemantics.immutableMutInterfaceLeak(context, declaration, superTypeRef) ?: continue
            val diagnosticSource = declaration.source?.let { source ->
                CjOffsetsOnlySourceElement(source.startOffset, source.endOffset)
            } ?: superTypeRef.source
            reporter.reportOn(
                source = diagnosticSource,
                factory = CfirErrors.EXTEND_INTERFACE_NOT_EXTENDABLE,
                a = leak.interfaceClassId.shortClassName,
            )
        }
    }
}

/**
 * 不可变目标上的 extend 成员合法性检查器。
 */
object CfirExtendImmutableMemberChecker : CfirExtendChecker() {
    /**
     * 检查不可变目标不能声明 mut 属性或 index assignment operator。
     */
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

/**
 * 泛型目标 extend 特化冲突检查器。
 */
object CfirExtendSpecializationConflictChecker : CfirExtendChecker() {
    /**
     * 检查泛型目标不同特化 extend 之间的接口冲突。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirExtend) {
        if (!declaration.requiresSpecializationConflictCheck(context)) return

        val query = context.session.extendRuleQueryServiceOrNull ?: return
        val localTargetType = declaration.semanticExtendedType(context.session) ?: return
        val genericExtends = query
            .extendDeclarationsForNominalTarget(declaration)
            .filterIsInstance<CfirExtend>()
            .filter { candidate ->
                candidate !== declaration && candidate.extendedTypeRef.containsAnyExtendTypeParameter(candidate)
            }

        for (sourceTypeRef in declaration.superTypeRefs) {
            if (sourceTypeRef.containsAnyExtendTypeParameter(declaration)) continue
            val localInterfaceType = sourceTypeRef.semanticExtendType(context.session) as? ConeLookupTagBasedType ?: continue
            if (localInterfaceType.typeArguments.isEmpty()) continue

            val conflict = genericExtends.any genericExtend@{ genericExtend ->
                val genericTargetPattern = genericExtend.extendedTypeRef.coneTypeOrNull ?: return@genericExtend false
                val targetSubstitution = createExtendDeclarationSubstitutionForConstraintDerivation(
                    session = context.session,
                    extend = genericExtend,
                    targetPattern = genericTargetPattern,
                    concreteReceiverType = localTargetType,
                ) ?: return@genericExtend false

                genericExtend.superTypeRefs.any interfaceType@{ genericInterfaceTypeRef ->
                    if (!genericInterfaceTypeRef.containsAnyExtendTypeParameter(genericExtend)) {
                        return@interfaceType false
                    }
                    val genericInterfaceType = genericInterfaceTypeRef.semanticExtendType(context.session)
                        ?: return@interfaceType false
                    val instantiatedInterfaceType = targetSubstitution.substitutor
                        .substituteOrSelf(genericInterfaceType)
                        .semanticExtendType(context.session)
                    AbstractTypeChecker.equalTypes(
                        context.session.typeContext,
                        localInterfaceType,
                        instantiatedInterfaceType,
                    )
                }
            }
            if (!conflict) continue

            reporter.reportOn(
                source = sourceTypeRef.source,
                factory = CfirErrors.EXTEND_DUPLICATE_INTERFACE,
                a = localInterfaceType.classIdOrPrimitiveClassId?.shortClassName ?: sourceTypeRef.toApproxName(),
            )
        }
    }

    /**
     * 官方 `CheckSpecializationExtend` 只检查泛型名义类型的特化版本；
     * primitive built-in 目标只对 `CPointer` 额外执行这一类检查。
     */
    private fun CfirExtend.requiresSpecializationConflictCheck(context: CheckerContext): Boolean {
        val targetType = (extendedTypeRef as? CfirResolvedTypeRef)
            ?.coneType
            ?.semanticExtendType(context.session)
            ?: return false
        return when (targetType) {
            is ConeClassLikeType -> targetType.typeArguments.isNotEmpty()
            is ConeStructType -> targetType.typeArguments.isNotEmpty()
            is ConeEnumType -> targetType.typeArguments.isNotEmpty()
            is ConePointerType -> true
            else -> false
        }
    }
}

/**
 * 泛型接口独立默认成员的重复 extend 检查器。
 *
 * 同一 nominal 目标的多个 extend 以不同实例化形式引入同一泛型接口时，接口中不依赖
 * 接口类型参数的默认成员会成为同一份实现。官方将 shadow 诊断定位到该接口成员原声明。
 */
object CfirExtendDefaultIndependentMemberShadowChecker : CfirExtendChecker() {
    /**
     * 检查当前 extend 与同目标其它 extend 引入接口默认实现是否冲突。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirExtend) {
        val query = context.session.extendRuleQueryServiceOrNull ?: return
        val targetKey = query.targetKeyOf(declaration) ?: return
        if (query.isFirstExtendForTarget(declaration, targetKey)) return
        val localInterfaces = query.inheritedInterfacesOf(declaration)
        if (localInterfaces.isEmpty()) return

        val foreignInterfaceIds = query
            .inheritedInterfacesForTarget(targetKey, excludingDeclaration = declaration)
            .mapNotNull { it.classId }
            .toSet()
        if (foreignInterfaceIds.isEmpty()) return

        for ((index, localInterface) in localInterfaces.withIndex()) {
            val interfaceClassId = localInterface.classId ?: continue
            if (interfaceClassId !in foreignInterfaceIds) continue
            val conflictMembers = query.defaultIndependentMembersOfInterface(interfaceClassId)
            if (conflictMembers.isEmpty()) continue
            val interfaceDeclaration = context.session.symbolProvider
                .getClassLikeSymbolByClassId(interfaceClassId)
                ?.cfir as? CfirInterface ?: continue
            val memberDeclarations = interfaceDeclaration.declarations
                .mapNotNull { member ->
                    when (member) {
                        is CfirNamedFunction -> member.name to member
                        is CfirProperty -> member.name to member
                        else -> null
                    }
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, declarations) -> declarations.iterator() }
            val targetName = query.targetClassIdOf(declaration)?.shortClassName
                ?: declaration.extendedTypeRef.toApproxName()
            for (memberName in conflictMembers) {
                val declarations = memberDeclarations[memberName] ?: continue
                if (!declarations.hasNext()) continue
                val member = declarations.next()
                if (!member.isDefaultIndependentOf(interfaceDeclaration.typeParameters, context)) continue
                val source = when (member) {
                    is CfirNamedFunction -> member.functionNameDiagnosticSource()
                    is CfirProperty -> member.propertyNameDiagnosticSource()
                    else -> null
                }
                reporter.reportOn(
                    source = source ?: member.source ?: declaration.superTypeRefs.getOrNull(index)?.source,
                    factory = CfirErrors.EXTEND_MEMBER_CANNOT_SHADOW,
                    a = memberName,
                    b = targetName,
                )
            }
        }
    }
}

/**
 * 使用 checker 阶段已经推断完成的 callable 类型判断默认成员是否依赖接口类型参数。
 *
 * EXTENSIONS 阶段建立的索引只能保守收录隐式返回类型成员；官方检查读取的是类型检查
 * 完成后的 `member->ty`，因此最终判定必须包含推断返回类型，不能只看早期 type ref。
 */
private fun CfirDeclaration.isDefaultIndependentOf(
    interfaceTypeParameters: List<CfirTypeParameter>,
    context: CheckerContext,
): Boolean {
    val parameterSymbols = interfaceTypeParameters.mapTo(linkedSetOf()) { it.symbol }
    if (parameterSymbols.isEmpty()) return true

    fun ConeCangJieType.isValidAndIndependent(): Boolean =
        !containsErrorType() && !containsAnyTypeParameter(parameterSymbols)

    return when (this) {
        is CfirNamedFunction -> {
            val returnType = context.returnTypeCalculator.tryCalculateReturnType(this).coneType
            if (!returnType.isValidAndIndependent()) return false
            valueParameters.all { parameter ->
                val parameterType = parameter.returnTypeRef.coneTypeOrNull ?: return false
                parameterType.isValidAndIndependent()
            }
        }

        is CfirProperty -> {
            val propertyType = context.returnTypeCalculator.tryCalculateReturnType(this).coneType
            propertyType.isValidAndIndependent()
        }

        else -> false
    }
}

/**
 * 判断类型引用是否确定不是接口类型。
 */
private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.isDefinitelyNotInterfaceType(
    session: CfirSession,
): Boolean = when (this) {
    is CfirBasicTypeRef,
    is CfirImplicitTypeRef -> true
    is CfirResolvedTypeRef -> semanticExtendType(session)?.isDefinitelyNotExtendInterfaceType() == true
    else -> false
}

/**
 * 判断 cone 类型是否确定不能作为 extend super interface。
 */
private fun ConeCangJieType.isDefinitelyNotExtendInterfaceType(): Boolean = when (this) {
    // extend 的接口类型检查对齐官方 PreCheckExtend：类型实例化失败时按 Ty 正确性决定是否派生
    // EXTEND_NOT_INTERFACE。泛型实参数量错误已经有独立诊断，内部错误实参则仍会让整个接口实例无效。
    is ConeErrorType -> when (diagnostic) {
        is ConeUnmatchedTypeArgumentsError -> false
        is ConeUnreportedDuplicateDiagnostic -> true
        else -> false
    }
    else -> if (containsErrorType()) {
        true
    } else when (this) {
    is ConeClassLikeType -> !isInterface
    is ConePrimitiveType,
    is ConeStructType,
    is ConeEnumType -> true
    else -> false
    }
}

/**
 * 判断 extend 目标类型引用是否在未能恢复语义类型时仍确定非法。
 */
private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.isDefinitelyIllegalExtendedType(): Boolean = when (this) {
    is CfirImplicitTypeRef -> true
    is CfirErrorTypeRef -> this.diagnostic !is ConeUnresolvedTypeQualifierError
    else -> false
}

/**
 * 为接口重复检查构造稳定语义 key。
 */
private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.toSemanticStableKey(): String {
    val coneType = (this as? CfirResolvedTypeRef)?.coneType
    return coneType?.toString() ?: toString()
}

/**
 * 判断类型引用中是否包含指定 extend 类型参数。
 */
private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.containsTypeParameter(parameterName: String): Boolean {
    val coneType = (this as? CfirResolvedTypeRef)?.coneType ?: return false
    return coneType.containsTypeParameter(parameterName)
}

/**
 * 判断类型引用中是否包含当前 extend 的任一类型参数。
 */
private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.containsAnyExtendTypeParameter(extend: CfirExtend): Boolean {
    val parameterNames = extend.typeParameters.mapTo(linkedSetOf()) { it.name.asString() }
    if (parameterNames.isEmpty()) return false
    val coneType = (this as? CfirResolvedTypeRef)?.coneType ?: return false
    return parameterNames.any { parameterName -> coneType.containsTypeParameter(parameterName) }
}

/**
 * 判断 cone 类型或其 abbreviated type 中是否包含指定类型参数。
 */
private fun ConeCangJieType.containsTypeParameter(parameterName: String): Boolean =
    abbreviatedType?.containsTypeParameter(parameterName) == true || containsTypeParameterInConstructor(parameterName)

/**
 * 判断 cone 类型构造内部是否包含指定类型参数。
 */
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

/** 在 Cone 类型结构中按符号身份查找接口类型参数。 */
private fun ConeCangJieType.containsAnyTypeParameter(
    parameterSymbols: Set<CfirTypeParameterSymbol>,
): Boolean = when (this) {
    is ConeTypeParameterType -> lookupTag.typeParameterSymbol in parameterSymbols
    is ConeClassLikeType -> typeArguments.any { it.type.containsAnyTypeParameter(parameterSymbols) }
    is ConeStructType -> typeArguments.any { it.type.containsAnyTypeParameter(parameterSymbols) }
    is ConeEnumType -> typeArguments.any { it.type.containsAnyTypeParameter(parameterSymbols) }
    is ConeTypeAliasType -> typeArguments.any { it.type.containsAnyTypeParameter(parameterSymbols) } ||
        (expandedType?.containsAnyTypeParameter(parameterSymbols) == true)
    is ConeFunctionType -> parameterTypes.any { it.containsAnyTypeParameter(parameterSymbols) } ||
        returnType.containsAnyTypeParameter(parameterSymbols)
    is ConeTupleType -> elementTypes.any { it.containsAnyTypeParameter(parameterSymbols) }
    is ConeVArrayType -> elementType.containsAnyTypeParameter(parameterSymbols)
    is ConePointerType -> pointeeType.containsAnyTypeParameter(parameterSymbols)
    is ConeIntersectionType -> intersectedTypes.any { it.containsAnyTypeParameter(parameterSymbols) }
    is ConeUnionType -> unionTypes.any { it.containsAnyTypeParameter(parameterSymbols) }
    else -> arrayElementType?.containsAnyTypeParameter(parameterSymbols) == true
}

/**
 * 为诊断构造类型引用的近似名称。
 */
private fun org.cangnova.cangjie.cfir.types.CfirTypeRef.toApproxName(): Name {
    val classId = CfirExtendSemantics.run { toClassIdOrNull() }
    if (classId != null) return classId.shortClassName

    val raw = toString().substringAfterLast('.').substringBefore('<')
    return Name.identifierIfValid(raw) ?: Name.ERROR_NAME
}
