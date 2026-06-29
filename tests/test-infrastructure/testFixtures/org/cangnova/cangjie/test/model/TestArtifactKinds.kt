package org.cangnova.cangjie.test.model

import org.cangnova.cangjie.test.frontend.CfirOutputArtifact

/**
 * 提供 `FrontendKinds` 单例，集中承载测试模型的共享状态、常量或默认行为。
 */
object FrontendKinds {
    /**
     * 提供 `CFIR` 单例，集中承载测试模型的共享状态、常量或默认行为。
     */
    object CFIR : FrontendKind<CfirOutputArtifact>("CFIR")
}
