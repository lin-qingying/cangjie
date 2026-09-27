package org.cangnova.cangjie.frontend.pipeline

import PackageFormat.Package
import PackageFormat.DeclKind
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpChirOutput
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpModuleDebug
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpModuleOptLevel
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpPackageFeatures
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpCommonPartChirPaths
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpCommonPartCjoPaths
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFormatVersion
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFunctionBodyInfo
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFunctionInfo
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFunctionTypeInfoMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageDeclaration
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSchemaProfile
import org.cangnova.cangjie.cfir.serialization.cjo.CjoTypeMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjmpCommonPartLoadGate
import org.cangnova.cangjie.cfir.session.cjmpMappingStorageOrNull
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.frontend.arguments.Freezable
import org.cangnova.cangjie.phaser.CompilerPhase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
 *   cjo 携带声明源码位置（G20），以编译消息外显并指向 common 源文件行列（对齐官方锚 common 源）。
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
        val completed: Boolean,
        val locations: List<CompilerMessageSourceLocation?> = emptyList(),
        val cjmpNominalMappings: Map<String, Pair<String, Map<String, String>>> = emptyMap(),
    )

    private class CjmpArgumentProbe : CommonCompilerArguments() {
        override fun copyOf(): Freezable = CjmpArgumentProbe()
    }

    private class CjmpArgumentProbePipeline : AbstractFrontendPipeline<CjmpArgumentProbe>() {
        override fun createCompoundPhase(
            arguments: CjmpArgumentProbe,
        ): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<CjmpArgumentProbe>, *> =
            error("a mismatched common cjo/chir argument pair must stop before phase construction")
    }

    private fun cjmpAttributeWords(vararg attributes: Attribute): List<ULong> {
        return List(attributes.maxOf { it.ordinal } / 64 + 1) { wordIndex ->
            attributes
                .filter { it.ordinal / 64 == wordIndex }
                .fold(0uL) { word, attribute -> word or (1uL shl (attribute.ordinal % 64)) }
        }
    }

    private fun hasCjoAttribute(declaration: PackageFormat.Decl, attribute: Attribute): Boolean {
        val word = (0 until declaration.attributesLength)
            .firstOrNull { it == attribute.ordinal / 64 }
            ?.let(declaration::attributes)
            ?: return false
        return word and (1uL shl (attribute.ordinal % 64)) != 0uL
    }

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
            val artifact = CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))
            val cjmpNominalMappings = buildMap {
                for (output in artifact?.frontendOutput?.outputs.orEmpty()) {
                    val mappingStorage = output.session.cjmpMappingStorageOrNull ?: continue
                    for (file in output.fir) {
                        for (specific in file.declarations.filterIsInstance<CfirClassLikeDeclaration>()) {
                            val common = mappingStorage.commonFor(specific) as? CfirClassLikeDeclaration ?: continue
                            put(
                                specific.name.asString(),
                                common.name.asString() to mappingStorage.typeParameterMappingFor(specific)
                                    .entries.associate { (commonParameter, specificParameter) ->
                                        commonParameter.name.asString() to specificParameter.name.asString()
                                    },
                            )
                        }
                    }
                }
            }
            return CompileResult(
                diagnostics = diagnostics.diagnostics,
                messages = collector.messages.toList(),
                completed = artifact != null,
                locations = collector.locations.toList(),
                cjmpNominalMappings = cjmpNominalMappings,
            )
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private fun writeCommon(
        content: String,
        packageName: String = "cjmp_p",
        features: Set<String> = emptySet(),
    ): Path {
        val dir = Files.createDirectories(tempDir.resolve("common"))
        val file = dir.resolve("common.cj")
        val featuresDirective = features.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "features { ", postfix = " }\n\n")
            .orEmpty()
        Files.writeString(file, "${featuresDirective}package $packageName\n\n$content")
        return file
    }

    private fun writeSpecific(content: String, features: Set<String> = emptySet()): Path {
        val dir = Files.createDirectories(tempDir.resolve("specific"))
        val file = dir.resolve("specific.cj")
        val featuresDirective = features.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "features { ", postfix = " }\n\n")
            .orEmpty()
        Files.writeString(file, "${featuresDirective}package cjmp_p\n\n$content")
        return file
    }

    /** 编译普通库并生成可由后续 session 从 classpath 反序列化的 CJO。 */
    private fun compileNormalCjoLibrary(content: String, packageName: String): Path {
        val sourceRoot = Files.createDirectories(tempDir.resolve("library-source-$packageName"))
        val source = sourceRoot.resolve("library.cj")
        Files.writeString(source, "package $packageName\n\n$content")
        val outputRoot = Files.createDirectories(tempDir.resolve("library-cjo-$packageName"))
        val result = compile(source) {
            cjoOutputDirectory = outputRoot.toString()
        }
        assertTrue(result.completed, "ordinary library frontend completes: ${result.messages}")
        assertTrue(result.diagnostics.none { it.severity.isError }, "ordinary library compiles: ${result.diagnostics}")
        val cjo = outputRoot.resolve(CjoConstants.packageNameToPath(packageName))
        assertTrue(Files.isRegularFile(cjo), "ordinary library writes the package cjo: $cjo")
        return outputRoot
    }

    /** 从一个普通 CJO classpath 编译独立 package 的消费端源码。 */
    private fun compileClasspathSource(
        content: String,
        packageName: String,
        classpathRoot: Path,
    ): CompileResult {
        val sourceRoot = Files.createDirectories(tempDir.resolve("consumer-source-$packageName"))
        val source = sourceRoot.resolve("consumer.cj")
        Files.writeString(source, "package $packageName\n\n$content")
        return compile(source) {
            addClasspathRoot(classpathRoot.toString())
        }
    }

    /**
     * common part 编译并返回写出的 cjo 路径。
     *
     * 走 CHIR 输出模式推导（官方 `IsCompilingCJMP()` 的 Common 判据；CLI `-Xcjmp-compile-common` 落到同一配置），
     * 而非显式模式覆盖。
     */
    private fun compileCommon(
        content: String,
        packageName: String = "cjmp_p",
        packageFeatures: Set<String> = emptySet(),
        moduleDebug: Boolean = false,
        moduleOptLevel: String = "O0",
    ): Path {
        val outDir = Files.createDirectories(tempDir.resolve("out"))
        val result = compile(writeCommon(content, packageName, packageFeatures)) {
            cjmpChirOutput = true
            cjoOutputDirectory = outDir.toString()
            cjmpPackageFeatures = packageFeatures
            cjmpModuleDebug = moduleDebug
            cjmpModuleOptLevel = moduleOptLevel
        }
        assertTrue(result.diagnostics.none { it.severity.isError }, "common part must compile: ${result.diagnostics}")
        assertTrue(result.completed, "common part pipeline completes: ${result.messages}")
        val cjo = outDir.resolve(CjoConstants.packageNameToPath(packageName))
        assertTrue(Files.isRegularFile(cjo), "common part cjo is written: $cjo")
        return cjo
    }

    private fun compileSpecific(
        content: String,
        commonCjo: Path,
        packageFeatures: Set<String> = emptySet(),
        moduleDebug: Boolean = false,
        moduleOptLevel: String = "O0",
    ): CompileResult =
        compile(writeSpecific(content, packageFeatures)) {
            cjmpCommonPartCjoPaths = listOf(commonCjo.toString())
            // specific 编译判据（官方 IsCompilingCJMPSpecific）：common-part chir 输入非空
            val chirName = commonCjo.fileName.toString().removeSuffix(".cjo") + ".chir"
            cjmpCommonPartChirPaths = listOf(commonCjo.resolveSibling(chirName).toString())
            cjmpPackageFeatures = packageFeatures
            cjmpModuleDebug = moduleDebug
            cjmpModuleOptLevel = moduleOptLevel
        }

    private val commonSource = """
        public common func platform(): Int64

        public common func withDefault(): Int64 {
            1
        }
    """.trimIndent()

    private val commonMetadataSource = commonSource + """

        public common class Defaulted {
            public common init() {}
            public common func value(): Int64 { 1 }
        }

        public common extend Defaulted {
            public common func extensionValue(): Int64 { 1 }
        }
    """.trimIndent()

    @Test
    fun `common part cjo carries cjmp attributes and options`() {
        val cjo = compileCommon(commonMetadataSource)
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(cjo)))
        val header = CjoPackageHeader.fromPackage(pkg)
        assertTrue(header.isCjmpCommonPart, "common part cjo is recognised as CJMP content")
        assertNotNull(header.options, "common part cjo embeds compile options (ASTWriter SaveOptions)")

        val declarations = (0 until pkg.allDeclsLength).map { pkg.allDecls(it)!! }
        fun has(decl: PackageFormat.Decl, attribute: Attribute): Boolean {
            val words = (0 until decl.attributesLength).map { decl.attributes(it) }
            val word = words.getOrNull(attribute.ordinal / 64) ?: return false
            return (word shr (attribute.ordinal % 64)) and 1uL == 1uL
        }
        val platform = declarations.single { it.kind == DeclKind.FuncDecl && it.identifier == "platform" }
        val withDefault = declarations.single { it.kind == DeclKind.FuncDecl && it.identifier == "withDefault" }
        val defaultedConstructor =
            declarations.single { it.kind == DeclKind.FuncDecl && it.identifier == SpecialNames.INIT.asString() }
        val defaultedClass = declarations.single { it.kind == DeclKind.ClassDecl && it.identifier == "Defaulted" }
        val defaultedExtend = declarations.single { it.kind == DeclKind.ExtendDecl }

        assertTrue(has(platform, Attribute.COMMON))
        assertTrue(has(platform, Attribute.FROM_COMMON_PART))
        assertTrue(!has(platform, Attribute.COMMON_WITH_DEFAULT), "bodyless common function has no default")
        assertTrue(has(withDefault, Attribute.COMMON_WITH_DEFAULT))
        assertTrue(has(defaultedConstructor, Attribute.COMMON_WITH_DEFAULT), "constructor body contributes the default bit")
        assertTrue(has(defaultedClass, Attribute.COMMON_WITH_DEFAULT), "nominal default is derived from its common members")
        assertTrue(has(defaultedExtend, Attribute.COMMON_WITH_DEFAULT), "extend default is derived from its common members")
        assertTrue(has(defaultedConstructor, Attribute.CONSTRUCTOR), "CJO preserves the constructor declaration kind")
        assertTrue(has(defaultedConstructor, Attribute.PRIMARY_CONSTRUCTOR), "CJO preserves primary constructor identity")
        assertTrue(has(defaultedConstructor, Attribute.FROM_COMMON_PART))
        assertTrue(has(defaultedClass, Attribute.FROM_COMMON_PART))
        assertTrue(has(defaultedExtend, Attribute.FROM_COMMON_PART))
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
        assertTrue(result.completed, "specific pipeline completes")
    }

    @Test
    fun `generic enum constructors match through a common part cjo`() {
        val cjo = compileCommon(
            """
            public common enum Box<T> {
                | Value(T)
            }
            """.trimIndent(),
        )
        val commonPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(cjo)))
        val commonDeclarations = (0 until commonPackage.allDeclsLength).map { commonPackage.allDecls(it)!! }
        val commonEnum = commonDeclarations.single { it.kind == DeclKind.EnumDecl && it.identifier == "Box" }
        val commonValue = commonDeclarations.single { it.identifier == "Value" }
        assertTrue(hasCjoAttribute(commonEnum, Attribute.GENERIC), "CJO marks a generic enum owner")
        assertTrue(hasCjoAttribute(commonValue, Attribute.ENUM_CONSTRUCTOR), "CJO preserves enum constructor identity")
        val result = compileSpecific(
            """
            public specific enum Box<U> {
                | Value(U)
            }
            """.trimIndent(),
            cjo,
        )

        assertTrue(
            result.completed,
            "specific enum compilation consumes common constructor identities from CJO: " +
                "messages=${result.messages}, diagnostics=${result.diagnostics}",
        )
        assertEquals(emptyList<String>(), result.diagnostics.map { it.factoryName.removePrefix("CFIR_") })
        assertEquals(
            "Box" to mapOf("T" to "U"),
            result.cjmpNominalMappings["Box"],
            "the enum mapping carries its generic parameter identity through the CJO constructor table",
        )
    }

    @Test
    fun `unmarked specific nominal member is merged once in an extend owner group`() {
        val cjo = compileCommon(
            """
            public common class Box<T> {
                public common init()
                public common func f(value: T): Unit { }
            }
            """.trimIndent(),
        )
        val result = compileSpecific(
            """
            public specific class Box<U> {
                public specific init() { }
                public func f(value: U): Unit { }
            }

            public extend<U> Box<U> {
                public func g(value: U): Unit { }
            }

            public func instantiate(): Box<Int64> {
                Box<Int64>()
            }
            """.trimIndent(),
            cjo,
        )

        assertTrue(
            result.completed,
            "specific compilation consumes the serialized common cjo: messages=${result.messages}, diagnostics=${result.diagnostics}",
        )
        assertEquals(
            "Box" to mapOf("T" to "U"),
            result.cjmpNominalMappings["Box"],
            "the specific nominal declaration is bound to the deserialized common nominal and type parameter",
        )
        assertEquals(
            listOf("CONFLICTING_OVERLOADS"),
            result.diagnostics.map { it.factoryName.removePrefix("CFIR_") },
            "the unmarked specific declaration is diagnosed once; its nominal common counterpart must not reappear in the extend owner group",
        )
    }

    @Test
    fun `extend check sequence diagnostic retains peer interface evidence and source range`() {
        val result = compile(
            writeSpecific(
                """
                interface I1 { }
                interface I2 <: I1 { }
                interface I3 { }
                interface I4 <: I3 { }
                class Box { }

                extend Box <: I1 & I4 { }
                extend Box <: I3 & I2 { }
                """.trimIndent(),
            ),
        ) { }

        val sequenceDiagnostics = result.diagnostics.filter {
            it.factoryName.removePrefix("CFIR_") == "EXTEND_CHECK_SEQUENCE_CANNOT_DECIDE"
        }
        assertEquals(2, sequenceDiagnostics.size, "each undecidable source owner keeps the primary diagnostic")
        assertTrue(sequenceDiagnostics.all { it.relatedInformation.size == 1 })
        assertTrue(
            sequenceDiagnostics.all { diagnostic ->
                val related = diagnostic.relatedInformation.single()
                related.element != null && related.element != (diagnostic as? org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithSource)?.element &&
                        related.message.contains("I1") && related.message.contains("I3")
            },
            "each primary diagnostic carries the peer extend range and both direct-interface witnesses",
        )
        assertTrue(sequenceDiagnostics.all { "note:" in it.renderMessage() })
    }

    @Test
    fun `generic instantiation notes the inherited default instead of its private peer`() {
        val source = writeSpecific(
            """
            interface I {
                func f(value: Int64): Unit { }
            }

            class Box<T> {
                init() { }
            }

            extend<T> Box<T> <: I {
                public func f(value: T): Unit { }
            }

            extend<U> Box<U> {
                private func f(value: Int64): Unit { }
            }

            func trigger(box: Box<Int64>): Unit { }
            """.trimIndent(),
        )
        val sourceText = Files.readString(source)
        val peerFunctionNameOffset = sourceText.indexOf("private func f") + "private func ".length
        val result = compile(source) { }
        val ambiguity = result.diagnostics.single {
            it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
        }

        assertEquals(2, ambiguity.relatedInformation.size, "the ambiguity retains exactly the two effective candidates")
        assertTrue(
            ambiguity.relatedInformation.all { it.message == "found candidate" },
            "candidate notes have source ranges and cjc-compatible note text",
        )
        assertTrue(
            ambiguity.relatedInformation.none { it.element?.startOffset == peerFunctionNameOffset },
            "the private peer cannot replace the interface default as the stable candidate",
        )
        assertTrue(ambiguity.renderMessage().contains("note: found candidate"))
    }

    @Test
    fun `deserialized generic extend peers honor target substitutions and bounds`() {
        val ownerLibrary = """
            public interface Parent<V> {
                public func f(value: V): Unit { }
            }

            public interface Child<W> <: Parent<W> { }
            public interface Bound { }

            public class OwnerBox<T> {
                public init() { }
            }

            public extend<T> OwnerBox<T> <: Child<T> {
                public func f(value: T): Unit { }
            }

            public extend<U> OwnerBox<U> where U <: Bound {
                private func f(value: Int64): Unit { }
            }

            public class ChainOnlyBox<T> {
                public init() { }
            }

            public extend<T> ChainOnlyBox<T> <: Child<T> {
                public func f(value: T): Unit { }
            }
        """.trimIndent()
        val consumerSource = """
            import ownermatrixlib.*

            func trigger(owner: OwnerBox<Int64>, chain: ChainOnlyBox<Int64>): Unit { }
        """.trimIndent()

        val unsatisfiedLibrary = compileNormalCjoLibrary(ownerLibrary, "ownermatrixlib")
        val unsatisfiedConsumer = compileClasspathSource(consumerSource, "ownermatrixclient", unsatisfiedLibrary)
        assertTrue(
            unsatisfiedConsumer.completed,
            "consumer pipeline completes with an unsatisfied generic extend bound: " +
                "messages=${unsatisfiedConsumer.messages}, diagnostics=${unsatisfiedConsumer.diagnostics}",
        )
        assertTrue(
            unsatisfiedConsumer.diagnostics.none {
                it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
            },
            "the private peer whose where-bound is unsatisfied is absent after CJO deserialization",
        )

        val satisfiedLibrary = compileNormalCjoLibrary(
            ownerLibrary + "\n\npublic extend Int64 <: Bound { }\n",
            "ownermatrixlib",
        )
        val ownerPackagePath = satisfiedLibrary.resolve(CjoConstants.packageNameToPath("ownermatrixlib"))
        val ownerPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(ownerPackagePath)))
        val genericExtends = (0 until ownerPackage.allDeclsLength)
            .map { ownerPackage.allDecls(it)!! }
            .filter { it.kind == DeclKind.ExtendDecl && (it.generic?.typeParametersLength ?: 0) > 0 }
        assertEquals(3, genericExtends.size, "the ordinary CJO retains all generic owner declarations")
        assertTrue(genericExtends.all { hasCjoAttribute(it, Attribute.GENERIC) }, "CJO marks generic extend owners")
        val satisfiedConsumer = compileClasspathSource(consumerSource, "ownermatrixclient", satisfiedLibrary)
        assertTrue(
            satisfiedConsumer.completed,
            "consumer pipeline completes with a satisfied generic extend bound: " +
                "messages=${satisfiedConsumer.messages}, diagnostics=${satisfiedConsumer.diagnostics}",
        )
        assertEquals(
            listOf("GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"),
            satisfiedConsumer.diagnostics
                .filter { it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS" }
                .map { it.factoryName.removePrefix("CFIR_") },
            "the applicable private peer conflicts once; matching Child<W> -> Parent<V> parameters do not add a duplicate",
        )
        val ambiguity = satisfiedConsumer.diagnostics.single {
            it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
        }
        assertEquals(2, ambiguity.relatedInformation.size, "both effective overload candidates retain their CJO positions")
        assertTrue(
            ambiguity.relatedInformation.all { candidate ->
                val location = candidate.sourceLocation ?: return@all false
                location.filePath.endsWith("library.cj") && location.line > 0 && location.column > 0
            },
            "the peer notes point back to the source file and range serialized in the ordinary CJO",
        )
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

    @Test
    fun `common part with a different package is rejected through the pipeline`() {
        val sourceCjo = compileCommon(commonSource, packageName = "other_pkg")
        val inputDirectory = Files.createDirectories(tempDir.resolve("wrong-package-input"))
        val cjo = inputDirectory.resolve("cjmp_p.cjo")
        Files.copy(sourceCjo, cjo)
        val result = compileSpecific(
            """
            public specific func platform(): Int64 { 0 }
            """.trimIndent(),
            cjo,
        )

        assertFalse(result.completed, "a wrong-package common part aborts the frontend pipeline: ${result.messages}")
        assertTrue(
            result.messages.any { it.first.isError && it.second.contains("package", ignoreCase = true) },
            "the G17 message collector receives the wrong-package diagnostic: ${result.messages}",
        )
    }

    @Test
    fun `unsupported common cjo version is rejected through the pipeline`() {
        val supported = CjmpCommonPartLoadGate.supportedVersion
        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(
                fullPackageName = "cjmp_p",
                moduleName = "cjmp_p",
                schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP,
                cjoVersion = CjoFormatVersion(
                    major = supported.major,
                    minor = (supported.minor.toInt() + 1).toUByte(),
                    patch = supported.patch,
                ),
            ),
        )
        val inputDirectory = Files.createDirectories(tempDir.resolve("unsupported-version"))
        val cjo = Files.write(inputDirectory.resolve("cjmp_p.cjo"), bytes)
        val result = compileSpecific(
            """
            public specific func platform(): Int64 { 0 }
            """.trimIndent(),
            cjo,
        )

        assertFalse(result.completed, "an unsupported common cjo version aborts the frontend pipeline: ${result.messages}")
        assertTrue(
            result.messages.any { it.first.isError && it.second.contains("format version", ignoreCase = true) },
            "the G17 message collector receives the cjo-version diagnostic: ${result.messages}",
        )
    }

    @Test
    fun `common part feature mismatch is rejected through the pipeline`() {
        val cjo = compileCommon(commonSource, packageFeatures = setOf("sample.common"))
        val result = compileSpecific(
            """
            public specific func platform(): Int64 { 0 }
            """.trimIndent(),
            cjo,
            packageFeatures = setOf("sample.specific"),
        )

        assertFalse(result.completed, "a feature-set mismatch aborts the frontend pipeline: ${result.messages}")
        assertTrue(
            result.messages.any { it.first.isError && it.second.contains("feature", ignoreCase = true) },
            "the G17 message collector receives the feature-subset diagnostic: ${result.messages}",
        )
    }

    @Test
    fun `common part compile-option mismatch is rejected through the pipeline`() {
        val cjo = compileCommon(commonSource, moduleDebug = true)
        val result = compileSpecific(
            """
            public specific func platform(): Int64 { 0 }
            """.trimIndent(),
            cjo,
            moduleDebug = false,
        )

        assertFalse(result.completed, "a debug-option mismatch aborts the frontend pipeline: ${result.messages}")
        assertTrue(
            result.messages.any { it.first.isError && it.second.contains("debug", ignoreCase = true) },
            "the G17 message collector receives the options mismatch diagnostic: ${result.messages}",
        )
    }

    @Test
    fun `common part without serialized options warns but continues through the pipeline`() {
        val supported = CjmpCommonPartLoadGate.supportedVersion
        val bytes = CjoPackageWriter.toByteArray(
            CjoPackageMetadata(
                fullPackageName = "cjmp_p",
                moduleName = "cjmp_p",
                schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP,
                cjoVersion = CjoFormatVersion(supported.major, supported.minor, supported.patch),
                options = null,
                declarations = listOf(
                    CjoPackageDeclaration(
                        identifier = "platform",
                        kind = DeclKind.FuncDecl,
                        type = 1u,
                        attributes = cjmpAttributeWords(Attribute.COMMON, Attribute.FROM_COMMON_PART),
                        info = CjoFunctionInfo(body = CjoFunctionBodyInfo(returnType = 2u)),
                    ),
                ),
                types = listOf(
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Func,
                        typeArguments = listOf(2u),
                        semanticInfo = CjoFunctionTypeInfoMetadata(
                            returnType = 2u,
                            isC = false,
                            hasVariableLenArg = false,
                        ),
                    ),
                    CjoTypeMetadata(kind = PackageFormat.TypeKind.Int64),
                ),
            ),
        )
        val inputDirectory = Files.createDirectories(tempDir.resolve("missing-options"))
        val cjo = Files.write(inputDirectory.resolve("cjmp_p.cjo"), bytes)
        val result = compileSpecific(
            content = "public specific func platform(): Int64 { 0 }",
            commonCjo = cjo,
        )

        assertTrue(result.completed, "missing options is a warning and does not reject the common part")
        assertEquals(emptyList<String>(), result.diagnostics.map { it.factoryName }, "matching common declaration loads")
        assertTrue(
            result.messages.any {
                it.first == CompilerMessageSeverity.WARNING && it.second.contains("missing serialized options")
            },
            "the G17 warning reaches the message collector: ${result.messages}",
        )
        assertFalse(result.messages.any { it.first.isError }, "warning-only loading completes without errors")
    }

    @Test
    fun `common part command line arguments require paired cjo and chir paths`() {
        val collector = RecordingMessageCollector()
        val configuration = CompilerConfiguration().apply {
            messageCollector = collector
        }
        val arguments = CjmpArgumentProbe().apply {
            languageVersion = "1.1.0"
            cjmpCommonPart = arrayOf("common.cjo")
            cjmpCommonPartChir = emptyArray()
        }

        val completed = CjmpArgumentProbePipeline().execute(arguments, configuration)

        assertFalse(completed)
        assertTrue(
            collector.messages.any {
                it.first.isError && it.second ==
                        "common .chir files count should be equal to common .cjo files count"
            },
            "the compiler argument pipeline reports the paired-path validation error: ${collector.messages}",
        )
    }
}
