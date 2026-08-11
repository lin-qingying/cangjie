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

package org.cangnova.cangjie.cfir.calls

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.buildImplicitThisReference
import org.cangnova.cangjie.cfir.scopes.CfirCompositeScope
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirCompositeTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirExtendMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirUnionTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.staticScopeForBuiltinQualifierType
import org.cangnova.cangjie.cfir.scopes.impl.staticScopeForQualifierType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.fakeElement

/**
 * 调用解析中的 receiver 值抽象。
 */
sealed interface ReceiverValue {
    /**
     * receiver 当前参与成员查找的类型。
     */
    val type: ConeCangJieType

    /**
     * receiver 在 CFIR 表达式树中的表达。
     */
    val receiverExpression: CfirExpression

    /**
     * 返回该 receiver 可用于成员查找的 scope。
     */
    fun scope(c: SessionAndScopeSessionHolder): CfirScope?
}

/**
 * 显式表达式 receiver。
 *
 * @property receiverExpression 原始表达式 receiver。
 */
class ExpressionReceiverValue(
    /**
     * 调用点显式写出的 receiver 表达式，后续类型与 qualifier scope 都从该表达式读取。
     */
    override val receiverExpression: CfirExpression,
) : ReceiverValue {
    /**
     * 显式 receiver 的类型直接来自表达式解析类型。
     */
    override val type: ConeCangJieType
        get() = receiverExpression.resolvedType

    /**
     * 优先返回 qualifier 静态 scope；否则按 receiver 类型构造成员 scope。
     */
    override fun scope(c: SessionAndScopeSessionHolder): CfirScope? =
        receiverExpression.qualifierScopeOrNull(c.session, c.scopeSession)
            ?: typeToScope(
                c.session,
                c.scopeSession,
                type,
                scopeKind = receiverExpression.memberScopeKind(),
            )
}

/**
 * 隐式 receiver 基类。
 *
 * 隐式 receiver 同时是 [ImplicitValue] 与 [SessionAndScopeSessionHolder]，负责在 smartcast 更新后
 * 重建可见成员 scope。
 *
 * @property boundSymbol 绑定的 this owner symbol。
 * @property session receiver 所属 use-site session。
 * @property scopeSession 成员 scope 缓存会话。
 */
sealed class ImplicitReceiverValue<S : CfirThisOwnerSymbol<*>>(
    /**
     * 隐式 receiver 绑定的 this owner symbol，用于构造 implicit-this 引用和成员归属。
     */
    override val boundSymbol: S,
    type: ConeCangJieType,
    originalType: ConeCangJieType,
    /**
     * 当前 receiver 参与查找时使用的 use-site session。
     */
    override val session: CfirSession,
    /**
     * 成员 scope 缓存所属的 scope session。
     */
    override val scopeSession: ScopeSession,
    mutable: Boolean,
    /**
     * 不可访问 receiver 的诊断分类；为 `null` 表示普通隐式 receiver。
     */
    private val inaccessibleReceiverKind: InaccessibleReceiverKind? = null,
) : ImplicitValue<S>(type, originalType, mutable), ReceiverValue, SessionAndScopeSessionHolder {

    /**
     * 隐式 receiver 成员查找使用的 class member scope 模式。
     */
    protected open val implicitMemberScopeKind: CfirClassMemberScopeKind = CfirClassMemberScopeKind.BODY_LOOKUP

    /**
     * 当前隐式 receiver 的成员查找 scope。
     */
    val implicitScope: CfirTypeScope?
        get() = lazyImplicitScope.value

    /**
     * 随 smartcast 类型变化而失效重建的 scope 缓存。
     */
    private var lazyImplicitScope: Lazy<CfirTypeScope?> = lazy(LazyThreadSafetyMode.PUBLICATION) {
        typeToScope(session, scopeSession, type, scopeKind = implicitMemberScopeKind)
    }

    /**
     * 构造隐式 this 或 inaccessible receiver 的原始表达式。
     */
    override fun computeOriginalExpression(): CfirExpression =
        receiverExpression(boundSymbol, originalType, inaccessibleReceiverKind)

    /**
     * 返回隐式 receiver 的成员 scope。
     */
    override fun scope(c: SessionAndScopeSessionHolder): CfirScope? = implicitScope

    /**
     * 当前 receiver 表达式，必要时包含 smartcast 包装。
     */
    final override val receiverExpression: CfirExpression
        get() = computeExpression()

    /**
     * 更新 smartcast 类型并重建成员 scope 缓存。
     */
    @ImplicitValue.ImplicitValueInternals
    override fun updateTypeFromSmartcast(type: ConeCangJieType) {
        super.updateTypeFromSmartcast(type)
        lazyImplicitScope = lazy(LazyThreadSafetyMode.PUBLICATION) {
            typeToScope(session, scopeSession, type, scopeKind = implicitMemberScopeKind)
        }
    }

    /**
     * 创建隐式 receiver 的快照。
     */
    abstract override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<S>

    /**
     * 在新 session/scopeSession 中复用该隐式 receiver。
     */
    abstract fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): ImplicitReceiverValue<S>
}

/**
 * 根据 symbol 与类型构造隐式 receiver 表达式。
 */
private fun receiverExpression(
    symbol: CfirThisOwnerSymbol<*>,
    type: ConeCangJieType,
    inaccessibleReceiverKind: InaccessibleReceiverKind?,
): CfirExpression {
    val calleeReference = buildImplicitThisReference {
        boundSymbol = symbol
    }
    val source = symbol.cfir.source?.fakeElement(CjFakeSourceElementKind.ImplicitThisReceiverExpression)

    return when (inaccessibleReceiverKind) {
        null -> buildThisReceiverExpression {
            this.source = source
            this.calleeReference = calleeReference
            this.coneTypeOrNull = type
        }

        else -> buildInaccessibleReceiverExpression {
            this.source = source
            this.calleeReference = calleeReference
            this.coneTypeOrNull = type
            this.kind = inaccessibleReceiverKind
        }
    }
}

/**
 * class/struct/enum/interface 的隐式 dispatch receiver。
 */
class ImplicitDispatchReceiverValue private constructor(
    boundSymbol: CfirClassLikeSymbol<*>,
    type: ConeCangJieType,
    originalType: ConeCangJieType,
    useSiteSession: CfirSession,
    scopeSession: ScopeSession,
    mutable: Boolean,
) : ImplicitReceiverValue<CfirClassLikeSymbol<*>>(boundSymbol, type, originalType, useSiteSession, scopeSession, mutable) {
    /**
     * 构造可变 dispatch receiver。
     */
    constructor(
        boundSymbol: CfirClassLikeSymbol<*>,
        type: ConeCangJieType = boundSymbol.constructType(),
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ) : this(boundSymbol, type, originalType = type, useSiteSession, scopeSession, mutable = true)

    /**
     * 创建 dispatch receiver 快照。
     */
    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<CfirClassLikeSymbol<*>> =
        ImplicitDispatchReceiverValue(boundSymbol, type, originalType, session, scopeSession, keepMutable)

    /**
     * 替换 use-site session 与 scope session。
     */
    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): ImplicitDispatchReceiverValue =
        ImplicitDispatchReceiverValue(boundSymbol, type, originalType, newSession, newScopeSession, mutable)
}

/**
 * extend 声明体内的隐式 extension receiver。
 */
class ImplicitExtensionReceiverValue private constructor(
    boundSymbol: CfirExtendSymbol,
    type: ConeCangJieType,
    originalType: ConeCangJieType,
    useSiteSession: CfirSession,
    scopeSession: ScopeSession,
    mutable: Boolean,
) : ImplicitReceiverValue<CfirExtendSymbol>(boundSymbol, type, originalType, useSiteSession, scopeSession, mutable) {
    /**
     * extend receiver 必须按 use-site 方式查找目标类型成员与 extend 成员。
     */
    override val implicitMemberScopeKind: CfirClassMemberScopeKind = CfirClassMemberScopeKind.USE_SITE

    /**
     * 构造可变 extension receiver。
     */
    constructor(
        boundSymbol: CfirExtendSymbol,
        type: ConeCangJieType,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ) : this(boundSymbol, type, originalType = type, useSiteSession, scopeSession, mutable = true)

    /**
     * 创建 extension receiver 快照。
     */
    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<CfirExtendSymbol> =
        ImplicitExtensionReceiverValue(boundSymbol, type, originalType, session, scopeSession, keepMutable)

    /**
     * 替换 use-site session 与 scope session。
     */
    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): ImplicitExtensionReceiverValue =
        ImplicitExtensionReceiverValue(boundSymbol, type, originalType, newSession, newScopeSession, mutable)
}

/**
 * 对当前调用不可访问但仍需参与候选诊断建模的隐式 receiver。
 *
 * @property kind 不可访问原因。
 */
class InaccessibleImplicitReceiverValue private constructor(
    boundSymbol: CfirClassLikeSymbol<*>,
    type: ConeCangJieType,
    originalType: ConeCangJieType,
    useSiteSession: CfirSession,
    scopeSession: ScopeSession,
    mutable: Boolean,
    /**
     * 当前 receiver 不可访问的具体原因，调用候选诊断会据此决定适用性与错误文本。
     */
    val kind: InaccessibleReceiverKind,
) : ImplicitReceiverValue<CfirClassLikeSymbol<*>>(
    boundSymbol = boundSymbol,
    type = type,
    originalType = originalType,
    session = useSiteSession,
    scopeSession = scopeSession,
    mutable = mutable,
    inaccessibleReceiverKind = kind,
) {
    /**
     * 构造可变 inaccessible receiver。
     */
    constructor(
        boundSymbol: CfirClassLikeSymbol<*>,
        type: ConeCangJieType,
        kind: InaccessibleReceiverKind,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ) : this(boundSymbol, type, originalType = type, useSiteSession, scopeSession, mutable = true, kind = kind)

    /**
     * 创建 inaccessible receiver 快照。
     */
    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<CfirClassLikeSymbol<*>> =
        InaccessibleImplicitReceiverValue(boundSymbol, type, originalType, session, scopeSession, keepMutable, kind)

    /**
     * 替换 use-site session 与 scope session。
     */
    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): InaccessibleImplicitReceiverValue =
        InaccessibleImplicitReceiverValue(boundSymbol, type, originalType, newSession, newScopeSession, mutable, kind)
}

/**
 * 返回隐式 receiver 对外代表的成员 symbol。
 */
val ImplicitReceiverValue<*>.referencedMemberSymbol: CfirBasedSymbol<*>
    get() = when (val boundSymbol = boundSymbol) {
        is CfirExtendSymbol -> boundSymbol
        else -> boundSymbol
    }

/**
 * 判断不可访问隐式 receiver 是否会产生不可适用候选。
 */
fun ImplicitReceiverValue<*>.producesInapplicableCandidate(): Boolean =
    this is InaccessibleImplicitReceiverValue && !kind.producesApplicableCandidate

/**
 * 为显式 receiver 表达式选择成员 scope 查找模式。
 */
private fun CfirExpression.memberScopeKind(): CfirClassMemberScopeKind = when (this) {
    // 官方查找在原类型本体的 this/super 路径关闭 extend；extend 体内的 this 仍以 extend 声明为当前 owner。
    is CfirThisReceiverExpression -> if (calleeReference.boundSymbol is CfirExtendSymbol) {
        CfirClassMemberScopeKind.USE_SITE
    } else {
        CfirClassMemberScopeKind.BODY_LOOKUP
    }

    is CfirSuperReceiverExpression -> CfirClassMemberScopeKind.USE_SITE
    else -> CfirClassMemberScopeKind.USE_SITE
}

/**
 * 将 receiver 类型转换为可查找成员的类型 scope。
 */
private fun typeToScope(
    session: CfirSession,
    scopeSession: ScopeSession,
    type: ConeCangJieType,
    scopeKind: CfirClassMemberScopeKind,
): CfirTypeScope? {
    val scopes = linkedSetOf<CfirTypeScope>()
    collectTypeScopes(session, scopeSession, type, scopeKind, scopes, linkedSetOf(), linkedSetOf())
    return when (scopes.size) {
        0 -> null
        1 -> scopes.single()
        else -> if (type.requiresPrimitiveExtendUnionScope()) {
            CfirUnionTypeScope(scopes.toList())
        } else {
            CfirCompositeTypeScope(scopes.toList(), session)
        }
    }
}

/**
 * 判断 ideal primitive receiver 是否需要使用 union scope 合并多个候选 primitive scope。
 */
private fun ConeCangJieType.requiresPrimitiveExtendUnionScope(): Boolean =
    this is ConeIdealLiteralType || this is ConePrimitiveType && kind.isIdeal

/**
 * 递归收集 [type] 可见的成员 scope。
 *
 * 该函数统一处理类型变量上界、交叉类型、ideal literal、primitive extend 和普通 class-like 类型。
 */
private fun collectTypeScopes(
    session: CfirSession,
    scopeSession: ScopeSession,
    type: ConeCangJieType,
    scopeKind: CfirClassMemberScopeKind,
    destination: MutableSet<CfirTypeScope>,
    visitedTypes: MutableSet<ConeCangJieType>,
    visitedTypeParameters: MutableSet<ConeTypeParameterLookupTag>,
) {
    when (type) {
        is ConeTypeVariableType -> {
            val originalTypeParameter = type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag ?: return
            collectTypeParameterBoundsScopes(
                session,
                scopeSession,
                originalTypeParameter,
                scopeKind,
                destination,
                visitedTypes,
                visitedTypeParameters,
            )
        }

        is org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType -> {
            collectTypeParameterBoundsScopes(
                session,
                scopeSession,
                type.lookupTag,
                scopeKind,
                destination,
                visitedTypes,
                visitedTypeParameters,
            )
        }

        is ConeIntersectionType -> {
            type.intersectedTypes.forEach {
                collectTypeScopes(
                    session,
                    scopeSession,
                    it,
                    scopeKind,
                    destination,
                    visitedTypes,
                    visitedTypeParameters,
                )
            }
        }

        is ConeIdealLiteralType -> {
            collectIdealPrimitiveTypeScopes(
                session,
                scopeSession,
                type,
                scopeKind,
                destination,
                visitedTypes,
                visitedTypeParameters,
            )
        }

        is ConePrimitiveType if type.kind.isIdeal -> {
            collectIdealPrimitiveTypeScopes(
                session,
                scopeSession,
                type,
                scopeKind,
                destination,
                visitedTypes,
                visitedTypeParameters,
            )
        }

        else -> {
            if (collectNonClassBuiltinExtendScopes(
                    session = session,
                    scopeSession = scopeSession,
                    type = type,
                    scopeKind = scopeKind,
                    destination = destination,
                    visitedTypes = visitedTypes,
                    visitedTypeParameters = visitedTypeParameters,
                )
            ) {
                return
            }

            val classId = type.classIdOrPrimitiveClassId ?: return
            // 同一个泛型声明的不同实例拥有不同的成员签名，不能只按 ClassId 去重。
            // 以完整 Cone 类型作为访问键，同时仍可阻断真正相同类型形成的递归父类型环。
            if (!visitedTypes.add(type)) return
            val symbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return
            val declaration = symbol.cfir
            val rawScope = when (declaration) {
                is CfirClass -> CfirClassUseSiteMemberScope(
                    session,
                    symbol,
                    session.symbolProvider,
                    session.extendProvider,
                    session.directSupertypeProviderOrNull,
                    ownerType = type,
                    scopeKind = scopeKind,
                )
                is CfirExtend -> CfirClassUseSiteMemberScope(
                    session,
                    symbol,
                    session.symbolProvider,
                    session.extendProvider,
                    session.directSupertypeProviderOrNull,
                    ownerType = type,
                    scopeKind = scopeKind,
                )
                else -> CfirClassUseSiteMemberScope(
                    session,
                    symbol,
                    session.symbolProvider,
                    session.extendProvider,
                    session.directSupertypeProviderOrNull,
                    ownerType = type,
                    scopeKind = scopeKind,
                )
            }
            destination += CfirClassSubstitutionScope(session, rawScope, type)
        }
    }
}

/**
 * 收集非 class builtin 或特殊类型上的 extend scope。
 *
 * 返回 `true` 表示当前类型已经由该分支完整处理，调用方不应继续按 class id 构造成员 scope。
 */
private fun collectNonClassBuiltinExtendScopes(
    session: CfirSession,
    scopeSession: ScopeSession,
    type: ConeCangJieType,
    scopeKind: CfirClassMemberScopeKind,
    destination: MutableSet<CfirTypeScope>,
    visitedTypes: MutableSet<ConeCangJieType>,
    visitedTypeParameters: MutableSet<ConeTypeParameterLookupTag>,
): Boolean {
    val targetKey = type.expandedExtendTargetKey ?: return false
    if (targetKey.classIdOrNull != null) return false

    if (scopeKind == CfirClassMemberScopeKind.USE_SITE) {
        destination += CfirExtendMemberScope(
            targetKey = targetKey,
            extendProvider = session.extendProvider,
            session = session,
            receiverType = type,
        )
    }

    val directSupertypes = if (scopeKind == CfirClassMemberScopeKind.DECLARATION_SITE) {
        emptyList()
    } else {
        session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(type).orEmpty()
    }
    for (supertype in directSupertypes) {
        collectTypeScopes(
            session,
            scopeSession,
            supertype,
            scopeKind,
            destination,
            visitedTypes,
            visitedTypeParameters,
        )
    }
    return true
}

/**
 * 将 ideal primitive 类型展开为所有可能的 primitive lookup 类型并收集 scope。
 */
private fun collectIdealPrimitiveTypeScopes(
    session: CfirSession,
    scopeSession: ScopeSession,
    type: ConeCangJieType,
    scopeKind: CfirClassMemberScopeKind,
    destination: MutableSet<CfirTypeScope>,
    visitedTypes: MutableSet<ConeCangJieType>,
    visitedTypeParameters: MutableSet<ConeTypeParameterLookupTag>,
) {
    for (primitiveType in type.idealExtendLookupTypes) {
        collectTypeScopes(
            session,
            scopeSession,
            primitiveType,
            scopeKind,
            destination,
            visitedTypes,
            visitedTypeParameters,
        )
    }
}

/**
 * 收集类型参数上界对应的成员 scope。
 */
private fun collectTypeParameterBoundsScopes(
    session: CfirSession,
    scopeSession: ScopeSession,
    lookupTag: ConeTypeParameterLookupTag,
    scopeKind: CfirClassMemberScopeKind,
    destination: MutableSet<CfirTypeScope>,
    visitedTypes: MutableSet<ConeCangJieType>,
    visitedTypeParameters: MutableSet<ConeTypeParameterLookupTag>,
) {
    if (!visitedTypeParameters.add(lookupTag)) return
    val typeParameterType = org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl(lookupTag)
    if (typeParameterType.hasInvalidDeclaredUpperBounds(session)) return
    val bounds = collectTypeParameterUpperBounds(typeParameterType)
    bounds.forEach {
        collectTypeScopes(
            session,
            scopeSession,
            it,
            scopeKind,
            destination,
            visitedTypes,
            visitedTypeParameters,
        )
    }
}

/**
 * 收集类型参数类型的非错误、非类型参数递归上界。
 */
private fun collectTypeParameterUpperBounds(
    typeParameterType: org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType,
): Set<ConeCangJieType> {
    val upperBounds = linkedSetOf<ConeCangJieType>()
    val seen = linkedSetOf<ConeCangJieType>()

    fun collect(type: ConeCangJieType) {
        if (!seen.add(type)) return

        when (type) {
            is ConeErrorType -> Unit
            is org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType -> {
                type.lookupTag.collectUpperBoundsTo(::collect)
            }
            is ConeTypeVariableType -> {
                val originalTypeParameter = type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag ?: return
                originalTypeParameter.collectUpperBoundsTo(::collect)
            }
            is ConeIntersectionType -> type.intersectedTypes.forEach(::collect)
            else -> upperBounds += type
        }
    }

    collect(typeParameterType)
    return upperBounds
}

/**
 * 将 lookup tag 解析到 TYPES 阶段并把所有 resolved upper bound 交给 [collect]。
 */
private fun ConeTypeParameterLookupTag.collectUpperBoundsTo(collect: (ConeCangJieType) -> Unit) {
    declaredUpperBoundRefsAfterTypeResolve()
        .mapNotNull { it.declaredUpperBoundConeTypeOrNull() }
        .filterNot { it is ConeErrorType }
        .forEach(collect)
}

/**
 * 返回 qualifier 表达式解析出的 class-like symbol。
 */
fun CfirExpression.resolvedQualifierClassifier(session: CfirSession): CfirClassLikeSymbol<*>? {
    return resolvedQualifierSymbol(session) as? CfirClassLikeSymbol<*>
}

/**
 * 返回 qualifier 表达式解析出的 classifier symbol。
 *
 * typealias qualifier 会展开到最终 class-like symbol。
 */
fun CfirExpression.resolvedQualifierSymbol(session: CfirSession): CfirClassifierSymbol<*>? {
    val resolvedSymbol = ((this as? CfirResolvable)?.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol
        ?: return null
    return when (resolvedSymbol) {
        is CfirTypeAliasSymbol -> resolvedSymbol.expandedClassLikeSymbol(session)
        is CfirClassifierSymbol<*> -> resolvedSymbol
        else -> null
    }
}

/**
 * 判断表达式是否已经解析为类型限定符，而不是运行时值接收者。
 *
 * CFIR 当前复用 qualified-access 表达 class-like、typealias 与类型参数限定符；所有后续阶段
 * 必须通过同一个符号分类入口区分类型限定符和值接收者，不能再从表达式类型反向猜测角色。
 */
fun CfirExpression.isResolvedTypeQualifier(session: CfirSession): Boolean =
    resolvedQualifierSymbol(session) != null

/**
 * 返回 qualifier 表达式解析出的类型参数 symbol。
 */
fun CfirExpression.resolvedQualifierTypeParameter(): CfirTypeParameterSymbol? {
    val resolvedSymbol = ((this as? CfirResolvable)?.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol
        ?: return null
    return resolvedSymbol as? CfirTypeParameterSymbol
}

/**
 * 返回 qualifier 表达式可用于静态成员查找的 scope。
 */
fun CfirExpression.qualifierScopeOrNull(
    session: CfirSession,
    scopeSession: ScopeSession,
): CfirScope? {
    resolvedQualifierClassifier(session)?.let { classifier ->
        val qualifierType = coneTypeOrNull ?: classifier.constructType()
        return classifier.staticScopeForQualifierType(session, scopeSession, qualifierType)
    }

    coneTypeOrNull?.staticScopeForBuiltinQualifierType(session, scopeSession)?.let { return it }

    val typeParameter = resolvedQualifierTypeParameter() ?: return null
    return typeParameter.staticScopeForQualifierType(session, scopeSession)
}

/**
 * 将 typealias symbol 展开到其底层 class-like symbol。
 */
private fun CfirTypeAliasSymbol.expandedClassLikeSymbol(session: CfirSession): CfirClassLikeSymbol<*>? {
    if (!isBound) return null
    val expandedType = (cfir as? CfirTypeAlias)?.expandedTypeRef?.coneTypeOrNull ?: return null
    val classId = expandedType.expandedClassIdOrPrimitiveClassId ?: return null
    return session.symbolProvider.getClassLikeSymbolByClassId(classId)
}
