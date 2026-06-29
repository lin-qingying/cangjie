

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import jdk.jfr.*
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaDanglingFileModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryFallbackDependenciesModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.LLPartialBodyAnalysisState
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.LLCfirResolveDesignationCollector
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.PartialBodyAnalysisSuspendedException
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirPropertyAccessor
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.util.classId
import org.cangnova.cangjie.utils.exceptions.shouldIjPlatformExceptionBeRethrown

/**
 * low-level 代码分析 JFR 事件分类名。
 */
private const val CANGJIE_CODE_ANALYSIS_EVENT_CATEGORY = "CangJie Code Analysis"


/**
 * low-level CFIR 分析的 JFR 事件记录器。
 */
object LLFlightRecorder {
    /**
     * 是否记录带堆栈的阶段事件。
     */
    private val includePhaseTraces: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
        System.getProperty("cangjie.analysis.jfr.includePhaseTraces") == "true"
                || System.getenv("CANGJIE_ANALYSIS_JFR_INCLUDE_PHASE_TRACES") == "true"
    }

    /**
     * 不带堆栈的阶段执行事件类型。
     */
    private val phaseEventType = EventType.getEventType(LLPhaseEvent::class.java)
    /**
     * 带堆栈的阶段执行事件类型。
     */
    private val phaseWithTraceEventType = EventType.getEventType(LLPhaseWithTraceEvent::class.java)

    /**
     * 记录 [target] 开始推进到 [requestedPhase] 的阶段事件。
     *
     * @param target 正在分析的声明或元素。
     * @param containingDeclarations 从文件开始包围 [target] 的声明列表。
     * @param requestedPhase 目标解析阶段。
     */
    internal fun phase(
        target: CfirElementWithResolveState,
        containingDeclarations: List<CfirDeclaration>,
        requestedPhase: CfirResolvePhase
    ): LLPhaseEventCompleter? {
        if (includePhaseTraces) {
            if (!phaseWithTraceEventType.isEnabled) {
                return null
            }

            return LLPhaseWithTraceEvent(
                path = path(containingDeclarations, target),
                hash = System.identityHashCode(target),
                phase = PHASE_COMPACT_NAMES[requestedPhase.ordinal],
                moduleKind = computeModuleKind(target)
            ).apply {
                begin()
            }
        } else {
            if (!phaseEventType.isEnabled) {
                return null
            }

            return LLPhaseEvent(
                path = path(containingDeclarations, target),
                hash = System.identityHashCode(target),
                phase = PHASE_COMPACT_NAMES[requestedPhase.ordinal],
                moduleKind = computeModuleKind(target)
            ).apply {
                begin()
            }
        }
    }

    /**
     * 局部 body 分析事件类型。
     */
    private val partialBodyAnalysisEventType = EventType.getEventType(LLPartialBodyAnalysisEvent::class.java)

    /**
     * 记录 [declaration] 的 body 完成一次局部分析。
     *
     * @param declaration 被局部分析的声明。
     * @param state 当前局部分析状态。
     */
    internal fun partialBodyAnalyzed(declaration: CfirElementWithResolveState, state: LLPartialBodyAnalysisState) {
        if (!partialBodyAnalysisEventType.isEnabled) {
            return
        }

        LLPartialBodyAnalysisEvent(
            hash = System.identityHashCode(declaration),
            count = state.analyzedPsiStatementCount,
            attempt = state.performedAnalysesCount
        ).commit()
    }

    /**
     * 已就绪阶段事件类型。
     */
    private val readyPhaseEventType = EventType.getEventType(LLReadyPhaseEvent::class.java)

    /**
     * 记录 [target] 被请求推进到 [requestedPhase]，但目标已经处于该阶段或更高阶段。
     *
     * 当调用方已经持有 containing declarations 时，应使用另一个重载以避免重新收集 designation。
     */
    internal fun readyPhase(target: CfirElementWithResolveState, requestedPhase: CfirResolvePhase) {
        if (!readyPhaseEventType.isEnabled) {
            return
        }

        val designation = LLCfirResolveDesignationCollector.getDesignationToResolve(target)?.designation ?: return

        LLReadyPhaseEvent(
            path = path(designation.path, target),
            hash = System.identityHashCode(target),
            phase = PHASE_COMPACT_NAMES[requestedPhase.ordinal],
            moduleKind = computeModuleKind(target)
        ).commit()
    }

    /**
     * 记录 [target] 被请求推进到 [requestedPhase]，但目标已经处于该阶段或更高阶段。
     *
     * @param containingDeclarations 从文件开始包围 [target] 的声明列表。
     */
    internal fun readyPhase(
        target: CfirElementWithResolveState,
        containingDeclarations: List<CfirDeclaration>,
        requestedPhase: CfirResolvePhase
    ) {
        if (!readyPhaseEventType.isEnabled) {
            return
        }

        LLReadyPhaseEvent(
            path = path(containingDeclarations, target),
            hash = System.identityHashCode(target),
            phase = PHASE_COMPACT_NAMES[requestedPhase.ordinal],
            moduleKind = computeModuleKind(target)
        ).commit()
    }

    /**
     * 阶段挂起事件类型。
     */
    private val phaseSuspensionEventType = EventType.getEventType(LLPhaseSuspensionEvent::class.java)

    /**
     * 记录当前线程等待其他线程完成 [declaration] 的 [requestedPhase] 阶段分析。
     *
     * 返回的 completer 用于在等待结束时提交事件。
     */
    internal fun phaseSuspension(declaration: CfirElementWithResolveState, requestedPhase: CfirResolvePhase): LLPhaseSuspensionEventCompleter? {
        if (!phaseSuspensionEventType.isEnabled) {
            return null
        }

        return LLPhaseSuspensionEvent(
            hash = System.identityHashCode(declaration),
            phase = PHASE_COMPACT_NAMES[requestedPhase.ordinal]
        ).apply {
            begin()
        }
    }

    /**
     * stop-the-world 失效事件类型。
     */
    private val stopWorldInvalidationEventType = EventType.getEventType(LLStopWorldInvalidation::class.java)

    /**
     * 记录 stop-the-world 会话失效已经被调度。
     */
    fun stopWorldSessionInvalidationScheduled() {
        stopWorldSessionInvalidation(newState = true)
    }

    /**
     * 记录 stop-the-world 会话失效已经完成。
     */
    fun stopWorldSessionInvalidationComplete() {
        stopWorldSessionInvalidation(newState = false)
    }

    /**
     * 提交 stop-the-world 会话失效状态事件。
     */
    private fun stopWorldSessionInvalidation(newState: Boolean) {
        if (!stopWorldInvalidationEventType.isEnabled) {
            return
        }

        LLStopWorldInvalidation(state = newState).commit()
    }

    /**
     * 计算 [declaration] 在 JFR 事件路径中的短名称。
     */
    private fun name(declaration: CfirElementWithResolveState): String {
        /**
         * As [name] is used as a component of [path], names must not contain colons.
         * So theoretically, we should escape/substitute all colon characters.
         * However, colons are forbidden in JVM bytecode, and overall, the chance that we find them is considerably low.
         */
        @Suppress("SpellCheckingInspection")
        return when (declaration) {
            is CfirFile -> "fl/" + declaration.name
            is CfirTypeParameter -> "tp/" + declaration.name.asString()
            is CfirTypeAlias -> "ta/" + declaration.classId.asString()
            is CfirClass -> "c/" + declaration.classId.asString()
            is CfirExtend -> "x/" + (declaration.psi?.text ?: declaration::class.simpleName ?: "<extend>")
            is CfirProperty -> "p/" + declaration.name.asString()
            is CfirValueParameter -> "vp/" + declaration.name.asString()
            is CfirVariable -> "v/" + declaration.symbol.name.asString() + "/${declaration::class.java.simpleName.lowercase()}"
            is CfirPropertyAccessor -> (if (declaration.isGetter) "pg/" else "ps/") + declaration.propertySymbol.name.asString()
            is CfirConstructor -> "ctor/" + signature(declaration)
            is CfirAnonymousFunction -> "lambda"
            is CfirNamedFunction, is CfirMainFunction -> {
                val baseName = "f/" + declaration.symbol.name.asString()
                baseName + '/' + signature(declaration)
            }
            is CfirCodeFragment -> "code"
            else -> "?/" + declaration.javaClass.simpleName
        }
    }

    /**
     * 计算函数参数签名文本。
     */
    private fun signature(declaration: CfirFunction): String {
        return declaration.valueParameters.joinToString(",") { it.name.asString() }
    }

    /**
     * 计算 [target] 的 designation 路径文本。
     */
    private fun path(containingDeclarations: List<CfirDeclaration>, target: CfirElementWithResolveState): String = buildString {
        for (entry in containingDeclarations) {
            append(name(entry))
            append(":")
        }
        append(name(target))
    }
}

/**
 * 计算 [target] 所属 Analysis API 模块的紧凑类别编号。
 */
private fun computeModuleKind(target: CfirElementWithResolveState): Byte {
    val moduleData = target.moduleData as LLCfirModuleData
    return when (moduleData.caModule) {
        is CaDanglingFileModule -> 1
        is CaSourceModule -> 0
        is CaNotUnderContentRootModule -> 2
        is CaLibraryFallbackDependenciesModule -> 3
        is CaLibraryModule -> 4
        is CaLibrarySourceModule -> 5
        is CaBuiltinsModule -> 6
        else -> -1
    }
}

/**
 *                  !!!
 * When adding or removing phases, use unused numbers.
 * Never change existing mappings!
 */
/**
 * 解析阶段到稳定 JFR 编号的映射表。
 */
private val PHASE_COMPACT_NAMES = run {
    val phases = CfirResolvePhase.entries
    ByteArray(phases.size) {
        when (phases[it]) {
            CfirResolvePhase.RAW_CFIR -> 0
            CfirResolvePhase.IMPORTS -> 1
            CfirResolvePhase.SUPER_TYPES -> 4
            CfirResolvePhase.TYPES -> 6
            CfirResolvePhase.STATUS -> 7
            CfirResolvePhase.BODY_RESOLVE -> 13
            // 14 was MACRO_EXPAND, removed: macro expansion now runs in the
            // pre-resolve construction step (baseline 第 1 节). Do not reuse 14.
            CfirResolvePhase.EXTENSIONS -> 15
            CfirResolvePhase.IMPLICIT_TYPES -> 16
        }
    }
}

/**
 * 阶段执行事件的完成回调。
 */
internal interface LLPhaseEventCompleter {
    /**
     * 标记阶段执行成功完成。
     */
    fun notifyCompleted()
    /**
     * 标记阶段执行以 [throwable] 失败或取消。
     */
    fun notifyCompletedWithFailure(throwable: Throwable)
}

@Suppress("unused")
@Name("org.cangnova.cangjie.LLPhase")
@Category(CANGJIE_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("CangJie Declaration Phase Execution")
@Description("A CangJie declaration is analyzed to the specified CFIR resolution phase (either successfully or with an error)")
@StackTrace(false)
/**
 * 不记录堆栈的声明阶段执行 JFR 事件。
 */
private class LLPhaseEvent(
    @Label("Designation Path")
    /**
     * 目标声明的 designation 路径。
     */
    private val path: String,

    @Label("Declaration Hash")
    /**
     * 目标声明对象的 identity hash。
     */
    private val hash: Int,

    @Label("Phase")
    /**
     * 目标解析阶段的紧凑编号。
     */
    private val phase: Byte,

    @Label("Module Kind")
    /**
     * 目标所属模块的紧凑类别编号。
     */
    private val moduleKind: Byte
) : LLAbstractPhaseEvent() {
    @Label("Execution Result")
    @Description("0 - Success, 1 - Cancellation, 2 - Exception")
    /**
     * 阶段执行结果编号。
     */
    override var result: Byte = -1
}

@Suppress("unused")
@Name("org.cangnova.cangjie.LLPhaseWithTrace")
@Category(CANGJIE_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("CangJie Declaration Phase Execution")
@Description("A CangJie declaration is analyzed to the specified CFIR resolution phase (either successfully or with an error)")
@StackTrace(true)
/**
 * 记录堆栈的声明阶段执行 JFR 事件。
 */
private class LLPhaseWithTraceEvent(
    @Label("Designation Path")
    /**
     * 目标声明的 designation 路径。
     */
    private val path: String,

    @Label("Declaration Hash")
    /**
     * 目标声明对象的 identity hash。
     */
    private val hash: Int,

    @Label("Phase")
    /**
     * 目标解析阶段的紧凑编号。
     */
    private val phase: Byte,

    @Label("Module Kind")
    /**
     * 目标所属模块的紧凑类别编号。
     */
    private val moduleKind: Byte
) : LLAbstractPhaseEvent() {
    @Label("Execution Result")
    @Description("0 - Success, 1 - Cancellation, 2 - Exception")
    /**
     * 阶段执行结果编号。
     */
    override var result: Byte = -1
}

/**
 * 阶段执行事件的公共基类。
 */
private abstract class LLAbstractPhaseEvent : Event(), LLPhaseEventCompleter {
    /**
     * 阶段执行结果编号。
     */
    protected abstract var result: Byte

    /**
     * 标记事件成功完成并提交。
     */
    override fun notifyCompleted() {
        result = 0
        end()
        commit()
    }

    /**
     * 根据 [throwable] 类型标记事件结果并提交。
     */
    override fun notifyCompletedWithFailure(throwable: Throwable) {
        result = when {
            throwable is PartialBodyAnalysisSuspendedException -> 0
            shouldIjPlatformExceptionBeRethrown(throwable) -> 1
            else -> 2
        }
        end()
        commit()
    }
}

@Suppress("unused")
@Name("org.cangnova.cangjie.LLPartialBodyAnalysis")
@Category(CANGJIE_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("CangJie Declaration Partial Body Analysis")
@Description("A CangJie declaration's body is analyzed up to the specified PSI statement number (inclusive)")
@StackTrace(false)
/**
 * 局部 body 分析进度 JFR 事件。
 */
private class LLPartialBodyAnalysisEvent(
    @Label("Declaration Hash")
    /**
     * 被分析声明对象的 identity hash。
     */
    private val hash: Int,

    @Label("Analyzed Statement Count")
    /**
     * 已分析 PSI 语句数量。
     */
    private val count: Int,

    @Label("Analysis Attempt Number")
    /**
     * 当前声明的局部分析尝试次数。
     */
    private val attempt: Int
) : Event()

@Suppress("unused")
@Enabled(false) // The event is disabled by default due to the huge number of events
@Name("org.cangnova.cangjie.LLReadyPhase")
@Category(CANGJIE_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("Ready CangJie Declaration Analysis")
@Description("A CangJie declaration is requested to be analyzed, yet the analysis have been already done")
@StackTrace(false)
/**
 * 请求阶段已经就绪时提交的 JFR 事件。
 */
private class LLReadyPhaseEvent(
    @Label("Designation path")
    /**
     * 目标声明的 designation 路径。
     */
    private val path: String,

    @Label("Declaration Hash")
    /**
     * 目标声明对象的 identity hash。
     */
    private val hash: Int,

    @Label("Module Kind")
    /**
     * 目标所属模块的紧凑类别编号。
     */
    private val moduleKind: Byte,

    @Label("Phase")
    /**
     * 已就绪解析阶段的紧凑编号。
     */
    private val phase: Byte
) : Event()

/**
 * 阶段挂起事件的完成回调。
 */
internal interface LLPhaseSuspensionEventCompleter {
    /**
     * 标记挂起等待完成。
     */
    fun notifyCompleted()
}

@Suppress("unused")
@Name("org.cangnova.cangjie.LLPhaseSuspension")
@Category(CANGJIE_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("Suspended CangJie Declaration Analysis")
@Description("A CangJie declaration analysis was suspended, as the other thread was already progressing with the same analysis")
@StackTrace(false)
/**
 * 等待其他线程推进同一阶段时提交的挂起事件。
 */
private class LLPhaseSuspensionEvent(
    @Label("Declaration Hash")
    /**
     * 被等待声明对象的 identity hash。
     */
    private val hash: Int,

    @Label("Phase")
    /**
     * 等待阶段的紧凑编号。
     */
    private val phase: Byte
) : Event(), LLPhaseSuspensionEventCompleter {
    /**
     * 结束并提交挂起等待事件。
     */
    override fun notifyCompleted() {
        end()
        commit()
    }
}

@Suppress("unused")
@Name("org.cangnova.cangjie.LLStopWorldInvalidation")
@Category(CANGJIE_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("Stop-the-world Session Invalidation")
@Description("Stop-the-world session invalidation either has been requested, or it has just completed")
@StackTrace(false)
/**
 * stop-the-world 会话失效状态事件。
 */
private class LLStopWorldInvalidation(
    @Label("Invalidation State")
    @Description("If true, the invalidation has been requested, otherwise it has completed")
    /**
     * `true` 表示失效已请求，`false` 表示失效已完成。
     */
    private val state: Boolean
) : Event()
