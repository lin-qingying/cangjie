package org.cangnova.cangjie.jvm.codegen.module

import org.cangnova.cangjie.chir.core.declaration.ChirCustomTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.effectiveMemberDeclarations
import org.cangnova.cangjie.chir.core.type.ChirBoxType
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirEnumType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirVArrayType
import org.cangnova.cangjie.chir.core.value.ChirValue
import org.cangnova.cangjie.jvm.codegen.api.JvmClassFileArtifact
import org.cangnova.cangjie.jvm.codegen.classgen.JvmClassCodegen
import org.cangnova.cangjie.jvm.codegen.classgen.JvmTypeDeclarationCodegen
import org.cangnova.cangjie.jvm.codegen.context.JvmBackendContext
import org.cangnova.cangjie.jvm.codegen.runtime.JvmRuntimeArtifacts

/**
 * 单个 CHIR module 的 JVM codegen 结果。
 */
data class JvmModuleCodegenResult(
    /**
     * 当前 module 生成的 class 文件集合。
     */
    val classes: List<JvmClassFileArtifact>,
    /**
     * 当前 module 生成的 Java main bridge 所在 class；没有 main bridge 时为空。
     */
    val mainClassInternalName: String? = null,
    /**
     * 当前 module 的 lowering trace。
     */
    val loweringTrace: List<String> = emptyList(),
)

/**
 * CHIR 模块到 JVM 类的降级入口。当前采用一个模块一个 facade class，顶层函数生成为 public static 方法。
 */
class JvmModuleCodegen(
    /**
     * 当前 JVM 后端上下文。
     */
    private val context: JvmBackendContext,
    /**
     * 待生成 JVM class 的 CHIR module。
     */
    private val module: ChirModule,
) {
    /**
     * 生成 module facade、自定义类型 class 以及必要的运行时 helper class。
     */
    fun generate(): JvmModuleCodegenResult {
        val classCodegen = JvmClassCodegen(context, module)
        val facadeArtifact = classCodegen.generate()
        val typeArtifacts = module.customTypeDeclarationsForCodegen()
            .map { declaration -> JvmTypeDeclarationCodegen(context, module, declaration).generate() }
        val runtimeArtifacts = buildList {
            if (module.requiresPointerRuntime()) {
                add(JvmRuntimeArtifacts.pointerRuntimeArtifact())
            }
            if (module.requiresUnsignedRuntime()) {
                add(JvmRuntimeArtifacts.unsignedRuntimeArtifact())
            }
        }
        val hasMainBridge = module.declarations
            .filterIsInstance<ChirFunctionDeclaration>()
            .any(classCodegen::canGenerateMainBridge)
        return JvmModuleCodegenResult(
            classes = listOf(facadeArtifact) + typeArtifacts + runtimeArtifacts,
            mainClassInternalName = facadeArtifact.internalName.takeIf { hasMainBridge },
            loweringTrace = buildList {
                add("jvm.module=${module.name} facade=${facadeArtifact.internalName}")
                typeArtifacts.forEach { artifact -> add("jvm.type=${artifact.internalName}") }
                runtimeArtifacts.forEach { artifact -> add("jvm.runtime=${artifact.internalName}") }
            },
        )
    }

    /**
     * 判断当前 module 是否需要输出 pointer runtime helper。
     */
    private fun ChirModule.requiresPointerRuntime(): Boolean {
        return declarations.any(::declarationRequiresPointerRuntime)
    }

    /**
     * 判断当前 module 是否需要输出 unsigned runtime helper。
     */
    private fun ChirModule.requiresUnsignedRuntime(): Boolean {
        return declarations.any(::declarationRequiresUnsignedRuntime)
    }

    /**
     * CHIR 类型成员中可以继续声明类型；JVM 后端必须为每个可达类型声明生成 classfile。
     */
    private fun ChirModule.customTypeDeclarationsForCodegen(): List<ChirCustomTypeDeclaration> = buildList {
        fun addCustomTypeDeclaration(declaration: ChirCustomTypeDeclaration) {
            add(declaration)
            declaration.effectiveMemberDeclarations()
                .filterIsInstance<ChirCustomTypeDeclaration>()
                .forEach(::addCustomTypeDeclaration)
        }
        declarations
            .filterIsInstance<ChirCustomTypeDeclaration>()
            .forEach(::addCustomTypeDeclaration)
    }.distinctBy { it.semanticId }

    /**
     * 判断声明及其子结构是否引用了 pointer runtime。
     */
    private fun declarationRequiresPointerRuntime(declaration: ChirDeclaration): Boolean {
        return when (declaration) {
            is ChirVariableDeclaration -> typeRequiresPointerRuntime(declaration.type)
            is ChirFunctionDeclaration ->
                typeRequiresPointerRuntime(declaration.returnType) ||
                    declaration.parameters.any { typeRequiresPointerRuntime(it.type) } ||
                    declaration.blocks.any { block ->
                        block.expressions.any(::expressionRequiresPointerRuntime) ||
                            listOfNotNull(
                                (block.terminator as? org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator)?.returnValue,
                                (block.terminator as? org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator)?.exceptionValue,
                                (block.terminator as? org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator)?.condition,
                            ).any(::valueRequiresPointerRuntime)
                    }
            is ChirCustomTypeDeclaration -> declaration.effectiveMemberDeclarations().any(::declarationRequiresPointerRuntime)
            else -> false
        }
    }

    /**
     * 判断表达式是否引用了 pointer runtime。
     */
    private fun expressionRequiresPointerRuntime(expression: ChirExpression): Boolean {
        return when (expression) {
            is ChirMemoryExpression ->
                typeRequiresPointerRuntime(expression.address.type) ||
                    expression.value?.let(::valueRequiresPointerRuntime) == true ||
                    expression.resultType?.let(::typeRequiresPointerRuntime) == true
            is ChirCallExpression ->
                valueRequiresPointerRuntime(expression.callee) ||
                    expression.arguments.any(::valueRequiresPointerRuntime) ||
                    typeRequiresPointerRuntime(expression.resultType)
            is ChirOtherExpression ->
                expression.operation.lowercase().trim() in pointerRuntimeOperations ||
                    expression.operands.any(::valueRequiresPointerRuntime) ||
                    expression.resultType?.let(::typeRequiresPointerRuntime) == true
            else -> expression.resultType?.let(::typeRequiresPointerRuntime) == true
        }
    }

    /**
     * 判断声明及其子结构是否引用了 unsigned runtime。
     */
    private fun declarationRequiresUnsignedRuntime(declaration: ChirDeclaration): Boolean {
        return when (declaration) {
            is ChirFunctionDeclaration -> declaration.blocks.any { block ->
                block.expressions.any(::expressionRequiresUnsignedRuntime)
            }
            is ChirCustomTypeDeclaration -> declaration.effectiveMemberDeclarations().any(::declarationRequiresUnsignedRuntime)
            else -> false
        }
    }

    /**
     * 判断表达式是否需要 unsigned runtime 辅助转换。
     */
    private fun expressionRequiresUnsignedRuntime(expression: ChirExpression): Boolean {
        return expression is ChirOtherExpression && expression.operation.lowercase().trim() == "fptoui"
    }

    /**
     * 判断 CHIR value 的类型是否需要 pointer runtime。
     */
    private fun valueRequiresPointerRuntime(value: ChirValue): Boolean = typeRequiresPointerRuntime(value.type)

    /**
     * 判断 CHIR type ref 是否递归包含 pointer 类型。
     */
    private fun typeRequiresPointerRuntime(typeRef: ChirTypeRef): Boolean {
        val type = (typeRef as? ChirResolvedTypeRef)?.type ?: return false
        return typeRequiresPointerRuntime(type)
    }

    /**
     * 判断 CHIR type 是否递归包含 pointer 类型。
     */
    private fun typeRequiresPointerRuntime(type: ChirType): Boolean {
        return when (type) {
            is ChirCPointerType -> true
            is ChirRawArrayType -> typeRequiresPointerRuntime(type.elementType)
            is ChirVArrayType -> typeRequiresPointerRuntime(type.elementType)
            is ChirRefType -> typeRequiresPointerRuntime(type.referencedType)
            is ChirBoxType -> typeRequiresPointerRuntime(type.boxedType)
            is ChirTupleType -> type.elementTypes.any(::typeRequiresPointerRuntime)
            is ChirFunctionType ->
                type.parameterTypes.any(::typeRequiresPointerRuntime) ||
                    typeRequiresPointerRuntime(type.returnType) ||
                    type.receiverType?.let(::typeRequiresPointerRuntime) == true
            is ChirClassType ->
                type.fieldTypes.any(::typeRequiresPointerRuntime) ||
                    type.superTypes.any(::typeRequiresPointerRuntime) ||
                    type.typeArguments.any(::typeRequiresPointerRuntime)
            is ChirStructType ->
                type.fieldTypes.any(::typeRequiresPointerRuntime) ||
                    type.typeArguments.any(::typeRequiresPointerRuntime)
            is ChirEnumType ->
                type.cases.any { enumCase -> enumCase.payloadTypes.any(::typeRequiresPointerRuntime) } ||
                    type.typeArguments.any(::typeRequiresPointerRuntime)
            is ChirNamedType -> type.typeArguments.any(::typeRequiresPointerRuntime)
            is ChirGenericType -> type.upperBounds.any(::typeRequiresPointerRuntime)
            else -> false
        }
    }

    private companion object {
        /**
         * 需要 pointer runtime 的 CHIR other operation 名称。
         */
        val pointerRuntimeOperations = setOf("ptrtoint", "inttoptr")
    }
}
