package org.cangnova.cangjie.cfir.resolve.calls.candidate

import java.util.Collections
import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.resolve.calls.*

/** 遍历整个 completion atom/candidate 图；回边和重复共享节点按真实对象身份去重。 */
fun ConeResolutionAtom.processCandidatesAndPostponedAtoms(
    candidateProcessor: (Candidate) -> Unit,
    postponedAtomsProcessor: (ConePostponedResolvedAtom) -> Unit,
) {
    CandidateTraversal(candidateProcessor, postponedAtomsProcessor).visitAtom(this)
}

/** FULL 根已写回引用后仍可通过保留的 candidate 进入同一 completion 任务闭包。 */
fun Candidate.processCandidatesAndPostponedAtoms(
    candidateProcessor: (Candidate) -> Unit,
    postponedAtomsProcessor: (ConePostponedResolvedAtom) -> Unit,
) {
    CandidateTraversal(candidateProcessor, postponedAtomsProcessor).visitCandidate(this)
}

private class CandidateTraversal(
    private val candidateProcessor: (Candidate) -> Unit,
    private val postponedAtomsProcessor: (ConePostponedResolvedAtom) -> Unit,
) {
    private val atoms = identitySet<ConeResolutionAtom>()
    private val candidates = identitySet<Candidate>()

    fun visitCandidate(candidate: Candidate) {
        if (!candidates.add(candidate)) return
        candidateProcessor(candidate)
        candidate.dispatchReceiver?.let(::visitAtom)
        candidate.givenExtensionReceiver?.let(::visitAtom)
        candidate.chosenExtensionReceiver?.let(::visitAtom)
        candidate.contextArguments?.forEach(::visitAtom)
        candidate.arguments.forEach(::visitAtom)
        candidate.postponedAtoms.forEach(::visitAtom)
        candidate.postponedPCLACalls.forEach(::visitAtom)
    }

    fun visitAtom(atom: ConeResolutionAtom) {
        if (!atoms.add(atom)) return
        when (atom) {
            is ConeAtomWithCandidate -> visitCandidate(atom.candidate)
            is ConeResolvedLambdaAtom -> {
                postponedAtomsProcessor(atom)
                if (atom.analyzed) atom.returnStatements.forEach(::visitAtom)
            }
            is ConeLambdaWithTypeVariableAsExpectedTypeAtom -> {
                postponedAtomsProcessor(atom)
                atom.subAtom?.let(::visitAtom)
            }
            is ConeResolvedCallableReferenceAtom -> {
                postponedAtomsProcessor(atom)
                (atom.resultingReference as? CfirNamedReferenceWithCandidate)?.candidate?.let(::visitCandidate)
            }
            is ConeSimpleNameForContextSensitiveResolution,
            is ConeContextSensitiveAlternativeForQualifierAtom -> postponedAtomsProcessor(atom as ConePostponedResolvedAtom)
            is ConeResolutionAtomWithPostponedChild -> atom.subAtom?.let(::visitAtom)
            is ConeResolutionAtomWithSingleChild -> atom.subAtom?.let(::visitAtom)
            is ConeSimpleLeafResolutionAtom -> Unit
        }
    }

    private fun <T : Any> identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap<T, Boolean>())
}
