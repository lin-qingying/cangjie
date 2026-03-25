package org.cangnova.cangjie.cfir.resolve.calls.candidate

import org.cangnova.cangjie.cfir.resolve.calls.*

/**
 * Atom 树遍历工具。
 *
 * 对齐 K2 `CandidateTraversal.kt`，提供对调用解析 atom 树的深度优先遍历，
 * 分别处理候选和延迟 atom。
 *
 * 使用 visited set 避免在存在共享子树（如 PCLA 场景）时重复处理。
 */
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



        is ConeSimpleNameForContextSensitiveResolution -> {
            postponedAtomsProcessor(this)
        }

        is ConeContextSensitiveAlternativeForQualifierAtom -> {
            postponedAtomsProcessor(this)
        }

        is ConeResolutionAtomWithPostponedChild -> {
            val child = subAtom
            child?.processAtomRecursively(visited, candidateProcessor, postponedAtomsProcessor)
        }

        is ConeSimpleLeafResolutionAtom -> {
            // 叶子 atom，无需递归
        }
    }
}
