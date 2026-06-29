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
    /**
     * JVM 后端上下文，集中提供 classfile 版本、ABI 属性读取、命名策略与类型映射能力。
     */
    private val context: JvmBackendContext,
    /**
     * 类型声明所在的 CHIR 模块，用于解析模块 facade 名称以及生成成员函数体时访问同模块函数。
     */
    private val module: ChirModule,
    /**
     * 当前要生成为独立 JVM classfile 的 CHIR 自定义类型声明。
     */
    private val declaration: ChirCustomTypeDeclaration,
) {
    /**
     * 当前类型在 JVM classfile 中的 internal name。
     */
    private val internalName: String = context.namePolicy.typeInternalName(context.inputPackage, declaration)
    /**
     * 当前模块 facade 的 internal name，成员方法访问包级函数或全局字段时使用。
     */
    private val moduleFacadeInternalName: String = context.namePolicy.moduleInternalName(context.inputPackage, module)

    /**
     * 根据 CHIR 类型声明的具体种类分派到对应 classfile 生成路径。
     *
     * 支持 class、struct、enum 与 extend；其他自定义类型会以结构化 JVM 后端异常失败。
     */
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

    /**
     * 生成普通 class 声明的 JVM classfile。
     *
     * CHIR 的首个 superType 作为 JVM superclass，其余实现接口来自 implementedTypes；
     * 字段和函数直接取声明成员，不做 struct 的有效成员展开。
     */
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

    /**
     * 生成 struct 声明的 JVM classfile。
     *
     * struct 当前映射为 final class，并通过 effectiveMemberDeclarations 展开可生成成员。
     */
    private fun generateStructDeclaration(declaration: ChirStructDeclaration): JvmClassFileArtifact {
        return generateClassLike(
            access = Opcodes.ACC_PUBLIC or Opcodes.ACC_FINAL or Opcodes.ACC_SUPER,
            superInternalName = "java/lang/Object",
            interfaces = emptyArray(),
            fieldDeclarations = declaration.effectiveMemberDeclarations().filterIsInstance<ChirVariableDeclaration>(),
            memberFunctions = declaration.effectiveMemberDeclarations().filterIsInstance<ChirFunctionDeclaration>(),
        )
    }

    /**
     * 生成 class/struct 共用的 classfile 骨架和实例成员。
     *
     * 该路径负责写入类头、字段、显式构造器或默认构造器，以及非构造器实例方法。
     */
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

    /**
     * 生成 extend 声明的 JVM 承载类。
     *
     * extend 没有实例状态，当前映射为 final 工具类：私有构造器、静态字段与静态函数。
     */
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

    /**
     * 生成 enum 声明的 JVM classfile。
     *
     * 该路径写入 enum case 字段、合成 `$VALUES`、私有 enum 构造器、`values()`、`valueOf(String)`、
     * 静态初始化逻辑以及用户声明的非构造器成员方法。
     */
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

    /**
     * 校验 class/struct 最终会生成的字段、构造器与实例方法签名是否冲突。
     */
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

    /**
     * 校验 extend 承载类上的静态字段、默认私有构造器与静态方法签名是否冲突。
     */
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

    /**
     * 校验 enum 生成的显式字段、case 字段、`$VALUES` 字段和合成方法不会与用户成员冲突。
     */
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

    /**
     * 计算成员字段最终落到 classfile 中的 JVM 字段签名。
     */
    private fun jvmFieldSignature(field: ChirVariableDeclaration): JvmMemberSignature {
        val name = JvmAbiAttributes.optionalString(field.attributes, JvmAbiAttributes.NAME)
            ?: context.namePolicy.fieldJvmName(field.name)
        val descriptor = JvmAbiAttributes.optionalString(field.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.mapValueType(field.type).descriptor
        return JvmMemberSignature(name, descriptor)
    }

    /**
     * 计算普通成员函数最终落到 classfile 中的 JVM 方法签名。
     */
    private fun jvmMethodSignature(function: ChirFunctionDeclaration): JvmMemberSignature {
        val name = JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.NAME)
            ?: context.namePolicy.functionJvmName(function)
        return JvmMemberSignature(name, jvmMethodDescriptor(function, isConstructor = false))
    }

    /**
     * 计算构造器函数最终落到 classfile 中的 JVM `<init>` 签名。
     */
    private fun jvmConstructorSignature(function: ChirFunctionDeclaration): JvmMemberSignature {
        val name = JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.NAME)
            ?: "<init>"
        return JvmMemberSignature(name, jvmMethodDescriptor(function, isConstructor = true))
    }

    /**
     * 计算成员函数或构造器的 JVM method descriptor。
     *
     * 构造器 descriptor 固定返回 void；普通函数按 CHIR 返回类型映射。
     */
    private fun jvmMethodDescriptor(function: ChirFunctionDeclaration, isConstructor: Boolean): String {
        return JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: Type.getMethodDescriptor(
                if (isConstructor) Type.VOID_TYPE else context.typeMapper.mapReturnType(function.returnType),
                *function.parameters.map { context.typeMapper.mapValueType(it.type) }.toTypedArray(),
            )
    }

    /**
     * 检查同一 JVM class 内字段或方法是否有重复的 name+descriptor。
     */
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

    /**
     * 生成实例字段，保持 CHIR 可变性到 JVM `final` 标记的映射。
     */
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

    /**
     * 生成静态字段，主要用于 extend 承载类上的成员变量。
     */
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

    /**
     * 当 class/struct 没有显式 CHIR 构造器时，生成 public 无参默认构造器。
     */
    private fun generateDefaultConstructor(writer: ClassWriter, superInternalName: String) {
        val method = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null)
        method.visitCode()
        method.visitVarInsn(Opcodes.ALOAD, 0)
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, superInternalName, "<init>", "()V", false)
        method.visitInsn(Opcodes.RETURN)
        method.visitMaxs(0, 0)
        method.visitEnd()
    }

    /**
     * 为纯静态承载类生成私有无参构造器。
     */
    private fun generatePrivateConstructor(writer: ClassWriter) {
        val method = writer.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null)
        method.visitCode()
        method.visitVarInsn(Opcodes.ALOAD, 0)
        method.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        method.visitInsn(Opcodes.RETURN)
        method.visitMaxs(0, 0)
        method.visitEnd()
    }

    /**
     * 生成 enum 固定签名的私有构造器，并转发给 `java/lang/Enum.<init>(String, int)`。
     */
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

    /**
     * 生成 enum 标准 `values()` 方法，返回 `$VALUES` 的克隆数组。
     */
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

    /**
     * 生成 enum 标准 `valueOf(String)` 方法，委托 `java/lang/Enum.valueOf` 后进行类型转换。
     */
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

    /**
     * 生成 enum 的 `<clinit>`，实例化每个 case 并填充合成 `$VALUES` 数组。
     */
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

    /**
     * 将 CHIR 类型引用映射为 JVM object internal name，并校验该位置确实需要对象类型。
     */
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

    /**
     * 判断 CHIR 函数是否按 JVM 构造器语义生成。
     *
     * 既支持函数名直接为 `<init>`，也支持 ABI 属性覆盖为 `<init>`。
     */
    private fun isJvmConstructor(function: ChirFunctionDeclaration): Boolean {
        return function.name == "<init>" ||
            JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.NAME) == "<init>"
    }

    /**
     * JVM 成员冲突检查使用的规范化签名键。
     */
    private data class JvmMemberSignature(
        /**
         * JVM 字段名或方法名。
         */
        val name: String,
        /**
         * JVM 字段 descriptor 或方法 descriptor。
         */
        val descriptor: String,
    )
}
