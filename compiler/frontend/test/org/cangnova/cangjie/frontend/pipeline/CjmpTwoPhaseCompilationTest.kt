package org.cangnova.cangjie.frontend.pipeline

import PackageFormat.Package
import PackageFormat.DeclKind
import PackageFormat.CompositeTyInfo
import PackageFormat.ClassInfo
import PackageFormat.ExtendInfo
import PackageFormat.GenericTyInfo
import PackageFormat.EnumInfo
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithSource
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
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
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.descriptors.Visibilities
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
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.source.CjPsiSourceElement
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
        val deserializedExtendFacts: List<DeserializedExtendFact> = emptyList(),
    )

    private data class DeserializedExtendFact(
        val packageName: String,
        val targetType: String,
        val superTypes: List<String>,
        val typeParameterUpperBounds: List<List<DeserializedTypeFact>>,
        val privateFunctionNames: List<String>,
    )

    private data class DeserializedTypeFact(
        val renderedType: String,
        val classIdOrPrimitiveClassId: ClassId?,
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

    private fun assertCjoFunctionParameterUsesOwnerGeneric(
        pkg: Package,
        owner: PackageFormat.Decl,
        function: PackageFormat.Decl,
    ) {
        val functionInfo = function.info(PackageFormat.FuncInfo()) as PackageFormat.FuncInfo
        val functionBody = checkNotNull(functionInfo.funcBody)
        val parameterIndex = functionBody.paramLists(0)!!.params(0).toInt() - 1
        val parameter = checkNotNull(pkg.allDecls(parameterIndex))
        val parameterTypeIndex = parameter.type.toInt() - 1
        val parameterType = checkNotNull(pkg.allTypes(parameterTypeIndex))
        assertEquals(
            PackageFormat.TypeKind.Generic,
            parameterType.kind,
            "${function.identifier}.${parameter.identifier} type index ${parameter.type} must retain its generic parameter type",
        )
        assertEquals(PackageFormat.SemaTyInfo.GenericTyInfo, parameterType.infoType)

        val genericTypeInfo = parameterType.info(PackageFormat.GenericTyInfo()) as PackageFormat.GenericTyInfo
        val genericParameterReference = checkNotNull(genericTypeInfo.declPtr)
        val ownerGeneric = checkNotNull(owner.generic)
        assertEquals(ownerGeneric.typeParameters(0), genericParameterReference.index)
        assertEquals(
            DeclKind.GenericParamDecl,
            checkNotNull(pkg.allDecls(genericParameterReference.index.toInt() - 1)).kind,
        )
    }

    private fun compile(source: Path, configure: CompilerConfiguration.() -> Unit): CompileResult =
        compile(source, captureDeserializedExtendFacts = false, configure = configure)

    private fun compile(
        source: Path,
        captureDeserializedExtendFacts: Boolean,
        configure: CompilerConfiguration.() -> Unit,
    ): CompileResult {
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
            val deserializedExtendFacts = if (captureDeserializedExtendFacts) {
                artifact?.frontendOutput?.outputs.orEmpty().flatMap { output ->
                    val session = output.session
                    session.extendProvider.getAllExtends().mapNotNull { extend ->
                        val packageName = session.extendProvider.getPackageFqName(extend)?.asString()
                            ?: return@mapNotNull null
                        DeserializedExtendFact(
                            packageName = packageName,
                            targetType = extend.extendedTypeRef.coneTypeOrNull?.toString().orEmpty(),
                            superTypes = extend.superTypeRefs.map { it.coneTypeOrNull?.toString().orEmpty() },
                            typeParameterUpperBounds = extend.typeParameters.map { parameter ->
                                parameter.bounds.map { bound ->
                                    val coneType = bound.coneTypeOrNull
                                    DeserializedTypeFact(
                                        renderedType = coneType?.toString().orEmpty(),
                                        classIdOrPrimitiveClassId = coneType?.classIdOrPrimitiveClassId,
                                    )
                                }
                            },
                            privateFunctionNames = extend.declarations.filterIsInstance<CfirFunction>()
                                .filter { it.status.visibility == Visibilities.Private }
                                .map { it.symbol.name.asString() },
                        )
                    }
                }
            } else {
                emptyList()
            }
            return CompileResult(
                diagnostics = diagnostics.diagnostics,
                messages = collector.messages.toList(),
                completed = artifact != null,
                locations = collector.locations.toList(),
                cjmpNominalMappings = cjmpNominalMappings,
                deserializedExtendFacts = deserializedExtendFacts,
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
        val diagnosticLocations = result.diagnostics.joinToString { diagnostic ->
            val psi = ((diagnostic as? CjDiagnosticWithSource)?.element as? CjPsiSourceElement)?.psi
            val line = psi?.let { element ->
                element.containingFile.text.take(element.textRange.startOffset).count { it == '\n' } + 1
            }
            "${diagnostic.factoryName}@$line '${psi?.text ?: "<no PSI>"}'"
        }
        assertTrue(
            result.diagnostics.none { it.severity.isError },
            "ordinary library compiles: $diagnosticLocations",
        )
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
        return compile(source, captureDeserializedExtendFacts = true) {
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

        public common func identity<T>(value: T): T { value }

        public common class Defaulted {
            public common init() {}
            public common func value(): Int64 { 1 }
        }

        public common class GenericBox<T> {
            public common init() {}
        }

        public type GenericAlias<U> = GenericBox<U>

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
        val identity = declarations.single { it.kind == DeclKind.FuncDecl && it.identifier == "identity" }
        val defaultedClass = declarations.single { it.kind == DeclKind.ClassDecl && it.identifier == "Defaulted" }
        val genericBox = declarations.single { it.kind == DeclKind.ClassDecl && it.identifier == "GenericBox" }
        val genericAlias = declarations.single { it.kind == DeclKind.TypeAliasDecl && it.identifier == "GenericAlias" }
        val defaultedExtend = declarations.single { it.kind == DeclKind.ExtendDecl }
        val defaultedClassInfo = defaultedClass.info(ClassInfo()) as ClassInfo
        val defaultedConstructor = (0 until defaultedClassInfo.bodyLength)
            .map { defaultedClassInfo.body(it).toInt() - 1 }
            .map { pkg.allDecls(it)!! }
            .single { it.kind == DeclKind.FuncDecl && it.identifier == SpecialNames.INIT.asString() }

        assertTrue(has(platform, Attribute.COMMON))
        assertTrue(has(platform, Attribute.FROM_COMMON_PART))
        assertTrue(!has(platform, Attribute.COMMON_WITH_DEFAULT), "bodyless common function has no default")
        assertTrue(has(withDefault, Attribute.COMMON_WITH_DEFAULT))
        assertTrue(has(defaultedConstructor, Attribute.COMMON_WITH_DEFAULT), "constructor body contributes the default bit")
        assertTrue(has(defaultedClass, Attribute.COMMON_WITH_DEFAULT), "nominal default is derived from its common members")
        assertTrue(has(defaultedExtend, Attribute.COMMON_WITH_DEFAULT), "extend default is derived from its common members")
        assertTrue(has(defaultedConstructor, Attribute.CONSTRUCTOR), "CJO preserves the constructor declaration kind")
        assertTrue(has(identity, Attribute.GENERIC), "CJO marks generic function owners")
        assertTrue(has(genericBox, Attribute.GENERIC), "CJO marks generic class owners")
        assertTrue(has(genericAlias, Attribute.GENERIC), "CJO marks generic type alias owners")
        assertNotNull(identity.generic, "CJO preserves generic function parameters")
        assertNotNull(genericBox.generic, "CJO preserves generic class parameters")
        assertNotNull(genericAlias.generic, "CJO preserves generic type alias parameters")
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
                | Empty
                | Value(T)
            }
            """.trimIndent(),
        )
        val commonPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(cjo)))
        val commonDeclarations = (0 until commonPackage.allDeclsLength).map { commonPackage.allDecls(it)!! }
        val commonEnum = commonDeclarations.single { it.kind == DeclKind.EnumDecl && it.identifier == "Box" }
        val commonEmpty = commonDeclarations.single { it.identifier == "Empty" }
        val commonValue = commonDeclarations.single { it.identifier == "Value" }
        val commonEnumInfo = commonEnum.info(EnumInfo()) as EnumInfo
        assertEquals(
            listOf("Empty", "Value"),
            (0 until commonEnumInfo.bodyLength).map { index ->
                commonPackage.allDecls(commonEnumInfo.body(index).toInt() - 1)!!.identifier
            },
            "CJO enum body contains the variants and excludes the synthesized CFIR primary constructor",
        )
        assertTrue(hasCjoAttribute(commonEnum, Attribute.COMMON), "CJO preserves the enum owner's common status")
        assertTrue(hasCjoAttribute(commonEnum, Attribute.GENERIC), "CJO marks a generic enum owner")
        assertEquals(DeclKind.VarDecl, commonEmpty.kind, "CJO stores a payload-free enum constructor as VarDecl")
        assertTrue(hasCjoAttribute(commonEmpty, Attribute.ENUM_CONSTRUCTOR), "CJO preserves payload-free enum constructor identity")
        assertEquals(DeclKind.FuncDecl, commonValue.kind, "CJO stores a payload enum constructor as FuncDecl")
        assertTrue(hasCjoAttribute(commonValue, Attribute.ENUM_CONSTRUCTOR), "CJO preserves enum constructor identity")
        assertCjoFunctionParameterUsesOwnerGeneric(commonPackage, commonEnum, commonValue)
        val result = compileSpecific(
            """
            public specific enum Box<U> {
                | Empty
                | Value(U)
            }
            """.trimIndent(),
            cjo,
        )

        assertTrue(
            result.completed,
            "specific enum compilation consumes common constructor identities from CJO: " +
                "mappings=${result.cjmpNominalMappings}, messages=${result.messages}, diagnostics=${result.diagnostics}",
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
        val commonPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(cjo)))
        val commonBox = (0 until commonPackage.allDeclsLength)
            .map { commonPackage.allDecls(it)!! }
            .single { it.kind == DeclKind.ClassDecl && it.identifier == "Box" }
        assertTrue(hasCjoAttribute(commonBox, Attribute.GENERIC), "CJO marks a generic nominal owner")
        val commonFunction = (0 until commonPackage.allDeclsLength)
            .map { commonPackage.allDecls(it)!! }
            .single { it.kind == DeclKind.FuncDecl && it.identifier == "f" }
        assertCjoFunctionParameterUsesOwnerGeneric(commonPackage, commonBox, commonFunction)
        val result = compileSpecific(
            """
            public specific class Box<U> {
                public specific init() { }
                public func f(value: U): Unit { }
            }

            extend<U> Box<U> {
                public func g(value: U): Unit { }
            }

            public func instantiate(): Box<Int64> {
                Box<Int64>()
            }
            """.trimIndent(),
            cjo,
        )

        assertEquals(
            listOf("CONFLICTING_OVERLOADS"),
            result.diagnostics.map { it.factoryName.removePrefix("CFIR_") },
            "the unmarked specific declaration is diagnosed once; its nominal common counterpart must not reappear in the extend owner group",
        )
    }

    @Test
    fun `specific secondary constructor conflicts with unmarked common primary constructor`() {
        val cjo = compileCommon(
            """
            public common class P {
                public P(x: Int64) {}
            }
            """.trimIndent(),
        )

        val result = compileSpecific(
            """
            public specific class P {
                public specific init(x: Int64) {}
            }
            """.trimIndent(),
            cjo,
        )

        assertEquals(
            2,
            result.diagnostics.size,
            result.diagnostics.joinToString { diagnostic -> "${diagnostic.factoryName}: ${diagnostic.renderMessage()}" },
        )
        assertEquals(
            setOf("CONFLICTING_OVERLOADS", "NOT_MATCHED"),
            result.diagnostics.map { it.factoryName.removePrefix("CFIR_") }.toSet(),
            "the primary constructor participates in overload checking but remains ineligible as a CJMP counterpart",
        )
        val overload = result.diagnostics.single { it.factoryName.removePrefix("CFIR_") == "CONFLICTING_OVERLOADS" }
        val note = overload.relatedInformation.single()
        assertEquals("conflict with the declaration", note.message)
        assertNotNull(note.sourceLocation, "the CJO peer note keeps its source location after deserialization")
        assertTrue(note.sourceLocation!!.filePath.endsWith("common.cj"))
        assertEquals(4, note.sourceLocation!!.line)
        assertEquals(12, note.sourceLocation!!.column, "the note points to the primary constructor identifier")
        assertTrue(overload.renderMessage().contains("note: conflict with the declaration"))
    }

    @Test
    fun `body inferred return types are checked after common cjo matching`() {
        val cjo = compileCommon(
            """
            public common func inferredSame() { 1 }
            public common func inferredMismatch() { 1 }
            """.trimIndent(),
        )

        val result = compileSpecific(
            """
            public specific func inferredSame() { 1 }
            public specific func inferredMismatch() { "wrong" }
            """.trimIndent(),
            cjo,
        )

        assertTrue(result.completed, "specific compilation consumes the common CJO: ${result.messages}")
        assertEquals(
            listOf("RETURN_TYPE_INCOMPATIBLE"),
            result.diagnostics.map { it.factoryName.removePrefix("CFIR_") },
            "the equal inferred return binds; the unequal inferred return keeps its pair and reports the post-check",
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

            extend<T> OwnerBox<T> <: Child<T> {
                public func f(value: T): Unit { }
            }

            extend<U> OwnerBox<U> where U <: Bound {
                private func f(value: Int64): Unit { }
            }

            public class ChainOnlyBox<T> {
                public init() { }
            }

            extend<T> ChainOnlyBox<T> <: Child<T> {
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
        val loadedBoundedPeer = unsatisfiedConsumer.deserializedExtendFacts.singleOrNull { fact ->
            fact.packageName == "ownermatrixlib" &&
                    fact.typeParameterUpperBounds.flatten().any { "Bound" in it.renderedType }
        }
        assertNotNull(loadedBoundedPeer, "the consumer recovers the bounded CJO extend: ${unsatisfiedConsumer.deserializedExtendFacts}")
        assertTrue(
            loadedBoundedPeer!!.privateFunctionNames.contains("f"),
            "the deserialized bounded owner retains its private direct member",
        )
        val satisfiedLibrary = compileNormalCjoLibrary(
            ownerLibrary + "\n\nextend Int64 <: Bound { }\n",
            "ownermatrixlib",
        )
        val ownerPackagePath = satisfiedLibrary.resolve(CjoConstants.packageNameToPath("ownermatrixlib"))
        val ownerPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(ownerPackagePath)))
        val genericExtends = (0 until ownerPackage.allDeclsLength)
            .map { ownerPackage.allDecls(it)!! }
            .filter { it.kind == DeclKind.ExtendDecl && (it.generic?.typeParametersLength ?: 0) > 0 }
        assertEquals(3, genericExtends.size, "the ordinary CJO retains all generic owner declarations")
        assertTrue(genericExtends.all { hasCjoAttribute(it, Attribute.GENERIC) }, "CJO marks generic extend owners")
        val indexedDeclarations = (0 until ownerPackage.allDeclsLength)
            .map { index -> index to ownerPackage.allDecls(index)!! }
        val (privatePeerFunctionIndex, privatePeerFunction) = indexedDeclarations
            .single { (_, declaration) -> declaration.identifier == "f" && hasCjoAttribute(declaration, Attribute.PRIVATE) }
        assertTrue(hasCjoAttribute(privatePeerFunction, Attribute.PRIVATE), "ordinary CJO preserves private members")
        val boundedGenericExtend = genericExtends.single { declaration ->
            val extendInfo = declaration.info(ExtendInfo()) as ExtendInfo
            (0 until extendInfo.bodyLength).any { childIndex ->
                extendInfo.body(childIndex).toInt() - 1 == privatePeerFunctionIndex
            }
        }
        val boundedConstraint = boundedGenericExtend.generic!!.constraints(0)!!
        assertEquals(1, boundedGenericExtend.generic!!.constraintsLength)
        assertTrue(boundedConstraint.uppersLength >= 1, "ordinary CJO retains the bounded extend's upper types")
        val boundedParameterType = checkNotNull(ownerPackage.allTypes(boundedConstraint.type.toInt() - 1))
        assertEquals(PackageFormat.TypeKind.Generic, boundedParameterType.kind)
        val boundedParameterInfo = boundedParameterType.info(GenericTyInfo()) as GenericTyInfo
        val boundedParameterReference = checkNotNull(boundedParameterInfo.declPtr)
        assertEquals(
            "U",
            ownerPackage.allDecls(boundedParameterReference.index.toInt() - 1)!!.identifier,
            "CJO attaches the where-bound to the bounded extend's own type parameter",
        )
        val upperBoundNames = (0 until boundedConstraint.uppersLength).map { upperIndex ->
            val upperType = checkNotNull(ownerPackage.allTypes(boundedConstraint.uppers(upperIndex).toInt() - 1))
            if (upperType.infoType != PackageFormat.SemaTyInfo.CompositeTyInfo) return@map "<${upperType.kind}>"
            val compositeInfo = upperType.info(CompositeTyInfo()) as CompositeTyInfo
            val boundReference = checkNotNull(compositeInfo.declPtr)
            boundReference.decl?.takeIf(String::isNotBlank)
                ?: checkNotNull(ownerPackage.allDecls(boundReference.index.toInt() - 1)).identifier
        }
        val ownerPackageImports = (0 until ownerPackage.importsLength).map(ownerPackage::imports)
        val upperBoundFullIds = (0 until boundedConstraint.uppersLength).mapNotNull { upperIndex ->
            val upperType = checkNotNull(ownerPackage.allTypes(boundedConstraint.uppers(upperIndex).toInt() - 1))
            if (upperType.infoType != PackageFormat.SemaTyInfo.CompositeTyInfo) return@mapNotNull null
            val fullId = checkNotNull((upperType.info(CompositeTyInfo()) as CompositeTyInfo).declPtr)
            val packageName = ownerPackageImports.getOrNull(fullId.pkgId)
            "pkgId=${fullId.pkgId} package=$packageName decl=${fullId.decl} index=${fullId.index}"
        }
        assertTrue("Bound" in upperBoundNames, "ordinary CJO retains the actual interface bound: $upperBoundNames")
        assertTrue(
            loadedBoundedPeer!!.typeParameterUpperBounds.flatten().any { bound ->
                bound.classIdOrPrimitiveClassId == StdlibClassIds.Any
            },
            "CJO resolves the standard library Any default bound by ClassId without a separate std.core CJO: " +
                "bounds=${loadedBoundedPeer!!.typeParameterUpperBounds}, imports=$ownerPackageImports, " +
                "fullIds=$upperBoundFullIds",
        )
        val satisfiedConsumer = compileClasspathSource(consumerSource, "ownermatrixclient", satisfiedLibrary)
        assertTrue(
            satisfiedConsumer.completed,
            "consumer frontend completes: messages=${satisfiedConsumer.messages}, extends=${satisfiedConsumer.deserializedExtendFacts}",
        )
        assertTrue(
            satisfiedConsumer.diagnostics.none {
                it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
            },
            "the satisfied private peer remains invisible to a cross-package CJO consumer",
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
