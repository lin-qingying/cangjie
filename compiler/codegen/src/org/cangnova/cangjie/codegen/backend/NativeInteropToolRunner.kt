package org.cangnova.cangjie.codegen.backend

import org.cangnova.cangjie.codegen.diagnostics.LlvmBackendUnavailableException
import java.io.IOException

data class NativeInteropProbeResult(
    val llvmVersion: String,
    val symbols: Set<String>,
)

interface NativeInteropToolRunner {
    fun probe(tool: String): NativeInteropProbeResult

    fun emitBitcode(tool: String, moduleName: String, llvmIr: String): ByteArray
}

class ProcessNativeInteropToolRunner : NativeInteropToolRunner {
    override fun probe(tool: String): NativeInteropProbeResult {
        val process = run(tool, "probe", "--json")
        val output = process.inputStream.bufferedReader().readText().trim()
        if (process.waitFor() != 0) {
            val stderr = process.errorStream.bufferedReader().readText().trim()
            throw LlvmBackendUnavailableException(
                "native interop backend probe failed for tool '$tool': ${stderr.ifBlank { output.ifBlank { "unknown error" } }}",
            )
        }
        val llvmVersion = extractString(output, "llvmVersion")
            ?: throw LlvmBackendUnavailableException("native interop backend probe output missing llvmVersion")
        val symbols = extractArray(output, "symbols").toSet()
        return NativeInteropProbeResult(llvmVersion = llvmVersion, symbols = symbols)
    }

    override fun emitBitcode(tool: String, moduleName: String, llvmIr: String): ByteArray {
        val process = run(tool, "emit-bitcode", "--module", moduleName)
        process.outputStream.bufferedWriter().use { writer ->
            writer.write(llvmIr)
        }
        val bitcode = process.inputStream.readBytes()
        if (process.waitFor() != 0) {
            val stderr = process.errorStream.bufferedReader().readText().trim()
            throw LlvmBackendUnavailableException(
                "native interop backend bitcode emission failed for module '$moduleName': ${stderr.ifBlank { "unknown error" }}",
            )
        }
        return bitcode
    }

    private fun run(vararg args: String): Process {
        return try {
            ProcessBuilder(*args)
                .redirectErrorStream(false)
                .start()
        } catch (error: IOException) {
            throw LlvmBackendUnavailableException("native interop backend tool '${args.firstOrNull() ?: "<unknown>"}' is unavailable", error)
        }
    }

    private fun extractString(json: String, key: String): String? {
        val regex = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
        return regex.find(json)?.groupValues?.getOrNull(1)
    }

    private fun extractArray(json: String, key: String): List<String> {
        val regex = Regex("\"$key\"\\s*:\\s*\\[(.*?)\\]")
        val body = regex.find(json)?.groupValues?.getOrNull(1)?.trim().orEmpty()
        if (body.isBlank()) return emptyList()
        return body.split(",")
            .map { token -> token.trim().removeSurrounding("\"") }
            .filter { it.isNotBlank() }
    }
}

