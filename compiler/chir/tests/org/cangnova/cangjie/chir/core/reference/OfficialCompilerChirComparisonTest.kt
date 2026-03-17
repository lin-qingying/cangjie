package org.cangnova.cangjie.chir.core.reference

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.printer.ChirPrinter
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets

class OfficialCompilerChirComparisonTest {
    @Test
    fun `compare project chir printer against pre-generated official chir txt`() {
        val repoRoot = detectRepoRoot() ?: run {
            assumeTrue(false, "skip chir parity: repo root not found")
            return
        }
        val fixtureRoot = repoRoot.resolve("compiler/codegen/testData/chirParity/basic")
        val officialRoot = repoRoot.resolve("compiler/chir/testData/chirParity/official-generated-chir/basic")

        assumeTrue(fixtureRoot.exists(), "skip chir parity: missing fixture root $fixtureRoot")
        assumeTrue(officialRoot.exists(), "skip chir parity: missing official baseline root $officialRoot")

        var compared = 0
        fixtureRoot.listFiles { file -> file.isFile && file.name.endsWith(".chir.json") }
            ?.sortedBy { it.name }
            ?.forEach { fixture ->
                val sample = fixture.name.removeSuffix(".chir.json")
                val officialTxt = File(officialRoot, "$sample.txt")
                if (!officialTxt.exists()) return@forEach

                val pkg = buildSimpleReturnPackageFromFixture(fixture.readText(StandardCharsets.UTF_8))
                val projectText = normalizeText(ChirPrinter.print(pkg))
                val officialText = normalizeText(officialTxt.readText(StandardCharsets.UTF_8))
                val projectLiteral = extractProjectReturnLiteral(projectText)
                val officialLiteral = extractOfficialReturnLiteral(officialText)
                assertEquals(officialLiteral, projectLiteral, "chir return literal mismatch for sample $sample")
                compared++
            }

        assumeTrue(compared > 0, "skip chir parity: no matched official baseline files under $officialRoot")
    }

    private fun buildSimpleReturnPackageFromFixture(raw: String): ChirPackage {
        val pkgName = requiredStringField(raw, "package")
        val moduleName = requiredStringField(raw, "module")
        val functionName = requiredStringField(raw, "function")
        val operand = requiredStringField(raw, "operand")

        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val fn = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:$functionName"),
            name = functionName,
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:return"),
                            type = intType,
                            literal = operand,
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        return ChirPackage(
            semanticId = ChirSemanticId("pkg:$pkgName"),
            name = pkgName,
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:$moduleName"),
                    name = moduleName,
                    declarations = listOf(fn),
                ),
            ),
        )
    }

    private fun requiredStringField(text: String, fieldName: String): String {
        val regex = """"$fieldName"\s*:\s*"([^"]+)"""".toRegex()
        return regex.find(text)?.groupValues?.get(1)
            ?: error("missing required field '$fieldName'")
    }

    private fun normalizeText(raw: String): String {
        return raw.replace("\r\n", "\n")
            .lines()
            .map { it.trimEnd() }
            .joinToString("\n")
            .trim()
    }

    private fun extractProjectReturnLiteral(projectText: String): String {
        val literalRegex = """literal=(-?\d+)""".toRegex()
        return literalRegex.find(projectText)?.groupValues?.get(1)
            ?: error("project CHIR text does not contain literal=<number>")
    }

    private fun extractOfficialReturnLiteral(officialText: String): String {
        val lines = officialText.lines()
        val returnLine = lines.firstOrNull { it.contains("return", ignoreCase = true) }
        if (returnLine != null) {
            val number = """(-?\d+)""".toRegex().find(returnLine)?.groupValues?.get(1)
            if (number != null) return number
        }
        val literalLine = lines.firstOrNull {
            it.contains("literal", ignoreCase = true) || it.contains("operand", ignoreCase = true)
        }
        if (literalLine != null) {
            val number = """(-?\d+)""".toRegex().find(literalLine)?.groupValues?.get(1)
            if (number != null) return number
        }
        error("official CHIR text does not contain parseable return literal")
    }

    private fun detectRepoRoot(): File? {
        var current = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            if (File(current, "settings.gradle.kts").exists()) return current
            val parent = current.parentFile ?: return null
            current = parent
        }
    }
}
