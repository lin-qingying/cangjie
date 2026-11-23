package org.cangnova.cangjie.wasm.codegen.pipeline

import org.cangnova.cangjie.chir.core.checker.ChirValidationReportFormatter
import org.cangnova.cangjie.chir.core.checker.DefaultChirValidator
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirPackageAccessLevel
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirFunctionValue
import org.cangnova.cangjie.chir.core.attribute.ChirBooleanAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.wasm.codegen.api.ChirToWasmCodeGenerator
import org.cangnova.cangjie.wasm.codegen.api.WasmCodegenInput
import org.cangnova.cangjie.wasm.codegen.api.WasmCodegenOutput
import org.cangnova.cangjie.wasm.codegen.diagnostics.WasmCodegenException
import org.cangnova.cangjie.wasm.codegen.lowering.ChirFunctionLowerer
import org.cangnova.cangjie.wasm.codegen.lowering.WasmCodegenPlan
import org.cangnova.cangjie.wasm.ir.WasmBinaryWriter
import org.cangnova.cangjie.wasm.ir.WasmExport
import org.cangnova.cangjie.wasm.ir.WasmFunction
import org.cangnova.cangjie.wasm.ir.WasmFunctionType
import org.cangnova.cangjie.wasm.ir.WasmInstruction
import org.cangnova.cangjie.wasm.ir.WasmMemory
import org.cangnova.cangjie.wasm.ir.WasmMemoryLimits
import org.cangnova.cangjie.wasm.ir.WasmModule
import org.cangnova.cangjie.wasm.ir.WasmTable
import org.cangnova.cangjie.wasm.ir.WasmTableLimits
import org.cangnova.cangjie.wasm.ir.WasmElementSegment
import org.cangnova.cangjie.wasm.ir.WasmValueType

/** 默认独立 CHIR→Wasm Core 后端实现。 */
class DefaultChirToWasmCodeGenerator(
    private val binaryWriter: WasmBinaryWriter = WasmBinaryWriter(),
) : ChirToWasmCodeGenerator {
    override fun generate(input: WasmCodegenInput): WasmCodegenOutput {
        val validation = DefaultChirValidator().validatePackage(input.chirPackage)
        if (validation.hasErrors) {
            throw WasmCodegenException(
                "invalid CHIR package '${input.chirPackage.name}' before Wasm lowering:\n" +
                    ChirValidationReportFormatter.render(validation),
                input.chirPackage.semanticId,
            )
        }

        val plan = WasmCodegenPlan(input)
        val definedFunctions = plan.definedFunctions.map { ChirFunctionLowerer(plan, it).lower() }.toMutableList()
        val exports = mutableListOf<WasmExport>()

        val entryExport = input.options.target.profile.entryExportName
        val requestedEntry = if (input.options.target.hostAbi == org.cangnova.cangjie.wasm.common.WasmHostAbi.WASI_PREVIEW_1) {
            input.options.entryFunctionName
        } else {
            null
        }
        if (requestedEntry != null && entryExport != null) {
            val entry = requireEntryFunction(plan, requestedEntry, input.chirPackage.semanticId)
            definedFunctions += buildWasiEntryFunction(plan, entry)
            exports += WasmExport.Function(entryExport, WASI_ENTRY_FUNCTION)
        }

        exports += requestedExports(plan)
        if (input.options.target.hostAbi == org.cangnova.cangjie.wasm.common.WasmHostAbi.CORE_MODULE) {
            input.chirPackage.packageLiteralInitFunctionId?.let { functionId ->
                exports += WasmExport.Function("__cangjie_package_literal_init", plan.functionSymbolName(functionId))
            }
            input.chirPackage.packageInitFunctionId?.let { functionId ->
                exports += WasmExport.Function("__cangjie_package_init", plan.functionSymbolName(functionId))
            }
        }
        input.options.target.profile.memoryExportName?.let { memoryExport ->
            exports += WasmExport.Memory(memoryExport, WASM_MEMORY_NAME)
        }
        val duplicateExport = exports.groupingBy(WasmExport::externalName).eachCount().entries.firstOrNull { it.value > 1 }
        if (duplicateExport != null) {
            throw WasmCodegenException("duplicate Wasm export name '${duplicateExport.key}'", input.chirPackage.semanticId)
        }

        val requiredDataPages = pagesForDataEnd(plan.dataPool.stackStartOffset())
        if (requiredDataPages > input.options.initialMemoryPages) {
            throw WasmCodegenException(
                "static Wasm data and stack base require $requiredDataPages memory pages, " +
                    "but initialMemoryPages is ${input.options.initialMemoryPages}",
                input.chirPackage.semanticId,
            )
        }

        val functionTableNames = plan.functionTableNames + definedFunctions
            .filter { it.name == WASI_ENTRY_FUNCTION }
            .map(WasmFunction::name)
        val functionTable = functionTableNames.takeIf { it.isNotEmpty() }?.let { names ->
            WasmTable(plan.functionTableName, WasmTableLimits(names.size.toUInt()))
        }
        val elementSegments = functionTable?.let {
            listOf(WasmElementSegment(it.name, offset = 0u, functionNames = functionTableNames))
        }.orEmpty()

        val module = WasmModule(
            name = input.chirPackage.name,
            imports = plan.imports,
            globalImports = plan.globalImports,
            functions = definedFunctions,
            globals = plan.globals,
            table = functionTable,
            elementSegments = elementSegments,
            memory = WasmMemory(
                name = WASM_MEMORY_NAME,
                limits = WasmMemoryLimits(input.options.initialMemoryPages, input.options.maximumMemoryPages),
            ),
            exports = exports,
            startFunctionName = null,
            dataSegments = plan.dataPool.moduleSegments(),
        )
        val bytes = try {
            binaryWriter.write(module, input.options.emitNameSection)
        } catch (failure: IllegalArgumentException) {
            throw WasmCodegenException("Wasm module verification or binary emission failed: ${failure.message}", cause = failure)
        }
        return WasmCodegenOutput(
            target = input.options.target,
            moduleName = input.chirPackage.name,
            bytes = bytes,
            imports = plan.imports,
            globalImports = plan.globalImports,
            exports = module.exports,
        )
    }

    private fun requireEntryFunction(
        plan: WasmCodegenPlan,
        name: String,
        packageId: ChirSemanticId,
    ): ChirFunctionDeclaration {
        val matches = plan.definedFunctions.filter { it.name == name }
        if (matches.size != 1) {
            throw WasmCodegenException(
                "WASI entry '$name' must identify exactly one defined CHIR function; found ${matches.size}",
                packageId,
            )
        }
        return matches.single()
    }

    private fun buildWasiEntryFunction(plan: WasmCodegenPlan, entry: ChirFunctionDeclaration): WasmFunction {
        if (entry.parameters.isNotEmpty()) {
            throw WasmCodegenException("WASI entry '${entry.name}' must have no parameters", entry.semanticId)
        }
        val entryType = plan.functionTypeById.getValue(entry.semanticId)
        val body = mutableListOf<WasmInstruction>()
        plan.chirPackage.packageLiteralInitFunctionId?.let { body += WasmInstruction.Call(plan.functionSymbolName(it)) }
        plan.chirPackage.packageInitFunctionId?.let { body += WasmInstruction.Call(plan.functionSymbolName(it)) }
        body += WasmInstruction.Call(plan.definedFunctionName(entry.semanticId))
        when {
            entry.returnType == ChirResolvedTypeRef(ChirPrimitiveType.UNIT) ||
                entry.returnType == ChirResolvedTypeRef(ChirPrimitiveType.VOID) -> Unit
            entry.returnType == ChirResolvedTypeRef(ChirPrimitiveType.INT32) ||
                entry.returnType == ChirResolvedTypeRef(ChirPrimitiveType.UINT32) -> {
                if (entryType.results != listOf(WasmValueType.I32)) {
                    throw WasmCodegenException("WASI entry '${entry.name}' has an inconsistent Int32 result signature", entry.semanticId)
                }
                body += WasmInstruction.Call(plan.wasiExitFunctionName(entry.semanticId))
                body += WasmInstruction.Unreachable
            }
            else -> throw WasmCodegenException(
                "WASI entry '${entry.name}' must return Unit or Int32, got ${entry.returnType.renderName}",
                entry.semanticId,
            )
        }
        return WasmFunction(
            name = WASI_ENTRY_FUNCTION,
            type = WasmFunctionType(emptyList(), emptyList()),
            parameters = emptyList(),
            locals = emptyList(),
            body = body,
        )
    }

    private fun requestedExports(plan: WasmCodegenPlan): List<WasmExport.Function> {
        val requestedNames = plan.input.options.exportedFunctionNames
        if (requestedNames != null) {
            return requestedNames.sorted().map { name ->
                val matches = plan.definedFunctions.filter { it.name == name }
                if (matches.size != 1) {
                    throw WasmCodegenException(
                        "export '$name' must identify exactly one defined CHIR function; found ${matches.size}",
                        plan.chirPackage.semanticId,
                    )
                }
                WasmExport.Function(name, plan.definedFunctionName(matches.single().semanticId))
            }
        }
        if (!plan.targetProfile.defaultExportPublicFunctions) return emptyList()
        return plan.definedFunctions.filter { it.isExportedByDefault(plan.chirPackage.accessLevel) }
            .map { function -> WasmExport.Function(function.exportName(), plan.definedFunctionName(function.semanticId)) }
    }

    private fun ChirFunctionDeclaration.isExportedByDefault(packageAccess: ChirPackageAccessLevel): Boolean {
        val visibility = attributes.filterIsInstance<ChirStringAttribute>()
            .firstOrNull { it.key == "visibility" || it.key == "access" }
            ?.value
            ?.lowercase()
        if (visibility != null) return visibility == "public" || visibility == "external"
        val explicitPublic = attributes.filterIsInstance<ChirBooleanAttribute>()
            .firstOrNull { it.key == "public" || it.key == "wasm.export" }
            ?.enabled
        return explicitPublic ?: (packageAccess == ChirPackageAccessLevel.PUBLIC)
    }

    private fun ChirFunctionDeclaration.exportName(): String =
        attributes.filterIsInstance<ChirStringAttribute>()
            .firstOrNull { it.key == "wasm.export.name" }
            ?.value
            ?: name

    private fun pagesForDataEnd(dataEnd: UInt): UInt {
        val pages = (dataEnd.toULong() + WASM_PAGE_SIZE - 1uL) / WASM_PAGE_SIZE
        if (pages > 65_536uL) throw WasmCodegenException("static Wasm data exceeds the memory32 limit")
        return maxOf(1u, pages.toUInt())
    }

    private companion object {
        const val WASM_MEMORY_NAME = "memory"
        const val WASI_ENTRY_FUNCTION = "__wasm_wasi_start"
        const val WASM_PAGE_SIZE = 65_536uL
    }
}
