package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl

/**
 * 注册的 PRIVATE additional-results 生产任务。
 *
 * 普通 callback 调用只执行暂定写回；最终完成必须显式传入本次实际 owner 的 writer system。
 * dispatcher 代次不进入 memento，完成事实由所属 resource graph 的快照保存。
 */
internal class CfirRetainedAdditionalResultsProducer(
    val graph: CfirImplicitTypeComputation,
    val owner: Candidate,
    internal val epoch: Any,
) : (ConeSubstitutor) -> Unit {
    override fun invoke(substitutor: ConeSubstitutor) {
        graph.writePrivateAdditionalResults(this, substitutor, null)
    }

    fun complete(system: ConstraintSystemImpl, substitutor: ConeSubstitutor) {
        graph.writePrivateAdditionalResults(this, substitutor, system)
    }
}
