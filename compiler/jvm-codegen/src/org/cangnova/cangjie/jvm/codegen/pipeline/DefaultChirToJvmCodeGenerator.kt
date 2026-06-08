package org.cangnova.cangjie.jvm.codegen.pipeline

import org.cangnova.cangjie.chir.core.checker.ChirValidationReportFormatter
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
import org.cangnova.cangjie.jvm.codegen.api.ChirJvmCodegenInput
import org.cangnova.cangjie.jvm.codegen.api.ChirJvmCodegenOutput
import org.cangnova.cangjie.jvm.codegen.api.ChirToJvmCodeGenerator
import org.cangnova.cangjie.jvm.codegen.context.JvmBackendContext
import org.cangnova.cangjie.jvm.codegen.diagnostics.JvmCodegenException
import org.cangnova.cangjie.jvm.codegen.module.JvmModuleCodegen

class DefaultChirToJvmCodeGenerator : ChirToJvmCodeGenerator {
    override fun generate(input: ChirJvmCodegenInput): ChirJvmCodegenOutput {
        if (!input.options.enabled) {
            return ChirJvmCodegenOutput(emptyList())
        }
        if (input.options.validateChirBeforeLowering) {
            val validationReport = DefaultChirValidator().validatePackage(input.chirPackage)
            if (validationReport.hasErrors) {
                throw JvmCodegenException(
                    "invalid CHIR package '${input.chirPackage.name}' before JVM lowering:\n" +
                        ChirValidationReportFormatter.render(validationReport),
                    input.chirPackage.semanticId,
                )
            }
        }

        val context = JvmBackendContext(input.chirPackage, input.options)
        val moduleResults = input.chirPackage.modules.map { module ->
            JvmModuleCodegen(context, module).generate()
        }
        val trace = if (input.options.emitLoweringTrace) {
            moduleResults.flatMap { it.loweringTrace }
        } else {
            emptyList()
        }
        return ChirJvmCodegenOutput(
            classes = moduleResults.flatMap { it.classes },
            mainClassInternalName = moduleResults.firstNotNullOfOrNull { it.mainClassInternalName },
            loweringTrace = trace,
        )
    }
}
