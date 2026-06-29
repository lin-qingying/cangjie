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
    /**
     * 当前 JVM 后端共享上下文，提供类型映射、命名策略、输入包 ABI 与 classfile 版本选项。
     */
    private val context: JvmBackendContext,
    /**
     * 正在生成 module facade 的 CHIR 模块，模块中的全局变量与函数会被映射为静态字段和静态方法。
     */
    private val module: ChirModule,
) {
    /**
     * module facade 在 JVM classfile 中使用的 internal name，由输入包与模块名策略共同决定。
     */
    private val internalName: String = context.namePolicy.moduleInternalName(context.inputPackage, module)

    /**
     * 生成当前模块的 JVM facade classfile。
     *
     * 生成过程先收集全局字段、普通函数与包初始化函数，再校验 JVM 成员签名冲突，
     * 最后写入私有构造器、静态字段、静态函数、`<clinit>` 与可选的 Java `main(String[])` 桥接方法。
     */
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

    /**
     * 判断一个 CHIR 函数是否可以暴露为 Java 启动入口桥。
     *
     * 只有无参数、Unit 返回、实际 JVM descriptor 为 `()V` 且命名为 `main` 的函数，
     * 才能安全生成 `public static void main(String[])` 桥接方法。
     */
    fun canGenerateMainBridge(function: ChirFunctionDeclaration): Boolean {
        return function.name == "main" &&
            function.parameters.isEmpty() &&
            context.typeMapper.mapReturnType(function.returnType) == Type.VOID_TYPE &&
            function.returnType == ChirResolvedTypeRef(ChirPrimitiveType.UNIT) &&
            jvmMethodDescriptor(function) == "()V"
    }

    /**
     * 为 facade class 写入私有无参构造器，阻止外部实例化纯静态承载类。
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
     * 将 CHIR 全局变量生成为 facade 上的 JVM 静态字段。
     *
     * 字段名与 descriptor 优先使用显式 ABI 属性，否则由命名策略与类型映射器推导；
     * 不可变变量会额外标记为 `final`。
     */
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

    /**
     * 在写 classfile 前校验 facade 会生成的所有 JVM 字段与方法签名。
     *
     * 这里把默认构造器、包初始化 `<clinit>` 与可选 main bridge 一并纳入冲突检查，
     * 保证后续 ASM 写入不会产生同名同 descriptor 的重复成员。
     */
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

    /**
     * 计算全局变量最终落到 classfile 中的字段签名。
     */
    private fun jvmFieldSignature(variable: ChirVariableDeclaration): JvmMemberSignature {
        val name = JvmAbiAttributes.optionalString(variable.attributes, JvmAbiAttributes.NAME)
            ?: context.namePolicy.fieldJvmName(variable.name)
        val descriptor = JvmAbiAttributes.optionalString(variable.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.mapValueType(variable.type).descriptor
        return JvmMemberSignature(name, descriptor)
    }

    /**
     * 计算 CHIR 函数最终落到 facade classfile 中的方法签名。
     */
    private fun jvmMethodSignature(function: ChirFunctionDeclaration): JvmMemberSignature {
        val name = JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.NAME)
            ?: context.namePolicy.functionJvmName(function)
        return JvmMemberSignature(name, jvmMethodDescriptor(function))
    }

    /**
     * 检查同一 JVM class 内的字段或方法是否存在完全重复的 name+descriptor 签名。
     *
     * 发现冲突时使用后出现成员的语义 id 报告，使诊断能定位到触发重复生成的 CHIR 节点。
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
     * 按输入包记录的初始化函数 id，从模块函数列表中恢复包字面量初始化与包初始化函数。
     *
     * 返回顺序保持字面量初始化在前、包初始化在后，并去除重复 id。
     */
    private fun packageInitializerFunctions(functions: List<ChirFunctionDeclaration>): List<ChirFunctionDeclaration> {
        val functionsById = functions.associateBy { it.semanticId }
        return listOfNotNull(
            context.inputPackage.packageLiteralInitFunctionId,
            context.inputPackage.packageInitFunctionId,
        )
            .distinct()
            .mapNotNull(functionsById::get)
    }

    /**
     * 生成 facade 的 JVM `<clinit>`，按固定顺序调用包级初始化函数。
     *
     * 如果模块没有包初始化函数则不写 `<clinit>` 成员；否则先校验每个初始化函数满足无参 Unit 契约。
     */
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

    /**
     * 校验 CHIR 包初始化函数是否能被 `<clinit>` 直接调用。
     *
     * JVM class initializer 无法传参也没有返回值，因此对应 CHIR 函数必须是无参数且返回 Unit/Void。
     */
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

    /**
     * 计算 CHIR 函数的 JVM method descriptor。
     *
     * 显式 ABI descriptor 优先；否则根据函数返回类型与参数类型通过类型映射器生成。
     */
    private fun jvmMethodDescriptor(function: ChirFunctionDeclaration): String {
        return JvmAbiAttributes.optionalString(function.attributes, JvmAbiAttributes.DESCRIPTOR)
            ?: context.typeMapper.methodDescriptor(
                function.returnType,
                function.parameters.map(ChirVariableDeclaration::type),
            )
    }

    /**
     * JVM 成员冲突检查使用的规范化签名键。
     */
    private data class JvmMemberSignature(
        /**
         * 字段名或方法名，构造器和 class initializer 使用 JVM 保留名。
         */
        val name: String,
        /**
         * 字段 descriptor 或方法 descriptor。
         */
        val descriptor: String,
    )
}
