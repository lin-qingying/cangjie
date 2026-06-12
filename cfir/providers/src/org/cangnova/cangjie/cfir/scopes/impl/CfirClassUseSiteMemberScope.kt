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
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.resolve.providers.CfirDirectSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.isStaticMemberForOverride
import org.cangnova.cangjie.cfir.scopes.overrideSignatureKey
import org.cangnova.cangjie.cfir.session.*
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * Use-site member scope for class-like receivers. Declared members win over
 * extend members for classifier/property hiding, while functions are merged.
 */
enum class CfirClassMemberScopeKind {
    /**
     * 接收者 use-site scope。
     *
     * 用于调用解析，允许看到 extend 注入成员以及类型感知后的父类型链。
     */
    USE_SITE,

    /**
     * 声明检查 scope。
     *
     * 仅反映源码显式声明的继承关系，不把 extend 注入的父接口/成员视为类本体义务。
     */
    DECLARATION_SITE,
}

class CfirClassUseSiteMemberScope private constructor(
    private val session: CfirSession,
    private val classSymbol: CfirClassLikeSymbol<*>,
    private val symbolProvider: CfirSymbolProvider,
    private val extendProvider: CfirExtendProvider? = null,
    private val directSupertypeProvider: CfirDirectSupertypeProvider? = null,
    private val ownerType: ConeCangJieType? = declarationSelfType(classSymbol),
    private val dispatchReceiverType: ConeCangJieType? = ownerType,
    private val scopeKind: CfirClassMemberScopeKind = CfirClassMemberScopeKind.USE_SITE,
    private val supertypePath: CfirSupertypePath = CfirSupertypePath.root(classSymbol.classId),
) : CfirTypeScope() {
    constructor(
        session: CfirSession,
        classSymbol: CfirClassLikeSymbol<*>,
        symbolProvider: CfirSymbolProvider,
        extendProvider: CfirExtendProvider? = null,
        directSupertypeProvider: CfirDirectSupertypeProvider? = null,
        ownerType: ConeCangJieType? = declarationSelfType(classSymbol),
        dispatchReceiverType: ConeCangJieType? = ownerType,
        scopeKind: CfirClassMemberScopeKind = CfirClassMemberScopeKind.USE_SITE,
    ) : this(
        session = session,
        classSymbol = classSymbol,
        symbolProvider = symbolProvider,
        extendProvider = extendProvider,
        directSupertypeProvider = directSupertypeProvider,
        ownerType = ownerType,
        dispatchReceiverType = dispatchReceiverType,
        scopeKind = scopeKind,
        supertypePath = CfirSupertypePath.root(classSymbol.classId),
    )

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
        supertypePath = CfirSupertypePath.root(classSymbol.classId),
    )

    private val declaredScope = CfirClassDeclaredMemberScope(classSymbol)
    private val extendScope = takeIf { scopeKind == CfirClassMemberScopeKind.USE_SITE }
        ?.let { extendProvider?.let { provider -> CfirExtendMemberScope(classSymbol.classId, provider) } }
    private val parentScopes: List<CfirTypeScope> by lazy { buildParentScopes() }
    private val callableNamesCached by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildSet {
            addAll(declaredScope.getCallableNames())
            addAll(extendCallableNames())
            addAll(parentCallableNames())
        }
    }
    private val classifierNamesCached by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildSet {
            addAll(declaredScope.getClassifierNames())
            addAll(extendClassifierNames())
            addAll(parentClassifierNames())
        }
    }

    override fun getCallableNames(): Set<Name> = callableNamesCached

    override fun getClassifierNames(): Set<Name> = classifierNamesCached

    override fun processDirectOverriddenFunctionsWithBaseScope(
        functionSymbol: CfirNamedFunctionSymbol,
        processor: (CfirNamedFunctionSymbol, CfirTypeScope) -> ProcessorAction
    ): ProcessorAction {
        for (parent in parentScopes) {
            val candidates = mutableListOf<MemberWithBaseScope<CfirNamedFunctionSymbol>>()
            parent.processFunctionsByName(functionSymbol.name) { candidates += MemberWithBaseScope(it, parent) }
            for (candidate in filterOutOverridden(candidates, CfirTypeScope::processDirectOverriddenFunctionsWithBaseScope)) {
                if (processor(candidate.symbol, candidate.scope) == ProcessorAction.STOP) {
                    return ProcessorAction.STOP
                }
            }
        }
        return ProcessorAction.NEXT
    }

    override fun processDirectOverriddenPropertiesWithBaseScope(
        propertySymbol: CfirPropertySymbol,
        processor: (CfirPropertySymbol, CfirTypeScope) -> ProcessorAction,
    ): ProcessorAction {
        for (parent in parentScopes) {
            val candidates = mutableListOf<MemberWithBaseScope<CfirPropertySymbol>>()
            parent.processPropertiesByName(propertySymbol.name) { candidates += MemberWithBaseScope(it, parent) }
            for (candidate in filterOutOverridden(candidates, CfirTypeScope::processDirectOverriddenPropertiesWithBaseScope)) {
                if (processor(candidate.symbol, candidate.scope) == ProcessorAction.STOP) {
                    return ProcessorAction.STOP
                }
            }
        }
        return ProcessorAction.NEXT
    }

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
        supertypePath = supertypePath,
    )

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

    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        if (name !in getCallableNames()) return

        val local = mutableListOf<CfirNamedFunctionSymbol>()
        declaredScope.processFunctionsByName(name) { local += it }
        extendScope?.processFunctionsByName(name) { local += it }
        local.forEach(processor)

        val parentCandidates = mutableListOf<MemberWithBaseScope<CfirNamedFunctionSymbol>>()
        for (parent in parentScopes) {
            parent.processFunctionsByName(name) { parentCandidates += MemberWithBaseScope(it, parent) }
        }
        for (candidate in filterOutOverridden(
            parentCandidates,
            CfirTypeScope::processDirectOverriddenFunctionsWithBaseScope
        )) {
            if (local.any { it.overridesFunctionCandidate(candidate.symbol) }) continue
            processor(candidate.symbol)
        }
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        if (name !in getCallableNames()) return

        val local = mutableListOf<CfirPropertySymbol>()
        declaredScope.processPropertiesByName(name) { local += it }
        if (local.isEmpty()) {
            extendScope?.processPropertiesByName(name) { local += it }
        }
        if (local.isNotEmpty()) {
            local.forEach(processor)
            return
        }
        for (parent in parentScopes) {
            parent.processPropertiesByName(name, processor)
        }
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        if (name !in getCallableNames()) return

        val local = mutableListOf<CfirCallableSymbol<*>>()
        declaredScope.processCallablesByName(name) { local += it }
        extendScope?.processCallablesByName(name) { local += it }
        if (local.isNotEmpty()) {
            local.forEach(processor)
            if (local.any { it.isValueLikeCallable() }) return
        }
        for (parent in parentScopes) {
            parent.processCallablesByName(name, processor)
        }
    }

    private fun CfirCallableSymbol<*>.isValueLikeCallable(): Boolean =
        this is CfirPropertySymbol || this is CfirVariableSymbol<*> || this is CfirEnumConstructorSymbol

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
                scopeKind = scopeKind,
                supertypePath = supertypePath.child(classId),
            )
            parentScope.substitutionScopeForSupertype(parentSymbol, supertype)
        }
    }

    private fun extendCallableNames(): Set<Name> = buildSet {
        for (extend in extendsForCurrentClass()) {
            for (declaration in extend.declarations) {
                when (declaration) {
                    is CfirFunction -> declaration.callableNameOrNull()?.let(::add)
                    is org.cangnova.cangjie.cfir.declarations.CfirProperty -> add(declaration.name)
                    is org.cangnova.cangjie.cfir.declarations.CfirFieldVariable -> add(declaration.name)
                    is org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor -> add(declaration.name)
                    else -> Unit
                }
            }
        }
    }

    private fun extendClassifierNames(): Set<Name> = buildSet {
        for (extend in extendsForCurrentClass()) {
            for (declaration in extend.declarations) {
                if (declaration is CfirClassLikeDeclaration) {
                    add((declaration.symbol as CfirClassLikeSymbol<*>).name)
                }
            }
        }
    }

    private fun extendsForCurrentClass() = buildList {
        if (scopeKind != CfirClassMemberScopeKind.USE_SITE) return@buildList
        val provider = extendProvider ?: return@buildList
        addAll(provider.getExtendsForClass(classSymbol.classId))
        classSymbol.classId.toPrimitiveTypeKindOrNull()?.let { kind ->
            addAll(provider.getExtendsForBuiltinType(kind))
        }
    }.filter { extendProvider?.isExtendAccessible(it) != false }

    private fun parentCallableNames(): Set<Name> = buildSet {
        collectParentNames(
            ownerType = ownerType ?: return@buildSet,
            visitedClassIds = linkedSetOf(classSymbol.classId),
            collectDeclaredNames = CfirClassDeclaredMemberScope::getCallableNames,
            collectExtendNames = CfirClassUseSiteMemberScope::extendCallableNames,
            addNames = ::addAll,
        )
    }

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
                scopeKind = scopeKind,
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

    private fun directParentTypesOf(type: ConeCangJieType): List<ConeCangJieType> {
        if (scopeKind == CfirClassMemberScopeKind.DECLARATION_SITE) {
            val substitutor = classSymbol.createDeclarationSubstitutor(type)
            return classSymbol.cfir.superTypeRefs.mapNotNull { superTypeRef ->
                val resolvedRef = superTypeRef as? CfirResolvedTypeRef ?: return@mapNotNull null
                val supertype = resolvedRef.coneType
                substitutor?.substituteOrSelf(supertype) ?: supertype
            }
        }

        val classId = type.classIdOrPrimitiveClassId ?: classSymbol.classId
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

    private fun CfirTypeScope.substitutionScopeForSupertype(
        parentSymbol: CfirClassLikeSymbol<*>,
        supertype: ConeCangJieType,
    ): CfirTypeScope {
        val receiverType = dispatchReceiverType ?: return this
        if (!parentSymbol.isBound || parentSymbol.cfir.typeParameters.isEmpty()) return this
        val typeArguments = (supertype as? ConeLookupTagBasedType)?.typeArguments.orEmpty()
        if (typeArguments.isEmpty()) return this
        return CfirClassSubstitutionScope(session, this, receiverType)
    }

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
    private val classId: ClassId,
    private val parent: CfirSupertypePath?,
) {
    fun contains(candidate: ClassId): Boolean {
        var current: CfirSupertypePath? = this
        while (current != null) {
            if (current.classId == candidate) return true
            current = current.parent
        }
        return false
    }

    fun child(classId: ClassId): CfirSupertypePath = CfirSupertypePath(classId, this)

    companion object {
        fun root(classId: ClassId): CfirSupertypePath = CfirSupertypePath(classId, parent = null)
    }
}

private data class MemberWithBaseScope<S : CfirCallableSymbol<*>>(
    val symbol: S,
    val scope: CfirTypeScope,
)

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

private fun CfirNamedFunctionSymbol.overridesFunctionCandidate(
    candidate: CfirNamedFunctionSymbol,
): Boolean {
    if (this == candidate) return true
    if (isStaticMemberForOverride() != candidate.isStaticMemberForOverride()) return false
    return overrideSignatureKey() == candidate.overrideSignatureKey()
}
