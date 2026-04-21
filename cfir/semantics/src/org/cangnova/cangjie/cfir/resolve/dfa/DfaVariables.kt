package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirSmartcastStability
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.descriptors.Visibilities
import java.util.Objects

sealed class DataFlowVariable {
    abstract val originalType: ConeCangJieType
}

class RealVariable(
    val symbol: CfirBasedSymbol<*>,
    val isImplicit: Boolean,
    val dispatchReceiver: RealVariable?,
    val extensionReceiver: RealVariable?,
    override val originalType: ConeCangJieType,
) : DataFlowVariable() {
    companion object {
        fun implicit(symbol: CfirBasedSymbol<*>, type: ConeCangJieType): RealVariable =
            RealVariable(symbol, isImplicit = true, dispatchReceiver = null, extensionReceiver = null, originalType = type)
    }

    override fun equals(other: Any?): Boolean =
        other is RealVariable &&
            symbol == other.symbol &&
            isImplicit == other.isImplicit &&
            dispatchReceiver == other.dispatchReceiver &&
            extensionReceiver == other.extensionReceiver

    override fun hashCode(): Int =
        Objects.hash(symbol, isImplicit, dispatchReceiver, extensionReceiver)

    override fun toString(): String = buildString {
        if (isImplicit) append("this@")
        append(
            when (symbol) {
                is CfirClassLikeSymbol<*> -> symbol.classId
                is CfirCallableSymbol<*> -> symbol.callableId
                else -> symbol
            },
        )
        if (dispatchReceiver != null && extensionReceiver != null) {
            append("($dispatchReceiver, $extensionReceiver)")
        } else if (dispatchReceiver != null || extensionReceiver != null) {
            append("(${dispatchReceiver ?: extensionReceiver})")
        }
    }

    fun getStability(flow: Flow, session: CfirSession): CfirSmartcastStability {
        if (isImplicit) return CfirSmartcastStability.STABLE_VALUE
        val declaration = symbol.cfir
        if (declaration is CfirProperty) {
            if (declaration.isLocal && declaration.status.isMut) {
                return CfirSmartcastStability.UNSTABLE_VALUE
            }
            if (declaration.status.isConst) {
                return CfirSmartcastStability.STABLE_VALUE
            }
            if (declaration.getter != null) {
                return CfirSmartcastStability.UNSTABLE_VALUE
            }
            if (declaration.status.visibility != Visibilities.Private && declaration.status.isOpen) {
                return CfirSmartcastStability.UNSTABLE_VALUE
            }
        }
        if (dispatchReceiver?.hasFinalType(flow, session) == false) {
            return CfirSmartcastStability.UNSTABLE_VALUE
        }
        if (dispatchReceiver?.getStability(flow, session) == CfirSmartcastStability.UNSTABLE_VALUE) {
            return CfirSmartcastStability.UNSTABLE_VALUE
        }
        return CfirSmartcastStability.STABLE_VALUE
    }

    private fun hasFinalType(flow: Flow, session: CfirSession): Boolean {
        if (originalType is ConeClassLikeType) {
            val symbol = originalType.fullyExpandedType(session).toSymbol(session)
            if (symbol.isFinalClassLikeSymbol()) return true
        }
        return flow.getTypeStatement(this)?.upperTypes.orEmpty().any { candidate ->
            candidate is ConeClassLikeType &&
                candidate.fullyExpandedType(session).toSymbol(session).isFinalClassLikeSymbol()
        }
    }
}

data class SyntheticVariable(val fir: CfirExpression) : DataFlowVariable() {
    override val originalType: ConeCangJieType
        get() = fir.coneTypeOrNull ?: error("Synthetic variable requires resolved expression type: ${fir::class.simpleName}")
}

private fun CfirBasedSymbol<*>?.isFinalClassLikeSymbol(): Boolean {
    this ?: return false
    lazyResolveToPhase(CfirResolvePhase.STATUS)
    val status = (cfir as? CfirMemberDeclaration)?.status as? CfirResolvedDeclarationStatus ?: return false
    return !status.isOpen
}
