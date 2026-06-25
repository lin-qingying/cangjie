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
import org.cangnova.cangjie.cfir.ScopeSessionKey
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityFileScope
import org.cangnova.cangjie.cfir.resolve.providers.isBareOrDeclarationSelfTypeOf
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.scopeSessionKey
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.collectUpperBounds
import org.cangnova.cangjie.cfir.types.extendTargetKey
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 类 qualifier 的 static scope，对齐 Kotlin FIR `FirStaticScope`。
 *
 * 这层只负责过滤 static callable；成员枚举、use-site 继承、extend 注入、
 * 泛型实参替换都必须先由 delegate scope 完成，避免 static 解析绕开
 * `CfirClassSubstitutionScope`。
 */
class CfirClassStaticScope(
    /**
     * 已完成 use-site 和类型替换的委托 scope。
     */
    private val delegateScope: CfirContainingNamesAwareScope,
) : CfirContainingNamesAwareScope() {
    /**
     * 返回委托 scope 的 callable 名称集合。
     */
    override fun getCallableNames(): Set<Name> = delegateScope.getCallableNames()

    /**
     * 返回委托 scope 的 classifier 名称集合。
     */
    override fun getClassifierNames(): Set<Name> = delegateScope.getClassifierNames()

    /**
     * 判断委托 scope 是否可能包含指定名称。
     */
    override fun mayContainName(name: Name): Boolean = delegateScope.mayContainName(name)

    /**
     * 返回委托 scope owner lookup 名称。
     */
    override val scopeOwnerLookupNames: List<String>
        get() = delegateScope.scopeOwnerLookupNames

    /**
     * 返回委托 scope 是否确定没有静态成员。
     */
    override val hasDefinitelyNoStaticMembers: Boolean
        get() = delegateScope.hasDefinitelyNoStaticMembers

    /**
     * 透传 classifier + substitutor 查询。
     */
    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (CfirClassifierSymbol<*>, ConeSubstitutor) -> Unit,
    ) {
        delegateScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    /**
     * 透传 classifier 查询。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        delegateScope.processClassifiersByName(name, processor)
    }

    /**
     * 只处理静态函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        delegateScope.processFunctionsByName(name) { function ->
            if (function.isStaticCallableForClassQualifier()) {
                processor(function)
            }
        }
    }

    /**
     * 只处理静态属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        delegateScope.processPropertiesByName(name) { property ->
            if (property.isStaticCallableForClassQualifier()) {
                processor(property)
            }
        }
    }

    /**
     * 只处理静态 callable。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        delegateScope.processCallablesByName(name) { callable ->
            if (callable.isStaticCallableForClassQualifier()) {
                processor(callable)
            }
        }
    }

    /**
     * 替换委托 scope 的 session 后重建 static scope。
     */
    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession,
    ): CfirContainingNamesAwareScope? =
        delegateScope.withReplacedSessionOrNull(newSession, newScopeSession)?.let(::CfirClassStaticScope)
}

/**
 * 返回类型参数 qualifier 的静态 scope。
 */
fun CfirTypeParameterSymbol.staticScopeForQualifierType(
    session: CfirSession,
    scopeSession: ScopeSession,
): CfirContainingNamesAwareScope =
    scopeSession.getOrBuild(this, StaticScopeForTypeParameterQualifierScopeKey) {
        val typeParameterType = constructType() as ConeTypeParameterType
        val upperBoundStaticScopes = typeParameterType
            .collectUpperBounds(session.typeContext)
            .mapNotNull { upperBound -> upperBound.staticScopeForUpperBound(session, scopeSession) }

        when (upperBoundStaticScopes.size) {
            0 -> CfirEmptyContainingNamesAwareScope
            1 -> upperBoundStaticScopes.single()
            else -> CfirCompositeContainingNamesAwareScope(upperBoundStaticScopes)
        }
    }

/**
 * 返回 class-like qualifier 的静态 scope。
 */
fun CfirClassLikeSymbol<*>.staticScopeForQualifierType(
    session: CfirSession,
    scopeSession: ScopeSession,
    qualifierType: ConeCangJieType = constructType(),
): CfirContainingNamesAwareScope {
    val expandedQualifierType = qualifierType.fullyExpandedType(session)
    val useSitePackage = CfirAccessibilityFileScope.currentPackageFqName()
    val cacheKey = StaticScopeForQualifierTypeKey(classId, expandedQualifierType, useSitePackage)
    return scopeSession.getOrBuild(cacheKey, StaticScopeForQualifierTypeScopeKey) {
        val allowBareGenericStaticQualifierExtends =
            expandedQualifierType is ConeLookupTagBasedType &&
                    expandedQualifierType.isBareOrDeclarationSelfTypeOf(this) &&
                    cfir.typeParameters.isNotEmpty()
        val useSiteScope = CfirClassUseSiteMemberScope(
            session = session,
            classSymbol = this,
            symbolProvider = session.symbolProvider,
            extendProvider = session.extendProvider,
            directSupertypeProvider = session.directSupertypeProviderOrNull,
            ownerType = expandedQualifierType,
            dispatchReceiverType = expandedQualifierType,
            scopeKind = CfirClassMemberScopeKind.USE_SITE,
            allowBareGenericStaticQualifierExtends = allowBareGenericStaticQualifierExtends,
            useSitePackage = useSitePackage,
        )
        CfirClassStaticScope(CfirClassSubstitutionScope(session, useSiteScope, expandedQualifierType))
    }
}

/**
 * 返回没有 class-like symbol 的内建类型 qualifier 的静态 scope。
 *
 * `CPointer<T>` / `CString` 在官方前端通过 `builtinTyToExtendMap`
 * 查询 extend 成员；它们没有 ClassId，不能复用 class-like static scope。
 */
fun ConeCangJieType.staticScopeForBuiltinQualifierType(
    session: CfirSession,
    scopeSession: ScopeSession,
): CfirContainingNamesAwareScope? {
    val expandedQualifierType = fullyExpandedType(session)
    val targetKey = expandedQualifierType.extendTargetKey ?: return null
    if (targetKey is CfirExtendTargetKey.ClassLike) return null

    val useSitePackage = CfirAccessibilityFileScope.currentPackageFqName()
    val cacheKey = StaticScopeForBuiltinQualifierTypeKey(targetKey, expandedQualifierType, useSitePackage)
    return scopeSession.getOrBuild(cacheKey, StaticScopeForBuiltinQualifierTypeScopeKey) {
        CfirClassStaticScope(
            CfirExtendMemberScope(
                targetKey = targetKey,
                extendProvider = session.extendProvider,
                session = session,
                receiverType = expandedQualifierType,
                useSitePackage = useSitePackage,
            )
        )
    }
}

/**
 * 根据类型参数上界恢复可用于静态查询的 scope。
 */
private fun ConeCangJieType.staticScopeForUpperBound(
    session: CfirSession,
    scopeSession: ScopeSession,
): CfirContainingNamesAwareScope? {
    return when (val expandedType = fullyExpandedType(session)) {
        is ConeErrorType -> null
        is ConeTypeParameterType -> CfirCompositeContainingNamesAwareScope(
            expandedType.collectUpperBounds(session.typeContext)
                .mapNotNull { it.staticScopeForUpperBound(session, scopeSession) }
        )
        is ConeTypeVariableType -> {
            val originalTypeParameter = expandedType.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag
                ?: return null
            originalTypeParameter.typeParameterSymbol.staticScopeForQualifierType(session, scopeSession)
        }
        is ConeIntersectionType -> CfirCompositeContainingNamesAwareScope(
            expandedType.intersectedTypes.mapNotNull { it.staticScopeForUpperBound(session, scopeSession) }
        )
        else -> {
            val classId = expandedType.classIdOrPrimitiveClassId ?: return null
            val symbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
            symbol.staticScopeForQualifierType(session, scopeSession, expandedType)
        }
    }
}

/**
 * 空的 containing-names-aware scope。
 */
private object CfirEmptyContainingNamesAwareScope : CfirContainingNamesAwareScope() {
    /**
     * 空 scope 没有 callable 名称。
     */
    override fun getCallableNames(): Set<Name> = emptySet()

    /**
     * 空 scope 没有 classifier 名称。
     */
    override fun getClassifierNames(): Set<Name> = emptySet()

    /**
     * 空 scope 确定没有静态成员。
     */
    override val hasDefinitelyNoStaticMembers: Boolean
        get() = true

    /**
     * 空 scope 不包含任何名称。
     */
    override fun mayContainName(name: Name): Boolean = false

    /**
     * 空 scope 跨 session 替换仍返回自身。
     */
    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession,
    ): CfirContainingNamesAwareScope = this
}

/**
 * 多个 containing-names-aware scope 的组合。
 */
private class CfirCompositeContainingNamesAwareScope(
    /**
     * 被组合的 scope 列表。
     */
    private val scopes: List<CfirContainingNamesAwareScope>,
) : CfirContainingNamesAwareScope() {
    /**
     * 返回 callable 名称并集。
     */
    override fun getCallableNames(): Set<Name> = buildSet {
        scopes.forEach { addAll(it.getCallableNames()) }
    }

    /**
     * 返回 classifier 名称并集。
     */
    override fun getClassifierNames(): Set<Name> = buildSet {
        scopes.forEach { addAll(it.getClassifierNames()) }
    }

    /**
     * 所有子 scope 都确定无静态成员时为 `true`。
     */
    override val hasDefinitelyNoStaticMembers: Boolean
        get() = scopes.all { it.hasDefinitelyNoStaticMembers }

    /**
     * 合并所有子 scope owner lookup 名称。
     */
    override val scopeOwnerLookupNames: List<String>
        get() = scopes.flatMap { it.scopeOwnerLookupNames }

    /**
     * 任意子 scope 可能包含指定名称时为 `true`。
     */
    override fun mayContainName(name: Name): Boolean = scopes.any { it.mayContainName(name) }

    /**
     * 在所有子 scope 中处理 classifier + substitutor。
     */
    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (CfirClassifierSymbol<*>, ConeSubstitutor) -> Unit,
    ) {
        scopes.forEach { it.processClassifiersByNameWithSubstitution(name, processor) }
    }

    /**
     * 在所有子 scope 中处理 classifier。
     */
    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        scopes.forEach { it.processClassifiersByName(name, processor) }
    }

    /**
     * 在所有子 scope 中处理函数。
     */
    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        scopes.forEach { it.processFunctionsByName(name, processor) }
    }

    /**
     * 在所有子 scope 中处理属性。
     */
    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        scopes.forEach { it.processPropertiesByName(name, processor) }
    }

    /**
     * 在所有子 scope 中处理 callable。
     */
    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        scopes.forEach { it.processCallablesByName(name, processor) }
    }

    /**
     * 替换所有子 scope 的 session 后重建组合 scope。
     */
    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession,
    ): CfirContainingNamesAwareScope? {
        val replacedScopes = scopes.mapNotNull { it.withReplacedSessionOrNull(newSession, newScopeSession) }
        return if (replacedScopes.size == scopes.size) {
            CfirCompositeContainingNamesAwareScope(replacedScopes)
        } else {
            null
        }
    }
}

/**
 * 判断 callable 是否可通过 class qualifier 作为静态成员访问。
 */
private fun CfirCallableSymbol<*>.isStaticCallableForClassQualifier(): Boolean {
    if (this is CfirEnumConstructorSymbol) return true
    if (this !is CfirNamedFunctionSymbol && this !is CfirPropertySymbol && this !is CfirFieldVariableSymbol) {
        return false
    }

    lazyResolveToPhase(org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.STATUS)
    return cfir.status.isStatic
}

/**
 * class-like qualifier static scope 的缓存 key。
 *
 * @property classId qualifier class id。
 * @property qualifierType qualifier 的具体类型。
 * @property useSitePackage 当前 use-site 包名。
 */
private data class StaticScopeForQualifierTypeKey(
    /**
     * qualifier 对应的 class id。
     */
    val classId: ClassId,
    /**
     * qualifier 在当前 use-site 的具体类型。
     */
    val qualifierType: ConeCangJieType,
    /**
     * 当前 use-site 包名，用于过滤 extend 静态成员可见性。
     */
    val useSitePackage: FqName?,
)

/**
 * non-class built-in qualifier static scope 的缓存 key。
 */
private data class StaticScopeForBuiltinQualifierTypeKey(
    /**
     * non-class built-in qualifier 对应的 extend 目标 key。
     */
    val targetKey: CfirExtendTargetKey,
    /**
     * qualifier 在当前 use-site 的具体类型。
     */
    val qualifierType: ConeCangJieType,
    /**
     * 当前 use-site 包名，用于过滤 extend 静态成员可见性。
     */
    val useSitePackage: FqName?,
)

/**
 * class-like qualifier static scope 的 ScopeSession key。
 */
private val StaticScopeForQualifierTypeScopeKey: ScopeSessionKey<StaticScopeForQualifierTypeKey, CfirContainingNamesAwareScope> =
    scopeSessionKey()

/**
 * non-class built-in qualifier static scope 的 ScopeSession key。
 */
private val StaticScopeForBuiltinQualifierTypeScopeKey: ScopeSessionKey<StaticScopeForBuiltinQualifierTypeKey, CfirContainingNamesAwareScope> =
    scopeSessionKey()

/**
 * 类型参数 qualifier static scope 的 ScopeSession key。
 */
private val StaticScopeForTypeParameterQualifierScopeKey: ScopeSessionKey<CfirTypeParameterSymbol, CfirContainingNamesAwareScope> =
    scopeSessionKey()
