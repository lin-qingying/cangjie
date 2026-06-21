/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.providers.findExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirCompositeTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExtendMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.extendRuleQueryServiceOrNull
import org.cangnova.cangjie.cfir.session.services.CfirExtendRuleQueryService
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.withReplacedSourceAndType
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 继承深层检查器（InheritanceDeep 分组）
 *
 * 对齐 C++ InheritanceChecker/ 目录：
 * - CANNOT_INHERIT_SEALED: sealed 类只能在同包中被继承
 * - INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC: 抽象类 static 成员未实现
 * - INVALID_MEMBER_VISIBILITY_IN_CLASS: abstract/open 成员必须是 public 或 protected
 *
 * 注册为 classLikeCheckers
 */
object CfirInheritanceDeepChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration is CfirClass) {
            checkSealedInheritanceScope(declaration)
            checkAbstractClassStaticUnimplemented(declaration)
            checkMemberVisibilityNotWiderThanClass(declaration)
        }
        checkInheritedMemberKindConsistency(declaration.memberInheritanceSubject())
        checkSuperMembersKindConsistency(declaration)
        checkInheritedMemberTypeConsistency(declaration)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    internal fun checkExtend(declaration: CfirExtend) {
        checkInheritedMemberKindConsistency(declaration.memberInheritanceSubject())
        checkExtendTargetMemberCompatibility(declaration)
    }

    /**
     * `extend T <: I` 需要让目标类型 `T` 的既有成员满足接口 `I` 的实现约束。
     *
     * 该路径不同于 extend 块内声明成员覆盖接口成员：冲突成员可能已经定义在目标类型上，
     * 因此诊断落在 extend 声明本身。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkExtendTargetMemberCompatibility(extend: CfirExtend) {
        val targetScope = extend.extendedTypeRef.resolvedUseSiteMemberScope(excludingExtend = extend) ?: return
        val targetClassId = (extend.extendedTypeRef as? CfirResolvedTypeRef)
            ?.coneType
            ?.expandedClassIdOrPrimitiveClassId
        val ownMemberInfosByName = extend.declarations
            .mapNotNull { it.directMemberInfoOrNull(context) }
            .groupBy { it.name }
        checkDefaultInterfaceMemberConflicts(extend, targetScope, targetClassId, ownMemberInfosByName)

        val reportedMutConflicts = mutableSetOf<Name>()
        val reportedWeakVisibilities = mutableSetOf<String>()
        val reportedPropertyTypeConflicts = mutableSetOf<String>()
        val reportedPropertyMutabilityConflicts = mutableSetOf<String>()
        val reportedUnimplementedMembers = mutableSetOf<String>()

        for (superTypeRef in extend.superTypeRefs) {
            val superDecl = superTypeRef.resolvedClassLikeDeclaration() ?: continue
            for (superInfo in superTypeRef.collectInterfaceRequirementMemberInfos(superDecl)) {

                val implementationCandidates = buildList {
                    targetScope.processCallablesByName(superInfo.name) { symbol ->
                        symbol.inheritedMemberInfoOrNull(context)?.let { info ->
                            add(ExtendImplementationCandidate(info, extend.extendedTypeRef.source, null))
                        }
                    }
                    if (targetClassId != null) {
                        for (info in collectDirectExtendMemberInfos(targetClassId, superInfo.name, context, excludingExtend = extend)) {
                            add(ExtendImplementationCandidate(info, extend.extendedTypeRef.source, null))
                        }
                    }
                    for (info in ownMemberInfosByName[superInfo.name].orEmpty()) {
                        add(ExtendImplementationCandidate(info, info.nameSource ?: info.source ?: extend.source, info.source))
                    }
                }

                for (candidate in implementationCandidates) {
                    val implementationInfo = candidate.info
                    if (!implementationInfo.canImplement(superInfo)) continue

                    val propertyTypeMismatch = implementationInfo.propertyTypeMismatch(superInfo)
                    if (propertyTypeMismatch != null) {
                        val key = implementationInfo.overrideDiagnosticKey(superInfo)
                        if (reportedPropertyTypeConflicts.add(key)) {
                            reporter.reportOn(
                                source = candidate.declarationSource
                                    ?.firstCharacterDiagnosticSource()
                                    ?: candidate.diagnosticSource,
                                factory = CfirErrors.PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF,
                                a = propertyTypeMismatch.implementationType,
                                b = propertyTypeMismatch.baseType,
                                c = superInfo.name,
                            )
                        }
                    }

                    val propertyMutabilityConflict = if (propertyTypeMismatch == null) {
                        implementationInfo.propertyMutabilityConflict(superInfo)
                    } else {
                        null
                    }
                    if (propertyMutabilityConflict != null) {
                        val key = implementationInfo.overrideDiagnosticKey(superInfo)
                        if (reportedPropertyMutabilityConflicts.add(key)) {
                            reporter.reportOn(
                                source = candidate.declarationSource
                                    ?.firstCharacterDiagnosticSource()
                                    ?: candidate.diagnosticSource,
                                factory = when (propertyMutabilityConflict) {
                                    PropertyMutabilityConflict.MutExpected ->
                                        CfirErrors.PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT

                                    PropertyMutabilityConflict.ImmutExpected ->
                                        CfirErrors.PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT
                                },
                                a = superInfo.name,
                            )
                        }
                    }

                    if (implementationInfo.hasMutFunctionConflict(superInfo) && reportedMutConflicts.add(superInfo.name)) {
                        reporter.reportOn(
                            source = extend.source?.firstCharacterDiagnosticSource() ?: superTypeRef.source,
                            factory = CfirErrors.INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE,
                        )
                    }

                    if (implementationInfo.hasWeakVisibilityComparedTo(superInfo)) {
                        val key = implementationInfo.overrideDiagnosticKey(superInfo)
                        if (reportedWeakVisibilities.add(key)) {
                            reporter.reportOn(
                                source = candidate.diagnosticSource,
                                factory = CfirErrors.WEAK_VISIBILITY,
                                a = superInfo.name,
                                b = superInfo.visibility,
                            )
                        }
                    }
                }

                val hasConcreteImplementation = implementationCandidates.any { candidate ->
                    val implementationInfo = candidate.info
                    implementationInfo.canImplement(superInfo) &&
                        !implementationInfo.isAbstract &&
                        !implementationInfo.isDefaultInterfaceMember(context)
                }
                if (hasConcreteImplementation) continue

                if (superInfo.isAbstract) {
                    val requirementKey = superInfo.requirementDiagnosticKey()
                    if (reportedUnimplementedMembers.add(requirementKey)) {
                        reporter.reportOn(
                            source = extend.source?.firstCharacterDiagnosticSource() ?: extend.extendedTypeRef.source,
                            factory = CfirErrors.NEED_MEMBER_IMPLEMENTATION,
                            a = extend.targetDisplayName(),
                        )
                    }
                }
            }
        }
    }

    /**
     * 多个 default interface member 在同一目标类型的 use-site 发生同签名合并时，
     * extend 必须显式实现该成员。
     *
     * 对齐官方 `MergeInheritedMemberHelper::shouldBeImplemented`：
     * - 不把单个 default member 当作缺实现；
     * - 同一 extend 的接口列表产生重复 default 时需要实现；
     * - 不同 extend 的独立 default 冲突需要分别在相关 extend 上报；
     * - 普通父子接口关系分散到两个非泛型 extend 时不报，但具体/泛型 extend 重叠仍会在具体实例化处报。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkDefaultInterfaceMemberConflicts(
        extend: CfirExtend,
        targetScope: CfirTypeScope,
        targetClassId: ClassId?,
        ownMemberInfosByName: Map<Name, List<InheritedMemberInfo>>,
    ) {
        val receiverType = (extend.extendedTypeRef as? CfirResolvedTypeRef)?.coneType ?: return
        val query = context.session.extendRuleQueryServiceOrNull ?: return
        val targetKey = query.targetKeyOf(extend) ?: return
        val relatedExtends = context.session.extendProvider
            .getExtendsForTarget(targetKey)
            .filter(context.session.extendProvider::isExtendAccessible)
        if (relatedExtends.isEmpty()) return

        val occurrencesBySignature = relatedExtends
            .flatMap { relatedExtend ->
                relatedExtend.collectDefaultInterfaceMemberOccurrencesAtReceiver(receiverType)
            }
            .groupBy { it.info.defaultImplementationConflictKey() }

        val reported = mutableSetOf<String>()
        for ((signatureKey, occurrences) in occurrencesBySignature) {
            val currentOccurrences = occurrences.filter { it.ownerExtend === extend }
            if (currentOccurrences.isEmpty()) continue
            if (!occurrences.hasDefaultImplementationConflictFor(extend, currentOccurrences, query)) continue

            val representative = currentOccurrences.first().info
            if (hasConcreteImplementation(
                    representative,
                    extend,
                    targetScope,
                    targetClassId,
                    ownMemberInfosByName,
                )
            ) {
                continue
            }
            if (!reported.add(signatureKey)) continue

            reporter.reportOn(
                source = extend.source?.firstCharacterDiagnosticSource() ?: extend.extendedTypeRef.source,
                factory = CfirErrors.INTERFACE_MEMBER_MUST_BE_IMPLEMENTED,
                a = representative.kind,
                b = representative.name,
                c = extend.targetDisplayName(),
            )
        }
    }

    context(context: CheckerContext)
    private fun CfirExtend.collectDefaultInterfaceMemberOccurrencesAtReceiver(
        receiverType: ConeCangJieType,
    ): List<DefaultInterfaceMemberOccurrence> {
        val substitution = findExtendDeclarationSubstitution(context.session, this, receiverType)
            ?: return emptyList()
        return buildList {
            for (superTypeRef in superTypeRefs) {
                val substitutedSuperTypeRef = superTypeRef.substituteForDefaultConflict(substitution.substitutor)
                val superDecl = substitutedSuperTypeRef.resolvedClassLikeDeclaration() ?: continue
                for (info in substitutedSuperTypeRef.collectInterfaceRequirementMemberInfos(superDecl)) {
                    if (info.isDefaultInterfaceMember(context)) {
                        add(DefaultInterfaceMemberOccurrence(this@collectDefaultInterfaceMemberOccurrencesAtReceiver, info))
                    }
                }
            }
        }
    }

    private fun CfirTypeRef.substituteForDefaultConflict(substitutor: ConeSubstitutor): CfirTypeRef {
        val resolvedTypeRef = this as? CfirResolvedTypeRef ?: return this
        val substitutedType = substitutor.substituteOrSelf(resolvedTypeRef.coneType)
        if (substitutedType == resolvedTypeRef.coneType) return this
        return resolvedTypeRef.withReplacedSourceAndType(resolvedTypeRef.source, substitutedType)
    }

    private fun List<DefaultInterfaceMemberOccurrence>.hasDefaultImplementationConflictFor(
        extend: CfirExtend,
        currentOccurrences: List<DefaultInterfaceMemberOccurrence>,
        query: CfirExtendRuleQueryService,
    ): Boolean {
        if (currentOccurrences.size > 1) return true

        val hasGenericRelatedExtend = any { occurrence ->
            occurrence.ownerExtend !== extend &&
                occurrence.ownerExtend.extendedTypeUsesOwnTypeParameter()
        }
        if (hasGenericRelatedExtend && !extend.extendedTypeUsesOwnTypeParameter()) return true

        val originalSymbols = mapNotNull { occurrence ->
            occurrence.info.symbol?.unwrapSubstitutionOverrides()
        }.toSet()
        if (originalSymbols.size <= 1) return false

        val hasIndependentDefault = any { occurrence ->
            occurrence.ownerExtend !== extend &&
                !query.areExtendsInInheritRelation(extend, occurrence.ownerExtend)
        }
        if (hasIndependentDefault) return true
        return false
    }

    context(context: CheckerContext)
    private fun hasConcreteImplementation(
        superInfo: InheritedMemberInfo,
        extend: CfirExtend,
        targetScope: CfirTypeScope,
        targetClassId: ClassId?,
        ownMemberInfosByName: Map<Name, List<InheritedMemberInfo>>,
    ): Boolean {
        var found = false
        targetScope.processCallablesByName(superInfo.name) { symbol ->
            val info = symbol.inheritedMemberInfoOrNull(context) ?: return@processCallablesByName
            if (info.isConcreteImplementationOf(superInfo, context)) {
                found = true
            }
        }
        if (found) return true
        if (targetClassId != null) {
            for (info in collectDirectExtendMemberInfos(targetClassId, superInfo.name, context, excludingExtend = extend)) {
                if (info.isConcreteImplementationOf(superInfo, context)) return true
            }
        }
        for (info in ownMemberInfosByName[superInfo.name].orEmpty()) {
            if (info.isConcreteImplementationOf(superInfo, context)) return true
        }
        return false
    }

    private fun CfirExtend.extendedTypeUsesOwnTypeParameter(): Boolean {
        val parameterNames = typeParameters.mapTo(linkedSetOf()) { it.name.asString() }
        if (parameterNames.isEmpty()) return false
        val coneType = (extendedTypeRef as? CfirResolvedTypeRef)?.coneType ?: return false
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

    private fun InheritedMemberInfo.isConcreteImplementationOf(
        superInfo: InheritedMemberInfo,
        context: CheckerContext,
    ): Boolean =
        canImplement(superInfo) && !isAbstract && !isDefaultInterfaceMember(context)

    private fun InheritedMemberInfo.defaultImplementationConflictKey(): String =
        requirementDiagnosticKey()

    /**
     * extend 实现接口时必须检查接口完整 use-site 成员表，而不仅是接口直接声明。
     * 官方 `GetInheritedSuperMembers` 会把 super interface 的成员一并放入待实现集合。
     */
    context(context: CheckerContext)
    private fun CfirTypeRef.collectInterfaceRequirementMemberInfos(
        superDecl: CfirClassLikeDeclaration,
    ): List<InheritedMemberInfo> =
        buildMap {
            val superScope = resolvedUseSiteMemberScope() ?: context.createUseSiteMemberScope(superDecl)
            for (info in superScope.collectInheritedMemberInfos(context)) {
                put(info.requirementDiagnosticKey(), info)
            }
        }.values.toList()

    private fun CfirTypeScope.collectInheritedMemberInfos(context: CheckerContext): List<InheritedMemberInfo> =
        buildList {
            for (name in getCallableNames()) {
                processCallablesByName(name) { symbol ->
                    symbol.inheritedMemberInfoOrNull(context)?.let(::add)
                }
            }
        }

    /**
     * 多个父类型中同名成员的声明类型（function/property）不一致。
     *
     * 对齐 C++ sema_inherit_super_member_kind_inconsistent
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSuperMembersKindConsistency(classDecl: CfirClassLikeDeclaration) {
        val kindsByName = mutableMapOf<Name, MutableSet<String>>()
        for (superTypeRef in classDecl.superTypeRefs) {
            val type = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (type is ConeErrorType) continue
            val classId = (type as? ConeClassLikeType)?.classId ?: continue
            val superDecl = context.session.symbolProvider
                .getClassLikeSymbolByClassId(classId)?.cfir as? CfirClassLikeDeclaration ?: continue
            val superScope = context.createUseSiteMemberScope(superDecl)
            for (name in superScope.getCallableNames()) {
                superScope.processCallablesByName(name) { symbol ->
                    val info = symbol.inheritedMemberInfoOrNull(context) ?: return@processCallablesByName
                    kindsByName.getOrPut(info.name) { mutableSetOf() }.add(info.kind)
                }
            }
        }
        for ((name, kinds) in kindsByName) {
            if (kinds.size > 1) {
                reporter.reportOn(
                    source = classDecl.source,
                    factory = CfirErrors.INHERIT_SUPER_MEMBER_KIND_INCONSISTENT,
                    a = name,
                )
            }
        }
    }

    /**
     * 多个父类型中同名函数成员的返回类型不一致（且非子类型关系）。
     *
     * 对齐 C++ sema_inherit_member_type_inconsistent
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInheritedMemberTypeConsistency(classDecl: CfirClassLikeDeclaration) {
        val ownerClassId = (classDecl.symbol as? CfirClassLikeSymbol<*>)?.classId
        val classScope = context.createUseSiteMemberScope(classDecl)
        for (name in classScope.getCallableNames()) {
            val inheritedFunctions = mutableListOf<CfirFunctionSymbol<*>>()
            classScope.processFunctionsByName(name) { symbol ->
                if (symbol.ownerClassId(context) != ownerClassId) {
                    inheritedFunctions += symbol
                }
            }
            val bySignature = inheritedFunctions
                .filter { it.isBound }
                .groupBy { it.overrideSignatureKey() }
            for ((_, symbols) in bySignature) {
                val returnTypes = symbols.mapNotNull { it.resolvedReturnTypeOrNull(context) }
                    .filterNot { it is ConeErrorType }
                if (returnTypes.size < 2 || !returnTypes.hasInconsistentInheritedTypes(context)) continue
                reporter.reportOn(
                    source = classDecl.source,
                    factory = CfirErrors.INHERIT_MEMBER_TYPE_INCONSISTENT,
                    a = "return types",
                    b = "function",
                    c = name,
                )
                return
            }
        }
    }

    private fun CfirFunctionSymbol<*>.resolvedReturnTypeOrNull(
        context: CheckerContext,
    ): ConeCangJieType? {
        if (!isBound) return null
        (cfir.returnTypeRef as? CfirResolvedTypeRef)?.coneType?.let { return it }
        return context.returnTypeCalculator.tryCalculateReturnType(cfir).coneType
    }

    private fun List<ConeCangJieType>.hasInconsistentInheritedTypes(context: CheckerContext): Boolean {
        val typeCheckerState = context.session.typeContext
        for (i in indices) {
            for (j in i + 1 until size) {
                val first = this[i]
                val second = this[j]
                if (AbstractTypeChecker.equalTypes(typeCheckerState, first, second)) continue
                val related = AbstractTypeChecker.isSubtypeOf(typeCheckerState, first, second) ||
                        AbstractTypeChecker.isSubtypeOf(typeCheckerState, second, first)
                if (!related) return true
            }
        }
        return false
    }

    /**
     * sealed 类只能在同一个包中被继承。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkSealedInheritanceScope(classDecl: CfirClass) {
        for (superTypeRef in classDecl.superTypeRefs) {
            val resolvedType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (resolvedType is ConeErrorType) continue
            val superClassId = (resolvedType as? ConeClassLikeType)?.classId ?: continue
            val superSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(superClassId) ?: continue
            val superDecl = superSymbol.cfir as? CfirClass ?: continue

            if (superDecl.status.isSealed) {
                val superPackage = superClassId.packageFqName
                val currentPackage = classDecl.symbol.classId.packageFqName
                if (superPackage != currentPackage) {
                    reporter.reportOn(
                        source = superTypeRef.source ?: classDecl.source,
                        factory = CfirErrors.CANNOT_INHERIT_SEALED,
                        a = "class",
                        b = classDecl.name.asString(),
                        c = "sealed class",
                        d = superDecl.name,
                    )
                }
            }
        }
    }

    /**
     * 继承抽象类时，父类的 static 抽象函数必须被实现。
     *
     * 对齐 C++ DiagKind::sema_inherit_abstract_class_static_unimplement_func
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAbstractClassStaticUnimplemented(classDecl: CfirClass) {
        if (classDecl.status.isAbstract) return // 抽象类本身不需要实现

        for (superTypeRef in classDecl.superTypeRefs) {
            val resolvedType = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (resolvedType is ConeErrorType) continue
            val superClassId = (resolvedType as? ConeClassLikeType)?.classId ?: continue
            val superSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(superClassId) ?: continue
            val superDecl = superSymbol.cfir as? CfirClass ?: continue
            if (!superDecl.status.isAbstract) continue

            // 查找父类中的 static abstract 函数
            for (superMember in superDecl.declarations) {
                if (superMember !is CfirNamedFunction) continue
                if (!superMember.status.isStatic || !superMember.status.isAbstract) continue

                // 检查子类是否实现了该 static 函数
                val implemented = classDecl.declarations.any { member ->
                    member is CfirNamedFunction &&
                        member.status.isStatic &&
                        member.name == superMember.name &&
                        member.body != null
                }
                if (!implemented) {
                    reporter.reportOn(
                        source = classDecl.source,
                        factory = CfirErrors.INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC,
                        a = classDecl.name,
                        b = "static function",
                        c = superMember.name,
                    )
                }
            }
        }
    }

    /**
     * abstract/open 成员必须是 public 或 protected。
     *
     * 对齐 C++ DeclAttributeChecker.cpp:
     * - 抽象类中的 abstract 成员不能是 private/internal；
     * - 可继承类中的 open 成员不能是 private/internal。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkMemberVisibilityNotWiderThanClass(classDecl: CfirClass) {
        val classIsAbstract = classDecl.status.isAbstract
        val classIsInheritable = classDecl.status.isOpen || classDecl.status.isAbstract
        for (member in classDecl.declarations) {
            val memberInfo = when (member) {
                is CfirNamedFunction -> InvalidVisibilityMemberInfo(
                    visibility = member.status.visibility,
                    modifier = member.invalidVisibilityModifier(classIsAbstract, classIsInheritable),
                    kind = "function",
                    source = member.functionNameDiagnosticSource(),
                )

                is CfirProperty -> InvalidVisibilityMemberInfo(
                    visibility = member.status.visibility,
                    modifier = member.invalidVisibilityModifier(classIsAbstract, classIsInheritable),
                    kind = "property",
                    source = member.propertyNameDiagnosticSource(),
                )
                else -> continue
            }
            if (memberInfo.modifier != null &&
                memberInfo.visibility != Visibilities.Public &&
                memberInfo.visibility != Visibilities.Protected
            ) {
                reporter.reportOn(
                    source = memberInfo.source ?: member.source ?: classDecl.source,
                    factory = CfirErrors.INVALID_MEMBER_VISIBILITY_IN_CLASS,
                    a = memberInfo.modifier,
                    b = memberInfo.kind,
                )
            }
        }
    }

    private data class InvalidVisibilityMemberInfo(
        val visibility: Visibility,
        val modifier: String?,
        val kind: String,
        val source: AbstractCjSourceElement?,
    )

    private fun CfirNamedFunction.invalidVisibilityModifier(
        classIsAbstract: Boolean,
        classIsInheritable: Boolean,
    ): String? {
        if (status.isStatic) return null
        if (status.isAbstract && classIsAbstract) return "abstract"
        if (status.isOpen && classIsInheritable) return "open"
        return null
    }

    private fun CfirProperty.invalidVisibilityModifier(
        classIsAbstract: Boolean,
        classIsInheritable: Boolean,
    ): String? {
        if (status.isStatic) return null
        if (status.isAbstract && classIsAbstract) return "abstract"
        if (status.isOpen && classIsInheritable) return "open"
        return null
    }

    /**
     * 检查继承的同名成员之间的声明类型（函数/属性）一致性。
     *
     * 对齐 C++ InheritanceChecker:
     * - INHERIT_MEMBER_KIND_INCONSISTENT: 子类成员类型与父类同名成员不一致
     * - INHERIT_SUPER_MEMBER_KIND_INCONSISTENT: 多个父类型的同名成员类型不一致
     * - INHERIT_MEMBER_TYPE_INCONSISTENT: 多个父类型的同名成员返回类型不一致
     * - INHERIT_NOT_RETURN_THIS: open 函数返回 This 类型时 override 必须保持
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkInheritedMemberKindConsistency(subject: MemberInheritanceSubject) {
        val ownMembers = subject.declarations.mapNotNull { member ->
            when (member) {
                is CfirNamedFunction -> InheritedMemberInfo(
                    name = member.name,
                    kind = "function",
                    isStatic = member.status.isStatic,
                    isConst = member.status.isConst,
                    isMut = member.status.isMut,
                    isDefault = member.status.isDefault,
                    isAbstract = member.status.isAbstract,
                    visibility = member.status.visibility,
                    source = member.source,
                    nameSource = member.functionNameDiagnosticSource(),
                    ownerName = null,
                    symbol = member.symbol,
                )

                is CfirProperty -> InheritedMemberInfo(
                    name = member.name,
                    kind = member.inheritanceMemberKind(),
                    isStatic = member.status.isStatic,
                    isConst = member.status.isConst,
                    isMut = member.status.isMut,
                    isDefault = member.status.isDefault,
                    isAbstract = member.status.isAbstract,
                    visibility = member.status.visibility,
                    source = member.source,
                    nameSource = member.propertyNameDiagnosticSource(),
                    ownerName = null,
                    symbol = member.symbol,
                )

                is CfirFieldVariable -> InheritedMemberInfo(
                    name = member.name,
                    kind = "variable",
                    isStatic = member.status.isStatic,
                    isConst = false,
                    isMut = false,
                    isDefault = false,
                    isAbstract = member.status.isAbstract,
                    visibility = member.status.visibility,
                    source = member.source,
                    nameSource = member.fieldVariableNameDiagnosticSource(),
                    ownerName = null,
                    symbol = member.symbol,
                )

                else -> null
            }
        }.groupBy { it.name }

        val reportedStaticConflicts = mutableSetOf<Name>()
        val reportedKindConflicts = mutableSetOf<Name>()
        val reportedConstConflicts = mutableSetOf<String>()
        val reportedMutConflicts = mutableSetOf<String>()
        val reportedVariableShadows = mutableSetOf<Name>()
        val reportedCannotOverrides = mutableSetOf<String>()
        val reportedExtendOverrides = mutableSetOf<String>()
        for (inheritedSource in subject.inheritedSources) {
            val superType = (inheritedSource.typeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (superType is ConeErrorType) continue
            val superClassId = (superType as? ConeClassLikeType)?.classId ?: continue
            val superSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(superClassId) ?: continue
            val superDecl = superSymbol.cfir as? CfirClassLikeDeclaration ?: continue
            val superScope = context.createUseSiteMemberScope(superDecl)

            for (name in ownMembers.keys) {
                val superInfos = buildList {
                    superScope.processCallablesByName(name) { symbol ->
                        symbol.inheritedMemberInfoOrNull(context)?.let(::add)
                    }
                    if (inheritedSource.includeDirectExtends) {
                        addAll(collectDirectExtendMemberInfos(superClassId, name, context))
                    }
                }
                for (superInfo in superInfos) {
                    val classDecl = subject.classLikeDeclaration
                    if (classDecl != null && superInfo.symbol?.isVisibleIn(classDecl, context) == false) {
                        continue
                    }

                    val ownSameNameMembers = ownMembers[superInfo.name].orEmpty()

                    for (ownInfo in ownSameNameMembers) {
                        val hasStaticConflict = ownInfo.isStatic != superInfo.isStatic
                        if (hasStaticConflict) {
                            if (reportedStaticConflicts.add(ownInfo.name)) {
                                reporter.reportOn(
                                    source = ownInfo.source?.firstCharacterDiagnosticSource()
                                        ?: ownInfo.nameSource
                                        ?: subject.source,
                                    factory = CfirErrors.INHERIT_MEMBER_KIND_INCONSISTENT,
                                    a = ownInfo.staticKind,
                                    b = ownInfo.name,
                                    c = superInfo.staticKind,
                                    d = superInfo.ownerName ?: superClassId.shortClassName,
                                )
                            }
                        }

                        if (ownInfo.kind != superInfo.kind) {
                            if (!hasStaticConflict && reportedKindConflicts.add(ownInfo.name)) {
                                reporter.reportOn(
                                    source = ownInfo.nameSource ?: subject.source,
                                    factory = CfirErrors.INHERIT_MEMBER_KIND_INCONSISTENT,
                                    a = ownInfo.kind,
                                    b = ownInfo.name,
                                    c = superInfo.kind,
                                    d = superInfo.ownerName ?: superClassId.shortClassName,
                                )
                            }
                            continue
                        }

                        if (!hasStaticConflict && ownInfo.hasConstFunctionConflict(superInfo)) {
                            val key = ownInfo.overrideDiagnosticKey(superInfo)
                            if (reportedConstConflicts.add(key)) {
                                reporter.reportOn(
                                    source = ownInfo.nameSource ?: ownInfo.source ?: subject.source,
                                    factory = CfirErrors.INHERIT_MEMBER_KIND_INCONSISTENT,
                                    a = "non-constant function",
                                    b = ownInfo.name,
                                    c = "'const' function",
                                    d = superInfo.ownerName ?: superClassId.shortClassName,
                                )
                            }
                        }

                        if (!hasStaticConflict && ownInfo.hasMutFunctionConflict(superInfo)) {
                            val key = ownInfo.overrideDiagnosticKey(superInfo)
                            if (reportedMutConflicts.add(key)) {
                                reporter.reportOn(
                                    source = ownInfo.source?.firstCharacterDiagnosticSource()
                                        ?: ownInfo.nameSource
                                        ?: subject.source,
                                    factory = CfirErrors.INCOMPATIBLE_MUT_MODIFIER_BETWEEN_STRUCT_AND_INTERFACE,
                                )
                            }
                        }

                        if (!hasStaticConflict && ownInfo.kind == "variable" && reportedVariableShadows.add(ownInfo.name)) {
                            reporter.reportOn(
                                source = ownInfo.source?.firstCharacterDiagnosticSource() ?: subject.source,
                                factory = CfirErrors.MEMBER_VARIABLE_CAN_NOT_SHADOW,
                                a = ownInfo.name,
                            )
                        }

                        if (subject.classLikeDeclaration != null && ownInfo.overridesExtendMember(superInfo, context)) {
                            val key = ownInfo.overrideDiagnosticKey(superInfo)
                            if (reportedExtendOverrides.add(key)) {
                                reporter.reportOn(
                                    source = ownInfo.nameSource ?: ownInfo.source ?: subject.source,
                                    factory = CfirErrors.EXTEND_FUNCTION_CANNOT_OVERRIDDEN,
                                    a = ownInfo.kind,
                                    b = ownInfo.name,
                                )
                            }
                            continue
                        }

                        if (classDecl != null && ownInfo.canNotOverride(superInfo, classDecl, context)) {
                            val key = ownInfo.overrideDiagnosticKey(superInfo)
                            if (reportedCannotOverrides.add(key)) {
                                reporter.reportOn(
                                    source = ownInfo.nameSource ?: ownInfo.source ?: subject.source,
                                    factory = CfirErrors.CANNOT_OVERRIDE,
                                    a = ownInfo.kind,
                                    b = ownInfo.name,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun CfirDeclaration.inheritedMemberInfoOrNull(context: CheckerContext): InheritedMemberInfo? =
        when (this) {
            is CfirNamedFunction -> symbol?.inheritedMemberInfoOrNull(context)
            is CfirProperty -> symbol.inheritedMemberInfoOrNull(context)
            is CfirFieldVariable -> symbol.inheritedMemberInfoOrNull(context)
            else -> null
        }

    private fun CfirDeclaration.directMemberInfoOrNull(context: CheckerContext): InheritedMemberInfo? =
        when (this) {
            is CfirNamedFunction -> symbol?.directMemberInfoOrNull(context)
            is CfirProperty -> symbol.directMemberInfoOrNull(context)
            is CfirFieldVariable -> symbol.directMemberInfoOrNull(context)
            else -> null
        }

    private fun CfirDeclaration.inheritableDirectMemberInfoOrNull(context: CheckerContext): InheritedMemberInfo? =
        when (this) {
            is CfirNamedFunction -> symbol?.inheritedMemberInfoOrNull(context)
            is CfirProperty -> symbol.inheritedMemberInfoOrNull(context)
            is CfirFieldVariable -> symbol.inheritedMemberInfoOrNull(context)
            else -> null
        }

    private fun CfirCallableSymbol<*>.directMemberInfoOrNull(context: CheckerContext): InheritedMemberInfo? =
        memberInfoOrNull(context, inheritOnly = false)

    private fun CfirCallableSymbol<*>.inheritedMemberInfoOrNull(context: CheckerContext): InheritedMemberInfo? {
        return memberInfoOrNull(context, inheritOnly = true)
    }

    private fun CfirCallableSymbol<*>.memberInfoOrNull(
        context: CheckerContext,
        inheritOnly: Boolean,
    ): InheritedMemberInfo? {
        if (!isBound) return null
        val declaration = cfir
        if (inheritOnly && !canBeInheritedMember()) return null
        val ownerDeclaration = context.ownerClassSymbol(this)?.cfir
        val ownerName = ownerClassId(context)?.shortClassName
        return when (declaration) {
            is CfirNamedFunction -> InheritedMemberInfo(
                name = declaration.name,
                kind = "function",
                isStatic = declaration.status.isStatic,
                isConst = declaration.status.isConst,
                isMut = declaration.status.isMut,
                isDefault = declaration.status.isDefault,
                isAbstract = declaration.requiresInterfaceImplementation(ownerDeclaration),
                visibility = declaration.status.visibility,
                source = declaration.source,
                nameSource = declaration.functionNameDiagnosticSource(),
                ownerName = ownerName,
                symbol = this,
            )

            is CfirProperty -> InheritedMemberInfo(
                name = declaration.name,
                kind = declaration.inheritanceMemberKind(),
                isStatic = declaration.status.isStatic,
                isConst = declaration.status.isConst,
                isMut = declaration.status.isMut,
                isDefault = declaration.status.isDefault,
                isAbstract = declaration.requiresInterfaceImplementation(ownerDeclaration),
                visibility = declaration.status.visibility,
                source = declaration.source,
                nameSource = declaration.propertyNameDiagnosticSource(),
                ownerName = ownerName,
                symbol = this,
            )

            is CfirFieldVariable -> InheritedMemberInfo(
                name = declaration.name,
                kind = "variable",
                isStatic = declaration.status.isStatic,
                isConst = false,
                isMut = false,
                isDefault = false,
                isAbstract = declaration.status.isAbstract,
                visibility = declaration.status.visibility,
                source = declaration.source,
                nameSource = declaration.fieldVariableNameDiagnosticSource(),
                ownerName = ownerName,
                symbol = this,
            )

            else -> null
        }
    }

    private fun CfirCallableDeclaration.requiresInterfaceImplementation(
        ownerDeclaration: CfirClassLikeDeclaration?,
    ): Boolean {
        if (status.isAbstract) return true
        if (ownerDeclaration !is CfirInterface) return false
        return !hasOwnBodyOrAccessorBody()
    }

    private fun CfirCallableDeclaration.hasOwnBodyOrAccessorBody(): Boolean =
        when (this) {
            is CfirFunction -> body != null
            is CfirProperty -> getter?.body != null || setter?.body != null
            else -> true
        }

    /**
     * 官方 GetInheritedSuperMembers 会把父类型可见 extend 的成员并入继承成员表。
     * 本项目 declaration-site scope 不承担这件事，因此继承诊断在这里显式读取 extendProvider。
     */
    private fun collectDirectExtendMemberInfos(
        superClassId: ClassId,
        name: Name,
        context: CheckerContext,
        excludingExtend: CfirExtend? = null,
    ): List<InheritedMemberInfo> {
        val provider = context.session.extendProvider
        return buildList {
            for (extend in provider.getExtendsForClass(superClassId)) {
                if (extend === excludingExtend) continue
                if (!provider.isExtendAccessible(extend)) continue
                for (member in extend.declarations) {
                    when (member) {
                        is CfirNamedFunction -> {
                            if (member.name != name) continue
                            member.symbol?.inheritedMemberInfoOrNull(context)?.let(::add)
                        }

                        is CfirProperty -> {
                            if (member.name != name) continue
                            member.symbol.inheritedMemberInfoOrNull(context)?.let(::add)
                        }

                        else -> Unit
                    }
                }
            }
        }
    }

    /**
     * 官方继承检查在收集 inherited member 后会执行 RemoveMembersShouldNotInherit，
     * private 成员不会进入后续同名成员、类型一致性和 shadow 检查。
     * 继承诊断还需要在 use-site 处按派生类视角过滤 internal/package 可见性，
     * 该过滤在 [checkInheritedMemberKindConsistency] 拿到 subject 后完成。
     */
    private fun CfirCallableSymbol<*>.canBeInheritedMember(): Boolean =
        cfir.status.visibility != Visibilities.Private

    /**
     * 官方 CheckInheritanceAttributes 在同 kind 的函数/属性形成覆盖关系后检查父成员开放性。
     * 非 abstract、非 static、非 interface 且没有 open 语义的父成员不能被子成员覆盖。
     */
    private fun InheritedMemberInfo.canNotOverride(
        superInfo: InheritedMemberInfo,
        classDecl: CfirClassLikeDeclaration,
        context: CheckerContext,
    ): Boolean {
        if (kind != superInfo.kind) return false
        if (kind != "function" && kind != "property") return false
        if (!hasSameOverrideSignature(superInfo)) return false

        val superSymbol = superInfo.symbol ?: return false
        if (!superSymbol.isBound) return false
        if (superSymbol.cfir.status.isStatic) return false
        if (superSymbol.isAbstractLike(context)) return false
        if (superSymbol.isOverridableFrom(classDecl, context)) return false

        return true
    }

    /**
     * 官方 CheckExtendMemberValid 会先处理“类成员覆盖父类型 extend 成员”的情况，
     * 报 sema_extend_function_cannot_overridden 后不再进入普通 cannot-override 检查。
     */
    private fun InheritedMemberInfo.overridesExtendMember(
        superInfo: InheritedMemberInfo,
        context: CheckerContext,
    ): Boolean {
        if (kind != superInfo.kind) return false
        if (kind != "function" && kind != "property") return false
        if (!hasSameOverrideSignature(superInfo)) return false
        val superSymbol = superInfo.symbol ?: return false
        val originalSuperSymbol = superSymbol.unwrapSubstitutionOverrides()
        return context.session.extendProvider.getContainingExtend(originalSuperSymbol) != null
    }

    private fun InheritedMemberInfo.hasSameOverrideSignature(superInfo: InheritedMemberInfo): Boolean {
        if (kind == "property") return true
        val ownSymbol = symbol ?: return false
        val superSymbol = superInfo.symbol ?: return false
        return ownSymbol.overrideSignatureKey() == superSymbol.overrideSignatureKey()
    }

    private fun InheritedMemberInfo.canImplement(superInfo: InheritedMemberInfo): Boolean {
        if (kind != superInfo.kind) return false
        if (isStatic != superInfo.isStatic) return false
        if (kind != "function" && kind != "property") return false
        return hasSameOverrideSignature(superInfo)
    }

    private fun InheritedMemberInfo.hasWeakVisibilityComparedTo(superInfo: InheritedMemberInfo): Boolean {
        if (!canImplement(superInfo)) return false
        val compareResult = Visibilities.compare(visibility, superInfo.visibility)
        return compareResult == null || compareResult < 0
    }

    context(context: CheckerContext)
    private fun InheritedMemberInfo.propertyTypeMismatch(
        superInfo: InheritedMemberInfo,
    ): PropertyTypeMismatch? {
        if (kind != "property" || superInfo.kind != "property") return null
        val implementationSymbol = symbol as? CfirPropertySymbol ?: return null
        val baseSymbol = superInfo.symbol as? CfirPropertySymbol ?: return null
        if (!implementationSymbol.isBound || !baseSymbol.isBound) return null

        val implementationType = context.returnTypeCalculator.tryCalculateReturnType(implementationSymbol.cfir).coneType
        val baseType = context.returnTypeCalculator.tryCalculateReturnType(baseSymbol.cfir).coneType
        if (implementationType is ConeErrorType || baseType is ConeErrorType) return null
        if (AbstractTypeChecker.equalTypes(context.session.typeContext, implementationType, baseType)) return null

        return PropertyTypeMismatch(
            implementationType = implementationType,
            baseType = baseType,
        )
    }

    private fun InheritedMemberInfo.propertyMutabilityConflict(
        superInfo: InheritedMemberInfo,
    ): PropertyMutabilityConflict? {
        if (kind != "property" || superInfo.kind != "property") return null
        return when {
            superInfo.isMut && !isMut -> PropertyMutabilityConflict.MutExpected

            !superInfo.isMut && isMut -> PropertyMutabilityConflict.ImmutExpected

            else -> null
        }
    }

    private fun InheritedMemberInfo.hasConstFunctionConflict(superInfo: InheritedMemberInfo): Boolean {
        if (kind != "function" || superInfo.kind != "function") return false
        if (isConst || !superInfo.isConst) return false
        return hasSameOverrideSignature(superInfo)
    }

    private fun InheritedMemberInfo.hasMutFunctionConflict(superInfo: InheritedMemberInfo): Boolean {
        if (kind != "function" || superInfo.kind != "function") return false
        if (isMut == superInfo.isMut) return false
        return hasSameOverrideSignature(superInfo)
    }

    private fun InheritedMemberInfo.overrideDiagnosticKey(superInfo: InheritedMemberInfo): String =
        buildString {
            append(kind)
            append(':')
            append(name.asString())
            append(':')
            append(symbol?.overrideSignatureKey().orEmpty())
            append(':')
            append(superInfo.symbol?.overrideSignatureKey().orEmpty())
        }

    private fun InheritedMemberInfo.requirementDiagnosticKey(): String =
        buildString {
            append(kind)
            append(':')
            append(name.asString())
            append(':')
            append(isStatic)
            append(':')
            append(symbol?.overrideSignatureKey().orEmpty())
        }

    private fun InheritedMemberInfo.isDefaultInterfaceMember(context: CheckerContext): Boolean {
        if (!isDefault) return false
        val owner = symbol?.let { context.ownerClassSymbol(it)?.cfir }
        return owner is CfirInterface
    }

    context(context: CheckerContext)
    private fun CfirExtend.targetDisplayName(): String =
        "extend " + ((extendedTypeRef as? CfirResolvedTypeRef)?.coneType?.let { type ->
            type.classIdOrPrimitiveClassId?.shortClassName?.asString() ?: type.toString()
        } ?: "<unknown>")

    private data class InheritedMemberInfo(
        val name: Name,
        val kind: String,
        val isStatic: Boolean,
        val isConst: Boolean,
        val isMut: Boolean,
        val isDefault: Boolean,
        val isAbstract: Boolean,
        val visibility: Visibility,
        val source: CjSourceElement?,
        val nameSource: AbstractCjSourceElement?,
        val ownerName: Name?,
        val symbol: CfirCallableSymbol<*>?,
    ) {
        val staticKind: String get() = if (isStatic) "static" else "non-static"
    }

    private data class ExtendImplementationCandidate(
        val info: InheritedMemberInfo,
        val diagnosticSource: AbstractCjSourceElement?,
        val declarationSource: CjSourceElement?,
    )

    private data class DefaultInterfaceMemberOccurrence(
        val ownerExtend: CfirExtend,
        val info: InheritedMemberInfo,
    )

    private data class PropertyTypeMismatch(
        val implementationType: ConeCangJieType,
        val baseType: ConeCangJieType,
    )

    private enum class PropertyMutabilityConflict {
        MutExpected,
        ImmutExpected,
    }

    /**
     * 主构造 `let/var` 参数在 CFIR 中复用 property 结构承载 getter/setter，
     * 但官方继承检查把它们作为 VAR_DECL 参与同名成员 shadow 规则。
     */
    private fun CfirProperty.inheritanceMemberKind(): String =
        if (source?.kind == CjFakeSourceElementKind.PropertyFromParameter) "variable" else "property"

    private data class MemberInheritanceSubject(
        val declarations: List<CfirDeclaration>,
        val inheritedSources: List<InheritedMemberSource>,
        val source: CjSourceElement?,
        val classLikeDeclaration: CfirClassLikeDeclaration?,
    )

    private data class InheritedMemberSource(
        val typeRef: CfirTypeRef,
        val includeDirectExtends: Boolean,
    )

    private fun CfirClassLikeDeclaration.memberInheritanceSubject(): MemberInheritanceSubject =
        MemberInheritanceSubject(
            declarations = declarations,
            inheritedSources = superTypeRefs.map { InheritedMemberSource(it, includeDirectExtends = true) },
            source = source,
            classLikeDeclaration = this,
        )

    private fun CfirExtend.memberInheritanceSubject(): MemberInheritanceSubject =
        MemberInheritanceSubject(
            declarations = declarations,
            inheritedSources = buildList {
                add(InheritedMemberSource(extendedTypeRef, includeDirectExtends = false))
                addAll(superTypeRefs.map { InheritedMemberSource(it, includeDirectExtends = false) })
            },
            source = source,
            classLikeDeclaration = null,
        )

    context(context: CheckerContext)
    private fun CfirTypeRef.resolvedClassLikeDeclaration(): CfirClassLikeDeclaration? {
        val classId = (this as? CfirResolvedTypeRef)?.coneType?.expandedClassIdOrPrimitiveClassId ?: return null
        val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        return symbol.cfir as? CfirClassLikeDeclaration
    }

    context(context: CheckerContext)
    private fun CfirTypeRef.resolvedUseSiteMemberScope(excludingExtend: CfirExtend? = null): CfirTypeScope? {
        val coneType = (this as? CfirResolvedTypeRef)?.coneType ?: return null
        return coneType.resolvedUseSiteMemberScope(excludingExtend)
    }

    context(context: CheckerContext)
    private fun ConeCangJieType.resolvedUseSiteMemberScope(excludingExtend: CfirExtend? = null): CfirTypeScope? {
        val classId = expandedClassIdOrPrimitiveClassId
        if (classId != null) {
            val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
            val rawScope = CfirClassUseSiteMemberScope(
                session = context.session,
                classSymbol = symbol,
                symbolProvider = context.session.symbolProvider,
                extendProvider = context.session.extendProvider,
                directSupertypeProvider = context.session.directSupertypeProviderOrNull,
                ownerType = this,
                dispatchReceiverType = this,
                scopeKind = CfirClassMemberScopeKind.DECLARATION_SITE,
                excludingExtend = excludingExtend,
            )
            return CfirClassSubstitutionScope(
                session = context.session,
                useSiteMemberScope = rawScope,
                dispatchReceiverType = this,
                substitutionOwnerType = this,
            )
        }

        val targetKey = expandedExtendTargetKey ?: return null
        if (targetKey.classIdOrNull != null) return null
        val scopes = buildList {
            val builtinExtendScope = CfirExtendMemberScope(
                targetKey = targetKey,
                extendProvider = context.session.extendProvider,
                session = context.session,
                receiverType = this@resolvedUseSiteMemberScope,
                excludingExtend = excludingExtend,
            )
            add(
                CfirClassSubstitutionScope(
                    session = context.session,
                    useSiteMemberScope = builtinExtendScope,
                    dispatchReceiverType = this@resolvedUseSiteMemberScope,
                    substitutionOwnerType = this@resolvedUseSiteMemberScope,
                )
            )
            for (supertype in context.session.typeAwareSupertypeProviderOrNull
                ?.getDirectSupertypes(this@resolvedUseSiteMemberScope)
                .orEmpty()
            ) {
                supertype.resolvedUseSiteMemberScope(excludingExtend)?.let(::add)
            }
        }
        return CfirCompositeTypeScope(scopes)
    }

}

/**
 * extend 声明也属于官方 `InheritableDecl`，需要进入同一组继承成员一致性检查。
 */
object CfirExtendInheritanceDeepChecker : CfirExtendChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirExtend) {
        CfirInheritanceDeepChecker.checkExtend(declaration)
    }
}
