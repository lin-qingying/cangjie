/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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

private const val CANGJIE_CODE_ANALYSIS_EVENT_CATEGORY = "CangJie Code Analysis"


object LLFlightRecorder {
    private val includePhaseTraces: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
        System.getProperty("cangjie.analysis.jfr.includePhaseTraces") == "true"
                || System.getenv("CANGJIE_ANALYSIS_JFR_INCLUDE_PHASE_TRACES") == "true"
    }

    private val phaseEventType = EventType.getEventType(LLPhaseEvent::class.java)
    private val phaseWithTraceEventType = EventType.getEventType(LLPhaseWithTraceEvent::class.java)

    /**
     * Notify that the [target] declaration was successfully analyzed up to the given [phase] (possibly partially).
     *
     * @param target The declaration being analyzed.
     * @param containingDeclarations The list of declarations enclosing [target] starting from the [CfirFile].
     * @param phase The phase the declaration was analyzed to.
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

    private val partialBodyAnalysisEventType = EventType.getEventType(LLPartialBodyAnalysisEvent::class.java)

    /**
     * Notify that the [declaration]'s body is analyzed partially.
     *
     * @param declaration The declaration analyzed partially.
     * @param state The current partial analysis state of the [declaration].
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

    private val readyPhaseEventType = EventType.getEventType(LLReadyPhaseEvent::class.java)

    /**
     * Notify that the [target] declaration was required to be analyzed up to the given [phase].
     * However, the declaration already reached it, so no work has been performed.
     *
     * Use `readyPhase(target, containingDeclarations, requestedPhase, withCallableMembers)` when you have the list of containing
     * declarations, e.g., from a [org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation].
     *
     * @param target The declaration being analyzed.
     * @param phase The phase the declaration is already analyzed to.
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
     * Notify that the [target] declaration was required to be analyzed up to the given [phase].
     * However, the declaration already reached it, so no work has been performed.
     *
     * @param target The declaration being analyzed.
     * @param containingDeclarations The list of declarations enclosing [target] starting from the [CfirFile].
     * @param phase The phase the declaration is already analyzed to.
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

    private val phaseSuspensionEventType = EventType.getEventType(LLPhaseSuspensionEvent::class.java)

    /**
     * Notify that the current thread acknowledged the [declaration] is either finished analyzing up to [phase],
     * or got an exception, such as [com.intellij.openapi.progress.ProcessCanceledException].
     *
     * @param declaration The analyzed declaration.
     * @param phase The phase the [declaration] is being analyzed to.
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

    private val stopWorldInvalidationEventType = EventType.getEventType(LLStopWorldInvalidation::class.java)

    /**
     * Notify that a stop-the-world session invalidation has been scheduled.
     */
    fun stopWorldSessionInvalidationScheduled() {
        stopWorldSessionInvalidation(newState = true)
    }

    /**
     * Notify that a stop-the-world session invalidation has been completed (either after being scheduled, or immediately).
     */
    fun stopWorldSessionInvalidationComplete() {
        stopWorldSessionInvalidation(newState = false)
    }

    private fun stopWorldSessionInvalidation(newState: Boolean) {
        if (!stopWorldInvalidationEventType.isEnabled) {
            return
        }

        LLStopWorldInvalidation(state = newState).commit()
    }

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

    private fun signature(declaration: CfirFunction): String {
        return declaration.valueParameters.joinToString(",") { it.name.asString() }
    }

    private fun path(containingDeclarations: List<CfirDeclaration>, target: CfirElementWithResolveState): String = buildString {
        for (entry in containingDeclarations) {
            append(name(entry))
            append(":")
        }
        append(name(target))
    }
}

private fun computeModuleKind(target: CfirElementWithResolveState): Byte {
    val moduleData = target.moduleData as LLCfirModuleData
    return when (moduleData.caModule) {
        is CaSourceModule -> 0
        is CaDanglingFileModule -> 1
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
            CfirResolvePhase.MACRO_EXPAND -> 14
            CfirResolvePhase.EXTENSIONS -> 15
            CfirResolvePhase.IMPLICIT_TYPES -> 16
        }
    }
}

internal interface LLPhaseEventCompleter {
    fun notifyCompleted()
    fun notifyCompletedWithFailure(throwable: Throwable)
}

@Suppress("unused")
@Name("org.cangnova.cangjie.LLPhase")
@Category(CANGJIE_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("CangJie Declaration Phase Execution")
@Description("A CangJie declaration is analyzed to the specified CFIR resolution phase (either successfully or with an error)")
@StackTrace(false)
private class LLPhaseEvent(
    @Label("Designation Path")
    private val path: String,

    @Label("Declaration Hash")
    private val hash: Int,

    @Label("Phase")
    private val phase: Byte,

    @Label("Module Kind")
    private val moduleKind: Byte
) : LLAbstractPhaseEvent() {
    @Label("Execution Result")
    @Description("0 - Success, 1 - Cancellation, 2 - Exception")
    override var result: Byte = -1
}

@Suppress("unused")
@Name("org.cangnova.cangjie.LLPhaseWithTrace")
@Category(CANGJIE_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("CangJie Declaration Phase Execution")
@Description("A CangJie declaration is analyzed to the specified CFIR resolution phase (either successfully or with an error)")
@StackTrace(true)
private class LLPhaseWithTraceEvent(
    @Label("Designation Path")
    private val path: String,

    @Label("Declaration Hash")
    private val hash: Int,

    @Label("Phase")
    private val phase: Byte,

    @Label("Module Kind")
    private val moduleKind: Byte
) : LLAbstractPhaseEvent() {
    @Label("Execution Result")
    @Description("0 - Success, 1 - Cancellation, 2 - Exception")
    override var result: Byte = -1
}

private abstract class LLAbstractPhaseEvent : Event(), LLPhaseEventCompleter {
    protected abstract var result: Byte

    override fun notifyCompleted() {
        result = 0
        end()
        commit()
    }

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
private class LLPartialBodyAnalysisEvent(
    @Label("Declaration Hash")
    private val hash: Int,

    @Label("Analyzed Statement Count")
    private val count: Int,

    @Label("Analysis Attempt Number")
    private val attempt: Int
) : Event()

@Suppress("unused")
@Enabled(false) // The event is disabled by default due to the huge number of events
@Name("org.cangnova.cangjie.LLReadyPhase")
@Category(CANGJIE_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("Ready CangJie Declaration Analysis")
@Description("A CangJie declaration is requested to be analyzed, yet the analysis have been already done")
@StackTrace(false)
private class LLReadyPhaseEvent(
    @Label("Designation path")
    private val path: String,

    @Label("Declaration Hash")
    private val hash: Int,

    @Label("Module Kind")
    private val moduleKind: Byte,

    @Label("Phase")
    private val phase: Byte
) : Event()

internal interface LLPhaseSuspensionEventCompleter {
    fun notifyCompleted()
}

@Suppress("unused")
@Name("org.cangnova.cangjie.LLPhaseSuspension")
@Category(CANGJIE_CODE_ANALYSIS_EVENT_CATEGORY)
@Label("Suspended CangJie Declaration Analysis")
@Description("A CangJie declaration analysis was suspended, as the other thread was already progressing with the same analysis")
@StackTrace(false)
private class LLPhaseSuspensionEvent(
    @Label("Declaration Hash")
    private val hash: Int,

    @Label("Phase")
    private val phase: Byte
) : Event(), LLPhaseSuspensionEventCompleter {
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
private class LLStopWorldInvalidation(
    @Label("Invalidation State")
    @Description("If true, the invalidation has been requested, otherwise it has completed")
    private val state: Boolean
) : Event()
