package org.cangnova.cangjie.codegen.backend

import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendMissingSymbolsException
import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendVersionMismatchException

class NativeInteropLlvmBackendApi(
    private val tool: String,
    private val requiredMajorVersion: Int,
    private val requiredSymbols: Set<String> = defaultRequiredSymbols,
    private val runner: NativeInteropToolRunner = ProcessNativeInteropToolRunner(),
) : LlvmBackendApi {
    override val id: String = "native-interop:$tool"

    override fun initialize() {
        val probe = runner.probe(tool)
        val actualMajor = parseMajorVersion(probe.llvmVersion)
        if (actualMajor != requiredMajorVersion) {
            throw LlvmBackendVersionMismatchException(
                expectedMajor = requiredMajorVersion,
                actualVersion = probe.llvmVersion,
            )
        }
        val missingSymbols = requiredSymbols - probe.symbols
        if (missingSymbols.isNotEmpty()) {
            throw LlvmBackendMissingSymbolsException(missingSymbols)
        }
    }

    override fun emitBitcode(moduleName: String, llvmIr: String): ByteArray {
        return runner.emitBitcode(tool, moduleName, llvmIr)
    }

    private fun parseMajorVersion(version: String): Int {
        return version.takeWhile { it.isDigit() }.toIntOrNull() ?: -1
    }

    private companion object {
        val defaultRequiredSymbols = setOf(
            "LLVMGetVersion",
            "LLVMContextCreate",
            "LLVMModuleCreateWithName",
            "LLVMPrintModuleToString",
        )
    }
}

