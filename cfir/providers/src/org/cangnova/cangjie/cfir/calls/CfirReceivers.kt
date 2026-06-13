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
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirCompositeTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.staticScopeForQualifierType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.fakeElement

sealed interface ReceiverValue {
    val type: ConeCangJieType

    val receiverExpression: CfirExpression

    fun scope(c: SessionAndScopeSessionHolder): CfirScope?
}

    class ExpressionReceiverValue(
    override val receiverExpression: CfirExpression,
) : ReceiverValue {
    override val type: ConeCangJieType
        get() = receiverExpression.resolvedType
    override fun scope(c: SessionAndScopeSessionHolder): CfirScope? =
        receiverExpression.qualifierScopeOrNull(c.session, c.scopeSession)
            ?: typeToScope(c.session, c.scopeSession, type)
}

sealed class ImplicitReceiverValue<S : CfirThisOwnerSymbol<*>>(
    override val boundSymbol: S,
    type: ConeCangJieType,
    originalType: ConeCangJieType,
    override val session: CfirSession,
    override val scopeSession: ScopeSession,
    mutable: Boolean,
    private val inaccessibleReceiverKind: InaccessibleReceiverKind? = null,
) : ImplicitValue<S>(type, originalType, mutable), ReceiverValue, SessionAndScopeSessionHolder {

    val implicitScope: CfirTypeScope?
        get() = lazyImplicitScope.value

    private var lazyImplicitScope: Lazy<CfirTypeScope?> = lazy(LazyThreadSafetyMode.PUBLICATION) {
        typeToScope(session, scopeSession, type)
    }

    override fun computeOriginalExpression(): CfirExpression =
        receiverExpression(boundSymbol, originalType, inaccessibleReceiverKind)

    override fun scope(c: SessionAndScopeSessionHolder): CfirScope? = implicitScope

    final override val receiverExpression: CfirExpression
        get() = computeExpression()

    @ImplicitValue.ImplicitValueInternals
    override fun updateTypeFromSmartcast(type: ConeCangJieType) {
        super.updateTypeFromSmartcast(type)
        lazyImplicitScope = lazy(LazyThreadSafetyMode.PUBLICATION) {
            typeToScope(session, scopeSession, type)
        }
    }

    abstract override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<S>

    abstract fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): ImplicitReceiverValue<S>
}

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

class ImplicitDispatchReceiverValue private constructor(
    boundSymbol: CfirClassLikeSymbol<*>,
    type: ConeCangJieType,
    originalType: ConeCangJieType,
    useSiteSession: CfirSession,
    scopeSession: ScopeSession,
    mutable: Boolean,
) : ImplicitReceiverValue<CfirClassLikeSymbol<*>>(boundSymbol, type, originalType, useSiteSession, scopeSession, mutable) {
    constructor(
        boundSymbol: CfirClassLikeSymbol<*>,
        type: ConeCangJieType = boundSymbol.constructType(),
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ) : this(boundSymbol, type, originalType = type, useSiteSession, scopeSession, mutable = true)

    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<CfirClassLikeSymbol<*>> =
        ImplicitDispatchReceiverValue(boundSymbol, type, originalType, session, scopeSession, keepMutable)

    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): ImplicitDispatchReceiverValue =
        ImplicitDispatchReceiverValue(boundSymbol, type, originalType, newSession, newScopeSession, mutable)
}

class ImplicitExtensionReceiverValue private constructor(
    boundSymbol: CfirExtendSymbol,
    type: ConeCangJieType,
    originalType: ConeCangJieType,
    useSiteSession: CfirSession,
    scopeSession: ScopeSession,
    mutable: Boolean,
) : ImplicitReceiverValue<CfirExtendSymbol>(boundSymbol, type, originalType, useSiteSession, scopeSession, mutable) {
    constructor(
        boundSymbol: CfirExtendSymbol,
        type: ConeCangJieType,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ) : this(boundSymbol, type, originalType = type, useSiteSession, scopeSession, mutable = true)

    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<CfirExtendSymbol> =
        ImplicitExtensionReceiverValue(boundSymbol, type, originalType, session, scopeSession, keepMutable)

    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): ImplicitExtensionReceiverValue =
        ImplicitExtensionReceiverValue(boundSymbol, type, originalType, newSession, newScopeSession, mutable)
}

class InaccessibleImplicitReceiverValue private constructor(
    boundSymbol: CfirClassLikeSymbol<*>,
    type: ConeCangJieType,
    originalType: ConeCangJieType,
    useSiteSession: CfirSession,
    scopeSession: ScopeSession,
    mutable: Boolean,
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
    constructor(
        boundSymbol: CfirClassLikeSymbol<*>,
        type: ConeCangJieType,
        kind: InaccessibleReceiverKind,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ) : this(boundSymbol, type, originalType = type, useSiteSession, scopeSession, mutable = true, kind = kind)

    override fun createSnapshot(keepMutable: Boolean): ImplicitReceiverValue<CfirClassLikeSymbol<*>> =
        InaccessibleImplicitReceiverValue(boundSymbol, type, originalType, session, scopeSession, keepMutable, kind)

    override fun withReplacedSessionOrNull(newSession: CfirSession, newScopeSession: ScopeSession): InaccessibleImplicitReceiverValue =
        InaccessibleImplicitReceiverValue(boundSymbol, type, originalType, newSession, newScopeSession, mutable, kind)
}

val ImplicitReceiverValue<*>.referencedMemberSymbol: CfirBasedSymbol<*>
    get() = when (val boundSymbol = boundSymbol) {
        is CfirExtendSymbol -> boundSymbol
        else -> boundSymbol
    }

fun ImplicitReceiverValue<*>.producesInapplicableCandidate(): Boolean =
    this is InaccessibleImplicitReceiverValue && !kind.producesApplicableCandidate

private fun typeToScope(
    session: CfirSession,
    scopeSession: ScopeSession,
    type: ConeCangJieType,
): CfirTypeScope? {
    val scopes = linkedSetOf<CfirTypeScope>()
    collectTypeScopes(session, scopeSession, type, scopes, linkedSetOf(), linkedSetOf())
    return when (scopes.size) {
        0 -> null
        1 -> scopes.single()
        else -> CfirCompositeTypeScope(scopes.toList())
    }
}

private fun collectTypeScopes(
    session: CfirSession,
    scopeSession: ScopeSession,
    type: ConeCangJieType,
    destination: MutableSet<CfirTypeScope>,
    visitedClassIds: MutableSet<org.cangnova.cangjie.name.ClassId>,
    visitedTypeParameters: MutableSet<ConeTypeParameterLookupTag>,
) {
    when (type) {
        is ConeTypeVariableType -> {
            val originalTypeParameter = type.typeConstructor.originalTypeParameter as? ConeTypeParameterLookupTag ?: return
            collectTypeParameterBoundsScopes(
                session,
                scopeSession,
                originalTypeParameter,
                destination,
                visitedClassIds,
                visitedTypeParameters,
            )
        }

        is org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType -> {
            collectTypeParameterBoundsScopes(
                session,
                scopeSession,
                type.lookupTag,
                destination,
                visitedClassIds,
                visitedTypeParameters,
            )
        }

        is ConeIntersectionType -> {
            type.intersectedTypes.forEach {
                collectTypeScopes(session, scopeSession, it, destination, visitedClassIds, visitedTypeParameters)
            }
        }

        else -> {
            val classId = type.classIdOrPrimitiveClassId ?: return
            if (!visitedClassIds.add(classId)) return
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
                )
                is CfirExtend -> CfirClassUseSiteMemberScope(
                    session,
                    symbol,
                    session.symbolProvider,
                    session.extendProvider,
                    session.directSupertypeProviderOrNull,
                    ownerType = type,
                )
                else -> CfirClassUseSiteMemberScope(
                    session,
                    symbol,
                    session.symbolProvider,
                    session.extendProvider,
                    session.directSupertypeProviderOrNull,
                    ownerType = type,
                )
            }
            destination += CfirClassSubstitutionScope(session, rawScope, type)
        }
    }
}

private fun collectTypeParameterBoundsScopes(
    session: CfirSession,
    scopeSession: ScopeSession,
    lookupTag: ConeTypeParameterLookupTag,
    destination: MutableSet<CfirTypeScope>,
    visitedClassIds: MutableSet<org.cangnova.cangjie.name.ClassId>,
    visitedTypeParameters: MutableSet<ConeTypeParameterLookupTag>,
) {
    if (!visitedTypeParameters.add(lookupTag)) return
    val typeParameterType = org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl(lookupTag)
    val bounds = collectTypeParameterUpperBounds(typeParameterType)
    bounds.forEach {
        collectTypeScopes(session, scopeSession, it, destination, visitedClassIds, visitedTypeParameters)
    }
}

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

private fun ConeTypeParameterLookupTag.collectUpperBoundsTo(collect: (ConeCangJieType) -> Unit) {
    typeParameterSymbol.lazyResolveToPhase(org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.TYPES)
    typeParameterSymbol.resolvedBounds.map { it.coneType }.forEach(collect)
}

fun CfirExpression.resolvedQualifierClassifier(session: CfirSession): CfirClassLikeSymbol<*>? {
    val resolvedSymbol = ((this as? CfirResolvable)?.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol
        ?: return null
    return when (resolvedSymbol) {
        is CfirTypeAliasSymbol -> resolvedSymbol.expandedClassLikeSymbol(session)
        is CfirClassLikeSymbol<*> -> resolvedSymbol
        else -> null
    }
}

fun CfirExpression.resolvedQualifierTypeParameter(): CfirTypeParameterSymbol? {
    val resolvedSymbol = ((this as? CfirResolvable)?.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol
        ?: return null
    return resolvedSymbol as? CfirTypeParameterSymbol
}

fun CfirExpression.qualifierScopeOrNull(
    session: CfirSession,
    scopeSession: ScopeSession,
): CfirScope? {
    resolvedQualifierClassifier(session)?.let { classifier ->
        val qualifierType = coneTypeOrNull ?: classifier.constructType()
        return classifier.staticScopeForQualifierType(session, scopeSession, qualifierType)
    }

    val typeParameter = resolvedQualifierTypeParameter() ?: return null
    return typeParameter.staticScopeForQualifierType(session, scopeSession)
}

private fun CfirTypeAliasSymbol.expandedClassLikeSymbol(session: CfirSession): CfirClassLikeSymbol<*>? {
    if (!isBound) return null
    val expandedType = (cfir as? CfirTypeAlias)?.expandedTypeRef?.coneTypeOrNull ?: return null
    val classId = when (expandedType) {
        is ConeClassLikeType -> expandedType.classId
        is ConeStructType -> expandedType.classId
        is ConeEnumType -> expandedType.classId
        is ConeTypeAliasType -> expandedType.expandedType?.let { nested ->
            when (nested) {
                is ConeClassLikeType -> nested.classId
                is ConeStructType -> nested.classId
                is ConeEnumType -> nested.classId
                else -> expandedType.classId
            }
        } ?: expandedType.classId
        else -> return null
    }
    return session.symbolProvider.getClassLikeSymbolByClassId(classId)
}
