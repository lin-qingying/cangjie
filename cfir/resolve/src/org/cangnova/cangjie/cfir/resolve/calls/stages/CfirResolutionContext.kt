package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.resolve.body.CfirBodyResolveContext
import org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceComponents
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker

/**
 * 解析上下文，为验证阶段提供所需的环境信息。
 *
 * 对齐 K2 ResolutionContext（去掉 BodyResolveComponents 依赖，仅暴露必要服务）。
 */
class CfirResolutionContext(
    /** 编译器 session */
    val session: CfirSession,
    /** Body 解析上下文 */
    val bodyResolveContext: CfirBodyResolveContext,
    /** 子类型检查器 */
    val subtypeChecker: ConeSubtypeChecker,
    /** 推断组件（Phase 4 泛型推断） */
    val inferenceComponents: CfirInferenceComponents? = null,
)
