package org.cangnova.cangjie.cfir.calls

import org.cangnova.cangjie.cfir.CfirResolvable
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.InaccessibleReceiverKind
import org.cangnova.cangjie.cfir.expressions.buildInaccessibleReceiverExpression
import org.cangnova.cangjie.cfir.expressions.buildThisReceiverExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.references.buildImplicitThisReference
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassStaticScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.symbols.CfirThisOwnerSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
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
        get() = receiverExpression.coneTypeOrNull
            ?: error("Receiver expression '${receiverExpression::class.simpleName}' has no resolved type")

    override fun scope(c: SessionAndScopeSessionHolder): CfirScope? =
        receiverExpression.qualifierScopeOrNull(c.session)
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

val ImplicitReceiverValue<*>.referencedMemberSymbol: CfirSymbol<*>
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
    val classId = type.classIdOrPrimitiveClassId ?: return null
    val symbol = session.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
    val declaration = symbol.cfir

    return when (declaration) {
        is CfirClass -> session.cangjieScopeProvider.getUseSiteMemberScope(declaration, session, scopeSession)
        is CfirExtend -> CfirClassUseSiteMemberScope(symbol, session.symbolProvider, session.extendProvider)
        else -> CfirClassUseSiteMemberScope(symbol, session.symbolProvider, session.extendProvider)
    }
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

fun CfirExpression.qualifierScopeOrNull(
    session: CfirSession,
): CfirScope? {
    val classifier = resolvedQualifierClassifier(session) ?: return null
    val declaration = classifier.cfir as? CfirClassLikeDeclaration ?: return null
    return CfirClassStaticScope(declaration)
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
