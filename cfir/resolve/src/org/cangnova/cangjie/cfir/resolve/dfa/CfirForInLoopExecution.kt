package org.cangnova.cangjie.cfir.resolve.dfa

import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.CfirEnumPattern
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.source.CfirSourceForInKind
import org.cangnova.cangjie.cfir.source.CfirSourceOperator
import org.cangnova.cangjie.cfir.types.coneTypeOrNull

/**
 * 保留 For 的实际执行载荷。此读取器不拥有或复制 AST，不生成条件/赋值/声明。
 * native 14276740 的 ForInExpr.cpp 保存顺序与执行顺序不同，BODY、mini CFG 与
 * resolved replay 共用本产品；解析、CFG、flow 和发布仍由各自原 owner 负责。
 */
internal sealed class CfirForInLoopExecution(val loop: CfirForInExpression) {
    abstract val initializers: List<CfirVariable>
    open val pattern: CfirPattern get() = loop.pattern

    /** 同一实际阶段序列由 BODY、已解析 replay 和 mini 的原 owner 分别执行。 */
    interface Visitor {
        fun expression(expression: CfirExpression)
        fun initialize(variable: CfirVariable)
        fun enterLoop(execution: CfirForInLoopExecution)
        fun initialCheck(execution: CfirForInLoopExecution)
        fun enterCondition(execution: CfirForInLoopExecution)
        fun enterIteration(execution: CfirForInLoopExecution)
        fun check(execution: CfirForInLoopExecution)
        fun bindAndBody(execution: CfirForInLoopExecution)
        fun exitBody(execution: CfirForInLoopExecution)
        fun updateString(execution: StringIteration)
        fun exitLoop(execution: CfirForInLoopExecution)
    }

    /** 存储节点不克隆；阶段中的重复执行引用同一个 e/d/P。 */
    fun accept(visitor: Visitor) {
        if (this is Direct) visitor.expression(sequence)
        visitor.enterLoop(this)
        initializers.forEach(visitor::initialize)
        when (this) {
            is Range, is StringIteration -> {
                visitor.initialCheck(this)
                visitor.enterCondition(this)
            }
            is Iterator -> {
                visitor.enterIteration(this)
                visitor.expression(next)
            }
            is Direct -> Unit
        }
        visitor.check(this)
        visitor.bindAndBody(this)
        visitor.exitBody(this)
        when (this) {
            is Range -> {
                visitor.enterIteration(this)
                visitor.expression(update)
            }
            is StringIteration -> {
                visitor.enterIteration(this)
                visitor.updateString(this)
            }
            is Iterator, is Direct -> Unit
        }
        visitor.exitLoop(this)
    }

    class Direct(loop: CfirForInExpression) : CfirForInLoopExecution(loop) {
        override val initializers: List<CfirVariable> = emptyList()
        val sequence: CfirExpression = loop.iterable
    }

    class Range internal constructor(
        loop: CfirForInExpression,
        val index: CfirVariable,
        val bound: CfirVariable,
        val update: CfirAssignment,
        val comparison: CfirBinaryExpression,
    ) : CfirForInLoopExecution(loop) {
        override val initializers: List<CfirVariable> = listOf(index, bound)
    }

    class StringIteration internal constructor(
        loop: CfirForInExpression,
        val index: CfirVariable,
        val string: CfirVariable,
        val bound: CfirVariable,
    ) : CfirForInLoopExecution(loop) {
        override val initializers: List<CfirVariable> = listOf(index, string, bound)
    }

    class Iterator internal constructor(
        loop: CfirForInExpression,
        val iterator: CfirVariable,
        val selectorHolder: CfirNativeMatchExpression,
        val some: CfirEnumPattern,
    ) : CfirForInLoopExecution(loop) {
        override val initializers: List<CfirVariable> = listOf(iterator)
        val next: CfirExpression = selectorHolder.selector
        // Some 包装由原生阶段拥有，成功后绑定的是移入其中的原 P。
        override val pattern: CfirPattern = some.arguments.single()
    }

    companion object {
        fun read(loop: CfirForInExpression): CfirForInLoopExecution {
            if (loop.iterationKind == CfirSourceForInKind.DIRECT) return Direct(loop)
            val block = loop.iterable
            check(block is CfirBlock) { "Native For must retain its actual initializer Block" }
            val slots = block.statements
            fun variable(index: Int): CfirVariable {
                val declaration = slots[index]
                check(declaration is CfirVariable && declaration.initializer != null) {
                    "Native For initializer slot must retain its actual initialized declaration"
                }
                checkNotNull(declaration.returnTypeRef.coneTypeOrNull) { "Native For declaration must retain its completed type" }
                return declaration
            }
            return when (loop.iterationKind) {
                CfirSourceForInKind.DIRECT -> error("Direct For has no native Block protocol")
                CfirSourceForInKind.RANGE -> {
                    check(slots.size == 4) { "Native RANGE For requires two declarations, update, comparison" }
                    val index = variable(0)
                    val bound = variable(1)
                    val update = slots[2]
                    val comparison = slots[3]
                    check(update is CfirAssignment && update.isCompound && update.operation == CfirSourceOperator.ADD_ASSIGN) {
                        "Native RANGE For must retain its actual compound update"
                    }
                    check(comparison is CfirBinaryExpression && comparison.operation in setOf(
                        CfirSourceOperator.LESS, CfirSourceOperator.LESS_EQUAL,
                        CfirSourceOperator.GREATER, CfirSourceOperator.GREATER_EQUAL,
                    )) { "Native RANGE For must retain its actual comparison" }
                    requireReferenceTo(update.lValue, index)
                    requireReferenceTo(comparison.left, index)
                    requireReferenceTo(comparison.right, bound)
                    Range(loop, index, bound, update, comparison)
                }
                CfirSourceForInKind.STRING -> {
                    check(slots.size == 3) { "Native STRING For requires index, string temporary, bound declarations" }
                    StringIteration(loop, variable(0), variable(1), variable(2))
                }
                CfirSourceForInKind.ITERATOR -> {
                    check(slots.size == 2) { "Native ITERATOR For requires iterator declaration and selector holder" }
                    val iterator = variable(0)
                    val holder = slots[1]
                    check(holder is CfirNativeMatchExpression && holder.cases.isEmpty()) {
                        "Native ITERATOR selector holder must retain its zero-case Match"
                    }
                    val some = loop.pattern
                    check(some is CfirEnumPattern && some.arguments.size == 1) {
                        "Native ITERATOR must retain Some with the original owned pattern"
                    }
                    checkNotNull(some.constructorReference as? CfirResolvedNamedReference) {
                        "Native ITERATOR must retain its actual bound Some constructor reference"
                    }
                    checkNotNull(holder.selector.coneTypeOrNull) { "Native ITERATOR next must retain its completed result type" }
                    Iterator(loop, iterator, holder, some)
                }
            }
        }

        private fun requireReferenceTo(expression: CfirExpression, declaration: CfirVariable) {
            check(expression is CfirQualifiedAccessExpression && expression.explicitReceiver == null && expression.dispatchReceiver == null) {
                "Native RANGE operand must be an actual direct declaration reference"
            }
            val reference = expression.calleeReference
            check(reference is CfirResolvedNamedReference && reference.resolvedSymbol === declaration.symbol) {
                "Native RANGE references must retain the stored declaration identity"
            }
        }
    }
}
