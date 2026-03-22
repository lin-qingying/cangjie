/*
 * Copyright 2010-2026. cangjie.
 *
 * 推理日志记录器，对标 K2 FirInferenceLogger。
 * 用于跟踪约束系统的推理过程，便于调试和测试。
 */

package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.constraints.CfirConstraint
import org.cangnova.cangjie.cfir.constraints.CfirConstraintIssue
import org.cangnova.cangjie.cfir.constraints.CfirConstraintPosition
import org.cangnova.cangjie.cfir.constraints.CfirConstraintSystem
import org.cangnova.cangjie.cfir.constraints.CfirTypeVariable
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirCandidate
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 推理日志记录器，记录约束系统中的约束添加、变量注册、固定和错误。
 *
 * 对标 K2 `InferenceLogger` + `FirInferenceLogger`，简化为单一类，
 * 使用本项目的 `CfirConstraint`、`CfirTypeVariable`、`CfirConstraintSystem` 等类型。
 */
open class CfirInferenceLogger : CfirSessionComponent {

    // ---- 日志元素 ----

    sealed class LoggingElement

    sealed class BlockOwner {
        class CandidateOwner(val candidate: CfirCandidate) : BlockOwner()
        data object Unknown : BlockOwner()
    }

    class BlockElement(
        val name: String,
        val items: MutableList<BlockItemElement> = mutableListOf(),
        val owner: BlockOwner,
    ) : LoggingElement()

    sealed class BlockItemElement : LoggingElement()

    class NewVariableElement(val variable: CfirTypeVariable) : BlockItemElement()

    class ErrorElement(val issue: CfirConstraintIssue) : BlockItemElement()

    class ConstraintElement(
        val constraint: CfirConstraint,
        val formatted: String,
    ) : BlockItemElement()

    class FixVariableElement(
        val variable: CfirTypeVariable,
        val resultType: ConeCangJieType,
    ) : BlockItemElement()

    // ---- 状态 ----

    val topLevelElements: MutableList<BlockElement> = mutableListOf()

    private var currentSystem: CfirConstraintSystem? = null
    private var currentCandidate: BlockOwner.CandidateOwner? = null
    private lateinit var currentBlock: BlockElement

    private val systemToBlock: MutableMap<CfirConstraintSystem, BlockElement> = mutableMapOf()
    private val systemToCandidate: MutableMap<CfirConstraintSystem, BlockOwner.CandidateOwner> = mutableMapOf()

    // ---- 公开 API ----

    /**
     * 标记当前正在处理的候选项。
     */
    fun logCandidate(candidate: CfirCandidate) {
        val owner = BlockOwner.CandidateOwner(candidate)
        val cs = candidate.constraintSystem ?: return
        systemToCandidate[cs] = owner
        currentCandidate = owner
        currentSystem = cs
    }

    /**
     * 开始一个新的日志阶段。
     */
    fun logStage(name: String, system: CfirConstraintSystem) {
        updateCurrentSystem(system)
        currentBlock = BlockElement(name, owner = currentCandidate ?: BlockOwner.Unknown).apply {
            register(system)
        }
    }

    /**
     * 记录新类型变量的注册。
     */
    fun logNewVariable(variable: CfirTypeVariable, system: CfirConstraintSystem) {
        prepareProperBlock(system)
        currentBlock.items += NewVariableElement(variable)
    }

    /**
     * 记录约束的添加。
     */
    fun logConstraint(constraint: CfirConstraint, system: CfirConstraintSystem) {
        prepareProperBlock(system)
        currentBlock.items += ConstraintElement(constraint, formatConstraint(constraint))
    }

    /**
     * 记录约束系统错误。
     */
    fun logError(issue: CfirConstraintIssue, system: CfirConstraintSystem) {
        prepareProperBlock(system)
        currentBlock.items += ErrorElement(issue)
    }

    /**
     * 记录类型变量的固定。
     */
    fun logFixVariable(variable: CfirTypeVariable, resultType: ConeCangJieType, system: CfirConstraintSystem) {
        prepareProperBlock(system)
        currentBlock.items += FixVariableElement(variable, resultType)
    }

    // ---- 内部方法 ----

    private fun updateCurrentSystem(system: CfirConstraintSystem) {
        if (system == currentSystem) return
        currentSystem = system
        currentCandidate = systemToCandidate[system]
    }

    private fun prepareProperBlock(system: CfirConstraintSystem) {
        if (system == currentSystem && ::currentBlock.isInitialized) return
        updateCurrentSystem(system)

        val knownBlock = systemToBlock[system]
        val blockName = when {
            knownBlock != null -> "Continue ${knownBlock.name}"
            else -> "Unknown System"
        }
        currentBlock = BlockElement(blockName, owner = currentCandidate ?: BlockOwner.Unknown).apply {
            register(system)
        }
    }

    private fun BlockElement.register(system: CfirConstraintSystem) {
        // 移除末尾空块
        if (topLevelElements.lastOrNull()?.items?.isEmpty() == true) {
            topLevelElements.removeLast()
        }
        systemToBlock[system] = this
        topLevelElements += this
    }

    companion object {
        /**
         * 格式化约束为可读字符串。
         */
        fun formatConstraint(constraint: CfirConstraint): String = when (constraint) {
            is CfirConstraint.Subtype -> "${constraint.lower} <: ${constraint.upper}"
            is CfirConstraint.Equality -> "${constraint.left} == ${constraint.right}"
            is CfirConstraint.Compatibility -> "${constraint.source} ~> ${constraint.target}"
            is CfirConstraint.ExpectedType -> "${constraint.actual} <=expected=> ${constraint.expected}"
            is CfirConstraint.UpperBound -> "${constraint.variable.lookupTag.name} <: ${constraint.bound}"
        }
    }
}

/**
 * 从 CfirSession 获取可选的推理日志记录器。
 */
val CfirSession.inferenceLogger: CfirInferenceLogger? by CfirSession.nullableSessionComponentAccessor()
