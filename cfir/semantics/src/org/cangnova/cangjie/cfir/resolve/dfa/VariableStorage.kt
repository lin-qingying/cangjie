package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirSmartCastExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

class VariableStorage private constructor(
    private val session: CfirSession,
    private val realVariables: MutableMap<RealVariable, RealVariable>,
    private val memberVariables: MutableMap<RealVariable, MutableSet<RealVariable>>,
) {
    constructor(session: CfirSession) : this(
        session = session,
        realVariables = LinkedHashMap(),
        memberVariables = LinkedHashMap(),
    )

    fun createSnapshot(): VariableStorage {
        val membersCopy = LinkedHashMap<RealVariable, MutableSet<RealVariable>>()
        for ((key, value) in memberVariables) {
            membersCopy[key] = LinkedHashSet(value)
        }
        return VariableStorage(
            session = session,
            realVariables = LinkedHashMap(realVariables),
            memberVariables = membersCopy,
        )
    }

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

    fun getKnown(variable: RealVariable): RealVariable? = realVariables[variable]

    fun remember(variable: RealVariable): RealVariable =
        rememberWithKnownReceivers(variable.mapReceivers(::remember))

    fun replaceReceiverReferencesInMembers(from: RealVariable, to: RealVariable?, processMember: (RealVariable, RealVariable?) -> Unit) {
        for (member in memberVariables[from].orEmpty()) {
            val remapped = to?.let { replacement ->
                rememberWithKnownReceivers(member.mapReceivers { if (it == from) replacement else it })
            }
            processMember(member, remapped)
        }
    }

    private fun rememberWithKnownReceivers(variable: RealVariable): RealVariable =
        realVariables.getOrPut(variable) {
            variable.dispatchReceiver?.let { receiver ->
                memberVariables.getOrPut(receiver) { linkedSetOf() }.add(variable)
            }
            variable
        }

    private inline fun RealVariable.mapReceivers(block: (RealVariable) -> RealVariable): RealVariable =
        RealVariable(symbol, isImplicit, dispatchReceiver?.let(block), extensionReceiver?.let(block), originalType)

    private fun CfirExpression.unwrapElement(): CfirExpression = when (this) {
        is CfirSmartCastExpression -> originalExpression.unwrapElement()
        else -> this
    }

    private fun CfirExpression.toDataFlowSymbol(): CfirBasedSymbol<*>? {
        val resolvedReference = (this as? CfirQualifiedAccessExpression)?.calleeReference as? CfirResolvedNamedReference
        val symbol = resolvedReference?.resolvedSymbol ?: return null
        return if (symbol.cfir is CfirProperty) symbol else null
    }
}
