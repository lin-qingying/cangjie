package org.cangnova.cangjie.jvm.codegen.classgen

import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.jvm.codegen.api.JvmClassFileArtifact
import org.cangnova.cangjie.jvm.codegen.context.JvmAbiAttributes
import org.cangnova.cangjie.jvm.codegen.context.JvmBackendContext
import org.cangnova.cangjie.jvm.codegen.diagnostics.JvmCodegenException
import org.cangnova.cangjie.jvm.codegen.function.JvmFunctionCodegen
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

/**
 * JVM class 生成器。对齐 Kotlin JVM backend 的 ClassCodegen 分层：类壳、方法、入口桥分阶段生成。
 */
class JvmClassCodegen(
    private val context: JvmBackendContext,
    private val module: ChirModule,
) {
    private val internalName: String = context.namePolicy.moduleInternalName(context.inputPackage, module)

    fun generate(): JvmClassFileArtifact {
        val globalVariables = module.declarations.filterIsInstance<ChirVariableDeclaration>()
        val functions = module.declarations.filterIsInstance<ChirFunctionDeclaration>()
        val initializerFunctions = packageInitializerFunctions(functions)
        validateGeneratedMembers(globalVariables, functions, initializerFunctions)

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
        globalVariables.forEach { variable -> generateGlobalField(writer, variable) }

        functions.forEach { function ->
            JvmFunctionCodegen(context, module, internalName, writer, function).generate()
        }
        generateClassInitializer(writer, initializerFunctions)
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
            context.typeMapper.mapReturnType(function.returnType) == Type.VOID_TYPE &&
            function.returnType == ChirResolvedTypeRef(ChirPrimitiveType.UNIT) &&
            jvmMethodDescriptor(function) == "()V"
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

    private fun generateGlobalField(writer: ClassWriter, variable: ChirVariableDeclaration) {
        val access = Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or (if (variable.mutable) 0 else Opcodes.ACC_FINAL)
        writer.visitField(
            access,
            JvmAbiAttributes.optionalString(variable.attributes, JvmAbiAttributes.NAME)
                ?: context.namePolicy.fieldJvmName(variable.name),
            JvmAbiAttributes.optionalString(variable.attributes, JvmAbiAttributes.DESCRIPTOR)
                ?: context.typeMapper.mapValueType(variable.type).descriptor,
            null,
            null,
        ).visitEnd()
    }

    private fun validateGeneratedMembers(
        globalVariables: List<ChirVariableDeclaration>,
        functions: List<ChirFunctionDeclaration>,
        initializerFunctions: List<ChirFunctionDeclaration>,
    ) {
        checkDuplicateJvmMembers(
            members = globalVariables.map { field -> jvmFieldSignature(field) to field.semanticId },
            kind = "field",
        )

        val methodMembers = buildList {
            add(JvmMemberSignature("<init>", "()V") to module.semanticId)
            functions.forEach { function ->
                add(jvmMethodSignature(function) to function.semanticId)
            }
            if (initializerFunctions.isNotEmpty()) {
                add(JvmMemberSignature("<clinit>", "()V") to module.semanticId)
            }
            if (context.options.generateMainBridge) {
                functions.filter(::canGenerateMainBridge).forEach { function ->
                    add(JvmMemberSignature("main", "([Ljava/lang/String;)V") to function.semanticId)
                }
            }
        }
        checkDuplicateJvmMembers(methodMembers, kind = "method")
    }

    private fun jvmFieldSignature(variable: ChirVariableDeclaration): JvmMemberSignature {
        val name = JvmAbiAttributes.optionalString(variable.attributes, JvmAbiAttributes.NAME)
            ?: context.namePolicy.fieldJvmName(variable.name)
        val descriptor = JvmAbiAttributes.optionalString(variable.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.mapValueType(variable.type).descriptor
        return JvmMemberSignature(name, descriptor)
    }

    private fun jvmMethodSignature(function: ChirFunctionDeclaration): JvmMemberSignature {
        val name = JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.NAME)
            ?: context.namePolicy.functionJvmName(function)
        return JvmMemberSignature(name, jvmMethodDescriptor(function))
    }

    private fun checkDuplicateJvmMembers(
        members: List<Pair<JvmMemberSignature, org.cangnova.cangjie.chir.core.identity.ChirSemanticId>>,
        kind: String,
    ) {
        val seen = linkedMapOf<JvmMemberSignature, org.cangnova.cangjie.chir.core.identity.ChirSemanticId>()
        members.forEach { (signature, semanticId) ->
            val previous = seen.putIfAbsent(signature, semanticId)
            if (previous != null) {
                throw JvmCodegenException(
                    "duplicate JVM $kind '${signature.name}${signature.descriptor}' in class '$internalName'",
                    semanticId,
                )
            }
        }
    }

    private fun packageInitializerFunctions(functions: List<ChirFunctionDeclaration>): List<ChirFunctionDeclaration> {
        val functionsById = functions.associateBy { it.semanticId }
        return listOfNotNull(
            context.inputPackage.packageLiteralInitFunctionId,
            context.inputPackage.packageInitFunctionId,
        )
            .distinct()
            .mapNotNull(functionsById::get)
    }

    private fun generateClassInitializer(
        writer: ClassWriter,
        initializerFunctions: List<ChirFunctionDeclaration>,
    ) {
        if (initializerFunctions.isEmpty()) return
        initializerFunctions.forEach(::verifyPackageInitializerFunction)
        val method = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)
        method.visitCode()
        initializerFunctions.forEach { function ->
            method.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                internalName,
                JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.NAME)
                    ?: context.namePolicy.functionJvmName(function),
                jvmMethodDescriptor(function),
                false,
            )
        }
        method.visitInsn(Opcodes.RETURN)
        method.visitMaxs(0, 0)
        method.visitEnd()
    }

    private fun verifyPackageInitializerFunction(function: ChirFunctionDeclaration) {
        if (function.parameters.isNotEmpty()) {
            throw org.cangnova.cangjie.jvm.codegen.diagnostics.JvmCodegenException(
                "JVM package initializer '${function.name}' must not declare parameters",
                function.semanticId,
            )
        }
        if (context.typeMapper.mapReturnType(function.returnType) != Type.VOID_TYPE) {
            throw org.cangnova.cangjie.jvm.codegen.diagnostics.JvmCodegenException(
                "JVM package initializer '${function.name}' must return Unit/Void",
                function.semanticId,
            )
        }
    }

    private fun jvmMethodDescriptor(function: ChirFunctionDeclaration): String {
        return JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.methodDescriptor(
                function.returnType,
                function.parameters.map(ChirVariableDeclaration::type),
            )
    }

    private data class JvmMemberSignature(
        val name: String,
        val descriptor: String,
    )
}
