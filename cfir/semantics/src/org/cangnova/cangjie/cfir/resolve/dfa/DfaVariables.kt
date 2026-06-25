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

/**
 * DFA 可追踪变量的公共基类。
 */
sealed class DataFlowVariable {
    /** 进入数据流分析前的原始类型。 */
    abstract val originalType: ConeCangJieType
}

/**
 * 具备符号身份的数据流变量。
 *
 * @property symbol 变量对应的 CFIR 符号。
 * @property isImplicit 是否为隐式 receiver 变量。
 * @property dispatchReceiver 成员访问的 dispatch receiver。
 * @property extensionReceiver 成员访问的 extension receiver。
 * @property originalType 进入数据流分析前的原始类型。
 */
class RealVariable(
    /**
     * 变量对应的 CFIR 符号。
     */
    val symbol: CfirBasedSymbol<*>,
    /**
     * 是否为隐式 receiver 变量。
     */
    val isImplicit: Boolean,
    /**
     * 成员访问的 dispatch receiver。
     */
    val dispatchReceiver: RealVariable?,
    /**
     * 成员访问的 extension receiver。
     */
    val extensionReceiver: RealVariable?,
    /**
     * 进入数据流分析前的原始类型。
     */
    override val originalType: ConeCangJieType,
) : DataFlowVariable() {
    /**
     * 真实变量构造工具。
     */
    companion object {
        /**
         * 创建隐式 receiver 变量。
         */
        fun implicit(symbol: CfirBasedSymbol<*>, type: ConeCangJieType): RealVariable =
            RealVariable(symbol, isImplicit = true, dispatchReceiver = null, extensionReceiver = null, originalType = type)
    }

    /** 基于符号、隐式标记和 receiver 身份比较真实变量。 */
    override fun equals(other: Any?): Boolean =
        other is RealVariable &&
            symbol == other.symbol &&
            isImplicit == other.isImplicit &&
            dispatchReceiver == other.dispatchReceiver &&
            extensionReceiver == other.extensionReceiver

    /** 与 [equals] 一致的哈希值。 */
    override fun hashCode(): Int =
        Objects.hash(symbol, isImplicit, dispatchReceiver, extensionReceiver)

    /** 数据流变量的调试文本。 */
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

    /**
     * 计算该变量是否可稳定 smart cast。
     *
     * 局部可变属性、带 getter 的属性、open 且非 private 的成员，以及接收者不稳定的成员访问都会
     * 被视为不稳定。
     */
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

    /**
     * 判断当前变量在给定 flow 中是否拥有 final 类型。
     */
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

/**
 * 没有稳定符号身份的合成数据流变量。
 *
 * @property fir 该合成变量对应的表达式。
 */
data class SyntheticVariable(val fir: CfirExpression) : DataFlowVariable() {
    /** 表达式解析后的原始类型。 */
    override val originalType: ConeCangJieType
        get() = fir.coneTypeOrNull ?: error("Synthetic variable requires resolved expression type: ${fir::class.simpleName}")
}

/**
 * 判断符号是否解析为 final class-like 声明。
 */
private fun CfirBasedSymbol<*>?.isFinalClassLikeSymbol(): Boolean {
    this ?: return false
    lazyResolveToPhase(CfirResolvePhase.STATUS)
    val status = (cfir as? CfirMemberDeclaration)?.status as? CfirResolvedDeclarationStatus ?: return false
    return !status.isOpen
}
