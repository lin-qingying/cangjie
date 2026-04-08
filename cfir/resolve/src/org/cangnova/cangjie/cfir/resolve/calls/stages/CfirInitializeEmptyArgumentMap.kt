package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink

/**
 * Align with Kotlin FIR `InitializeEmptyArgumentMap` for variable/name access calls.
 *
 * Variable access candidates still participate in completion and postponed-atom traversal,
 * so argument mapping must always be initialized even when the call has no argument checking.
 */
object CfirInitializeEmptyArgumentMap : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        candidate.initializeArgumentMapping(arguments = emptyList(), argumentMapping = linkedMapOf())
    }
}

