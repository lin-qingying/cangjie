package org.cangnova.cangjie.codegen.module

import org.cangnova.cangjie.chir.core.declaration.ChirEnumDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirStructDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirTypeDeclaration
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.codegen.context.CGContext
import org.cangnova.cangjie.codegen.dispatcher.ExpressionLoweringDispatcher
import org.cangnova.cangjie.codegen.function.CGFunction
import org.cangnova.cangjie.codegen.ir.LlvmFunctionArtifact
import org.cangnova.cangjie.codegen.ir.LlvmModuleArtifact
import org.cangnova.cangjie.codegen.ir.parseLlvmSignature
import org.cangnova.cangjie.codegen.ir.sanitizeIdentifier

class CGModule(
    private val context: CGContext,
    private val module: ChirModule,
    private val dispatcher: ExpressionLoweringDispatcher = ExpressionLoweringDispatcher(),
) {
    fun lower(): LlvmModuleArtifact {
        val lines = mutableListOf<String>()
        if (context.options.emitModuleHeader) {
            lines += "; module ${context.moduleName(module)}"
            lines += "target triple = \"${context.options.targetTriple}\""
            lines += "target datalayout = \"${context.options.targetDataLayout}\""
        }
        if (context.options.emitComments) {
            lines += "; package ${context.inputPackage.name} access=${context.inputPackage.accessLevel}"
        }

        lines += emitTypeDeclarations()
        lines += emitGlobalDeclarations()
        lines += emitRuntimeDeclarations()

        val functions = module.declarations
            .asSequence()
            .mapNotNull { it as? ChirFunctionDeclaration }
            .map { CGFunction(it, context, dispatcher).lower() }
            .toList()
        lines += functions.flatMap { it.ir.lines() }

        val moduleIr = lines.filter { it.isNotBlank() }.joinToString(System.lineSeparator())
        return LlvmModuleArtifact(
            name = context.moduleName(module),
            ir = moduleIr,
            functions = functions,
            bitcode = if (context.options.emitBitcode) moduleIr.toByteArray() else null,
        )
    }

    private fun emitTypeDeclarations(): List<String> {
        val declarations = mutableListOf<String>()
        val candidateTypes = linkedSetOf<ChirTypeDeclaration>()
        candidateTypes += context.inputPackage.typeDefinitions
        candidateTypes += context.inputPackage.importedTypeDefinitions
        module.declarations.filterIsInstance<ChirTypeDeclaration>().forEach(candidateTypes::add)

        candidateTypes.forEach { declaration ->
            when (declaration) {
                is ChirStructDeclaration -> {
                    val fields = declaration.fieldDeclarations.joinToString(", ") { context.typeLowering.lower(it.type) }
                    declarations += "%struct.${sanitizeIdentifier(declaration.name, "struct")} = type { $fields }"
                }
                is ChirEnumDeclaration -> {
                    declarations += "%enum.${sanitizeIdentifier(declaration.name, "enum")} = type { i32 }"
                }
                else -> {
                    declarations += "%type.${sanitizeIdentifier(declaration.name, "type")} = type opaque"
                }
            }
        }
        return declarations
    }

    private fun emitGlobalDeclarations(): List<String> {
        val declarations = mutableListOf<String>()
        context.inputPackage.members.globalVariables.forEach { variable ->
            val ty = context.typeLowering.lower(variable.type)
            val name = sanitizeIdentifier(variable.name, "global")
            val initializer = if (variable.mutable) defaultGlobalInitializer(ty) else defaultGlobalInitializer(ty)
            declarations += "@$name = global $ty $initializer"
        }
        context.inputPackage.members.importedVariables.forEach { variable ->
            val ty = context.typeLowering.lower(variable.type)
            val name = sanitizeIdentifier(variable.name, "imported_global")
            declarations += "@$name = external global $ty"
        }
        context.inputPackage.members.importedFunctions.forEach { function ->
            val name = sanitizeIdentifier(function.name, "imported_fn")
            val returnType = context.typeLowering.lower(function.returnType)
            val params = function.parameters.joinToString(", ") { context.typeLowering.lower(it.type) }
            declarations += "declare $returnType @$name($params)"
        }
        return declarations
    }

    private fun emitRuntimeDeclarations(): List<String> {
        if (!context.options.emitRuntimeDeclarations) return emptyList()
        return context.runtimeSymbols.allSymbols().mapNotNull { symbol ->
            val signature = parseLlvmSignature(symbol.llvmSignature) ?: return@mapNotNull null
            "declare ${signature.returnType} @${symbol.name}(${signature.argumentTypes.joinToString(", ")})"
        }
    }

    private fun defaultGlobalInitializer(llvmType: String): String {
        return when {
            llvmType == "i1" -> "0"
            llvmType.matches(Regex("i\\d+")) -> "0"
            llvmType == "half" || llvmType == "float" || llvmType == "double" -> "0.0"
            llvmType == "ptr" -> "null"
            else -> "zeroinitializer"
        }
    }
}

