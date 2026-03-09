package org.cangjie.cfir.resolve

import org.cangjie.cfir.declarations.CfirResolvePhase
import org.cangjie.cfir.session.CfirSessionComponent

/**
 * 按阶段注册解析处理器。
 */
class CfirPhaseResolverRegistry : CfirSessionComponent {

    private val processors = mutableMapOf<CfirResolvePhase, CfirResolveProcessor>()

    fun registerProcessor(phase: CfirResolvePhase, processor: CfirResolveProcessor) {
        check(phase !in processors) { "Processor already registered for phase: $phase" }
        processors[phase] = processor
    }

    fun getProcessor(phase: CfirResolvePhase): CfirResolveProcessor? = processors[phase]

    val registeredPhases: Set<CfirResolvePhase> get() = processors.keys
}
