package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

/**
 * 按阶段注册解析处理器。
 */
class CfirPhaseResolverRegistry : CfirSessionComponent {

    /** resolve phase 到处理器的注册表。 */
    private val processors = mutableMapOf<CfirResolvePhase, CfirResolveProcessor>()

    /** 注册指定 resolve phase 的处理器，并校验处理器自身目标阶段一致。 */
    fun registerProcessor(phase: CfirResolvePhase, processor: CfirResolveProcessor) {
        check(phase !in processors) { "Processor already registered for phase: $phase" }
        val processorPhase = processor.phase
        check(processorPhase == phase) {
            "Processor target phase mismatch: register phase=$phase, processor.phase=$processorPhase"
        }
        processors[phase] = processor
    }

    /** 获取指定阶段的处理器；未注册时返回 null。 */
    fun getProcessor(phase: CfirResolvePhase): CfirResolveProcessor? = processors[phase]

    /** 当前已经注册的 resolve phase 集合。 */
    val registeredPhases: Set<CfirResolvePhase> get() = processors.keys
}
