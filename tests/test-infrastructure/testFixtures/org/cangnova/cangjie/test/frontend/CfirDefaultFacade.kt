package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.frontend.pipeline.CfirFrontendPipelinePhase
import org.cangnova.cangjie.frontend.pipeline.DefaultCfirFrontendPipelineArtifact
import org.cangnova.cangjie.test.services.TestServices

class CfirDefaultFacade(
    testServices: TestServices,
) : CfirFrontendPipelineFacade<CfirFrontendPipelinePhase, DefaultCfirFrontendPipelineArtifact>
    (testServices, CfirFrontendPipelinePhase)
