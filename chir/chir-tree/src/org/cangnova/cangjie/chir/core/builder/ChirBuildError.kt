package org.cangnova.cangjie.chir.core.builder

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId

sealed interface ChirBuildError {
    data class DuplicateSymbol(
        val symbolName: String,
        val symbolId: ChirSemanticId,
        val detail: String,
    ) : ChirBuildError

    data class UnresolvedReference(
        val referenceId: ChirSemanticId,
        val targetName: String,
    ) : ChirBuildError

    data class InvalidGraph(
        val detail: String,
    ) : ChirBuildError
}

interface ChirDiagnosticCollector {
    fun report(error: ChirBuildError)
}

object NoopChirDiagnosticCollector : ChirDiagnosticCollector {
    override fun report(error: ChirBuildError) = Unit
}

class RecordingChirDiagnosticCollector : ChirDiagnosticCollector {
    private val mutableErrors = mutableListOf<ChirBuildError>()

    val errors: List<ChirBuildError>
        get() = mutableErrors

    override fun report(error: ChirBuildError) {
        mutableErrors += error
    }
}
