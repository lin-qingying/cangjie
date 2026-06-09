package org.cangnova.cangjie.jvm.codegen.classgen

import org.cangnova.cangjie.chir.core.declaration.ChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirCustomTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirExtendDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.effectiveMemberDeclarations
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.jvm.codegen.api.JvmClassFileArtifact
import org.cangnova.cangjie.jvm.codegen.context.JvmAbiAttributes
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
    private val moduleFacadeInternalName: String = context.namePolicy.moduleInternalName(context.inputPackage, module)

    fun generate(): JvmClassFileArtifact {
        return when (declaration) {
            is ChirClassDeclaration -> generateClassDeclaration(declaration)
            is ChirStructDeclaration -> generateStructDeclaration(declaration)
            is ChirEnumDeclaration -> generateEnumDeclaration(declaration)
            is ChirExtendDeclaration -> generateExtendDeclaration(declaration)
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
            fieldDeclarations = declaration.effectiveMemberDeclarations().filterIsInstance<ChirVariableDeclaration>(),
            memberFunctions = declaration.effectiveMemberDeclarations().filterIsInstance<ChirFunctionDeclaration>(),
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
        val constructors = memberFunctions.filter(::isJvmConstructor)
        val regularMemberFunctions = memberFunctions.filterNot(::isJvmConstructor)
        validateClassLikeMembers(fieldDeclarations, constructors, regularMemberFunctions)
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
        if (constructors.isEmpty()) {
            generateDefaultConstructor(writer, superInternalName)
        }
        constructors.forEach { constructor ->
            JvmFunctionCodegen(
                context = context,
                module = module,
                ownerInternalName = internalName,
                classWriter = writer,
                function = constructor,
                methodAccess = Opcodes.ACC_PUBLIC,
                isStaticMethod = false,
                isConstructor = true,
                superInternalName = superInternalName,
                moduleFacadeInternalName = moduleFacadeInternalName,
            ).generate()
        }
        regularMemberFunctions.forEach { function ->
            JvmFunctionCodegen(
                context = context,
                module = module,
                ownerInternalName = internalName,
                classWriter = writer,
                function = function,
                methodAccess = Opcodes.ACC_PUBLIC,
                isStaticMethod = false,
                moduleFacadeInternalName = moduleFacadeInternalName,
            ).generate()
        }
        writer.visitEnd()
        return JvmClassFileArtifact(internalName = internalName, bytes = writer.toByteArray())
    }

    private fun generateExtendDeclaration(declaration: ChirExtendDeclaration): JvmClassFileArtifact {
        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        val fields = declaration.memberDeclarations.filterIsInstance<ChirVariableDeclaration>()
        val functions = declaration.memberDeclarations.filterIsInstance<ChirFunctionDeclaration>()
        validateExtendMembers(fields, functions)
        writer.visit(
            context.options.classFileVersion,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER,
            internalName,
            null,
            "java/lang/Object",
            emptyArray(),
        )
        writer.visitSource("${declaration.name}.cj", null)
        generatePrivateConstructor(writer)
        fields.forEach { variable -> generateStaticField(writer, variable) }
        functions.forEach { function ->
                JvmFunctionCodegen(
                    context = context,
                    module = module,
                    ownerInternalName = internalName,
                    classWriter = writer,
                    function = function,
                    methodAccess = Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC,
                    isStaticMethod = true,
                    moduleFacadeInternalName = moduleFacadeInternalName,
                ).generate()
            }
        writer.visitEnd()
        return JvmClassFileArtifact(internalName = internalName, bytes = writer.toByteArray())
    }

    private fun generateEnumDeclaration(declaration: ChirEnumDeclaration): JvmClassFileArtifact {
        val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES or ClassWriter.COMPUTE_MAXS)
        val enumDescriptor = "L$internalName;"
        validateEnumMembers(declaration, enumDescriptor)
        writer.visit(
            context.options.classFileVersion,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER or Opcodes.ACC_ENUM,
            internalName,
            "Ljava/lang/Enum<$enumDescriptor>;",
            "java/lang/Enum",
            emptyArray(),
        )
        writer.visitSource("${declaration.name}.cj", null)
        declaration.memberDeclarations
            .filterIsInstance<ChirVariableDeclaration>()
            .forEach { field -> generateField(writer, field) }
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
        declaration.memberDeclarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .filterNot(::isJvmConstructor)
            .forEach { function ->
                JvmFunctionCodegen(
                    context = context,
                    module = module,
                    ownerInternalName = internalName,
                    classWriter = writer,
                    function = function,
                    methodAccess = Opcodes.ACC_PUBLIC,
                    isStaticMethod = false,
                    moduleFacadeInternalName = moduleFacadeInternalName,
                ).generate()
            }
        writer.visitEnd()
        return JvmClassFileArtifact(internalName = internalName, bytes = writer.toByteArray())
    }

    private fun validateClassLikeMembers(
        fieldDeclarations: List<ChirVariableDeclaration>,
        constructors: List<ChirFunctionDeclaration>,
        regularMemberFunctions: List<ChirFunctionDeclaration>,
    ) {
        checkDuplicateJvmMembers(
            members = fieldDeclarations.map { field -> jvmFieldSignature(field) to field.semanticId },
            kind = "field",
        )
        val methodMembers = buildList {
            if (constructors.isEmpty()) {
                add(JvmMemberSignature("<init>", "()V") to declaration.semanticId)
            }
            constructors.forEach { constructor ->
                add(jvmConstructorSignature(constructor) to constructor.semanticId)
            }
            regularMemberFunctions.forEach { function ->
                add(jvmMethodSignature(function) to function.semanticId)
            }
        }
        checkDuplicateJvmMembers(methodMembers, kind = "method")
    }

    private fun validateExtendMembers(
        fields: List<ChirVariableDeclaration>,
        functions: List<ChirFunctionDeclaration>,
    ) {
        checkDuplicateJvmMembers(
            members = fields.map { field -> jvmFieldSignature(field) to field.semanticId },
            kind = "field",
        )
        val methodMembers = buildList {
            add(JvmMemberSignature("<init>", "()V") to declaration.semanticId)
            functions.forEach { function -> add(jvmMethodSignature(function) to function.semanticId) }
        }
        checkDuplicateJvmMembers(methodMembers, kind = "method")
    }

    private fun validateEnumMembers(declaration: ChirEnumDeclaration, enumDescriptor: String) {
        val fields = buildList {
            declaration.memberDeclarations
                .filterIsInstance<ChirVariableDeclaration>()
                .forEach { field -> add(jvmFieldSignature(field) to field.semanticId) }
            declaration.cases.forEach { caseName ->
                add(JvmMemberSignature(context.namePolicy.fieldJvmName(caseName), enumDescriptor) to declaration.semanticId)
            }
            add(JvmMemberSignature("\$VALUES", "[$enumDescriptor") to declaration.semanticId)
        }
        checkDuplicateJvmMembers(fields, kind = "field")

        val methods = buildList {
            add(JvmMemberSignature("<init>", "(Ljava/lang/String;I)V") to declaration.semanticId)
            add(JvmMemberSignature("values", "()[${enumDescriptor}") to declaration.semanticId)
            add(JvmMemberSignature("valueOf", "(Ljava/lang/String;)$enumDescriptor") to declaration.semanticId)
            declaration.memberDeclarations
                .filterIsInstance<ChirFunctionDeclaration>()
                .filterNot(::isJvmConstructor)
                .forEach { function -> add(jvmMethodSignature(function) to function.semanticId) }
        }
        checkDuplicateJvmMembers(methods, kind = "method")
    }

    private fun jvmFieldSignature(field: ChirVariableDeclaration): JvmMemberSignature {
        val name = JvmAbiAttributes.optionalString(field.attributes, JvmAbiAttributes.NAME)
            ?: context.namePolicy.fieldJvmName(field.name)
        val descriptor = JvmAbiAttributes.optionalString(field.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.mapValueType(field.type).descriptor
        return JvmMemberSignature(name, descriptor)
    }

    private fun jvmMethodSignature(function: ChirFunctionDeclaration): JvmMemberSignature {
        val name = JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.NAME)
            ?: context.namePolicy.functionJvmName(function)
        return JvmMemberSignature(name, jvmMethodDescriptor(function, isConstructor = false))
    }

    private fun jvmConstructorSignature(function: ChirFunctionDeclaration): JvmMemberSignature {
        val name = JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.NAME)
            ?: "<init>"
        return JvmMemberSignature(name, jvmMethodDescriptor(function, isConstructor = true))
    }

    private fun jvmMethodDescriptor(function: ChirFunctionDeclaration, isConstructor: Boolean): String {
        return JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: Type.getMethodDescriptor(
                if (isConstructor) Type.VOID_TYPE else context.typeMapper.mapReturnType(function.returnType),
                *function.parameters.map { context.typeMapper.mapValueType(it.type) }.toTypedArray(),
            )
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

    private fun generateField(writer: ClassWriter, field: ChirVariableDeclaration) {
        val access = Opcodes.ACC_PUBLIC or (if (field.mutable) 0 else Opcodes.ACC_FINAL)
        writer.visitField(
            access,
            JvmAbiAttributes.optionalString(field.attributes, JvmAbiAttributes.NAME)
                ?: context.namePolicy.fieldJvmName(field.name),
            JvmAbiAttributes.optionalString(field.attributes, JvmAbiAttributes.DESCRIPTOR)
                ?: context.typeMapper.mapValueType(field.type).descriptor,
            null,
            null,
        ).visitEnd()
    }

    private fun generateStaticField(writer: ClassWriter, field: ChirVariableDeclaration) {
        val access = Opcodes.ACC_PUBLIC or Opcodes.ACC_STATIC or (if (field.mutable) 0 else Opcodes.ACC_FINAL)
        writer.visitField(
            access,
            JvmAbiAttributes.optionalString(field.attributes, JvmAbiAttributes.NAME)
                ?: context.namePolicy.fieldJvmName(field.name),
            JvmAbiAttributes.optionalString(field.attributes, JvmAbiAttributes.DESCRIPTOR)
                ?: context.typeMapper.mapValueType(field.type).descriptor,
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

    private fun generatePrivateConstructor(writer: ClassWriter) {
        val method = writer.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null)
        method.visitCode()
        method.visitVarInsn(Opcodes.ALOAD, 0)
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
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

    private fun isJvmConstructor(function: ChirFunctionDeclaration): Boolean {
        return function.name == "<init>" ||
            JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.NAME) == "<init>"
    }

    private data class JvmMemberSignature(
        val name: String,
        val descriptor: String,
    )
}
