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

/**
 * 对比项目 CHIR 打印结果与预生成官方 CHIR 基线中的关键语义。
 *
 * 该测试以简单返回样本为桥接格式，确认项目打印器保留官方基线中的返回字面量语义。
 */
class OfficialCompilerChirComparisonTest {
    /**
     * 校验项目 CHIR 打印文本与官方预生成文本的返回字面量一致。
     *
     * 测试在缺少夹具或官方基线时跳过，存在匹配样本时逐个读取并比较返回值语义。
     */
    @Test
    fun `compare project chir printer against pre-generated official chir txt`() {
        val repoRoot = detectRepoRoot() ?: run {
            assumeTrue(false, "skip chir parity: repo root not found")
            return
        }
        val fixtureRoot = repoRoot.resolve("compiler/codegen/testData/chirParity/basic")
        val officialRoot = repoRoot.resolve("chir/chir-tree/testData/chirParity/official-generated-chir/basic")

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

    /**
     * 根据 `.chir.json` 夹具构造简单返回函数的 CHIR 包。
     *
     * 该方法只提取包、模块、函数和返回操作数字段，用最小 CHIR 图承载官方基线需要比较的语义。
     */
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

    /**
     * 从文本夹具中读取必需的字符串字段。
     *
     * 字段缺失时立即失败，避免后续比较在不完整样本上产生误导性结果。
     */
    private fun requiredStringField(text: String, fieldName: String): String {
        val regex = """"$fieldName"\s*:\s*"([^"]+)"""".toRegex()
        return regex.find(text)?.groupValues?.get(1)
            ?: error("missing required field '$fieldName'")
    }

    /**
     * 规范化 CHIR 文本的换行、行尾空白和首尾空白。
     *
     * 该方法让官方基线与项目输出的比较不受平台换行或文件末尾格式差异影响。
     */
    private fun normalizeText(raw: String): String {
        return raw.replace("\r\n", "\n")
            .lines()
            .map { it.trimEnd() }
            .joinToString("\n")
            .trim()
    }

    /**
     * 从项目 CHIR 打印文本中提取返回字面量。
     *
     * 项目打印器使用 `literal=<number>` 格式，该方法固定该格式作为比较入口。
     */
    private fun extractProjectReturnLiteral(projectText: String): String {
        val literalRegex = """literal=(-?\d+)""".toRegex()
        return literalRegex.find(projectText)?.groupValues?.get(1)
            ?: error("project CHIR text does not contain literal=<number>")
    }

    /**
     * 从官方 CHIR 文本中提取可比较的返回字面量。
     *
     * 官方文本可能使用返回行或字面量/操作数行表达值，该方法按优先级解析这些稳定锚点。
     */
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

    /**
     * 从当前工作目录向上查找仓库根目录。
     *
     * 查找以 `settings.gradle.kts` 为根标记，保证测试能从 Gradle 子目录或 IDE 工作目录启动。
     */
    private fun detectRepoRoot(): File? {
        var current = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            if (File(current, "settings.gradle.kts").exists()) return current
            val parent = current.parentFile ?: return null
            current = parent
        }
    }
}
