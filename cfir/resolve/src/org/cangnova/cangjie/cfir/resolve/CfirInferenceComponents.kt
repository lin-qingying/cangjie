package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceLogger
import org.cangnova.cangjie.cfir.resolve.inference.inferenceLogger
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeTypeContext

/**
 * 中层调用解析对新约束系统的最小接入壳。
 *
 * 这一层目前只负责为 calls/stages 提供新的约束系统实例，
 * 不再依赖旧的 Kotlin 风格 inference 组件实现。
 */
class CfirInferenceComponents(
    private val session: CfirSession,
    private val typeContext: ConeTypeContext,
) {
    fun createConstraintSystem(): org.cangnova.cangjie.cfir.constraints.CfirConstraintSystem {
        return CfirConstraintSystemImpl(
            typeRelations = CfirTypeRelations(typeContext),
            variableManager = CfirVariableManager(),
            inferenceLogger = session.inferenceLogger,
        )
    }
}
