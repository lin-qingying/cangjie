package org.cangnova.cangjie.codegen.module

import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirBooleanAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.codegen.backend.LlvmBackendApi
import org.cangnova.cangjie.codegen.backend.LlvmBackendEmissionOptions
import org.cangnova.cangjie.codegen.context.CGContext
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException
import org.cangnova.cangjie.codegen.dispatcher.ExpressionLoweringDispatcher
import org.cangnova.cangjie.codegen.function.CGFunction
import org.cangnova.cangjie.codegen.ir.LlvmFunctionArtifact
import org.cangnova.cangjie.codegen.ir.LlvmModuleArtifact
import org.cangnova.cangjie.codegen.ir.parseLlvmSignature
import org.cangnova.cangjie.codegen.ir.sanitizeIdentifier
import org.cangnova.cangjie.codegen.types.collectLlvmTypeDeclarations

class CGModule(
    private val context: CGContext,
    private val module: ChirModule,
    private val backendApi: LlvmBackendApi,
    private val dispatcher: ExpressionLoweringDispatcher = ExpressionLoweringDispatcher(),
    private val emitPackageDefinitions: Boolean = true,
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
        lines += emitRuntimeEntryMappings()

        val functions = (packageFunctionsToLower() + module.declarations.filterIsInstance<ChirFunctionDeclaration>())
            .distinctBy { it.semanticId }
            .map { CGFunction(it, context, dispatcher).lower() }
            .toList()
        lines += functions.flatMap { it.ir.lines() }

        val moduleIr = lines.filter { it.isNotBlank() }.joinToString(System.lineSeparator())
        val moduleName = context.moduleName(module)
        val emissionOptions = LlvmBackendEmissionOptions(
            targetTriple = context.options.targetTriple,
            targetDataLayout = context.options.targetDataLayout,
            targetCpu = context.options.targetCpu,
            targetFeatures = context.options.targetFeatures,
            optimizeModule = context.options.optimizeLlvmModule,
            optimizationLevel = context.options.optimizationLevel,
            relocationMode = context.options.relocationMode,
            codeModel = context.options.codeModel,
        )
        return LlvmModuleArtifact(
            name = moduleName,
            ir = moduleIr,
            functions = functions,
            bitcode = if (context.options.emitBitcode) {
                backendApi.emitBitcode(moduleName, moduleIr, emissionOptions)
            } else {
                null
            },
            objectCode = if (context.options.emitObjectCode) {
                backendApi.emitObjectCode(moduleName, moduleIr, emissionOptions)
            } else {
                null
            },
        )
    }

    private fun emitTypeDeclarations(): List<String> {
        return collectLlvmTypeDeclarations(context.inputPackage, module, context.typeLowering)
    }

    private fun emitGlobalDeclarations(): List<String> {
        val declarations = mutableListOf<String>()
        if (emitPackageDefinitions) {
            context.inputPackage.members.globalVariables.forEach { variable ->
                val ty = context.typeLowering.lower(variable.type)
                val name = sanitizeIdentifier(variable.name, "global")
                val linkage = attributeValue(variable.attributes, "linkage")
                val initializer = attributeValue(variable.attributes, "initializer")
                    ?: defaultGlobalInitializer(variable.name, ty, variable.semanticId)
                val storageClass = if (variable.mutable) "global" else "constant"
                declarations += buildString {
                    append("@$name = ")
                    if (!linkage.isNullOrBlank()) {
                        append(linkage)
                        append(' ')
                    }
                    append("$storageClass $ty $initializer")
                }
            }
        }
        context.inputPackage.members.importedVariables.forEach { variable ->
            val ty = context.typeLowering.lower(variable.type)
            val name = sanitizeIdentifier(variable.name, "imported_global")
            val linkage = attributeValue(variable.attributes, "linkage")
            declarations += buildString {
                append("@$name = ")
                append(if (linkage.isNullOrBlank()) "external" else linkage)
                append(" global $ty")
            }
        }
        context.inputPackage.members.importedFunctions.forEach { function ->
            declarations += renderImportedFunctionDeclaration(function)
        }
        return declarations
    }

    private fun emitRuntimeDeclarations(): List<String> {
        if (!context.options.emitRuntimeDeclarations) return emptyList()
        return context.runtimeSymbols.allSymbols().map { symbol ->
            val signature = parseLlvmSignature(symbol.llvmSignature)
                ?: throw CodegenLoweringException(
                    "runtime symbol ${symbol.name} has invalid LLVM signature '${symbol.llvmSignature}'",
                    null,
                )
            "declare ${signature.returnType} @${symbol.name}(${signature.argumentTypes.joinToString(", ")})"
        }
    }

    private fun emitRuntimeEntryMappings(): List<String> {
        if (!emitPackageDefinitions) return emptyList()
        val localFunctionsById = (context.inputPackage.members.globalFunctions + context.inputPackage.modules.flatMap { it.declarations }
            .filterIsInstance<ChirFunctionDeclaration>())
            .associateBy { it.semanticId }
        val mappings = mutableListOf<String>()

        fun addMapping(functionId: org.cangnova.cangjie.chir.core.identity.ChirSemanticId?, symbolName: String) {
            if (functionId == null) return
            val function = localFunctionsById[functionId] ?: throw CodegenLoweringException(
                "package runtime entry $symbolName references missing function ${functionId.value}",
                functionId,
            )
            val targetName = sanitizeIdentifier(function.name, "fn")
            mappings += "@$symbolName = internal constant ptr @$targetName"
        }

        addMapping(context.inputPackage.packageInitFunctionId, "cangjie.package.init")
        addMapping(context.inputPackage.packageLiteralInitFunctionId, "cangjie.package.literal_init")

        return mappings
    }

    private fun packageFunctionsToLower(): List<ChirFunctionDeclaration> {
        return if (emitPackageDefinitions) {
            context.inputPackage.members.globalFunctions
        } else {
            emptyList()
        }
    }

    private fun defaultGlobalInitializer(
        variableName: String,
        llvmType: String,
        semanticId: org.cangnova.cangjie.chir.core.identity.ChirSemanticId,
    ): String {
        return when {
            llvmType == "i1" -> "0"
            llvmType.matches(Regex("i\\d+")) -> "0"
            llvmType == "half" || llvmType == "float" || llvmType == "double" -> "0.0"
            llvmType == "ptr" -> "null"
            else -> throw CodegenLoweringException(
                "global variable $variableName of LLVM type $llvmType requires an explicit initializer",
                semanticId,
            )
        }
    }

    private fun renderImportedFunctionDeclaration(function: ChirFunctionDeclaration): String {
        val name = sanitizeIdentifier(function.name, "imported_fn")
        val returnType = context.typeLowering.lower(function.returnType)
        val linkage = attributeValue(function.attributes, "linkage")
        val callingConvention = attributeValue(function.attributes, "calling_conv")
            ?: attributeValue(function.attributes, "cc")
        val params = function.parameters.joinToString(", ") { parameter ->
            val parameterType = context.typeLowering.lower(parameter.type)
            val attrs = collectAttributeTokens(parameter.attributes)
            if (attrs.isEmpty()) parameterType else "$parameterType ${attrs.joinToString(" ")}"
        }
        val functionAttrs = collectAttributeTokens(function.attributes)

        return buildString {
            append("declare ")
            if (!linkage.isNullOrBlank()) {
                append(linkage)
                append(' ')
            }
            if (!callingConvention.isNullOrBlank()) {
                append(callingConvention)
                append(' ')
            }
            append(returnType)
            append(" @")
            append(name)
            append('(')
            append(params)
            append(')')
            if (functionAttrs.isNotEmpty()) {
                append(' ')
                append(functionAttrs.joinToString(" "))
            }
        }
    }

    private fun collectAttributeTokens(attributes: Set<ChirAttribute>): List<String> {
        val tokens = mutableListOf<String>()
        attributes.forEach { attribute ->
            when (attribute) {
                is ChirBooleanAttribute -> {
                    if (attribute.enabled && attribute.key !in reservedFunctionAttributeKeys) {
                        tokens += attribute.key
                    }
                }
                is ChirStringAttribute -> {
                    if (attribute.key == "align") {
                        tokens += "align ${attribute.value}"
                    }
                }
            }
        }
        return tokens
    }

    private fun attributeValue(attributes: Set<ChirAttribute>, key: String): String? {
        return attributes.asSequence()
            .filterIsInstance<ChirStringAttribute>()
            .firstOrNull { it.key == key }
            ?.value
    }

    private companion object {
        val reservedFunctionAttributeKeys = setOf("linkage", "personality", "calling_conv", "cc")
    }
}
