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

    private fun ChirModule.requiresPointerRuntime(): Boolean {
        return declarations.any(::declarationRequiresPointerRuntime)
    }

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

    private fun declarationRequiresUnsignedRuntime(declaration: ChirDeclaration): Boolean {
        return when (declaration) {
            is ChirFunctionDeclaration -> declaration.blocks.any { block ->
                block.expressions.any(::expressionRequiresUnsignedRuntime)
            }
            is ChirCustomTypeDeclaration -> declaration.effectiveMemberDeclarations().any(::declarationRequiresUnsignedRuntime)
            else -> false
        }
    }

    private fun expressionRequiresUnsignedRuntime(expression: ChirExpression): Boolean {
        return expression is ChirOtherExpression && expression.operation.lowercase().trim() == "fptoui"
    }

    private fun valueRequiresPointerRuntime(value: ChirValue): Boolean = typeRequiresPointerRuntime(value.type)

    private fun typeRequiresPointerRuntime(typeRef: ChirTypeRef): Boolean {
        val type = (typeRef as? ChirResolvedTypeRef)?.type ?: return false
        return typeRequiresPointerRuntime(type)
    }

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
        val pointerRuntimeOperations = setOf("ptrtoint", "inttoptr")
    }
}
