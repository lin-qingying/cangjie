package org.cangnova.cangjie.frontend.pipeline

import PackageFormat.Package
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpCommonPartChirPaths
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpCommonPartCjoPaths
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.metadata.model.Attribute
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.ByteBuffer
import java.nio.file.Files
import java.nio.file.Path

/**
 * CJMP 两段式编译端到端验收（计划 Phase 4.4）：common part 编译写出 cjo → specific part 编译加载该 cjo
 * 并配对。对位 cjc 1.1.3 实测流程：
 * `cjc common.cj --output-type=chir`（产出 `<pkg>.cjo`）→ `cjc specific.cj --common-part-cjo=<pkg>.cjo`。
 *
 * 断言面：
 * - 写侧：common part cjo 携带 CJMP 属性位、`Package.options`（CJMP 档位）；
 * - 读侧：specific 编译经 depends-on 模块加载 common 声明，完全配对时零诊断；
 * - 配对失败时 specific 方向 NOT_MATCHED 照常报告；common 方向诊断锚在反序列化声明上——
 *   本仓库 cjo 不携带声明源码位置（G20 核实结论），该方向不报（与官方锚 common 源的差异已登记）。
 */
@OptIn(CompilerConfiguration.Internals::class)
class CjmpTwoPhaseCompilationTest {
    @TempDir
    lateinit var tempDir: Path

    private class RecordingMessageCollector : MessageCollector {
        val messages = mutableListOf<Pair<CompilerMessageSeverity, String>>()
        val locations = mutableListOf<CompilerMessageSourceLocation?>()
        override fun clear() = messages.clear()
        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?,
        ) {
            messages += severity to message
            locations += location
        }

        override fun hasErrors(): Boolean = messages.any { it.first.isError }
    }

    private data class CompileResult(
        val diagnostics: List<CjDiagnostic>,
        val messages: List<Pair<CompilerMessageSeverity, String>>,
        val locations: List<CompilerMessageSourceLocation?> = emptyList(),
    )

    private fun compile(source: Path, configure: CompilerConfiguration.() -> Unit): CompileResult {
        val disposable = Disposer.newDisposable("cjmp-two-phase")
        try {
            val collector = RecordingMessageCollector()
            val diagnostics = DiagnosticsCollectorImpl()
            val configuration = CompilerConfiguration().apply {
                messageCollector = collector
                diagnosticsCollector = diagnostics
                moduleName = "cjmp_p"
                addCangJieSourceRoot(source.toString())
                configure()
            }
            CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))
            return CompileResult(diagnostics.diagnostics, collector.messages.toList(), collector.locations.toList())
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private fun writeCommon(content: String): Path {
        val dir = Files.createDirectories(tempDir.resolve("common"))
        val file = dir.resolve("common.cj")
        Files.writeString(file, "package cjmp_p\n\n$content")
        return file
    }

    private fun writeSpecific(content: String): Path {
        val dir = Files.createDirectories(tempDir.resolve("specific"))
        val file = dir.resolve("specific.cj")
        Files.writeString(file, "package cjmp_p\n\n$content")
        return file
    }

    /** common part 编译（CHIR 输出模式对位：显式 COMMON）并返回写出的 cjo 路径。 */
    private fun compileCommon(content: String): Path {
        val outDir = Files.createDirectories(tempDir.resolve("out"))
        val result = compile(writeCommon(content)) {
            cjmpMode = CfirCjmpMode.COMMON
            cjoOutputDirectory = outDir.toString()
        }
        assertTrue(result.diagnostics.none { it.severity.isError }, "common part must compile: ${result.diagnostics}")
        val cjo = outDir.resolve(CjoConstants.packageNameToPath("cjmp_p"))
        assertTrue(Files.isRegularFile(cjo), "common part cjo is written: $cjo")
        return cjo
    }

    private fun compileSpecific(content: String, commonCjo: Path): CompileResult =
        compile(writeSpecific(content)) {
            cjmpCommonPartCjoPaths = listOf(commonCjo.toString())
            // specific 编译判据（官方 IsCompilingCJMPSpecific）：common-part chir 输入非空
            cjmpCommonPartChirPaths = listOf(commonCjo.resolveSibling("cjmp_p.chir").toString())
        }

    private val commonSource = """
        public common func platform(): Int64

        public common func withDefault(): Int64 {
            1
        }
    """.trimIndent()

    @Test
    fun `common part cjo carries cjmp attributes and options`() {
        val cjo = compileCommon(commonSource)
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(cjo)))
        val header = CjoPackageHeader.fromPackage(pkg)
        assertTrue(header.isCjmpCommonPart, "common part cjo is recognised as CJMP content")
        assertNotNull(header.options, "common part cjo embeds compile options (ASTWriter SaveOptions)")

        val attributesByName = (0 until pkg.allDeclsLength).associate { index ->
            val decl = pkg.allDecls(index)!!
            decl.identifier to (0 until decl.attributesLength).map { decl.attributes(it) }
        }
        fun has(name: String, attribute: Attribute): Boolean {
            val words = attributesByName.getValue(name)
            val word = words.getOrNull(attribute.ordinal / 64) ?: return false
            return (word shr (attribute.ordinal % 64)) and 1uL == 1uL
        }
        assertTrue(has("platform", Attribute.COMMON))
        assertTrue(!has("platform", Attribute.COMMON_WITH_DEFAULT), "bodyless common function has no default")
        assertTrue(has("withDefault", Attribute.COMMON_WITH_DEFAULT))
    }

    @Test
    fun `specific part pairs with deserialized common part`() {
        val cjo = compileCommon(commonSource)
        val result = compileSpecific(
            """
            public specific func platform(): Int64 {
                0
            }
            """.trimIndent(),
            cjo,
        )
        assertEquals(emptyList<String>(), result.diagnostics.map { it.factoryName }, "fully matched specific part")
        assertTrue(result.messages.none { it.first.isError }, "no load gate errors: ${result.messages}")
    }

    @Test
    fun `specific declaration without common counterpart is not matched`() {
        val cjo = compileCommon(commonSource)
        val result = compileSpecific(
            """
            public specific func platform(): Int64 {
                0
            }

            public specific func orphan(): Int64 {
                2
            }
            """.trimIndent(),
            cjo,
        )
        assertEquals(listOf("NOT_MATCHED"), result.diagnostics.map { it.factoryName.removePrefix("CFIR_") })
    }

    @Test
    fun `unmatched deserialized common declaration is reported at its common source position`() {
        val cjo = compileCommon(
            """
            public common func a(): Int64
            public common func b(): Int64
            """.trimIndent(),
        )
        val result = compileSpecific(
            """
            public specific func a(): Int64 {
                1
            }
            """.trimIndent(),
            cjo,
        )
        // cjc 1.1.3 实测：`'common' function 'b' can not find 'specific' match ==> common.cj:4:1`
        val index = result.messages.indexOfFirst { it.second == "'common' function 'b' can not find 'specific' match" }
        assertTrue(index >= 0, "common direction NOT_MATCHED reported: ${result.messages}")
        assertEquals(CompilerMessageSeverity.ERROR, result.messages[index].first)
        val location = result.locations[index]
        assertNotNull(location, "diagnostic carries the common source location")
        location!!
        assertTrue(location.path.endsWith("common.cj"), "anchored in the common source file: ${location.path}")
        assertEquals(4, location.line)
        assertEquals(1, location.column)
    }
}
