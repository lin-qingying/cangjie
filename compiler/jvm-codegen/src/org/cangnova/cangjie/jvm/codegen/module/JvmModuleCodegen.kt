package org.cangnova.cangjie.jvm.codegen.module

import org.cangnova.cangjie.chir.core.declaration.ChirCustomTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.jvm.codegen.api.JvmClassFileArtifact
import org.cangnova.cangjie.jvm.codegen.classgen.JvmClassCodegen
import org.cangnova.cangjie.jvm.codegen.classgen.JvmTypeDeclarationCodegen
import org.cangnova.cangjie.jvm.codegen.context.JvmBackendContext

data class JvmModuleCodegenResult(
    val classes: List<JvmClassFileArtifact>,
    val mainClassInternalName: String? = null,
    val loweringTrace: List<String> = emptyList(),
)

/**
 * CHIR 模块到 JVM 类的降级入口。当前采用一个模块一个 facade class，顶层函数生成为 public static 方法。
 */
class JvmModuleCodegen(
    private val context: JvmBackendContext,
    private val module: ChirModule,
) {
    fun generate(): JvmModuleCodegenResult {
        val classCodegen = JvmClassCodegen(context, module)
        val facadeArtifact = classCodegen.generate()
        val typeArtifacts = module.declarations
            .filterIsInstance<ChirCustomTypeDeclaration>()
            .map { declaration -> JvmTypeDeclarationCodegen(context, module, declaration).generate() }
        val hasMainBridge = module.declarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .any(classCodegen::canGenerateMainBridge)
        return JvmModuleCodegenResult(
            classes = listOf(facadeArtifact) + typeArtifacts,
            mainClassInternalName = facadeArtifact.internalName.takeIf { hasMainBridge },
            loweringTrace = buildList {
                add("jvm.module=${module.name} facade=${facadeArtifact.internalName}")
                typeArtifacts.forEach { artifact -> add("jvm.type=${artifact.internalName}") }
            },
        )
    }
}
