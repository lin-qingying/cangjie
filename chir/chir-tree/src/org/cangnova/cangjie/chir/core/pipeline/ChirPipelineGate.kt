package org.cangnova.cangjie.chir.core.pipeline

import org.cangnova.cangjie.chir.core.checker.ChirValidationReportFormatter
import org.cangnova.cangjie.chir.core.checker.ChirValidator
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
import org.cangnova.cangjie.chir.core.context.ChirContext
import org.cangnova.cangjie.chir.core.model.ChirPackage

/**
 * Pipeline 阶段执行前的 CHIR 校验入口。
 */
object ChirPipelineGate {
    /**
     * 要求 [chirPackage] 在进入 [stageName] 前通过校验。
     */
    fun requireValidForStage(
        chirPackage: ChirPackage,
        stageName: String,
        context: ChirContext? = null,
        validator: ChirValidator = DefaultChirValidator(),
    ) {
        val report = validator.validatePackage(chirPackage, context)
        require(!report.hasErrors) { "pipeline stage '$stageName' blocked by CHIR validation:\n${ChirValidationReportFormatter.render(report)}" }
    }
}
