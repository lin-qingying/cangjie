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
import org.cangnova.cangjie.cfir.resolve.providers.classifyDeclaredSupertype
import org.cangnova.cangjie.cfir.resolve.providers.scopeTraversalTypeOrNull
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.scopes.impl.staticScopeForQualifierType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.Name

/**
 * body resolve 中的 tower data 上下文。
 *
 * 该上下文以 persistent 结构保存当前可见的局部作用域、非局部作用域和隐式接收者。
 */
@ConsistentCopyVisibility
data class CfirTowerDataContext private constructor(
    /**
     * 当前完整 tower data 元素列表。
     */
    val towerDataElements: PersistentList<CfirTowerDataElement>,
    /**
     * 隐式接收者存储。
     */
    val implicitValueStorage: ImplicitValueStorage,
    /**
     * 当前局部作用域栈。
     */
    val localScopes: CfirLocalScopes,
    /**
     * 非局部 tower data 元素列表。
     */
    val nonLocalTowerDataElements: PersistentList<CfirTowerDataElement>,
    /**
     * 局部变量符号作用域存储。
     */
    val localVariableScopeStorage: LocalVariableScopeStorage,
) {
    /**
     * 创建空 tower data 上下文。
     */
    constructor() : this(
        towerDataElements = persistentListOf(),
        implicitValueStorage = ImplicitValueStorage(),
        localScopes = persistentListOf(),
        nonLocalTowerDataElements = persistentListOf(),
        localVariableScopeStorage = LocalVariableScopeStorage(),
    )
    /**
     * 将局部变量加入最后一个局部作用域。
     */
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

    /**
     * 将局部属性加入最后一个局部作用域。
     */
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


    /**
     * 替换最后一个局部作用域。
     */
    fun setLastLocalScope(newLastScope: CfirLocalScope): CfirTowerDataContext {
        val oldLastScope = localScopes.last()
        val indexOfLastLocalScope = towerDataElements.indexOfLast { it.scope === oldLastScope }

        return copy(
            towerDataElements = towerDataElements.set(indexOfLastLocalScope, newLastScope.asTowerDataElement(isLocal = true)),
            localScopes = localScopes.set(localScopes.lastIndex, newLastScope),
        )
    }

    /**
     * 追加一组非局部 tower data 元素。
     */
    fun addNonLocalTowerDataElements(newElements: List<CfirTowerDataElement>): CfirTowerDataContext {
        return copy(
            towerDataElements = towerDataElements.addAll(newElements),
            implicitValueStorage = implicitValueStorage.addAllImplicitReceivers(newElements.mapNotNull { it.implicitReceiver }),
            nonLocalTowerDataElements = nonLocalTowerDataElements.addAll(newElements),
        )
    }

    /**
     * 追加一个局部作用域。
     */
    fun addLocalScope(localScope: CfirLocalScope): CfirTowerDataContext {
        return copy(
            towerDataElements = towerDataElements.add(localScope.asTowerDataElement(isLocal = true)),
            localScopes = localScopes.add(localScope),
        )
    }

    /**
     * 追加隐式接收者。
     */
    fun addReceiver(name: Name?, implicitReceiverValue: ImplicitReceiverValue<*>): CfirTowerDataContext {
        val element = implicitReceiverValue.asTowerDataElement()
        return copy(
            towerDataElements = towerDataElements.add(element),
            implicitValueStorage = implicitValueStorage.addImplicitReceiver(name, implicitReceiverValue),
            nonLocalTowerDataElements = nonLocalTowerDataElements.add(element),
        )
    }

    /**
     * 非空时追加隐式接收者。
     */
    fun addReceiverIfNotNull(name: Name?, implicitReceiverValue: ImplicitReceiverValue<*>?): CfirTowerDataContext {
        if (implicitReceiverValue == null) return this
        return addReceiver(name, implicitReceiverValue)
    }

    /**
     * 非空时追加非局部作用域。
     */
    fun addNonLocalScopeIfNotNull(scope: CfirScope?): CfirTowerDataContext {
        if (scope == null) return this
        return addNonLocalScope(scope)
    }

    /**
     * 按顺序追加最多两个非局部作用域。
     */
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

    /**
     * 追加一个非局部作用域。
     */
    fun addNonLocalScope(scope: CfirScope): CfirTowerDataContext {
        val element = scope.asTowerDataElement(isLocal = false)
        return copy(
            towerDataElements = towerDataElements.add(element),
            nonLocalTowerDataElements = nonLocalTowerDataElements.add(element),
        )
    }

    /**
     * 追加多个非局部作用域元素。
     */
    private fun addNonLocalScopeElements(elements: List<CfirTowerDataElement>): CfirTowerDataContext {
        return copy(
            towerDataElements = towerDataElements.addAll(elements),
            nonLocalTowerDataElements = nonLocalTowerDataElements.addAll(elements),
        )
    }

    /**
     * 创建 tower data 快照。
     */
    fun createSnapshot(keepMutable: Boolean): CfirTowerDataContext {
        val implicitValueMapper = object : ImplicitValueMapper {
            private val implicitValueCache = HashMap<ImplicitValue<*>, ImplicitValue<*>>()

            /** 为隐式值创建或复用快照，保证同一原始 receiver 在快照中仍保持引用一致性。 */
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

    /**
     * 替换完整 tower data 元素列表。
     */
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

/**
 * 单个 tower data 元素。
 */
class CfirTowerDataElement(
    /**
     * 元素提供的作用域。
     */
    val scope: CfirScope?,
    /**
     * 元素提供的隐式接收者。
     */
    val implicitReceiver: ImplicitReceiverValue<*>?,
    /**
     * 元素是否来自局部作用域。
     */
    val isLocal: Boolean,
    /**
     * 静态作用域的 owner 符号。
     */
    val staticScopeOwnerSymbol: CfirClassLikeSymbol<*>? = null,
) {
    /**
     * 创建 tower data 元素快照。
     */
    internal fun createSnapshot(keepMutable: Boolean, mapper: ImplicitValueMapper): CfirTowerDataElement =
        CfirTowerDataElement(
            scope = scope,
            implicitReceiver = implicitReceiver?.let { mapper(it) },
            isLocal = isLocal,
            staticScopeOwnerSymbol = staticScopeOwnerSymbol,
        )

    /**
     * 获取该 tower data 元素当前可用的作用域列表。
     */
    fun getAvailableScopes(
        processTypeScope: CfirTypeScope.(ConeCangJieType) -> CfirTypeScope = { this },
    ): List<CfirScope> = when {
        scope != null -> listOf(scope)
        implicitReceiver != null -> listOf(implicitReceiver.getImplicitScope(processTypeScope))
        else -> error("Tower data element is expected to have either scope or implicit receiver.")
    }

    /**
     * 从隐式接收者创建可查找作用域。
     */
    private fun ImplicitReceiverValue<*>.getImplicitScope(
        processTypeScope: CfirTypeScope.(ConeCangJieType) -> CfirTypeScope,
    ): CfirScope {
        val implicitScope = implicitScope ?: return CfirTypeScope.Empty
        if (type is ConeErrorType || type is ConeStubType) return CfirTypeScope.Empty
        return implicitScope.processTypeScope(type)
    }
}

/**
 * 将隐式接收者包装为 tower data 元素。
 */
fun ImplicitReceiverValue<*>.asTowerDataElement(): CfirTowerDataElement =
    CfirTowerDataElement(scope = null, implicitReceiver = this, isLocal = false)

/**
 * 将作用域包装为 tower data 元素。
 */
fun CfirScope.asTowerDataElement(isLocal: Boolean): CfirTowerDataElement =
    CfirTowerDataElement(scope = this, implicitReceiver = null, isLocal = isLocal)

/**
 * 将静态作用域包装为 tower data 元素。
 */
fun CfirScope.asTowerDataElementForStaticScope(staticScopeOwnerSymbol: CfirClassLikeSymbol<*>?): CfirTowerDataElement =
    CfirTowerDataElement(scope = this, implicitReceiver = null, isLocal = false, staticScopeOwnerSymbol = staticScopeOwnerSymbol)

/**
 * class-like 声明参与 tower resolve 的元素集合。
 */
class CfirTowerElementsForClass(
    /**
     * 当前类的 `this` 隐式接收者。
     */
    val thisReceiver: ImplicitReceiverValue<*>,
    /**
     * 当前类的静态作用域。
     */
    val staticScope: CfirScope?,
    /**
     * 父类链上的静态作用域元素。
     */
    val superClassesStaticScopes: List<CfirTowerDataElement>,
)

/**
 * 收集 class-like 声明进入 body resolve 时需要加入 tower 的元素。
 */
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
        // 类体中的静态 scope 只是未限定 static 成员的词法入口，不能重新启用
        // 当前类型的 extend 成员图；否则 receiver 的 BODY_LOOKUP 过滤会被此 tower level 绕过。
        staticScope = owner.staticScope(
            session,
            scopeSession,
            defaultType,
            CfirClassMemberScopeKind.BODY_LOOKUP,
        ),
        superClassesStaticScopes = superClassesStatics.toList().asReversed(),
    )
}

/**
 * 使用 holder 中的会话和作用域会话获取 class-like 静态作用域。
 */
fun CfirClassLikeDeclaration.staticScope(sessionHolder: SessionAndScopeSessionHolder): CfirContainingNamesAwareScope? =
    staticScope(sessionHolder.session, sessionHolder.scopeSession)

/**
 * 获取 class-like 声明的静态作用域。
 */
fun CfirClassLikeDeclaration.staticScope(
    session: CfirSession,
    scopeSession: ScopeSession,
    qualifierType: ConeCangJieType? = null,
    memberScopeKind: CfirClassMemberScopeKind = CfirClassMemberScopeKind.USE_SITE,
): CfirContainingNamesAwareScope? {
    val symbol = symbol as? CfirClassLikeSymbol<*> ?: return null
    return symbol.staticScopeForQualifierType(
        session,
        scopeSession,
        qualifierType ?: symbol.constructType(),
        memberScopeKind,
    )
}

/**
 * 返回 class-like 声明在 tower 中暴露的类型参数。
 */
fun CfirClassLikeDeclaration.typeParametersForTower(): List<CfirTypeParameter> = when (this) {
    is CfirClass -> typeParameters
    is org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration -> emptyList()
    is CfirInterface -> typeParameters
    is CfirStruct -> typeParameters
    is CfirEnum -> typeParameters
    else -> emptyList()
}

/**
 * 递归收集父类链上的静态作用域。
 */
private fun collectSuperClassesStaticScopes(
    owner: CfirClassLikeDeclaration,
    session: CfirSession,
    scopeSession: ScopeSession,
    result: MutableSet<CfirTowerDataElement>,
    visited: MutableSet<CfirClassLikeSymbol<*>>,
) {
    for (superTypeRef in owner.superTypeRefs) {
        val supertype = superTypeRef
            .classifyDeclaredSupertype(session)
            .scopeTraversalTypeOrNull()
            ?: continue
        val classId = supertype.classId

        val superSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: continue
        if (!visited.add(superSymbol)) continue

        val superDeclaration = superSymbol.cfir
        if (superDeclaration !is CfirClassLikeDeclaration || superDeclaration is CfirInterface) continue

        // 父类静态 scope 也属于当前类型本体的词法查找链。实例 extend 成员只能由
        // dispatch receiver 的父成员图引入，不能经 static scope 回退重新进入候选集。
        superDeclaration.staticScope(
            session,
            scopeSession,
            supertype,
            CfirClassMemberScopeKind.BODY_LOOKUP,
        )
            ?.asTowerDataElementForStaticScope(superSymbol)
            ?.let(result::add)

        collectSuperClassesStaticScopes(superDeclaration, session, scopeSession, result, visited)
    }
}
