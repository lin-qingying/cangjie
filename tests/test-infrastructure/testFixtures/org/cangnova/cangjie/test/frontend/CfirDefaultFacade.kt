package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.cli.pipeline.CfirFrontendPipelinePhase
import org.cangnova.cangjie.cli.pipeline.DefaultCfirFrontendPipelineArtifact
import org.cangnova.cangjie.test.services.TestServices

class CfirDefaultFacade(
    testServices: TestServices,
) : CfirCliFacade<CfirFrontendPipelinePhase, DefaultCfirFrontendPipelineArtifact>
    (testServices, CfirFrontendPipelinePhase)
