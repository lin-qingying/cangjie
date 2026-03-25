package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.SessionAndScopeSessionHolder
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.cfir.calls.ImplicitDispatchReceiverValue
import org.cangnova.cangjie.cfir.calls.ImplicitReceiverValue
import org.cangnova.cangjie.cfir.calls.ImplicitValue
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirLocalScopes
import org.cangnova.cangjie.cfir.resolve.ImplicitValueMapper
import org.cangnova.cangjie.cfir.resolve.ImplicitValueStorage
import org.cangnova.cangjie.cfir.resolve.LocalVariableScopeStorage
import org.cangnova.cangjie.cfir.scopes.CfirContainingNamesAwareScope
import org.cangnova.cangjie.cfir.scopes.CfirLocalScope
import org.cangnova.cangjie.cfir.scopes.CfirScope
import org.cangnova.cangjie.cfir.scopes.CfirTypeScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassStaticScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScopeImpl
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeStubType
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

    fun addLocalVariable(name: Name, symbol: CfirCallableSymbol<*>): CfirTowerDataContext {
        val oldLastScope = localScopes.lastOrNull() as? CfirLocalScopeImpl ?: return this
        val indexOfLastLocalScope = towerDataElements.indexOfLast { it.scope === oldLastScope }
        if (indexOfLastLocalScope < 0) return this

        val newLastScope = oldLastScope.withVariable(name, symbol)
        return copy(
            towerDataElements = towerDataElements.set(indexOfLastLocalScope, newLastScope.asTowerDataElement(isLocal = true)),
            localScopes = localScopes.set(localScopes.lastIndex, newLastScope),
            localVariableScopeStorage = localVariableScopeStorage.addLocalVariable(symbol),
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

            override fun <S : org.cangnova.cangjie.cfir.symbols.CfirSymbol<*>, T : ImplicitValue<S>> invoke(value: T): T {
                @Suppress("UNCHECKED_CAST")
                return implicitValueCache.getOrPut(value) { value.createSnapshot(keepMutable) } as T
            }
        }

        return copy(
            towerDataElements = towerDataElements.map { it.createSnapshot(keepMutable, implicitValueMapper) }.toPersistentList(),
            implicitValueStorage = implicitValueStorage.createSnapshot(implicitValueMapper),
            localScopes = localScopes.map {
                if (it is CfirLocalScopeImpl) it.snapshot() else it
            }.toPersistentList(),
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
        staticScope = owner.staticScope(session, scopeSession),
        superClassesStaticScopes = superClassesStatics.toList().asReversed(),
    )
}

fun CfirClassLikeDeclaration.staticScope(sessionHolder: SessionAndScopeSessionHolder): CfirContainingNamesAwareScope? =
    staticScope(sessionHolder.session, sessionHolder.scopeSession)

fun CfirClassLikeDeclaration.staticScope(
    session: CfirSession,
    scopeSession: ScopeSession,
): CfirContainingNamesAwareScope? {
    val symbol = symbol as? CfirClassLikeSymbol<*> ?: return null
    val key = "static:" + symbol.classId.asString()
    return scopeSession.getOrBuild(key, ClassStaticScopeKey) {
        CfirClassStaticScope(this)
    }
}

fun CfirClassLikeDeclaration.typeParametersForTower(): List<CfirTypeParameter> = when (this) {
    is CfirClass -> typeParameters
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

        superDeclaration.staticScope(session, scopeSession)
            ?.asTowerDataElementForStaticScope(superSymbol)
            ?.let(result::add)

        collectSuperClassesStaticScopes(superDeclaration, session, scopeSession, result, visited)
    }
}

private object ClassStaticScopeKey : org.cangnova.cangjie.cfir.ScopeSessionKey<String, CfirContainingNamesAwareScope>()
