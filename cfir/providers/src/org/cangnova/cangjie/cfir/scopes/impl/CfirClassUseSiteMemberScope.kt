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

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.providers.CfirDirectSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeDescriptor
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirSuperTypeGraphEdge
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.DeclaredSupertypeClassification
import org.cangnova.cangjie.cfir.resolve.providers.classifyDeclaredSupertype
import org.cangnova.cangjie.cfir.resolve.providers.createCallableOwnerUseSiteSubstitutor
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.providers.getContainingClass
import org.cangnova.cangjie.cfir.resolve.providers.getContainingExtend
import org.cangnova.cangjie.cfir.resolve.providers.getDeclarationPackage
import org.cangnova.cangjie.cfir.resolve.providers.scopeTraversalTypeOrNull
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenanceScope
import org.cangnova.cangjie.cfir.scopes.CfirCallableWithLookupProvenance
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
 * 继承函数在 use-site scope 中的稳定来源身份。
 *
 * [directInputMember] 标识进入继承图前的原始函数声明，[inheritanceOwnerType] 标识该声明
 * 沿泛型父类型路径形成的 owner 实例。二者共同区分 `I<X>.foo` 与 `I<Y>.foo`，同时让
 * 菱形继承中重复到达的同一个 `I<T>.foo` 保持同一身份。
 */
data class CfirFunctionInheritanceIdentity(
    /** substitution override 链最初的函数声明。 */
    val directInputMember: CfirNamedFunctionSymbol,

    /** 进入当前实例化之前的泛型 owner/interface 实例。 */
    val inheritanceOwnerType: ConeCangJieType?,
)

/**
 * use-site scope 产生的函数继承结果。
 *
 * @property member 已完成当前 owner 替换、供 resolver 消费的最终函数。
 * @property identity 直接输入成员与泛型父类型实例组成的稳定来源身份。
 * @property baseScope 处理 [member] 覆盖链的 base scope。
 */
data class CfirFunctionInheritanceProvenance(
    val member: CfirNamedFunctionSymbol,
    val identity: CfirFunctionInheritanceIdentity,
    val baseScope: CfirTypeScope,
    /** 该函数进入当前 effective member graph 时的 extend/interface 来源。 */
    val lookupProvenance: CfirCallableLookupProvenance,
)

/**
 * 能够保留函数继承来源的 type scope。
 *
 * 普通成员查询只暴露最终函数集合；需要判断“实例化前独立、实例化后冲突”的消费者通过
 * 本接口读取 intersection 输入，不得自行重走父类型图。
 */
interface CfirFunctionInheritanceScope {
    /** 按名称处理最终可见函数及其继承来源。 */
    fun processFunctionsByNameWithProvenance(
        name: Name,
        processor: (CfirFunctionInheritanceProvenance) -> Unit,
    )

    /** 处理函数的直接覆盖输入及其继承来源。 */
    fun processDirectOverriddenFunctionsWithProvenance(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirFunctionInheritanceProvenance) -> ProcessorAction,
    ): ProcessorAction
}

/**
 * class-like 类型 use-site 成员 scope。
 *
 * 该 scope 统一处理声明成员、extend 成员、父类型成员、泛型 owner 替换和直接覆盖查询。
 */
class CfirClassUseSiteMemberScope private constructor(
    /**
     * 当前成员查找发生的 use-site session。
     */
    private val session: CfirSession,
    /**
     * 被查询成员的 class-like symbol。
     */
    private val classSymbol: CfirClassLikeSymbol<*>,
    /**
     * 用于解析父类型与关联 class-like symbol 的 symbol provider。
     */
    private val symbolProvider: CfirSymbolProvider,
    /**
     * 提供当前类型可见 extend 成员的 provider。
     */
    private val extendProvider: CfirExtendProvider? = null,
    /**
     * 提供声明侧直接父类型的 provider。
     */
    private val directSupertypeProvider: CfirDirectSupertypeProvider? = null,
    /**
     * 当前 scope 所代表 owner 的具体类型，泛型 owner 替换从这里提取实参。
     */
    private val ownerType: ConeCangJieType? = declarationSelfType(classSymbol),
    /**
     * 当前继承路径在最终实例化之前的 owner 类型。
     *
     * 默认与 [ownerType] 相同；泛型实例化检查会传入声明 self type，使 scope 在计算最终
     * 签名的同时保留 `I<X>`/`I<Y>` 这类直接输入身份。
     */
    private val inheritanceProvenanceOwnerType: ConeCangJieType? = ownerType,
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
    /**
     * 当前父类型展开路径，用于阻断继承图中的递归 class id。
     */
    private val supertypePath: CfirSupertypePath = CfirSupertypePath.root(classSymbol.classId),
) : CfirTypeScope(), CfirFunctionInheritanceScope, CfirCallableLookupProvenanceScope,
    CfirMemberLookupCompletenessScope {
    /**
     * 创建 class-like use-site 成员 scope。
     */
    constructor(
        session: CfirSession,
        classSymbol: CfirClassLikeSymbol<*>,
        symbolProvider: CfirSymbolProvider,
        extendProvider: CfirExtendProvider? = null,
        directSupertypeProvider: CfirDirectSupertypeProvider? = null,
        ownerType: ConeCangJieType? = declarationSelfType(classSymbol),
        inheritanceProvenanceOwnerType: ConeCangJieType? = ownerType,
        dispatchReceiverType: ConeCangJieType? = ownerType,
        scopeKind: CfirClassMemberScopeKind = CfirClassMemberScopeKind.USE_SITE,
        allowBareGenericStaticQualifierExtends: Boolean = false,
        excludingExtend: CfirExtend? = null,
    ) : this(
        session = session,
        classSymbol = classSymbol,
        symbolProvider = symbolProvider,
        extendProvider = extendProvider,
        directSupertypeProvider = directSupertypeProvider,
        ownerType = ownerType,
        inheritanceProvenanceOwnerType = inheritanceProvenanceOwnerType,
        dispatchReceiverType = dispatchReceiverType,
        scopeKind = scopeKind,
        allowBareGenericStaticQualifierExtends = allowBareGenericStaticQualifierExtends,
        excludingExtend = excludingExtend,
        inheritedLookupProvenance = CfirCallableLookupProvenance.None,
        supertypePath = CfirSupertypePath.root(classSymbol.classId),
    )

    /**
     * 兼容旧调用点的构造器，session 从 [symbolProvider] 取得。
     */
    constructor(
        classSymbol: CfirClassLikeSymbol<*>,
        symbolProvider: CfirSymbolProvider,
        extendProvider: CfirExtendProvider? = null,
        directSupertypeProvider: CfirDirectSupertypeProvider? = null,
    ) : this(
        session = symbolProvider.session,
        classSymbol = classSymbol,
        symbolProvider = symbolProvider,
        extendProvider = extendProvider,
        directSupertypeProvider = directSupertypeProvider,
        ownerType = declarationSelfType(classSymbol),
        inheritanceProvenanceOwnerType = declarationSelfType(classSymbol),
        dispatchReceiverType = declarationSelfType(classSymbol),
        scopeKind = CfirClassMemberScopeKind.USE_SITE,
        inheritedLookupProvenance = CfirCallableLookupProvenance.None,
        supertypePath = CfirSupertypePath.root(classSymbol.classId),
    )

    companion object {
        /**
         * 为任意具体接收者类型创建来源保留的结构性 use-site member scope。
         *
         * class-like/primitive 目标统一复用本类的父边 descriptor；tuple、函数等没有
         * nominal class id 的目标则组合直接 extend scope 与实例化父边。整个工厂只构造
         * 结构成员图，不读取文件可见性；调用方必须把完整 provenance 交给统一访问 checker。
         */
        fun createForUseSiteType(
            session: CfirSession,
            ownerType: ConeCangJieType,
            excludingExtend: CfirExtend? = null,
        ): CfirTypeScope? = createForUseSiteType(
            session = session,
            ownerType = ownerType,
            provenanceOwnerType = ownerType,
            dispatchReceiverType = ownerType,
            excludingExtend = excludingExtend,
            inheritedLookupProvenance = CfirCallableLookupProvenance.None,
            typePath = emptySet(),
        )

        /**
         * 沿非 nominal 类型父边递归创建 scope；每个分支独立维护路径，保留菱形图中的
         * 多个真实来源，同时阻断非法循环。
         */
        private fun createForUseSiteType(
            session: CfirSession,
            ownerType: ConeCangJieType,
            provenanceOwnerType: ConeCangJieType,
            dispatchReceiverType: ConeCangJieType,
            excludingExtend: CfirExtend?,
            inheritedLookupProvenance: CfirCallableLookupProvenance,
            typePath: Set<ConeCangJieType>,
        ): CfirTypeScope? {
            if (ownerType in typePath) return null
            val nextTypePath = typePath + ownerType

            val classId = ownerType.expandedClassIdOrPrimitiveClassId
            if (classId != null) {
                val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
                val rawScope = CfirClassUseSiteMemberScope(
                    session = session,
                    classSymbol = classSymbol,
                    symbolProvider = session.symbolProvider,
                    extendProvider = session.extendProvider,
                    directSupertypeProvider = session.directSupertypeProviderOrNull,
                    ownerType = ownerType,
                    inheritanceProvenanceOwnerType = provenanceOwnerType,
                    dispatchReceiverType = dispatchReceiverType,
                    scopeKind = CfirClassMemberScopeKind.USE_SITE,
                    excludingExtend = excludingExtend,
                    inheritedLookupProvenance = inheritedLookupProvenance,
                    supertypePath = CfirSupertypePath.root(classId),
                )
                return CfirClassSubstitutionScope(
                    session = session,
                    useSiteMemberScope = rawScope,
                    dispatchReceiverType = dispatchReceiverType,
                    substitutionOwnerType = ownerType,
                )
            }

            val targetKey = ownerType.expandedExtendTargetKey ?: return null
            val scopes = buildList {
                val directExtendScope = CfirExtendMemberScope(
                    targetKey = targetKey,
                    extendProvider = session.extendProvider,
                    session = session,
                    receiverType = ownerType,
                    excludingExtend = excludingExtend,
                )
                add(
                    CfirClassSubstitutionScope(
                        session = session,
                        useSiteMemberScope = directExtendScope,
                        dispatchReceiverType = dispatchReceiverType,
                        substitutionOwnerType = ownerType,
                    )
                )

                for (descriptor in session.typeAwareSupertypeProviderOrNull
                    ?.getDirectSupertypeDescriptors(ownerType)
                    .orEmpty()
                ) {
                    val descriptorExtend = (descriptor.origin as? CfirInstantiatedSupertypeOrigin.Extend)
                        ?.sourceExtend
                    if (descriptorExtend === excludingExtend) continue
                    val parentLookupProvenance = if (descriptorExtend != null) {
                        CfirCallableLookupProvenance.inheritedThroughExtend(descriptor)
                    } else {
                        inheritedLookupProvenance
                    }
                    createForUseSiteType(
                        session = session,
                        ownerType = descriptor.type,
                        provenanceOwnerType = descriptor.provenanceType,
                        dispatchReceiverType = dispatchReceiverType,
                        excludingExtend = excludingExtend,
                        inheritedLookupProvenance = parentLookupProvenance,
                        typePath = nextTypePath,
                    )?.let(::add)
                }
            }
            return when (scopes.size) {
                0 -> null
                1 -> scopes.single()
                else -> CfirCompositeTypeScope(scopes, session)
            }
        }
    }

    /**
     * 声明成员 scope。
     */
    private val declaredScope = CfirClassDeclaredMemberScope(classSymbol)

    /**
     * 当前 receiver 可见的 extend 成员 scope。
     */
    private val extendScope = takeIf { scopeKind == CfirClassMemberScopeKind.USE_SITE }
        ?.let {
            val provider = extendProvider ?: return@let null
            val receiverType = ownerType ?: return@let null
            CfirExtendMemberScope(
                targetKey = receiverType.expandedExtendTargetKey
                    ?: CfirExtendTargetKey.ClassLike(classSymbol.classId),
                extendProvider = provider,
                session = session,
                receiverType = receiverType,
                allowBareGenericStaticQualifierExtends = allowBareGenericStaticQualifierExtends,
                excludingExtend = excludingExtend,
            )
        }

    /**
     * 直接父类型 scope 缓存。
     */
    private val parentScopes: List<CfirTypeScope> by lazy { buildParentScopes() }

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
     * 父 scope 属性候选缓存。
     */
    private val propertiesFromParents = hashMapOf<Name, List<MemberWithBaseScope<CfirPropertySymbol>>>()

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
    ): CfirTypeScope? = CfirClassUseSiteMemberScope(
        session = newSession,
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
        supertypePath = supertypePath,
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
            if (local.any { it.overridesFunctionCandidate(candidate.member, it.overrideSubstitutorForOwnFunction()) } &&
                !candidate.member.isExtendMemberForOverrideResolution()
            ) {
                continue
            }
            add(candidate)
        }
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
        return getFunctionsFromParentsByName(functionSymbol.name)
            .filter { functionSymbol.overridesFunctionCandidate(it.member, functionSymbol.overrideSubstitutorForOwnFunction()) }
    }

    /**
     * 计算声明属性覆盖的父属性集合。
     */
    private fun computeDirectOverriddenForDeclaredProperty(
        propertySymbol: CfirPropertySymbol,
    ): List<MemberWithBaseScope<CfirPropertySymbol>> {
        return getPropertiesFromParentsByName(propertySymbol.name)
            .filter { propertySymbol.overridesPropertyCandidate(it.symbol) }
    }

    /**
     * 返回指定名称的父函数候选。
     */
    private fun getFunctionsFromParentsByName(name: Name): List<CfirFunctionInheritanceProvenance> {
        return functionsFromParents.getOrPut(name) {
            val parentCandidates = mutableListOf<CfirFunctionInheritanceProvenance>()
            for (parent in parentScopes) {
                parent.requireFunctionInheritanceScope().processFunctionsByNameWithProvenance(name) {
                    parentCandidates += it
                }
            }
            parentCandidates
                .filterInheritedFunctionsForCurrentScope()
                .filterOutOverriddenFunctions()
                .filterOutAbstractFunctionsImplementedByConcrete()
                .filterOutInterfaceDefaultFunctionsImplementedByConcrete()
                .mergeEquivalentInheritedFunctions()
                .toList()
        }
    }

    /**
     * 返回指定名称的父属性候选。
     */
    private fun getPropertiesFromParentsByName(name: Name): List<MemberWithBaseScope<CfirPropertySymbol>> {
        return propertiesFromParents.getOrPut(name) {
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
            val inheritableParentCandidates = parentCandidates.filterInheritedForCurrentScope()
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
                        concreteCandidate.member.overridesFunctionCandidate(candidate.member)
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
        if (!overridesPropertyCandidate(candidate)) return false

        val ownType = cfir.returnTypeRef.coneTypeOrNull ?: return false
        val candidateType = candidate.cfir.returnTypeRef.coneTypeOrNull ?: return false
        return AbstractTypeChecker.equalTypes(session.typeContext, ownType, candidateType)
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

        // 函数和属性已经由上面的 effective collection 合并；只透传没有专用 collection
        // 的 callable，避免 provenance 路径绕开 inherited override/shadow 过滤。
        for (parent in parentScopes) {
            parent.processCallablesByNameWithLookupProvenance(name) { candidate ->
                if (candidate.symbol !is CfirNamedFunctionSymbol &&
                    candidate.symbol !is CfirPropertySymbol &&
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
            if (localFunctions.any { it.overridesFunctionCandidate(candidate.member, it.overrideSubstitutorForOwnFunction()) } &&
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

    /**
     * extend 成员的 owner 类型参数来自 extend 声明本身，而不是目标 class 声明。
     * 因此判断它是否实现接口成员时，必须用成员访问 receiver 推导出的 extend-owner
     * substitution；否则 `extend<T> A<T> <: I<T>` 会把 `test(T)` 和 `test(Int32)`
     * 视为不同签名。
     */
    private fun CfirNamedFunctionSymbol.overrideSubstitutorForOwnFunction(): ConeSubstitutor {
        val original = unwrapSubstitutionOverrides()
        if (original.getContainingExtend() == null) return ownerSubstitutor
        val receiverType = dispatchReceiverType ?: ownerType ?: return ConeSubstitutor.Empty
        return createCallableOwnerUseSiteSubstitutor(session, original, receiverType)
    }

    /**
     * 构造所有直接父类型对应的成员 scope。
     */
    private fun buildParentScopes(): List<CfirTypeScope> {
        val rootType = ownerType ?: return emptyList()
        val provenanceRootType = inheritanceProvenanceOwnerType ?: rootType
        val parentDescriptors = directParentDescriptorsOf(
            concreteType = rootType,
            provenanceType = provenanceRootType,
        )

        return parentDescriptors.mapNotNull { descriptor ->
            val supertype = descriptor.type
            val classId = supertype.classIdOrPrimitiveClassId ?: return@mapNotNull null
            if (supertypePath.contains(classId)) return@mapNotNull null
            val parentSymbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: return@mapNotNull null
            val parentLookupProvenance = when (descriptor.origin) {
                is CfirInstantiatedSupertypeOrigin.Extend ->
                    CfirCallableLookupProvenance.inheritedThroughExtend(descriptor)

                is CfirInstantiatedSupertypeOrigin.Declared,
                is CfirInstantiatedSupertypeOrigin.ImplicitObject,
                -> inheritedLookupProvenance
            }
            val parentScope = CfirClassUseSiteMemberScope(
                session = session,
                classSymbol = parentSymbol,
                symbolProvider = symbolProvider,
                extendProvider = extendProvider,
                directSupertypeProvider = directSupertypeProvider,
                ownerType = supertype,
                inheritanceProvenanceOwnerType = descriptor.provenanceType,
                dispatchReceiverType = dispatchReceiverType ?: rootType,
                scopeKind = parentScopeKind(),
                allowBareGenericStaticQualifierExtends = allowBareGenericStaticQualifierExtends,
                excludingExtend = excludingExtend,
                inheritedLookupProvenance = parentLookupProvenance,
                supertypePath = supertypePath.child(classId),
            )
            parentScope.substitutionScopeForSupertype(parentSymbol, supertype)
        }
    }

    /**
     * 返回当前具体 owner 的直接父边，同时保留泛型实例化前的父类型身份。
     *
     * 先从 provenance owner 建立每条来源边，再统一代入 concrete owner；仅在具体实例化后
     * 新满足约束的 extend 边才从 concrete 视图追加。来源比较使用完整 origin，禁止按
     * ClassId 或父类型压平。
     */
    private fun directParentDescriptorsOf(
        concreteType: ConeCangJieType,
        provenanceType: ConeCangJieType,
    ): List<CfirInstantiatedSupertypeDescriptor> {
        if (concreteType == provenanceType) {
            return directParentDescriptorsFor(concreteType)
        }

        val provenanceToConcreteSubstitutor = provenanceType.provenanceToConcreteSubstitutor(concreteType)
        val provenanceDescriptors = directParentDescriptorsFor(provenanceType).map { descriptor ->
            descriptor.copy(
                type = provenanceToConcreteSubstitutor
                    .substituteOrSelf(descriptor.type)
                    .fullyExpandedType(session),
                provenanceType = descriptor.type.fullyExpandedType(session),
            )
        }
        val representedOrigins = provenanceDescriptors.mapTo(linkedSetOf()) { it.origin }

        return buildList {
            addAll(provenanceDescriptors)
            for (descriptor in directParentDescriptorsFor(concreteType)) {
                if (descriptor.origin in representedOrigins) continue
                // 仅具体类型适用的 extend 父边不属于泛型声明形态，但仍是当前成员图的独立输入。
                add(descriptor)
            }
        }
    }

    /**
     * 构造“泛型继承来源 owner -> 当前具体 owner”的替换器。
     *
     * 父 scope 的 provenance type 可能携带外层声明的类型参数，例如
     * `B<X, Y> -> A<X, Y> -> I<X>`。因此不能再次按当前 class 的声明参数建立映射，
     * 而要按 provenance/concrete owner 的同形类型实参递归收集真实来源参数。
     */
    private fun ConeCangJieType.provenanceToConcreteSubstitutor(
        concreteType: ConeCangJieType,
    ): ConeSubstitutor {
        val replacements = linkedMapOf<TypeConstructorMarker, ConeCangJieType>()

        fun collect(provenance: ConeCangJieType, concrete: ConeCangJieType) {
            if (provenance is ConeTypeParameterType) {
                val constructor = provenance.lookupTag as TypeConstructorMarker
                val previous = replacements.putIfAbsent(constructor, concrete)
                check(previous == null || previous == concrete) {
                    "Inconsistent inheritance provenance substitution for $provenance: $previous and $concrete"
                }
                return
            }

            check(provenance.typeArguments.size == concrete.typeArguments.size) {
                "Inheritance provenance owner shape differs from concrete owner: $provenance and $concrete"
            }
            provenance.typeArguments.zip(concrete.typeArguments).forEach { (provenanceArgument, concreteArgument) ->
                collect(provenanceArgument.type, concreteArgument.type)
            }
        }

        collect(this, concreteType)
        return replacements.takeIf { it.isNotEmpty() }
            ?.let(::CfirTypeSubstitutorByMap)
            ?: ConeSubstitutor.Empty
    }

    /**
     * 返回当前 extend scope 的 callable 名称。
     */
    private fun extendCallableNames(): Set<Name> = extendScope?.getCallableNames().orEmpty()

    /**
     * 返回当前 extend scope 的 classifier 名称。
     */
    private fun extendClassifierNames(): Set<Name> = extendScope?.getClassifierNames().orEmpty()

    /**
     * 返回父类型链上的 callable 名称集合。
     */
    private fun parentCallableNames(): Set<Name> = buildSet {
        collectParentNames(
            ownerType = ownerType ?: return@buildSet,
            visitedClassIds = linkedSetOf(classSymbol.classId),
            collectDeclaredNames = CfirClassDeclaredMemberScope::getCallableNames,
            collectExtendNames = CfirClassUseSiteMemberScope::extendCallableNames,
            addNames = ::addAll,
        )
    }

    /**
     * 返回父类型链上的 classifier 名称集合。
     */
    private fun parentClassifierNames(): Set<Name> = buildSet {
        collectParentNames(
            ownerType = ownerType ?: return@buildSet,
            visitedClassIds = linkedSetOf(classSymbol.classId),
            collectDeclaredNames = CfirClassDeclaredMemberScope::getClassifierNames,
            collectExtendNames = CfirClassUseSiteMemberScope::extendClassifierNames,
            addNames = ::addAll,
        )
    }

    /**
     * 对齐 Kotlin `lookupSuperTypes(...).traverseDepthFirstWithoutDuplicates` 的 containing-names 收集语义。
     *
     * `parentScopes` 仍然保留当前 use-site 查询路径，用于按名称处理成员；但 containing names
     * 只需要集合结果。如果沿每条继承路径递归调用父 scope 的 `getCallableNames()`，标准库中
     * diamond 继承会重复构造大量等价父 scope，LLT 大套件会在成员名缓存阶段耗尽堆。
     */
    private fun collectParentNames(
        ownerType: ConeCangJieType,
        visitedClassIds: MutableSet<ClassId>,
        collectDeclaredNames: CfirClassDeclaredMemberScope.() -> Set<Name>,
        collectExtendNames: CfirClassUseSiteMemberScope.() -> Set<Name>,
        addNames: (Set<Name>) -> Unit,
    ) {
        for (supertype in directParentTypesOf(ownerType)) {
            val classId = supertype.classIdOrPrimitiveClassId ?: continue
            if (!visitedClassIds.add(classId)) continue
            val parentSymbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: continue
            val parentScope = CfirClassUseSiteMemberScope(
                session = session,
                classSymbol = parentSymbol,
                symbolProvider = symbolProvider,
                extendProvider = extendProvider,
                directSupertypeProvider = directSupertypeProvider,
                ownerType = supertype,
                scopeKind = parentScopeKind(),
                allowBareGenericStaticQualifierExtends = allowBareGenericStaticQualifierExtends,
                excludingExtend = excludingExtend,
                supertypePath = supertypePath.child(classId),
            )
            addNames(parentScope.declaredScope.collectDeclaredNames())
            addNames(parentScope.collectExtendNames())
            parentScope.collectParentNames(
                ownerType = supertype,
                visitedClassIds = visitedClassIds,
                collectDeclaredNames = collectDeclaredNames,
                collectExtendNames = collectExtendNames,
                addNames = addNames,
            )
        }
    }

    /**
     * 返回 [type] 的直接父类型。
     *
     * 成员 scope 遍历必须消费完全展开后的名义类型。父边可能由 typealias 写出，
     * 若直接按别名 ClassId 构造 scope，会在别名声明处中断，无法进入实际父接口。
     */
    private fun directParentTypesOf(type: ConeCangJieType): List<ConeCangJieType> {
        return directParentDescriptorsFor(type)
            .map(CfirInstantiatedSupertypeDescriptor::type)
            .distinct()
    }

    /**
     * 返回 [type] 的来源保留直接父边。
     *
     * declaration/body 模式只读取声明边；use-site 模式优先消费类型感知 provider 的完整
     * descriptor。graph fallback 也在这里实例化，所有入口最终形成同一种结构模型。
     */
    private fun directParentDescriptorsFor(type: ConeCangJieType): List<CfirInstantiatedSupertypeDescriptor> {
        val descriptors = if (
            scopeKind == CfirClassMemberScopeKind.DECLARATION_SITE ||
            scopeKind == CfirClassMemberScopeKind.BODY_LOOKUP
        ) {
            val substitutor = classSymbol.createDeclarationSubstitutor(type)
            classSymbol.cfir.superTypeRefs.mapNotNull { superTypeRef ->
                val supertype = superTypeRef
                    .classifyDeclaredSupertype(session)
                    .scopeTraversalTypeOrNull()
                    ?: return@mapNotNull null
                CfirInstantiatedSupertypeDescriptor(
                    type = substitutor?.substituteOrSelf(supertype) ?: supertype,
                    origin = CfirInstantiatedSupertypeOrigin.Declared(superTypeRef),
                )
            }
        } else {
            val classId = type.classIdOrPrimitiveClassId ?: classSymbol.classId
            val typeAwareSupertypeProvider = session.typeAwareSupertypeProviderOrNull
            when {
                excludingExtend != null && directSupertypeProvider != null -> {
                    directParentDescriptorsFromEdges(type, classId, excludingExtend)
                }
                typeAwareSupertypeProvider != null -> {
                    typeAwareSupertypeProvider.getDirectSupertypeDescriptors(type)
                }
                directSupertypeProvider != null -> {
                    directParentDescriptorsFromEdges(type, classId, excludedExtend = null)
                }
                else -> {
                    val substitutor = classSymbol.createDeclarationSubstitutor(type)
                    classSymbol.cfir.superTypeRefs.mapNotNull { superTypeRef ->
                        val supertype = superTypeRef
                            .classifyDeclaredSupertype(session)
                            .scopeTraversalTypeOrNull()
                            ?: return@mapNotNull null
                        CfirInstantiatedSupertypeDescriptor(
                            type = substitutor?.substituteOrSelf(supertype) ?: supertype,
                            origin = CfirInstantiatedSupertypeOrigin.Declared(superTypeRef),
                        )
                    }
                }
            }
        }

        return descriptors.map { descriptor ->
            descriptor.copy(
                type = descriptor.type.fullyExpandedType(session),
                provenanceType = descriptor.provenanceType.fullyExpandedType(session),
            )
        }
    }

    /**
     * 在 extend 声明体内查询父类型时按 graph edge 来源跳过当前 extend 自己贡献的接口。
     */
    private fun directParentDescriptorsFromEdges(
        type: ConeCangJieType,
        classId: ClassId,
        excludedExtend: CfirExtend?,
    ): List<CfirInstantiatedSupertypeDescriptor> {
        val ownerSymbol = symbolProvider.getClassLikeSymbolByClassId(classId)
        val ownerDeclaration = ownerSymbol?.cfir as? CfirClassLikeDeclaration
        val declarationSubstitutor = ownerSymbol?.createDeclarationSubstitutor(type)
        val directSupertypes = directSupertypeProvider
            ?.getDirectSuperTypeEdges(classId)
            .orEmpty()
            .mapNotNull { edge ->
                if (excludedExtend != null && edge.sourceExtend === excludedExtend) return@mapNotNull null
                edge.toUseSiteDescriptor(type, declarationSubstitutor)
            }
        return directSupertypes.withImplicitObjectSuperclass(ownerDeclaration)
    }

    /**
     * 将 supertype graph edge 还原为当前 owner type 上的来源保留父边。
     */
    private fun CfirSuperTypeGraphEdge.toUseSiteDescriptor(
        ownerType: ConeCangJieType,
        declarationSubstitutor: ConeSubstitutor?,
    ): CfirInstantiatedSupertypeDescriptor? {
        val supertype = typeRef.coneType
        val extend = sourceExtend
        if (extend != null) {
            val targetPattern = extend.extendedTypeRef.coneTypeOrNull ?: return null
            val substitution = createExtendDeclarationSubstitution(
                session = session,
                extend = extend,
                targetPattern = targetPattern,
                concreteReceiverType = ownerType,
            ) ?: return null
            return CfirInstantiatedSupertypeDescriptor(
                type = substitution.substitutor.substituteOrSelf(supertype),
                origin = CfirInstantiatedSupertypeOrigin.Extend(
                    sourceExtend = extend,
                    declarationPackage = requireNotNull(extend.getDeclarationPackage()) {
                        "Extend declaration package is not indexed: ${extend.symbol}"
                    },
                    sourceTypeRef = typeRef,
                ),
            )
        }
        return CfirInstantiatedSupertypeDescriptor(
            type = declarationSubstitutor?.substituteOrSelf(supertype) ?: supertype,
            origin = CfirInstantiatedSupertypeOrigin.Declared(typeRef),
        )
    }

    /**
     * graph edge 不包含官方隐式 `Object` 父类；edge 路径需要显式补回。
     */
    private fun List<CfirInstantiatedSupertypeDescriptor>.withImplicitObjectSuperclass(
        declaration: CfirClassLikeDeclaration?,
    ): List<CfirInstantiatedSupertypeDescriptor> {
        if (declaration !is CfirClass) return this
        if (declaration.symbol.classId == StdlibClassIds.Any) return this
        if (any { it.type.isConcreteSuperclassCandidate() }) return this

        val implicitSuperclass = if (declaration.symbol.classId == StdlibClassIds.Object) {
            ConeClassLikeType(StdlibClassIds.Any.toLookupTag(), isInterface = true)
        } else {
            ConeClassLikeType(StdlibClassIds.Object.toLookupTag())
        }
        return this + CfirInstantiatedSupertypeDescriptor(
            type = implicitSuperclass,
            origin = CfirInstantiatedSupertypeOrigin.ImplicitObject(declaration.symbol.classId),
        )
    }

    /**
     * 判断类型是否占用 class 的 concrete superclass 槽位。
     */
    private fun ConeCangJieType.isConcreteSuperclassCandidate(): Boolean = when (this) {
        is ConeClassLikeType -> !isInterface
        is ConeStructType, is ConeEnumType -> true
        else -> false
    }

    /**
     * 根据当前 scope 模式选择父 scope 模式。
     */
    private fun parentScopeKind(): CfirClassMemberScopeKind = when (scopeKind) {
        CfirClassMemberScopeKind.BODY_LOOKUP -> CfirClassMemberScopeKind.USE_SITE
        else -> scopeKind
    }

    /**
     * 对父类型 scope 应用 supertype 所需的类型替换。
     */
    private fun CfirTypeScope.substitutionScopeForSupertype(
        parentSymbol: CfirClassLikeSymbol<*>,
        supertype: ConeCangJieType,
    ): CfirTypeScope {
        val receiverType = dispatchReceiverType ?: return this
        if (!parentSymbol.isBound || parentSymbol.cfir.typeParameters.isEmpty()) return this
        val typeArguments = (supertype as? ConeLookupTagBasedType)?.typeArguments.orEmpty()
        if (typeArguments.isEmpty()) return this
        return CfirClassSubstitutionScope(session, this, receiverType, substitutionOwnerType = supertype)
    }

    /**
     * 根据声明 self type 创建 owner 类型参数替换器。
     */
    private fun CfirClassLikeSymbol<*>.createDeclarationSubstitutor(type: ConeCangJieType): CfirTypeSubstitutorByMap? {
        if (type !is ConeLookupTagBasedType) return null
        if (!isBound || cfir.typeParameters.isEmpty()) return null
        if (cfir.typeParameters.size != type.typeArguments.size) return null

        val replacements: Map<TypeConstructorMarker, ConeCangJieType> =
            cfir.typeParameters.zip(type.typeArguments).associate { (typeParameter, argument) ->
                typeParameter.symbol.toLookupTag() to argument.type
            }
        return replacements.takeIf { it.isNotEmpty() }?.let(::CfirTypeSubstitutorByMap)
    }

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
 * use-site scope 仍采用 lazy 父 scope 构造，因此这里用链式路径表达同一语义，
 * 避免递归层级较深时反复复制完整 `Set<ClassId>` 导致 O(n^2) 内存增长。
 */
private class CfirSupertypePath private constructor(
    /**
     * 当前路径节点代表的 class id。
     */
    private val classId: ClassId,
    /**
     * 父路径节点；根路径为 `null`。
     */
    private val parent: CfirSupertypePath?,
) {
    /**
     * 判断路径中是否已经包含 [candidate]。
     */
    fun contains(candidate: ClassId): Boolean {
        var current: CfirSupertypePath? = this
        while (current != null) {
            if (current.classId == candidate) return true
            current = current.parent
        }
        return false
    }

    /**
     * 返回追加 [classId] 后的子路径。
     */
    fun child(classId: ClassId): CfirSupertypePath = CfirSupertypePath(classId, this)

    /**
     * 父类型路径工厂。
     */
    companion object {
        /**
         * 创建根路径。
         */
        fun root(classId: ClassId): CfirSupertypePath = CfirSupertypePath(classId, parent = null)
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

/**
 * 在函数 provenance 集合中移除已被 concrete 同签名成员实现的抽象输入。
 */
private fun Collection<CfirFunctionInheritanceProvenance>.filterOutAbstractFunctionsImplementedByConcrete():
        Collection<CfirFunctionInheritanceProvenance> {
    return filter { candidate ->
        if (!candidate.member.isAbstractForIntersectionResolution()) return@filter true

        none { concreteCandidate ->
            concreteCandidate !== candidate &&
                    !concreteCandidate.member.isAbstractForIntersectionResolution() &&
                    concreteCandidate.member.overridesFunctionCandidate(candidate.member)
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
    if (isStaticMemberForOverride() != candidate.isStaticMemberForOverride()) return false
    return overrideSignatureKey(ownerSubstitutor) == candidate.overrideSignatureKey()
}

/**
 * 判断当前属性是否覆盖候选属性。
 */
private fun CfirPropertySymbol.overridesPropertyCandidate(
    candidate: CfirPropertySymbol,
): Boolean {
    if (this == candidate) return true
    if (isStaticMemberForOverride() != candidate.isStaticMemberForOverride()) return false
    return overrideSignatureKey() == candidate.overrideSignatureKey()
}
