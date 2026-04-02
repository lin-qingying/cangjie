package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.CangjieLspEnvironment
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisFacade
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisLifecycleContext
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisRequestContext
import org.cangnova.cangjie.lsp.capabilities.CangjieLanguageServerDescriptor
import org.cangnova.cangjie.lsp.capabilities.CangjieLspFeatureSet
import org.cangnova.cangjie.lsp.state.LspDocumentStore
import org.cangnova.cangjie.lsp.state.LspWorkspaceState
import org.eclipse.lsp4j.services.LanguageClient

class CangjieServerContext(
    val descriptor: CangjieLanguageServerDescriptor,
    val environment: CangjieLspEnvironment,
    val documentStore: LspDocumentStore = LspDocumentStore(),
    val workspaceState: LspWorkspaceState = LspWorkspaceState(),
    val requestExecutor: CangjieRequestExecutor = CangjieRequestExecutor(),
    analysisFacadeFactory: (CangjieAnalysisLifecycleContext) -> CangjieAnalysisFacade,
) : AutoCloseable {
    @Volatile
    var client: LanguageClient? = null

    val analysisFacade: CangjieAnalysisFacade = analysisFacadeFactory(
        CangjieAnalysisLifecycleContext(
            environment = environment,
            descriptor = descriptor,
            workspaceState = workspaceState,
            documentStore = documentStore,
        ),
    )

    val enabledFeatures: CangjieLspFeatureSet
        get() = descriptor.features.intersect(analysisFacade.supportedFeatures)

    fun requestContext(): CangjieAnalysisRequestContext {
        return CangjieAnalysisRequestContext(
            environment = environment,
            descriptor = descriptor,
            workspaceState = workspaceState,
            documentStore = documentStore,
        )
    }

    override fun close() {
        analysisFacade.close()
        requestExecutor.close()
        environment.close()
    }
}
