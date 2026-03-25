package org.cangnova.cangjie.cfir.resolve

import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentHashMapOf
import kotlinx.collections.immutable.persistentHashSetOf
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.collections.immutable.toPersistentMap
import org.cangnova.cangjie.cfir.calls.ImplicitDispatchReceiverValue
import org.cangnova.cangjie.cfir.calls.ImplicitReceiverValue
import org.cangnova.cangjie.cfir.calls.ImplicitValue
import org.cangnova.cangjie.cfir.calls.producesInapplicableCandidate
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name

class ImplicitValueStorage private constructor(
    private val implicitReceiverStack: PersistentList<ImplicitReceiverValue<*>>,
    private val implicitReceiversByLabel: PersistentMap<Name, PersistentSet<ImplicitReceiverValue<*>>>,
    private val implicitValuesBySymbol: PersistentMap<CfirSymbol<*>, ImplicitValue<*>>,
) {
    constructor() : this(
        persistentListOf(),
        persistentHashMapOf(),
        persistentHashMapOf(),
    )

    val implicitReceivers: List<ImplicitReceiverValue<*>>
        get() = implicitReceiverStack

    val implicitValues: Collection<ImplicitValue<*>>
        get() = implicitValuesBySymbol.values

    fun addAllImplicitReceivers(receivers: List<ImplicitReceiverValue<*>>): ImplicitValueStorage {
        return receivers.fold(this) { acc, receiver ->
            acc.addImplicitReceiver(name = null, value = receiver)
        }
    }

    fun addImplicitReceiver(name: Name?, value: ImplicitReceiverValue<*>): ImplicitValueStorage {
        val updatedReceiversByLabel = if (name != null) {
            val receivers = implicitReceiversByLabel[name] ?: persistentHashSetOf()
            implicitReceiversByLabel.put(name, receivers.add(value))
        } else {
            implicitReceiversByLabel
        }

        return ImplicitValueStorage(
            implicitReceiverStack = implicitReceiverStack.add(value),
            implicitReceiversByLabel = updatedReceiversByLabel,
            implicitValuesBySymbol = implicitValuesBySymbol.put(value.boundSymbol, value),
        )
    }

    operator fun get(name: String?): Set<ImplicitReceiverValue<*>> {
        if (name == null) {
            val best = implicitReceiverStack.lastOrNull { !it.producesInapplicableCandidate() }
                ?: implicitReceiverStack.lastOrNull()
            return best?.let(::setOf).orEmpty()
        }

        val receivers = implicitReceiversByLabel[Name.identifier(name)].orEmpty()
        return if (receivers.any { !it.producesInapplicableCandidate() }) {
            receivers.filterNotTo(linkedSetOf()) { it.producesInapplicableCandidate() }
        } else {
            receivers
        }
    }

    fun getBySymbol(symbol: CfirSymbol<*>): ImplicitValue<*>? = implicitValuesBySymbol[symbol]

    fun lastDispatchReceiver(): ImplicitDispatchReceiverValue? =
        implicitReceiverStack.filterIsInstance<ImplicitDispatchReceiverValue>().lastOrNull()

    fun receiversAsReversed(): List<ImplicitReceiverValue<*>> = implicitReceiverStack.asReversed()

    @ImplicitValue.ImplicitValueInternals
    fun replaceImplicitValueType(symbol: CfirSymbol<*>, type: ConeCangJieType) {
        implicitValuesBySymbol[symbol]?.updateTypeFromSmartcast(type)
    }

    fun createSnapshot(mapper: ImplicitValueMapper): ImplicitValueStorage = ImplicitValueStorage(
        implicitReceiverStack = implicitReceiverStack.map { mapper(it) }.toPersistentList(),
        implicitReceiversByLabel = implicitReceiversByLabel.mapValues { (_, values) ->
            values.mapTo(linkedSetOf()) { mapper(it) }.toPersistentSet()
        }.toPersistentMap(),
        implicitValuesBySymbol = implicitValuesBySymbol.mapValues { (_, value) -> mapper(value) }.toPersistentMap(),
    )
}

interface ImplicitValueMapper {
    operator fun <S, T> invoke(value: T): T
        where S : CfirSymbol<*>, T : ImplicitValue<S>
}

fun Set<ImplicitReceiverValue<*>>.ambiguityDiagnosticFor(labelName: String?): ConeSimpleDiagnostic {
    val reason = if (labelName == null) {
        "Ambiguous implicit receiver"
    } else {
        "Ambiguous implicit receiver"
    }
    return ConeSimpleDiagnostic(reason, DiagnosticKind.Other)
}
