package org.cangnova.cangjie.codegen.types

import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirThrowTerminator
import org.cangnova.cangjie.chir.core.declaration.ChirClassDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirCustomTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirExtendDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.type.ChirBoxType
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirCStringType
import org.cangnova.cangjie.chir.core.type.ChirEnumType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirThisType
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirUnresolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirVArrayType
import org.cangnova.cangjie.chir.core.value.ChirValue
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException
import org.cangnova.cangjie.codegen.ir.sanitizeIdentifier

internal enum class LlvmNominalTypeKind(val prefix: String) {
    TYPE("type"),
    STRUCT("struct"),
    CLASS("class"),
    ENUM("enum"),
    VARRAY("varray"),
    GENERIC("generic"),
    BOX("box"),
    THIS("this"),
}

/**
 * 后端统一的 LLVM identified type 命名入口。
 *
 * CHIR 名称可能包含包路径、泛型实参或编译期展示字符；所有 LLVM 类型声明和类型引用
 * 必须经过同一个规范化函数，避免声明处和使用处生成不同的 `%struct.*` 名称。
 */
internal fun llvmNominalTypeName(kind: LlvmNominalTypeKind, rawName: String): String =
    "%${kind.prefix}.${sanitizeIdentifier(rawName, kind.prefix)}"

internal fun llvmNominalTypeName(type: ChirType): String = when (type) {
    is ChirNamedType -> llvmNominalTypeName(LlvmNominalTypeKind.TYPE, nominalRawName(type.renderName, type.typeArguments))
    is ChirStructType -> llvmNominalTypeName(LlvmNominalTypeKind.STRUCT, nominalRawName(type.name, type.typeArguments))
    is ChirClassType -> llvmNominalTypeName(LlvmNominalTypeKind.CLASS, nominalRawName(type.name, type.typeArguments))
    is ChirEnumType -> llvmNominalTypeName(LlvmNominalTypeKind.ENUM, nominalRawName(type.name, type.typeArguments))
    is ChirVArrayType -> llvmNominalTypeName(LlvmNominalTypeKind.VARRAY, "${type.rank}.${type.elementType.renderName}")
    is ChirGenericType -> llvmNominalTypeName(LlvmNominalTypeKind.GENERIC, type.identifier)
    is ChirBoxType -> llvmNominalTypeName(LlvmNominalTypeKind.BOX, type.boxedType.renderName)
    is ChirThisType -> llvmNominalTypeName(LlvmNominalTypeKind.THIS, type.ownerTypeName)
    else -> throw CodegenLoweringException("type ${type.renderName} is not an LLVM nominal type", null)
}

internal fun collectLlvmTypeDeclarations(
    chirPackage: ChirPackage,
    module: ChirModule,
    typeLowering: TypeLowering,
): List<String> {
    val collector = LlvmTypeDeclarationCollector(typeLowering)
    chirPackage.typeDefinitions.forEach(collector::recordDeclaredType)
    chirPackage.importedTypeDefinitions.forEach(collector::recordDeclaredType)
    module.declarations.filterIsInstance<ChirTypeDeclaration>().forEach(collector::recordDeclaredType)

    chirPackage.members.globalVariables.forEach(collector::visitDeclaration)
    chirPackage.members.globalFunctions.forEach(collector::visitDeclaration)
    chirPackage.members.importedVariables.forEach(collector::visitDeclaration)
    chirPackage.members.importedFunctions.forEach(collector::visitDeclaration)
    module.declarations.forEach(collector::visitDeclaration)

    return collector.declarations()
}

private fun nominalRawName(baseName: String, typeArguments: List<ChirTypeRef>): String {
    if (typeArguments.isEmpty()) return baseName
    return buildString {
        append(baseName)
        append('<')
        append(typeArguments.joinToString(",") { it.renderName })
        append('>')
    }
}

private enum class TypeDeclarationBodyKind {
    OPAQUE,
    DEFINED,
}

private data class TypeDeclarationLine(
    val line: String,
    val bodyKind: TypeDeclarationBodyKind,
)

private class LlvmTypeDeclarationCollector(
    private val typeLowering: TypeLowering,
) {
    private val declarationsByName = linkedMapOf<String, TypeDeclarationLine>()
    private val visitedTypes = mutableSetOf<String>()

    fun declarations(): List<String> = declarationsByName.values.map { it.line }

    fun recordDeclaredType(declaration: ChirTypeDeclaration) {
        when (declaration) {
            is ChirStructDeclaration -> {
                declaration.fieldDeclarations.forEach(::visitDeclaration)
                recordDefined(
                    llvmNominalTypeName(LlvmNominalTypeKind.STRUCT, nominalRawName(declaration.name, declaration.typeParameters.map(::genericTypeRef))),
                    declaration.fieldDeclarations.map { it.type },
                )
                visitCustomTypeMembers(declaration)
            }
            is ChirClassDeclaration -> {
                declaration.superTypes.forEach(::visitTypeRef)
                declaration.implementedTypes.forEach(::visitTypeRef)
                val fieldTypes = declaration.memberDeclarations.filterIsInstance<ChirVariableDeclaration>().map { it.type }
                fieldTypes.forEach(::visitTypeRef)
                val name = llvmNominalTypeName(
                    LlvmNominalTypeKind.CLASS,
                    nominalRawName(declaration.name, declaration.typeParameters.map(::genericTypeRef)),
                )
                if (fieldTypes.isEmpty()) {
                    recordOpaque(name)
                } else {
                    recordDefined(name, fieldTypes)
                }
                visitCustomTypeMembers(declaration)
            }
            is ChirEnumDeclaration -> {
                recordLine(
                    llvmNominalTypeName(LlvmNominalTypeKind.ENUM, nominalRawName(declaration.name, declaration.typeParameters.map(::genericTypeRef))),
                    "type { i32 }",
                    TypeDeclarationBodyKind.DEFINED,
                )
                visitCustomTypeMembers(declaration)
            }
            is ChirExtendDeclaration -> {
                visitTypeRef(declaration.targetType)
                declaration.extendedTypes.forEach(::visitTypeRef)
                visitCustomTypeMembers(declaration)
            }
            else -> recordOpaque(
                llvmNominalTypeName(LlvmNominalTypeKind.TYPE, nominalRawName(declaration.name, declaration.typeParameters.map(::genericTypeRef))),
            )
        }
    }

    fun visitDeclaration(declaration: ChirDeclaration) {
        when (declaration) {
            is ChirTypeDeclaration -> recordDeclaredType(declaration)
            is ChirVariableDeclaration -> visitTypeRef(declaration.type)
            is ChirFunctionDeclaration -> visitFunctionDeclaration(declaration)
        }
    }

    private fun visitCustomTypeMembers(declaration: ChirCustomTypeDeclaration) {
        declaration.memberDeclarations.forEach(::visitDeclaration)
    }

    private fun visitFunctionDeclaration(function: ChirFunctionDeclaration) {
        visitTypeRef(function.returnType)
        function.parameters.forEach(::visitDeclaration)
        function.blocks.forEach { block ->
            block.expressions.forEach(::visitExpression)
            visitTerminator(block.terminator)
        }
    }

    private fun visitExpression(expression: ChirExpression) {
        expression.resultType?.let(::visitTypeRef)
        when (expression) {
            is ChirUnaryExpression -> visitValue(expression.operand)
            is ChirBinaryExpression -> {
                visitValue(expression.left)
                visitValue(expression.right)
            }
            is ChirMemoryExpression -> {
                visitValue(expression.address)
                expression.value?.let(::visitValue)
            }
            is ChirCallExpression -> {
                visitValue(expression.callee)
                expression.arguments.forEach(::visitValue)
            }
            is ChirOtherExpression -> expression.operands.forEach(::visitValue)
        }
    }

    private fun visitTerminator(terminator: ChirTerminator) {
        when (terminator) {
            is ChirReturnTerminator -> terminator.returnValue?.let(::visitValue)
            is ChirConditionalBranchTerminator -> visitValue(terminator.condition)
            is ChirThrowTerminator -> visitValue(terminator.exceptionValue)
        }
    }

    private fun visitValue(value: ChirValue) {
        visitTypeRef(value.type)
    }

    private fun visitTypeRef(typeRef: ChirTypeRef) {
        when (typeRef) {
            is ChirResolvedTypeRef -> visitType(typeRef.type)
            is ChirUnresolvedTypeRef -> throw CodegenLoweringException(
                "cannot collect LLVM declaration for unresolved CHIR type '${typeRef.symbol}'",
                null,
            )
        }
    }

    private fun visitType(type: ChirType) {
        val key = type.renderNameWithArguments()
        if (!visitedTypes.add("${type::class.qualifiedName}:$key")) return
        when (type) {
            is ChirNamedType -> {
                type.typeArguments.forEach(::visitTypeRef)
                recordOpaque(llvmNominalTypeName(type))
            }
            is ChirTupleType -> type.elementTypes.forEach(::visitTypeRef)
            is ChirFunctionType -> {
                type.receiverType?.let(::visitTypeRef)
                type.parameterTypes.forEach(::visitTypeRef)
                visitTypeRef(type.returnType)
            }
            is ChirStructType -> {
                type.typeArguments.forEach(::visitTypeRef)
                type.fieldTypes.forEach(::visitTypeRef)
                recordDefined(llvmNominalTypeName(type), type.fieldTypes)
            }
            is ChirClassType -> {
                type.typeArguments.forEach(::visitTypeRef)
                type.superTypes.forEach(::visitTypeRef)
                type.fieldTypes.forEach(::visitTypeRef)
                if (type.fieldTypes.isEmpty()) {
                    recordOpaque(llvmNominalTypeName(type))
                } else {
                    recordDefined(llvmNominalTypeName(type), type.fieldTypes)
                }
            }
            is ChirEnumType -> {
                type.typeArguments.forEach(::visitTypeRef)
                type.cases.flatMap { it.payloadTypes }.forEach(::visitTypeRef)
                recordLine(llvmNominalTypeName(type), "type { i32 }", TypeDeclarationBodyKind.DEFINED)
            }
            is ChirRawArrayType -> visitTypeRef(type.elementType)
            is ChirVArrayType -> {
                visitTypeRef(type.elementType)
                recordOpaque(llvmNominalTypeName(type))
            }
            is ChirCPointerType -> visitTypeRef(type.pointeeType)
            is ChirGenericType -> {
                type.upperBounds.forEach(::visitTypeRef)
                recordOpaque(llvmNominalTypeName(type))
            }
            is ChirRefType -> visitTypeRef(type.referencedType)
            is ChirBoxType -> {
                visitTypeRef(type.boxedType)
                recordDefined(llvmNominalTypeName(type), listOf(type.boxedType))
            }
            is ChirThisType -> recordOpaque(llvmNominalTypeName(type))
            is ChirPrimitiveType,
            is ChirCStringType,
            -> Unit
        }
    }

    private fun recordDefined(typeName: String, fieldTypes: List<ChirTypeRef>) {
        val fields = fieldTypes.joinToString(", ") { typeLowering.lower(it) }
        val body = if (fields.isEmpty()) "type { }" else "type { $fields }"
        recordLine(typeName, body, TypeDeclarationBodyKind.DEFINED)
    }

    private fun recordOpaque(typeName: String) {
        recordLine(typeName, "type opaque", TypeDeclarationBodyKind.OPAQUE)
    }

    private fun recordLine(typeName: String, body: String, bodyKind: TypeDeclarationBodyKind) {
        val line = "$typeName = $body"
        val existing = declarationsByName[typeName]
        if (existing == null) {
            declarationsByName[typeName] = TypeDeclarationLine(line, bodyKind)
            return
        }
        if (existing.line == line) return
        if (existing.bodyKind == TypeDeclarationBodyKind.OPAQUE && bodyKind == TypeDeclarationBodyKind.DEFINED) {
            declarationsByName[typeName] = TypeDeclarationLine(line, bodyKind)
            return
        }
        if (existing.bodyKind == TypeDeclarationBodyKind.DEFINED && bodyKind == TypeDeclarationBodyKind.OPAQUE) {
            return
        }
        throw CodegenLoweringException(
            "conflicting LLVM type declaration for $typeName: '${existing.line}' vs '$line'",
            null,
        )
    }

    private fun genericTypeRef(name: String): ChirTypeRef = ChirResolvedTypeRef(ChirGenericType(name))

    private fun ChirType.renderNameWithArguments(): String = when (this) {
        is ChirNamedType -> nominalRawName(renderName, typeArguments)
        is ChirStructType -> nominalRawName(name, typeArguments)
        is ChirClassType -> nominalRawName(name, typeArguments)
        is ChirEnumType -> nominalRawName(name, typeArguments)
        is ChirVArrayType -> renderName
        is ChirGenericType -> identifier
        is ChirBoxType -> renderName
        is ChirThisType -> renderName
        else -> renderName
    }
}
