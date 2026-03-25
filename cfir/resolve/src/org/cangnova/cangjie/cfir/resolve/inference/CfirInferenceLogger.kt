package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemMarker
import org.cangnova.cangjie.resolve.calls.inference.components.InferenceLogger

open class CfirInferenceLogger : InferenceLogger(), CfirSessionComponent {
    data class Call(
        val element: CfirElement,
        val render: String = element::class.simpleName ?: "CfirElement",
    )

    sealed class BlockOwner {
        data class CandidateOwner(val candidate: Candidate) : BlockOwner() {
            val owningCall: Call = Call(candidate.callInfo.callSite)
        }

        data object Unknown : BlockOwner()
    }

    data class BlockElement(
        val name: String,
        val owner: BlockOwner,
    )

    val topLevelBlocks: MutableList<BlockElement> = mutableListOf()

    private var currentSystem: ConstraintSystemMarker? = null
    private var currentCandidate: BlockOwner.CandidateOwner? = null
    private val systemToCandidate: MutableMap<ConstraintSystemMarker, BlockOwner.CandidateOwner> = mutableMapOf()

    fun logCandidate(candidate: Candidate) {
        if (currentCandidate?.candidate === candidate) return
        val owner = BlockOwner.CandidateOwner(candidate)
        systemToCandidate[candidate.system] = owner
        currentCandidate = owner
        currentSystem = candidate.system
    }

    fun logStage(name: String, system: ConstraintSystemMarker) {
        if (currentSystem !== system) {
            currentSystem = system
            currentCandidate = systemToCandidate[system]
        }
        topLevelBlocks += BlockElement(name, currentCandidate ?: BlockOwner.Unknown)
    }
}

val CfirSession.inferenceLogger: CfirInferenceLogger? by CfirSession.nullableSessionComponentAccessor()
