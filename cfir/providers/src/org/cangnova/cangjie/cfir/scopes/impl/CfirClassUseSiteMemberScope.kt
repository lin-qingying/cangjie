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
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityFileScope
import org.cangnova.cangjie.cfir.resolve.providers.CfirDirectSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSuperTypeGraphEdge
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.createCallableOwnerUseSiteSubstitutor
import org.cangnova.cangjie.cfir.resolve.providers.createExtendDeclarationSubstitution
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.isStaticMemberForOverride
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
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
    /**
     * 当前 use-site 文件包名，用于 private/protected extend 成员可见性。
     */
    private val useSitePackage: FqName? = CfirAccessibilityFileScope.currentPackageFqName(),
    /**
     * 当前父类型展开路径，用于阻断继承图中的递归 class id。
     */
    private val supertypePath: CfirSupertypePath = CfirSupertypePath.root(classSymbol.classId),
) : CfirTypeScope() {
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
        dispatchReceiverType: ConeCangJieType? = ownerType,
        scopeKind: CfirClassMemberScopeKind = CfirClassMemberScopeKind.USE_SITE,
        allowBareGenericStaticQualifierExtends: Boolean = false,
        excludingExtend: CfirExtend? = null,
        useSitePackage: FqName? = CfirAccessibilityFileScope.currentPackageFqName(),
    ) : this(
        session = session,
        classSymbol = classSymbol,
        symbolProvider = symbolProvider,
        extendProvider = extendProvider,
        directSupertypeProvider = directSupertypeProvider,
        ownerType = ownerType,
        dispatchReceiverType = dispatchReceiverType,
        scopeKind = scopeKind,
        allowBareGenericStaticQualifierExtends = allowBareGenericStaticQualifierExtends,
        excludingExtend = excludingExtend,
        useSitePackage = useSitePackage,
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
        dispatchReceiverType = declarationSelfType(classSymbol),
        scopeKind = CfirClassMemberScopeKind.USE_SITE,
        useSitePackage = CfirAccessibilityFileScope.currentPackageFqName(),
        supertypePath = CfirSupertypePath.root(classSymbol.classId),
    )

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
                useSitePackage = useSitePackage,
            )
        }

    /**
     * 直接父类型 scope 缓存。
     */
    private val parentScopes: List<CfirTypeScope> by lazy { buildParentScopes() }

    /**
     * 按名称缓存的函数结果。
     */
    private val functions = hashMapOf<Name, Collection<CfirNamedFunctionSymbol>>()

    /**
     * 按名称缓存的属性结果。
     */
    private val properties = hashMapOf<Name, Collection<CfirPropertySymbol>>()

    /**
     * 父 scope 函数候选缓存。
     */
    private val functionsFromParents = hashMapOf<Name, List<MemberWithBaseScope<CfirNamedFunctionSymbol>>>()

    /**
     * 父 scope 属性候选缓存。
     */
    private val propertiesFromParents = hashMapOf<Name, List<MemberWithBaseScope<CfirPropertySymbol>>>()

    /**
     * 直接覆盖函数缓存。
     */
    private val directOverriddenFunctions = hashMapOf<CfirNamedFunctionSymbol, List<MemberWithBaseScope<CfirNamedFunctionSymbol>>>()

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
    ): ProcessorAction {
        val directOverridden = directOverriddenFunctions[functionSymbol]
            ?: computeDirectOverriddenForOwnFunctionIfAny(functionSymbol)
            ?: getFunctionsFromParentsByName(functionSymbol.name)
                .firstOrNull { it.symbol == functionSymbol }
                ?.let { parentMember ->
                    return parentMember.scope.processDirectOverriddenFunctionsWithBaseScope(functionSymbol, processor)
                }
            ?: return ProcessorAction.NONE
        for (candidate in directOverridden) {
            if (processor(candidate.symbol, candidate.scope) == ProcessorAction.STOP) {
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
    ): List<MemberWithBaseScope<CfirNamedFunctionSymbol>>? {
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
        dispatchReceiverType = dispatchReceiverType,
        scopeKind = scopeKind,
        allowBareGenericStaticQualifierExtends = allowBareGenericStaticQualifierExtends,
        excludingExtend = excludingExtend,
        useSitePackage = useSitePackage,
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
        }.forEach(processor)
    }

    /**
     * 收集本 scope 与父 scope 中的可见函数。
     */
    private fun collectFunctions(name: Name): Collection<CfirNamedFunctionSymbol> = buildList {
        val local = mutableListOf<CfirNamedFunctionSymbol>()
        declaredScope.processFunctionsByName(name) { local += it }
        extendScope?.processFunctionsByName(name) { local += it }
        for (symbol in local) {
            directOverriddenFunctions[symbol] = computeDirectOverriddenForDeclaredFunction(symbol)
            add(symbol)
        }

        for (candidate in getFunctionsFromParentsByName(name)) {
            if (local.any { it.overridesFunctionCandidate(candidate.symbol, it.overrideSubstitutorForOwnFunction()) } &&
                !candidate.symbol.isExtendMemberForOverrideResolution()
            ) {
                continue
            }
            add(candidate.symbol)
        }
    }

    /**
     * 按名称处理可见属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        if (name !in getCallableNames()) return

        properties.getOrPut(name) {
            collectProperties(name)
        }.forEach(processor)
    }

    /**
     * 收集本 scope 与父 scope 中的可见属性。
     */
    private fun collectProperties(name: Name): Collection<CfirPropertySymbol> = buildList {
        val local = mutableListOf<CfirPropertySymbol>()
        declaredScope.processPropertiesByName(name) { local += it }
        if (local.isEmpty()) {
            extendScope?.processPropertiesByName(name) { local += it }
        }
        if (local.isNotEmpty()) {
            for (symbol in local) {
                directOverriddenProperties[symbol] = computeDirectOverriddenForDeclaredProperty(symbol)
                add(symbol)
            }
            return@buildList
        }
        for (candidate in getPropertiesFromParentsByName(name)) {
            add(candidate.symbol)
        }
    }

    /**
     * 计算声明函数覆盖的父函数集合。
     */
    private fun computeDirectOverriddenForDeclaredFunction(
        functionSymbol: CfirNamedFunctionSymbol,
    ): List<MemberWithBaseScope<CfirNamedFunctionSymbol>> {
        return getFunctionsFromParentsByName(functionSymbol.name)
            .filter { functionSymbol.overridesFunctionCandidate(it.symbol, functionSymbol.overrideSubstitutorForOwnFunction()) }
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
    private fun getFunctionsFromParentsByName(name: Name): List<MemberWithBaseScope<CfirNamedFunctionSymbol>> {
        return functionsFromParents.getOrPut(name) {
            val parentCandidates = mutableListOf<MemberWithBaseScope<CfirNamedFunctionSymbol>>()
            for (parent in parentScopes) {
                parent.processFunctionsByName(name) { parentCandidates += MemberWithBaseScope(it, parent) }
            }
            filterOutOverridden(
                parentCandidates,
                CfirTypeScope::processDirectOverriddenFunctionsWithBaseScope,
            ).filterOutAbstractMembersImplementedByConcrete(
                CfirNamedFunctionSymbol::overridesFunctionCandidate,
            ).toList()
        }
    }

    /**
     * 返回指定名称的父属性候选。
     */
    private fun getPropertiesFromParentsByName(name: Name): List<MemberWithBaseScope<CfirPropertySymbol>> {
        return propertiesFromParents.getOrPut(name) {
            val parentCandidates = mutableListOf<MemberWithBaseScope<CfirPropertySymbol>>()
            for (parent in parentScopes) {
                parent.processPropertiesByName(name) { parentCandidates += MemberWithBaseScope(it, parent) }
            }
            filterOutOverridden(
                parentCandidates,
                CfirTypeScope::processDirectOverriddenPropertiesWithBaseScope,
            ).filterOutAbstractMembersImplementedByConcrete(
                { candidate -> canImplementPropertyCandidate(candidate) },
            ).toList()
        }
    }

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
            if (localFunctions.any { it.overridesFunctionCandidate(candidate.symbol, it.overrideSubstitutorForOwnFunction()) } &&
                !candidate.symbol.isExtendMemberForOverrideResolution()
            ) {
                continue
            }
            processor(candidate.symbol)
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
        return extendProvider?.getContainingExtend(original) != null
    }

    /**
     * extend 成员的 owner 类型参数来自 extend 声明本身，而不是目标 class 声明。
     * 因此判断它是否实现接口成员时，必须用成员访问 receiver 推导出的 extend-owner
     * substitution；否则 `extend<T> A<T> <: I<T>` 会把 `test(T)` 和 `test(Int32)`
     * 视为不同签名。
     */
    private fun CfirNamedFunctionSymbol.overrideSubstitutorForOwnFunction(): ConeSubstitutor {
        val original = unwrapSubstitutionOverrides()
        if (extendProvider?.getContainingExtend(original) == null) return ownerSubstitutor
        val receiverType = dispatchReceiverType ?: ownerType ?: return ConeSubstitutor.Empty
        return createCallableOwnerUseSiteSubstitutor(session, original, receiverType)
    }

    /**
     * 构造所有直接父类型对应的成员 scope。
     */
    private fun buildParentScopes(): List<CfirTypeScope> {
        val rootType = ownerType ?: return emptyList()
        return directParentTypesOf(rootType).mapNotNull { supertype ->
            val classId = supertype.classIdOrPrimitiveClassId ?: return@mapNotNull null
            if (supertypePath.contains(classId)) return@mapNotNull null
            val parentSymbol = symbolProvider.getClassLikeSymbolByClassId(classId) ?: return@mapNotNull null
            val parentScope = CfirClassUseSiteMemberScope(
                session = session,
                classSymbol = parentSymbol,
                symbolProvider = symbolProvider,
                extendProvider = extendProvider,
                directSupertypeProvider = directSupertypeProvider,
                ownerType = supertype,
                dispatchReceiverType = dispatchReceiverType ?: rootType,
                scopeKind = parentScopeKind(),
                allowBareGenericStaticQualifierExtends = allowBareGenericStaticQualifierExtends,
                excludingExtend = excludingExtend,
                useSitePackage = useSitePackage,
                supertypePath = supertypePath.child(classId),
            )
            parentScope.substitutionScopeForSupertype(parentSymbol, supertype)
        }
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
                useSitePackage = useSitePackage,
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
     */
    private fun directParentTypesOf(type: ConeCangJieType): List<ConeCangJieType> {
        if (scopeKind == CfirClassMemberScopeKind.DECLARATION_SITE ||
            scopeKind == CfirClassMemberScopeKind.BODY_LOOKUP
        ) {
            val substitutor = classSymbol.createDeclarationSubstitutor(type)
            return classSymbol.cfir.superTypeRefs.mapNotNull { superTypeRef ->
                val resolvedRef = superTypeRef as? CfirResolvedTypeRef ?: return@mapNotNull null
                val supertype = resolvedRef.coneType
                substitutor?.substituteOrSelf(supertype) ?: supertype
            }
        }

        val classId = type.classIdOrPrimitiveClassId ?: classSymbol.classId
        if (excludingExtend != null && directSupertypeProvider != null) {
            return directParentTypesFromEdges(type, classId, excludingExtend)
        }

        return session.typeAwareSupertypeProviderOrNull
            ?.getDirectSupertypes(type)
            ?.takeIf { it.isNotEmpty() }
            ?: directSupertypeProvider
                ?.getDirectSuperTypes(classId)
                ?.map { it.coneType }
                ?.takeIf { it.isNotEmpty() }
            ?: classSymbol.cfir.superTypeRefs.mapNotNull { superTypeRef ->
                val resolvedRef = superTypeRef as? CfirResolvedTypeRef ?: return@mapNotNull null
                resolvedRef.coneType
            }
    }

    /**
     * 在 extend 声明体内查询父类型时按 graph edge 来源跳过当前 extend 自己贡献的接口。
     */
    private fun directParentTypesFromEdges(
        type: ConeCangJieType,
        classId: ClassId,
        excludedExtend: CfirExtend,
    ): List<ConeCangJieType> {
        val ownerSymbol = symbolProvider.getClassLikeSymbolByClassId(classId)
        val ownerDeclaration = ownerSymbol?.cfir as? CfirClassLikeDeclaration
        val declarationSubstitutor = ownerSymbol?.createDeclarationSubstitutor(type)
        val directSupertypes = directSupertypeProvider
            ?.getDirectSuperTypeEdges(classId)
            .orEmpty()
            .mapNotNull { edge ->
                if (edge.sourceExtend === excludedExtend) return@mapNotNull null
                edge.toUseSiteSupertype(type, declarationSubstitutor)
            }
        return directSupertypes.withImplicitObjectSuperclass(ownerDeclaration)
    }

    /**
     * 将 supertype graph edge 还原为当前 owner type 上的 use-site 父类型。
     */
    private fun CfirSuperTypeGraphEdge.toUseSiteSupertype(
        ownerType: ConeCangJieType,
        declarationSubstitutor: ConeSubstitutor?,
    ): ConeCangJieType? {
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
            return substitution.substitutor.substituteOrSelf(supertype)
        }
        return declarationSubstitutor?.substituteOrSelf(supertype) ?: supertype
    }

    /**
     * graph edge 不包含官方隐式 `Object` 父类；edge 路径需要显式补回。
     */
    private fun List<ConeCangJieType>.withImplicitObjectSuperclass(
        declaration: CfirClassLikeDeclaration?,
    ): List<ConeCangJieType> {
        if (declaration !is CfirClass) return this
        if (declaration.symbol.classId == StdlibClassIds.Any) return this
        if (any { it.isConcreteSuperclassCandidate() }) return this

        val implicitSuperclass = if (declaration.symbol.classId == StdlibClassIds.Object) {
            ConeClassLikeType(StdlibClassIds.Any.toLookupTag(), isInterface = true)
        } else {
            ConeClassLikeType(StdlibClassIds.Object.toLookupTag())
        }
        return (this + implicitSuperclass).distinct()
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
)

/**
 * 在 base scope 中处理指定成员的直接覆盖成员的函数类型。
 */
private typealias ProcessOverriddenWithBaseScope<S> =
        CfirTypeScope.(S, (S, CfirTypeScope) -> ProcessorAction) -> ProcessorAction

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
