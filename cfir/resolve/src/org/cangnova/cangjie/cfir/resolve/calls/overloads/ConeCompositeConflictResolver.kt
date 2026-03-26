package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate

class ConeCompositeConflictResolver(
    private vararg val conflictResolvers: ConeCallConflictResolver
) : ConeCallConflictResolver() {
    override fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
    ): Set<Candidate> {
        if (candidates.size <= 1) return candidates
        var currentCandidates = candidates
        var index = 0
        while (currentCandidates.size > 1 && index < conflictResolvers.size) {
            val conflictResolver = conflictResolvers[index++]
            currentCandidates = conflictResolver.chooseMaximallySpecificCandidates(currentCandidates)
        }
        return currentCandidates
    }
}
