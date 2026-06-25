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
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name

/**
 * 隐式 receiver 与隐式值的持久化存储。
 *
 * @property implicitReceiverStack 按进入作用域顺序保存的隐式 receiver 栈。
 * @property implicitReceiversByLabel 按标签名索引的隐式 receiver 集合。
 * @property implicitValuesBySymbol 按绑定符号索引的隐式值集合。
 */
class ImplicitValueStorage private constructor(
    /**
     * 按进入作用域顺序保存的隐式 receiver 栈。
     */
    private val implicitReceiverStack: PersistentList<ImplicitReceiverValue<*>>,
    /**
     * 按标签名索引的隐式 receiver 集合。
     */
    private val implicitReceiversByLabel: PersistentMap<Name, PersistentSet<ImplicitReceiverValue<*>>>,
    /**
     * 按绑定符号索引的隐式值集合。
     */
    private val implicitValuesBySymbol: PersistentMap<CfirBasedSymbol<*>, ImplicitValue<*>>,
) {
    constructor() : this(
        persistentListOf(),
        persistentHashMapOf(),
        persistentHashMapOf(),
    )

    /**
     * 当前可见的隐式 receiver 列表。
     */
    val implicitReceivers: List<ImplicitReceiverValue<*>>
        get() = implicitReceiverStack

    /**
     * 当前可见的全部隐式值。
     */
    val implicitValues: Collection<ImplicitValue<*>>
        get() = implicitValuesBySymbol.values

    /**
     * 批量追加隐式 receiver。
     *
     * @return 包含追加 receiver 的新存储实例。
     */
    fun addAllImplicitReceivers(receivers: List<ImplicitReceiverValue<*>>): ImplicitValueStorage {
        return receivers.fold(this) { acc, receiver ->
            acc.addImplicitReceiver(name = null, value = receiver)
        }
    }

    /**
     * 追加单个隐式 receiver。
     *
     * @param name receiver 标签名；为空表示只加入无标签 receiver 栈。
     * @param value 要追加的隐式 receiver 值。
     * @return 包含该 receiver 的新存储实例。
     */
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

    /**
     * 按标签查询隐式 receiver。
     *
     * 未指定标签时返回最近且可适用的 receiver；如果全部不可适用，则返回最近 receiver 以便生成
     * 正确诊断。
     */
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

    /**
     * 按绑定符号查询隐式值。
     */
    fun getBySymbol(symbol: CfirBasedSymbol<*>): ImplicitValue<*>? = implicitValuesBySymbol[symbol]

    /**
     * 返回最近的 dispatch receiver。
     */
    fun lastDispatchReceiver(): ImplicitDispatchReceiverValue? =
        implicitReceiverStack.filterIsInstance<ImplicitDispatchReceiverValue>().lastOrNull()

    /**
     * 以从内到外的顺序返回隐式 receiver。
     */
    fun receiversAsReversed(): List<ImplicitReceiverValue<*>> = implicitReceiverStack.asReversed()

    /**
     * 用 smart cast 后的类型替换指定隐式值类型。
     */
    @ImplicitValue.ImplicitValueInternals
    fun replaceImplicitValueType(symbol: CfirBasedSymbol<*>, type: ConeCangJieType) {
        implicitValuesBySymbol[symbol]?.updateTypeFromSmartcast(type)
    }

    /**
     * 创建隐式值存储快照。
     *
     * [mapper] 可替换 receiver 和隐式值实例，用于 DFA 分支复制或作用域快照。
     */
    fun createSnapshot(mapper: ImplicitValueMapper): ImplicitValueStorage = ImplicitValueStorage(
        implicitReceiverStack = implicitReceiverStack.map { mapper(it) }.toPersistentList(),
        implicitReceiversByLabel = implicitReceiversByLabel.mapValues { (_, values) ->
            values.mapTo(linkedSetOf()) { mapper(it) }.toPersistentSet()
        }.toPersistentMap(),
        implicitValuesBySymbol = implicitValuesBySymbol.mapValues { (_, value) -> mapper(value) }.toPersistentMap(),
    )
}

/**
 * 隐式值快照映射器。
 */
interface ImplicitValueMapper {
    /**
     * 映射单个隐式值。
     */
    operator fun <S, T> invoke(value: T): T
        where S : CfirBasedSymbol<*>, T : ImplicitValue<S>
}

/**
 * 为一组同名隐式 receiver 生成歧义诊断。
 */
fun Set<ImplicitReceiverValue<*>>.ambiguityDiagnosticFor(labelName: String?): ConeSimpleDiagnostic {
    val reason = if (labelName == null) {
        "Ambiguous implicit receiver"
    } else {
        "Ambiguous implicit receiver"
    }
    return ConeSimpleDiagnostic(reason, DiagnosticKind.Other)
}
