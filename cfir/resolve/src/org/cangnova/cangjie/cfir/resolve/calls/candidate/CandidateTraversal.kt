package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.resolve.calls.*

fun ConeResolutionAtom.processCandidatesAndPostponedAtoms(
    candidateProcessor: (Candidate) -> Unit,
    postponedAtomsProcessor: (ConePostponedResolvedAtom) -> Unit,
) {
    val visited = hashSetOf<ConeResolutionAtom>()
    processAtomRecursively(visited, candidateProcessor, postponedAtomsProcessor)
}

private fun ConeResolutionAtom.processAtomRecursively(
    visited: MutableSet<ConeResolutionAtom>,
    candidateProcessor: (Candidate) -> Unit,
    postponedAtomsProcessor: (ConePostponedResolvedAtom) -> Unit,
) {
    if (!visited.add(this)) return

    when (this) {
        is ConeAtomWithCandidate -> {
            candidateProcessor(candidate)
            candidate.dispatchReceiver?.processAtomRecursively(visited, candidateProcessor, postponedAtomsProcessor)
            candidate.givenExtensionReceiver?.processAtomRecursively(visited, candidateProcessor, postponedAtomsProcessor)
            for (argument in candidate.arguments) {
                argument.processAtomRecursively(visited, candidateProcessor, postponedAtomsProcessor)
            }
            for (pclaCall in candidate.postponedPCLACalls) {
                pclaCall.processAtomRecursively(visited, candidateProcessor, postponedAtomsProcessor)
            }
        }

        is ConeResolvedLambdaAtom -> {
            postponedAtomsProcessor(this)
            if (analyzed) {
                for (statement in returnStatements) {
                    statement.processAtomRecursively(visited, candidateProcessor, postponedAtomsProcessor)
                }
            }
        }

        is ConeLambdaWithTypeVariableAsExpectedTypeAtom -> {
            postponedAtomsProcessor(this)
            subAtom?.processAtomRecursively(visited, candidateProcessor, postponedAtomsProcessor)
        }

        is ConeResolvedCallableReferenceAtom,
        is ConeSimpleNameForContextSensitiveResolution,
        is ConeContextSensitiveAlternativeForQualifierAtom -> {
            postponedAtomsProcessor(this)
        }

        is ConeResolutionAtomWithPostponedChild -> {
            subAtom?.processAtomRecursively(visited, candidateProcessor, postponedAtomsProcessor)
        }

        is ConeResolutionAtomWithSingleChild -> {
            subAtom?.processAtomRecursively(visited, candidateProcessor, postponedAtomsProcessor)
        }

        is ConeSimpleLeafResolutionAtom -> Unit
    }
}
