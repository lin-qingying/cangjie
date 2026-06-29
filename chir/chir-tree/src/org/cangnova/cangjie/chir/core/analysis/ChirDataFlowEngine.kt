package org.cangnova.cangjie.chir.core.analysis

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirUnwindTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

/**
 * 数据流分析方向。
 */
enum class ChirDataFlowDirection {
    FORWARD,
    BACKWARD,
}

/**
 * 数据流分析使用的格接口。
 */
interface ChirLattice<S> {
    /**
     * 返回格的 top 状态。
     */
    fun top(): S

    /**
     * 合并两个状态。
     */
    fun join(left: S, right: S): S
}

/**
 * 数据流分析结果。
 */
data class ChirDataFlowResult<S>(
    /**
     * 每个基本块入口状态。
     */
    val inState: Map<ChirSemanticId, S>,

    /**
     * 每个基本块出口状态。
     */
    val outState: Map<ChirSemanticId, S>,

    /**
     * 收敛所需迭代次数。
     */
    val iterations: Int,
)

/**
 * 通用 CHIR 数据流分析引擎。
 */
class ChirDataFlowEngine<S>(
    /**
     * 分析方向。
     */
    private val direction: ChirDataFlowDirection,

    /**
     * 状态格。
     */
    private val lattice: ChirLattice<S>,

    /**
     * 单个基本块上的 transfer 函数。
     */
    private val transfer: (ChirBlock, S) -> S,

    /**
     * 前向分析入口种子状态。
     */
    private val entrySeed: S? = null,

    /**
     * 后向分析出口种子状态。
     */
    private val exitSeed: S? = null,
) {
    /**
     * 对 [function] 执行数据流分析直到收敛。
     */
    fun analyze(function: ChirFunctionDeclaration): ChirDataFlowResult<S> {
        require(function.blocks.isNotEmpty()) { "function ${function.name} has no blocks" }

        val blocks = function.blocks
        val blockById = blocks.associateBy { it.semanticId }
        val successors = buildSuccessorMap(blocks)
        val predecessors = buildPredecessorMap(blocks, successors)

        val inState = blocks.associate { it.semanticId to lattice.top() }.toMutableMap()
        val outState = blocks.associate { it.semanticId to lattice.top() }.toMutableMap()

        when (direction) {
            ChirDataFlowDirection.FORWARD -> {
                entrySeed?.let { inState[function.entryBlockId] = it }
            }
            ChirDataFlowDirection.BACKWARD -> {
                val exitBlocks = blocks.filter { successors.getValue(it.semanticId).isEmpty() }
                if (exitBlocks.isNotEmpty()) {
                    val seed = exitSeed ?: lattice.top()
                    exitBlocks.forEach { outState[it.semanticId] = seed }
                }
            }
        }

        var changed: Boolean
        var iteration = 0
        do {
            changed = false
            iteration += 1
            val ordered = when (direction) {
                ChirDataFlowDirection.FORWARD -> blocks
                ChirDataFlowDirection.BACKWARD -> blocks.asReversed()
            }

            ordered.forEach { block ->
                val blockId = block.semanticId
                when (direction) {
                    ChirDataFlowDirection.FORWARD -> {
                        val incoming = predecessors.getValue(blockId)
                            .map { outState.getValue(it) }
                            .foldOptionalJoin(inState.getValue(blockId), lattice)
                        if (incoming != inState.getValue(blockId)) {
                            inState[blockId] = incoming
                            changed = true
                        }
                        val transferred = transfer(block, inState.getValue(blockId))
                        if (transferred != outState.getValue(blockId)) {
                            outState[blockId] = transferred
                            changed = true
                        }
                    }

                    ChirDataFlowDirection.BACKWARD -> {
                        val outgoing = successors.getValue(blockId)
                            .map { inState.getValue(it) }
                            .foldOptionalJoin(outState.getValue(blockId), lattice)
                        if (outgoing != outState.getValue(blockId)) {
                            outState[blockId] = outgoing
                            changed = true
                        }
                        val transferred = transfer(block, outState.getValue(blockId))
                        if (transferred != inState.getValue(blockId)) {
                            inState[blockId] = transferred
                            changed = true
                        }
                    }
                }
            }
        } while (changed && iteration < 10_000)

        require(iteration < 10_000) { "data flow did not converge for function ${function.name}" }
        require(blockById.keys.containsAll(inState.keys))

        return ChirDataFlowResult(
            inState = inState,
            outState = outState,
            iterations = iteration,
        )
    }

    /**
     * 构建基本块后继映射。
     */
    private fun buildSuccessorMap(blocks: List<ChirBlock>): Map<ChirSemanticId, Set<ChirSemanticId>> {
        return blocks.associate { block ->
            val next = when (val terminator = block.terminator) {
                is ChirBranchTerminator -> setOf(terminator.targetBlockId)
                is ChirConditionalBranchTerminator -> setOf(terminator.trueTargetBlockId, terminator.falseTargetBlockId)
                is ChirUnwindTerminator -> setOf(terminator.targetBlockId)
                else -> emptySet()
            }
            block.semanticId to next
        }
    }

    /**
     * 根据后继映射构建基本块前驱映射。
     */
    private fun buildPredecessorMap(
        blocks: List<ChirBlock>,
        successors: Map<ChirSemanticId, Set<ChirSemanticId>>,
    ): Map<ChirSemanticId, Set<ChirSemanticId>> {
        val mutable = blocks.associate { it.semanticId to linkedSetOf<ChirSemanticId>() }.toMutableMap()
        successors.forEach { (from, tos) ->
            tos.forEach { to ->
                mutable.getOrPut(to) { linkedSetOf() }.add(from)
            }
        }
        return mutable
    }

    /**
     * 对状态列表执行 join；列表为空时返回 [seed]。
     */
    private fun List<S>.foldOptionalJoin(seed: S, lattice: ChirLattice<S>): S {
        if (isEmpty()) return seed
        return drop(1).fold(first()) { acc, state -> lattice.join(acc, state) }
    }
}
