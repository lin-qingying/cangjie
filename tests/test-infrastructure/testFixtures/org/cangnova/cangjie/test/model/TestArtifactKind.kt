package org.cangnova.cangjie.test.model

import org.cangnova.cangjie.test.services.CompilationStage

abstract class TestArtifactKind<R : ResultingArtifact<R>>(private val representation: String) {
    open val shouldRunAnalysis: Boolean
        get() = true

    override fun toString(): String {
        return representation
    }
}

object SourcesKind : TestArtifactKind<ResultingArtifact.Source>("Sources")

abstract class FrontendKind<R : ResultingArtifact.FrontendOutput<R>>(representation: String) : TestArtifactKind<R>(representation) {
    object NoFrontend : FrontendKind<ResultingArtifact.FrontendOutput.Empty>("NoFrontend") {
        override val shouldRunAnalysis: Boolean
            get() = false
    }
}

abstract class BackendKind<I : ResultingArtifact.BackendInput<I>>(representation: String) : TestArtifactKind<I>(representation) {
    object NoBackend : BackendKind<ResultingArtifact.BackendInput.Empty>("NoBackend") {
        override val shouldRunAnalysis: Boolean
            get() = false
    }
}

abstract class ArtifactKind<A : ResultingArtifact.Binary<A>>(
    representation: String,
    val producedBy: CompilationStage,
) : TestArtifactKind<A>(representation) {
    object NoArtifact : ArtifactKind<ResultingArtifact.Binary.Empty>("NoArtifact", CompilationStage.FIRST) {
        override val shouldRunAnalysis: Boolean
            get() = false
    }
}