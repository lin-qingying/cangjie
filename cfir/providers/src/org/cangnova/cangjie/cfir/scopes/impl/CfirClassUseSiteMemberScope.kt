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

package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.resolve.substitution.createTypeSubstitutorByTypeConstructor

import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.ScopeSessionCacheOwner
import org.cangnova.cangjie.cfir.scopes.CfirRequiredMembersPhaseStamp
import org.cangnova.cangjie.cfir.scopes.createMemberDeclarationSubstitutor
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.fullyExpandedTypeUsingAbbreviation
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeDescriptor
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.DeclaredSupertypeClassification
import org.cangnova.cangjie.cfir.resolve.providers.classifyDeclaredSupertype
import org.cangnova.cangjie.cfir.resolve.providers.createCallableOwnerUseSiteSubstitutor
import org.cangnova.cangjie.cfir.resolve.providers.getContainingClass
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.resolve.providers.getDeclarationPackage
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenanceScope
import org.cangnova.cangjie.cfir.scopes.CfirCallableWithLookupProvenance
import org.cangnova.cangjie.cfir.scopes.CfirMemberScopeOwner
import org.cangnova.cangjie.cfir.scopes.CfirFunctionInstantiationScope
import org.cangnova.cangjie.cfir.scopes.CfirFunctionInstantiationTable
import org.cangnova.cangjie.cfir.scopes.CfirFunctionMemberTableKind
import org.cangnova.cangjie.cfir.scopes.CfirStoredFunctionMember
import org.cangnova.cangjie.cfir.scopes.CfirInheritanceMemberKind
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.isStaticMemberForOverride
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.scopes.processCallablesByNameWithLookupProvenance
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * class-like receiver 的 use-site 成员 scope。
 *
 * 声明成员在 classifier/property 隐藏规则中优先于 extend 成员，函数成员则与 extend 成员合并。
 */
enum class CfirClassMemberScopeKind {
    /**
     * 接收者 use-site scope。
     *
     * 用于调用解析，允许看到 extend 注入成员以及类型感知后的父类型链。
     */
    USE_SITE,

    /**
     * 类型本体 body 查找 scope。
     *
     * 对齐官方 `LookUpImpl::ProcessStructDeclBody`：当前类型本体不查找自己的 extend 成员，
     * 但沿父类递归查找时父类型按 `lookupExtend=true` 处理，因此可见父类 extend 成员。
     */
    BODY_LOOKUP,

    /**
     * 声明检查 scope。
     *
     * 仅反映源码显式声明的继承关系，不把 extend 注入的父接口/成员视为类本体义务。
     */
    DECLARATION_SITE,
}

/**
 * class-like 类型 use-site 成员 scope。
 *
 * 该 scope 统一处理声明成员、extend 成员、父类型成员、泛型 owner 替换和直接覆盖查询。
 */
class CfirClassUseSiteMemberScope internal constructor(
    /**
     * 当前成员查找发生的 use-site session。
     */
    private val session: CfirSession,
    /**
     * 被查询成员的 class-like symbol。
     */
    private val classSymbol: CfirClassLikeSymbol<*>,
    /** 当前查找与全部父 scope 共用的缓存会话。 */
    private val scopeSession: ScopeSession,
    /**
     * 用于解析父类型与关联 class-like symbol 的 symbol provider。
     */
    private val symbolProvider: CfirSymbolProvider,
    /**
     * 当前 scope 所代表 owner 的具体类型，泛型 owner 替换从这里提取实参。
     */
    private val ownerType: ConeCangJieType? = declarationSelfType(classSymbol),
    /**
     * 当前继承路径在最终实例化之前的 owner 类型。
     *
     * 默认取 class 声明的 self type；需要表达 owner 特有约束时由调用方显式传入该 owner
     * 的目标类型。这样具体 use-site 的替换不会改写 override 图中的 `I<X>`/`I<Y>` 身份。
     */
    private val inheritanceProvenanceOwnerType: ConeCangJieType? = declarationSelfType(classSymbol),
    /**
     * 调用点 dispatch receiver 类型，父 scope substitution override 需要保留原始 receiver。
     */
    private val dispatchReceiverType: ConeCangJieType? = ownerType,
    /**
     * 当前成员查找模式，决定是否纳入 extend 成员以及父类型是否使用 use-site 语义。
     */
    private val scopeKind: CfirClassMemberScopeKind = CfirClassMemberScopeKind.USE_SITE,
    /**
     * 是否允许裸泛型静态 qualifier 暂缓 extend 泛型适用性判断。
     */
    private val allowBareGenericStaticQualifierExtends: Boolean = false,
    /**
     * 在 extend 声明体内构造 scope 时需要排除的当前 extend。
     */
    private val excludingExtend: CfirExtend? = null,
    /** 当前 scope 是否经某条 extend 接口边进入 receiver 的继承图。 */
    private val inheritedLookupProvenance: CfirCallableLookupProvenance = CfirCallableLookupProvenance.None,
    /** 当前成员图的 owner 视角，负责区分名义 owner、extend owner 与普通 use-site 查找。 */
    private val scopeOwner: CfirMemberScopeOwner = CfirMemberScopeOwner.UseSite,
    /** STATUS 等前置阶段使用 RAW；CJMP_MATCHING 后的 body/use-site 查询可使用实际化成员图。 */
    private val memberScopeView: CfirCjmpMemberScopeView = CfirCjmpMemberScopeView.RAW,
    /** provider 已完成准备的共享声明、来源快照、有序父图和同域阶段保障。 */
    private val declaredScope: CfirClassDeclaredMemberScope,
    internal val declarationMemberSnapshot: CfirCjmpMemberSnapshot?,
    internal val parentScopeInputs: List<Pair<CfirInstantiatedSupertypeDescriptor, CfirTypeScope>>,
    private val extendScope: CfirExtendMemberScope?,
    private val memberInstantiationInput: CfirMemberInstantiationInput,
    private val requiredMembersPhaseStamp: CfirRequiredMembersPhaseStamp,
    internal val cacheOwner: ScopeSessionCacheOwner,
    internal val cacheGeneration: Long,
    internal val cacheDependencies: Set<org.cangnova.cangjie.cfir.scopes.CfirScopeCacheEpoch>,
) : CfirTypeScope(), CfirFunctionInheritanceScope, CfirFunctionInstantiationScope, CfirPropertyInheritanceScope, CfirCallableLookupProvenanceScope,
    CfirMemberLookupCompletenessScope, CfirCjmpMemberSubstitutorProvider {
    companion object {
        /**
         * 为具体接收者取得共享结构成员图；来源、阶段和缓存生命周期统一由 provider 负责。
         * 文件可见性仍由使用真实 lookup provenance 的访问检查器判定。
         */
        fun createForUseSiteType(
            session: CfirSession,
            ownerType: ConeCangJieType,
            scopeSession: ScopeSession,
            excludingExtend: CfirExtend? = null,
            scopeOwner: CfirMemberScopeOwner = CfirMemberScopeOwner.UseSite,
            memberScopeView: CfirCjmpMemberScopeView = CfirCjmpMemberScopeView.RAW,
            memberRequiredPhase: CfirResolvePhase?,
        ): CfirTypeScope? = session.cangjieScopeProvider.getMemberScopeForUseSiteType(
            session, ownerType, scopeSession, excludingExtend, scopeOwner, memberScopeView, memberRequiredPhase,
        )
    }

    internal val declarationMembers: List<CfirDeclaration>
        get() = declaredScope.declarations

    private val publishedInstantiationFunctions = hashMapOf<Name, CfirFunctionInstantiationTable>()
    private val instantiationFunctions = hashMapOf<Name, CfirFunctionInstantiationTable>()
    private var instantiationConstructors: CfirFunctionInstantiationTable? = null

    /**
     * 对位 CheckMembersWithInheritedDecls 的三个独立容器：接口输入、实例输入、可见扩展输入。
     * 当前 nominal 的 direct extend 不写进 nominal 发布表；父类查询通过继承入口单独附加。
     */
    override fun publishedFunctionMemberTable(name: Name): CfirFunctionInstantiationTable {
        check(memberScopeView == CfirCjmpMemberScopeView.CJMP_INSTANTIATION)
        if (scopeOwner is CfirMemberScopeOwner.ExtendInstantiation) {
            return checkNotNull(extendScope).publishedFunctionMemberTable(name)
        }
        return publishedInstantiationFunctions.getOrPut(name) {
            val interfaces = CfirFunctionInstantiationTableBuilder(session, scopeOwner.extendOwner)
            val instances = CfirFunctionInstantiationTableBuilder(session, scopeOwner.extendOwner)
            val visible = CfirFunctionInstantiationTableBuilder(session, scopeOwner.extendOwner)
            for ((descriptor, parent) in parentScopeInputs) {
                // 名义声明的 inheritedTypes 不包含当前/传播的 extend 接口边；它们由父类的 extend 表提供。
                if (descriptor.origin is CfirInstantiatedSupertypeOrigin.Extend) continue
                val input = checkNotNull(parent as? CfirFunctionInstantiationScope) {
                    "An instantiation parent scope must retain its stored member table: $parent"
                }
                val table = input.publishedFunctionMemberTable(name)
                if (table.kind == CfirFunctionMemberTableKind.INTERFACE) {
                    interfaces.addTable(table, inheritedInterfaces = true)
                } else {
                    instances.addTable(input.inheritedFunctionMemberTable(name, classSymbol.cfir))
                }
            }
            visible.addTable(instances.finish(CfirFunctionMemberTableKind.INSTANCE))
            for (member in ownInstantiationFunctions(name)) {
                visible.update(member)
                interfaces.addOwnDeclaration(member.declaration)
            }
            interfaces.addTable(visible.finish(CfirFunctionMemberTableKind.INSTANCE))
            interfaces.removePrivateMembers()
            interfaces.clearPublishedTransientState()
            interfaces.finish(if (classSymbol.cfir is CfirInterface) {
                CfirFunctionMemberTableKind.INTERFACE
            } else {
                CfirFunctionMemberTableKind.INSTANCE
            })
        }
    }

    /** 父类发布表与它可见的 direct extend 按原生 GetInheritedSuperMembers 顺序归并。 */
    override fun inheritedFunctionMemberTable(name: Name, useSiteOwner: CfirDeclaration): CfirFunctionInstantiationTable {
        val published = publishedFunctionMemberTable(name)
        if (published.kind == CfirFunctionMemberTableKind.INTERFACE) return published
        val builder = CfirFunctionInstantiationTableBuilder(session, scopeOwner.extendOwner)
        val useSitePackage = if (useSiteOwner is CfirExtend) useSiteOwner.getDeclarationPackage() else {
            useSiteOwner.symbol.getDeclarationPackage()
        }
        builder.addTable(published.visibleForInheritance(useSitePackage))
        extendScope?.processPublishedOwnerTablesForInheritance(name, useSiteOwner) { builder.addTable(it) }
        return builder.finish(CfirFunctionMemberTableKind.INSTANCE)
    }

    /** 实例化补入只改变副本；父类型永远读取已清理的发布表。 */
    override fun functionInstantiationTable(name: Name): CfirFunctionInstantiationTable {
        check(memberScopeView == CfirCjmpMemberScopeView.CJMP_INSTANTIATION)
        if (scopeOwner is CfirMemberScopeOwner.ExtendInstantiation) {
            return checkNotNull(extendScope).functionInstantiationTable(name)
        }
        return instantiationFunctions.getOrPut(name) {
            val builder = CfirFunctionInstantiationTableBuilder(session, scopeOwner.extendOwner)
            builder.copyFrom(publishedFunctionMemberTable(name))
            ownInstantiationFunctions(name).filter { it.isPrivate }.forEach { member ->
                builder.addOwnDeclaration(member.declaration)
                builder.update(member.copy(upperBounds = emptyMap()))
            }
            builder.finish(CfirFunctionMemberTableKind.INSTANTIATION_COPY)
        }
    }

    private fun ownInstantiationFunctions(name: Name): List<CfirStoredFunctionMember> {
        val input = checkNotNull(memberInstantiationInput as? CfirMemberInstantiationInput.Complete)
        return input.headers.values.filter { it.kind == CfirInheritanceMemberKind.FUNCTION && !it.flags.isConstructor && it.name == name }.map { header ->
            val mapping = input.substitutions.getValue(header.declaration)
            checkNotNull(header.bindInheritanceMember(this, mapping.structType,
                createTypeSubstitutorByTypeConstructor(mapping.replacements, session.typeContext, false), null,
                inheritedLookupProvenance).function)
        }
    }

    private fun CfirFunctionInstantiationTable.visibleForInheritance(useSitePackage: FqName?): CfirFunctionInstantiationTable =
        CfirFunctionInstantiationTable(
            members.filter { !it.isPrivate && (it.declaration.cfir.status.visibility == Visibilities.Protected ||
                    session.accessibilityChecker.isCallableVisibleFromPackage(it.declaration, useSitePackage)) },
            updates, emptySet(), kind,
        )

    override fun constructorInstantiationTable(): CfirFunctionInstantiationTable {
        check(memberScopeView == CfirCjmpMemberScopeView.CJMP_INSTANTIATION)
        if (scopeOwner is CfirMemberScopeOwner.ExtendInstantiation) {
            return checkNotNull(extendScope).constructorInstantiationTable()
        }
        instantiationConstructors?.let { return it }
        val builder = CfirFunctionInstantiationTableBuilder(session)
        val input = checkNotNull(memberInstantiationInput as? CfirMemberInstantiationInput.Complete)
        for (header in input.headers.values.filter { it.flags.isConstructor }) {
            val mapping = input.substitutions.getValue(header.declaration)
            builder.addOwnDeclaration(header.declaration)
            builder.update(checkNotNull(header.bindInheritanceMember(this, mapping.structType,
                createTypeSubstitutorByTypeConstructor(mapping.replacements, session.typeContext, false), null,
                CfirCallableLookupProvenance.None).function))
        }
        return builder.finish(CfirFunctionMemberTableKind.INSTANTIATION_COPY).also { instantiationConstructors = it }
    }

    /** 容器身份消费已完成 family/父 scope 投影，不按 symbol 的物理 session 推断 logical owner。 */
    override fun cjmpMemberOwnerFor(symbol: CfirCallableSymbol<*>): CfirCjmpMemberOwnerProjection? {
        val original = symbol.unwrapSubstitutionOverrides().cfir
        val family = declarationMemberSnapshot
        val physical = family?.physicalOwnerFor(original)
        if (physical != null) return CfirCjmpMemberOwnerProjection(physical,
            checkNotNull(family.logicalOwnerFor(original)) { "A prepared CJMP member requires its logical owner" })
        val inherited = parentScopeInputs.mapNotNull { (_, parent) ->
            (parent as? CfirCjmpMemberSubstitutorProvider)?.cjmpMemberOwnerFor(symbol)
        }.distinct()
        check(inherited.size <= 1) { "An inherited declaration has conflicting logical owner projections" }
        return inherited.singleOrNull()
    }

    /** 取得 common 物理 owner 成员到 specific 实例 owner 的类型替换。 */
    override fun cjmpOwnerSubstitutorFor(symbol: CfirCallableSymbol<*>): ConeSubstitutor? =
        cjmpOwnerSubstitutorFor(symbol, ownerSubstitutor)

    override fun cjmpOwnerSubstitutorFor(
        symbol: CfirCallableSymbol<*>,
        declarationOwnerSubstitutor: ConeSubstitutor,
    ): ConeSubstitutor? {
        val family = declarationMemberSnapshot ?: return null
        val originalDeclaration = symbol.unwrapSubstitutionOverrides().cfir
        val mapping = family.ownerTypeParameterMappingFor(originalDeclaration)
        if (mapping.isEmpty()) return null
        val replacements: Map<TypeConstructorMarker, ConeCangJieType> = mapping.map { (common, specific) ->
            common.symbol.toLookupTag() to declarationOwnerSubstitutor.substituteOrSelf(specific.symbol.constructType())
        }.toMap()
        return createTypeSubstitutorByTypeConstructor(replacements, session.typeContext, false)
    }

    /** 当前有序父 scope 来自 provider 完成的共享输入。 */
    private val parentScopes: List<CfirTypeScope> by lazy { parentScopeInputs.map { it.second } }

    /**
     * 当前声明及合法父链中因 recoverable nominal 父边造成的 lookup blocker。
     *
     * blocker 沿已经构造的父 scope 传播，不重新遍历父类型图；错误父边自身仍不会进入
     * [parentScopes]，因此这里不会恢复其成员。
     */
    override val memberLookupBlockers: List<CfirMemberLookupBlocker> by lazy {
        buildSet {
            for (superTypeRef in classSymbol.cfir.superTypeRefs) {
                val classification = superTypeRef.classifyDeclaredSupertype(session)
                if (classification is DeclaredSupertypeClassification.RecoverableNominalError) {
                    add(
                        CfirMemberLookupBlocker(
                            ownerSymbol = classSymbol,
                            rootDiagnostic = classification.errorType.diagnostic,
                        )
                    )
                }
            }
            for (parentScope in parentScopes) {
                addAll(
                    (parentScope as? CfirMemberLookupCompletenessScope)
                        ?.memberLookupBlockers
                        .orEmpty()
                )
            }
        }.toList()
    }

    /**
     * 按名称缓存的函数结果。
     */
    private val functions = hashMapOf<Name, Collection<CfirFunctionInheritanceProvenance>>()

    /**
     * 按名称缓存的属性结果。
     */
    private val properties = hashMapOf<Name, Collection<MemberWithBaseScope<CfirPropertySymbol>>>()

    /**
     * 父 scope 函数候选缓存。
     */
    private val functionsFromParents = hashMapOf<Name, List<CfirFunctionInheritanceProvenance>>()

    /**
     * 父 scope 归并前函数候选缓存。
     */
    private val unmergedFunctionsFromParents = hashMapOf<Name, List<CfirFunctionInheritanceProvenance>>()

    /**
     * 父 scope 属性候选缓存。
     */
    private val propertiesFromParents = hashMapOf<Name, List<MemberWithBaseScope<CfirPropertySymbol>>>()

    /** 父 scope 归并前属性候选缓存。 */
    private val unmergedPropertiesFromParents = hashMapOf<Name, List<MemberWithBaseScope<CfirPropertySymbol>>>()

    /**
     * 直接覆盖函数缓存。
     */
    private val directOverriddenFunctions = hashMapOf<CfirNamedFunctionSymbol, List<CfirFunctionInheritanceProvenance>>()

    /**
     * 直接覆盖属性缓存。
     */
    private val directOverriddenProperties = hashMapOf<CfirPropertySymbol, List<MemberWithBaseScope<CfirPropertySymbol>>>()

    /**
     * owner 类型参数替换器。
     */
    private val ownerSubstitutor: ConeSubstitutor by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ownerType?.let { classSymbol.createDeclarationSubstitutor(it) } ?: ConeSubstitutor.Empty
    }

    /**
     * 当前 scope 可见 callable 名称缓存。
     */
    private val callableNamesCached by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildSet {
            addAll(declaredScope.getCallableNames())
            addAll(extendCallableNames())
            addAll(parentCallableNames())
        }
    }

    /**
     * 当前 scope 可见 classifier 名称缓存。
     */
    private val classifierNamesCached by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildSet {
            addAll(declaredScope.getClassifierNames())
            addAll(extendClassifierNames())
            addAll(parentClassifierNames())
        }
    }

    /**
     * 返回可见 callable 名称集合。
     */
    override fun getCallableNames(): Set<Name> = callableNamesCached

    /**
     * 返回可见 classifier 名称集合。
     */
    override fun getClassifierNames(): Set<Name> = classifierNamesCached

    /**
     * 处理指定函数的直接覆盖成员。
     */
    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction
    ): ProcessorAction = processDirectOverriddenFunctionsWithProvenance(functionSymbol) { provenance ->
        processor(provenance.member, provenance.baseScope)
    }

    /**
     * 处理指定函数的直接覆盖输入，并保留其泛型继承来源。
     */
    override fun processDirectOverriddenFunctionsWithProvenance(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirFunctionInheritanceProvenance) -> ProcessorAction,
    ): ProcessorAction {
        val directOverridden = directOverriddenFunctions[functionSymbol]
            ?: computeDirectOverriddenForOwnFunctionIfAny(functionSymbol)
            ?: getFunctionsFromParentsByName(functionSymbol.name)
                .firstOrNull { it.member == functionSymbol }
                ?.let { parentMember ->
                    val inheritanceScope = parentMember.baseScope.requireFunctionInheritanceScope()
                    return inheritanceScope.processDirectOverriddenFunctionsWithProvenance(functionSymbol, processor)
                }
            ?: return ProcessorAction.NONE
        for (candidate in directOverridden) {
            if (processor(candidate) == ProcessorAction.STOP) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NONE
    }

    /**
     * 处理指定属性的直接覆盖成员。
     */
    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        val directOverridden = directOverriddenProperties[propertySymbol]
            ?: computeDirectOverriddenForOwnPropertyIfAny(propertySymbol)
            ?: getPropertiesFromParentsByName(propertySymbol.name)
                .firstOrNull { it.symbol == propertySymbol }
                ?.let { parentMember ->
                    return parentMember.scope.processDirectOverriddenPropertiesWithBaseScope(propertySymbol, processor)
                }
            ?: return ProcessorAction.NONE
        for (candidate in directOverridden) {
            if (processor(candidate.symbol, candidate.scope) == ProcessorAction.STOP) {
                return ProcessorAction.STOP
            }
        }
        return ProcessorAction.NONE
    }

    // direct-overridden 查询可能先于按名称收集成员发生；这里按需建立与 collectFunctions/collectProperties 相同的缓存。
    /**
     * 若 [functionSymbol] 是当前 scope 自有函数，则计算其直接覆盖成员。
     */
    private fun computeDirectOverriddenForOwnFunctionIfAny(
        functionSymbol: CfirNamedFunctionSymbol,
    ): List<CfirFunctionInheritanceProvenance>? {
        if (!containsOwnFunction(functionSymbol)) return null
        return directOverriddenFunctions.getOrPut(functionSymbol) {
            computeDirectOverriddenForDeclaredFunction(functionSymbol)
        }
    }

    /**
     * 若 [propertySymbol] 是当前 scope 自有属性，则计算其直接覆盖成员。
     */
    private fun computeDirectOverriddenForOwnPropertyIfAny(
        propertySymbol: CfirPropertySymbol,
    ): List<MemberWithBaseScope<CfirPropertySymbol>>? {
        if (!containsOwnProperty(propertySymbol)) return null
        return directOverriddenProperties.getOrPut(propertySymbol) {
            computeDirectOverriddenForDeclaredProperty(propertySymbol)
        }
    }

    /**
     * 判断函数是否来自当前声明成员或当前 extend scope。
     */
    private fun containsOwnFunction(functionSymbol: CfirNamedFunctionSymbol): Boolean {
        var found = false
        declaredScope.processFunctionsByName(functionSymbol.name) {
            if (it == functionSymbol) found = true
        }
        if (found) return true

        extendScope?.processFunctionsByName(functionSymbol.name) {
            if (it == functionSymbol) found = true
        }
        return found
    }

    /**
     * 判断属性是否来自当前声明成员或当前 extend scope。
     */
    private fun containsOwnProperty(propertySymbol: CfirPropertySymbol): Boolean {
        var declaredPropertyWithSameName = false
        var found = false
        declaredScope.processPropertiesByName(propertySymbol.name) {
            declaredPropertyWithSameName = true
            if (it == propertySymbol) found = true
        }
        if (declaredPropertyWithSameName) return found

        extendScope?.processPropertiesByName(propertySymbol.name) {
            if (it == propertySymbol) found = true
        }
        return found
    }

    /**
     * 使用新的 session 重建当前 scope。
     */
    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession,
    ): CfirTypeScope? = newSession.cangjieScopeProvider.getUseSiteMemberScope(
        useSiteSession = newSession,
        scopeSession = newScopeSession,
        classSymbol = classSymbol,
        symbolProvider = newSession.symbolProvider,
        extendProvider = newSession.extendProviderOrNull,
        directSupertypeProvider = newSession.directSupertypeProviderOrNull,
        ownerType = ownerType,
        inheritanceProvenanceOwnerType = inheritanceProvenanceOwnerType,
        dispatchReceiverType = dispatchReceiverType,
        scopeKind = scopeKind,
        allowBareGenericStaticQualifierExtends = allowBareGenericStaticQualifierExtends,
        excludingExtend = excludingExtend,
        inheritedLookupProvenance = inheritedLookupProvenance,
        supertypePath = CfirSupertypePath.root(classSymbol),
        scopeOwner = scopeOwner,
        memberScopeView = memberScopeView,
        memberRequiredPhase = requiredMembersPhaseStamp.highestRequiredPhase,
    )

    /**
     * 按名称处理可见 classifier。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        if (name !in getClassifierNames()) return

        val local = mutableListOf<CfirClassLikeSymbol<*>>()
        declaredScope.processClassifiersByName(name) { local += it }
        if (local.isEmpty()) {
            extendScope?.processClassifiersByName(name) { local += it }
        }
        if (local.isNotEmpty()) {
            local.forEach(processor)
            return
        }
        for (parent in parentScopes) {
            parent.processClassifiersByName(name, processor)
        }
    }

    /**
     * 处理当前声明的构造器。
     */
    override fun processDeclaredConstructors(processor: (CfirConstructorSymbol) -> Unit) {
        declaredScope.processDeclaredConstructors(processor)
    }

    /**
     * 按名称处理可见函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        if (name !in getCallableNames()) return

        functions.getOrPut(name) {
            collectFunctions(name)
        }.forEach { processor(it.member) }
    }

    /**
     * 按名称处理最终可见函数及其继承来源。
     */
    override fun processFunctionsByNameWithProvenance(
        name: Name,
        processor: (CfirFunctionInheritanceProvenance) -> Unit,
    ) {
        if (name !in getCallableNames()) return

        functions.getOrPut(name) {
            collectFunctions(name)
        }.forEach(processor)
    }

    /**
     * 按名称处理归并前的直接继承输入，并保留每个父 scope 的 provenance。
     */
    override fun processUnmergedInheritedFunctionsByNameWithProvenance(
        name: Name,
        processor: (CfirFunctionInheritanceProvenance) -> Unit,
    ) {
        if (name !in getCallableNames()) return
        getUnmergedFunctionsFromParentsByName(name).forEach(processor)
    }

    /**
     * 收集本 scope 与父 scope 中的可见函数。
     */
    private fun collectFunctions(name: Name): Collection<CfirFunctionInheritanceProvenance> = buildList {
        val local = mutableListOf<CfirNamedFunctionSymbol>()
        declaredScope.processFunctionsByName(name) { local += it }
        extendScope?.processFunctionsByName(name) { local += it }
        for (symbol in local) {
            directOverriddenFunctions[symbol] = computeDirectOverriddenForDeclaredFunction(symbol)
            add(symbol.localFunctionInheritanceProvenance())
        }

        for (candidate in getFunctionsFromParentsByName(name)) {
            if (candidate.isImplementedByLocalFunction(local) &&
                !candidate.member.isExtendMemberForOverrideResolution()
            ) {
                continue
            }
            add(candidate)
        }
    }

    /**
     * 判断当前 scope 的直接函数是否在声明级实现了父成员。
     *
     * 父、子函数分别使用继承图中的声明 owner 类型做比较，不能因为某个具体实参令签名
     * 偶然相等就提前合并；实例化冲突由后续消费者在替换后的签名上检查。
     */
    private fun CfirFunctionInheritanceProvenance.isImplementedByLocalFunction(
        localFunctions: List<CfirNamedFunctionSymbol>,
    ): Boolean = localFunctions.any { localFunction ->
        if (!localFunction.localFunctionInheritanceProvenance().overridesByDeclaration(this, session)) {
            return@any false
        }
        if (scopeOwner.extendOwner == null) return@any true

        val localOwnerExtend = localFunction.getContainingExtend()
            ?: return@any localFunction.cfir.status.visibility != Visibilities.Private
        lookupProvenance.sourceExtend === localOwnerExtend ||
                (lookupProvenance.sourceExtend == null &&
                        localFunction.cfir.status.visibility != Visibilities.Private)
    }

    /**
     * 按名称处理可见属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        if (name !in getCallableNames()) return

        properties.getOrPut(name) {
            collectProperties(name)
        }.forEach { processor(it.symbol) }
    }

    /** 按名称处理归并前的直接继承属性及其来源。 */
    override fun processUnmergedInheritedPropertiesByNameWithProvenance(
        name: Name,
        processor: (CfirPropertyInheritanceProvenance) -> Unit,
    ) {
        if (name !in getCallableNames()) return
        getUnmergedPropertiesFromParentsByName(name).forEach {
            processor(CfirPropertyInheritanceProvenance(it.symbol, it.scope, it.lookupProvenance))
        }
    }

    /**
     * 收集本 scope 与父 scope 中的可见属性。
     */
    private fun collectProperties(name: Name): Collection<MemberWithBaseScope<CfirPropertySymbol>> = buildList {
        val local = mutableListOf<CfirPropertySymbol>()
        declaredScope.processPropertiesByName(name) { local += it }
        if (local.isEmpty()) {
            extendScope?.processPropertiesByName(name) { local += it }
        }
        if (local.isNotEmpty()) {
            for (symbol in local) {
                directOverriddenProperties[symbol] = computeDirectOverriddenForDeclaredProperty(symbol)
                add(
                    MemberWithBaseScope(
                        symbol = symbol,
                        scope = this@CfirClassUseSiteMemberScope,
                        lookupProvenance = symbol.localCallableLookupProvenance(),
                    )
                )
            }
            return@buildList
        }
        for (candidate in getPropertiesFromParentsByName(name)) {
            add(candidate)
        }
    }

    /**
     * 计算声明函数覆盖的父函数集合。
     */
    private fun computeDirectOverriddenForDeclaredFunction(
        functionSymbol: CfirNamedFunctionSymbol,
    ): List<CfirFunctionInheritanceProvenance> {
        val declaredFunction = functionSymbol.localFunctionInheritanceProvenance()
        return getFunctionsFromParentsByName(functionSymbol.name)
            .filter { declaredFunction.overridesByDeclaration(it, session) }
    }

    /**
     * 计算声明属性覆盖的父属性集合。
     */
    private fun computeDirectOverriddenForDeclaredProperty(
        propertySymbol: CfirPropertySymbol,
    ): List<MemberWithBaseScope<CfirPropertySymbol>> {
        return getPropertiesFromParentsByName(propertySymbol.name)
            .filter { propertySymbol.overridesPropertyCandidate(it.symbol, propertySymbol.overrideSubstitutorForOwnProperty()) }
    }

    /**
     * 返回指定名称的父函数候选。
     */
    private fun getFunctionsFromParentsByName(name: Name): List<CfirFunctionInheritanceProvenance> {
        return functionsFromParents.getOrPut(name) {
            getUnmergedFunctionsFromParentsByName(name)
                .filterOutOverriddenFunctions()
                .filterOutAbstractFunctionsImplementedByConcrete(session)
                .filterOutInterfaceDefaultFunctionsImplementedByConcrete()
                .mergeEquivalentInheritedFunctions()
                .toList()
        }
    }

    /**
     * 收集当前 scope 的直接父候选，保留覆盖和实现关系归并前的输入。
     *
     * 这里只排除语言上不可继承的成员。是否由某个实现满足属于后续 effective graph
     * 归并；抽象义务检查必须先保留 requirement，再判断实现是否在使用点可见。
     */
    private fun getUnmergedFunctionsFromParentsByName(
        name: Name,
    ): List<CfirFunctionInheritanceProvenance> {
        return unmergedFunctionsFromParents.getOrPut(name) {
            val parentCandidates = mutableListOf<CfirFunctionInheritanceProvenance>()
            for (parent in parentScopes) {
                parent.requireFunctionInheritanceScope().processFunctionsByNameWithProvenance(name) {
                    parentCandidates += it
                }
            }
            parentCandidates
                .filterInheritedFunctionsForCurrentScope()
                .toList()
        }
    }

    /**
     * 返回指定名称的父属性候选。
     */
    private fun getPropertiesFromParentsByName(name: Name): List<MemberWithBaseScope<CfirPropertySymbol>> {
        return propertiesFromParents.getOrPut(name) {
            val inheritableParentCandidates = getUnmergedPropertiesFromParentsByName(name)
            filterOutOverridden(
                inheritableParentCandidates,
                CfirTypeScope::processDirectOverriddenPropertiesWithBaseScope,
            ).filterOutAbstractMembersImplementedByConcrete(
                { candidate -> canImplementPropertyCandidate(candidate) },
            ).filterOutInterfaceDefaultMembersImplementedByConcrete(
                { candidate -> canImplementPropertyCandidate(candidate) },
            ).toList()
        }
    }

    /** 收集父类型的属性输入，延后 effective candidate 过滤供继承检查读取。 */
    private fun getUnmergedPropertiesFromParentsByName(
        name: Name,
    ): List<MemberWithBaseScope<CfirPropertySymbol>> {
        return unmergedPropertiesFromParents.getOrPut(name) {
            val parentCandidates = mutableListOf<MemberWithBaseScope<CfirPropertySymbol>>()
            for (parent in parentScopes) {
                parent.processCallablesByNameWithLookupProvenance(name) { candidate ->
                    val property = candidate.symbol as? CfirPropertySymbol ?: return@processCallablesByNameWithLookupProvenance
                    parentCandidates += MemberWithBaseScope(
                        symbol = property,
                        scope = parent,
                        lookupProvenance = candidate.provenance,
                    )
                }
            }
            parentCandidates.filterInheritedForCurrentScope()
        }
    }

    /**
     * 父类型成员进入当前 class use-site scope 前先执行官方继承过滤：
     * private 父成员不是子类型的继承成员，不能污染调用解析或接口 default 合并。
     */
    private fun <S : CfirCallableSymbol<*>> Collection<MemberWithBaseScope<S>>.filterInheritedForCurrentScope():
            List<MemberWithBaseScope<S>> =
        filter { it.symbol.canBeInheritedIntoCurrentScope() }

    /**
     * 过滤不能进入当前 class 的继承函数，同时保留来源身份。
     */
    private fun Collection<CfirFunctionInheritanceProvenance>.filterInheritedFunctionsForCurrentScope():
            List<CfirFunctionInheritanceProvenance> =
        filter { it.member.canBeInheritedIntoCurrentScope() }

    /**
     * 父类 concrete 成员实现同签名 interface default 后，interface default 不再参与 use-site 候选。
     *
     * 该规则覆盖函数与属性，并与抽象成员过滤并列放在父候选合并层，避免调用解析和继承检查各自发明规则。
     */
    private fun <S : CfirCallableSymbol<*>> Collection<MemberWithBaseScope<S>>.filterOutInterfaceDefaultMembersImplementedByConcrete(
        overridesCandidate: S.(S) -> Boolean,
    ): Collection<MemberWithBaseScope<S>> {
        return filter { candidate ->
            if (!candidate.symbol.isInterfaceDefaultMemberForCurrentScope()) return@filter true

            none { concreteCandidate ->
                concreteCandidate !== candidate &&
                        concreteCandidate.symbol.isConcreteClassMemberForCurrentScope() &&
                        concreteCandidate.symbol.overridesCandidate(candidate.symbol)
            }
        }
    }

    /**
     * 函数 provenance 版本的 interface default/concrete 归并。
     */
    private fun Collection<CfirFunctionInheritanceProvenance>.filterOutInterfaceDefaultFunctionsImplementedByConcrete():
            Collection<CfirFunctionInheritanceProvenance> {
        return filter { candidate ->
            if (!candidate.member.isInterfaceDefaultMemberForCurrentScope()) return@filter true

            none { concreteCandidate ->
                concreteCandidate !== candidate &&
                        concreteCandidate.member.isConcreteClassMemberForCurrentScope() &&
                        concreteCandidate.overridesByDeclaration(candidate, session)
            }
        }
    }

    /**
     * 判断父 callable 是否可以作为当前 class 的继承成员。
     */
    private fun CfirCallableSymbol<*>.canBeInheritedIntoCurrentScope(): Boolean {
        if (!isBound) return true
        if (cfir.status.visibility != Visibilities.Private) return true
        return ownerClassIdForCurrentScope() == classSymbol.classId
    }

    /**
     * 判断 callable 是否是接口 default 成员。
     */
    private fun CfirCallableSymbol<*>.isInterfaceDefaultMemberForCurrentScope(): Boolean {
        if (!isBound || !cfir.status.isDefault) return false
        return ownerClassLikeDeclarationForCurrentScope() is CfirInterface
    }

    /**
     * 判断 callable 是否是可实现接口 default 的类侧 concrete 成员。
     */
    private fun CfirCallableSymbol<*>.isConcreteClassMemberForCurrentScope(): Boolean {
        if (!isBound || cfir.status.isAbstract) return false
        return ownerClassLikeDeclarationForCurrentScope() !is CfirInterface
    }

    /**
     * 读取 callable 所属 class-like 声明。
     */
    private fun CfirCallableSymbol<*>.ownerClassLikeDeclarationForCurrentScope(): CfirClassLikeDeclaration? {
        val ownerClassId = ownerClassIdForCurrentScope() ?: return null
        return symbolProvider.getClassLikeSymbolByClassId(ownerClassId)?.cfir as? CfirClassLikeDeclaration
    }

    /**
     * 读取 callable 所属 class id；substitution/intersection override 可回退到 provider 记录。
     */
    private fun CfirCallableSymbol<*>.ownerClassIdForCurrentScope(): ClassId? =
        callableId.classId ?: getContainingClass()?.classId

    /**
     * 判断当前属性是否可以实现父属性候选。
     */
    private fun CfirPropertySymbol.canImplementPropertyCandidate(
        candidate: CfirPropertySymbol,
    ): Boolean {
        if (!isBound || !candidate.isBound) return false
        val ownerSubstitutor = overrideSubstitutorForOwnProperty()
        if (!overridesPropertyCandidate(candidate, ownerSubstitutor)) return false

        val ownType = cfir.returnTypeRef.coneTypeOrNull?.let(ownerSubstitutor::substituteOrSelf) ?: return false
        val candidateType = candidate.cfir.returnTypeRef.coneTypeOrNull ?: return false
        return AbstractTypeChecker.equalTypes(session.typeContext, ownType, candidateType)
    }

    private fun CfirPropertySymbol.overrideSubstitutorForOwnProperty(): ConeSubstitutor {
        val original = unwrapSubstitutionOverrides()
        if (original.getContainingExtend() == null) {
            return cjmpOwnerSubstitutorFor(this) ?: ownerSubstitutor
        }
        val receiverType = dispatchReceiverType ?: ownerType ?: return ConeSubstitutor.Empty
        return createCallableOwnerUseSiteSubstitutor(session, original, receiverType)
    }

    /**
     * 为当前 scope 的直接声明/extend 函数创建继承来源。
     */
    private fun CfirNamedFunctionSymbol.localFunctionInheritanceProvenance(): CfirFunctionInheritanceProvenance {
        return CfirFunctionInheritanceProvenance(
            member = this,
            identity = CfirFunctionInheritanceIdentity(
                directInputMember = unwrapSubstitutionOverrides(),
                inheritanceOwnerType = inheritanceProvenanceOwnerType,
            ),
            baseScope = this@CfirClassUseSiteMemberScope,
            lookupProvenance = localCallableLookupProvenance(),
        )
    }

    /** 返回当前 scope 中直接 callable 的结构来源。 */
    private fun CfirCallableSymbol<*>.localCallableLookupProvenance(): CfirCallableLookupProvenance {
        val ownerExtend = getContainingExtend()
        return if (ownerExtend != null) {
            CfirCallableLookupProvenance.directExtendMember(ownerExtend)
        } else {
            inheritedLookupProvenance
        }
    }

    /**
     * 按名称暴露完整 callable provenance。结构 scope 只收集候选；访问控制仍由 use-site
     * checker 处理。函数和属性统一走这里，避免属性路径退化为无来源 symbol。
     */
    override fun processCallablesByNameWithLookupProvenance(
        name: Name,
        processor: (CfirCallableWithLookupProvenance) -> Unit,
    ) {
        if (name !in getCallableNames()) return

        val emitted = linkedSetOf<CfirCallableWithLookupProvenance>()
        val local = mutableListOf<CfirCallableSymbol<*>>()
        declaredScope.processCallablesByName(name) { local += it }
        extendScope?.processCallablesByName(name) { local += it }
        local.forEach { symbol ->
            val candidate = CfirCallableWithLookupProvenance(
                symbol = symbol,
                provenance = symbol.localCallableLookupProvenance(),
            )
            if (emitted.add(candidate)) processor(candidate)
        }

        // provenance 只补充 effective member graph 的结构来源，不能重新引入已经被当前
        // value-like 成员隐藏的父成员。这个截断必须与 processCallablesByName 保持相同
        // 的优先级，否则 tower 会把 Base.value 与 Derived.value 同时作为调用实参候选。
        if (local.any { it.isValueLikeCallable() }) return

        processFunctionsByNameWithProvenance(name) { provenance ->
            val candidate = CfirCallableWithLookupProvenance(
                symbol = provenance.member,
                provenance = provenance.lookupProvenance,
            )
            if (emitted.add(candidate)) processor(candidate)
        }
        properties.getOrPut(name) {
            collectProperties(name)
        }.forEach { property ->
            val candidate = CfirCallableWithLookupProvenance(
                symbol = property.symbol,
                provenance = property.lookupProvenance,
            )
            if (emitted.add(candidate)) processor(candidate)
        }

        // private 类函数不参与继承/override 图，但仍是同包调用发现的结构目标，
        // 由统一 accessibility checker 排除并形成 no-match；跨包则不可发现。
        // 其它函数/属性沿用上面的 effective collection，不能在这里重新引入被覆盖成员。
        for (parent in parentScopes) {
            parent.processCallablesByNameWithLookupProvenance(name) { candidate ->
                val symbol = candidate.symbol
                val privateClassFunction = symbol is CfirNamedFunctionSymbol &&
                        symbol.cfir.status.visibility == Visibilities.Private && symbol.getContainingExtend() == null
                if ((privateClassFunction || (symbol !is CfirNamedFunctionSymbol && symbol !is CfirPropertySymbol)) &&
                    emitted.add(candidate)
                ) {
                    processor(candidate)
                }
            }
        }
    }

    /**
     * 按名称处理所有可见 callable。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        if (name !in getCallableNames()) return

        val local = mutableListOf<CfirCallableSymbol<*>>()
        declaredScope.processCallablesByName(name) { local += it }
        extendScope?.processCallablesByName(name) { local += it }
        if (local.isNotEmpty()) {
            local.forEach(processor)
            if (local.any { it.isValueLikeCallable() }) return
        }
        val localFunctions = local.filterIsInstance<CfirNamedFunctionSymbol>()
        for (candidate in getFunctionsFromParentsByName(name)) {
            if (candidate.isImplementedByLocalFunction(localFunctions) &&
                !candidate.member.isExtendMemberForOverrideResolution()
            ) {
                continue
            }
            processor(candidate.member)
        }
        for (candidate in getPropertiesFromParentsByName(name)) {
            processor(candidate.symbol)
        }
        for (parent in parentScopes) {
            parent.processCallablesByName(name) { parentSymbol ->
                if (parentSymbol is CfirNamedFunctionSymbol || parentSymbol is CfirPropertySymbol) return@processCallablesByName
                processor(parentSymbol)
            }
        }
    }

    /**
     * 只暴露本类（含作用在它上面的 extend）直接声明的绑定。
     *
     * 类型位置查找的遮蔽规则必须排除继承来源：官方 `LookUpImpl` 的类型位置查找只收集当前
     * 作用域链上的声明，父类型成员由 `ProcessStructDeclBody` 在另一条路径处理；实测
     * `class D <: B { var y: Foo }` 在 `B.Foo` 存在时并不产生 `not_a_type`。
     * 因此这里刻意不调用 [processCallablesByName]（它会把父 scope 成员一起透出）。
     */
    override fun processOwnNonTypeBindingsByName(
        name: Name,
        processor: (CfirCallableSymbol<*>) -> Unit,
    ) {
        declaredScope.processOwnNonTypeBindingsByName(name, processor)
        extendScope?.processCallablesByName(name, processor)
    }

    /**
     * 判断 callable 是否按值成员规则隐藏后续父 callable。
     */
    private fun CfirCallableSymbol<*>.isValueLikeCallable(): Boolean =
        this is CfirPropertySymbol || this is CfirVariableSymbol<*> || this is CfirEnumConstructorSymbol

    /**
     * 父类型 extend 函数不能被子类同签名函数吞掉。
     *
     * 官方继承检查会继续把它当作 extend member 冲突处理；调用解析也需要同时看到
     * 本类函数和父类型 extend 函数，才能形成普通歧义而不是静默选择本类函数。
     */
    private fun CfirNamedFunctionSymbol.isExtendMemberForOverrideResolution(): Boolean {
        val original = unwrapSubstitutionOverrides()
        return original.getContainingExtend() != null
    }

    /** 名称集合复用已完成父节点，不为 names 查询构造第二棵父树。 */
    private fun extendCallableNames(): Set<Name> = extendScope?.getCallableNames().orEmpty()

    private fun extendClassifierNames(): Set<Name> = extendScope?.getClassifierNames().orEmpty()

    private fun parentCallableNames(): Set<Name> = parentScopes.flatMapTo(linkedSetOf()) { it.getCallableNames() }

    private fun parentClassifierNames(): Set<Name> = parentScopes.flatMapTo(linkedSetOf()) { it.getClassifierNames() }

    /**
     * 根据声明 self type 创建 owner 类型参数替换器。
     */
    private fun CfirClassLikeSymbol<*>.createDeclarationSubstitutor(type: ConeCangJieType): ConeSubstitutor? =
        createMemberDeclarationSubstitutor(this, type, session)

    /**
     * 返回调试文本。
     */
    override fun toString(): String {
        return "Use site scope of ${classSymbol.classId}"
    }
}

/**
 * 父类型展开路径。
 *
 * Kotlin FIR 在 `lookupSuperTypes` 层用可变 visited set 阻断重复 symbol。当前 CFIR
 * provider 构造期间使用此链检查父图不变量；它不进入完成 raw 或缓存身份。
 * 保留实际声明 symbol；避免递归层级较深时反复复制完整集合。
 */
internal class CfirSupertypePath private constructor(
    /**
     * 当前路径节点代表的原声明 symbol。
     */
    private val symbol: CfirClassLikeSymbol<*>,
    /**
     * 父路径节点；根路径为 `null`。
     */
    private val parent: CfirSupertypePath?,
) {
    /**
     * 判断路径中是否已经包含 [candidate]。
     */
    fun contains(candidate: CfirClassLikeSymbol<*>): Boolean {
        var current: CfirSupertypePath? = this
        while (current != null) {
            if (current.symbol === candidate) return true
            current = current.parent
        }
        return false
    }

    /**
     * 返回追加 [symbol] 后的子路径。
     */
    fun child(symbol: CfirClassLikeSymbol<*>): CfirSupertypePath = CfirSupertypePath(symbol, this)

    /**
     * 父类型路径工厂。
     */
    companion object {
        /**
         * 创建根路径。
         */
        fun root(symbol: CfirClassLikeSymbol<*>): CfirSupertypePath = CfirSupertypePath(symbol, parent = null)
    }
}

/**
 * 携带来源 base scope 的成员候选。
 *
 * @property symbol 候选 callable symbol。
 * @property scope 该候选来自的类型 scope。
 */
private data class MemberWithBaseScope<S : CfirCallableSymbol<*>>(
    /**
     * 候选 callable symbol。
     */
    val symbol: S,
    /**
     * 该候选来自的类型 scope。
     */
    val scope: CfirTypeScope,
    /** 该成员沿 effective graph 到达当前 scope 的来源身份。 */
    val lookupProvenance: CfirCallableLookupProvenance = CfirCallableLookupProvenance.None,
)

/**
 * 在 base scope 中处理指定成员的直接覆盖成员的函数类型。
 */
private typealias ProcessOverriddenWithBaseScope<S> =
        CfirTypeScope.(S, (S, CfirTypeScope) -> ProcessorAction) -> ProcessorAction

/**
 * 取得 scope 的函数继承来源能力；父 scope 构造链违反该不变量时立即暴露架构错误。
 */
private fun CfirTypeScope.requireFunctionInheritanceScope(): CfirFunctionInheritanceScope =
    checkNotNull(this as? CfirFunctionInheritanceScope) {
        "Class use-site parent scope must preserve function inheritance provenance: $this"
    }

/**
 * 对齐 Kotlin FIR `FirOverrideUtils.filterOutOverridden`。
 *
 * 仓颉当前没有 FIR 的 intersection result 模型；这里保留 Kotlin 的过滤位置和
 * “用 direct overridden 链判断候选之间覆盖关系”的语义，避免父 scope 返回已被其它父候选覆盖的成员。
 */
private fun <S : CfirCallableSymbol<*>> filterOutOverridden(
    extractedOverridden: Collection<MemberWithBaseScope<S>>,
    processAllOverridden: ProcessOverriddenWithBaseScope<S>,
): Collection<MemberWithBaseScope<S>> {
    return extractedOverridden.filter { overridden1 ->
        extractedOverridden.none { overridden2 ->
            overridden1 !== overridden2 && overrides(overridden2, overridden1.symbol, processAllOverridden)
        }
    }
}

/**
 * 使用 provenance 携带的 base scope 过滤已被其它父候选覆盖的函数。
 */
private fun Collection<CfirFunctionInheritanceProvenance>.filterOutOverriddenFunctions():
        Collection<CfirFunctionInheritanceProvenance> {
    return filter { overridden1 ->
        none { overridden2 ->
            overridden1 !== overridden2 && overrides(overridden2, overridden1.member)
        }
    }
}

/**
 * 通过 provenance 直接覆盖链判断 [member] 是否覆盖目标 [target]。
 */
private fun overrides(
    member: CfirFunctionInheritanceProvenance,
    target: CfirNamedFunctionSymbol,
): Boolean {
    val visited = linkedSetOf<Pair<CfirTypeScope, CfirNamedFunctionSymbol>>()

    fun visit(current: CfirFunctionInheritanceProvenance): Boolean {
        if (!visited.add(current.baseScope to current.member)) return false

        var found = false
        current.baseScope.requireFunctionInheritanceScope()
            .processDirectOverriddenFunctionsWithProvenance(current.member) { overridden ->
                when {
                    overridden.member == target -> {
                        found = true
                        ProcessorAction.STOP
                    }

                    visit(overridden) -> {
                        found = true
                        ProcessorAction.STOP
                    }

                    else -> ProcessorAction.NEXT
                }
            }
        return found
    }

    return visit(member)
}

/**
 * 对齐官方 `LookUpImpl::ResolveOverrideOrShadow` 与 Kotlin FIR 的交叉父类型 scope：
 * 在同一个 use-site 父候选集合中，非抽象同签名成员实现抽象成员时，抽象候选不参与调用解析。
 */
private fun <S : CfirCallableSymbol<*>> Collection<MemberWithBaseScope<S>>.filterOutAbstractMembersImplementedByConcrete(
    overridesCandidate: S.(S) -> Boolean,
): Collection<MemberWithBaseScope<S>> {
    return filter { candidate ->
        if (!candidate.symbol.isAbstractForIntersectionResolution()) return@filter true

        none { concreteCandidate ->
            concreteCandidate !== candidate &&
                    !concreteCandidate.symbol.isAbstractForIntersectionResolution() &&
                    concreteCandidate.symbol.overridesCandidate(candidate.symbol)
        }
    }
}

/** 在父、子函数各自的声明 owner 类型中比较 override 身份，实例化只影响后续成员替换。 */
private fun CfirFunctionInheritanceProvenance.overridesByDeclaration(
    candidate: CfirFunctionInheritanceProvenance,
    session: CfirSession,
): Boolean {
    val overriding = identity.directInputMember
    val overridden = candidate.identity.directInputMember
    overriding.lazyResolveToPhase(CfirResolvePhase.TYPES)
    overridden.lazyResolveToPhase(CfirResolvePhase.TYPES)
    if (overriding.isStaticMemberForOverride() != overridden.isStaticMemberForOverride()) return false

    val overridingKey = overriding.overrideSignatureKey(declarationGraphSubstitutor(session))
    val overriddenKey = overridden.overrideSignatureKey(candidate.declarationGraphSubstitutor(session))
    return overridingKey == overriddenKey
}

private fun CfirFunctionInheritanceProvenance.declarationGraphSubstitutor(
    session: CfirSession,
): ConeSubstitutor {
    val symbol = identity.directInputMember
    val ownerType = identity.inheritanceOwnerType ?: return ConeSubstitutor.Empty
    val ownerSubstitutor = createCallableOwnerUseSiteSubstitutor(session, symbol, ownerType)
    return (baseScope as? CfirCjmpMemberSubstitutorProvider)
        ?.cjmpOwnerSubstitutorFor(symbol, ownerSubstitutor)
        ?: ownerSubstitutor
}

/**
 * 在函数 provenance 集合中移除已被 concrete 同签名成员实现的抽象输入。
 */
private fun Collection<CfirFunctionInheritanceProvenance>.filterOutAbstractFunctionsImplementedByConcrete(
    session: CfirSession,
):
        Collection<CfirFunctionInheritanceProvenance> {
    return filter { candidate ->
        if (!candidate.member.isAbstractForIntersectionResolution()) return@filter true

        none { concreteCandidate ->
            concreteCandidate !== candidate &&
                    !concreteCandidate.member.isAbstractForIntersectionResolution() &&
                    concreteCandidate.overridesByDeclaration(candidate, session)
        }
    }
}

/**
 * 归并继承图中代表同一最终函数契约的父成员。
 *
 * 官方 `MergeInheritedMembers` 先归并继承图中原本等价的 requirement，再执行实例化替换。
 * 因此 key 同时包含直接输入成员的归一化签名、owner 的类型实参结构、实例化后的签名与
 * lookup 来源：原始签名区分不同泛型函数契约，owner 实参结构区分 `I<X>`/`I<Y>` 这类
 * 实例化前独立的继承输入，实例化签名区分同一契约经不同父类型实参得到的 overload，
 * 来源则保留不同 extend 注入的独立成员图。三者都相同时，才是重复继承路径贡献的同一
 * 个 requirement。
 */
private fun Collection<CfirFunctionInheritanceProvenance>.mergeEquivalentInheritedFunctions():
        Collection<CfirFunctionInheritanceProvenance> {
    val merged = linkedMapOf<InheritedFunctionMergeKey, CfirFunctionInheritanceProvenance>()
    for (candidate in this) {
        val symbol = candidate.member
        val directInput = candidate.identity.directInputMember
        val key = InheritedFunctionMergeKey(
            isStatic = directInput.isStaticMemberForOverride(),
            provenanceSignature = directInput.overrideSignatureKey(),
            inheritanceOwnerTypeArguments = candidate.identity.inheritanceOwnerType
                ?.typeArguments
                .orEmpty(),
            instantiatedSignature = symbol.overrideSignatureKey(),
            lookupProvenance = candidate.lookupProvenance,
            defaultImplementationOwner = candidate.identity.directInputMember
                .takeIf { it.isBound && it.cfir.status.isDefault }
                ?.callableId
                ?.classId,
        )
        val previous = merged[key]
        if (previous == null || previous.member.shouldBeReplacedByConcrete(symbol)) {
            merged[key] = candidate
        }
    }
    return merged.values
}

/** 继承 requirement 归并所需的 substitution 前后联合签名。 */
private data class InheritedFunctionMergeKey(
    /** static 与实例成员不能归并。 */
    val isStatic: Boolean,

    /** 当前 owner substitution 之前的函数参数签名。 */
    val provenanceSignature: String,

    /**
     * 当前继承 owner 的类型实参结构。
     *
     * 这里只保留实参而不保留 owner classifier：`I<X>` 与 `I<Y>` 中的类型参数 tag
     * 仍然不同，而 `F1<Int>` 与 `F2<Int>` 的同形实参可以按官方 requirement 规则归并。
     */
    val inheritanceOwnerTypeArguments: List<ConeTypeProjection>,

    /** 沿父类型边完成实例化后的签名。 */
    val instantiatedSignature: String,

    /** 同一接口契约由不同真实 extend 注入时仍属于独立成员图输入。 */
    val lookupProvenance: CfirCallableLookupProvenance,

    /**
     * 接口 default implementation 的声明 owner。
     *
     * 同一接口成员经菱形继承重复到达当前 scope 时仍应合并；不同接口各自提供同签名
     * default 时必须保留为独立候选，供未实现检查识别官方 `shouldBeImplemented` 冲突。
     */
    val defaultImplementationOwner: ClassId?,
)

/** 同一归并契约存在 concrete 实现时，concrete symbol 作为 scope 代表。 */
private fun CfirNamedFunctionSymbol.shouldBeReplacedByConcrete(candidate: CfirNamedFunctionSymbol): Boolean =
    isBound && candidate.isBound && cfir.status.isAbstract && !candidate.cfir.status.isAbstract

/**
 * 通过 direct-overridden 链判断 [member] 是否覆盖目标 [target]。
 */
private fun <S : CfirCallableSymbol<*>> overrides(
    member: MemberWithBaseScope<S>,
    target: S,
    overriddenProducer: ProcessOverriddenWithBaseScope<S>,
): Boolean {
    val visited = linkedSetOf<Pair<CfirTypeScope, S>>()

    fun visit(current: MemberWithBaseScope<S>): Boolean {
        if (!visited.add(current.scope to current.symbol)) return false

        var found = false
        current.scope.overriddenProducer(current.symbol) { overridden, baseScope ->
            when {
                overridden == target -> {
                    found = true
                    ProcessorAction.STOP
                }

                visit(MemberWithBaseScope(overridden, baseScope)) -> {
                    found = true
                    ProcessorAction.STOP
                }

                else -> ProcessorAction.NEXT
            }
        }
        return found
    }

    return visit(member)
}

/**
 * 判断 callable 是否按交叉父类型解析规则视为抽象成员。
 */
private fun CfirCallableSymbol<*>.isAbstractForIntersectionResolution(): Boolean =
    isBound && cfir.status.isAbstract

/**
 * 判断当前函数是否覆盖候选函数。
 */
private fun CfirNamedFunctionSymbol.overridesFunctionCandidate(
    candidate: CfirNamedFunctionSymbol,
    ownerSubstitutor: ConeSubstitutor = ConeSubstitutor.Empty,
): Boolean {
    if (this == candidate) return true
    // 函数签名中的类型引用到 TYPES 阶段才稳定；先完成两侧签名，再比较 override 身份。
    lazyResolveToPhase(CfirResolvePhase.TYPES)
    candidate.lazyResolveToPhase(CfirResolvePhase.TYPES)
    if (isStaticMemberForOverride() != candidate.isStaticMemberForOverride()) return false
    return overrideSignatureKey(ownerSubstitutor) == candidate.overrideSignatureKey()
}

/**
 * 判断当前属性是否覆盖候选属性。
 */
private fun CfirPropertySymbol.overridesPropertyCandidate(
    candidate: CfirPropertySymbol,
    ownerSubstitutor: ConeSubstitutor = ConeSubstitutor.Empty,
): Boolean {
    if (this == candidate) return true
    if (isStaticMemberForOverride() != candidate.isStaticMemberForOverride()) return false
    return overrideSignatureKey(ownerSubstitutor) == candidate.overrideSignatureKey()
}
