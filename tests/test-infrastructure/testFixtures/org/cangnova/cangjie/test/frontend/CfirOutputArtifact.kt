package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.test.model.FrontendKinds
import org.cangnova.cangjie.test.model.ResultingArtifact
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule

data class CfirOutputPartForDependsOnModule(
    val module: TestModule,
    val session: CfirSession,
    val scopeSession: ScopeSession,
    val firAnalyzerFacade: AbstractCfirAnalyzerFacade?,
    val firFilesByTestFile: Map<TestFile, CfirFile>,
)

abstract class CfirOutputArtifact(
    val partsForDependsOnModules: List<CfirOutputPartForDependsOnModule>,
) : ResultingArtifact.FrontendOutput<CfirOutputArtifact>() {
    val allFirFilesByTestFile: Map<TestFile, CfirFile> =
        partsForDependsOnModules.fold(emptyMap()) { acc, part -> acc + part.firFilesByTestFile }

    override val kind: FrontendKinds.CFIR
        get() = FrontendKinds.CFIR

    val mainFirFilesByTestFile: Map<TestFile, CfirFile> by lazy {
        allFirFilesByTestFile.filterKeys { !it.isAdditional }
    }

    abstract val allFirFiles: Collection<CfirFile>
}

class CfirOutputArtifactImpl(parts: List<CfirOutputPartForDependsOnModule>) : CfirOutputArtifact(parts) {
    override val allFirFiles: Collection<CfirFile>
        get() = allFirFilesByTestFile.values
}
