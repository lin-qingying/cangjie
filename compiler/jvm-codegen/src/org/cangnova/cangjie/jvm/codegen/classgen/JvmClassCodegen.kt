package org.cangnova.cangjie.jvm.codegen.classgen

import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.jvm.codegen.api.JvmClassFileArtifact
import org.cangnova.cangjie.jvm.codegen.context.JvmBackendContext
import org.cangnova.cangjie.jvm.codegen.function.JvmFunctionCodegen
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

/**
 * JVM class 生成器。对齐 Kotlin JVM backend 的 ClassCodegen 分层：类壳、方法、入口桥分阶段生成。
 */
class JvmClassCodegen(
    private val context: JvmBackendContext,
    private val module: ChirModule,
) {
    private val internalName: String = context.namePolicy.moduleInternalName(context.inputPackage, module)

    fun generate(): JvmClassFileArtifact {
        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        writer.visit(
            context.options.classFileVersion,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER,
            internalName,
            null,
            "java/lang/Object",
            emptyArray(),
        )
        writer.visitSource("${module.name}.cj", null)
        generatePrivateConstructor(writer)

        val functions = module.declarations.filterIsInstance<ChirFunctionDeclaration>()
        functions.forEach { function ->
            JvmFunctionCodegen(context, module, internalName, writer, function).generate()
        }
        if (context.options.generateMainBridge) {
            functions.filter(::canGenerateMainBridge).forEach { function ->
                JvmFunctionCodegen(context, module, internalName, writer, function).generateMainBridge()
            }
        }

        writer.visitEnd()
        return JvmClassFileArtifact(internalName = internalName, bytes = writer.toByteArray())
    }

    fun canGenerateMainBridge(function: ChirFunctionDeclaration): Boolean {
        return function.name == "main" &&
            function.parameters.isEmpty() &&
            context.typeMapper.mapReturnType(function.returnType) == org.objectweb.asm.Type.VOID_TYPE &&
            function.returnType == ChirResolvedTypeRef(ChirPrimitiveType.UNIT)
    }

    private fun generatePrivateConstructor(writer: ClassWriter) {
        val method = writer.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null)
        method.visitCode()
        method.visitVarInsn(Opcodes.ALOAD, 0)
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        method.visitInsn(Opcodes.RETURN)
        method.visitMaxs(0, 0)
        method.visitEnd()
    }
}
