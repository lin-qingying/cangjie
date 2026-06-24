package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirSmartCastExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

/**
 * DFA 中表达式到数据流变量的存储。
 *
 * @property realVariables 已知真实变量的规范实例表。
 * @property memberVariables 按 receiver 记录的成员变量集合，用于 receiver 别名替换时同步成员引用。
 */
class VariableStorage private constructor(
    private val realVariables: MutableMap<RealVariable, RealVariable>,
    private val memberVariables: MutableMap<RealVariable, MutableSet<RealVariable>>,
) {
    constructor() : this(
        realVariables = LinkedHashMap(),
        memberVariables = LinkedHashMap(),
    )

    /**
     * 创建当前变量存储的深拷贝快照。
     */
    fun createSnapshot(): VariableStorage {
        val membersCopy = LinkedHashMap<RealVariable, MutableSet<RealVariable>>()
        for ((key, value) in memberVariables) {
            membersCopy[key] = LinkedHashSet(value)
        }
        return VariableStorage(
            realVariables = LinkedHashMap(realVariables),
            memberVariables = membersCopy,
        )
    }

    /**
     * 清空所有变量与成员索引。
     */
    fun reset() {
        realVariables.clear()
        memberVariables.clear()
    }

    /**
     * 获取表达式对应的数据流变量。
     *
     * @param fir 要映射的数据流表达式。
     * @param createReal 是否允许为尚未登记的真实变量创建规范实例。
     * @param unwrapAlias 普通变量别名展开函数。
     * @param unwrapAliasInReceivers receiver 位置使用的别名展开函数。
     */
    fun get(
        fir: CfirExpression,
        createReal: Boolean,
        unwrapAlias: (RealVariable) -> RealVariable?,
        unwrapAliasInReceivers: (RealVariable) -> RealVariable? = unwrapAlias,
    ): DataFlowVariable? {
        val unwrapped = fir.unwrapElement()
        val symbol = unwrapped.toDataFlowSymbol() ?: return SyntheticVariable(unwrapped)
        val qualifiedAccess = unwrapped as? CfirQualifiedAccessExpression
        val dispatchReceiverVar = qualifiedAccess?.dispatchReceiver?.let {
            (get(it, createReal, unwrapAliasInReceivers) ?: return null) as? RealVariable ?: return SyntheticVariable(unwrapped)
        }
        val prototype = RealVariable(
            symbol = symbol,
            isImplicit = false,
            dispatchReceiver = dispatchReceiverVar,
            extensionReceiver = null,
            originalType = unwrapped.coneTypeOrNull ?: return null,
        )
        val real = if (createReal) rememberWithKnownReceivers(prototype) else realVariables[prototype] ?: return null
        return unwrapAlias(real)
    }

    /**
     * 查找已经登记过的真实变量规范实例。
     */
    fun getKnown(variable: RealVariable): RealVariable? = realVariables[variable]

    /**
     * 登记真实变量及其 receiver 链。
     */
    fun remember(variable: RealVariable): RealVariable =
        rememberWithKnownReceivers(variable.mapReceivers(::remember))

    /**
     * 替换指定 receiver 下所有成员变量中的 receiver 引用。
     *
     * @param from 原 receiver 变量。
     * @param to 新 receiver 变量；为空表示成员变量失去可映射 receiver。
     * @param processMember 每个成员变量及其替换结果的回调。
     */
    fun replaceReceiverReferencesInMembers(from: RealVariable, to: RealVariable?, processMember: (RealVariable, RealVariable?) -> Unit) {
        for (member in memberVariables[from].orEmpty()) {
            val remapped = to?.let { replacement ->
                rememberWithKnownReceivers(member.mapReceivers { if (it == from) replacement else it })
            }
            processMember(member, remapped)
        }
    }

    /**
     * 登记 receiver 已经规范化后的真实变量。
     */
    private fun rememberWithKnownReceivers(variable: RealVariable): RealVariable =
        realVariables.getOrPut(variable) {
            variable.dispatchReceiver?.let { receiver ->
                memberVariables.getOrPut(receiver) { linkedSetOf() }.add(variable)
            }
            variable
        }

    /**
     * 映射真实变量的 dispatch/extension receiver。
     */
    private inline fun RealVariable.mapReceivers(block: (RealVariable) -> RealVariable): RealVariable =
        RealVariable(symbol, isImplicit, dispatchReceiver?.let(block), extensionReceiver?.let(block), originalType)

    /**
     * 去除不改变变量身份的包装表达式。
     */
    private fun CfirExpression.unwrapElement(): CfirExpression = when (this) {
        is CfirSmartCastExpression -> originalExpression.unwrapElement()
        else -> this
    }

    /**
     * 提取表达式对应的数据流符号。
     *
     * 当前 DFA 只把属性符号作为可稳定追踪的真实变量；其他表达式退化为合成变量。
     */
    private fun CfirExpression.toDataFlowSymbol(): CfirBasedSymbol<*>? {
        val resolvedReference = (this as? CfirQualifiedAccessExpression)?.calleeReference as? CfirResolvedNamedReference
        val symbol = resolvedReference?.resolvedSymbol ?: return null
        return if (symbol.cfir is CfirProperty) symbol else null
    }
}
