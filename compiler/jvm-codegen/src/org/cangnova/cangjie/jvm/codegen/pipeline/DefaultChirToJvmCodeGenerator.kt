package org.cangnova.cangjie.jvm.codegen.pipeline

import org.cangnova.cangjie.chir.core.checker.ChirValidationReportFormatter
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.jvm.codegen.api.ChirJvmCodegenInput
import org.cangnova.cangjie.jvm.codegen.api.ChirJvmCodegenOutput
import org.cangnova.cangjie.jvm.codegen.api.ChirToJvmCodeGenerator
import org.cangnova.cangjie.jvm.codegen.api.JvmClassFileArtifact
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
        val moduleResults = input.chirPackage.jvmCodegenModules().map { module ->
            JvmModuleCodegen(context, module).generate()
        }
        val trace = if (input.options.emitLoweringTrace) {
            moduleResults.flatMap { it.loweringTrace }
        } else {
            emptyList()
        }
        return ChirJvmCodegenOutput(
            classes = mergeClassArtifacts(moduleResults.flatMap { it.classes }, input.chirPackage),
            mainClassInternalName = resolveMainClassInternalName(
                moduleResults.mapNotNull { it.mainClassInternalName },
                input.chirPackage,
            ),
            loweringTrace = trace,
        )
    }

    /**
     * Java 可执行 JAR 只有一个 Main-Class。多个 CHIR 模块同时生成 main bridge 时不能按遍历顺序静默选择。
     */
    private fun resolveMainClassInternalName(
        mainClassInternalNames: List<String>,
        chirPackage: ChirPackage,
    ): String? {
        val distinctMainClasses = mainClassInternalNames.distinct()
        return when (distinctMainClasses.size) {
            0 -> null
            1 -> distinctMainClasses.single()
            else -> throw JvmCodegenException(
                "multiple JVM main classes generated for CHIR package '${chirPackage.name}': " +
                    distinctMainClasses.joinToString(),
                chirPackage.semanticId,
            )
        }
    }

    /**
     * JVM internal name 是 classfile 的唯一身份。不同 CHIR 声明不能静默落到同一个 class；
     * 只有运行时 helper 这类字节完全一致的重复 artifact 可以合并。
     */
    private fun mergeClassArtifacts(
        artifacts: List<JvmClassFileArtifact>,
        chirPackage: ChirPackage,
    ): List<JvmClassFileArtifact> {
        val merged = linkedMapOf<String, JvmClassFileArtifact>()
        artifacts.forEach { artifact ->
            val existing = merged[artifact.internalName]
            if (existing == null) {
                merged[artifact.internalName] = artifact
            } else if (!existing.bytes.contentEquals(artifact.bytes)) {
                throw JvmCodegenException(
                    "duplicate JVM class artifact '${artifact.internalName}' generated from CHIR package '${chirPackage.name}'",
                    chirPackage.semanticId,
                )
            }
        }
        return merged.values.toList()
    }

    private fun ChirPackage.jvmCodegenModules(): List<ChirModule> = buildList {
        addAll(modules)
        val packageLevelDeclarations =
            members.globalVariables +
                members.globalFunctions +
                typeDefinitions
        if (packageLevelDeclarations.isNotEmpty()) {
            add(
                ChirModule(
                    semanticId = ChirSemanticId("${semanticId.value}:jvm-package-members"),
                    name = packageFacadeModuleName(),
                    declarations = packageLevelDeclarations,
                ),
            )
        }
    }

    private fun ChirPackage.packageFacadeModuleName(): String {
        val simplePackageName = name.split('.').lastOrNull { it.isNotBlank() } ?: "package"
        return "${simplePackageName}Package"
    }
}
