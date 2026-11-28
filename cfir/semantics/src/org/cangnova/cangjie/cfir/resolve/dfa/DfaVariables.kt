package org.cangnova.cangjie.cfir.resolve.dfa

import kotlinx.collections.immutable.toPersistentList

import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.SessionHolder
import org.cangnova.cangjie.cfir.originalIfFakeOverride
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirResolvedDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirSmartCastExpression
import org.cangnova.cangjie.cfir.expressions.CfirOptionalChainExpression
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirAugmentedAssignment
import org.cangnova.cangjie.cfir.expressions.CfirIncrementDecrementExpression
import org.cangnova.cangjie.cfir.expressions.CfirThisReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirSmartcastStability
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.descriptors.Visibilities
import java.util.Objects

/**
 * DFA 可追踪变量的公共基类。
 */
sealed class DataFlowVariable {
    /** 进入数据流分析前的原始类型。 */
    abstract val originalType: ConeCangJieType

    /** 访问变量实际保存的源关联，不构造新的变量或解析符号。 */
    abstract fun acceptSourceChildren(visitor: DfaSourceVisitor)

    companion object {
        /**
         * 对位固定 Kotlin DataFlowVariable.of：变量、参数、模式绑定使用真实存储身份。
         * symbolMapper 在读取借用 reference 后投影有效目标，不改写原 reference 或 receiver。
         */
        context(holder: SessionHolder)
        fun of(
            fir: CfirExpression,
            unwrap: (RealVariable) -> RealVariable? = { it },
            unwrapInReceivers: (RealVariable) -> RealVariable?,
            symbolMapper: (CfirBasedSymbol<*>) -> CfirBasedSymbol<*> = { it },
        ): DataFlowVariable? {
            val unwrapped = fir.unwrapElement() ?: return null
            val isImplicit = unwrapped is CfirThisReceiverExpression
            val selected = when (unwrapped) {
                is CfirThisReceiverExpression -> unwrapped.calleeReference.boundSymbol
                is CfirQualifiedAccessExpression ->
                    (unwrapped.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol
                else -> null
            }?.let(symbolMapper)
            // qualifier 的 alias 展开保留实际 session/binder 身份，展开后的目标再进入同次投影。
            val expanded = if (selected is CfirTypeAliasSymbol) {
                selected.cfir.expandedTypeRef.coneTypeOrNull?.fullyExpandedType(holder.session)?.toSymbol(holder.session)
                    ?.let(symbolMapper)
            } else selected
            val symbol = expanded?.takeIf {
                isImplicit || it is CfirVariableSymbol<*> || it is CfirPropertySymbol ||
                    it is CfirClassLikeSymbol<*> && it !is CfirTypeAliasSymbol
            }?.unwrapFakeOverridesIfNecessary(symbolMapper)
                ?: return SyntheticVariable(unwrapped)
            val access = unwrapped as? CfirQualifiedAccessExpression
            val dispatchReceiver = access?.dispatchReceiver?.let {
                val variable = of(it, unwrapInReceivers, unwrapInReceivers, symbolMapper) ?: return null
                variable as? RealVariable ?: return SyntheticVariable(unwrapped)
            }
            return unwrap(RealVariable(
                symbol, isImplicit, dispatchReceiver, null,
                checkNotNull(unwrapped.coneTypeOrNull) { "A real DFA variable requires a resolved expression type" },
            ))
        }

        private fun CfirExpression.unwrapElement(): CfirExpression? = when (this) {
            is CfirSmartCastExpression -> originalExpression.unwrapElement()
            is CfirOptionalChainExpression -> when (expression.unwrapElement()) {
                null, is CfirAssignment, is CfirAugmentedAssignment, is CfirIncrementDecrementExpression -> null
                // 读链具有独立 Option 结果，不能把 selector 的存储或 payload 当作整链的值。
                else -> this
            }
            else -> this
        }
    }
}

/** 正式classifier qualifier没有运行时值；其他显式/dispatch base都必须完成本次求值。 */
fun CfirQualifiedAccessExpression.runtimeReceiversForDfa(): List<CfirExpression> {
    val seen = java.util.Collections.newSetFromMap(java.util.IdentityHashMap<CfirExpression, Boolean>())
    return listOfNotNull(explicitReceiver, dispatchReceiver).filter { receiver ->
        val target = ((receiver as? CfirQualifiedAccessExpression)?.calleeReference as? CfirResolvedNamedReference)?.resolvedSymbol
        target !is CfirClassLikeSymbol<*> && target !is CfirTypeAliasSymbol && seen.add(receiver)
    }
}

/** fake origin 每次揭露的符号都经过有效映射，借用 wrapper 与原声明保持不变。 */
private fun CfirBasedSymbol<*>.unwrapFakeOverridesIfNecessary(
    symbolMapper: (CfirBasedSymbol<*>) -> CfirBasedSymbol<*>,
): CfirBasedSymbol<*> {
    var current = this
    val visited = mutableSetOf<CfirBasedSymbol<*>>()
    while (current is CfirCallableSymbol<*> && current.cfir.dispatchReceiverType != null) {
        check(visited.add(current)) { "Effective DFA symbol mappings contain a cyclic fake origin" }
        val original = current.originalIfFakeOverride() ?: return current
        current = symbolMapper(original)
    }
    return current
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
    override fun acceptSourceChildren(visitor: DfaSourceVisitor) {
        visitor.visitElement(symbol.cfir)
        visitor.visitType(originalType)
        dispatchReceiver?.let(visitor::visitVariable)
        extensionReceiver?.let(visitor::visitVariable)
    }

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
        if (declaration is CfirVariable && !declaration.isLocal && declaration.isVar) {
            return CfirSmartcastStability.UNSTABLE_VALUE
        }
        if (declaration is CfirProperty) {
            if (declaration.isLocal && declaration.status.isMut) {
                return CfirSmartcastStability.UNSTABLE_VALUE
            }
            if (declaration.status.isConst) {
                return CfirSmartcastStability.STABLE_VALUE
            }
            if (declaration.status.isMut) {
                return CfirSmartcastStability.UNSTABLE_VALUE
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
        // 真实Struct/Enum classifier同样检查正式STATUS finality；这不授予Class引用地址共享。
        fun ConeCangJieType.isNominalType(): Boolean = this is ConeClassLikeType || this is ConeStructType || this is ConeEnumType
        if (originalType.isNominalType()) {
            val symbol = originalType.fullyExpandedType(session).toSymbol(session)
            if (symbol.isFinalClassLikeSymbol()) return true
        }
        return flow.getTypeStatement(this)?.upperTypes.orEmpty().any { candidate ->
            candidate.isNominalType() &&
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
    override fun acceptSourceChildren(visitor: DfaSourceVisitor) {
        visitor.visitElement(fir)
        fir.coneTypeOrNull?.let(visitor::visitType)
    }

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

/** 值节点只表示不可变值身份；事实仍由 Flow 持有。 */
sealed class ValueVariable : DataFlowVariable()

/** N 的 RHS 保存值；每个实例是一份完成值，不用 AST/source 相等替代身份。 */
class CompletedValueVariable(
    val origin: org.cangnova.cangjie.cfir.CfirElement,
    override val originalType: ConeCangJieType,
    inputs: List<ValueVariable> = emptyList(),
) : ValueVariable() {
    val inputs = inputs.toPersistentList()
    override fun acceptSourceChildren(visitor: DfaSourceVisitor) {
        visitor.visitElement(origin)
        visitor.visitType(originalType)
        inputs.forEach(visitor::visitVariable)
    }
}

/** 已成功观察的稳定存储 SSA 值；null 是 K 的初始版本，不是伪造的 index=0。 */
class StorageValueVariable(
    val storage: RealVariable,
    val index: Int?,
    val knowledgeEvent: ValueEvent? = null,
) : ValueVariable() {
    override val originalType: ConeCangJieType get() = storage.originalType
    override fun equals(other: Any?): Boolean =
        other is StorageValueVariable && storage == other.storage && index == other.index && knowledgeEvent === other.knowledgeEvent
    override fun hashCode(): Int = 31 * (31 * storage.hashCode() + (index?.hashCode() ?: 0)) + System.identityHashCode(knowledgeEvent)
    override fun acceptSourceChildren(visitor: DfaSourceVisitor) {
        visitor.visitVariable(storage)
        knowledgeEvent?.let { visitor.visitElement(it.origin) }
    }
}

/** N 的纯 tuple 投影；类型属于 use，不参与 subject/path 值身份比较。 */
class ProjectedValueVariable(
    val subject: ValueVariable,
    val index: Int,
    override val originalType: ConeCangJieType,
) : ValueVariable() {
    override fun equals(other: Any?): Boolean =
        other is ProjectedValueVariable && subject == other.subject && index == other.index
    override fun hashCode(): Int = 31 * subject.hashCode() + index
    override fun acceptSourceChildren(visitor: DfaSourceVisitor) {
        visitor.visitVariable(subject)
        visitor.visitType(originalType)
    }
}
