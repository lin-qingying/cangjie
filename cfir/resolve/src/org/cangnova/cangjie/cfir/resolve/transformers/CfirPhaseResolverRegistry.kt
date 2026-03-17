package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

/**
 * 鎸夐樁娈垫敞鍐岃В鏋愬鐞嗗櫒銆? */
class CfirPhaseResolverRegistry : CfirSessionComponent {

    private val processors = mutableMapOf<CfirResolvePhase, CfirResolveProcessor>()

    fun registerProcessor(phase: CfirResolvePhase, processor: CfirResolveProcessor) {
        check(phase !in processors) { "Processor already registered for phase: $phase" }
        val processorPhase = processor.phase
        check(processorPhase == phase) {
            "Processor target phase mismatch: register phase=$phase, processor.phase=$processorPhase"
        }
        processors[phase] = processor
    }

    fun getProcessor(phase: CfirResolvePhase): CfirResolveProcessor? = processors[phase]

    val registeredPhases: Set<CfirResolvePhase> get() = processors.keys
}

