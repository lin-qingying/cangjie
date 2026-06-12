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

package org.cangnova.cangjie.cfir.resolve.body

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.calls.ImplicitDispatchReceiverValue
import org.cangnova.cangjie.cfir.calls.ImplicitReceiverValue
import org.cangnova.cangjie.cfir.calls.ImplicitValue
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.ImplicitValueMapper
import org.cangnova.cangjie.cfir.resolve.ImplicitValueStorage
import org.cangnova.cangjie.cfir.resolve.LocalVariableScopeStorage
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.scopes.impl.staticScopeForQualifierType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.Name

@ConsistentCopyVisibility
data class CfirTowerDataContext private constructor(
    val towerDataElements: PersistentList<CfirTowerDataElement>,
    val implicitValueStorage: ImplicitValueStorage,
    val localScopes: CfirLocalScopes,
    val nonLocalTowerDataElements: PersistentList<CfirTowerDataElement>,
    val localVariableScopeStorage: LocalVariableScopeStorage,
) {
    constructor() : this(
        towerDataElements = persistentListOf(),
        implicitValueStorage = ImplicitValueStorage(),
        localScopes = persistentListOf(),
        nonLocalTowerDataElements = persistentListOf(),
        localVariableScopeStorage = LocalVariableScopeStorage(),
    )
    fun addLocalVariable(variable: CfirVariable, session: CfirSession): CfirTowerDataContext {
        val oldLastScope = localScopes.lastOrNull() ?: return this
        val indexOfLastLocalScope = towerDataElements.indexOfLast { it.scope === oldLastScope }
        val newLastScope = oldLastScope.storeVariable(variable, session)

        return copy(
            towerDataElements = towerDataElements.set(indexOfLastLocalScope, newLastScope.asTowerDataElement(isLocal = true)),
            localScopes = localScopes.set(localScopes.lastIndex, newLastScope),
            localVariableScopeStorage = localVariableScopeStorage.addLocalVariable(variable.symbol)
        )
    }

    fun addLocalProperty(property: CfirProperty, session: CfirSession): CfirTowerDataContext {
        val oldLastScope = localScopes.lastOrNull() ?: return this
        val indexOfLastLocalScope = towerDataElements.indexOfLast { it.scope === oldLastScope }
        val newLastScope = oldLastScope.storeProperty(property, session)

        return copy(
            towerDataElements = towerDataElements.set(indexOfLastLocalScope, newLastScope.asTowerDataElement(isLocal = true)),
            localScopes = localScopes.set(localScopes.lastIndex, newLastScope),
            localVariableScopeStorage = localVariableScopeStorage.addLocalVariable(property.symbol)
        )
    }


    fun setLastLocalScope(newLastScope: CfirLocalScope): CfirTowerDataContext {
        val oldLastScope = localScopes.last()
        val indexOfLastLocalScope = towerDataElements.indexOfLast { it.scope === oldLastScope }

        return copy(
            towerDataElements = towerDataElements.set(indexOfLastLocalScope, newLastScope.asTowerDataElement(isLocal = true)),
            localScopes = localScopes.set(localScopes.lastIndex, newLastScope),
        )
    }

    fun addNonLocalTowerDataElements(newElements: List<CfirTowerDataElement>): CfirTowerDataContext {
        return copy(
            towerDataElements = towerDataElements.addAll(newElements),
            implicitValueStorage = implicitValueStorage.addAllImplicitReceivers(newElements.mapNotNull { it.implicitReceiver }),
            nonLocalTowerDataElements = nonLocalTowerDataElements.addAll(newElements),
        )
    }

    fun addLocalScope(localScope: CfirLocalScope): CfirTowerDataContext {
        return copy(
            towerDataElements = towerDataElements.add(localScope.asTowerDataElement(isLocal = true)),
            localScopes = localScopes.add(localScope),
        )
    }

    fun addReceiver(name: Name?, implicitReceiverValue: ImplicitReceiverValue<*>): CfirTowerDataContext {
        val element = implicitReceiverValue.asTowerDataElement()
        return copy(
            towerDataElements = towerDataElements.add(element),
            implicitValueStorage = implicitValueStorage.addImplicitReceiver(name, implicitReceiverValue),
            nonLocalTowerDataElements = nonLocalTowerDataElements.add(element),
        )
    }

    fun addReceiverIfNotNull(name: Name?, implicitReceiverValue: ImplicitReceiverValue<*>?): CfirTowerDataContext {
        if (implicitReceiverValue == null) return this
        return addReceiver(name, implicitReceiverValue)
    }

    fun addNonLocalScopeIfNotNull(scope: CfirScope?): CfirTowerDataContext {
        if (scope == null) return this
        return addNonLocalScope(scope)
    }

    fun addNonLocalScopesIfNotNull(scope1: CfirScope?, scope2: CfirScope?): CfirTowerDataContext {
        return if (scope1 != null) {
            if (scope2 != null) {
                addNonLocalScopeElements(listOf(scope1.asTowerDataElement(isLocal = false), scope2.asTowerDataElement(isLocal = false)))
            } else {
                addNonLocalScope(scope1)
            }
        } else if (scope2 != null) {
            addNonLocalScope(scope2)
        } else {
            this
        }
    }

    fun addNonLocalScope(scope: CfirScope): CfirTowerDataContext {
        val element = scope.asTowerDataElement(isLocal = false)
        return copy(
            towerDataElements = towerDataElements.add(element),
            nonLocalTowerDataElements = nonLocalTowerDataElements.add(element),
        )
    }

    private fun addNonLocalScopeElements(elements: List<CfirTowerDataElement>): CfirTowerDataContext {
        return copy(
            towerDataElements = towerDataElements.addAll(elements),
            nonLocalTowerDataElements = nonLocalTowerDataElements.addAll(elements),
        )
    }

    fun createSnapshot(keepMutable: Boolean): CfirTowerDataContext {
        val implicitValueMapper = object : ImplicitValueMapper {
            private val implicitValueCache = HashMap<ImplicitValue<*>, ImplicitValue<*>>()

            override fun <S : org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>, T : ImplicitValue<S>> invoke(value: T): T {
                @Suppress("UNCHECKED_CAST")
                return implicitValueCache.getOrPut(value) { value.createSnapshot(keepMutable) } as T
            }
        }

        return copy(
            towerDataElements = towerDataElements.map { it.createSnapshot(keepMutable, implicitValueMapper) }.toPersistentList(),
            implicitValueStorage = implicitValueStorage.createSnapshot(implicitValueMapper),
            // CfirLocalScope 本身 persistent/immutable，复用即可
            localScopes = localScopes,
            nonLocalTowerDataElements = nonLocalTowerDataElements.map { it.createSnapshot(keepMutable, implicitValueMapper) }.toPersistentList(),
        )
    }

    fun replaceTowerDataElements(
        towerDataElements: PersistentList<CfirTowerDataElement>,
        nonLocalTowerDataElements: PersistentList<CfirTowerDataElement>,
    ): CfirTowerDataContext {
        return copy(
            towerDataElements = towerDataElements,
            nonLocalTowerDataElements = nonLocalTowerDataElements,
        )
    }
}

class CfirTowerDataElement(
    val scope: CfirScope?,
    val implicitReceiver: ImplicitReceiverValue<*>?,
    val isLocal: Boolean,
    val staticScopeOwnerSymbol: CfirClassLikeSymbol<*>? = null,
) {
    internal fun createSnapshot(keepMutable: Boolean, mapper: ImplicitValueMapper): CfirTowerDataElement =
        CfirTowerDataElement(
            scope = scope,
            implicitReceiver = implicitReceiver?.let { mapper(it) },
            isLocal = isLocal,
            staticScopeOwnerSymbol = staticScopeOwnerSymbol,
        )

    fun getAvailableScopes(
        processTypeScope: CfirTypeScope.(ConeCangJieType) -> CfirTypeScope = { this },
    ): List<CfirScope> = when {
        scope != null -> listOf(scope)
        implicitReceiver != null -> listOf(implicitReceiver.getImplicitScope(processTypeScope))
        else -> error("Tower data element is expected to have either scope or implicit receiver.")
    }

    private fun ImplicitReceiverValue<*>.getImplicitScope(
        processTypeScope: CfirTypeScope.(ConeCangJieType) -> CfirTypeScope,
    ): CfirScope {
        val implicitScope = implicitScope ?: return CfirTypeScope.Empty
        if (type is ConeErrorType || type is ConeStubType) return CfirTypeScope.Empty
        return implicitScope.processTypeScope(type)
    }
}

fun ImplicitReceiverValue<*>.asTowerDataElement(): CfirTowerDataElement =
    CfirTowerDataElement(scope = null, implicitReceiver = this, isLocal = false)

fun CfirScope.asTowerDataElement(isLocal: Boolean): CfirTowerDataElement =
    CfirTowerDataElement(scope = this, implicitReceiver = null, isLocal = isLocal)

fun CfirScope.asTowerDataElementForStaticScope(staticScopeOwnerSymbol: CfirClassLikeSymbol<*>?): CfirTowerDataElement =
    CfirTowerDataElement(scope = this, implicitReceiver = null, isLocal = false, staticScopeOwnerSymbol = staticScopeOwnerSymbol)

class CfirTowerElementsForClass(
    val thisReceiver: ImplicitReceiverValue<*>,
    val staticScope: CfirScope?,
    val superClassesStaticScopes: List<CfirTowerDataElement>,
)

fun SessionAndScopeSessionHolder.collectTowerDataElementsForClass(
    owner: CfirClassLikeDeclaration,
    defaultType: ConeCangJieType,
): CfirTowerElementsForClass {
    val ownerSymbol = owner.symbol as? CfirClassLikeSymbol<*>
        ?: error("Class-like declaration ${owner::class.simpleName} must have CfirClassLikeSymbol")

    val superClassesStatics = linkedSetOf<CfirTowerDataElement>()
    collectSuperClassesStaticScopes(owner, session, scopeSession, superClassesStatics, linkedSetOf())

    val thisReceiver = ImplicitDispatchReceiverValue(ownerSymbol, defaultType, session, scopeSession)

    return CfirTowerElementsForClass(
        thisReceiver = thisReceiver,
        staticScope = owner.staticScope(session, scopeSession, defaultType),
        superClassesStaticScopes = superClassesStatics.toList().asReversed(),
    )
}

fun CfirClassLikeDeclaration.staticScope(sessionHolder: SessionAndScopeSessionHolder): CfirContainingNamesAwareScope? =
    staticScope(sessionHolder.session, sessionHolder.scopeSession)

fun CfirClassLikeDeclaration.staticScope(
    session: CfirSession,
    scopeSession: ScopeSession,
    qualifierType: ConeCangJieType? = null,
): CfirContainingNamesAwareScope? {
    val symbol = symbol as? CfirClassLikeSymbol<*> ?: return null
    return symbol.staticScopeForQualifierType(session, scopeSession, qualifierType ?: symbol.constructType())
}

fun CfirClassLikeDeclaration.typeParametersForTower(): List<CfirTypeParameter> = when (this) {
    is CfirClass -> typeParameters
    is org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration -> emptyList()
    is CfirInterface -> typeParameters
    is CfirStruct -> typeParameters
    is CfirEnum -> typeParameters
    else -> emptyList()
}

private fun collectSuperClassesStaticScopes(
    owner: CfirClassLikeDeclaration,
    session: CfirSession,
    scopeSession: ScopeSession,
    result: MutableSet<CfirTowerDataElement>,
    visited: MutableSet<CfirClassLikeSymbol<*>>,
) {
    for (superTypeRef in owner.superTypeRefs) {
        val resolvedTypeRef = superTypeRef as? CfirResolvedTypeRef ?: continue
        val classId = when (val coneType = resolvedTypeRef.coneType) {
            is ConeClassLikeType -> coneType.classId
            is ConeStructType -> coneType.classId
            is ConeEnumType -> coneType.classId
            else -> continue
        }

        val superSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: continue
        if (!visited.add(superSymbol)) continue

        val superDeclaration = superSymbol.cfir
        if (superDeclaration !is CfirClassLikeDeclaration || superDeclaration is CfirInterface) continue

        superDeclaration.staticScope(session, scopeSession, resolvedTypeRef.coneType)
            ?.asTowerDataElementForStaticScope(superSymbol)
            ?.let(result::add)

        collectSuperClassesStaticScopes(superDeclaration, session, scopeSession, result, visited)
    }
}
