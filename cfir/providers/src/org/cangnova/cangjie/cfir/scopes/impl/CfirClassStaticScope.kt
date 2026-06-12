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
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.scopeSessionKey
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * 类 qualifier 的 static scope，对齐 Kotlin FIR `FirStaticScope`。
 *
 * 这层只负责过滤 static callable；成员枚举、use-site 继承、extend 注入、
 * 泛型实参替换都必须先由 delegate scope 完成，避免 static 解析绕开
 * `CfirClassSubstitutionScope`。
 */
class CfirClassStaticScope(
    private val delegateScope: CfirContainingNamesAwareScope,
) : CfirContainingNamesAwareScope() {
    override fun getCallableNames(): Set<Name> = delegateScope.getCallableNames()

    override fun getClassifierNames(): Set<Name> = delegateScope.getClassifierNames()

    override fun mayContainName(name: Name): Boolean = delegateScope.mayContainName(name)

    override val scopeOwnerLookupNames: List<String>
        get() = delegateScope.scopeOwnerLookupNames

    override val hasDefinitelyNoStaticMembers: Boolean
        get() = delegateScope.hasDefinitelyNoStaticMembers

    override fun processClassifiersByNameWithSubstitution(
        name: Name,
        processor: (CfirClassifierSymbol<*>, ConeSubstitutor) -> Unit,
    ) {
        delegateScope.processClassifiersByNameWithSubstitution(name, processor)
    }

    override fun processClassifiersByName(name: Name, processor: (CfirClassLikeSymbol<*>) -> Unit) {
        delegateScope.processClassifiersByName(name, processor)
    }

    override fun processFunctionsByName(name: Name, processor: (CfirNamedFunctionSymbol) -> Unit) {
        delegateScope.processFunctionsByName(name) { function ->
            if (function.isStaticCallableForClassQualifier()) {
                processor(function)
            }
        }
    }

    override fun processPropertiesByName(name: Name, processor: (CfirPropertySymbol) -> Unit) {
        delegateScope.processPropertiesByName(name) { property ->
            if (property.isStaticCallableForClassQualifier()) {
                processor(property)
            }
        }
    }

    override fun processCallablesByName(name: Name, processor: (CfirCallableSymbol<*>) -> Unit) {
        delegateScope.processCallablesByName(name) { callable ->
            if (callable.isStaticCallableForClassQualifier()) {
                processor(callable)
            }
        }
    }

    override fun withReplacedSessionOrNull(
        newSession: CfirSession,
        newScopeSession: ScopeSession,
    ): CfirContainingNamesAwareScope? =
        delegateScope.withReplacedSessionOrNull(newSession, newScopeSession)?.let(::CfirClassStaticScope)
}

fun CfirClassLikeSymbol<*>.staticScopeForQualifierType(
    session: CfirSession,
    scopeSession: ScopeSession,
    qualifierType: ConeCangJieType = constructType(),
): CfirContainingNamesAwareScope {
    val expandedQualifierType = qualifierType.fullyExpandedType(session)
    val cacheKey = StaticScopeForQualifierTypeKey(classId, expandedQualifierType)
    return scopeSession.getOrBuild(cacheKey, StaticScopeForQualifierTypeScopeKey) {
        val useSiteScope = CfirClassUseSiteMemberScope(
            session = session,
            classSymbol = this,
            symbolProvider = session.symbolProvider,
            extendProvider = session.extendProvider,
            directSupertypeProvider = session.directSupertypeProviderOrNull,
            ownerType = expandedQualifierType,
            dispatchReceiverType = expandedQualifierType,
            scopeKind = CfirClassMemberScopeKind.USE_SITE,
        )
        CfirClassStaticScope(CfirClassSubstitutionScope(session, useSiteScope, expandedQualifierType))
    }
}

private fun CfirCallableSymbol<*>.isStaticCallableForClassQualifier(): Boolean {
    if (this is CfirEnumConstructorSymbol) return true
    if (this !is CfirNamedFunctionSymbol && this !is CfirPropertySymbol && this !is CfirFieldVariableSymbol) {
        return false
    }

    lazyResolveToPhase(org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.STATUS)
    return cfir.status.isStatic
}

private data class StaticScopeForQualifierTypeKey(
    val classId: ClassId,
    val qualifierType: ConeCangJieType,
)

private val StaticScopeForQualifierTypeScopeKey: ScopeSessionKey<StaticScopeForQualifierTypeKey, CfirContainingNamesAwareScope> =
    scopeSessionKey()
