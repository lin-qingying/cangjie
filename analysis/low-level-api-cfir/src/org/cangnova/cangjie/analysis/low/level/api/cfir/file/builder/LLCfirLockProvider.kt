/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder

import com.intellij.openapi.util.registry.Registry
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.partialBodyAnalysisState
import org.cangnova.cangjie.analysis.low.level.api.cfir.transformers.PartialBodyAnalysisSuspendedException
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.LLPhaseSuspensionEventCompleter
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.LLFlightRecorder
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.checkCanceled
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.lockWithPCECheck
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater
import java.util.concurrent.locks.ReentrantLock

/**
 * This class is responsible for the locking strategy in the lazy resolution mode.
 * Each [CfirElementWithResolveState] have [CfirResolveState] which is used by this provider
 * to build the lock system.
 *
 * @see withWriteLock
 * @see withReadLock
 * @see withJumpingLock
 */
internal class LLCfirLockProvider {
    private val globalLock = ReentrantLock()

    inline fun <R> withGlobalLock(action: () -> R): R {
        if (!globalLockEnabled) return action()

        return globalLock.lockWithPCECheck(action)
    }

    /**
     * Locks an a [CfirElementWithResolveState] to resolve from `phase - 1` to [phase] and
     * then updates the [CfirElementWithResolveState.resolveState] to a [phase].
     * Does nothing if [target] already has at least [phase] phase.
     *
     * [action] will be executed once if [target] is not yet resolved to [phase] phase.
     *
     * @see withReadLock
     * @see withJumpingLock
     */
    inline fun withWriteLock(
        target: CfirElementWithResolveState,
        phase: CfirResolvePhase,
        action: () -> Unit,
    ) {
        target.withLock(toPhase = phase, updatePhase = true, action = action)
    }

    /**
     * Locks an a [CfirElementWithResolveState] to read something required for [phase].
     * Does nothing if [target] already has at least [phase] phase.
     *
     * [action] will be executed once if [target] is not yet resolved to [phase] phase.
     *
     * @see withWriteLock
     */
    inline fun withReadLock(
        target: CfirElementWithResolveState,
        phase: CfirResolvePhase,
        action: () -> Unit,
    ) {
        target.withLock(toPhase = phase, updatePhase = false, action = action)
    }

    /**
     * Locks an a [CfirElementWithResolveState] to resolve from `toPhase - 1` to [toPhase] and
     * then updates the [CfirElementWithResolveState.resolveState] to a [toPhase] if [updatePhase] is **true**.
     *
     * [updatePhase] == false means that we want to read some data under a lock.
     *
     * If [CfirElementWithResolveState] is already at least at [toPhase], does nothing.
     *
     * Otherwise:
     *  - Marks [CfirElementWithResolveState] as in a process of resovle
     *  - performs the resolve by calling [action]
     *  - updates the resolve phase to [toPhase] if [updatePhase] is **true**.
     *  - notifies other threads waiting on the same lock that the declaration is already resolved by this thread, so other threads can continue its execution.
     *
     *
     *  Contention handling:
     *  - on lock acquisition, no real lock or barrier is created. Instead, the [CfirElementWithResolveState.resolveState] is updated to indicate that the declaration is being resolved now.
     *  - If some other thread tries to resolve current [CfirElementWithResolveState], it changes `resolveState` and puts the barrier there. Then it awaits on it until the initial thread which hold the lock finishes its job.
     *  - This way, no barrier is used in a case when no contention arise.
     */
    private inline fun CfirElementWithResolveState.withLock(
        toPhase: CfirResolvePhase,
        updatePhase: Boolean,
        action: () -> Unit,
    ) {
        var event: LLPhaseSuspensionEventCompleter? = null

        while (true) {
            checkCanceled()

            @OptIn(ResolveStateAccess::class)
            val stateSnapshot = resolveState
            if (stateSnapshot.resolvePhase >= toPhase) {
                // already resolved by some other thread
                event?.notifyCompleted()
                return
            }

            when (stateSnapshot) {
                is CfirInProcessOfResolvingToPhaseStateWithoutBarrier -> {
                    // some thread is resolving the phase, so we wait until it finishes
                    if (event == null && updatePhase) {
                        event = LLFlightRecorder.phaseSuspension(this@withLock, toPhase)
                    }
                    trySettingBarrier(toPhase, stateSnapshot)
                    continue
                }

                is CfirInProcessOfResolvingToPhaseStateWithBarrier -> {
                    // some thread is waiting on a barrier as the declaration is being resolved, so we try too
                    if (event == null && updatePhase) {
                        event = LLFlightRecorder.phaseSuspension(this@withLock, toPhase)
                    }
                    waitOnBarrier(stateSnapshot)
                    continue
                }

                is CfirResolvedToPhaseState -> {
                    if (!tryLock(toPhase, stateSnapshot)) continue
                    event?.notifyCompleted()

                    var exceptionOccurred = false
                    try {
                        action()
                    } catch (e: Throwable) {
                        if (e !is PartialBodyAnalysisSuspendedException) {
                            // Partial body analysis is complete (and successful), there is no real error
                            exceptionOccurred = true
                        }
                        throw e
                    } finally {
                        val newPhase = when {
                            !updatePhase || exceptionOccurred -> stateSnapshot.resolvePhase
                            else -> computeNewPhase(stateSnapshot, toPhase)
                        }
                        unlock(toPhase = newPhase)
                    }

                    return
                }

                is CfirInProcessOfResolvingToJumpingPhaseState -> {
                    errorWithCfirSpecificEntries("$stateSnapshot state are not allowed to be inside non-jumping lock", fir = this)
                }
            }
        }
    }

    private fun CfirElementWithResolveState.computeNewPhase(stateSnapshot: CfirResolveState, toPhase: CfirResolvePhase): CfirResolvePhase {
        if (this is CfirDeclaration && toPhase == CfirResolvePhase.BODY_RESOLVE) {
            val state = partialBodyAnalysisState
            if (state != null && !state.isFullyAnalyzed) {
                LLFlightRecorder.partialBodyAnalyzed(this, state)

                // We only update the phase to BODY_RESOLVE if all statements are resolved.
                // Otherwise, we set (BODY_RESOLVE - 1), so the next BODY_RESOLVE phase run can finish the analysis.
                return stateSnapshot.resolvePhase
            }
        }

        return toPhase
    }

    private fun waitOnBarrier(
        stateSnapshot: CfirInProcessOfResolvingToPhaseStateWithBarrier,
    ): Boolean {
        return stateSnapshot.barrier.await(lockingInterval, TimeUnit.MILLISECONDS)
    }

    private fun CfirElementWithResolveState.trySettingBarrier(
        toPhase: CfirResolvePhase,
        stateSnapshot: CfirResolveState,
    ) {
        val newState = CfirInProcessOfResolvingToPhaseStateWithBarrier(toPhase)
        resolveStateFieldUpdater.compareAndSet(this, stateSnapshot, newState)
    }

    private fun CfirElementWithResolveState.tryLock(
        toPhase: CfirResolvePhase,
        stateSnapshot: CfirResolveState,
    ): Boolean {
        val newState = CfirInProcessOfResolvingToPhaseStateWithoutBarrier(toPhase)
        return resolveStateFieldUpdater.compareAndSet(this, stateSnapshot, newState)
    }

    private fun CfirElementWithResolveState.unlock(toPhase: CfirResolvePhase) {
        when (val stateSnapshotAfter = resolveStateFieldUpdater.getAndSet(this, CfirResolvedToPhaseState(toPhase))) {
            is CfirInProcessOfResolvingToPhaseStateWithoutBarrier -> {}
            is CfirInProcessOfResolvingToPhaseStateWithBarrier -> {
                stateSnapshotAfter.barrier.countDown()
            }
            is CfirResolvedToPhaseState, is CfirInProcessOfResolvingToJumpingPhaseState -> {
                errorWithCfirSpecificEntries("phase is unexpectedly unlocked $stateSnapshotAfter", fir = this)
            }
        }
    }

    /**
     * Locks on an a [CfirElementWithResolveState] to resolve from `phase - 1` to [phase] and
     * then updates the [resolve state][CfirElementWithResolveState.resolveState] to a [phase].
     * Does nothing if [target] already has at least [phase] phase.
     *
     * @param actionUnderLock will be executed once under the lock if [target] is not yet resolved to [phase] phase and there are no cycles
     * @param actionOnCycle will be executed once without the lock if [target] is not yet resolved to [phase] phase and a resolution cycle is found
     *
     * @see withWriteLock
     * @see withJumpingLockImpl
     */
    fun withJumpingLock(
        target: CfirElementWithResolveState,
        phase: CfirResolvePhase,
        actionUnderLock: () -> Unit,
        actionOnCycle: () -> Unit,
    ) {
        target.withJumpingLockImpl(phase, actionUnderLock, actionOnCycle)
    }

    /**
     * Holds resolution states of the current thread.
     * This information is required to properly process possible cycles
     * during resolution.
     *
     * @see withJumpingLockImpl
     * @see tryJumpingLock
     * @see jumpingUnlock
     */
    private val jumpingResolutionStatesStack = JumpingResolutionStatesStack()

    /**
     * Locks an a [CfirElementWithResolveState] to resolve from `toPhase - 1` to [toPhase] and
     * then updates the [CfirElementWithResolveState.resolveState] to a
     * [toPhase] if no exceptions were found during [actionUnderLock].
     *
     * If [CfirElementWithResolveState] is already at least at [toPhase], does nothing.
     *
     * ### Happy path:
     *  1. Marks [CfirElementWithResolveState] as in a process of resolve
     *  2. Performs the resolve by calling [actionUnderLock]
     *  3. Updates the resolve phase to [toPhase] if there is no exceptions
     *  4. Notifies other threads waiting on the same lock that this thread already resolved the declaration,
     *  so other threads can continue its execution
     *
     *  ### Cycle handling
     *  During step 1 we can realize someone already set [CfirInProcessOfResolvingToJumpingPhaseState]
     *  for the current [CfirElementWithResolveState], so there is a room for a possible deadlock.
     *
     *  The requirement for the deadlock is not empty [jumpingResolutionStatesStack] as we should already hold another lock.
     *  Otherwise, we can just wait on the [latch][CfirInProcessOfResolvingToJumpingPhaseState.latch].
     *
     *  In the case of not empty [jumpingResolutionStatesStack], we have the following algorithm:
     *  1. Set [waitingFor][CfirInProcessOfResolvingToJumpingPhaseState.waitingFor] for the previous state
     *  as we have an intention to take the next lock
     *  2. Iterate over all [waitingFor][CfirInProcessOfResolvingToJumpingPhaseState.waitingFor] recursively
     *  to detect the possible cycle
     *  3. Execute [actionOnCycle] without the lock in the case of cycle or waining on
     *  the [latch][CfirInProcessOfResolvingToJumpingPhaseState.latch] to try to take the lock again later
     *
     * @param actionUnderLock will be executed once under the lock if [this] is not yet resolved to [toPhase] phase and there are no cycles
     * @param actionOnCycle will be executed once without the lock if [this] is not yet resolved to [toPhase] phase and a resolution cycle is found
     *
     *  @see withJumpingLock
     */
    private fun CfirElementWithResolveState.withJumpingLockImpl(
        toPhase: CfirResolvePhase,
        actionUnderLock: () -> Unit,
        actionOnCycle: () -> Unit,
    ) {
        var event: LLPhaseSuspensionEventCompleter? = null

        while (true) {
            checkCanceled()

            @OptIn(ResolveStateAccess::class)
            val currentState = resolveState
            if (currentState.resolvePhase >= toPhase) {
                // already resolved by some other thread
                event?.notifyCompleted()
                return
            }

            when (currentState) {
                is CfirResolvedToPhaseState -> {
                    if (!tryJumpingLock(toPhase, currentState)) continue
                    event?.notifyCompleted()

                    var exceptionOccurred = false
                    try {
                        actionUnderLock()
                    } catch (e: Throwable) {
                        exceptionOccurred = true
                        throw e
                    } finally {
                        val newPhase = if (!exceptionOccurred) toPhase else currentState.resolvePhase
                        jumpingUnlock(toPhase = newPhase)
                    }

                    return
                }

                is CfirInProcessOfResolvingToJumpingPhaseState -> {
                    val previousState = jumpingResolutionStatesStack.peek()

                    // Not null value means we already hold a lock for another declaration in the current thread,
                    // so we have to check the possible cycle
                    if (previousState != null) {
                        // All writing to waitingFor will be consistent, as it is the last writing if we have cycle
                        previousState.waitingFor = currentState

                        // Cycle check
                        var nextState: CfirInProcessOfResolvingToJumpingPhaseState? = currentState
                        while (nextState != null) {
                            if (nextState === previousState) {
                                previousState.waitingFor = null

                                event?.notifyCompleted()
                                return actionOnCycle()
                            }

                            nextState = nextState.waitingFor
                        }
                    }

                    if (event == null) {
                        event = LLFlightRecorder.phaseSuspension(this, toPhase)
                    }

                    try {
                        // Waiting until another thread released the lock
                        currentState.latch.await(lockingInterval, TimeUnit.MILLISECONDS)
                    } finally {
                        previousState?.waitingFor = null
                    }
                }

                is CfirInProcessOfResolvingToPhaseStateWithoutBarrier, is CfirInProcessOfResolvingToPhaseStateWithBarrier -> {
                    errorWithCfirSpecificEntries("$currentState state are not allowed to be inside jumping lock", fir = this)
                }
            }
        }
    }

    /**
     * Trying to set [CfirInProcessOfResolvingToJumpingPhaseState] to [this].
     *
     * @return **true** if the state is published successfully
     *
     * @see withJumpingLockImpl
     * @see CfirInProcessOfResolvingToJumpingPhaseState
     */
    private fun CfirElementWithResolveState.tryJumpingLock(
        toPhase: CfirResolvePhase,
        stateSnapshot: CfirResolveState,
    ): Boolean {
        val newState = CfirInProcessOfResolvingToJumpingPhaseState(toPhase)
        val isSucceed = resolveStateFieldUpdater.compareAndSet(this, stateSnapshot, newState)
        if (!isSucceed) return false

        jumpingResolutionStatesStack.push(newState)

        return true
    }

    /**
     * Publish [CfirResolvedToPhaseState] with [toPhase] phase and unlocks current [CfirInProcessOfResolvingToJumpingPhaseState].
     *
     * @see withJumpingLockImpl
     * @see CfirInProcessOfResolvingToJumpingPhaseState
     * @see CfirResolvedToPhaseState
     */
    private fun CfirElementWithResolveState.jumpingUnlock(toPhase: CfirResolvePhase) {
        val currentState = jumpingResolutionStatesStack.pop()

        resolveStateFieldUpdater.set(this, CfirResolvedToPhaseState(toPhase))
        currentState.latch.countDown()
    }

    companion object {
        val lockingInterval: Long by lazy(LazyThreadSafetyMode.PUBLICATION) {
            Registry.intValue("kotlin.analysis.ll.locking.interval", 100).toLong()
        }
    }
}

private val resolveStateFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(
    CfirElementWithResolveState::class.java,
    CfirResolveState::class.java,
    "resolveState"
)

private val globalLockEnabled: Boolean by lazy(LazyThreadSafetyMode.PUBLICATION) {
    Registry.`is`("kotlin.parallel.resolve.under.global.lock", false)
}

/**
 * @see CfirInProcessOfResolvingToJumpingPhaseState
 */
private class JumpingResolutionStatesStack {
    private val stateStackHolder = ThreadLocal.withInitial<MutableList<CfirInProcessOfResolvingToJumpingPhaseState>> {
        mutableListOf()
    }

    /**
     * Adds [newState] to the stack and set [waitingFor][CfirInProcessOfResolvingToJumpingPhaseState.waitingFor]
     * for the previous state if needed
     */
    fun push(newState: CfirInProcessOfResolvingToJumpingPhaseState) {
        val states = stateStackHolder.get()

        val currentState = states.lastOrNull()
        currentState?.waitingFor = newState
        states += newState
    }

    /**
     * Pops from the top of the stack the last state and return it.
     * Updates [waitingFor][CfirInProcessOfResolvingToJumpingPhaseState.waitingFor] for
     * the previous state if needed
     *
     * Note: it doesn't release the [lock][CfirInProcessOfResolvingToJumpingPhaseState.latch]
     */
    fun pop(): CfirInProcessOfResolvingToJumpingPhaseState {
        val states = stateStackHolder.get()

        val currentState = states.removeLast()
        val prevState = states.lastOrNull()
        requireWithAttachment(
            condition = prevState == null || prevState.waitingFor === currentState,
            message = { "The lock contact is violated" },
        )

        prevState?.waitingFor = null

        // Drop the empty stack to avoid memory leak
        // as the updated capacity of the stack can be high
        if (states.isEmpty()) {
            stateStackHolder.remove()
        }

        return currentState
    }

    /**
     * Current state on the top if exists
     */
    fun peek(): CfirInProcessOfResolvingToJumpingPhaseState? = stateStackHolder.get().lastOrNull()
}
