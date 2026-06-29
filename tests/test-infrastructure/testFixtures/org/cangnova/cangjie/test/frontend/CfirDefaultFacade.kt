package org.cangnova.cangjie.test.frontend

import org.cangnova.cangjie.frontend.pipeline.CfirFrontendPipelinePhase
import org.cangnova.cangjie.frontend.pipeline.DefaultCfirFrontendPipelineArtifact
import org.cangnova.cangjie.test.services.TestServices

/**
 * 表示 `CfirDefaultFacade`，承载CFIR 前端测试中的配置数据、测试产物或处理步骤。
 */
class CfirDefaultFacade(
    testServices: TestServices,
) : CfirFrontendPipelineFacade<CfirFrontendPipelinePhase, DefaultCfirFrontendPipelineArtifact>
    (testServices, CfirFrontendPipelinePhase)
