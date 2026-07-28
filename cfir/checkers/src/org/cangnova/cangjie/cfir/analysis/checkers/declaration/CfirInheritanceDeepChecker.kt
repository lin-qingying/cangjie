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
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
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
import org.cangnova.cangjie.cfir.scopes.createCallableTypeParameterSubstitutorForOverride
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
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
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.symbols.toLookupTag
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
import org.cangnova.cangjie.type.model.TypeConstructorMarker

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
    /**
     * 对 class-like 声明执行继承深层语义检查。
     *
     * 该入口负责把类、接口、结构等声明统一接入继承成员一致性流水线；
     * 其中类声明额外承担 sealed 继承范围、抽象 static 成员实现、
     * 以及可继承成员可见性约束。
     */
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

    /**
     * 对 `extend` 声明执行与 class-like 声明共享的继承成员一致性检查。
     *
     * `extend` 没有自身 class symbol，因此通过 [memberInheritanceSubject]
     * 构造临时继承主体，并额外检查目标类型既有成员是否满足接口约束。
     */
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
        val receiverType = extend.extendedTypeRef.coneTypeOrNull ?: return
        val targetScope = extend.extendedTypeRef.resolvedUseSiteMemberScope(excludingExtend = extend) ?: return
        val targetClassId = (extend.extendedTypeRef as? CfirResolvedTypeRef)
            ?.coneType
            ?.expandedClassIdOrPrimitiveClassId
        val ownMemberInfosByName = extend.declarations
            .mapNotNull { it.directMemberInfoOrNull(context) }
            .groupBy { it.name }
        val inheritedDefaultImplementationsByName = extend.collectInheritedDefaultInterfaceImplementationsByName()
        checkDefaultInterfaceMemberConflicts(extend, targetScope, targetClassId, ownMemberInfosByName)

        val reportedMutConflicts = mutableSetOf<Name>()
        val reportedWeakVisibilities = mutableSetOf<String>()
        val reportedPropertyTypeConflicts = mutableSetOf<String>()
        val reportedPropertyMutabilityConflicts = mutableSetOf<String>()
        val reportedFunctionReturnTypeConflicts = mutableSetOf<String>()
        var hasUnimplementedMember = false

        for (superTypeRef in extend.superTypeRefs) {
            if (superTypeRef.coneTypeOrNull?.containsErrorType() == true) continue
            val superDecl = superTypeRef.resolvedClassLikeDeclaration() ?: continue
            for (superInfo in superTypeRef.collectInterfaceRequirementMemberInfos(superDecl)) {
                val builtinOperatorImplementation = extend.builtinPrimitiveOperatorImplementation(superInfo, context)
                if (
                    builtinOperatorImplementation is BuiltinPrimitiveOperatorImplementation.ReturnTypeMismatch &&
                    reportedFunctionReturnTypeConflicts.add(superInfo.requirementDiagnosticKey())
                ) {
                    reporter.reportOn(
                        source = extend.source,
                        factory = CfirErrors.RETURN_TYPE_INCOMPATIBLE,
                        a = superInfo.name,
                    )
                }

                val implementationCandidates = buildList {
                    targetScope.processCallablesByName(superInfo.name) { symbol ->
                        symbol.inheritedMemberInfoOrNull(context)?.let { info ->
                            add(ExtendImplementationCandidate(info, extend.extendedTypeRef.source, null))
                        }
                    }
                    if (targetClassId != null) {
                        for (info in collectDirectExtendMemberInfos(receiverType, superInfo.name, context, excludingExtend = extend)) {
                            add(ExtendImplementationCandidate(info, extend.extendedTypeRef.source, null))
                        }
                    }
                    for (info in ownMemberInfosByName[superInfo.name].orEmpty()) {
                        add(ExtendImplementationCandidate(info, info.nameSource ?: info.source ?: extend.source, info.source))
                    }
                    for (info in inheritedDefaultImplementationsByName[superInfo.name].orEmpty()) {
                        add(ExtendImplementationCandidate(info, info.nameSource ?: info.source ?: superTypeRef.source, null))
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

                    if (candidate.declarationSource == null) {
                        val functionReturnTypeConflict = if (builtinOperatorImplementation == null) {
                            implementationInfo.functionReturnTypeConflict(superInfo, context)
                        } else {
                            null
                        }
                        if (functionReturnTypeConflict != null) {
                            val key = implementationInfo.overrideDiagnosticKey(superInfo)
                            if (reportedFunctionReturnTypeConflicts.add(key)) {
                                functionReturnTypeConflict.report(
                                    source = superInfo.nameSource ?: candidate.diagnosticSource,
                                    name = superInfo.name,
                                    reporter = reporter,
                                )
                            }
                        }
                    }

                    if (extend.hasStructTarget() &&
                        implementationInfo.hasMutFunctionConflict(superInfo) &&
                        reportedMutConflicts.add(superInfo.name)
                    ) {
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

                val hasSatisfiedImplementation = implementationCandidates.any { candidate ->
                    val implementationInfo = candidate.info
                    implementationInfo.canImplement(superInfo) &&
                        implementationInfo.satisfiesExtendInterfaceRequirement(context)
                } || builtinOperatorImplementation != null
                if (hasSatisfiedImplementation) continue

                if (superInfo.isAbstract) {
                    hasUnimplementedMember = true
                }
            }
        }

        if (hasUnimplementedMember) {
            reporter.reportOn(
                source = extend.source?.firstCharacterDiagnosticSource() ?: extend.extendedTypeRef.source,
                factory = CfirErrors.NEED_MEMBER_IMPLEMENTATION,
                a = extend.targetDisplayName(),
            )
        }
    }

    /**
     * 同一条 `extend T <: A & B` 中，接口 `A` 的默认成员可以满足接口 `B`
     * 的同签名抽象要求。官方继承检查在合并接口成员表后判断实现状态，
     * 因此这里把当前 extend 的默认接口成员纳入共享实现候选，而不是在
     * 某个 fixture 上特殊跳过缺实现诊断。
     */
    context(context: CheckerContext)
    private fun CfirExtend.collectInheritedDefaultInterfaceImplementationsByName(): Map<Name, List<InheritedMemberInfo>> =
        buildList {
            for (superTypeRef in superTypeRefs) {
                val superDecl = superTypeRef.resolvedClassLikeDeclaration() ?: continue
                for (info in superTypeRef.collectInterfaceRequirementMemberInfos(superDecl)) {
                    if (info.isDefaultInterfaceMember(context)) {
                        add(info)
                    }
                }
            }
        }.groupBy { it.name }

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

    /**
     * 在默认接口成员冲突检查中按当前 receiver 类型重放 extend 声明。
     *
     * 泛型 extend 需要先把声明中的目标类型形参替换为实际 receiver 类型，
     * 再收集其 super interface 中带默认实现的成员。
     */
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

    /**
     * 使用 extend 声明替换关系替换父类型引用。
     *
     * 如果替换后类型没有变化，则保留原始 type ref，避免制造无意义的
     * fake source 或破坏后续诊断位置。
     */
    private fun CfirTypeRef.substituteForDefaultConflict(substitutor: ConeSubstitutor): CfirTypeRef {
        val resolvedTypeRef = this as? CfirResolvedTypeRef ?: return this
        val substitutedType = substitutor.substituteOrSelf(resolvedTypeRef.coneType)
        if (substitutedType == resolvedTypeRef.coneType) return this
        return resolvedTypeRef.withReplacedSourceAndType(resolvedTypeRef.source, substitutedType)
    }

    /**
     * 判断同一默认接口成员签名是否需要当前 extend 显式实现。
     *
     * 该逻辑区分同一 extend 内部重复默认实现、具体 extend 与泛型 extend
     * 的实例化重叠、以及互不继承的独立 extend 默认实现冲突。
     */
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

    /**
     * 判断目标类型、直接 extend 成员或当前 extend 自身是否已经提供具体实现。
     *
     * 只有非 abstract 且非 interface default 的成员才算作这里的 concrete implementation。
     */
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
            val receiverType = extend.extendedTypeRef.coneTypeOrNull ?: return false
            for (info in collectDirectExtendMemberInfos(receiverType, superInfo.name, context, excludingExtend = extend)) {
                if (info.isConcreteImplementationOf(superInfo, context)) return true
            }
        }
        for (info in ownMemberInfosByName[superInfo.name].orEmpty()) {
            if (info.isConcreteImplementationOf(superInfo, context)) return true
        }
        return false
    }

    /**
     * 判断 extend 目标类型是否引用了当前 extend 自己声明的类型参数。
     *
     * 该信息用于区分泛型 extend 和具体 extend 的默认接口成员冲突传播规则。
     */
    private fun CfirExtend.extendedTypeUsesOwnTypeParameter(): Boolean {
        val parameterNames = typeParameters.mapTo(linkedSetOf()) { it.name.asString() }
        if (parameterNames.isEmpty()) return false
        val coneType = (extendedTypeRef as? CfirResolvedTypeRef)?.coneType ?: return false
        return parameterNames.any { parameterName -> coneType.containsTypeParameter(parameterName) }
    }

    /**
     * 判断类型或其缩略类型中是否包含指定名称的类型参数。
     */
    private fun ConeCangJieType.containsTypeParameter(parameterName: String): Boolean =
        abbreviatedType?.containsTypeParameter(parameterName) == true || containsTypeParameterInConstructor(parameterName)

    /**
     * 在具体类型构造器及类型实参中递归查找指定类型参数。
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

    /**
     * 判断成员是否可以作为某个接口/父类型要求的具体实现。
     */
    private fun InheritedMemberInfo.isConcreteImplementationOf(
        superInfo: InheritedMemberInfo,
        context: CheckerContext,
    ): Boolean =
        canImplement(superInfo) && !isAbstract && !isDefaultInterfaceMember(context)

    /**
     * 官方 `DiagnoseForUnimplementedInterfaces` 对 extend 声明跳过抽象类中已继承的
     * abstract 成员：它们虽然不是 concrete implementation，但已经在目标类型的
     * 抽象成员表中承担接口签名，不能要求 extend 块重新实现。
     */
    private fun InheritedMemberInfo.satisfiesExtendInterfaceRequirement(
        context: CheckerContext,
    ): Boolean {
        if (!isAbstract) return true
        val owner = symbol?.let { context.ownerClassSymbol(it)?.cfir }
        return owner is CfirClass && owner.status.isAbstract
    }

    /**
     * 生成默认接口实现冲突的签名 key。
     */
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

    /**
     * 从 use-site scope 中收集所有可作为继承成员参与检查的 callable 信息。
     */
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
                    source = classDecl.classLikeNameDiagnosticSource(),
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
            if (inheritedFunctions.hasStaticAndNonStaticMembers()) {
                continue
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

    /**
     * 官方继承检查在同名成员已经发生 static/non-static 冲突后，不再继续报告类型不一致。
     */
    private fun List<CfirFunctionSymbol<*>>.hasStaticAndNonStaticMembers(): Boolean {
        var hasStatic = false
        var hasNonStatic = false
        for (symbol in this) {
            if (!symbol.isBound) continue
            if (symbol.cfir.status.isStatic) {
                hasStatic = true
            } else {
                hasNonStatic = true
            }
            if (hasStatic && hasNonStatic) return true
        }
        return false
    }

    /**
     * 获取函数符号的解析后返回类型。
     *
     * 优先读取已解析 type ref；当返回类型仍需懒计算时，通过当前 checker context
     * 的 return type calculator 计算，保持与其他 CFIR 检查器一致。
     */
    private fun CfirFunctionSymbol<*>.resolvedReturnTypeOrNull(
        context: CheckerContext,
    ): ConeCangJieType? {
        if (!isBound) return null
        (cfir.returnTypeRef as? CfirResolvedTypeRef)?.coneType?.let { return it }
        return context.returnTypeCalculator.tryCalculateReturnType(cfir).coneType
    }

    /**
     * 判断一组继承函数返回类型是否存在既不相等也不存在子类型关系的冲突。
     */
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
     * 抽象类继承 static 抽象成员时，仍必须在继承诊断层暴露未实现成员。
     *
     * 对齐 C++ `DiagnoseForUnimplementedInterfaces`：
     * 当前声明是 abstract class 且继承到外部 static abstract 成员时，报告
     * `sema_inherit_abstract_class_static_unimplement_func`。普通非抽象类仍由
     * `ABSTRACT_MEMBER_NOT_IMPLEMENTED` 路径负责。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkAbstractClassStaticUnimplemented(classDecl: CfirClass) {
        if (!classDecl.status.isAbstract) return

        val ownerClassId = (classDecl.symbol as? CfirClassLikeSymbol<*>)?.classId
        // class 声明自身只承担 declaration-site 继承义务；extend 引入的接口由 extend owner 报告。
        val staticMembers = context.session.cangjieScopeProvider
            .getDeclarationSiteMemberScope(classDecl, context.session, context.scopeSession)
            .collectInheritedMemberInfos(context)
            .filter { info ->
                info.isStatic &&
                    (info.kind == "function" || info.kind == "property") &&
                    info.symbol?.isVisibleIn(classDecl, context) != false
            }
            .groupBy { it.requirementDiagnosticKey() }

        val reported = mutableSetOf<String>()
        for ((_, members) in staticMembers) {
            val concreteImplementations = members.filter { !it.isAbstract }
            for (abstractMember in members) {
                val sourceOwnerClassId = abstractMember.symbol?.ownerClassId(context)
                if (!abstractMember.isAbstract || sourceOwnerClassId == ownerClassId) continue

                val implemented = concreteImplementations.any { implementation ->
                    implementation.canImplement(abstractMember) &&
                        !implementation.hasWeakVisibilityComparedTo(abstractMember)
                }
                if (implemented) continue

                val key = abstractMember.requirementDiagnosticKey()
                if (!reported.add(key)) continue

                reporter.reportOn(
                    source = classDecl.classLikeNameDiagnosticSource() ?: classDecl.source,
                    factory = CfirErrors.INHERIT_ABSTRACT_CLASS_STATIC_UNIMPLEMENT_FUNC,
                    a = classDecl.name,
                    b = "static ${abstractMember.kind}",
                    c = abstractMember.name,
                )
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

    /**
     * 记录成员可见性检查所需的最小诊断信息。
     *
     * @property visibility 成员声明的实际可见性。
     * @property modifier 触发约束的继承相关修饰符，例如 `abstract` 或 `open`。
     * @property kind 诊断中展示的成员种类。
     * @property source 优先用于报错的成员名称级 source。
     */
    private data class InvalidVisibilityMemberInfo(
        /** 成员声明的实际可见性。 */
        val visibility: Visibility,
        /** 触发约束的继承相关修饰符，未触发时为 null。 */
        val modifier: String?,
        /** 诊断中展示的成员种类。 */
        val kind: String,
        /** 优先用于报错的成员名称级 source。 */
        val source: AbstractCjSourceElement?,
    )

    /**
     * 返回函数在当前类继承语义下需要满足可见性约束的修饰符。
     */
    private fun CfirNamedFunction.invalidVisibilityModifier(
        classIsAbstract: Boolean,
        classIsInheritable: Boolean,
    ): String? {
        if (status.isStatic) return null
        if (status.isAbstract && classIsAbstract) return "abstract"
        if (status.isOpen && classIsInheritable) return "open"
        return null
    }

    /**
     * 返回属性在当前类继承语义下需要满足可见性约束的修饰符。
     */
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
                    isOverride = member.status.isOverride,
                    isRedef = member.status.isRedef,
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
                    isOverride = member.status.isOverride,
                    isRedef = member.status.isRedef,
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
                    isOverride = false,
                    isRedef = false,
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
        val reportedReturnTypeConflicts = mutableSetOf<String>()
        val reportedVariableShadows = mutableSetOf<Name>()
        val reportedCannotOverrides = mutableSetOf<String>()
        val reportedInvalidAbstractOverrides = mutableSetOf<String>()
        val reportedWeakVisibilities = mutableSetOf<String>()
        val reportedExtendOverrides = mutableSetOf<String>()
        for (inheritedSource in subject.inheritedSources) {
            val superType = (inheritedSource.typeRef as? CfirResolvedTypeRef)?.coneType ?: continue
            if (superType.containsErrorType()) continue
            val superClassId = (superType as? ConeClassLikeType)?.classId ?: continue
            val superSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(superClassId) ?: continue
            val superDecl = superSymbol.cfir as? CfirClassLikeDeclaration ?: continue
            val superScope = inheritedSource.typeRef.resolvedUseSiteMemberScope()
                ?: context.createUseSiteMemberScope(superDecl)

            for (name in ownMembers.keys) {
                val superInfos = buildList {
                    superScope.processCallablesByName(name) { symbol ->
                        symbol.inheritedMemberInfoOrNull(context)?.let(::add)
                    }
                    if (inheritedSource.includeDirectExtends) {
                        addAll(collectDirectExtendMemberInfos(superType, name, context))
                        addAll(collectEffectiveExtendInterfaceMemberInfos(superType, name, context))
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
                                    source = ownInfo.nameSource ?: ownInfo.source ?: subject.source,
                                    factory = CfirErrors.STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME,
                                    a = ownInfo.staticKind,
                                    b = ownInfo.name,
                                    c = superInfo.staticKind,
                                    d = if (subject.isExtendSubject) "extended type" else "parent class or interfaces",
                                )
                            }
                        }

                        if (ownInfo.kind != superInfo.kind) {
                            if (!hasStaticConflict && reportedKindConflicts.add(ownInfo.name)) {
                                if (subject.isExtendSubject) {
                                    reporter.reportOn(
                                        source = ownInfo.nameSource ?: subject.source,
                                        factory = CfirErrors.EXTEND_MEMBER_CANNOT_SHADOW,
                                        a = ownInfo.name,
                                        b = superInfo.ownerName ?: superClassId.shortClassName,
                                    )
                                } else {
                                    reporter.reportOn(
                                        source = ownInfo.nameSource ?: subject.source,
                                        factory = CfirErrors.INHERIT_MEMBER_KIND_INCONSISTENT,
                                        a = ownInfo.kind,
                                        b = ownInfo.name,
                                        c = superInfo.kind,
                                        d = superInfo.ownerName ?: superClassId.shortClassName,
                                    )
                                }
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

                        if (!hasStaticConflict && subject is CfirStruct && ownInfo.hasMutFunctionConflict(superInfo)) {
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

                        val returnTypeConflict = if (!hasStaticConflict) {
                            ownInfo.functionReturnTypeConflict(superInfo, context)
                        } else {
                            null
                        }
                        if (returnTypeConflict != null) {
                            val key = ownInfo.overrideDiagnosticKey(superInfo)
                            if (reportedReturnTypeConflicts.add(key)) {
                                returnTypeConflict.report(
                                    source = ownInfo.nameSource ?: ownInfo.source ?: subject.source,
                                    name = superInfo.name,
                                    reporter = reporter,
                                )
                            }
                        }

                        if (classDecl != null &&
                            !ownInfo.isOverride &&
                            !ownInfo.isRedef &&
                            returnTypeConflict == null &&
                            ownInfo.hasWeakVisibilityComparedTo(superInfo)
                        ) {
                            val key = ownInfo.overrideDiagnosticKey(superInfo)
                            if (reportedWeakVisibilities.add(key)) {
                                reporter.reportOn(
                                    source = ownInfo.source ?: ownInfo.nameSource ?: subject.source,
                                    factory = CfirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE,
                                    a = superInfo.name,
                                    b = superInfo.visibility,
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

                        if (subject.classLikeDeclaration != null && ownInfo.overridesExtendMember(superInfo, superType, context)) {
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
                        } else if (classDecl != null && ownInfo.invalidAbstractOverrideInClass(superInfo, classDecl, context)) {
                            val key = ownInfo.overrideDiagnosticKey(superInfo)
                            if (reportedInvalidAbstractOverrides.add(key)) {
                                reporter.reportOn(
                                    source = ownInfo.source?.firstCharacterDiagnosticSource()
                                        ?: ownInfo.nameSource
                                        ?: subject.source,
                                    factory = CfirErrors.INVALID_OVERRIDE_MEMBER_IN_CLASS,
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

    /**
     * 判断当前成员实现父成员时是否存在函数返回类型冲突。
     *
     * 普通 override 要求实现返回类型可以作为父返回类型的子类型；
     * 官方对 extend/boxing 关系还有返回类型不变性约束，因此这里先检查
     * [hasOfficialReturnTypeInvarianceAgainst]，再执行普通子类型检查。
     */
    private fun InheritedMemberInfo.functionReturnTypeConflict(
        superInfo: InheritedMemberInfo,
        context: CheckerContext,
    ): FunctionReturnTypeConflict? {
        if (!canImplement(superInfo)) return null
        if (kind != "function") return null
        val implementationSymbol = symbol as? CfirFunctionSymbol<*> ?: return null
        val baseSymbol = superInfo.symbol as? CfirFunctionSymbol<*> ?: return null
        val implementationType = implementationSymbol.resolvedReturnTypeOrNull(context) ?: return null
        val unsubstitutedBaseType = baseSymbol.resolvedReturnTypeOrNull(context) ?: return null
        val typeParameterSubstitutor = createCallableTypeParameterSubstitutorForOverride(
            overriding = implementationSymbol,
            overridden = baseSymbol,
            context = context.session.typeContext,
        ) ?: return null
        val baseType = typeParameterSubstitutor.substituteOrSelf(unsubstitutedBaseType)
        if (implementationType is ConeErrorType || baseType is ConeErrorType) return null
        if (implementationType.hasGenericReturnTypeInvarianceAgainst(baseType, context)) {
            return FunctionReturnTypeConflict.Invariance(baseType)
        }
        if (implementationType.hasOfficialReturnTypeInvarianceAgainst(baseType, context)) {
            return FunctionReturnTypeConflict.Invariance(baseType)
        }
        if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, implementationType, baseType)) return null
        return FunctionReturnTypeConflict.Mismatch(
            implementationType = implementationType,
            baseType = baseType,
        )
    }

    /**
     * 对齐官方 `StructInheritanceChecker::CheckReturnOverrideByGeneric`：实现方直接以类型参数
     * 作为返回类型且与父返回类型不同时，只有类上界能够提供返回类型协变依据；无上界、
     * 接口上界或类型参数上界都必须保持返回类型不变。
     */
    private fun ConeCangJieType.hasGenericReturnTypeInvarianceAgainst(
        baseType: ConeCangJieType,
        context: CheckerContext,
    ): Boolean {
        val typeParameter = this as? ConeTypeParameterType ?: return false
        if (AbstractTypeChecker.equalTypes(context.session.typeContext, typeParameter, baseType)) return false
        return !typeParameter.hasConcreteClassUpperBound(context, mutableSetOf())
    }

    /**
     * 判断类型参数的直接或传递上界中是否存在具体类上界。
     *
     * 官方在继承检查前通过 `ExposeGenericUpperBounds` 展开上界闭包；CFIR 在检查点按需递归，
     * 并用 visited 保持循环上界仍属于“仅泛型上界”的不变性分支。
     */
    private fun ConeTypeParameterType.hasConcreteClassUpperBound(
        context: CheckerContext,
        visited: MutableSet<CfirTypeParameterSymbol>,
    ): Boolean {
        val symbol = lookupTag.typeParameterSymbol
        if (!visited.add(symbol)) return false
        return symbol.resolvedBounds.any { boundRef ->
            when (val bound = boundRef.coneType) {
                is ConeTypeParameterType -> bound.hasConcreteClassUpperBound(context, visited)
                else -> !bound.isInterfaceType(context)
            }
        }
    }

    /**
     * primitive 目标实现接口时，内建 operator 可作为接口抽象 operator 的实现。
     *
     * 对齐官方 `StructInheritanceChecker::IsBuiltInOperatorFuncInExtend`：
     * 如果接口抽象 operator 的参数与 extend 目标组成内建 primitive operator，
     * 则返回类型正确时视为已实现；返回类型错误时在 extend 声明上报告
     * `RETURN_TYPE_INCOMPATIBLE`，并且不再把该接口成员当作未实现成员。
     */
    private fun CfirExtend.builtinPrimitiveOperatorImplementation(
        superInfo: InheritedMemberInfo,
        context: CheckerContext,
    ): BuiltinPrimitiveOperatorImplementation? {
        if (superInfo.kind != "function" || !superInfo.isAbstract) return null
        val superSymbol = superInfo.symbol as? CfirFunctionSymbol<*> ?: return null
        val superFunction = superSymbol.cfir as? CfirNamedFunction ?: return null
        if (!superFunction.status.isOperator) return null

        val receiverType = BuiltinPrimitiveOperators.normalizePrimitiveOperand(extendedTypeRef.coneTypeOrNull)
            ?: return null
        val parameterTypes = superFunction.valueParameters.map { parameter ->
            BuiltinPrimitiveOperators.normalizePrimitiveOperand(parameter.returnTypeRef.coneTypeOrNull) ?: return null
        }
        val builtinMatch = BuiltinPrimitiveOperators.resolve(
            name = superInfo.name,
            receiverType = receiverType,
            argumentTypes = parameterTypes,
        ) ?: return null
        val interfaceReturnType = superSymbol.resolvedReturnTypeOrNull(context) ?: return null
        if (interfaceReturnType is ConeErrorType) return null
        return if (AbstractTypeChecker.equalTypes(context.session.typeContext, builtinMatch.returnType, interfaceReturnType)) {
            BuiltinPrimitiveOperatorImplementation.Compatible
        } else {
            BuiltinPrimitiveOperatorImplementation.ReturnTypeMismatch
        }
    }

    /**
     * 对齐官方 `TypeManager::HasExtensionRelation` 在返回类型 override 中的特殊分支：
     * extend/boxing 关系不能作为 override/implement 返回类型协变依据。
     */
    private fun ConeCangJieType.hasOfficialReturnTypeInvarianceAgainst(
        interfaceType: ConeCangJieType,
        context: CheckerContext,
    ): Boolean {
        val targetInterface = interfaceType as? ConeClassLikeType ?: return false
        if (!targetInterface.isInterface) return false
        if (isNothing) return false
        if ((this as? ConeClassLikeType)?.isInterface == true) return false
        if (targetInterface.classId == StdlibClassIds.Any) return true

        val targetKey = expandedExtendTargetKey ?: return false
        for (extend in context.session.extendProvider.getExtendsForTarget(targetKey)) {
            if (!context.session.extendProvider.isExtendAccessible(extend)) continue
            val targetPattern = extend.extendedTypeRef.coneTypeOrNull ?: continue
            val substitution = createExtendDeclarationSubstitution(
                session = context.session,
                extend = extend,
                targetPattern = targetPattern,
                concreteReceiverType = this,
            ) ?: continue
            for (superTypeRef in extend.superTypeRefs) {
                val superType = superTypeRef.coneTypeOrNull ?: continue
                val substitutedSuperType = substitution.substitutor.substituteOrSelf(superType)
                if (AbstractTypeChecker.equalTypes(context.session.typeContext, substitutedSuperType, targetInterface)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * 将 CFIR 声明转换为继承检查使用的成员信息。
     *
     * 该入口只返回可继承成员；private 等不应继承的成员会由 symbol 侧过滤。
     */
    private fun CfirDeclaration.inheritedMemberInfoOrNull(context: CheckerContext): InheritedMemberInfo? =
        when (this) {
            is CfirNamedFunction -> symbol?.inheritedMemberInfoOrNull(context)
            is CfirProperty -> symbol.inheritedMemberInfoOrNull(context)
            is CfirFieldVariable -> symbol.inheritedMemberInfoOrNull(context)
            else -> null
        }

    /**
     * 将当前声明转换为直接成员信息。
     *
     * direct member 用于 extend 自身声明成员候选，不做“只能继承成员”的过滤。
     */
    private fun CfirDeclaration.directMemberInfoOrNull(context: CheckerContext): InheritedMemberInfo? =
        when (this) {
            is CfirNamedFunction -> symbol?.directMemberInfoOrNull(context)
            is CfirProperty -> symbol.directMemberInfoOrNull(context)
            is CfirFieldVariable -> symbol.directMemberInfoOrNull(context)
            else -> null
        }

    /**
     * 将声明转换为可继承的直接成员信息。
     *
     * 该方法保留给需要表达“从声明出发但仍应用继承过滤”的调用点。
     */
    private fun CfirDeclaration.inheritableDirectMemberInfoOrNull(context: CheckerContext): InheritedMemberInfo? =
        when (this) {
            is CfirNamedFunction -> symbol?.inheritedMemberInfoOrNull(context)
            is CfirProperty -> symbol.inheritedMemberInfoOrNull(context)
            is CfirFieldVariable -> symbol.inheritedMemberInfoOrNull(context)
            else -> null
        }

    /**
     * 将 callable symbol 转换为 direct member 信息，不过滤 private 等不可继承成员。
     */
    private fun CfirCallableSymbol<*>.directMemberInfoOrNull(context: CheckerContext): InheritedMemberInfo? =
        memberInfoOrNull(context, inheritOnly = false)

    /**
     * 将 callable symbol 转换为 inherited member 信息，并应用继承可见性过滤。
     */
    private fun CfirCallableSymbol<*>.inheritedMemberInfoOrNull(context: CheckerContext): InheritedMemberInfo? {
        return memberInfoOrNull(context, inheritOnly = true)
    }

    /**
     * 抽取继承检查使用的统一成员模型。
     *
     * 函数、属性和字段变量在 CFIR 中是不同声明形态，但继承规则只关心名称、
     * 成员种类、static/const/mut/default/abstract 状态、可见性和来源位置。
     */
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
                isOverride = declaration.status.isOverride,
                isRedef = declaration.status.isRedef,
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
                isOverride = declaration.status.isOverride,
                isRedef = declaration.status.isRedef,
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
                isOverride = false,
                isRedef = false,
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

    /**
     * 判断 callable 是否需要作为接口要求由实现方提供实现。
     *
     * 接口中没有函数体或访问器体的默认成员在继承检查中等价于 abstract requirement。
     */
    private fun CfirCallableDeclaration.requiresInterfaceImplementation(
        ownerDeclaration: CfirClassLikeDeclaration?,
    ): Boolean {
        if (status.isAbstract) return true
        if (ownerDeclaration !is CfirInterface) return false
        return !hasOwnBodyOrAccessorBody()
    }

    /**
     * 判断声明自身是否已经提供函数体或属性访问器体。
     */
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
        receiverType: ConeCangJieType,
        name: Name,
        context: CheckerContext,
        excludingExtend: CfirExtend? = null,
    ): List<InheritedMemberInfo> {
        val provider = context.session.extendProvider
        val targetKey = receiverType.expandedExtendTargetKey ?: return emptyList()
        return buildList {
            for (extend in provider.getExtendsForTarget(targetKey)) {
                if (extend === excludingExtend) continue
                if (!provider.isExtendAccessible(extend)) continue
                if (findExtendDeclarationSubstitution(context.session, extend, receiverType) == null) continue
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
        if (isRedef) return false
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
     * 官方 `CheckInheritanceAttributes` 禁止 class 用 abstract 成员覆盖父 class
     * 中已有实现的同签名函数/属性，即使父成员本身是 `open`。
     */
    private fun InheritedMemberInfo.invalidAbstractOverrideInClass(
        superInfo: InheritedMemberInfo,
        classDecl: CfirClassLikeDeclaration,
        context: CheckerContext,
    ): Boolean {
        if (classDecl !is CfirClass) return false
        if (!isAbstract) return false
        if (kind != superInfo.kind) return false
        if (kind != "function" && kind != "property") return false
        if (!hasSameOverrideSignature(superInfo)) return false

        val superSymbol = superInfo.symbol ?: return false
        if (!superSymbol.isBound) return false
        if (superSymbol.cfir.status.isStatic) return false
        if (superInfo.isAbstract) return false

        val superOwner = context.ownerClassSymbol(superSymbol)?.cfir
        if (superOwner is CfirInterface) return false

        return true
    }

    /**
     * 官方 CheckExtendMemberValid 会先处理“类成员覆盖父类型 extend 成员”的情况，
     * 报 sema_extend_function_cannot_overridden 后不再进入普通 cannot-override 检查。
     */
    private fun InheritedMemberInfo.overridesExtendMember(
        superInfo: InheritedMemberInfo,
        inheritedOwnerType: ConeCangJieType,
        context: CheckerContext,
    ): Boolean {
        if (kind != superInfo.kind) return false
        if (kind != "function" && kind != "property") return false
        if (!hasSameOverrideSignature(superInfo)) return false
        val superSymbol = superInfo.symbol ?: return false
        val originalSuperSymbol = superSymbol.unwrapSubstitutionOverrides()
        if (context.session.extendProvider.getContainingExtend(originalSuperSymbol) != null) return true
        return superInfo.isExtendedDefaultImplementationFrom(inheritedOwnerType, context)
    }

    /**
     * `extend C <: I` 引入的 interface default member 被子类声明同签名成员时，
     * 官方 `IsExtendedDefaultImpl` 与普通接口继承区分处理，统一报 extend override 冲突。
     */
    private fun InheritedMemberInfo.isExtendedDefaultImplementationFrom(
        inheritedOwnerType: ConeCangJieType,
        context: CheckerContext,
    ): Boolean {
        if (!isDefault) return false
        val ownerInterface = symbol?.let { context.ownerClassSymbol(it)?.cfir } as? CfirInterface ?: return false
        val interfaceClassId = (ownerInterface.symbol as? CfirClassLikeSymbol<*>)?.classId ?: return false
        return inheritedOwnerType.hasEffectiveExtendInterface(interfaceClassId, context)
    }

    /**
     * 判断类型自身或其非接口父类型链上是否存在指向目标接口的有效 extend。
     */
    private fun ConeCangJieType.hasEffectiveExtendInterface(
        targetInterfaceClassId: ClassId,
        context: CheckerContext,
    ): Boolean {
        val visited = linkedSetOf<ConeCangJieType>()

        fun visit(type: ConeCangJieType): Boolean {
            if (!visited.add(type)) return false
            if (type.hasDirectExtendInterface(targetInterfaceClassId, context)) return true
            for (supertype in type.declaredNonInterfaceSupertypes(context)) {
                if (visit(supertype)) return true
            }
            return false
        }

        return visit(this)
    }

    /**
     * 判断当前类型是否直接通过可见 extend 关系扩展到目标接口。
     */
    private fun ConeCangJieType.hasDirectExtendInterface(
        targetInterfaceClassId: ClassId,
        context: CheckerContext,
    ): Boolean {
        return collectDirectExtendInterfaceTypes(context).any {
            it.classIdOrPrimitiveClassId == targetInterfaceClassId
        }
    }

    /**
     * 收集继承所有者类型通过有效 extend 接口暴露的默认接口成员。
     *
     * 该集合用于检测类成员覆盖 extend 引入的 interface default implementation。
     */
    private fun collectEffectiveExtendInterfaceMemberInfos(
        inheritedOwnerType: ConeCangJieType,
        name: Name,
        context: CheckerContext,
    ): List<InheritedMemberInfo> {
        return buildList {
            for (interfaceType in inheritedOwnerType.collectEffectiveExtendInterfaceTypes(context)) {
                val interfaceClassId = interfaceType.classIdOrPrimitiveClassId ?: continue
                val interfaceSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(interfaceClassId) ?: continue
                val interfaceDeclaration = interfaceSymbol.cfir as? CfirInterface ?: continue
                val interfaceScope = context.createUseSiteMemberScope(interfaceDeclaration)
                interfaceScope.processCallablesByName(name) { symbol ->
                    val info = symbol.inheritedMemberInfoOrNull(context) ?: return@processCallablesByName
                    if (info.isDefaultInterfaceMember(context)) {
                        add(info)
                    }
                }
            }
        }
    }

    /**
     * 收集当前类型及其非接口父类型链上所有有效 extend 接口类型。
     */
    private fun ConeCangJieType.collectEffectiveExtendInterfaceTypes(
        context: CheckerContext,
    ): List<ConeCangJieType> {
        val visited = linkedSetOf<ConeCangJieType>()
        val result = linkedSetOf<ConeCangJieType>()

        fun visit(type: ConeCangJieType) {
            if (!visited.add(type)) return
            result += type.collectDirectExtendInterfaceTypes(context)
            for (supertype in type.declaredNonInterfaceSupertypes(context)) {
                visit(supertype)
            }
        }

        visit(this)
        return result.toList()
    }

    /**
     * 收集当前具体 receiver 类型通过直接 extend 声明获得的接口类型。
     *
     * 泛型 extend 会先根据 receiver 类型创建声明替换，再判断替换后的父类型是否为接口。
     */
    private fun ConeCangJieType.collectDirectExtendInterfaceTypes(
        context: CheckerContext,
    ): List<ConeCangJieType> {
        val targetKey = expandedExtendTargetKey ?: return emptyList()
        val result = mutableListOf<ConeCangJieType>()
        for (extend in context.session.extendProvider.getExtendsForTarget(targetKey)) {
            if (!context.session.extendProvider.isExtendAccessible(extend)) continue
            val targetPattern = extend.extendedTypeRef.coneTypeOrNull ?: continue
            val substitution = createExtendDeclarationSubstitution(
                session = context.session,
                extend = extend,
                targetPattern = targetPattern,
                concreteReceiverType = this,
            ) ?: continue
            for (superTypeRef in extend.superTypeRefs) {
                val superType = superTypeRef.coneTypeOrNull ?: continue
                val substitutedSuperType = substitution.substitutor.substituteOrSelf(superType)
                if (substitutedSuperType.isInterfaceType(context)) {
                    result += substitutedSuperType
                }
            }
        }
        return result
    }

    /**
     * 判断类型对应的 class-like 声明是否为接口。
     */
    private fun ConeCangJieType.isInterfaceType(context: CheckerContext): Boolean {
        if ((this as? ConeClassLikeType)?.isInterface == true) return true
        val classId = classIdOrPrimitiveClassId ?: return false
        return context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir is CfirInterface
    }

    /**
     * 收集声明在源码中写出的非接口父类型，并按当前具体类型实参替换父类型。
     *
     * 只沿非接口父类型传播 extend-interface 关系，避免把接口继承链误当作
     * 类/结构继承链上的 extend 目标。
     */
    private fun ConeCangJieType.declaredNonInterfaceSupertypes(context: CheckerContext): List<ConeCangJieType> {
        val classId = classIdOrPrimitiveClassId ?: return emptyList()
        val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return emptyList()
        val declaration = symbol.cfir as? CfirClassLikeDeclaration ?: return emptyList()
        val substitutor = declaration.createDeclarationSubstitutor(this)
        return declaration.superTypeRefs.mapNotNull { superTypeRef ->
            val supertype = (superTypeRef as? CfirResolvedTypeRef)?.coneType ?: return@mapNotNull null
            val substituted = substitutor?.substituteOrSelf(supertype) ?: supertype
            substituted.takeIf { it.shouldPropagateExtendInterfaceRelation(context) }
        }
    }

    /**
     * 判断类型是否允许继续传播 extend-interface 关系。
     */
    private fun ConeCangJieType.shouldPropagateExtendInterfaceRelation(context: CheckerContext): Boolean {
        val classId = classIdOrPrimitiveClassId ?: return false
        val declaration = context.session.symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir ?: return false
        return declaration !is CfirInterface
    }

    /**
     * 根据 class-like 声明的类型形参和当前实际类型实参创建声明替换器。
     */
    private fun CfirClassLikeDeclaration.createDeclarationSubstitutor(type: ConeCangJieType): ConeSubstitutor? {
        if (type !is ConeLookupTagBasedType) return null
        if (typeParameters.isEmpty()) return ConeSubstitutor.Empty
        if (typeParameters.size != type.typeArguments.size) return null

        val replacements: Map<TypeConstructorMarker, ConeCangJieType> =
            typeParameters.zip(type.typeArguments).associate { (typeParameter, argument) ->
                typeParameter.symbol.toLookupTag() to argument.type
            }
        return replacements.takeIf { it.isNotEmpty() }?.let(::CfirTypeSubstitutorByMap) ?: ConeSubstitutor.Empty
    }

    /**
     * 判断两个成员是否拥有相同 override 签名。
     *
     * 属性在当前继承诊断中只按名称和 kind 合并；函数则使用 scope 层生成的
     * override signature key 区分重载。
     */
    private fun InheritedMemberInfo.hasSameOverrideSignature(superInfo: InheritedMemberInfo): Boolean {
        if (kind == "property") return true
        val ownSymbol = symbol ?: return false
        val superSymbol = superInfo.symbol ?: return false
        return ownSymbol.overrideSignatureKey() == superSymbol.overrideSignatureKey()
    }

    /**
     * 判断当前成员能否作为父成员的实现候选。
     */
    private fun InheritedMemberInfo.canImplement(superInfo: InheritedMemberInfo): Boolean {
        if (kind != superInfo.kind) return false
        if (isStatic != superInfo.isStatic) return false
        if (kind != "function" && kind != "property") return false
        return hasSameOverrideSignature(superInfo)
    }

    /**
     * 判断实现成员可见性是否弱于被实现的父成员。
     */
    private fun InheritedMemberInfo.hasWeakVisibilityComparedTo(superInfo: InheritedMemberInfo): Boolean {
        if (!canImplement(superInfo)) return false
        val compareResult = Visibilities.compare(visibility, superInfo.visibility)
        return compareResult == null || compareResult < 0
    }

    /**
     * 判断属性实现与接口/父属性之间是否存在类型不一致。
     */
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

    /**
     * 判断属性实现与接口/父属性之间是否存在 mut/immut 可变性冲突。
     */
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

    /**
     * 判断非 const 函数是否试图实现同签名 const 函数。
     */
    private fun InheritedMemberInfo.hasConstFunctionConflict(superInfo: InheritedMemberInfo): Boolean {
        if (kind != "function" || superInfo.kind != "function") return false
        if (isConst || !superInfo.isConst) return false
        return hasSameOverrideSignature(superInfo)
    }

    /**
     * 判断同签名函数之间是否存在 mut 修饰符不一致。
     */
    context(context: CheckerContext)
    private fun InheritedMemberInfo.hasMutFunctionConflict(superInfo: InheritedMemberInfo): Boolean {
        if (kind != "function" || superInfo.kind != "function") return false
        if (isMut == superInfo.isMut) return false
        val superOwner = superInfo.symbol?.let { context.ownerClassSymbol(it)?.cfir }
        if (superOwner !is CfirInterface) return false
        return hasSameOverrideSignature(superInfo)
    }

    /**
     * 官方只要求 struct 及 struct extend 在实现 interface mut 函数时保持 mut 一致。
     */
    private fun CfirExtend.hasStructTarget(): Boolean =
        (extendedTypeRef as? CfirResolvedTypeRef)?.coneType is ConeStructType

    /**
     * 生成实现/覆盖类诊断的去重 key。
     */
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

    /**
     * 生成接口实现要求类诊断的去重 key。
     */
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

    /**
     * 判断成员是否为接口中的 default 成员。
     */
    private fun InheritedMemberInfo.isDefaultInterfaceMember(context: CheckerContext): Boolean {
        if (!isDefault) return false
        val owner = symbol?.let { context.ownerClassSymbol(it)?.cfir }
        return owner is CfirInterface
    }

    /**
     * 构造 extend 目标在诊断中的展示名称。
     */
    context(context: CheckerContext)
    private fun CfirExtend.targetDisplayName(): String =
        "extend " + ((extendedTypeRef as? CfirResolvedTypeRef)?.coneType?.let { type ->
            type.classIdOrPrimitiveClassId?.shortClassName?.asString() ?: type.toString()
        } ?: "<unknown>")

    /**
     * 构造 extend 未实现成员诊断使用的目标类型名称。
     */
    context(context: CheckerContext)
    private fun CfirExtend.targetDiagnosticName(): Name =
        (extendedTypeRef as? CfirResolvedTypeRef)
            ?.coneType
            ?.classIdOrPrimitiveClassId
            ?.shortClassName
            ?: Name.special("<unknown>")

    /**
     * 继承检查使用的统一成员描述。
     *
     * @property name 成员名称。
     * @property kind 成员类别，当前包括 `function`、`property`、`variable`。
     * @property isStatic 成员是否为 static。
     * @property isRedef 函数或属性是否带 redef 语义。
     * @property isConst 函数或属性是否带 const 语义。
     * @property isMut 函数或属性是否带 mut 语义。
     * @property isDefault 是否为接口 default 成员。
     * @property isAbstract 是否仍需要实现方提供实现。
     * @property visibility 成员声明可见性。
     * @property source 成员声明 source，用于声明级诊断。
     * @property nameSource 成员名称 source，用于名称级诊断。
     * @property ownerName 成员所属 class-like 名称。
     * @property symbol 成员绑定后的 callable symbol。
     */
    private data class InheritedMemberInfo(
        /** 成员名称。 */
        val name: Name,
        /** 成员类别，当前包括 `function`、`property`、`variable`。 */
        val kind: String,
        /** 成员是否为 static。 */
        val isStatic: Boolean,
        /** 函数或属性是否带 override 语义。 */
        val isOverride: Boolean,
        /** 函数或属性是否带 redef 语义。 */
        val isRedef: Boolean,
        /** 函数或属性是否带 const 语义。 */
        val isConst: Boolean,
        /** 函数或属性是否带 mut 语义。 */
        val isMut: Boolean,
        /** 是否为接口 default 成员。 */
        val isDefault: Boolean,
        /** 是否仍需要实现方提供实现。 */
        val isAbstract: Boolean,
        /** 成员声明可见性。 */
        val visibility: Visibility,
        /** 成员声明 source，用于声明级诊断。 */
        val source: CjSourceElement?,
        /** 成员名称 source，用于名称级诊断。 */
        val nameSource: AbstractCjSourceElement?,
        /** 成员所属 class-like 名称。 */
        val ownerName: Name?,
        /** 成员绑定后的 callable symbol。 */
        val symbol: CfirCallableSymbol<*>?,
    ) {
        /**
         * 返回 static/non-static 的诊断展示文本。
         */
        val staticKind: String get() = if (isStatic) "static" else "non-static"
    }

    /**
     * extend 接口实现检查中的实现候选。
     *
     * @property info 候选成员的统一继承信息。
     * @property diagnosticSource 默认诊断位置，通常来自被检查的 type ref 或成员名称。
     * @property declarationSource 候选来自当前 extend 声明时的声明 source。
     */
    private data class ExtendImplementationCandidate(
        /** 候选成员的统一继承信息。 */
        val info: InheritedMemberInfo,
        /** 默认诊断位置，通常来自被检查的 type ref 或成员名称。 */
        val diagnosticSource: AbstractCjSourceElement?,
        /** 候选来自当前 extend 声明时的声明 source。 */
        val declarationSource: CjSourceElement?,
    )

    /**
     * 一个默认接口成员在某个 extend 声明中的出现记录。
     *
     * @property ownerExtend 引入该默认成员的 extend 声明。
     * @property info 默认接口成员信息。
     */
    private data class DefaultInterfaceMemberOccurrence(
        /** 引入该默认成员的 extend 声明。 */
        val ownerExtend: CfirExtend,
        /** 默认接口成员信息。 */
        val info: InheritedMemberInfo,
    )

    /**
     * primitive 内建 operator 满足接口 operator 要求的分类。
     */
    private sealed class BuiltinPrimitiveOperatorImplementation {
        /** 内建 operator 返回类型与接口要求一致。 */
        data object Compatible : BuiltinPrimitiveOperatorImplementation()

        /** 内建 operator 返回类型与接口要求不一致。 */
        data object ReturnTypeMismatch : BuiltinPrimitiveOperatorImplementation()
    }

    /**
     * 属性实现与父属性之间的类型不一致信息。
     *
     * @property implementationType 实现方属性类型。
     * @property baseType 被实现的父属性类型。
     */
    private data class PropertyTypeMismatch(
        /** 实现方属性类型。 */
        val implementationType: ConeCangJieType,
        /** 被实现的父属性类型。 */
        val baseType: ConeCangJieType,
    )

    /**
     * 函数返回类型实现冲突。
     */
    private sealed class FunctionReturnTypeConflict {
        /**
         * 普通 override 返回类型不满足父返回类型。
         *
         * @property implementationType 实现方返回类型。
         * @property baseType 被实现的父返回类型。
         */
        data class Mismatch(
            /** 实现方返回类型。 */
            val implementationType: ConeCangJieType,
            /** 被实现的父返回类型。 */
            val baseType: ConeCangJieType,
        ) : FunctionReturnTypeConflict()

        /**
         * 官方 extend/boxing 返回类型不变性冲突。
         *
         * @property interfaceType 触发不变性约束的接口类型。
         */
        data class Invariance(
            /** 触发不变性约束的接口类型。 */
            val interfaceType: ConeCangJieType,
        ) : FunctionReturnTypeConflict()
    }

    /**
     * 把返回类型冲突转换为对应 CFIR 诊断。
     */
    context(context: CheckerContext)
    private fun FunctionReturnTypeConflict.report(
        source: AbstractCjSourceElement?,
        name: Name,
        reporter: DiagnosticReporter,
    ) {
        when (this) {
            is FunctionReturnTypeConflict.Mismatch -> reporter.reportOn(
                source = source,
                factory = CfirErrors.RETURN_TYPE_INCOMPATIBLE,
                a = name,
            )

            is FunctionReturnTypeConflict.Invariance -> reporter.reportOn(
                source = source,
                factory = CfirErrors.RETURN_TYPE_INVARIANCE,
                a = name,
                b = interfaceType,
            )
        }
    }

    /**
     * 属性 mutability 实现冲突的分类。
     */
    private enum class PropertyMutabilityConflict {
        /** 父属性要求 mut，实现方不是 mut。 */
        MutExpected,
        /** 父属性要求 immut，实现方声明为 mut。 */
        ImmutExpected,
    }

    /**
     * 主构造 `let/var` 参数在 CFIR 中复用 property 结构承载 getter/setter，
     * 但官方继承检查把它们作为 VAR_DECL 参与同名成员 shadow 规则。
     */
    private fun CfirProperty.inheritanceMemberKind(): String =
        if (source?.kind == CjFakeSourceElementKind.PropertyFromParameter) "variable" else "property"

    /**
     * 继承成员一致性检查的统一主体。
     *
     * @property declarations 当前主体直接声明的成员。
     * @property inheritedSources 当前主体需要对比的继承来源。
     * @property source 主体声明 source，用作兜底诊断位置。
     * @property classLikeDeclaration class-like 主体；extend 主体为 null。
     */
    private data class MemberInheritanceSubject(
        /** 当前主体直接声明的成员。 */
        val declarations: List<CfirDeclaration>,
        /** 当前主体需要对比的继承来源。 */
        val inheritedSources: List<InheritedMemberSource>,
        /** 主体声明 source，用作兜底诊断位置。 */
        val source: CjSourceElement?,
        /** class-like 主体；extend 主体为 null。 */
        val classLikeDeclaration: CfirClassLikeDeclaration?,
    ) {
        /**
         * 当前主体是否来自 extend 声明。
         */
        val isExtendSubject: Boolean get() = classLikeDeclaration == null
    }

    /**
     * 一个需要纳入继承成员对比的父类型来源。
     *
     * @property typeRef 父类型或 extend 目标类型引用。
     * @property includeDirectExtends 是否额外收集该类型上的直接 extend 成员。
     */
    private data class InheritedMemberSource(
        /** 父类型或 extend 目标类型引用。 */
        val typeRef: CfirTypeRef,
        /** 是否额外收集该类型上的直接 extend 成员。 */
        val includeDirectExtends: Boolean,
    )

    /**
     * 将 class-like 声明适配为继承成员一致性检查主体。
     */
    private fun CfirClassLikeDeclaration.memberInheritanceSubject(): MemberInheritanceSubject =
        MemberInheritanceSubject(
            declarations = declarations,
            inheritedSources = superTypeRefs.map { InheritedMemberSource(it, includeDirectExtends = true) },
            source = source,
            classLikeDeclaration = this,
        )

    /**
     * 将 extend 声明适配为继承成员一致性检查主体。
     *
     * extend 需要同时对比目标类型成员和 super interface 成员，但不再重复把
     * 这些来源上的直接 extend 成员并入。
     */
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

    /**
     * 解析类型引用指向的 class-like 声明。
     */
    context(context: CheckerContext)
    private fun CfirTypeRef.resolvedClassLikeDeclaration(): CfirClassLikeDeclaration? {
        val classId = (this as? CfirResolvedTypeRef)?.coneType?.expandedClassIdOrPrimitiveClassId ?: return null
        val symbol = context.session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        return symbol.cfir as? CfirClassLikeDeclaration
    }

    /**
     * 从类型引用创建 use-site member scope。
     */
    context(context: CheckerContext)
    private fun CfirTypeRef.resolvedUseSiteMemberScope(excludingExtend: CfirExtend? = null): CfirTypeScope? {
        val coneType = (this as? CfirResolvedTypeRef)?.coneType ?: return null
        return coneType.resolvedUseSiteMemberScope(excludingExtend)
    }

    /**
     * 为具体类型创建继承检查使用的 use-site member scope。
     *
     * class-like 类型使用 declaration-site scope 再套 substitution scope；
     * 内建/非 class target 则由 extend member scope 与直接父类型 scope 组合而成。
     */
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
        return CfirCompositeTypeScope(scopes, context.session)
    }

}

/**
 * extend 声明也属于官方 `InheritableDecl`，需要进入同一组继承成员一致性检查。
 */
object CfirExtendInheritanceDeepChecker : CfirExtendChecker() {
    /**
     * 将 extend 声明转发到继承深层检查器。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirExtend) {
        CfirInheritanceDeepChecker.checkExtend(declaration)
    }
}
