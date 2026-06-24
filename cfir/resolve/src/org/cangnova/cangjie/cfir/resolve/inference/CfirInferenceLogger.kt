package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemMarker
import org.cangnova.cangjie.resolve.calls.inference.components.InferenceLogger
import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintError
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemError
import org.cangnova.cangjie.resolve.calls.inference.model.InitialConstraint
import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.TypeVariableMarker

/**
 * CFIR 类型推断日志收集器。
 *
 * 该组件接收通用约束系统的日志回调，并把类型变量、约束、错误、固定结果按照候选与推断阶段
 * 组织成可渲染的 CFIR 日志树，供调试调用完成和 postponed argument 推断使用。
 */
open class CfirInferenceLogger : InferenceLogger(), CfirSessionComponent {
    /**
     * 推断日志树中的基础元素。
     */
    sealed class LoggingElement

    /**
     * 日志中用于标识一次 CFIR 调用或表达式的轻量视图。
     */
    data class Call(
        /** 原始 CFIR 元素。 */
        val element: CfirElement,
        /** 用于日志显示的元素名称。 */
        val render: String = element::class.simpleName ?: "CfirElement",
    )

    /**
     * 日志 block 的归属信息。
     */
    sealed class BlockOwner {
        /**
         * 归属于某个候选的日志 block。
         */
        data class CandidateOwner(
            /** 当前日志 block 归属的候选。 */
            val candidate: Candidate,
        ) : BlockOwner() {
            /** 该候选所属调用的显示视图。 */
            val owningCall: Call = Call(candidate.callInfo.callSite)
        }

        /** 无法关联到具体候选的日志 block。 */
        data object Unknown : BlockOwner()
    }

    /**
     * 一个顶层推断日志 block，通常对应某个候选的一次推断阶段。
     */
    data class BlockElement(
        /** block 名称，例如阶段名或继续记录提示。 */
        val name: String,
        /** block 内按顺序记录的约束、变量、错误和固定结果。 */
        val items: MutableList<BlockItemElement> = mutableListOf(),
        /** block 归属的候选或未知上下文。 */
        val owner: BlockOwner,
    ) : LoggingElement()

    /**
     * 日志 block 内部元素的基础类型。
     */
    sealed class BlockItemElement : LoggingElement()

    /**
     * 已记录的类型变量。
     */
    data class LoggedTypeVariable(
        /** 类型变量在日志中的名称标签。 */
        val lookupTag: LoggedLookupTag,
        /** 原始约束系统类型变量对象。 */
        val original: TypeVariableMarker,
    )

    /**
     * 日志中使用的 lookup tag 视图。
     */
    data class LoggedLookupTag(
        /** 类型变量在日志中使用的稳定名称。 */
        val name: Name,
    )

    /**
     * 约束系统错误的日志化描述。
     */
    data class ConstraintIssue(
        /** 错误文本。 */
        val message: String,
        /** 错误来源位置或错误类型名称。 */
        val position: String,
    )

    /**
     * 新注册类型变量的日志项。
     */
    data class NewVariableElement(
        /** 新注册的类型变量。 */
        val variable: LoggedTypeVariable,
    ) : BlockItemElement()

    /**
     * 约束日志项。
     */
    data class ConstraintElement(
        /** 已格式化的约束表达式。 */
        val formatted: String,
        /** 该约束由哪些既有约束派生而来。 */
        val origins: List<ConstraintElement> = emptyList(),
    ) : BlockItemElement()

    /**
     * 约束系统错误日志项。
     */
    data class ErrorElement(
        /** 约束系统错误信息。 */
        val issue: ConstraintIssue,
    ) : BlockItemElement()

    /**
     * 类型变量固定结果日志项。
     */
    data class FixVariableElement(
        /** 被固定的类型变量。 */
        val variable: LoggedTypeVariable,
        /** 固定后的类型。 */
        val resultType: CangJieTypeMarker,
    ) : BlockItemElement()

    /**
     * 按时间顺序收集到的顶层推断日志 block。
     */
    val topLevelElements: MutableList<BlockElement> = mutableListOf()

    /** 当前正在接收日志的约束系统。 */
    private var currentSystem: ConstraintSystemMarker? = null
    /** 当前正在写入的日志 block。 */
    private var currentBlock: BlockElement? = null
    /** 当前约束系统关联的候选归属。 */
    private var currentCandidate: BlockOwner.CandidateOwner? = null

    /** 每个约束系统最后一次注册的日志 block。 */
    private val systemToKnownBlock: MutableMap<ConstraintSystemMarker, BlockElement> = mutableMapOf()
    /** 每个约束系统对应的候选归属。 */
    private val systemToCandidate: MutableMap<ConstraintSystemMarker, BlockOwner.CandidateOwner> = mutableMapOf()
    /** 初始约束到日志元素的索引，用于后续派生约束追踪来源。 */
    private val initialConstraintToKnownElement = mutableMapOf<InitialConstraint, ConstraintElement>()
    /** 类型变量约束到日志元素的索引，用于 `withOrigins` 关联派生约束。 */
    private val variableConstraintToKnownElement = mutableMapOf<Pair<TypeVariableMarker, Constraint>, ConstraintElement>()

    /** 当前回调栈中正在作为派生来源的约束元素。 */
    private var origins: List<ConstraintElement> = emptyList()

    /**
     * 记录当前正在处理的候选。
     */
    fun logCandidate(candidate: Candidate) {
        if (currentCandidate?.candidate === candidate) return
        val owner = BlockOwner.CandidateOwner(candidate)
        systemToCandidate[candidate.system] = owner
        currentCandidate = owner
        currentSystem = candidate.system
    }

    /**
     * 记录一个新的推断阶段 block。
     */
    fun logStage(name: String, system: ConstraintSystemMarker) {
        updateCurrentSystem(system)
        currentBlock = registerBlock(BlockElement(name, owner = currentCandidate ?: BlockOwner.Unknown), system)
    }

    /**
     * 记录初始约束。
     */
    override fun logInitial(initialConstraint: InitialConstraint, context: Any?) {
        val system = context as? ConstraintSystemMarker ?: return
        val block = prepareProperBlock(system)
        val element = ConstraintElement(
            formatted = formatInitialConstraint(initialConstraint),
        )
        initialConstraintToKnownElement.putIfAbsent(initialConstraint, element)
        block.items += element
    }

    /**
     * 记录类型变量相关约束。
     */
    override fun log(variable: TypeVariableMarker, constraint: Constraint, context: Any?) {
        val system = context as? ConstraintSystemMarker ?: return
        val block = prepareProperBlock(system)
        val element = ConstraintElement(
            formatted = formatVariableConstraint(variable, constraint),
            origins = origins,
        )
        variableConstraintToKnownElement.putIfAbsent(variable to constraint, element)
        block.items += element
    }

    /**
     * 记录约束系统错误。
     */
    override fun logError(error: ConstraintSystemError, context: Any?) {
        val system = context as? ConstraintSystemMarker ?: return
        prepareProperBlock(system).items += ErrorElement(
            ConstraintIssue(
                message = error.toString(),
                position = extractErrorPosition(error),
            )
        )
    }

    /**
     * 记录新注册的类型变量。
     */
    override fun logNewVariable(variable: TypeVariableMarker, context: Any?) {
        val system = context as? ConstraintSystemMarker ?: return
        prepareProperBlock(system).items += NewVariableElement(variable.toLoggedTypeVariable())
    }

    /**
     * 记录类型变量 readiness 信息。
     */
    override fun logReadiness(record: FixationLogRecord, context: Any?) {
        // Readiness records are intentionally not rendered yet by the current local handler.
        // Keep the callback as a no-op until Task 5/7 converges richer block item rendering.
    }

    /**
     * 记录类型变量固定结果。
     */
    override fun logFixVariable(variable: TypeVariableMarker, resultType: CangJieTypeMarker, context: Any?) {
        val system = context as? ConstraintSystemMarker ?: return
        prepareProperBlock(system).items += FixVariableElement(variable.toLoggedTypeVariable(), resultType)
    }

    /**
     * 在执行回调期间把后续日志标记为来源于某个初始约束。
     */
    override fun <T> withOrigin(origin: Any?, action: () -> T): T {
        val initialConstraint = origin as? InitialConstraint ?: return action()
        val element = initialConstraintToKnownElement[initialConstraint] ?: return action()
        return withOriginatingElements(listOf(element), action)
    }

    /**
     * 在执行回调期间把后续日志标记为来源于两个已知变量约束。
     */
    override fun <T> withOrigins(
        firstOwner: Any?,
        firstOrigin: Any?,
        secondOwner: Any?,
        secondOrigin: Any?,
        action: () -> T,
    ): T {
        val firstVariable = firstOwner as? TypeVariableMarker
        val firstConstraint = firstOrigin as? Constraint
        val secondVariable = secondOwner as? TypeVariableMarker
        val secondConstraint = secondOrigin as? Constraint

        val firstElement = firstVariable?.let { variableConstraintToKnownElement[it to firstConstraint] }
        val secondElement = secondVariable?.let { variableConstraintToKnownElement[it to secondConstraint] }
        val elements = listOfNotNull(firstElement, secondElement)
        if (elements.isEmpty()) return action()
        return withOriginatingElements(elements, action)
    }

    /**
     * 获取适合指定约束系统继续写入的 block。
     */
    private fun prepareProperBlock(system: ConstraintSystemMarker): BlockElement {
        if (system == currentSystem) {
            return currentBlock ?: registerBlock(BlockElement("Unknown system", owner = currentCandidate ?: BlockOwner.Unknown), system)
        }

        updateCurrentSystem(system)
        val knownPreviousBlock = systemToKnownBlock[system]
        val nextBlockTitle = when {
            knownPreviousBlock != null && currentCandidate != null -> "Continue ${knownPreviousBlock.name}"
            knownPreviousBlock != null -> "Continue ${knownPreviousBlock.name}"
            else -> "Unknown system"
        }
        return registerBlock(BlockElement(nextBlockTitle, owner = currentCandidate ?: BlockOwner.Unknown), system)
    }

    /**
     * 切换当前约束系统，并恢复该系统已知的候选归属。
     */
    private fun updateCurrentSystem(system: ConstraintSystemMarker) {
        currentSystem = system
        currentCandidate = systemToCandidate[system]
    }

    /**
     * 注册新的日志 block，并维护当前 block / system 指针。
     */
    private fun registerBlock(block: BlockElement, system: ConstraintSystemMarker): BlockElement {
        if (topLevelElements.lastOrNull()?.items?.isEmpty() == true) {
            topLevelElements.removeLast()
        }
        systemToKnownBlock[system] = block
        topLevelElements += block
        currentBlock = block
        currentSystem = system
        return block
    }

    /**
     * 在回调执行期间临时设置派生约束来源。
     */
    private inline fun <T> withOriginatingElements(elements: List<ConstraintElement>, action: () -> T): T {
        val previous = origins
        return try {
            origins = elements
            action()
        } finally {
            origins = previous
        }
    }

    /**
     * 把通用类型变量转换成日志可显示的 CFIR 类型变量视图。
     */
    private fun TypeVariableMarker.toLoggedTypeVariable(): LoggedTypeVariable {
        val debugName = toString().removePrefix("ConeTypeVariableType(").removeSuffix(")")
        return LoggedTypeVariable(LoggedLookupTag(Name.identifier(debugName)), this)
    }

    /**
     * 格式化初始约束文本。
     */
    private fun formatInitialConstraint(constraint: InitialConstraint): String = constraint.asStringWithoutPosition()

    /**
     * 格式化变量约束文本。
     */
    private fun formatVariableConstraint(variable: TypeVariableMarker, constraint: Constraint): String {
        val variableName = variable.toLoggedTypeVariable().lookupTag.name
        return when (constraint.kind) {
            ConstraintKind.LOWER -> "${constraint.type} <: $variableName"
            ConstraintKind.UPPER -> "$variableName <: ${constraint.type}"
            ConstraintKind.EQUALITY -> "$variableName == ${constraint.type}"
        }
    }

    /**
     * 提取约束系统错误的来源位置。
     */
    private fun extractErrorPosition(error: ConstraintSystemError): String = when (error) {
        is ConstraintError -> error.position.toString()
        else -> error::class.simpleName ?: "ConstraintSystemError"
    }
}
