package org.cangnova.cangjie.chir.core.serializer

import org.cangnova.cangjie.chir.core.checker.ChirValidationReportFormatter
import org.cangnova.cangjie.chir.core.checker.ChirValidator
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
import org.cangnova.cangjie.chir.core.context.ChirContext
import org.cangnova.cangjie.chir.core.model.ChirPackage

/**
 * CHIR 序列化前的校验入口。
 */
object ChirSerializationGate {
    /**
     * 要求 [chirPackage] 在序列化前通过校验。
     */
    fun requireValidForSerialization(
        chirPackage: ChirPackage,
        context: ChirContext? = null,
        validator: ChirValidator = DefaultChirValidator(),
    ) {
        val report = validator.validatePackage(chirPackage, context)
        require(!report.hasErrors) { "serialization blocked by CHIR validation:\n${ChirValidationReportFormatter.render(report)}" }
    }
}
