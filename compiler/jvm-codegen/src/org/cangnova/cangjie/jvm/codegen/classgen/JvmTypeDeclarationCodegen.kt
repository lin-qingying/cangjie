package org.cangnova.cangjie.jvm.codegen.classgen

import org.cangnova.cangjie.chir.core.declaration.ChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirCustomTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.jvm.codegen.api.JvmClassFileArtifact
import org.cangnova.cangjie.jvm.codegen.context.JvmBackendContext
import org.cangnova.cangjie.jvm.codegen.diagnostics.JvmCodegenException
import org.cangnova.cangjie.jvm.codegen.function.JvmFunctionCodegen
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

/**
 * CHIR 类型声明到标准 JVM classfile 的生成器。
 *
 * CHIR 是 JVM 后端的唯一输入：这里只消费 CHIR class/struct/enum 声明，不回读前端或 CFIR 结构。
 */
class JvmTypeDeclarationCodegen(
    private val context: JvmBackendContext,
    private val module: ChirModule,
    private val declaration: ChirCustomTypeDeclaration,
) {
    private val internalName: String = context.namePolicy.typeInternalName(context.inputPackage, declaration)

    fun generate(): JvmClassFileArtifact {
        return when (declaration) {
            is ChirClassDeclaration -> generateClassDeclaration(declaration)
            is ChirStructDeclaration -> generateStructDeclaration(declaration)
            is ChirEnumDeclaration -> generateEnumDeclaration(declaration)
            else -> throw JvmCodegenException(
                "JVM backend does not support type declaration ${declaration::class.simpleName}",
                declaration.semanticId,
            )
        }
    }

    private fun generateClassDeclaration(declaration: ChirClassDeclaration): JvmClassFileArtifact {
        val superInternalName = declaration.superTypes.firstOrNull()?.let { objectInternalName(it, "class supertype") }
            ?: "java/lang/Object"
        val implementedTypes = declaration.implementedTypes.map { objectInternalName(it, "class implemented type") }.toTypedArray()
        return generateClassLike(
            access = Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER,
            superInternalName = superInternalName,
            interfaces = implementedTypes,
            fieldDeclarations = declaration.memberDeclarations.filterIsInstance<ChirVariableDeclaration>(),
            memberFunctions = declaration.memberDeclarations.filterIsInstance<ChirFunctionDeclaration>(),
        )
    }

    private fun generateStructDeclaration(declaration: ChirStructDeclaration): JvmClassFileArtifact {
        return generateClassLike(
            access = Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER,
            superInternalName = "java/lang/Object",
            interfaces = emptyArray(),
            fieldDeclarations = declaration.fieldDeclarations,
            memberFunctions = declaration.memberDeclarations.filterIsInstance<ChirFunctionDeclaration>(),
        )
    }

    private fun generateClassLike(
        access: Int,
        superInternalName: String,
        interfaces: Array<String>,
        fieldDeclarations: List<ChirVariableDeclaration>,
        memberFunctions: List<ChirFunctionDeclaration>,
    ): JvmClassFileArtifact {
        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        writer.visit(
            context.options.classFileVersion,
            access,
            internalName,
            null,
            superInternalName,
            interfaces,
        )
        writer.visitSource("${declaration.name}.cj", null)
        fieldDeclarations.forEach { field -> generateField(writer, field) }
        generateDefaultConstructor(writer, superInternalName)
        memberFunctions.forEach { function ->
            JvmFunctionCodegen(
                context = context,
                module = module,
                ownerInternalName = internalName,
                classWriter = writer,
                function = function,
                methodAccess = Opcodes.ACC_PUBLIC,
                isStaticMethod = false,
            ).generate()
        }
        writer.visitEnd()
        return JvmClassFileArtifact(internalName = internalName, bytes = writer.toByteArray())
    }

    private fun generateEnumDeclaration(declaration: ChirEnumDeclaration): JvmClassFileArtifact {
        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        val enumDescriptor = "L$internalName;"
        writer.visit(
            context.options.classFileVersion,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER or Opcodes.ACC_ENUM,
            internalName,
            "Ljava/lang/Enum<$enumDescriptor>;",
            "java/lang/Enum",
            emptyArray(),
        )
        writer.visitSource("${declaration.name}.cj", null)
        declaration.cases.forEach { caseName ->
            writer.visitField(
                Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or Opcodes.ACC_ENUM,
                context.namePolicy.fieldJvmName(caseName),
                enumDescriptor,
                null,
                null,
            ).visitEnd()
        }
        writer.visitField(
            Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC or Opcodes.ACC_FINAL or Opcodes.ACC_SYNTHETIC,
            "\$VALUES",
            "[$enumDescriptor",
            null,
            null,
        ).visitEnd()
        generateEnumConstructor(writer)
        generateEnumValuesMethod(writer, enumDescriptor)
        generateEnumValueOfMethod(writer, enumDescriptor)
        generateEnumClassInitializer(writer, declaration.cases, enumDescriptor)
        writer.visitEnd()
        return JvmClassFileArtifact(internalName = internalName, bytes = writer.toByteArray())
    }

    private fun generateField(writer: ClassWriter, field: ChirVariableDeclaration) {
        writer.visitField(
            Opcodes.ACC_PUBLIC,
            context.namePolicy.fieldJvmName(field.name),
            context.typeMapper.mapValueType(field.type).descriptor,
            null,
            null,
        ).visitEnd()
    }

    private fun generateDefaultConstructor(writer: ClassWriter, superInternalName: String) {
        val method = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
        method.visitCode()
        method.visitVarInsn(Opcodes.ALOAD, 0)
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, superInternalName, "<init>", "()V", false)
        method.visitInsn(Opcodes.RETURN)
        method.visitMaxs(0, 0)
        method.visitEnd()
    }

    private fun generateEnumConstructor(writer: ClassWriter) {
        val method = writer.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "(Ljava/lang/String;I)V", null, null)
        method.visitCode()
        method.visitVarInsn(Opcodes.ALOAD, 0)
        method.visitVarInsn(Opcodes.ALOAD, 1)
        method.visitVarInsn(Opcodes.ILOAD, 2)
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Enum", "<init>", "(Ljava/lang/String;I)V", false)
        method.visitInsn(Opcodes.RETURN)
        method.visitMaxs(0, 0)
        method.visitEnd()
    }

    private fun generateEnumValuesMethod(writer: ClassWriter, enumDescriptor: String) {
        val method = writer.visitMethod(Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC, "values", "()[${enumDescriptor}", null, null)
        method.visitCode()
        method.visitFieldInsn(Opcodes.GETSTATIC, internalName, "\$VALUES", "[$enumDescriptor")
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "[$enumDescriptor", "clone", "()Ljava/lang/Object;", false)
        method.visitTypeInsn(Opcodes.CHECKCAST, "[$enumDescriptor")
        method.visitInsn(Opcodes.ARETURN)
        method.visitMaxs(0, 0)
        method.visitEnd()
    }

    private fun generateEnumValueOfMethod(writer: ClassWriter, enumDescriptor: String) {
        val method = writer.visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
            "valueOf",
            "(Ljava/lang/String;)$enumDescriptor",
            null,
            null,
        )
        method.visitCode()
        method.visitLdcInsn(Type.getObjectType(internalName))
        method.visitVarInsn(Opcodes.ALOAD, 0)
        method.visitMethodInsn(
            Opcodes.INVOKESTATIC,
            "java/lang/Enum",
            "valueOf",
            "(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;",
            false,
        )
        method.visitTypeInsn(Opcodes.CHECKCAST, internalName)
        method.visitInsn(Opcodes.ARETURN)
        method.visitMaxs(0, 0)
        method.visitEnd()
    }

    private fun generateEnumClassInitializer(
        writer: ClassWriter,
        cases: List<String>,
        enumDescriptor: String,
    ) {
        val method = writer.visitMethod(Opcodes.ACC_STATIC, "<clinit>", "()V", null, null)
        method.visitCode()
        cases.forEachIndexed { index, caseName ->
            val fieldName = context.namePolicy.fieldJvmName(caseName)
            method.visitTypeInsn(Opcodes.NEW, internalName)
            method.visitInsn(Opcodes.DUP)
            method.visitLdcInsn(caseName)
            method.visitLdcInsn(index)
            method.visitMethodInsn(Opcodes.INVOKESPECIAL, internalName, "<init>", "(Ljava/lang/String;I)V", false)
            method.visitFieldInsn(Opcodes.PUTSTATIC, internalName, fieldName, enumDescriptor)
        }
        method.visitLdcInsn(cases.size)
        method.visitTypeInsn(Opcodes.ANEWARRAY, internalName)
        cases.forEachIndexed { index, caseName ->
            val fieldName = context.namePolicy.fieldJvmName(caseName)
            method.visitInsn(Opcodes.DUP)
            method.visitLdcInsn(index)
            method.visitFieldInsn(Opcodes.GETSTATIC, internalName, fieldName, enumDescriptor)
            method.visitInsn(Opcodes.AASTORE)
        }
        method.visitFieldInsn(Opcodes.PUTSTATIC, internalName, "\$VALUES", "[$enumDescriptor")
        method.visitInsn(Opcodes.RETURN)
        method.visitMaxs(0, 0)
        method.visitEnd()
    }

    private fun objectInternalName(typeRef: ChirTypeRef, location: String): String {
        val type = context.typeMapper.mapValueType(typeRef)
        if (type.sort != Type.OBJECT) {
            throw JvmCodegenException(
                "JVM $location must be an object type, actual descriptor ${type.descriptor}",
                declaration.semanticId,
            )
        }
        return type.internalName
    }
}
