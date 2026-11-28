package org.cangnova.cangjie.frontend.pipeline

import PackageFormat.Package
import PackageFormat.AnnoKind
import PackageFormat.DeclKind
import PackageFormat.CompositeTyInfo
import PackageFormat.ClassInfo
import PackageFormat.ExtendInfo
import PackageFormat.GenericTyInfo
import PackageFormat.EnumInfo
import PackageFormat.ExprInfo
import PackageFormat.ExprKind
import PackageFormat.FuncArgInfo
import PackageFormat.LitConstInfo
import PackageFormat.ParamInfo
import PackageFormat.UnaryInfo
import PackageFormat.VarInfo
import com.google.flatbuffers.Table
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticWithSource
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirCjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirCjoDeclarationSourceElement
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.isFromCommonPart
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpChirOutput
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpOptimizationLevel
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpModuleDebug
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpModuleOptLevel
import org.cangnova.cangjie.cfir.entrypoint.configuration.CjmpCommonPartInputResolution
import org.cangnova.cangjie.cfir.entrypoint.configuration.configureCjmpCommonPartInputs
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpCommonPartInputResolution
import org.cangnova.cangjie.cfir.entrypoint.configuration.requireCjmpCommonPartInputs
import org.cangnova.cangjie.cfir.entrypoint.session.createCfirCjmpSettingsComponent
import org.cangnova.cangjie.LanguageFeature
import org.cangnova.cangjie.cfir.serialization.CjoConstants
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFormatVersion
import org.cangnova.cangjie.cfir.serialization.cjo.PackageIndex
import org.cangnova.cangjie.cfir.serialization.cjo.CjoAdditionalExpressionInfoMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoExpressionMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoExpressionTargetMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFunctionBodyInfo
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFunctionInfo
import org.cangnova.cangjie.cfir.serialization.cjo.CjoFunctionTypeInfoMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoClassInfo
import org.cangnova.cangjie.cfir.serialization.cjo.CjoStructInfo
import org.cangnova.cangjie.cfir.serialization.cjo.CjoCompositeTypeInfoMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoDeclHashMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoGenericMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoGenericTypeInfoMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoModuleOptionMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoParameterInfo
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageDeclaration
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageHeader
import org.cangnova.cangjie.cfir.serialization.cjo.CjoPackageWriter
import org.cangnova.cangjie.cfir.serialization.cjo.CjoReferenceExpressionMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSchemaProfile
import org.cangnova.cangjie.cfir.serialization.cjo.CjoTypeMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoVariableInfo
import org.cangnova.cangjie.cfir.serialization.cjo.CjmpCommonPartLoadGate
import org.cangnova.cangjie.cfir.serialization.cjo.CjoAnnotationTargetMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoAnnotationArgumentMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoAnnotationMetadata
import org.cangnova.cangjie.cfir.serialization.cjo.CjoLiteralExpressionMetadata
import org.cangnova.cangjie.cfir.session.cjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.isCjmpSpecificCompilationEnabled
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.resolve.providers.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addClasspathRoot
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.cjoOutputDirectory
import org.cangnova.cangjie.config.cjoOutputFile
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.positionalChirInputs
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.config.configureLanguageVersionSettings
import org.cangnova.cangjie.config.languageVersionSettings
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.metadata.model.Attribute
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.frontend.arguments.Freezable
import org.cangnova.cangjie.phaser.CompilerPhase
import org.cangnova.cangjie.phaser.PhaseConfig
import org.cangnova.cangjie.phaser.PhaserState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertSame
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

    /** common 接口的默认实现位由其 common 成员共同决定，并写入 CJO。 */
    @Test
    fun `common interface default attributes are serialized from source`() {
        for (lightTree in listOf(false, true)) {
            val cjo = compileCommon(
                """
                public common interface DefaultOwner {
                    common func f(): Int64 { 1 }
                }
                public common interface RequiredOwner {
                    common func f(): Int64
                }
                """.trimIndent(),
                packageName = "interface_defaults",
                outputDirectoryName = "interface-defaults-$lightTree",
                languageVersion = "1.1.3",
                useLightTree = lightTree,
            )
            val pkg = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(cjo)))
            val declarations = (0 until pkg.allDeclsLength).mapNotNull(pkg::allDecls)
            for ((name, expectedDefault) in listOf("DefaultOwner" to true, "RequiredOwner" to false)) {
                val owner = declarations.single { it.kind == DeclKind.InterfaceDecl && it.identifier == name }
                val info = owner.info(PackageFormat.InterfaceInfo()) as PackageFormat.InterfaceInfo
                val member = (0 until info.bodyLength)
                    .map { pkg.allDecls(info.body(it).toInt() - 1)!! }
                    .single { it.kind == DeclKind.FuncDecl && it.identifier == "f" }
                for (declaration in listOf(owner, member)) {
                    assertTrue(hasCjoAttribute(declaration, Attribute.COMMON), "$name: common, LightTree=$lightTree")
                    assertEquals(
                        expectedDefault,
                        hasCjoAttribute(declaration, Attribute.COMMON_WITH_DEFAULT),
                        "$name/${declaration.identifier}: default, LightTree=$lightTree",
                    )
                }
            }
        }
    }

    /** E8：CJO 中的方法上界必须同时替换 nominal 与 callable 的类型参数。 */
    @Test
    fun `common cjo generic constraints map outer and method type parameters`() {
        for (lightTree in listOf(false, true)) {
            val cjo = compileCommon(
                """
                public interface Rel<T> {}
                public common class Box<T> {
                    public common init() {}
                    public common func apply<V>(value: V): Unit where V <: Rel<T>
                }
                """.trimIndent(),
                packageName = "e8_outer_constraints",
                outputDirectoryName = "outer-constraints-$lightTree",
                languageVersion = "1.1.3",
                useLightTree = lightTree,
            )
            val result = compileSpecific(
                """
                public specific class Box<U> {
                    public specific func apply<W>(value: W): Unit where W <: Rel<U> {}
                }
                """.trimIndent(),
                cjo,
                packageName = "e8_outer_constraints",
                languageVersion = "1.1.3",
                useLightTree = lightTree,
            )
            assertTrue(result.completed, "equivalent outer constraints: ${result.messages}; ${result.diagnostics}")
            assertTrue(result.diagnostics.none { it.severity.isError })
            assertEquals(listOf("Box" to mapOf("T" to "U")), result.cjmpNominalMappings["Box"])
        }
    }

    /** E8：完整映射不能掩盖 specific 侧新增的真实约束。 */
    @Test
    fun `common cjo mapped outer constraints reject an additional bound`() {
        for (lightTree in listOf(false, true)) {
            val cjo = compileCommon(
                """
                public interface Rel<T> {}
                public interface Extra<T> {}
                public common class Box<T> {
                    public common init() {}
                    public common func apply<V>(value: V): Unit where V <: Rel<T>
                }
                """.trimIndent(),
                packageName = "e8_outer_constraints",
                outputDirectoryName = "outer-constraints-stricter-$lightTree",
                languageVersion = "1.1.3",
                useLightTree = lightTree,
            )
            val result = compileSpecific(
                """
                public specific class Box<U> {
                    public specific func apply<W>(value: W): Unit where W <: Rel<U> & Extra<U> {}
                }
                """.trimIndent(),
                cjo,
                packageName = "e8_outer_constraints",
                languageVersion = "1.1.3",
                useLightTree = lightTree,
            )
            assertFalse(result.completed)
            assertEquals(
                listOf("CFIR_GENERIC_CONSTRAINT_NOT_LOOSER"),
                result.diagnostics.filter { it.severity.isError }.map { it.factoryName },
                "additional bounds are checked after the common method has matched",
            )
            assertEquals(listOf("Box" to mapOf("T" to "U")), result.cjmpNominalMappings["Box"])
        }
    }

    @Test
    fun `frontend output mapping defaults common output and clears reused configuration`() {
        val pipeline = CjmpOutputConfigurationProbePipeline()
        val messages = RecordingMessageCollector()
        val configuration = CompilerConfiguration().apply {
            messageCollector = messages
            diagnosticsCollector = DiagnosticsCollectorImpl()
        }

        assertTrue(
            pipeline.execute(CjmpArgumentProbe().apply { cjmpCompileCommon = true }, configuration),
        )
        assertEquals("." to null, pipeline.outputPaths.last(), "official common CLI default is the working directory")

        assertTrue(pipeline.execute(CjmpArgumentProbe(), configuration))
        assertEquals(null to null, pipeline.outputPaths.last(), "a later ordinary invocation must clear common output")

        val explicitOutput = tempDir.resolve("reused-output")
        assertTrue(
            pipeline.execute(
                CjmpArgumentProbe().apply {
                    outputDirectory = explicitOutput.toString()
                    outputFile = "product.chir"
                },
                configuration,
            ),
        )
        assertEquals(explicitOutput.toString() to "product.chir", pipeline.outputPaths.last())

        assertTrue(pipeline.execute(CjmpArgumentProbe(), configuration))
        assertEquals(null to null, pipeline.outputPaths.last(), "a later invocation must clear explicit output paths")
    }

    @Test
    fun `frontend rejects absolute product path when output directory is supplied`() {
        val outputDirectory = Files.createDirectories(tempDir.resolve("invalid-output-directory"))
        val messages = RecordingMessageCollector()
        val configuration = CompilerConfiguration().apply {
            messageCollector = messages
            diagnosticsCollector = DiagnosticsCollectorImpl()
        }
        val disposable = Disposer.newDisposable("cjmp-invalid-output")
        val pipeline = CjmpCfirFrontendPipeline(disposable)
        try {
            val completed = pipeline.execute(
                CjmpArgumentProbe().apply {
                    cjmpCompileCommon = true
                    this.outputDirectory = outputDirectory.toString()
                    outputFile = tempDir.resolve("absolute-product").toString()
                },
                configuration,
            )

            assertFalse(completed, "an absolute product path cannot be combined with an output directory")
            assertEquals(null, pipeline.artifact, "invalid output configuration must stop before the frontend")
            assertEquals(
                listOf(CangJieParsingBundle.message("cli.error.output.file.absolute.with.output.dir")),
                messages.messages.filter { it.first.isError }.map { it.second },
            )
            Files.list(outputDirectory).use { assertEquals(0, it.count(), "invalid output arguments must not write a CJO") }
        } finally {
            Disposer.dispose(disposable)
        }
    }

    /** 生产入口映射 CLI 输出参数，并以写出的 common CJO 驱动 specific 编译。 */
    @Test
    fun `frontend pipeline routes argument outputs and round trips common CJO`() {
        val commonSource = writeCommon(
            """
            public common func platform(): Int64
            public common func withDefault(): Int64 { 41 }
            public common class Constants {
                public common init() {}
                public common static const retained: Int64 = 73
            }
            public common func commonDefault(value!: Int64 = 19): Int64 { value }
            """.trimIndent(),
        )
        val commonOutputDirectory = Files.createDirectories(tempDir.resolve("cli-common-output"))
        val commonMessages = RecordingMessageCollector()
        val commonDiagnostics = DiagnosticsCollectorImpl()
        val commonConfiguration = CompilerConfiguration().apply {
            messageCollector = commonMessages
            diagnosticsCollector = commonDiagnostics
            moduleName = "cjmp_p"
            addCangJieSourceRoot(commonSource.toString())
        }
        val commonDisposable = Disposer.newDisposable("cjmp-cli-common")
        val commonPipeline = CjmpCfirFrontendPipeline(commonDisposable)
        try {
            val completed = commonPipeline.execute(
                CjmpArgumentProbe().apply {
                    cjmpCompileCommon = true
                    outputDirectory = commonOutputDirectory.toString()
                    outputFile = "product.chir"
                    languageVersion = "1.1.3"
                },
                commonConfiguration,
            )
            assertTrue(completed, "common frontend pipeline completes: ${commonMessages.messages}")
            assertNotNull(
                commonPipeline.artifact,
                "common frontend artifact is produced: messages=${commonMessages.messages}, " +
                    "diagnostics=${commonDiagnostics.diagnostics}",
            )
            assertFalse(commonMessages.hasErrors(), "common compilation has no errors: ${commonMessages.messages}")
            assertFalse(
                commonDiagnostics.diagnostics.any { it.severity.isError },
                "common compilation has no structured diagnostic errors: ${commonDiagnostics.diagnostics}",
            )
        } finally {
            Disposer.dispose(commonDisposable)
        }

        val commonCjo = commonOutputDirectory.resolve(CjoConstants.packageNameToPath("cjmp_p"))
        assertTrue(Files.isRegularFile(commonCjo), "common CLI compilation writes its CJO: $commonCjo")
        val commonPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(commonCjo)))
        assertEquals("cjmp_p", commonPackage.fullPkgName)
        val commonDeclarations = (0 until commonPackage.allDeclsLength).mapNotNull(commonPackage::allDecls)
        val serializedPlatform = commonDeclarations.single {
            it.kind == DeclKind.FuncDecl && it.identifier == "platform"
        }
        val serializedDefault = commonDeclarations.single {
            it.kind == DeclKind.FuncDecl && it.identifier == "withDefault"
        }
        val serializedConstants = commonDeclarations.single {
            it.kind == DeclKind.ClassDecl && it.identifier == "Constants"
        }
        val serializedConstantsInfo = serializedConstants.info(ClassInfo()) as ClassInfo
        val serializedConstant = (0 until serializedConstantsInfo.bodyLength)
            .map { commonPackage.allDecls(serializedConstantsInfo.body(it).toInt() - 1)!! }
            .single { it.identifier == "retained" }
        assertEquals(
            1,
            commonDeclarations.count { it.kind == DeclKind.FuncDecl && it.identifier == "commonDefault" },
            "common CJO contains the default-parameter function",
        )
        val serializedDefaultParameterFunction = commonDeclarations.single {
            it.kind == DeclKind.FuncDecl && it.identifier == "commonDefault"
        }
        assertTrue(hasCjoAttribute(serializedPlatform, Attribute.COMMON))
        assertTrue(hasCjoAttribute(serializedPlatform, Attribute.FROM_COMMON_PART))
        assertTrue(hasCjoAttribute(serializedDefault, Attribute.COMMON_WITH_DEFAULT))
        val serializedDefaultInfo = serializedDefault.info(PackageFormat.FuncInfo()) as PackageFormat.FuncInfo
        assertNotNull(serializedDefaultInfo.funcBody, "CJO serializes the default callable signature")
        val serializedConstantInfo = serializedConstant.info(VarInfo()) as VarInfo
        assertTrue(serializedConstantInfo.isConst)
        assertEquals(73L, serializedConstantInfo.int64Value())
        val serializedDefaultParameterIndex = checkNotNull(
            checkNotNull(
                (serializedDefaultParameterFunction.info(PackageFormat.FuncInfo()) as PackageFormat.FuncInfo)
                    .funcBody,
            ).paramLists(0),
        ).params(0).toInt() - 1
        val serializedDefaultParameter = commonPackage.allDecls(serializedDefaultParameterIndex)!!
        val serializedDefaultValueIndex =
            (serializedDefaultParameter.info(ParamInfo()) as ParamInfo).defaultVal.toInt() - 1
        val serializedDefaultValue = commonPackage.allExprs(serializedDefaultValueIndex)!!
        assertEquals(ExprInfo.LitConstInfo, serializedDefaultValue.infoType)
        assertEquals(
            "19",
            (serializedDefaultValue.info(LitConstInfo()) as LitConstInfo).strValue,
            "common CJO stores the source default expression",
        )

        val specificSource = writeSpecific(
            """
            public specific func platform(): Int64 { withDefault() }
            public func consume(): Int64 { Constants.retained + commonDefault() }
            """.trimIndent(),
        )
        val specificProductDirectory = Files.createDirectories(tempDir.resolve("cli-specific-product"))
        val specificMessages = RecordingMessageCollector()
        val specificDiagnostics = DiagnosticsCollectorImpl()
        val specificConfiguration = CompilerConfiguration().apply {
            messageCollector = specificMessages
            diagnosticsCollector = specificDiagnostics
            moduleName = "cjmp_p"
            addCangJieSourceRoot(specificSource.toString())
        }
        val specificDisposable = Disposer.newDisposable("cjmp-cli-specific")
        val specificPipeline = CjmpCfirFrontendPipeline(specificDisposable)
        try {
            val completed = specificPipeline.execute(
                CjmpArgumentProbe().apply {
                    cjmpCommonPart = arrayOf(commonCjo.toString())
                    outputFile = specificProductDirectory.resolve("product").toString()
                    languageVersion = "1.1.3"
                },
                specificConfiguration,
            )
            assertTrue(completed, "specific frontend pipeline completes: ${specificMessages.messages}")
            assertNotNull(
                specificPipeline.artifact,
                "specific frontend artifact is produced: messages=${specificMessages.messages}, " +
                    "diagnostics=${specificDiagnostics.diagnostics}",
            )
            assertFalse(specificMessages.hasErrors(), "specific compilation loads and matches common CJO: ${specificMessages.messages}")
            assertFalse(
                specificDiagnostics.diagnostics.any { it.severity.isError },
                "specific compilation has no structured diagnostic errors: ${specificDiagnostics.diagnostics}",
            )
        } finally {
            Disposer.dispose(specificDisposable)
        }
        val specificOutput = requireNotNull(specificPipeline.artifact).frontendOutput.outputs.single()
        val specificPlatform = specificOutput.fir
            .flatMap { it.declarations }
            .filterIsInstance<CfirCallableDeclaration>()
            .single { it.symbol.name.asString() == "platform" }
        val family = specificOutput.session.cjmpFamilyAggregate.ensureFamilyComplete(
            specificOutput.fir.single().packageDirective.packageFqName,
        )
        assertEquals(1, family.commonCounterpartsFor(specificPlatform).size)
        val commonPlatform = family.commonCounterpartsFor(specificPlatform).single()
        assertEquals(
            CfirCjoDeclarationOrigin(
                packagePath = commonCjo.toAbsolutePath().normalize().toString(),
                declarationIndex = commonDeclarations.indexOf(serializedPlatform),
                isTopLevel = serializedPlatform.isTopLevel,
            ),
            commonPlatform.cjoDeclarationOrigin,
        )
        val exportedRoots = family.packageDeclarationsForExport()
        assertTrue(exportedRoots.any {
            it is CfirCallableDeclaration && it.symbol.name.asString() == "withDefault"
        })
        assertFalse(
            family.commonCounterpartsFor(specificPlatform).single() in exportedRoots,
            "P3 export roots omit the common declaration replaced by the specific implementation",
        )
        val deserializedCommonPlatform = specificOutput.session.dependenciesSymbolProvider
            .getTopLevelCallableSymbols(
                specificOutput.fir.single().packageDirective.packageFqName,
                org.cangnova.cangjie.name.Name.identifier("platform"),
            )
            .map { it.cfir }
            .filterIsInstance<CfirMemberDeclaration>()
        assertTrue(
            deserializedCommonPlatform.any { it.status.isCommon && it.isFromCommonPart },
            "specific pipeline must load the serialized common declaration",
        )
        assertTrue(
            Files.isRegularFile(specificProductDirectory.resolve(CjoConstants.packageNameToPath("cjmp_p"))),
            "specific -o product path routes CJO to its parent directory",
        )
        val specificPackage = Package.getRootAsPackage(
            ByteBuffer.wrap(
                Files.readAllBytes(specificProductDirectory.resolve(CjoConstants.packageNameToPath("cjmp_p"))),
            ),
        )
        val specificDeclarations = (0 until specificPackage.allDeclsLength).mapNotNull(specificPackage::allDecls)
        assertEquals(
            1,
            specificDeclarations.count { it.kind == DeclKind.FuncDecl && it.identifier == "withDefault" },
            "specific CJO must retain the unshadowed common default implementation",
        )
        val exportedDefault = specificDeclarations.single {
            it.kind == DeclKind.FuncDecl && it.identifier == "withDefault"
        }
        assertTrue(hasCjoAttribute(exportedDefault, Attribute.COMMON))
        assertTrue(hasCjoAttribute(exportedDefault, Attribute.FROM_COMMON_PART))
        assertTrue(hasCjoAttribute(exportedDefault, Attribute.COMMON_WITH_DEFAULT))
        val exportedConstants = specificDeclarations.single {
            it.kind == DeclKind.ClassDecl && it.identifier == "Constants"
        }
        val exportedConstantsInfo = exportedConstants.info(ClassInfo()) as ClassInfo
        val exportedConstant = (0 until exportedConstantsInfo.bodyLength)
            .map { specificPackage.allDecls(exportedConstantsInfo.body(it).toInt() - 1)!! }
            .single { it.identifier == "retained" }
        val exportedConstantInfo = exportedConstant.info(VarInfo()) as VarInfo
        assertTrue(exportedConstantInfo.isConst)
        assertEquals(73L, exportedConstantInfo.int64Value())
        assertEquals(
            1,
            specificDeclarations.count { it.kind == DeclKind.FuncDecl && it.identifier == "commonDefault" },
            "specific CJO re-exports the common default-parameter function",
        )
        val exportedDefaultParameterFunction = specificDeclarations.single {
            it.kind == DeclKind.FuncDecl && it.identifier == "commonDefault"
        }
        val exportedDefaultParameterIndex = checkNotNull(
            checkNotNull(
                (exportedDefaultParameterFunction.info(PackageFormat.FuncInfo()) as PackageFormat.FuncInfo)
                    .funcBody,
            ).paramLists(0),
        ).params(0).toInt() - 1
        val exportedDefaultParameter = specificPackage.allDecls(exportedDefaultParameterIndex)!!
        val exportedDefaultValueIndex =
            (exportedDefaultParameter.info(ParamInfo()) as ParamInfo).defaultVal.toInt() - 1
        val exportedDefaultValue = specificPackage.allExprs(exportedDefaultValueIndex)!!
        assertEquals(ExprInfo.LitConstInfo, exportedDefaultValue.infoType)
        assertEquals(
            "19",
            (exportedDefaultValue.info(LitConstInfo()) as LitConstInfo).strValue,
            "specific CJO preserves the original common default expression",
        )
        assertEquals(
            1,
            specificDeclarations.count { it.kind == DeclKind.FuncDecl && it.identifier == "platform" },
            "the common declaration shadowed by the specific implementation must not be re-exported",
        )
        assertTrue(
            specificDeclarations.single { it.kind == DeclKind.FuncDecl && it.identifier == "platform" }
                .let { hasCjoAttribute(it, Attribute.SPECIFIC) },
        )
        assertTrue(
            (0 until specificPackage.allFilesLength).any { specificPackage.allFiles(it)?.endsWith("common.cj") == true },
            "specific CJO retains the source file for an exported common declaration",
        )
        assertTrue(
            (0 until specificPackage.allFilesLength).any { specificPackage.allFiles(it)?.endsWith("specific.cj") == true },
            "specific CJO retains the specific source file",
        )
    }

    @Test
    fun `specific CJO reexports common call initializers to specific counterparts`() {
        val cjo = compileCommon(
            """
            public common func identity(value: Int64): Int64 { value }
            public common class Calls {
                public common init() {}
                public common static var computed: Int64 = identity(19)
            }
            """.trimIndent(),
            outputDirectoryName = "call-initializer-reexport",
        )
        val commonPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(cjo)))
        val commonDeclarations = (0 until commonPackage.allDeclsLength).mapNotNull(commonPackage::allDecls)
        val commonCalls = commonDeclarations.single { it.kind == DeclKind.ClassDecl && it.identifier == "Calls" }
        val commonClassInfo = commonCalls.info(ClassInfo()) as ClassInfo
        val commonComputed = (0 until commonClassInfo.bodyLength)
            .map { commonPackage.allDecls(commonClassInfo.body(it).toInt() - 1)!! }
            .single { it.identifier == "computed" }
        val commonInitializerIndex = (commonComputed.info(VarInfo()) as VarInfo).initializer
        val commonInitializer = commonPackage.allExprs(commonInitializerIndex.toInt() - 1)!!
        assertEquals(PackageFormat.ExprKind.CallExpr, commonInitializer.kind)
        assertEquals(ExprInfo.CallInfo, commonInitializer.infoType)
        assertEquals(2, commonInitializer.operandsLength)
        val commonArgument = commonPackage.allExprs(commonInitializer.operands(1).toInt() - 1)!!
        assertEquals(ExprInfo.FuncArgInfo, commonArgument.infoType)
        assertEquals(1, commonArgument.operandsLength)

        val specificOutput = Files.createDirectories(tempDir.resolve("call-initializer-specific"))
        val specific = compileSpecific(
            content = """
                public specific func identity(value: Int64): Int64 { value }
                public func consume(): Int64 { Calls.computed }
            """.trimIndent(),
            commonCjo = cjo,
            outputDirectory = specificOutput,
        )
        assertTrue(specific.completed, "specific pipeline completes: ${specific.messages}")
        assertTrue(
            specific.diagnostics.none { it.severity.isError },
            "specific compilation loads the common CJO without diagnostics: ${specific.diagnostics}",
        )

        val specificCjo = specificOutput.resolve(CjoConstants.packageNameToPath("cjmp_p"))
        val specificPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(specificCjo)))
        val specificDeclarations = (0 until specificPackage.allDeclsLength).mapNotNull(specificPackage::allDecls)
        val exportedIdentity = specificDeclarations.single {
            it.kind == DeclKind.FuncDecl && it.identifier == "identity"
        }
        assertTrue(hasCjoAttribute(exportedIdentity, Attribute.SPECIFIC))
        val exportedCalls = specificDeclarations.single { it.kind == DeclKind.ClassDecl && it.identifier == "Calls" }
        val exportedClassInfo = exportedCalls.info(ClassInfo()) as ClassInfo
        val exportedComputed = (0 until exportedClassInfo.bodyLength)
            .map { specificPackage.allDecls(exportedClassInfo.body(it).toInt() - 1)!! }
            .single { it.identifier == "computed" }
        val initializerIndex = (exportedComputed.info(VarInfo()) as VarInfo).initializer
        val initializer = specificPackage.allExprs(initializerIndex.toInt() - 1)!!
        assertEquals(PackageFormat.ExprKind.CallExpr, initializer.kind)
        assertEquals(ExprInfo.CallInfo, initializer.infoType)
        val callInfo = initializer.info(PackageFormat.CallInfo()) as PackageFormat.CallInfo
        assertEquals(PackageFormat.CallKind.CallDeclaredFunction, callInfo.callKind)
        assertFalse(callInfo.hasSideEffect)
        assertEquals(2, initializer.operandsLength)
        val callee = specificPackage.allExprs(initializer.operands(0).toInt() - 1)!!
        assertEquals(PackageFormat.ExprKind.RefExpr, callee.kind)
        val reference = callee.info(PackageFormat.ReferenceInfo()) as PackageFormat.ReferenceInfo
        assertEquals("identity", reference.reference)
        assertEquals(PackageIndex.CURRENT.value, reference.target?.pkgId)
        assertEquals(
            specificDeclarations.indexOf(exportedIdentity).toUInt() + 1u,
            reference.target!!.index,
            "the re-exported call target is remapped to the identity declaration in the new CJO",
        )
        assertTrue(callee.type in 1u..specificPackage.allTypesLength.toUInt())
        val argument = specificPackage.allExprs(initializer.operands(1).toInt() - 1)!!
        assertEquals(ExprInfo.FuncArgInfo, argument.infoType)
        val argumentInfo = argument.info(FuncArgInfo()) as FuncArgInfo
        assertFalse(argumentInfo.withInout)
        assertFalse(argumentInfo.isDefaultVal)
        assertEquals(1, argument.operandsLength)
        val argumentValue = specificPackage.allExprs(argument.operands(0).toInt() - 1)!!
        assertEquals(PackageFormat.ExprKind.LitConstExpr, argumentValue.kind)
        assertEquals("19", (argumentValue.info(LitConstInfo()) as LitConstInfo).strValue)

        val reloadedConsumer = compileClasspathSource(
            """
            import cjmp_p.*

            public func consume(): Int64 { Calls.computed }
            public func consumeIdentity(): Int64 { identity(23) }
            """.trimIndent(),
            packageName = "cjmp_reexport_consumer",
            classpathRoot = specificOutput,
        )
        assertTrue(reloadedConsumer.completed, "a later consumer loads the specific CJO: ${reloadedConsumer.messages}")
        assertTrue(
            reloadedConsumer.diagnostics.none { it.severity.isError },
            "the specific CJO exposes the re-exported common declarations: ${reloadedConsumer.diagnostics}",
        )
    }

    @Test
    fun `source CJO preserves imported expression FullId`() {
        val dependencyOutput = compileNormalCjoLibrary(
            "public const func externalValue(): Int64 { 42 }",
            packageName = "cjo_expression_dependency",
        )
        val dependencyPackage = Package.getRootAsPackage(
            ByteBuffer.wrap(
                Files.readAllBytes(dependencyOutput.resolve(CjoConstants.packageNameToPath("cjo_expression_dependency"))),
            ),
        )
        val dependencyFunction = (0 until dependencyPackage.allDeclsLength)
            .mapNotNull(dependencyPackage::allDecls)
            .single { it.kind == DeclKind.FuncDecl && it.identifier == "externalValue" }
        val expectedExternalKey = dependencyFunction.exportId?.takeIf(String::isNotBlank)
            ?: dependencyFunction.identifier

        val sourceRoot = Files.createDirectories(tempDir.resolve("imported-expression-source"))
        val source = sourceRoot.resolve("consumer.cj")
        Files.writeString(
            source,
            """
            package cjo_expression_consumer

            import cjo_expression_dependency.*

            public let copied: Int64 = externalValue()
            public const constant: Int64 = externalValue()
            """.trimIndent(),
        )
        val outputDirectory = Files.createDirectories(tempDir.resolve("imported-expression-output"))
        val result = compile(source) {
            assertTrue(configureLanguageVersionSettings("1.1.3"))
            addClasspathRoot(dependencyOutput.toString())
            cjoOutputDirectory = outputDirectory.toString()
        }
        assertTrue(result.completed, "consumer frontend completes: ${result.messages}")
        assertTrue(result.diagnostics.none { it.severity.isError }, "consumer diagnostics: ${result.diagnostics}")

        val consumerPackage = Package.getRootAsPackage(
            ByteBuffer.wrap(
                Files.readAllBytes(outputDirectory.resolve(CjoConstants.packageNameToPath("cjo_expression_consumer"))),
            ),
        )
        val consumerImports = (0 until consumerPackage.importsLength).mapNotNull(consumerPackage::imports)
        val declarations = (0 until consumerPackage.allDeclsLength)
            .mapNotNull(consumerPackage::allDecls)
            .filter { it.kind == DeclKind.VarDecl }
        val copied = declarations.single { it.identifier == "copied" }
        val constant = declarations.single { it.identifier == "constant" }
        // 官方 1.1.3 的 VarDecl 保存筛选保留 const 初始化式，普通全局 let 的槽为空。
        assertEquals(0u, (copied.info(VarInfo()) as VarInfo).initializer)
        val initializer = consumerPackage.allExprs(
            ((constant.info(VarInfo()) as VarInfo).initializer.toInt() - 1),
        )!!
        assertEquals(PackageFormat.ExprKind.CallExpr, initializer.kind)
        val callee = consumerPackage.allExprs(initializer.operands(0).toInt() - 1)!!
        assertEquals(PackageFormat.ExprKind.RefExpr, callee.kind)
        val reference = callee.info(PackageFormat.ReferenceInfo()) as PackageFormat.ReferenceInfo
        val target = checkNotNull(reference.target)
        assertEquals(consumerImports.indexOf("cjo_expression_dependency"), target.pkgId)
        assertEquals(expectedExternalKey, target.decl)
        assertEquals(0u, target.index)
    }

    @Test
    fun `source CJO preserves imported class declaration FullId`() {
        val dependencyPackageName = "cjo_type_dependency"
        val exportedClassKey = "wire::external-type-key"
        val identifierFallbackKey = "IdentifierOnly"
        val dependencyOutput = Files.createDirectories(tempDir.resolve("imported-class-type-dependency"))
        CjoPackageWriter.write(
            dependencyOutput.resolve(CjoConstants.packageNameToPath(dependencyPackageName)),
            CjoPackageMetadata(
                fullPackageName = dependencyPackageName,
                moduleName = dependencyPackageName,
                allFiles = listOf("dependency.cj"),
                declarations = listOf(
                    CjoPackageDeclaration(
                        identifier = "ExternalType",
                        kind = DeclKind.ClassDecl,
                        exportId = exportedClassKey,
                        type = 1u,
                        info = CjoClassInfo(),
                    ),
                    CjoPackageDeclaration(
                        identifier = identifierFallbackKey,
                        kind = DeclKind.ClassDecl,
                        type = 2u,
                        info = CjoClassInfo(),
                    ),
                ),
                types = listOf(
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Class,
                        semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = 1u),
                    ),
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Class,
                        semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = 2u),
                    ),
                ),
            ),
        )
        CjoPackageWriter.write(
            dependencyOutput.resolve(CjoConstants.packageNameToPath("std.core")),
            CjoPackageMetadata(
                fullPackageName = "std.core",
                moduleName = "std.core",
                allFiles = listOf("std-core.cj"),
                declarations = listOf(
                    CjoPackageDeclaration(
                        identifier = "String",
                        kind = DeclKind.StructDecl,
                        exportId = "_CNat6StringE",
                        type = 1u,
                        info = CjoStructInfo(),
                    ),
                    CjoPackageDeclaration(
                        identifier = "Object",
                        kind = DeclKind.ClassDecl,
                        exportId = "_CNat6ObjectE",
                        type = 2u,
                        info = CjoClassInfo(),
                    ),
                ),
                types = listOf(
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Struct,
                        semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = 1u),
                    ),
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Class,
                        semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = 2u),
                    ),
                ),
            ),
        )
        val dependencyPackage = Package.getRootAsPackage(
            ByteBuffer.wrap(
                Files.readAllBytes(dependencyOutput.resolve(CjoConstants.packageNameToPath(dependencyPackageName))),
            ),
        )
        val dependencyClass = (0 until dependencyPackage.allDeclsLength)
            .mapNotNull(dependencyPackage::allDecls)
            .single { it.kind == DeclKind.ClassDecl && it.identifier == "ExternalType" }
        assertEquals(exportedClassKey, dependencyClass.exportId)
        assertTrue(dependencyClass.identifier != dependencyClass.exportId)
        val fallbackClass = (0 until dependencyPackage.allDeclsLength)
            .mapNotNull(dependencyPackage::allDecls)
            .single { it.kind == DeclKind.ClassDecl && it.identifier == identifierFallbackKey }
        assertEquals(null, fallbackClass.exportId)

        val sourceRoot = Files.createDirectories(tempDir.resolve("imported-class-type-source"))
        val source = sourceRoot.resolve("consumer.cj")
        Files.writeString(
            source,
            """
            package cjo_type_consumer

            import cjo_type_dependency.*
            import std.core.*

            public func roundTrip(value: ExternalType): ExternalType { value }
            public func identifierFallback(value: IdentifierOnly): IdentifierOnly { value }
            public func stringRoundTrip(value: String): String { value }
            public func objectRoundTrip(value: Object): Object { value }
            """.trimIndent(),
        )
        val outputDirectory = Files.createDirectories(tempDir.resolve("imported-class-type-output"))
        val result = compile(source) {
            assertTrue(configureLanguageVersionSettings("1.1.3"))
            addClasspathRoot(dependencyOutput.toString())
            cjoOutputDirectory = outputDirectory.toString()
        }
        assertTrue(result.completed, "consumer frontend completes: ${result.messages}")
        assertTrue(result.diagnostics.none { it.severity.isError }, "consumer diagnostics: ${result.diagnostics}")

        val consumerPackage = Package.getRootAsPackage(
            ByteBuffer.wrap(
                Files.readAllBytes(outputDirectory.resolve(CjoConstants.packageNameToPath("cjo_type_consumer"))),
            ),
        )
        val consumerImports = (0 until consumerPackage.importsLength).mapNotNull(consumerPackage::imports)
        val dependencyImportIndex = consumerImports.indexOf("cjo_type_dependency")
        assertTrue(dependencyImportIndex >= 0, "the imported type package is present in CJO imports: $consumerImports")
        val stdCoreImportIndex = consumerImports.indexOf("std.core")
        assertTrue(stdCoreImportIndex >= 0, "std.core is present in CJO imports: $consumerImports")
        val importedClassReferences = (0 until consumerPackage.allTypesLength)
            .mapNotNull(consumerPackage::allTypes)
            .filter { it.infoType == PackageFormat.SemaTyInfo.CompositeTyInfo }
            .mapNotNull { type ->
                (type.info(CompositeTyInfo()) as? CompositeTyInfo)?.declPtr
            }
            .filter { it.pkgId == dependencyImportIndex }
        assertTrue(importedClassReferences.isNotEmpty(), "the function signature references the imported class")
        val importedKeys = importedClassReferences.map { it.decl }.toSet()
        assertEquals(
            setOf(exportedClassKey, identifierFallbackKey),
            importedKeys,
            "external composite FullIds use exportId and then identifier fallback",
        )
        assertTrue(importedClassReferences.all { it.index == 0u })
        val stdCoreCompositeTypes = (0 until consumerPackage.allTypesLength)
            .mapNotNull(consumerPackage::allTypes)
            .filter { it.infoType == PackageFormat.SemaTyInfo.CompositeTyInfo }
            .mapNotNull { type ->
                (type.info(CompositeTyInfo()) as? CompositeTyInfo)?.declPtr?.let { it to type.kind }
            }
            .filter { (reference, _) -> reference.pkgId == stdCoreImportIndex }
        assertEquals(
            setOf(
                "_CNat6StringE" to PackageFormat.TypeKind.Struct,
                "_CNat6ObjectE" to PackageFormat.TypeKind.Class,
            ),
            stdCoreCompositeTypes.map { (reference, kind) -> reference.decl to kind }.toSet(),
            "standard-library composite types preserve official export IDs and String struct or Object class kinds",
        )
        assertTrue(stdCoreCompositeTypes.all { (reference, _) -> reference.index == 0u })
    }

    /** 三段真实源码编译保证外部类型 FullId 不会被同名泛型参数占用。 */
    @Test
    fun `source CJO distinguishes imported class from preceding generic parameter with same name`() {
        val sourceRoot = Files.createDirectories(tempDir.resolve("same-name-type-sources"))
        val dependencySource = sourceRoot.resolve("dependency.cj")
        Files.writeString(
            dependencySource,
            """
            package same_name_dependency

            public class Holder<T> {}
            public class T {}
            """.trimIndent(),
        )
        val dependencyOutput = Files.createDirectories(tempDir.resolve("same-name-type-dependency"))
        val dependency = compile(dependencySource) {
            moduleName = "same_name_dependency"
            assertTrue(configureLanguageVersionSettings("1.1.3"))
            cjoOutputDirectory = dependencyOutput.toString()
        }
        assertTrue(dependency.completed, "dependency frontend completes: ${dependency.messages}")
        assertTrue(dependency.diagnostics.none { it.severity.isError }, "dependency diagnostics: ${dependency.diagnostics}")
        assertTrue(dependency.messages.none { it.first.isError }, "dependency messages: ${dependency.messages}")

        val intermediateSource = sourceRoot.resolve("intermediate.cj")
        Files.writeString(
            intermediateSource,
            """
            package same_name_intermediate

            import same_name_dependency.*

            public func identity(value: T): T { value }
            """.trimIndent(),
        )
        val intermediateOutput = Files.createDirectories(tempDir.resolve("same-name-type-intermediate"))
        val intermediate = compile(intermediateSource) {
            moduleName = "same_name_intermediate"
            assertTrue(configureLanguageVersionSettings("1.1.3"))
            addClasspathRoot(dependencyOutput.toString())
            cjoOutputDirectory = intermediateOutput.toString()
        }
        assertTrue(intermediate.completed, "intermediate frontend completes: ${intermediate.messages}")
        assertTrue(intermediate.diagnostics.none { it.severity.isError }, "intermediate diagnostics: ${intermediate.diagnostics}")
        assertTrue(intermediate.messages.none { it.first.isError }, "intermediate messages: ${intermediate.messages}")

        val dependencyPackage = Package.getRootAsPackage(
            ByteBuffer.wrap(
                Files.readAllBytes(dependencyOutput.resolve(CjoConstants.packageNameToPath("same_name_dependency"))),
            ),
        )
        val dependencyDeclarations = (0 until dependencyPackage.allDeclsLength)
            .mapNotNull(dependencyPackage::allDecls)
        val importedClassIndex = dependencyDeclarations.indexOfFirst {
            it.kind == DeclKind.ClassDecl && it.identifier == "T"
        }
        assertTrue(importedClassIndex >= 0, "the dependency publishes the top-level class T")
        val importedClass = dependencyDeclarations[importedClassIndex]
        val genericParameter = dependencyDeclarations.single {
            it.kind == DeclKind.GenericParamDecl && it.identifier == "T"
        }
        val dependencyHeader = CjoPackageHeader.fromPackage(dependencyPackage)
        val intermediatePackage = Package.getRootAsPackage(
            ByteBuffer.wrap(
                Files.readAllBytes(intermediateOutput.resolve(CjoConstants.packageNameToPath("same_name_intermediate"))),
            ),
        )
        val intermediateImports = (0 until intermediatePackage.importsLength).mapNotNull(intermediatePackage::imports)
        val dependencyImportIndex = intermediateImports.indexOf("same_name_dependency")
        assertTrue(dependencyImportIndex >= 0, "the intermediate CJO imports the dependency: $intermediateImports")
        val importedTypes = (0 until intermediatePackage.allTypesLength)
            .mapNotNull(intermediatePackage::allTypes)
            .filter { it.infoType == PackageFormat.SemaTyInfo.CompositeTyInfo }
            .mapNotNull { type -> (type.info(CompositeTyInfo()) as? CompositeTyInfo)?.declPtr }
            .filter { it.pkgId == dependencyImportIndex }
        assertTrue(importedTypes.isNotEmpty(), "identity serializes an external composite FullId")
        assertTrue(importedTypes.all { it.index == 0u }, "external FullIds resolve through their declaration key")
        assertEquals(setOf(importedClass.exportId ?: importedClass.identifier), importedTypes.map { it.decl }.toSet())
        for (reference in importedTypes) {
            val resolvedIndex = dependencyHeader.fullIdReferenceKeyToIndex[reference.decl]
            assertEquals(importedClassIndex, resolvedIndex, "the actual external FullId must select the class T")
            val resolvedDeclaration = dependencyDeclarations[checkNotNull(resolvedIndex)]
            assertEquals(DeclKind.ClassDecl, resolvedDeclaration.kind)
            assertEquals("T", resolvedDeclaration.identifier)
        }
        assertTrue(
            (importedClass.exportId ?: importedClass.identifier) != (genericParameter.exportId ?: genericParameter.identifier),
            "the class T and Holder's generic T must publish different declaration reference keys",
        )

        val consumerSource = sourceRoot.resolve("consumer.cj")
        Files.writeString(
            consumerSource,
            """
            package same_name_consumer

            import same_name_dependency.*
            import same_name_intermediate.*

            public func consume(value: T): T { identity(value) }
            """.trimIndent(),
        )
        val consumer = compile(consumerSource) {
            moduleName = "same_name_consumer"
            assertTrue(configureLanguageVersionSettings("1.1.3"))
            addClasspathRoot(dependencyOutput.toString())
            addClasspathRoot(intermediateOutput.toString())
        }
        assertTrue(consumer.completed, "consumer frontend loads identity through its external type FullId: ${consumer.messages}")
        assertTrue(consumer.diagnostics.none { it.severity.isError }, "consumer diagnostics: ${consumer.diagnostics}")
        assertTrue(consumer.messages.none { it.first.isError }, "consumer messages: ${consumer.messages}")
    }

    @Test
    fun `specific CJO remaps common initialization dependencies to specific field counterparts`() {
        val commonCjo = compileCommon(
            """
            public common class Box {
                public common let target: Int64 = 1
                public common let dependent: Int64 = target
                public common init()
            }
            """.trimIndent(),
            outputDirectoryName = "initialization-counterpart-common",
        )
        val commonPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(commonCjo)))
        val commonDeclarations = (0 until commonPackage.allDeclsLength).mapNotNull(commonPackage::allDecls)
        val commonBox = commonDeclarations.single { it.kind == DeclKind.ClassDecl && it.identifier == "Box" }
        val commonBoxInfo = commonBox.info(ClassInfo()) as ClassInfo
        val commonFields = (0 until commonBoxInfo.bodyLength)
            .map { commonPackage.allDecls(commonBoxInfo.body(it).toInt() - 1)!! }
            .associateBy { it.identifier }
        checkNotNull(commonFields["target"])
        val commonDependent = checkNotNull(commonFields["dependent"])
        val commonTargetIndex = (0 until commonBoxInfo.bodyLength)
            .map(commonBoxInfo::body)
            .single { commonPackage.allDecls(it.toInt() - 1)?.identifier == "target" }
        assertEquals(1, commonDependent.dependenciesLength, "the common CJO records the initializer edge")
        assertEquals(commonTargetIndex, commonDependent.dependencies(0)!!.index)

        val specificOutput = Files.createDirectories(tempDir.resolve("initialization-counterpart-specific"))
        val specific = compileSpecific(
            content = """
                public specific class Box {
                    public specific let target: Int64 = 2
                    public specific init() {}
                }
            """.trimIndent(),
            commonCjo = commonCjo,
            outputDirectory = specificOutput,
        )
        assertTrue(specific.completed, "specific pipeline completes: ${specific.messages}")
        assertTrue(specific.diagnostics.none { it.severity.isError }, "specific diagnostics: ${specific.diagnostics}")

        val specificCjo = specificOutput.resolve(CjoConstants.packageNameToPath("cjmp_p"))
        val specificPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(specificCjo)))
        val specificDeclarations = (0 until specificPackage.allDeclsLength).mapNotNull(specificPackage::allDecls)
        val specificBox = specificDeclarations.single { it.kind == DeclKind.ClassDecl && it.identifier == "Box" }
        val specificBoxInfo = specificBox.info(ClassInfo()) as ClassInfo
        val specificFields = (0 until specificBoxInfo.bodyLength)
            .map { specificPackage.allDecls(specificBoxInfo.body(it).toInt() - 1)!! }
            .associateBy { it.identifier }
        checkNotNull(specificFields["target"])
        val specificDependent = checkNotNull(specificFields["dependent"])
        val specificTargetIndex = (0 until specificBoxInfo.bodyLength)
            .map(specificBoxInfo::body)
            .single { specificPackage.allDecls(it.toInt() - 1)?.identifier == "target" }
        assertEquals(1, specificDependent.dependenciesLength, "the common-only field keeps its dependency")
        val dependency = specificDependent.dependencies(0)!!
        assertEquals(PackageIndex.CURRENT.value, dependency.pkgId)
        assertEquals(specificTargetIndex, dependency.index, "the dependency resolves to the specific replacement field")
    }

    @Test
    fun `annotation literal matching normalizes source and CJO values for both frontends`() {
        val stdCoreOutput = Files.createDirectories(tempDir.resolve("annotation-literals-stdlib"))
        CjoPackageWriter.write(
            stdCoreOutput.resolve(CjoConstants.packageNameToPath("std.core")),
            CjoPackageMetadata(
                fullPackageName = "std.core",
                moduleName = "std.core",
                allFiles = listOf("std-core.cj"),
                declarations = listOf(
                    CjoPackageDeclaration(
                        identifier = "String",
                        kind = DeclKind.StructDecl,
                        exportId = "_CNat6StringE",
                        type = 1u,
                        info = CjoStructInfo(),
                    ),
                ),
                types = listOf(
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Struct,
                        semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = 1u),
                    ),
                ),
            ),
        )
        val annotationLibrary = compileNormalCjoLibrary(
            """
            import std.core.*

            @Annotation
            public class Tag {
                public let level: Int64 = 0
                public const init(level_val: Int64) {}
            }

            @Annotation
            public class TextTag {
                public let value: String = ""
                public const init(value: String) {}
            }

            @Annotation
            public class BoolTag {
                public let value: Bool = false
                public const init(value: Bool) {}
            }

            @Annotation
            public class ByteTag {
                public let value: UInt8 = 0
                public const init(value: UInt8) {}
            }

            @Annotation
            public class RuneTag {
                public let value: Rune = r'a'
                public const init(value: Rune) {}
            }
            """.trimIndent(),
            "cjmp_annotations",
            classpathRoots = listOf(stdCoreOutput),
        )
        val commonSource = """
            import std.core.*
            import cjmp_annotations.*

            @!Tag[-1]
            public common func negativeLiteral(): Int64

            @!TextTag["\$"]
            public common func escapedDollar(): Int64

            @!BoolTag[true]
            public common func booleanLiteral(): Int64

            @!ByteTag[b'a']
            public common func byteLiteral(): Int64

            @!RuneTag[r'a']
            public common func runeLiteral(): Int64

            @!RuneTag[r'\$']
            public common func runeEscapedDollar(): Int64
        """.trimIndent()

        val specificSource = """
            import std.core.*
            import cjmp_annotations.*

            @!Tag[-1]
            public specific func negativeLiteral(): Int64 { 1 }

            @!TextTag["\$"]
            public specific func escapedDollar(): Int64 { 3 }

            @!BoolTag[true]
            public specific func booleanLiteral(): Int64 { 4 }

            @!ByteTag[b'a']
            public specific func byteLiteral(): Int64 { 5 }

            @!RuneTag[r'\u{61}']
            public specific func runeLiteral(): Int64 { 6 }

            @!RuneTag[r'$']
            public specific func runeEscapedDollar(): Int64 { 7 }
        """.trimIndent()

        for (useLightTree in listOf(false, true)) {
            val commonCjo = compileCommon(
                commonSource,
                outputDirectoryName = "annotation-literals-common-$useLightTree",
                useLightTree = useLightTree,
                classpathRoots = listOf(stdCoreOutput, annotationLibrary),
            )
            val commonPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(commonCjo)))
            val commonDeclarations = (0 until commonPackage.allDeclsLength).mapNotNull(commonPackage::allDecls)
            val customAnnotation = requireNotNull(
                commonDeclarations.single { it.identifier == "negativeLiteral" }.annotations(0),
            )
            val customAnnotationTarget = requireNotNull(customAnnotation.target)
            assertTrue(
                customAnnotationTarget.pkgId in 0 until commonPackage.importsLength,
                "CUSTOM CJO annotations retain an imported annotation-constructor FullId",
            )
            assertFalse(customAnnotationTarget.decl.isNullOrBlank())

            fun assertSerializedAnnotationLiteral(
                declarationName: String,
                expectedKind: UByte,
                expectedValue: String,
            ) {
                val declaration = commonDeclarations.single { it.identifier == declarationName }
                val annotation = requireNotNull(declaration.annotations(0))
                val argument = requireNotNull(annotation.args(0))
                val literal = requireNotNull(commonPackage.allExprs(argument.expr.toInt() - 1))
                assertEquals(ExprInfo.LitConstInfo, literal.infoType, "$declarationName annotation argument kind")
                val literalInfo = literal.info(LitConstInfo()) as LitConstInfo
                assertEquals(expectedKind, literalInfo.constKind, "$declarationName CJO literal kind")
                assertEquals(expectedValue, literalInfo.strValue, "$declarationName CJO literal value")
            }

            assertSerializedAnnotationLiteral(
                "byteLiteral",
                PackageFormat.LitConstKind.RuneByte,
                "b'a'",
            )
            assertSerializedAnnotationLiteral(
                "runeLiteral",
                PackageFormat.LitConstKind.Rune,
                "a",
            )
            assertSerializedAnnotationLiteral(
                "runeEscapedDollar",
                PackageFormat.LitConstKind.Rune,
                "$",
            )
            assertSerializedAnnotationLiteral(
                "negativeLiteral",
                PackageFormat.LitConstKind.Integer,
                "-1",
            )
            val specific = compileSpecific(
                content = specificSource,
                commonCjo = commonCjo,
                classpathRoots = listOf(stdCoreOutput, annotationLibrary),
                useLightTree = useLightTree,
            )
            val diagnosticLocations = specific.diagnostics.joinToString { diagnostic ->
                val sourceOffset = (diagnostic as? CjDiagnosticWithSource)?.element?.startOffset
                val sourceLine = sourceOffset?.let { specificSource.take(it).count { character -> character == '\n' } + 1 }
                diagnostic.factoryName + "@" + (sourceLine?.toString() ?: "<unknown line>") +
                    ": " + diagnostic.renderMessage()
            }

            assertTrue(
                specific.completed,
                "specific pipeline completes; diagnostics=" + diagnosticLocations,
            )
            assertTrue(
                specific.diagnostics.none { it.factoryName.endsWith("SPECIFIC_HAS_DIFFERENT_ANNOTATION") },
                "equivalent annotation literals match after common CJO round-trip ($useLightTree): ${specific.diagnostics}",
            )
            assertTrue(specific.diagnostics.none { it.severity.isError }, "specific diagnostics: ${specific.diagnostics}")
        }
    }

    @Test
    fun `annotation mismatch retains both sides and the common CJO declaration location`() {
        val commonSource = """
            @Annotation
            public common class Tag {
                public common let value: Int64 = 0
                public common const init(value: Int64)
            }

            @!Tag[1]
            public common func annotated(): Int64
        """.trimIndent()
        val specificSource = """
            @Annotation
            public specific class Tag {
                public specific let value: Int64 = 0
                public specific const init(value: Int64) {}
            }

            @!Tag[2]
            public specific func annotated(): Int64 { 1 }
        """.trimIndent()

        for (useLightTree in listOf(false, true)) {
            val commonCjo = compileCommon(
                commonSource,
                outputDirectoryName = "annotation-mismatch-common-$useLightTree",
                useLightTree = useLightTree,
            )
            val specific = compileSpecific(
                specificSource,
                commonCjo,
                useLightTree = useLightTree,
            )
            val mismatches = specific.diagnostics.filter {
                it.factoryName.endsWith("SPECIFIC_HAS_DIFFERENT_ANNOTATION")
            }

            assertEquals(2, mismatches.size, "both unmatched annotation sides are diagnosed; LightTree=$useLightTree")
            val commonOnly = mismatches.single { diagnostic ->
                diagnostic.relatedInformation.any { it.message == "The mismatched annotation is @Tag(1)" }
            }
            assertEquals(
                listOf("The mismatched annotation is @Tag(1)"),
                commonOnly.relatedInformation.map { it.message },
            )

            val specificOnly = mismatches.single { diagnostic ->
                diagnostic.relatedInformation.any { it.message == "The corresponding common declaration" }
            }
            assertEquals(
                listOf(
                    "The corresponding common declaration",
                    "Either remove the specific annotation or add it to the common declaration",
                ),
                specificOnly.relatedInformation.map { it.message },
            )
            val commonDeclarationNote = specificOnly.relatedInformation.first()
            assertInstanceOf(CjBinarySourceElement::class.java, commonDeclarationNote.element)
            assertTrue(commonDeclarationNote.sourceLocation?.filePath?.endsWith("common.cj") == true)
            assertEquals(10, commonDeclarationNote.sourceLocation?.line)
            assertEquals(20, commonDeclarationNote.sourceLocation?.column)
            assertTrue(
                specificOnly.renderMessage().contains("note: The corresponding common declaration"),
                "the rendered diagnostic carries its CJO navigation note",
            )
        }
    }

    @Test
    fun `nonliteral common custom annotation arguments are omitted by CJO before matching`() {
        val commonSource = """
            @Annotation
            public common class Tag {
                public common let value: Int64 = 0
                public common const init(value: Int64)
            }

            @!Tag[1 + 0]
            public common func nonliteralArgument(): Int64
        """.trimIndent()
        val specificSource = """
            @!Tag[1]
            public specific func nonliteralArgument(): Int64 { 1 }
        """.trimIndent()

        for (useLightTree in listOf(false, true)) {
            val commonCjo = compileCommon(
                commonSource,
                outputDirectoryName = "annotation-nonliteral-common-$useLightTree",
                useLightTree = useLightTree,
            )
            val commonPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(commonCjo)))
            val declaration = (0 until commonPackage.allDeclsLength)
                .mapNotNull(commonPackage::allDecls)
                .single { it.identifier == "nonliteralArgument" }
            assertEquals(0, requireNotNull(declaration.annotations(0)).argsLength)

            val specific = compileSpecific(specificSource, commonCjo, useLightTree = useLightTree)
            val mismatches = specific.diagnostics.filter {
                it.factoryName.endsWith("SPECIFIC_HAS_DIFFERENT_ANNOTATION")
            }
            assertEquals(2, mismatches.size, "the specific literal differs from the CJO-projected common annotation")
            assertTrue(
                mismatches.any { diagnostic ->
                    diagnostic.relatedInformation.any { it.message.startsWith("The mismatched annotation is") }
                },
                "the CJO-projected common annotation is reported as unmatched",
            )
            assertTrue(
                mismatches.any { diagnostic ->
                    diagnostic.relatedInformation.any { it.message == "The corresponding common declaration" }
                },
                "the specific-only annotation retains its common declaration note",
            )
        }
    }

    @Test
    fun `nonliteral RuneByte annotation argument is omitted from CJO and remains unmatched`() {
        val commonSource = """
            @Annotation
            public common class ByteTag {
                public common let value: UInt8 = 0
                public common const init(value: UInt8)
            }

            @!ByteTag[-b'a']
            public common func unaryByteLiteral(): Int64
        """.trimIndent()
        val specificSource = """
            @!ByteTag[-b'a']
            public specific func unaryByteLiteral(): Int64 { 1 }
        """.trimIndent()

        for (useLightTree in listOf(false, true)) {
            val commonCjo = compileCommon(
                commonSource,
                outputDirectoryName = "annotation-unary-byte-common-$useLightTree",
                useLightTree = useLightTree,
            )
            val commonPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(commonCjo)))
            val declaration = (0 until commonPackage.allDeclsLength)
                .mapNotNull(commonPackage::allDecls)
                .single { it.identifier == "unaryByteLiteral" }
            val annotation = requireNotNull(declaration.annotations(0))
            assertEquals(0, annotation.argsLength, "ordinary CUSTOM CJO annotations omit non-literal arguments")

            val specific = compileSpecific(specificSource, commonCjo, useLightTree = useLightTree)
            val mismatches = specific.diagnostics.filter {
                it.factoryName.endsWith("SPECIFIC_HAS_DIFFERENT_ANNOTATION")
            }
            assertEquals(
                2,
                mismatches.size,
                "the official matcher rejects the CJO-projected common annotation and specific non-literal argument: ${specific.diagnostics}",
            )
            assertEquals(
                listOf("The mismatched annotation is @ByteTag"),
                mismatches.single { diagnostic ->
                    diagnostic.relatedInformation.any { it.message.startsWith("The mismatched annotation is") }
                }.relatedInformation.map { it.message },
            )
        }
    }

    @Test
    fun `specific CJO merges repeated common CJO identities across HMPP sessions`() {
        val commonCjo = compileCommon(
            "public common func shared(): Int64 { 1 }",
            outputDirectoryName = "hmpp-repeated-common-cjo",
        )
        val commonSource = Files.createDirectories(tempDir.resolve("hmpp-common-source"))
            .resolve("shared.cj")
            .also {
                Files.writeString(
                    it,
                    """
                    package cjmp_p
                    public func sourceShared(): Int64 { 1 }
                    public class SourceBox {
                        public func member(): Int64 { 1 }
                    }
                    """.trimIndent(),
                )
            }
        val moduleOneSource = Files.createDirectories(tempDir.resolve("hmpp-module-one"))
            .resolve("module-one.cj")
            .also { Files.writeString(it, "package cjmp_p\npublic func moduleOne(): Int64 { 1 }\n") }
        val moduleTwoSource = Files.createDirectories(tempDir.resolve("hmpp-module-two"))
            .resolve("module-two.cj")
            .also { Files.writeString(it, "package cjmp_p\npublic func moduleTwo(): Int64 { 2 }\n") }
        val outputDirectory = Files.createDirectories(tempDir.resolve("hmpp-repeated-common-cjo-output"))
        val result = compile(writeSpecific("public func rootModule(): Int64 { 0 }")) {
            addCangJieSourceRoot(commonSource.toString(), isCommon = true)
            addCangJieSourceRoot(moduleOneSource.toString(), hmppModuleName = "nativeOne")
            addCangJieSourceRoot(moduleTwoSource.toString(), hmppModuleName = "nativeTwo")
            configureCjmpCommonPartInputs(listOf(commonCjo.toString()), emptyList())
            cjoOutputDirectory = outputDirectory.toString()
        }
        assertTrue(result.completed, "specific HMPP pipeline completes: ${result.messages}")
        assertTrue(result.diagnostics.none { it.severity.isError }, "specific diagnostics: ${result.diagnostics}")

        val specificCjo = outputDirectory.resolve(CjoConstants.packageNameToPath("cjmp_p"))
        val specificPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(specificCjo)))
        val exportedFunctions = (0 until specificPackage.allDeclsLength)
            .mapNotNull(specificPackage::allDecls)
            .filter { it.kind == DeclKind.FuncDecl }
        val exportedNames = exportedFunctions.map { it.identifier }.toSet()
        assertTrue(
            exportedNames.containsAll(setOf("rootModule", "moduleOne", "moduleTwo", "shared", "sourceShared")),
            "the package CJO contains roots produced by all HMPP sessions: $exportedNames",
        )
        val sharedDeclarations = exportedFunctions.filter { it.identifier == "shared" }
        assertEquals(1, sharedDeclarations.size, "one source CJO identity has one output declaration index")
        assertEquals(1, exportedFunctions.count { it.identifier == "sourceShared" }, "one common source identity has one output declaration")
        val sourceBoxes = (0 until specificPackage.allDeclsLength)
            .mapNotNull(specificPackage::allDecls)
            .filter { it.kind == DeclKind.ClassDecl && it.identifier == "SourceBox" }
        assertEquals(1, sourceBoxes.size, "the repeated common source class has one output declaration")
        val sourceBoxInfo = sourceBoxes.single().info(ClassInfo()) as ClassInfo
        val sourceBoxMembers = (0 until sourceBoxInfo.bodyLength)
            .map { specificPackage.allDecls(sourceBoxInfo.body(it).toInt() - 1)!! }
        assertEquals(1, sourceBoxMembers.count { it.identifier == "member" }, "the selected member graph is merged once")
    }

    @Test
    fun `platform checkers run for every independent HMPP source graph`() {
        for (useLightTree in listOf(false, true)) {
            val sourceRoot = Files.createDirectories(tempDir.resolve("hmpp-platform-$useLightTree"))
            val rootSource = sourceRoot.resolve("root.cj")
            Files.writeString(rootSource, "package cjmp_p\npublic class Alpha {}\n")

            val subpackageRoot = Files.createDirectories(sourceRoot.resolve("subpackage"))
            val subpackageSource = subpackageRoot.resolve("child.cj")
            Files.writeString(subpackageSource, "package cjmp_p.Alpha\npublic func child(): Int64 { 1 }\n")

            val hmppRoot = Files.createDirectories(sourceRoot.resolve("native"))
            val hmppSource = hmppRoot.resolve("native.cj")
            Files.writeString(hmppSource, "package hmpp_marker\npublic func marker(): Int64 { 1 }\n")

            val result = compile(rootSource) {
                this.useLightTree = useLightTree
                addCangJieSourceRoot(subpackageSource.toString())
                addCangJieSourceRoot(hmppSource.toString(), hmppModuleName = "native")
            }

            val diagnostics = result.diagnostics.filter { it.factoryName == "CFIR_CONFLICT_WITH_SUB_PACKAGE" }
            assertEquals(1, diagnostics.size, "Platform diagnostic is found in the root source graph; LightTree=$useLightTree")
        }
    }

    @Test
    fun `platform diagnostics from common sources are reported once across HMPP graphs`() {
        for (useLightTree in listOf(false, true)) {
            val sourceRoot = Files.createDirectories(tempDir.resolve("hmpp-shared-platform-$useLightTree"))
            val rootSource = sourceRoot.resolve("root.cj")
            Files.writeString(rootSource, "package cjmp_root\npublic func root(): Int64 { 1 }\n")

            val commonRoot = Files.createDirectories(sourceRoot.resolve("common"))
            val classifierSource = commonRoot.resolve("alpha.cj")
            Files.writeString(classifierSource, "package cjmp_p\npublic class Alpha {}\n")
            val subpackageRoot = Files.createDirectories(commonRoot.resolve("subpackage"))
            val subpackageSource = subpackageRoot.resolve("child.cj")
            Files.writeString(subpackageSource, "package cjmp_p.Alpha\npublic func child(): Int64 { 1 }\n")
            val overloadSource = commonRoot.resolve("overloads.cj")
            Files.writeString(
                overloadSource,
                "package hmpp_common\npublic func repeated(): Int64 { 1 }\npublic func repeated(): Int64 { 2 }\n",
            )

            val nativeOneRoot = Files.createDirectories(sourceRoot.resolve("native-one"))
            val nativeOneSource = nativeOneRoot.resolve("native-one.cj")
            Files.writeString(nativeOneSource, "package hmpp_one\npublic func one(): Int64 { 1 }\n")
            val nativeTwoRoot = Files.createDirectories(sourceRoot.resolve("native-two"))
            val nativeTwoSource = nativeTwoRoot.resolve("native-two.cj")
            Files.writeString(nativeTwoSource, "package hmpp_two\npublic func two(): Int64 { 2 }\n")

            val result = compile(rootSource) {
                this.useLightTree = useLightTree
                addCangJieSourceRoot(classifierSource.toString(), isCommon = true)
                addCangJieSourceRoot(subpackageSource.toString(), isCommon = true)
                addCangJieSourceRoot(overloadSource.toString(), isCommon = true)
                addCangJieSourceRoot(nativeOneSource.toString(), hmppModuleName = "nativeOne")
                addCangJieSourceRoot(nativeTwoSource.toString(), hmppModuleName = "nativeTwo")
            }

            val diagnostics = result.diagnostics.filter { it.factoryName == "CFIR_CONFLICT_WITH_SUB_PACKAGE" }
            assertEquals(
                1,
                diagnostics.size,
                "the same common-source diagnostic is reported once across root and named HMPP graphs; LightTree=$useLightTree",
            )
            val overloadDiagnostics = result.diagnostics.filter { it.factoryName == "CFIR_CONFLICTING_OVERLOADS" }
            assertEquals(
                1,
                overloadDiagnostics.size,
                "the same common-source overload diagnostic is reported once across HMPP graphs; LightTree=$useLightTree",
            )
            assertEquals(
                listOf("conflict with the declaration"),
                overloadDiagnostics.single().relatedInformation.map { it.message },
                "deduplicating the diagnostic must retain its peer declaration note",
            )
            assertTrue(
                overloadDiagnostics.single().relatedInformation.single().element != null,
                "the peer note must retain its source navigation element; LightTree=$useLightTree",
            )
        }
    }

    @Test
    fun `specific CJO preserves imported declaration identities and generic owners`() {
        val commonVersion = CjmpCommonPartLoadGate.supportedVersion
        val commonAttributes = cjmpAttributeWords(
            Attribute.COMMON,
            Attribute.FROM_COMMON_PART,
            Attribute.COMMON_WITH_DEFAULT,
        )
        val commonCjo = tempDir.resolve("generic-owner-common.cjo")
        CjoPackageWriter.write(
            commonCjo,
            CjoPackageMetadata(
                fullPackageName = "cjmp_p",
                moduleName = "cjmp_p",
                schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP,
                cjoVersion = CjoFormatVersion(commonVersion.major, commonVersion.minor, commonVersion.patch),
                options = CjoModuleOptionMetadata(),
                allFiles = listOf("common.cj"),
                declarations = listOf(
                    CjoPackageDeclaration(
                        identifier = "T",
                        kind = DeclKind.GenericParamDecl,
                        isTopLevel = false,
                        genericDecl = CjoAnnotationTargetMetadata(pkgId = PackageIndex.CURRENT.value, index = 2u),
                        type = 2u,
                    ),
                    CjoPackageDeclaration(
                        identifier = "GenericCalls",
                        kind = DeclKind.ClassDecl,
                        type = 1u,
                        attributes = cjmpAttributeWords(
                            Attribute.GLOBAL, Attribute.GENERIC, Attribute.COMMON, Attribute.FROM_COMMON_PART,
                            Attribute.COMMON_WITH_DEFAULT,
                        ),
                        generic = CjoGenericMetadata(typeParameters = listOf(1u)),
                        info = CjoClassInfo(body = listOf(3u)),
                    ),
                    CjoPackageDeclaration(
                        identifier = "identity",
                        kind = DeclKind.FuncDecl,
                        isTopLevel = false,
                        genericDecl = CjoAnnotationTargetMetadata(pkgId = PackageIndex.CURRENT.value, index = 2u),
                        exportId = "cjmp_p::GenericCalls.identity",
                        mangledName = "official_mangled_identity",
                        mangledBeforeSema = "official_raw_identity",
                        declarationHash = CjoDeclHashMetadata(1u, 2u, 3u, 4u, 5u),
                        type = 3u,
                        attributes = commonAttributes,
                        info = CjoFunctionInfo(
                            body = CjoFunctionBodyInfo(
                                parameterLists = listOf(listOf(4u)),
                                desugaredParameterLists = listOf(listOf(0u)),
                                returnType = 2u,
                            ),
                        ),
                    ),
                    CjoPackageDeclaration(
                        identifier = "value",
                        kind = DeclKind.FuncParam,
                        isTopLevel = false,
                        type = 2u,
                        info = CjoParameterInfo(isNamed = true, isMember = false),
                    ),
                ),
                types = listOf(
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Class,
                        semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = 2u),
                    ),
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Generic,
                        semanticInfo = CjoGenericTypeInfoMetadata(declarationIndex = 1u),
                    ),
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Func,
                        typeArguments = listOf(2u),
                        semanticInfo = CjoFunctionTypeInfoMetadata(
                            returnType = 2u,
                            isC = false,
                            hasVariableLenArg = false,
                        ),
                    ),
                ),
            ),
        )

        val output = Files.createDirectories(tempDir.resolve("generic-owner-specific"))
        val specific = compileSpecific(
            content = "public func consume(): Int64 { 0 }",
            commonCjo = commonCjo,
            outputDirectory = output,
        )
        assertTrue(specific.completed, "the specific pipeline re-exports the imported declarations: ${specific.messages}")
        assertFalse(specific.diagnostics.any { it.severity.isError }, "the synthetic common declarations load cleanly")

        val packageTable = Package.getRootAsPackage(
            ByteBuffer.wrap(Files.readAllBytes(output.resolve(CjoConstants.packageNameToPath("cjmp_p")))),
        )
        val declarations = (0 until packageTable.allDeclsLength).mapNotNull(packageTable::allDecls)
        val exportedClassIndex = declarations.indexOfFirst {
            it.kind == DeclKind.ClassDecl && it.identifier == "GenericCalls"
        }
        val exportedClass = declarations[exportedClassIndex]
        val exportedClassInfo = exportedClass.info(ClassInfo()) as ClassInfo
        val exportedMethod = (0 until exportedClassInfo.bodyLength)
            .map { packageTable.allDecls(exportedClassInfo.body(it).toInt() - 1)!! }
            .single { it.identifier == "identity" }

        assertEquals("cjmp_p::GenericCalls.identity", exportedMethod.exportId)
        assertEquals("official_mangled_identity", exportedMethod.mangledName)
        assertEquals("official_raw_identity", exportedMethod.mangledBeforeSema)
        val hash = checkNotNull(exportedMethod.hash)
        assertEquals(listOf(1uL, 2uL, 3uL, 4uL, 5uL), listOf(hash.instVar, hash.virt, hash.sig, hash.srcUse, hash.bodyHash))
        assertEquals(PackageIndex.CURRENT.value, exportedMethod.genericDecl?.pkgId)
        assertEquals(
            exportedClassIndex.toUInt() + 1u,
            exportedMethod.genericDecl?.index,
            "the re-exported method owner points to the class in the new package",
        )
    }

    @Test
    fun `specific CJO remaps imported block locals through a generic owner counterpart`() {
        val commonVersion = CjmpCommonPartLoadGate.supportedVersion
        val commonAttributes = cjmpAttributeWords(
            Attribute.COMMON,
            Attribute.FROM_COMMON_PART,
            Attribute.COMMON_WITH_DEFAULT,
        )
        val commonCjo = tempDir.resolve("generic-block-common.cjo")
        CjoPackageWriter.write(
            commonCjo,
            CjoPackageMetadata(
                fullPackageName = "cjmp_p",
                moduleName = "cjmp_p",
                schemaProfile = CjoSchemaProfile.OFFICIAL_CJMP,
                cjoVersion = CjoFormatVersion(commonVersion.major, commonVersion.minor, commonVersion.patch),
                options = CjoModuleOptionMetadata(),
                allFiles = listOf("common.cj"),
                imports = listOf("std.core"),
                declarations = listOf(
                    CjoPackageDeclaration(
                        identifier = "T",
                        kind = DeclKind.GenericParamDecl,
                        isTopLevel = false,
                        genericDecl = CjoAnnotationTargetMetadata(pkgId = PackageIndex.CURRENT.value, index = 2u),
                        type = 2u,
                    ),
                    CjoPackageDeclaration(
                        identifier = "GenericCalls",
                        kind = DeclKind.ClassDecl,
                        type = 1u,
                        attributes = cjmpAttributeWords(
                            Attribute.GLOBAL,
                            Attribute.GENERIC,
                            Attribute.COMMON,
                            Attribute.FROM_COMMON_PART,
                            Attribute.COMMON_WITH_DEFAULT,
                        ),
                        generic = CjoGenericMetadata(typeParameters = listOf(1u)),
                        // 官方 PreCheck 为无显式父类的 GenericCalls 保存同一 core Object。
                        info = CjoClassInfo(inheritedTypes = listOf(6u), body = listOf(3u, 4u)),
                    ),
                    CjoPackageDeclaration(
                        identifier = SpecialNames.INIT.asString(),
                        kind = DeclKind.FuncDecl,
                        isTopLevel = false,
                        genericDecl = CjoAnnotationTargetMetadata(pkgId = PackageIndex.CURRENT.value, index = 2u),
                        type = 3u,
                        attributes = cjmpAttributeWords(
                            Attribute.CONSTRUCTOR,
                            Attribute.COMMON,
                            Attribute.FROM_COMMON_PART,
                        ),
                        info = CjoFunctionInfo(hasFunctionBody = false),
                    ),
                    CjoPackageDeclaration(
                        identifier = "identity",
                        kind = DeclKind.FuncDecl,
                        isTopLevel = false,
                        genericDecl = CjoAnnotationTargetMetadata(pkgId = PackageIndex.CURRENT.value, index = 2u),
                        type = 5u,
                        attributes = commonAttributes,
                        // @Frozen 使含真实局部 var 的原正文在 specific CJO 中符合官方保存资格。
                        annotations = listOf(CjoAnnotationMetadata(kind = AnnoKind.Frozen, identifier = "Frozen")),
                        info = CjoFunctionInfo(
                            body = CjoFunctionBodyInfo(
                                parameterLists = listOf(listOf(5u)),
                                desugaredParameterLists = listOf(listOf(0u)),
                                returnType = 2u,
                                body = 3u,
                            ),
                        ),
                    ),
                    CjoPackageDeclaration(
                        identifier = "candidate",
                        kind = DeclKind.FuncParam,
                        isTopLevel = false,
                        genericDecl = CjoAnnotationTargetMetadata(pkgId = PackageIndex.CURRENT.value, index = 4u),
                        type = 2u,
                        info = CjoParameterInfo(isNamed = true, isMember = false),
                    ),
                    CjoPackageDeclaration(
                        identifier = "local",
                        kind = DeclKind.VarDecl,
                        isTopLevel = false,
                        type = 2u,
                        info = CjoVariableInfo(isVar = true, initializer = 1u),
                    ),
                ),
                types = listOf(
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Class,
                        semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = 2u),
                    ),
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Generic,
                        semanticInfo = CjoGenericTypeInfoMetadata(declarationIndex = 1u),
                    ),
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Func,
                        semanticInfo = CjoFunctionTypeInfoMetadata(
                            returnType = 4u,
                            isC = false,
                            hasVariableLenArg = false,
                        ),
                    ),
                    CjoTypeMetadata(kind = PackageFormat.TypeKind.Unit),
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Func,
                        typeArguments = listOf(2u),
                        semanticInfo = CjoFunctionTypeInfoMetadata(
                            returnType = 2u,
                            isC = false,
                            hasVariableLenArg = false,
                        ),
                    ),
                    CjoTypeMetadata(
                        kind = PackageFormat.TypeKind.Class,
                        semanticInfo = CjoCompositeTypeInfoMetadata(
                            declarationIndex = 0u, packageId = 0, declarationKey = "_CNat6ObjectE",
                        ),
                    ),
                ),
                expressions = listOf(
                    CjoExpressionMetadata(
                        kind = PackageFormat.ExprKind.RefExpr,
                        type = 2u,
                        reference = CjoReferenceExpressionMetadata(
                            reference = "candidate",
                            target = CjoExpressionTargetMetadata(
                                pkgId = PackageIndex.CURRENT.value,
                                index = 5u,
                            ),
                        ),
                    ),
                    CjoExpressionMetadata(
                        kind = PackageFormat.ExprKind.RefExpr,
                        type = 2u,
                        reference = CjoReferenceExpressionMetadata(
                            reference = "local",
                            target = CjoExpressionTargetMetadata(
                                pkgId = PackageIndex.CURRENT.value,
                                index = 6u,
                            ),
                        ),
                    ),
                    CjoExpressionMetadata(
                        kind = PackageFormat.ExprKind.Block,
                        type = 2u,
                        operands = listOf(6u, 2u),
                        additionalInfo = CjoAdditionalExpressionInfoMetadata.Block(
                            expressions = listOf(false, true, false),
                        ),
                    ),
                ),
            ),
        )

        val output = Files.createDirectories(tempDir.resolve("generic-block-specific"))
        val specific = compileSpecific(
            content = """
                public specific class GenericCalls<U> {
                    public specific init() {}
                }
            """.trimIndent(),
            commonCjo = commonCjo,
            outputDirectory = output,
        )
        assertTrue(
            specific.completed,
            "the specific pipeline re-exports the imported body graph: ${specific.messages}; ${specific.diagnostics}",
        )
        assertFalse(specific.diagnostics.any { it.severity.isError }, "the specific generic owner loads cleanly")

        val packageTable = Package.getRootAsPackage(
            ByteBuffer.wrap(Files.readAllBytes(output.resolve(CjoConstants.packageNameToPath("cjmp_p")))),
        )
        val declarations = (0 until packageTable.allDeclsLength).mapNotNull(packageTable::allDecls)
        val exportedOwner = declarations.single {
            it.kind == DeclKind.ClassDecl && it.identifier == "GenericCalls"
        }
        val ownerInfo = exportedOwner.info(ClassInfo()) as ClassInfo
        val ownerGeneric = checkNotNull(exportedOwner.generic)
        val ownerTypeParameter = checkNotNull(
            packageTable.allDecls(ownerGeneric.typeParameters(0).toInt() - 1),
        )
        assertEquals("U", ownerTypeParameter.identifier, "the specific owner parameter replaces common T")
        assertTrue(declarations.none { it.kind == DeclKind.GenericParamDecl && it.identifier == "T" })

        val exportedIdentity = (0 until ownerInfo.bodyLength)
            .map { packageTable.allDecls(ownerInfo.body(it).toInt() - 1)!! }
            .single { it.identifier == "identity" }
        assertCjoFunctionParameterUsesOwnerGeneric(packageTable, exportedOwner, exportedIdentity)
        val functionInfo = exportedIdentity.info(PackageFormat.FuncInfo()) as PackageFormat.FuncInfo
        val functionBody = checkNotNull(functionInfo.funcBody)
        val exportedParameterIndex = functionBody.paramLists(0)!!.params(0)
        val block = checkNotNull(packageTable.allExprs(functionBody.body.toInt() - 1))
        assertEquals(PackageFormat.ExprKind.Block, block.kind)
        assertEquals(2, block.operandsLength)
        val blockInfo = block.info(PackageFormat.BlockInfo()) as PackageFormat.BlockInfo
        assertFalse(blockInfo.isExpr(0), "the first block operand remains a declaration")
        assertTrue(blockInfo.isExpr(1), "the second block operand remains an expression")

        val exportedLocalIndex = block.operands(0).toInt() - 1
        val exportedLocal = checkNotNull(packageTable.allDecls(exportedLocalIndex))
        assertEquals("local", exportedLocal.identifier)
        val localType = checkNotNull(packageTable.allTypes(exportedLocal.type.toInt() - 1))
        val localTypeInfo = localType.info(PackageFormat.GenericTyInfo()) as PackageFormat.GenericTyInfo
        assertEquals(ownerGeneric.typeParameters(0), localTypeInfo.declPtr!!.index)

        val localInfo = exportedLocal.info(VarInfo()) as VarInfo
        val initializer = checkNotNull(packageTable.allExprs(localInfo.initializer.toInt() - 1))
        assertEquals(PackageFormat.ExprKind.RefExpr, initializer.kind)
        val initializerReference = initializer.info(PackageFormat.ReferenceInfo()) as PackageFormat.ReferenceInfo
        assertEquals(PackageIndex.CURRENT.value, initializerReference.target?.pkgId)
        assertEquals(exportedParameterIndex, initializerReference.target?.index)

        val returnedReference = packageTable.allExprs(block.operands(1).toInt() - 1)!!
        val returnedReferenceInfo = returnedReference.info(PackageFormat.ReferenceInfo()) as PackageFormat.ReferenceInfo
        assertEquals(PackageIndex.CURRENT.value, returnedReferenceInfo.target?.pkgId)
        assertEquals(exportedLocalIndex.toUInt() + 1u, returnedReferenceInfo.target?.index)
    }

    /** 宏产物语法错误经生产管线报告原始消息，两种解析入口均不能完成或写出 CJO。 */
    @Test
    fun `macro generated common syntax is rejected by CLI without CFIR diagnostics or CJO`() {
        assertMacroGeneratedSyntaxFailure(MacroGeneratedSyntaxCase.FILE_ROLE)
    }

    /** 普通生成语法错误归属完整宏调用，两端位置均须保留，不能误投影为文件起点。 */
    @Test
    fun `macro generated input syntax reports the complete call range in both frontend modes`() {
        assertMacroGeneratedSyntaxFailure(MacroGeneratedSyntaxCase.RETURN_TYPE)
    }

    /** 官方 cjc 1.1.3 unknown-unicode.cj：LEX POINT；生成 token 的输入归属必须映射完整宏调用。 */
    @Test
    fun `macro generated lexical errors retain input origin and report the complete call range`() {
        assertMacroGeneratedSyntaxFailure(MacroGeneratedSyntaxCase.LEXICAL_INPUT)
    }

    private enum class MacroGeneratedSyntaxCase { FILE_ROLE, RETURN_TYPE, LEXICAL_INPUT }

    private fun assertMacroGeneratedSyntaxFailure(case: MacroGeneratedSyntaxCase) {
        val originalFileError = case == MacroGeneratedSyntaxCase.FILE_ROLE
        val macroCjo = tempDir.resolve("macros.cjo")
        CjoPackageWriter.write(macroCjo, CjoPackageMetadata(
            fullPackageName = "macros",
            moduleName = "macros",
            kind = PackageFormat.PackageKind.Macro,
            declarations = listOf(CjoPackageDeclaration("GenerateDeclaration")),
        ))
        val macroLibrary = tempDir.resolve("macros.dll")
        Files.write(macroLibrary, byteArrayOf(1))
        val generated = when (case) {
            MacroGeneratedSyntaxCase.FILE_ROLE -> "common func generated(): Unit"
            MacroGeneratedSyntaxCase.RETURN_TYPE -> "common func generated()"
            MacroGeneratedSyntaxCase.LEXICAL_INPUT -> "§"
        }
        val expected = when (case) {
            MacroGeneratedSyntaxCase.FILE_ROLE -> CangJieParsingBundle.message("parsing.error.cjmp.common.in.non.common.file")
            MacroGeneratedSyntaxCase.RETURN_TYPE -> CangJieParsingBundle.message("parsing.error.cjmp.function.return.type.required", "common")
            MacroGeneratedSyntaxCase.LEXICAL_INPUT -> "unknown start of token: §"
        }
        for (lightTree in listOf(false, true)) {
            val scenarios = org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService.Mode.entries.flatMap { mode ->
                listOf("GenerateDeclaration", "macros.GenerateDeclaration").flatMap { callName ->
                    listOf("\n", "\r\n").map { lineEnding -> Triple(mode, callName, lineEnding) }
                }
            }
            for ((mode, callName, lineEnding) in scenarios) {
                val source = writeCommon("import macros.*\n@$callName func original(): Unit {}", lineEnding = lineEnding)
                val outputDirectory = Files.createDirectories(tempDir.resolve("macro-syntax-$lightTree-$mode-$callName-${lineEnding.length}"))
                var executionCount = 0
                val result = compile(source) {
                    useLightTree = lightTree
                    cjmpMode = if (originalFileError) CfirCjmpMode.NONE else CfirCjmpMode.COMMON
                    cjoOutputDirectory = outputDirectory.toString()
                    macroConstructionMode = mode
                    macroArtifactPackages = listOf(MacroArtifactPackage(
                        packageFqName = org.cangnova.cangjie.name.FqName("macros"),
                        kind = MacroArtifactPackage.Kind.MACRO,
                        cjoPath = macroCjo.toString(),
                        dynamicLibPath = macroLibrary.toString(),
                        origin = MacroArtifactPackage.Origin.EXTERNAL_PATH,
                    ))
                    macroExecutorFactory = MacroExecutorFactory {
                        object : org.cangnova.cangjie.macro.MacroExecutor {
                            override fun loadLibraries(libPaths: List<String>) =
                                org.cangnova.cangjie.macro.MacroLibraryLoadResult.Success(libPaths)
                            override fun execute(calls: List<org.cangnova.cangjie.macro.MacroCallInfo>): List<org.cangnova.cangjie.macro.MacroExpansionResult> {
                                executionCount += calls.size
                                return calls.map {
                                    org.cangnova.cangjie.macro.MacroExpansionResult.Success(
                                        listOf(org.cangnova.cangjie.macro.TokenInfo(0u.toUByte(), generated)), generated,
                                    )
                                }
                            }
                            override fun reset() {}
                            override fun isAvailable(): Boolean = true
                            override fun close() {}
                        }
                    }
                }
                assertEquals(1, executionCount, "$lightTree/$mode/$callName: ${result.messages}; ${result.diagnostics.map { it.factoryName }}")
                assertFalse(result.completed, "$lightTree/$mode: ${result.messages}")
                assertTrue(result.diagnostics.isEmpty(), "${result.diagnostics}")
                if (case == MacroGeneratedSyntaxCase.LEXICAL_INPUT) {
                    assertEquals(listOf(expected), result.messages.filter { it.first.isError }.map { it.second })
                }
                val messageIndex = result.messages.indexOfFirst { it.first.isError && it.second == expected }
                assertTrue(messageIndex >= 0, "$lightTree/$mode: ${result.messages}")
                val location = requireNotNull(result.locations[messageIndex])
                assertEquals(source.toString().replace('\\', '/'), location.path.replace('\\', '/'))
                if (originalFileError) {
                    assertEquals(1, location.line)
                    assertEquals(1, location.column)
                    assertEquals(1, location.lineEnd)
                    assertEquals(2, location.columnEnd)
                } else {
                    val sourceText = Files.readString(source)
                    val callText = "@$callName func original(): Unit {}"
                    val callOffset = sourceText.indexOf(callText)
                    assertTrue(callOffset >= 0)
                    val callLine = sourceText.take(callOffset).count { it == '\n' } + 1
                    assertEquals(callLine, location.line)
                    assertEquals(1, location.column)
                    assertEquals(callLine, location.lineEnd)
                    assertEquals(callText.length + 1, location.columnEnd)
                }
                Files.list(outputDirectory).use { assertEquals(0, it.count()) }
            }
        }
    }

    /** parser 语法错误必须中止 CLI，不能靠 CFIR 的 parse 工厂或成功写出 CJO。 */
    @Test
    fun `bodyless common missing return type is rejected by parser in both frontend modes`() {
        for (lightTree in listOf(true, false)) {
            val source = writeCommon("public common func missing()")
            val outDir = Files.createDirectories(tempDir.resolve("syntax-$lightTree"))
            val result = compile(source) {
                useLightTree = lightTree
                cjmpChirOutput = true
                cjoOutputDirectory = outDir.toString()
            }
            val expectedMessage = CangJieParsingBundle.message("parsing.error.cjmp.function.return.type.required", "common")
            assertFalse(result.completed, "syntax errors must stop frontend: ${result.messages}")
            assertEquals(emptyList<String>(), result.diagnostics.map { it.factoryName })
            val messageIndex = result.messages.indexOfFirst { it.first.isError && it.second == expectedMessage }
            assertTrue(messageIndex >= 0, "parser error must reach CLI: ${result.messages}")
            val location = requireNotNull(result.locations[messageIndex])
            assertEquals(source.toString().replace('\\', '/'), location.path.replace('\\', '/'))
            val sourceText = Files.readString(source)
            val keywordOffset = sourceText.indexOf("func")
            assertEquals(sourceText.take(keywordOffset).count { it == '\n' } + 1, location.line)
            assertEquals(keywordOffset - sourceText.lastIndexOf('\n', keywordOffset), location.column)
            assertFalse(Files.exists(outDir.resolve(CjoConstants.packageNameToPath("cjmp_p"))))
        }
    }

    /** common 模式错误必须在 CLI 的 parsing 阶段终止，位置属于模式根而不是绑定名。 */
    @Test
    fun `common patterns are rejected by parsing without CFIR diagnostics or CJO`() {
        for (lightTree in listOf(true, false)) for (lineEnding in listOf("\n", "\r\n")) {
            for ((pattern, kind) in listOf("(a, b)" to "tuple", "Some(x)" to "enum", "_" to "wildcard")) {
                val source = writeCommon("public common let $pattern = value", lineEnding)
                val outDir = Files.createDirectories(tempDir.resolve("pattern-$lightTree-${lineEnding.length}-$kind"))
                val result = compile(source) {
                    useLightTree = lightTree
                    cjmpChirOutput = true
                    cjoOutputDirectory = outDir.toString()
                }
                val expected = CangJieParsingBundle.message("parsing.error.cjmp.pattern.declaration", kind, "common")
                assertFalse(result.completed, "${result.messages}")
                assertTrue(result.diagnostics.isEmpty(), "${result.diagnostics}")
                val messageIndex = result.messages.indexOfFirst { it.first.isError && it.second == expected }
                assertTrue(messageIndex >= 0, "${result.messages}")
                val location = requireNotNull(result.locations[messageIndex])
                assertEquals(source.toString().replace('\\', '/'), location.path.replace('\\', '/'))
                val sourceText = Files.readString(source)
                val offset = sourceText.indexOf(pattern)
                assertEquals(sourceText.take(offset).count { it == '\n' } + 1, location.line)
                assertEquals(offset - sourceText.lastIndexOf('\n', offset), location.column)
                assertFalse(Files.exists(outDir.resolve(CjoConstants.packageNameToPath("cjmp_p"))))
            }
        }
    }

    /** 错位角色必须在解析时拒绝，两种入口都不能产生 CFIR 语法工厂或写出 CJO。 */
    @Test
    fun `file role errors stop both frontend modes before semantic checks`() {
        for (lightTree in listOf(true, false)) {
            for ((mode, role) in listOf(
                CfirCjmpMode.NONE to "common",
                CfirCjmpMode.NONE to "specific",
                CfirCjmpMode.COMMON to "specific",
                CfirCjmpMode.SPECIFIC to "common",
            )) {
                val source = writeCommon("public $role func misplaced(): Unit {}")
                val outDir = Files.createDirectories(tempDir.resolve("role-$lightTree-$mode-$role"))
                val result = compile(source) {
                    useLightTree = lightTree
                    cjmpMode = mode
                    cjoOutputDirectory = outDir.toString()
                }
                val message = CangJieParsingBundle.message("parsing.error.cjmp.$role.in.non.$role.file")
                assertFalse(result.completed, "file role error must stop frontend: ${result.messages}")
                assertTrue(result.diagnostics.isEmpty(), "syntax errors cannot be CFIR factories")
                val errorIndices = result.messages.indices.filter { result.messages[it].first.isError }
                assertEquals(listOf(message), errorIndices.map { result.messages[it].second })
                val location = requireNotNull(result.locations[errorIndices.single()])
                assertEquals(1, location.line)
                assertEquals(1, location.column)
                assertFalse(Files.exists(outDir.resolve(CjoConstants.packageNameToPath("cjmp_p"))))
            }
        }
    }

    /** 配置 owner 的精确 alpha 身份必须进入两种生产 parser，并选择对应的原文件错误。 */
    @Test
    fun `exact alpha version selects native file role diagnostic in both frontend modes`() {
        for (lightTree in listOf(false, true)) for (version in listOf("1.2.0-alpha.19", "1.2.0-alpha.20")) {
            for (role in listOf("common", "specific")) {
                val source = writeCommon("public $role func misplaced(): Unit {}")
                val output = Files.createDirectories(tempDir.resolve("alpha-role-$lightTree-$version-$role"))
                val result = compile(source) {
                    useLightTree = lightTree
                    assertTrue(configureLanguageVersionSettings(version))
                    cjmpMode = CfirCjmpMode.NONE
                    cjoOutputDirectory = output.toString()
                }
                val key = if (version.endsWith(".20")) "parsing.error.cjmp.unexpected.declaration"
                    else "parsing.error.cjmp.$role.in.non.$role.file"
                val errorIndices = result.messages.indices.filter { result.messages[it].first.isError }
                assertEquals(listOf(CangJieParsingBundle.message(key)), errorIndices.map { result.messages[it].second })
                assertFalse(result.completed)
                assertTrue(result.diagnostics.isEmpty())
                val location = requireNotNull(result.locations[errorIndices.single()])
                assertEquals(source.toString().replace('\\', '/'), location.path.replace('\\', '/'))
                assertEquals(1, location.line)
                assertEquals(1, location.column)
                assertEquals(1, location.lineEnd)
                assertEquals(2, location.columnEnd)
                Files.list(output).use { assertEquals(0, it.count()) }
            }
        }
    }

    /** alpha.65 的默认值错误归 parser；alpha.66 起由配对 evaluator 报告双方默认值冲突及未配对结果。 */
    @Test
    fun `platform parameter default diagnostic moves from parser to matcher at the version boundary`() {
        for (lightTree in listOf(false, true)) {
            for (version in listOf("1.1.0-alpha.65", "1.1.0-alpha.66")) {
                val commonOutput = Files.createDirectories(tempDir.resolve("default-gate-common-$lightTree-$version"))
                val commonResult = compile(
                    writeCommon("public common func value(x!: Int64 = 1): Int64 { x }"),
                ) {
                    useLightTree = lightTree
                    assertTrue(configureLanguageVersionSettings(version))
                    cjmpChirOutput = true
                    cjoOutputDirectory = commonOutput.toString()
                }
                assertTrue(commonResult.completed, "$lightTree/$version common: ${commonResult.messages}")
                assertTrue(
                    commonResult.diagnostics.none { it.severity.isError },
                    "$lightTree/$version common diagnostics: ${commonResult.diagnostics}",
                )
                val commonCjo = commonOutput.resolve(CjoConstants.packageNameToPath("cjmp_p"))
                assertTrue(Files.isRegularFile(commonCjo), "$lightTree/$version common CJO: $commonCjo")

                val specificSource = writeSpecific(
                    "public platform func value(x!: Int64 = 2): Int64 { x }",
                )
                val result = compile(specificSource) {
                    useLightTree = lightTree
                    assertTrue(configureLanguageVersionSettings(version))
                    configureCjmpCommonPartInputs(listOf(commonCjo.toString()), emptyList())
                }
                val parserMessage = CangJieParsingBundle.message(
                    "parsing.error.cjmp.platform.parameter.default.unsupported",
                    "function",
                )
                if (version == "1.1.0-alpha.65") {
                    assertFalse(result.completed, "$lightTree/$version must stop at parser: ${result.messages}")
                    assertTrue(result.diagnostics.isEmpty(), "$lightTree/$version must not emit a CFIR diagnostic")
                    assertTrue(
                        result.messages.any { it.first.isError && it.second == parserMessage },
                        "$lightTree/$version parser diagnostic: ${result.messages}",
                    )
                    assertFalse(
                        result.diagnostics.any { it.factoryName == "CFIR_CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES" },
                    )
                } else {
                    assertTrue(
                        result.messages.none { it.first.isError && it.second == parserMessage },
                        "$lightTree/$version must not report the historical parser diagnostic: ${result.messages}",
                    )
                    assertEquals(
                        listOf("CFIR_NOT_MATCHED", "CFIR_CJMP_PARAMETER_DEFAULT_VALUE_BOTH_SIDES"),
                        result.diagnostics.map { it.factoryName },
                        "$lightTree/$version semantic diagnostic",
                    )
                }
            }
        }
    }

    /** alpha.65 的 common 泛型声明在 parser 停止；alpha.66 经真实版本设置进入两段式匹配。 */
    @Test
    fun `generic common and platform declarations follow the alpha 66 parser boundary`() {
        val genericCommon = "public common func identity<T>(value: T): T { value }"
        val genericSpecific = "public platform func identity<U>(value: U): U { value }"
        val oldVersionParserMessage = CangJieParsingBundle.message(
            "parsing.error.cjmp.generic.declaration.unsupported",
            "common",
        )

        for (lightTree in listOf(false, true)) {
            val alpha65Output = Files.createDirectories(tempDir.resolve("generic-alpha65-$lightTree"))
            val alpha65 = compile(writeCommon(genericCommon)) {
                useLightTree = lightTree
                assertTrue(configureLanguageVersionSettings("1.1.0-alpha.65"))
                cjmpChirOutput = true
                cjoOutputDirectory = alpha65Output.toString()
            }

            assertFalse(alpha65.completed, "alpha.65/$lightTree must stop at the parser: ${alpha65.messages}")
            assertTrue(alpha65.diagnostics.isEmpty(), "alpha.65/$lightTree must not reach CFIR: ${alpha65.diagnostics}")
            assertEquals(
                listOf(oldVersionParserMessage),
                alpha65.messages.filter { it.first.isError }.map { it.second },
                "alpha.65/$lightTree parser diagnostic",
            )
            assertFalse(
                Files.exists(alpha65Output.resolve(CjoConstants.packageNameToPath("cjmp_p"))),
                "alpha.65/$lightTree cannot publish a common CJO after the syntax error",
            )

            val alpha66CommonCjo = compileCommon(
                genericCommon,
                languageVersion = "1.1.0-alpha.66",
                useLightTree = lightTree,
                outputDirectoryName = "generic-alpha66-common-$lightTree",
            )
            val alpha66Specific = compileSpecific(
                genericSpecific,
                alpha66CommonCjo,
                languageVersion = "1.1.0-alpha.66",
                useLightTree = lightTree,
            )

            assertTrue(alpha66Specific.completed, "alpha.66/$lightTree two-phase compile: ${alpha66Specific.messages}")
            assertTrue(
                alpha66Specific.diagnostics.none { it.severity.isError },
                "alpha.66/$lightTree matching diagnostics: ${alpha66Specific.diagnostics}",
            )
            assertEquals(
                1,
                alpha66Specific.cjmpCallableCounterpartCounts["identity"],
                "alpha.66/$lightTree keeps the generic common/platform pair",
            )
        }
    }

    /** 配对后推断返回类型复核只在本地首次确认的 alpha.68 门启用。 */
    @Test
    fun `inferred return compatibility follows the alpha 68 post match boundary`() {
        val commonSource = """
            public common func inferredSame() { 1 }
            public common func inferredMismatch() { 1 }
        """.trimIndent()
        val specificSource = """
            public platform func inferredSame() { 1 }
            public platform func inferredMismatch() { "wrong" }
        """.trimIndent()

        for (lightTree in listOf(false, true)) {
            for (version in listOf("1.1.0-alpha.66", "1.1.0-alpha.68")) {
                val commonCjo = compileCommon(
                    commonSource,
                    languageVersion = version,
                    useLightTree = lightTree,
                    outputDirectoryName = "inferred-return-$version-common-$lightTree",
                )
                val specific = compileSpecific(
                    specificSource,
                    commonCjo,
                    languageVersion = version,
                    useLightTree = lightTree,
                )
                val factories = specific.diagnostics.map { it.factoryName }

                assertEquals(
                    1,
                    specific.cjmpCallableCounterpartCounts["inferredSame"],
                    "$version/$lightTree keeps the compatible inferred-return pair",
                )
                if (version == "1.1.0-alpha.66") {
                    assertEquals(
                        0,
                        specific.cjmpCallableCounterpartCounts["inferredMismatch"],
                        "alpha.66/$lightTree compares the inferred return during ordinary matching",
                    )
                    assertTrue("CFIR_NOT_MATCHED" in factories, "alpha.66/$lightTree diagnostics: $factories")
                    assertFalse("CFIR_RETURN_TYPE_INCOMPATIBLE" in factories, "alpha.66/$lightTree diagnostics: $factories")
                } else {
                    assertEquals(
                        1,
                        specific.cjmpCallableCounterpartCounts["inferredMismatch"],
                        "alpha.68/$lightTree retains the pair for its post-match return check",
                    )
                    assertTrue(
                        "CFIR_RETURN_TYPE_INCOMPATIBLE" in factories,
                        "alpha.68/$lightTree post-match diagnostic: $factories",
                    )
                    assertFalse("CFIR_NOT_MATCHED" in factories, "alpha.68/$lightTree diagnostics: $factories")
                }
            }
        }
    }

    /** alpha.66 全局变量在初始化类型推断后比较类型；类型诊断不取消官方仍保留的实现关系。 */
    @Test
    fun `alpha 66 inferred global variable mismatch keeps its counterpart`() {
        val commonSource = "public common let inferredGlobal = 1"
        val specificSource = "public platform let inferredGlobal = \"wrong\""

        for (lightTree in listOf(false, true)) {
            val commonCjo = compileCommon(
                commonSource,
                languageVersion = "1.1.0-alpha.66",
                useLightTree = lightTree,
                outputDirectoryName = "inferred-global-alpha66-common-$lightTree",
            )
            val commonPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(commonCjo)))
            val commonDeclarations = (0 until commonPackage.allDeclsLength).mapNotNull(commonPackage::allDecls)
            val exportedGlobal = commonDeclarations.single { it.identifier == "inferredGlobal" }
            assertEquals(DeclKind.VarDecl, exportedGlobal.kind)
            assertTrue(exportedGlobal.isTopLevel)
            assertNotNull(exportedGlobal.info(VarInfo()) as? VarInfo)
            assertFalse(commonDeclarations.any { it.identifier == "<pattern-variable>" })
            val specific = compileSpecific(
                specificSource,
                commonCjo,
                languageVersion = "1.1.0-alpha.66",
                useLightTree = lightTree,
            )
            val factories = specific.diagnostics.map { it.factoryName }

            assertEquals(
                1,
                specific.cjmpCallableCounterpartCounts["inferredGlobal"],
                "alpha.66/$lightTree retains the global declaration relationship after type mismatch; diagnostics=$factories",
            )
            assertTrue(
                "CFIR_SPECIFIC_HAS_DIFFERENT_TYPE" in factories,
                "alpha.66/$lightTree reports the inferred global type mismatch: $factories",
            )
            assertFalse("CFIR_NOT_MATCHED" in factories, "alpha.66/$lightTree diagnostics: $factories")
        }
    }

    /** CJO 包级 pattern 按官方 ASTWriter 规则导出真实 binding VarDecl，而非容器声明。 */
    @Test
    fun `specific tuple pattern round trips each package binding`() {
        for (lightTree in listOf(false, true)) {
            val commonCjo = compileCommon(
                "public common let a: Int64 = 1\n" +
                    "public common let b: Int64 = 2\n" +
                    "public common let copied: Int64 = a",
                languageVersion = "1.1.0-alpha.66",
                useLightTree = lightTree,
                outputDirectoryName = "tuple-pattern-alpha66-common-$lightTree",
            )
            val commonPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(commonCjo)))
            val indexedCommonDeclarations = (0 until commonPackage.allDeclsLength)
                .map { index -> index to checkNotNull(commonPackage.allDecls(index)) }
            val commonBindings = indexedCommonDeclarations
                .map { (_, declaration) -> declaration }
                .filter { it.identifier in setOf("a", "b", "copied") }
            assertEquals(setOf("a", "b", "copied"), commonBindings.map { it.identifier }.toSet())
            assertTrue(commonBindings.all { it.kind == DeclKind.VarDecl && it.isTopLevel })
            val commonAIndex = indexedCommonDeclarations
                .single { (_, declaration) -> declaration.identifier == "a" }
                .first
            val commonCopied = indexedCommonDeclarations
                .single { (_, declaration) -> declaration.identifier == "copied" }
                .second
            val commonCopiedInitializer = commonPackage.allExprs(
                (commonCopied.info(VarInfo()) as VarInfo).initializer.toInt() - 1,
            )!!
            assertEquals(PackageFormat.ExprKind.RefExpr, commonCopiedInitializer.kind)
            val commonCopiedReference = commonCopiedInitializer.info(
                PackageFormat.ReferenceInfo(),
            ) as PackageFormat.ReferenceInfo
            assertEquals(PackageIndex.CURRENT.value, commonCopiedReference.target?.pkgId)
            assertEquals(commonAIndex.toUInt() + 1u, commonCopiedReference.target?.index)

            val outputDirectory = Files.createDirectories(tempDir.resolve("tuple-pattern-alpha66-specific-$lightTree"))
            val specific = compileSpecific(
                "public platform let (a, b) = (1, 2)",
                commonCjo,
                languageVersion = "1.1.0-alpha.66",
                outputDirectory = outputDirectory,
                useLightTree = lightTree,
            )
            assertTrue(specific.completed, "alpha.66/$lightTree tuple pattern completes: ${specific.messages}")
            assertTrue(
                specific.diagnostics.none { it.severity.isError },
                "alpha.66/$lightTree tuple bindings pair without diagnostics: ${specific.diagnostics}",
            )
            assertEquals(1, specific.cjmpCallableCounterpartCounts["a"])
            assertEquals(1, specific.cjmpCallableCounterpartCounts["b"])

            val specificCjo = outputDirectory.resolve(CjoConstants.packageNameToPath("cjmp_p"))
            val specificPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(specificCjo)))
            val indexedSpecificDeclarations = (0 until specificPackage.allDeclsLength)
                .map { index -> index to checkNotNull(specificPackage.allDecls(index)) }
            val specificBindings = indexedSpecificDeclarations
                .map { (_, declaration) -> declaration }
                .filter { it.identifier == "a" || it.identifier == "b" }
            assertEquals(setOf("a", "b"), specificBindings.map { it.identifier }.toSet())
            assertTrue(specificBindings.all { it.kind == DeclKind.VarDecl && it.isTopLevel })
            assertFalse(
                (0 until specificPackage.allDeclsLength).mapNotNull(specificPackage::allDecls)
                    .any { it.identifier == "<pattern-variable>" },
            )
            val (exportedAIndex, exportedA) = indexedSpecificDeclarations
                .single { (_, declaration) -> declaration.identifier == "a" }
            assertEquals(1, indexedSpecificDeclarations.count { (_, declaration) -> declaration.identifier == "a" })
            assertTrue(hasCjoAttribute(exportedA, Attribute.SPECIFIC))
            val exportedCopied = indexedSpecificDeclarations
                .single { (_, declaration) -> declaration.identifier == "copied" }
                .second
            val copiedInitializer = specificPackage.allExprs(
                (exportedCopied.info(VarInfo()) as VarInfo).initializer.toInt() - 1,
            )!!
            assertEquals(PackageFormat.ExprKind.RefExpr, copiedInitializer.kind)
            val copiedReference = copiedInitializer.info(PackageFormat.ReferenceInfo()) as PackageFormat.ReferenceInfo
            assertEquals("a", copiedReference.reference)
            assertEquals(PackageIndex.CURRENT.value, copiedReference.target?.pkgId)
            assertNotNull(copiedReference.target)
            assertEquals(
                exportedAIndex.toUInt() + 1u,
                copiedReference.target!!.index,
                "the re-exported initializer reference resolves to the real pattern binding",
            )
            val referencedBinding = specificPackage.allDecls(copiedReference.target!!.index.toInt() - 1)!!
            assertEquals(DeclKind.VarDecl, referencedBinding.kind)
            assertEquals(exportedA.identifier, referencedBinding.identifier)
        }
    }

    /** 三种构造声明的返回类型错误属于 parser；CLI 保留类型全范围并中止 CJO 输出。 */
    @Test
    fun `constructor return types report exact parser ranges in both frontend modes`() {
        val declarations = listOf(
            "Owner()" to "parsing.error.primary.constructor.return.type",
            "init()" to "parsing.error.constructor.return.type",
            "~init()" to "parsing.error.finalizer.return.type",
        )
        for (lightTree in listOf(false, true)) for (lineEnding in listOf("\n", "\r\n")) {
            for ((declaration, key) in declarations) {
                val source = writeCommon(
                    """
                    class Owner {
                        $declaration: Array<Int64> { let body = 1 }
                        func after(): Unit { }
                    }
                    """.trimIndent(),
                    lineEnding = lineEnding,
                )
                val output = Files.createDirectories(
                    tempDir.resolve("constructor-return-$lightTree-${lineEnding.length}-${key.substringAfter("parsing.error.")}"),
                )
                val result = compile(source) {
                    useLightTree = lightTree
                    cjmpMode = CfirCjmpMode.NONE
                    // 仅开启输出，普通类不引入 CJMP 声明语法规则。
                    cjmpChirOutput = true
                    cjoOutputDirectory = output.toString()
                }
                assertFalse(result.completed, "返回类型语法错误必须中止前端：${result.messages}")
                assertEquals(emptyList<String>(), result.diagnostics.map { it.factoryName })
                val errorIndices = result.messages.indices.filter { result.messages[it].first.isError }
                assertEquals(
                    listOf(CangJieParsingBundle.message(key)),
                    errorIndices.map { result.messages[it].second },
                    "只能报告当前声明角色的返回类型 parser 错误：${result.messages}",
                )
                val location = requireNotNull(result.locations[errorIndices.single()])
                val sourceText = Files.readString(source)
                val typeOffset = sourceText.indexOf("Array<Int64>")
                assertTrue(typeOffset >= 0)
                val line = sourceText.take(typeOffset).count { it == '\n' } + 1
                val column = typeOffset - sourceText.lastIndexOf('\n', typeOffset)
                assertEquals(source.toString().replace('\\', '/'), location.path.replace('\\', '/'))
                assertEquals(line, location.line)
                assertEquals(column, location.column)
                assertEquals(line, location.lineEnd)
                assertEquals(column + 12, location.columnEnd)
                Files.list(output).use { assertEquals(0, it.count(), "语法错误不得写出 CJO") }
            }
        }
    }

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
        val cjmpNominalMappings: Map<String, List<Pair<String, Map<String, String>>>> = emptyMap(),
        val cjmpCallableCounterpartCounts: Map<String, Int> = emptyMap(),
        val normalizedCommonParentCount: Int = 0,
        val deserializedExtendFacts: List<DeserializedExtendFact> = emptyList(),
        val deserializedCallableRoles: Map<String, List<Pair<Boolean, Boolean>>> = emptyMap(),
        val deserializedAccessorProvenance: List<Boolean> = emptyList(),
        val cjmpPackageExportRoots: Map<kotlin.String, List<Pair<kotlin.String, Boolean>>> = emptyMap(),
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

    private class CjmpOutputConfigurationProbePipeline : AbstractFrontendPipeline<CjmpArgumentProbe>() {
        val outputPaths = mutableListOf<Pair<String?, String?>>()

        override fun createCompoundPhase(
            arguments: CjmpArgumentProbe,
        ): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<CjmpArgumentProbe>, *> =
            object : PipelinePhase<ArgumentsPipelineArtifact<CjmpArgumentProbe>, ArgumentsPipelineArtifact<CjmpArgumentProbe>>(
                name = "CjmpOutputConfigurationProbePipeline",
            ) {
                override fun executePhase(
                    input: ArgumentsPipelineArtifact<CjmpArgumentProbe>,
                ): ArgumentsPipelineArtifact<CjmpArgumentProbe> {
                    outputPaths += input.configuration.cjoOutputDirectory to input.configuration.cjoOutputFile
                    return input
                }
            }
    }

    private class CjmpCfirFrontendPipeline(
        private val rootDisposable: Disposable,
    ) : AbstractFrontendPipeline<CjmpArgumentProbe>() {
        var artifact: DefaultCfirFrontendPipelineArtifact? = null
            private set

        override fun createCompoundPhase(
            arguments: CjmpArgumentProbe,
        ): CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<CjmpArgumentProbe>, *> =
            object : CompilerPhase<PipelineContext, ArgumentsPipelineArtifact<CjmpArgumentProbe>, DefaultCfirFrontendPipelineArtifact> {
                override fun invoke(
                    phaseConfig: PhaseConfig,
                    phaserState: PhaserState,
                    context: PipelineContext,
                    input: ArgumentsPipelineArtifact<CjmpArgumentProbe>,
                ): DefaultCfirFrontendPipelineArtifact = cfirFrontendPipeline().invoke(
                    phaseConfig,
                    phaserState,
                    context,
                    ConfigurationPipelineArtifact(input.configuration, rootDisposable),
                ).also { artifact = it }

                override fun getNamedSubphases(startDepth: Int) = cfirFrontendPipeline().getNamedSubphases(startDepth)
            }
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
            val artifact = cfirFrontendPipeline().executePhase(ConfigurationPipelineArtifact(configuration, disposable))
            val cjmpNominalMappings = buildMap<String, List<Pair<String, Map<String, String>>>> {
                for (output in artifact?.frontendOutput?.outputs.orEmpty()) {
                    if (!output.session.isCjmpSpecificCompilationEnabled()) continue
                    for (file in output.fir) {
                        val family = output.session.cjmpFamilyAggregate.ensureFamilyComplete(
                            file.packageDirective.packageFqName,
                        )
                        for (specific in file.declarations.filterIsInstance<CfirClassLikeDeclaration>()) {
                            val commonMappings = family.typeParameterMappingsFor(specific).mapNotNull { (common, mapping) ->
                                val commonClass = common as? CfirClassLikeDeclaration ?: return@mapNotNull null
                                commonClass.name.asString() to mapping.entries.associate { (commonParameter, specificParameter) ->
                                    commonParameter.name.asString() to specificParameter.name.asString()
                                }
                            }
                            if (commonMappings.isEmpty()) continue
                            put(
                                specific.name.asString(),
                                commonMappings,
                            )
                        }
                    }
                }
            }
            val cjmpCallableCounterpartCounts = buildMap<String, Int> {
                for (output in artifact?.frontendOutput?.outputs.orEmpty()) {
                    if (!output.session.isCjmpSpecificCompilationEnabled()) continue
                    for (file in output.fir) {
                        val family = output.session.cjmpFamilyAggregate.ensureFamilyComplete(
                            file.packageDirective.packageFqName,
                        )
                        for (specific in family.specificDeclarations.filterIsInstance<CfirCallableDeclaration>()) {
                            if (!specific.status.isSpecific) continue
                            put(specific.symbol.name.asString(), family.commonCounterpartsFor(specific).size)
                        }
                    }
                }
            }
            val cjmpPackageExportRoots = mutableMapOf<kotlin.String, List<Pair<kotlin.String, Boolean>>>()
            for (output in artifact?.frontendOutput?.outputs.orEmpty()) {
                if (!output.session.isCjmpSpecificCompilationEnabled()) continue
                for (packageName in output.fir.map { it.packageDirective.packageFqName }.distinct()) {
                    val family = output.session.cjmpFamilyAggregate.ensureFamilyComplete(packageName)
                    cjmpPackageExportRoots[packageName.asString()] = family.packageDeclarationsForExport().mapNotNull { declaration ->
                        val callable = declaration as? CfirCallableDeclaration ?: return@mapNotNull null
                        callable.symbol.name.asString() to (callable.cjoDeclarationOrigin != null)
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
            val deserializedAccessorProvenance = mutableListOf<Boolean>()
            val deserializedCallableRoles = buildMap {
                for (output in artifact?.frontendOutput?.outputs.orEmpty()) {
                    val provider = output.session.dependenciesSymbolProvider
                    for (packageName in output.fir.map { it.packageDirective.packageFqName }.distinct()) {
                        for (name in provider.symbolNamesProvider.getTopLevelCallableNamesInPackage(packageName).orEmpty()) {
                            val declarations = provider.getTopLevelCallableSymbols(packageName, name)
                                .map { it.cfir }.filterIsInstance<CfirMemberDeclaration>()
                            put(name.asString(), declarations.map { it.status.isCommon to it.isFromCommonPart })
                        }
                        for (name in provider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(packageName).orEmpty()) {
                            val owner = provider.getClassLikeSymbolByClassId(ClassId(packageName, name))?.cfir ?: continue
                            for (property in owner.declarations.filterIsInstance<CfirProperty>()) {
                                listOfNotNull(property.getter, property.setter).forEach {
                                    deserializedAccessorProvenance += it.isFromCommonPart
                                }
                            }
                        }
                    }
                }
            }
            return CompileResult(
                diagnostics = diagnostics.diagnostics,
                messages = collector.messages.toList(),
                // executePhase 不运行 postActions；使用该阶段真实的生产错误后置条件判断编译是否完成。
                completed = artifact != null && !CheckCompilationErrors.CheckDiagnosticCollector.checkHasErrors(configuration),
                locations = collector.locations.toList(),
                cjmpNominalMappings = cjmpNominalMappings,
                cjmpCallableCounterpartCounts = cjmpCallableCounterpartCounts,
                normalizedCommonParentCount = configuration.requireCjmpCommonPartInputs().parents.size,
                deserializedExtendFacts = deserializedExtendFacts,
                deserializedCallableRoles = deserializedCallableRoles,
                deserializedAccessorProvenance = deserializedAccessorProvenance.toList(),
                cjmpPackageExportRoots = cjmpPackageExportRoots,
            )
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private fun writeCommon(
        content: String,
        packageName: String = "cjmp_p",
        features: Set<String> = emptySet(),
        lineEnding: String = "\n",
    ): Path {
        val dir = Files.createDirectories(tempDir.resolve("common"))
        val file = dir.resolve("common.cj")
        val featuresDirective = features.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "features { ", postfix = " }\n\n")
            .orEmpty()
        Files.writeString(file, "${featuresDirective}package $packageName\n\n$content".replace("\n", lineEnding))
        return file
    }

    private fun writeSpecific(
        content: String,
        features: Set<String> = emptySet(),
        packageName: String = "cjmp_p",
    ): Path {
        val dir = Files.createDirectories(tempDir.resolve("specific"))
        val file = dir.resolve("specific.cj")
        val featuresDirective = features.takeIf { it.isNotEmpty() }
            ?.joinToString(prefix = "features { ", postfix = " }\n\n")
            .orEmpty()
        Files.writeString(file, "${featuresDirective}package $packageName\n\n$content")
        return file
    }

    /** 编译普通库并生成可由后续 session 从 classpath 反序列化的 CJO。 */
    private fun compileNormalCjoLibrary(
        content: String,
        packageName: String,
        classpathRoots: List<Path> = emptyList(),
    ): Path {
        val sourceRoot = Files.createDirectories(tempDir.resolve("library-source-$packageName"))
        val source = sourceRoot.resolve("library.cj")
        Files.writeString(source, "package $packageName\n\n$content")
        val outputRoot = Files.createDirectories(tempDir.resolve("library-cjo-$packageName"))
        val result = compile(source) {
            classpathRoots.forEach { addClasspathRoot(it.toString()) }
            cjoOutputDirectory = outputRoot.toString()
        }
        assertTrue(result.completed, "ordinary library frontend completes: ${result.messages}")
        val diagnosticLocations = result.diagnostics.joinToString { diagnostic ->
            val psi = ((diagnostic as? CjDiagnosticWithSource)?.element as? CjPsiSourceElement)?.psi
            val line = runCatching {
                psi?.let { element ->
                    element.containingFile.text.take(element.textRange.startOffset).count { it == '\n' } + 1
                }
            }.getOrNull()
            val psiText = runCatching { psi?.text }.getOrNull()
            "${diagnostic.factoryName}@$line '${psiText ?: "<no PSI>"}'"
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
        moduleOptLevel: CfirCjmpOptimizationLevel = CfirCjmpOptimizationLevel.O0,
        outputDirectoryName: String = "out",
        languageVersion: String? = null,
        useLightTree: Boolean? = null,
        classpathRoots: List<Path> = emptyList(),
    ): Path {
        val outDir = Files.createDirectories(tempDir.resolve(outputDirectoryName))
        val result = compile(writeCommon(content, packageName, packageFeatures)) {
            useLightTree?.let { this.useLightTree = it }
            languageVersion?.let { assertTrue(configureLanguageVersionSettings(it)) }
            classpathRoots.forEach { addClasspathRoot(it.toString()) }
            cjmpChirOutput = true
            cjoOutputDirectory = outDir.toString()
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
        moduleOptLevel: CfirCjmpOptimizationLevel = CfirCjmpOptimizationLevel.O0,
        languageVersion: String? = null,
        packageName: String = "cjmp_p",
        outputDirectory: Path? = null,
        classpathRoots: List<Path> = emptyList(),
        useLightTree: Boolean? = null,
    ): CompileResult = compileSpecific(
        content = content,
        commonCjos = listOf(commonCjo),
        packageFeatures = packageFeatures,
        moduleDebug = moduleDebug,
        moduleOptLevel = moduleOptLevel,
        languageVersion = languageVersion,
        packageName = packageName,
        outputDirectory = outputDirectory,
        classpathRoots = classpathRoots,
        useLightTree = useLightTree,
    )

    private fun compileSpecific(
        content: String,
        commonCjos: List<Path>,
        packageFeatures: Set<String> = emptySet(),
        moduleDebug: Boolean = false,
        moduleOptLevel: CfirCjmpOptimizationLevel = CfirCjmpOptimizationLevel.O0,
        languageVersion: String? = null,
        additionalSourceRoots: List<Path> = emptyList(),
        packageName: String = "cjmp_p",
        outputDirectory: Path? = null,
        classpathRoots: List<Path> = emptyList(),
        useLightTree: Boolean? = null,
    ): CompileResult =
        compile(writeSpecific(content, packageFeatures, packageName)) {
            useLightTree?.let { this.useLightTree = it }
            languageVersion?.let { assertTrue(configureLanguageVersionSettings(it)) }
            additionalSourceRoots.forEach { addCangJieSourceRoot(it.toString()) }
            classpathRoots.forEach { addClasspathRoot(it.toString()) }
            outputDirectory?.let { this.cjoOutputDirectory = it.toString() }
            val cjoPaths = commonCjos.map(Path::toString)
            // This fixture drives CFIR directly; command-line CHIR linkage is validated by the driver boundary.
            configureCjmpCommonPartInputs(cjoPaths, emptyList())
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

        common extend Defaulted {
            public common func extensionValue(): Int64 { 1 }
        }
    """.trimIndent()

    @Test
    fun `legacy language version shares its last cjo input with session settings`() {
        val configuration = CompilerConfiguration()
        assertTrue(configuration.configureLanguageVersionSettings("1.1.3"))
        val firstCjo = Files.createFile(tempDir.resolve("legacy-first.cjo"))
        val lastCjo = Files.createFile(tempDir.resolve("legacy-last.cjo"))

        val resolution = configuration.configureCjmpCommonPartInputs(
            commonPartCjoPaths = listOf(firstCjo.toString(), lastCjo.toString()),
            commonPartChirPaths = emptyList(),
        )
        val inputs = assertInstanceOf(
            CjmpCommonPartInputResolution.Valid::class.java,
            resolution,
        ).inputs
        val settings = createCfirCjmpSettingsComponent(configuration)

        assertSame(inputs, settings.commonPartInputs)
        assertEquals(listOf(lastCjo.toString()), settings.commonPartCjoPaths)
        assertEquals(emptyList<String>(), settings.commonPartChirPaths)
        assertEquals(CfirCjmpMode.SPECIFIC, settings.mode)
    }

    @Test
    fun `legacy positional chir input activates driver specific mode`() {
        val configuration = CompilerConfiguration()
        assertTrue(configuration.configureLanguageVersionSettings("1.1.3"))
        val chirPath = tempDir.resolve("legacy-positional.chir")
        configuration.positionalChirInputs = listOf(chirPath)

        val settings = createCfirCjmpSettingsComponent(configuration)

        assertEquals(listOf(chirPath), settings.positionalChirInputs)
        assertTrue(settings.compileSpecific)
        assertEquals(CfirCjmpMode.SPECIFIC, settings.mode)
    }

    @Test
    fun `dedicated chir inputs are independent from multiple cjo parents`() {
        val languageVersionSettings = org.cangnova.cangjie.LanguageVersionSettingsImpl(
            languageVersion = org.cangnova.cangjie.LanguageVersion.CANGJIE_1_2_0_ALPHA_20,
            specificFeatures = mapOf(
                LanguageFeature.CjmpMultipleCommonParents to LanguageFeature.State.DISABLED,
            ),
        )

        val resolution = org.cangnova.cangjie.cfir.entrypoint.configuration.CjmpCommonPartInputResolver.resolve(
            commonPartCjoPaths = listOf("first.cjo", "selected.cjo"),
            commonPartChirPaths = listOf("selected.chir"),
            languageVersionSettings = languageVersionSettings,
        )

        val inputs = assertInstanceOf(CjmpCommonPartInputResolution.Valid::class.java, resolution).inputs
        assertEquals(listOf("selected.cjo"), inputs.commonPartCjoPaths)
        assertEquals(listOf("selected.chir"), inputs.commonPartChirPaths)
    }

    @Test
    fun `legacy protocol rejects a missing earlier cjo even when the last input exists`() {
        val configuration = CompilerConfiguration()
        assertTrue(configuration.configureLanguageVersionSettings("1.1.3"))
        val missingPath = tempDir.resolve("missing-parent.cjo").toString()
        val lastPath = Files.createFile(tempDir.resolve("last-parent.cjo")).toString()

        val resolution = configuration.configureCjmpCommonPartInputs(
            commonPartCjoPaths = listOf(missingPath, lastPath),
            commonPartChirPaths = emptyList(),
        )

        val invalid = assertInstanceOf(CjmpCommonPartInputResolution.Invalid::class.java, resolution)
        assertEquals(
            org.cangnova.cangjie.cfir.entrypoint.configuration.CjmpCommonPartInputErrorKind.COMMON_PART_INPUT_FILE_INVALID,
            invalid.kind,
        )
        assertEquals(missingPath, invalid.inputPath)
        assertThrows(IllegalStateException::class.java) { configuration.requireCjmpCommonPartInputs() }
    }

    @Test
    fun `common cjo parent directory is not an ordinary library search root`() {
        val commonCjo = compileCommon("public common func platform(): Int64")
        val libraryOutput = compileNormalCjoLibrary(
            "public func siblingOnly(): Int64 { 42 }",
            packageName = "siblingonlylib",
        )
        val libraryCjo = libraryOutput.resolve(CjoConstants.packageNameToPath("siblingonlylib"))
        val colocatedLibraryCjo = commonCjo.parent.resolve(CjoConstants.packageNameToPath("siblingonlylib"))
        Files.createDirectories(checkNotNull(colocatedLibraryCjo.parent))
        Files.copy(libraryCjo, colocatedLibraryCjo)
        assertEquals(commonCjo.parent, colocatedLibraryCjo.parent, "ordinary library CJO is beside the common CJO")

        val result = compileSpecific(
            """
            import siblingonlylib.*

            public specific func platform(): Int64 { siblingOnly() }
            """.trimIndent(),
            commonCjo,
        )

        assertTrue(
            result.diagnostics.any { it.severity.isError },
            "the specific compile reports an error when only the common CJO path is configured: ${result.diagnostics}",
        )
        assertTrue(
            result.diagnostics.any {
                it.factoryName == "CFIR_UNRESOLVED_IMPORT" &&
                        it.renderMessage() == "unresolved import 'siblingonlylib'"
            },
            "the sibling package import is unresolved: ${result.diagnostics.map { it.factoryName to it.renderMessage() }}",
        )
    }

    @Test
    fun `legacy version preloads only the last common cjo and pairs it through the dependency graph`() {
        val firstParent = compileCommon(
            "public common func firstParentOnly(): Int64 { 7 }",
            outputDirectoryName = "legacy-last-parent-first",
        )
        val lastParent = compileCommon(
            """
            public common func selected(value: Int64): Int64
            public common func lastParentHelper(): Int64 { 41 }
            """.trimIndent(),
            outputDirectoryName = "legacy-last-parent-last",
        )

        val firstPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(firstParent)))
        val lastPackage = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(lastParent)))
        val firstDeclaration = (0 until firstPackage.allDeclsLength)
            .map { firstPackage.allDecls(it)!! }
            .single { it.identifier == "firstParentOnly" }
        val lastSelectedDeclaration = (0 until lastPackage.allDeclsLength)
            .map { lastPackage.allDecls(it)!! }
            .single { it.identifier == "selected" }
        assertTrue(hasCjoAttribute(firstDeclaration, Attribute.COMMON), "the first CJO really contains its common declaration")
        assertTrue(hasCjoAttribute(lastSelectedDeclaration, Attribute.COMMON), "the last CJO really contains the paired declaration")

        val result = compileSpecific(
            "public specific func selected(value: Int64): Int64 { lastParentHelper() }",
            listOf(firstParent, lastParent),
            languageVersion = "1.1.3",
        )

        assertEquals(1, result.normalizedCommonParentCount, "legacy normalization retains exactly one parent input")
        assertTrue(result.completed, "legacy specific compilation completes: ${result.messages}; ${result.diagnostics}")
        assertTrue(result.messages.none { it.first.isError }, "preload and module graph report no errors: ${result.messages}")
        assertEquals(emptyList<String>(), result.diagnostics.map { it.factoryName }, "the last parent's common declaration pairs cleanly")
        assertEquals(1, result.cjmpCallableCounterpartCounts["selected"], "specific selected maps to the last CJO declaration")
        assertEquals(listOf(true to true), result.deserializedCallableRoles["selected"])
        assertEquals(
            listOf(true to true),
            result.deserializedCallableRoles["lastParentHelper"],
            "the specific body resolves a common helper deserialized from the last CJO",
        )
        assertEquals(
            null,
            result.deserializedCallableRoles["firstParentOnly"],
            "the first CJO's valid declaration is absent from the specific dependency provider",
        )
    }

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
        val identityGeneric = checkNotNull(identity.generic)
        assertEquals(
            0,
            identityGeneric.constraintsLength,
            "the common CJO does not manufacture an empty constraint for the synthetic default Any upper bound",
        )
        val identityInfo = identity.info(PackageFormat.FuncInfo()) as PackageFormat.FuncInfo
        val identityParameterIndex = checkNotNull(identityInfo.funcBody).paramLists(0)!!.params(0).toInt() - 1
        val identityParameter = checkNotNull(pkg.allDecls(identityParameterIndex))
        val identityParameterType = checkNotNull(pkg.allTypes(identityParameter.type.toInt() - 1))
        val identityTypeInfo = identityParameterType.info(GenericTyInfo()) as GenericTyInfo
        assertEquals(0, identityTypeInfo.upperBoundsLength, "GenericTyInfo also omits the synthetic default Any bound")

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
    fun `specific nominal scope resolves common-only members from a cjo`() {
        val cjo = compileCommon(
            """
            public common class Box<T> {
                public common init(value: T) {}
                public common func commonOnly(value: T): T { value }
                public common prop marker: Int64 { get() { 1 } }
            }
            """.trimIndent(),
        )
        val exportDirectory = Files.createDirectories(tempDir.resolve("specific-nominal-export"))
        val result = compileSpecific(
            """
            public specific class Box<U> {}

            public func construct(value: Int64): Int64 {
                let _ = Box<Int64>(value)
                value
            }

            public func use(box: Box<Int64>): Int64 {
                box.commonOnly(42) + box.marker
            }
            """.trimIndent(),
            cjo,
            outputDirectory = exportDirectory,
        )

        assertTrue(
            result.diagnostics.none { it.severity.isError },
            result.diagnostics.joinToString { diagnostic ->
                val element = (diagnostic as? CjDiagnosticWithSource)?.element
                val psi = (element as? CjPsiSourceElement)?.psi
                val source = psi?.containingFile?.text.orEmpty()
                val start = diagnostic.firstRange.startOffset
                val context = source.substring((start - 20).coerceAtLeast(0), (start + 20).coerceAtMost(source.length))
                "${diagnostic.factory.name}@$start:$context"
            },
        )
        assertTrue(result.messages.none { it.first.isError }, "${result.messages}")
        assertTrue(result.completed, "specific body resolution must see the merged CJO member: ${result.messages}")

        val exportedPackage = Package.getRootAsPackage(
            ByteBuffer.wrap(Files.readAllBytes(exportDirectory.resolve(CjoConstants.packageNameToPath("cjmp_p")))),
        )
        val exportedDeclarations = (0 until exportedPackage.allDeclsLength).mapNotNull(exportedPackage::allDecls)
        val exportedBox = exportedDeclarations.single { it.kind == DeclKind.ClassDecl && it.identifier == "Box" }
        val exportedBoxInfo = exportedBox.info(ClassInfo()) as ClassInfo
        val exportedMemberNames = (0 until exportedBoxInfo.bodyLength).map { index ->
            exportedDeclarations[exportedBoxInfo.body(index).toInt() - 1].identifier
        }
        assertEquals(1, exportedMemberNames.count { it == "commonOnly" })
        assertEquals(1, exportedMemberNames.count { it == "marker" })

        val missingCommonConstructorArgument = compileSpecific(
            """
            public specific class Box<U> {}

            public func use(): Box<Int64> {
                Box<Int64>()
            }
            """.trimIndent(),
            cjo,
        )
        assertEquals(
            listOf(CfirErrors.WRONG_NUMBER_OF_ARGUMENTS.name),
            missingCommonConstructorArgument.diagnostics.map { it.factory.name },
            "the common constructor remains visible and the specific raw builder contributes no zero-argument candidate",
        )

        val specificOnlyDefaults = compileSpecific(
            """
            public specific class EmptyClass {}
            public specific struct EmptyStruct {}

            public func constructSpecificDefaults(): Unit {
                let _ = EmptyClass()
                let _ = EmptyStruct()
            }
            """.trimIndent(),
            cjo,
        )
        assertEquals(
            listOf(CfirErrors.NOT_MATCHED.name, CfirErrors.NOT_MATCHED.name),
            specificOnlyDefaults.diagnostics.map { it.factory.name },
            "specific-only class and struct calls resolve through their post-merge default constructors: ${specificOnlyDefaults.diagnostics}",
        )

        val constructorNameCollision = compileSpecific(
            """
            public specific class ConstructionChoice {}
            public func ConstructionChoice(value: Int64): Int64 { value }

            public func constructionChoice(): ConstructionChoice {
                ConstructionChoice()
            }
            """.trimIndent(),
            cjo,
        )
        assertEquals(
            listOf(CfirErrors.NOT_MATCHED.name, CfirErrors.REDECLARATION.name),
            constructorNameCollision.diagnostics.map { it.factory.name },
            "the same-scope class constructor is selected while the illegal declaration conflict remains: ${constructorNameCollision.diagnostics}",
        )

        val nearerMemberFunction = compileSpecific(
            """
            public specific class ConstructionChoice {}
            public struct ConstructionOwner {
                public func ConstructionChoice(): Int64 { 42 }
                public func choose(): Int64 { ConstructionChoice() }
            }
            """.trimIndent(),
            cjo,
        )
        assertEquals(
            listOf(CfirErrors.NOT_MATCHED.name),
            nearerMemberFunction.diagnostics.map { it.factory.name },
            "a nearer member function shadows an outer class constructor: ${nearerMemberFunction.diagnostics}",
        )

        val inheritedMemberFunction = compileSpecific(
            """
            public specific class ConstructionChoice {}
            public open class ConstructionBase {
                public func ConstructionChoice(): Int64 { 42 }
            }
            public class ConstructionDerived <: ConstructionBase {
                public func choose(): Int64 { ConstructionChoice() }
            }
            """.trimIndent(),
            cjo,
        )
        assertEquals(
            listOf(CfirErrors.NOT_MATCHED.name),
            inheritedMemberFunction.diagnostics.map { it.factory.name },
            "an inherited member function shadows an outer class constructor: ${inheritedMemberFunction.diagnostics}",
        )
    }

    @Test
    fun `enum constructor does not shadow an outer classifier in a payload type`() {
        val cjo = compileCommon("public common func commonAnchor(): Int64 { 0 }")
        val result = compileSpecific(
            """
            public class A {}
            public enum E {
                A(A)
            }
            """.trimIndent(),
            cjo,
        )

        assertTrue(result.diagnostics.none { it.severity.isError }, "enum payload resolves the outer class type: ${result.diagnostics}")
        assertTrue(result.messages.none { it.first.isError }, "enum payload type lookup completes without errors: ${result.messages}")
        assertTrue(result.completed, "enum constructor names do not enter the non-type shadowing set")
    }

    @Test
    fun `alpha twenty matches a nominal and callables from multiple common parents`() {
        val firstParentContent = """
            public common class Box<T> {
                public common init()
                public common func identity(candidate: T): T { candidate }
            }
            public common func select<T>(candidate: T): T { candidate }
        """.trimIndent()
        val firstParent = compileCommon(firstParentContent, outputDirectoryName = "multiple-parents-first")
        val secondParent = compileCommon(
            "public common func select<T>(candidate: T): T { candidate }",
            outputDirectoryName = "multiple-parents-second",
        )
        val result = compileSpecific(
            """
                public specific class Box<U> {
                    public specific init() {}
                    public specific func identity(candidate: U): U { candidate }
                }
                public specific func select<U>(candidate: U): U { candidate }
            """.trimIndent(),
            listOf(firstParent, secondParent),
            languageVersion = "1.2.0-alpha.20",
        )

        assertEquals(2, result.normalizedCommonParentCount, "configuration retains both normalized parent inputs")
        assertTrue(result.completed, "the two-parent specific pipeline completes: ${result.messages}")
        assertEquals(emptyList<String>(), result.diagnostics.map { it.factoryName }, "both common declarations bind")
        assertEquals(listOf("Box" to mapOf("T" to "U")), result.cjmpNominalMappings["Box"])
        assertEquals(2, result.cjmpCallableCounterpartCounts["select"])
        assertEquals(1, result.cjmpCallableCounterpartCounts["identity"])
    }

    /** alpha.20 允许独立文件具有不同 CJMP 角色；有默认实现的 source common nominal 不要求 specific 对应项。 */
    @Test
    fun `alpha twenty multiple parents accept a source common default nominal`() {
        val commonParent = compileCommon(
            "public common class ParentOne { public common init() {} }",
            outputDirectoryName = "multiple-parents-with-source-common",
        )
        val secondCommonParent = compileCommon(
            "public common class ParentTwo { public common init() {} }",
            outputDirectoryName = "multiple-parents-with-source-common-second",
        )
        val sourceCommon = writeCommon("public common class Local { public common init() {} }")
        val result = compileSpecific(
            "",
            listOf(commonParent, secondCommonParent),
            languageVersion = "1.2.0-alpha.20",
            additionalSourceRoots = listOf(sourceCommon),
        )

        assertTrue(result.completed, "the source common nominal and two-parent pipeline complete: ${result.messages}")
        assertTrue(result.messages.none { it.first.isError }, "no compiler error is expected: ${result.messages}")
        assertTrue(result.diagnostics.none { it.severity.isError }, "no semantic error is expected: ${result.diagnostics}")
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
                "mappings=${result.cjmpNominalMappings}, messages=${result.messages}, diagnostics=" +
                result.diagnostics.map { it.renderMessage() },
        )
        assertEquals(emptyList<String>(), result.diagnostics.map { it.factoryName.removePrefix("CFIR_") })
        assertEquals(
            listOf("Box" to mapOf("T" to "U")),
            result.cjmpNominalMappings["Box"],
            "the enum mapping carries its generic parameter identity through the CJO constructor table",
        )
    }

    /** 官方 CollectDecl 只收集 COMMON；common part 中的普通 helper 不要求 specific 实现。 */
    @Test
    fun `unmarked common part helper does not become a required common declaration`() {
        val outputRoot = tempDir.resolve("helper-common-output")
        val common = compile(
            writeCommon(
                """
                public func helper(): Int64 { 1 }
                public class HelperBox {
                    public init() {}
                    public prop value: Int64 { get() { 1 } }
                }
                public common func api(): Int64
                """.trimIndent(),
            ),
        ) {
            cjmpChirOutput = true
            cjoOutputDirectory = outputRoot.toString()
        }
        assertTrue(common.completed, "common pipeline completes: ${common.messages}")
        assertTrue(common.messages.none { it.first.isError }, "common has no message errors: ${common.messages}")
        assertTrue(common.diagnostics.none { it.severity.isError }, "common has no diagnostic errors: ${common.diagnostics}")
        val cjo = outputRoot.resolve(CjoConstants.packageNameToPath("cjmp_p"))
        assertTrue(Files.isRegularFile(cjo), "common compilation writes the input CJO")
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(cjo)))
        val helper = (0 until pkg.allDeclsLength).mapNotNull { pkg.allDecls(it) }
            .single { it.identifier == "helper" }
        assertFalse(hasCjoAttribute(helper, Attribute.COMMON), "helper is not a common declaration")
        assertTrue(hasCjoAttribute(helper, Attribute.FROM_COMMON_PART), "helper still records its common-part provenance")

        val specific = compileSpecific("public specific func api(): Int64 { helper() }", cjo)
        assertTrue(specific.completed, "matched api and ordinary helper must complete: ${specific.messages}; ${specific.diagnostics}")
        assertTrue(specific.messages.none { it.first.isError }, "helper must not cause a common-side message error: ${specific.messages}")
        assertTrue(specific.diagnostics.none { it.severity.isError }, "helper remains callable: ${specific.diagnostics}")
        assertEquals(listOf(false to true), specific.deserializedCallableRoles["helper"], "role and provenance remain independent after loading")
        assertEquals(listOf(true), specific.deserializedAccessorProvenance, "accessors preserve the common-part origin through the shared publication path")
        val exportRoots = specific.cjmpPackageExportRoots.getValue("cjmp_p")
        assertTrue(exportRoots.any { (name, fromCjo) -> name == "helper" && fromCjo }, "ordinary common helper stays in package export roots")
        assertTrue(exportRoots.any { (name, fromCjo) -> name == "api" && !fromCjo }, "specific implementation stays in package export roots")
        assertFalse(exportRoots.any { (name, fromCjo) -> name == "api" && fromCjo }, "matched common declaration is removed from export roots")
    }

    @Test
    fun `family export roots retain every declaration in a package without cjmp candidates`() {
        val commonCjo = compileCommon(
            "public func commonOne(): Int64 { 1 }\npublic func commonTwo(): Int64 { 2 }",
            outputDirectoryName = "ordinary-roots-common",
        )
        val specific = compileSpecific(
            "public func specificOne(): Int64 { 3 }\npublic func specificTwo(): Int64 { 4 }",
            commonCjo,
        )

        assertTrue(specific.completed, "ordinary declarations do not require CJMP matching: ${specific.messages}; ${specific.diagnostics}")
        val roots = specific.cjmpPackageExportRoots.getValue("cjmp_p")
        assertEquals(
            setOf("commonOne" to true, "commonTwo" to true, "specificOne" to false, "specificTwo" to false),
            roots.toSet(),
            "empty candidate groups must not collapse package export roots to one declaration",
        )
    }

    /** FROM_COMMON_PART 不授予候选资格；未标 common 的 g 不能满足 specific g。 */
    @Test
    fun `specific function cannot match an unmarked common part function`() {
        val outputRoot = tempDir.resolve("unmarked-common-output")
        val common = compile(writeCommon("public func g(): Int64 { 1 }")) {
            cjmpChirOutput = true
            cjoOutputDirectory = outputRoot.toString()
        }
        assertTrue(common.completed, "ordinary declaration in common part compiles: ${common.messages}")
        assertTrue(common.messages.none { it.first.isError }, "common has no message errors: ${common.messages}")
        assertTrue(common.diagnostics.none { it.severity.isError }, "common has no diagnostic errors: ${common.diagnostics}")
        val cjo = outputRoot.resolve(CjoConstants.packageNameToPath("cjmp_p"))
        assertTrue(Files.isRegularFile(cjo), "common compilation writes the input CJO")
        val pkg = Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(cjo)))
        val unmarked = (0 until pkg.allDeclsLength).mapNotNull { pkg.allDecls(it) }
            .single { it.identifier == "g" }
        assertFalse(hasCjoAttribute(unmarked, Attribute.COMMON), "g must remain outside the common candidate set")
        assertTrue(hasCjoAttribute(unmarked, Attribute.FROM_COMMON_PART), "g records only its common-part provenance")

        val specific = compileSpecific("public specific func g(): Int64 { 2 }", cjo)
        assertTrue(
            specific.diagnostics.any { it.factoryName.removePrefix("CFIR_") == "NOT_MATCHED" },
            "specific g has no COMMON counterpart: ${specific.diagnostics}",
        )
        // 未标记 g 仍参与普通重定义检查，不固定额外重定义诊断的数量或顺序。
        assertFalse(specific.completed, "unmatched specific g must fail the pipeline: ${specific.messages}")
        assertEquals(listOf(false to true), specific.deserializedCallableRoles["g"], "unmarked g retains only common-part provenance")
        assertTrue(
            specific.messages.none { it.first.isError && it.second.contains("'common' function 'g'") },
            "the ordinary helper must not receive a false common-side error: ${specific.messages}",
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
    fun `specific variable redeclaration is anchored at common property in cjo`() {
        val cjo = compileCommon(
            """
            public common class PropertyBox {
                public common init()
                public prop value: Int64 { get() { 1 } }
            }
            """.trimIndent(),
        )

        val result = compileSpecific(
            """
            public specific class PropertyBox {
                public specific init() {}
                public var value: Int64 = 2
            }
            """.trimIndent(),
            cjo,
        )

        val diagnostic = result.diagnostics.single { it.factoryName == "CFIR_REDECLARATION" }
        val primarySource = assertInstanceOf(
            CjBinarySourceElement::class.java,
            (diagnostic as CjDiagnosticWithSource).element,
        )
        assertTrue(primarySource.sourceLocation?.filePath?.endsWith("common.cj") == true)
        assertEquals(5, primarySource.sourceLocation?.line)
        assertEquals(17, primarySource.sourceLocation?.column, "the primary CJO location is ${primarySource.sourceLocation}")

        val note = diagnostic.relatedInformation.single()
        assertEquals("'value' is previously declared here", note.message)
        assertNull(note.sourceLocation, "the specific property peer retains its source-tree location")
        assertTrue(note.element is CjPsiSourceElement)
        assertTrue((note.element as CjPsiSourceElement).psi.text == "value")
    }

    @Test
    fun `unmarked common member overload conflict retains cjo peer note`() {
        val cjo = compileCommon(
            """
            public common class Box<T> {
                public common init()
                public func f(value: T): Unit { }
            }
            """.trimIndent(),
        )

        val result = compileSpecific(
            """
            public specific class Box<U> {
                public specific init() { }
                public func f(value: U): Unit { }
            }
            """.trimIndent(),
            cjo,
        )

        val overload = result.diagnostics.single { it.factoryName == "CFIR_CONFLICTING_OVERLOADS" }
        val note = overload.relatedInformation.single()
        assertEquals("conflict with the declaration", note.message)
        assertTrue(note.sourceLocation?.filePath?.endsWith("common.cj") == true)
        assertEquals(5, note.sourceLocation?.line)
        assertEquals(17, note.sourceLocation?.column)
    }

    @Test
    fun `specific overload suppresses only its matched common signature`() {
        val cjo = compileCommon(
            """
            public class SignatureGroupMarker { }

            public common class SignatureGroupBox {
                public common init()
                public func f(value: Int64): Unit { }
                public common func f(value: SignatureGroupMarker): Unit { }
            }
            """.trimIndent(),
        )
        val specificSource = """
            public specific class SignatureGroupBox {
                public specific init() { }
                public specific func f(value: SignatureGroupMarker): Unit { }
                public func f(value: Int64): Unit { }
            }
        """.trimIndent()
        val result = compileSpecific(specificSource, cjo)

        assertEquals(
            listOf("CONFLICTING_OVERLOADS"),
            result.diagnostics.map { it.factoryName.removePrefix("CFIR_") },
            "the paired Marker signatures disappear while the unmarked Int64 redeclaration remains diagnosed",
        )
        val overload = result.diagnostics.single()
        val note = overload.relatedInformation.single()
        assertEquals("conflict with the declaration", note.message)
        assertTrue(note.sourceLocation?.filePath?.endsWith("common.cj") == true)
        assertEquals(7, note.sourceLocation?.line, "the note points to the common unmarked Int64 overload")
    }

    @Test
    fun `specific extend overload suppresses only its matched common signature`() {
        val cjo = compileCommon(
            """
            public common class ExtendSignatureGroupBox {
                public common init()
            }

            public class ExtendSignatureGroupMarker { }

            common extend ExtendSignatureGroupBox {
                public func f(value: Int64): Unit { }
                public common func f(value: ExtendSignatureGroupMarker): Unit { }
            }
            """.trimIndent(),
        )
        val specificSource = """
            public specific class ExtendSignatureGroupBox {
                public specific init() { }
            }

            specific extend ExtendSignatureGroupBox {
                public specific func f(value: ExtendSignatureGroupMarker): Unit { }
                public func f(value: Int64): Unit { }
            }
        """.trimIndent()
        val result = compileSpecific(specificSource, cjo)

        assertEquals(
            listOf("CONFLICTING_OVERLOADS"),
            result.diagnostics.map { it.factoryName.removePrefix("CFIR_") },
            "the paired Marker extend signature is filtered while the unmarked Int64 redeclaration remains diagnosed",
        )
        val note = result.diagnostics.single().relatedInformation.single()
        assertEquals("conflict with the declaration", note.message)
        assertTrue(note.sourceLocation?.filePath?.endsWith("common.cj") == true)
        assertEquals(10, note.sourceLocation?.line, "the note points to the common unmarked Int64 extend overload")
    }

    @Test
    fun `specific extend overload keeps unmatched common overload available to calls`() {
        val cjo = compileCommon(
            """
            public common class ExtendCallGroupBox {
                public common init()
            }

            public class ExtendCallGroupMarker { }

            common extend ExtendCallGroupBox {
                public func f(value: Int64): Int64 { value }
                public common func f(value: ExtendCallGroupMarker): Int64 { 1 }
            }
            """.trimIndent(),
        )
        val result = compileSpecific(
            """
            public specific class ExtendCallGroupBox {
                public specific init() { }
            }

            specific extend ExtendCallGroupBox {
                public specific func f(value: ExtendCallGroupMarker): Int64 { 2 }
            }

            public func callCommonOnlyOverload(box: ExtendCallGroupBox): Int64 {
                box.f(1)
            }
            """.trimIndent(),
            cjo,
        )

        assertTrue(
            result.diagnostics.none { it.severity.isError },
            "the unmatched common Int64 overload remains available after the Marker signature is paired: ${result.diagnostics}",
        )
    }

    @Test
    fun `specific generic class constructor filters only its paired overload`() {
        for (useLightTree in listOf(false, true)) {
            val cjo = compileCommon(
                """
                public class ConstructorGroupMarker { }

                public common class GenericConstructorGroupBox<T> {
                    public common init(value: T) { }
                    public init(value: ConstructorGroupMarker) { }
                }
                """.trimIndent(),
                outputDirectoryName = "generic-constructor-group-$useLightTree",
                useLightTree = useLightTree,
            )
            val result = compileSpecific(
                """
                public specific class GenericConstructorGroupBox<U> {
                    public specific init(value: U) { }
                    public init(value: ConstructorGroupMarker) { }
                }
                """.trimIndent(),
                cjo,
                useLightTree = useLightTree,
            )

            assertEquals(
                listOf("CONFLICTING_OVERLOADS"),
                result.diagnostics.map { it.factoryName.removePrefix("CFIR_") },
                "the generic constructor pair is filtered, while the unmarked Marker secondary constructor still conflicts",
            )
            val note = result.diagnostics.single().relatedInformation.single()
            assertEquals("conflict with the declaration", note.message)
            assertTrue(note.sourceLocation?.filePath?.endsWith("common.cj") == true)
            assertEquals(7, note.sourceLocation?.line, "the note points to the common unmarked Marker constructor")
        }
    }

    @Test
    fun `specific generic function type parameters filter alpha equivalent signatures`() {
        for (useLightTree in listOf(false, true)) {
            val cjo = compileCommon(
                """
                public class FunctionTypeParameterGroupMarker { }

                public common class FunctionTypeParameterGroupBox<T> {
                    public common init()
                    public common func f<V>(owner: T, value: V): Unit { }
                    public func f(value: FunctionTypeParameterGroupMarker): Unit { }
                }
                """.trimIndent(),
                outputDirectoryName = "function-type-parameter-group-$useLightTree",
                useLightTree = useLightTree,
            )
            val result = compileSpecific(
                """
                public specific class FunctionTypeParameterGroupBox<U> {
                    public specific init() { }
                    public specific func f<W>(owner: U, value: W): Unit { }
                    public func f(value: FunctionTypeParameterGroupMarker): Unit { }
                }

                public func instantiate(value: FunctionTypeParameterGroupBox<Int64>): Unit { }
                """.trimIndent(),
                cjo,
                useLightTree = useLightTree,
            )

            assertEquals(
                listOf("CONFLICTING_OVERLOADS"),
                result.diagnostics.map { it.factoryName.removePrefix("CFIR_") },
                "owner T/U and callable V/W substitutions preserve the paired generic function; only the unmarked Marker signature conflicts",
            )
            val note = result.diagnostics.single().relatedInformation.single()
            assertEquals("conflict with the declaration", note.message)
            assertTrue(note.sourceLocation?.filePath?.endsWith("common.cj") == true)
            assertEquals(8, note.sourceLocation?.line, "the note points to the common unmarked Marker overload")
        }
    }

    @Test
    fun `direct conflict and instantiated overload ambiguity keep their own candidates`() {
        for (useLightTree in listOf(false, true)) {
            val cjo = compileCommon(
                """
                public common class ThreeSignatureCandidateBox<T> {
                    public common init()
                    public func f(value: T): Unit { }
                    public func f(value: Int64): Unit { }
                }
                """.trimIndent(),
                outputDirectoryName = "three-signature-candidates-$useLightTree",
                useLightTree = useLightTree,
            )
            val specificSource = """
                public specific class ThreeSignatureCandidateBox<U> {
                    public specific init() { }
                    public func f(value: Int64): Unit { }
                }

                public func instantiate(value: ThreeSignatureCandidateBox<Int64>): Unit { }
            """.trimIndent()
            val result = compileSpecific(specificSource, cjo, useLightTree = useLightTree)

            assertEquals(
                listOf("CONFLICTING_OVERLOADS", "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"),
                result.diagnostics.map { it.factoryName.removePrefix("CFIR_") }.sorted(),
                "the direct Int64 redeclaration and the instantiated T/Int64 ambiguity remain separate diagnostics",
            )
            val directConflict = result.diagnostics.single {
                it.factoryName.removePrefix("CFIR_") == "CONFLICTING_OVERLOADS"
            }
            assertEquals(1, directConflict.relatedInformation.size, "the direct conflict has exactly one common peer")
            val directNote = directConflict.relatedInformation.single()
            assertEquals("conflict with the declaration", directNote.message)
            assertTrue(directNote.sourceLocation?.filePath?.endsWith("common.cj") == true)
            assertEquals(6, directNote.sourceLocation?.line, "the direct conflict note points to common f(Int64)")

            val ambiguity = result.diagnostics.single {
                it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
            }
            val candidates = ambiguity.relatedInformation
            assertEquals(listOf("found candidate", "found candidate"), candidates.map { it.message })
            val commonCandidates = candidates.filter { it.sourceLocation != null }
            assertEquals(
                1,
                commonCandidates.size,
                "the ambiguity keeps common f(T), but excludes common f(Int64), already reported by the direct conflict: $candidates",
            )
            val commonCandidate = commonCandidates.single()
            assertTrue(commonCandidate.sourceLocation!!.filePath.endsWith("common.cj"))
            assertEquals(5, commonCandidate.sourceLocation!!.line, "the unpaired generic common candidate remains")
            val specificCandidates = candidates.filter { it.sourceLocation == null }
            assertEquals(1, specificCandidates.size, "the ambiguity retains the specific Int64 candidate: $candidates")
            val specificCandidate = specificCandidates.single()
            assertNotNull(specificCandidate.element, "the specific Int64 candidate remains navigable")
            assertEquals(
                "package cjmp_p\n\n".length + specificSource.indexOf("public func f") + "public func ".length,
                specificCandidate.element!!.startOffset,
                "the generic-instantiation note points to the specific f(Int64)",
            )
        }
    }

    @Test
    fun `direct constructor conflict and instantiated ambiguity keep the specific candidate`() {
        for (useLightTree in listOf(false, true)) {
            val cjo = compileCommon(
                """
                public common class ConstructorCandidateBox<T> {
                    public init(value: T) { }
                    public init(value: Int64) { }
                }
                """.trimIndent(),
                outputDirectoryName = "constructor-candidates-$useLightTree",
                useLightTree = useLightTree,
            )
            val specificSource = """
                public specific class ConstructorCandidateBox<U> {
                    public init(value: Int64) { }
                }

                public func instantiate(value: ConstructorCandidateBox<Int64>): Unit { }
            """.trimIndent()
            val result = compileSpecific(specificSource, cjo, useLightTree = useLightTree)

            assertEquals(
                listOf("CONFLICTING_OVERLOADS", "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"),
                result.diagnostics.map { it.factoryName.removePrefix("CFIR_") }.sorted(),
                "the direct constructor conflict remains separate from the instantiated T/Int64 ambiguity",
            )
            val directConflict = result.diagnostics.single {
                it.factoryName.removePrefix("CFIR_") == "CONFLICTING_OVERLOADS"
            }
            val directNote = directConflict.relatedInformation.single()
            assertEquals("conflict with the declaration", directNote.message)
            assertTrue(directNote.sourceLocation?.filePath?.endsWith("common.cj") == true)
            assertEquals(5, directNote.sourceLocation?.line, "the direct conflict note points to common init(Int64)")

            val ambiguity = result.diagnostics.single {
                it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
            }
            val candidates = ambiguity.relatedInformation
            assertEquals(listOf("found candidate", "found candidate"), candidates.map { it.message })
            val commonCandidate = candidates.single { it.sourceLocation != null }
            assertTrue(commonCandidate.sourceLocation!!.filePath.endsWith("common.cj"))
            assertEquals(4, commonCandidate.sourceLocation!!.line, "the generic common constructor remains a candidate")
            val specificCandidate = candidates.single { it.sourceLocation == null }
            assertNotNull(specificCandidate.element, "the specific constructor candidate remains navigable")
            assertEquals(
                "package cjmp_p\n\n".length + specificSource.indexOf("public init(value: Int64)") + "public ".length,
                specificCandidate.element!!.startOffset,
                "the ambiguity candidate points to the specific init(Int64)",
            )
        }
    }

    @Test
    fun `specific struct filters only paired methods and secondary constructors`() {
        for (useLightTree in listOf(false, true)) {
            val cjo = compileCommon(
                """
                public class StructSignatureGroupMarker { }

                public common struct StructSignatureGroup {
                    public common init()
                    public func f(value: Int64): Unit { }
                    public common func f(value: StructSignatureGroupMarker): Unit { }
                    public init(value: Int64) { }
                    public common init(value: StructSignatureGroupMarker) { }
                }
                """.trimIndent(),
                outputDirectoryName = "struct-signature-group-$useLightTree",
                useLightTree = useLightTree,
            )
            val result = compileSpecific(
                """
                public specific struct StructSignatureGroup {
                    public specific init() { }
                    public func f(value: Int64): Unit { }
                    public specific func f(value: StructSignatureGroupMarker): Unit { }
                    public init(value: Int64) { }
                    public specific init(value: StructSignatureGroupMarker) { }
                }
                """.trimIndent(),
                cjo,
                useLightTree = useLightTree,
            )

            assertEquals(
                listOf("CONFLICTING_OVERLOADS", "CONFLICTING_OVERLOADS"),
                result.diagnostics.map { it.factoryName.removePrefix("CFIR_") }.sorted(),
                "struct direct functions and secondary constructors both retain only their unmatched common signature conflicts",
            )
            val noteLines = result.diagnostics.map { diagnostic ->
                val note = diagnostic.relatedInformation.single()
                assertEquals("conflict with the declaration", note.message)
                assertTrue(note.sourceLocation?.filePath?.endsWith("common.cj") == true)
                note.sourceLocation!!.line
            }.toSet()
            assertEquals(
                setOf(7, 9),
                noteLines,
                "notes identify the common unmarked Int64 method and secondary constructor",
            )
        }
    }

    @Test
    fun `specific enum filters only its paired direct method`() {
        for (useLightTree in listOf(false, true)) {
            val cjo = compileCommon(
                """
                public class EnumSignatureGroupMarker { }

                public common enum EnumSignatureGroup {
                    | Empty
                    public func f(value: Int64): Unit { }
                    public common func f(value: EnumSignatureGroupMarker): Unit { }
                }
                """.trimIndent(),
                outputDirectoryName = "enum-signature-group-$useLightTree",
                useLightTree = useLightTree,
            )
            val result = compileSpecific(
                """
                public specific enum EnumSignatureGroup {
                    | Empty
                    public func f(value: Int64): Unit { }
                    public specific func f(value: EnumSignatureGroupMarker): Unit { }
                }
                """.trimIndent(),
                cjo,
                useLightTree = useLightTree,
            )

            assertEquals(
                listOf("CONFLICTING_OVERLOADS"),
                result.diagnostics.map { it.factoryName.removePrefix("CFIR_") },
                "enum direct methods share the class-like signature filter while enum variants stay out of this group",
            )
            val note = result.diagnostics.single().relatedInformation.single()
            assertEquals("conflict with the declaration", note.message)
            assertTrue(note.sourceLocation?.filePath?.endsWith("common.cj") == true)
            assertEquals(7, note.sourceLocation?.line, "the note points to the common unmarked Int64 method")
        }
    }

    @Test
    fun `specific overload group with partial pairing keeps conflict and unmatched diagnostics`() {
        for (useLightTree in listOf(false, true)) {
            val cjo = compileCommon(
                """
                public class PartialSignatureGroupMarker { }
                public class PartialSignatureGroupUnmatchedMarker { }

                public common class PartialSignatureGroupBox {
                    public common init()
                    public common func f(value: PartialSignatureGroupMarker): Unit { }
                    public func f(value: Int64): Unit { }
                }
                """.trimIndent(),
                outputDirectoryName = "partial-signature-group-$useLightTree",
                useLightTree = useLightTree,
            )
            val result = compileSpecific(
                """
                public specific class PartialSignatureGroupBox {
                    public specific init() { }
                    public specific func f(value: PartialSignatureGroupMarker): Unit { }
                    public specific func f(value: PartialSignatureGroupUnmatchedMarker): Unit { }
                    public func f(value: Int64): Unit { }
                }
                """.trimIndent(),
                cjo,
                useLightTree = useLightTree,
            )

            assertEquals(
                listOf("CONFLICTING_OVERLOADS", "NOT_MATCHED"),
                result.diagnostics.map { it.factoryName.removePrefix("CFIR_") }.sorted(),
                "a paired specific overload must not suppress the unrelated Int64 conflict or unmatched specific overload",
            )
            val overload = result.diagnostics.single { it.factoryName.removePrefix("CFIR_") == "CONFLICTING_OVERLOADS" }
            val note = overload.relatedInformation.single()
            assertEquals("conflict with the declaration", note.message)
            assertTrue(note.sourceLocation?.filePath?.endsWith("common.cj") == true)
            assertEquals(9, note.sourceLocation?.line, "the note points to the common unmarked Int64 overload")
            val notMatched = result.diagnostics.single { it.factoryName.removePrefix("CFIR_") == "NOT_MATCHED" }
            assertTrue(notMatched.relatedInformation.isEmpty(), "the unmatched specific overload has no common peer note")
        }
    }

    @Test
    fun `same signature conflict reports every specific and common peer`() {
        for (useLightTree in listOf(false, true)) {
            val cjo = compileCommon(
                """
                public common class TripleSignatureGroupBox {
                    public common init()
                    public func f(value: Int64): Unit { }
                }
                """.trimIndent(),
                outputDirectoryName = "triple-signature-group-$useLightTree",
                useLightTree = useLightTree,
            )
            val specificSource = """
                public specific class TripleSignatureGroupBox {
                    public specific init() { }
                    public func f(value: Int64): Unit { }
                    public func f(value: Int64): Unit { }
                }
            """.trimIndent()
            val result = compileSpecific(specificSource, cjo, useLightTree = useLightTree)

            assertEquals(
                listOf("CONFLICTING_OVERLOADS"),
                result.diagnostics.map { it.factoryName.removePrefix("CFIR_") },
                "the three same-signature declarations form one overload conflict",
            )
            val diagnostic = result.diagnostics.single()
            val notes = diagnostic.relatedInformation
            assertEquals(2, notes.size, "the later specific function retains both conflicting peers")
            assertEquals(setOf("conflict with the declaration"), notes.map { it.message }.toSet())

            val commonPeer = notes.single { it.sourceLocation != null }
            assertTrue(commonPeer.sourceLocation!!.filePath.endsWith("common.cj"))
            assertEquals(5, commonPeer.sourceLocation!!.line)
            val specificPeer = notes.single { it.sourceLocation == null }
            assertNotNull(specificPeer.element, "the other specific declaration remains navigable")
            assertEquals(
                "package cjmp_p\n\n".length + specificSource.indexOf("public func f") + "public func ".length,
                specificPeer.element!!.startOffset,
                "the peer note points to the first specific overload name",
            )
        }
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

        assertFalse(result.completed, "the production post-condition rejects the incompatible inferred return type")
        assertEquals(listOf(true to true), result.deserializedCallableRoles["inferredMismatch"], "the common CJO was loaded before the return-type failure")
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
    fun `generic nominal overload diagnostics retain only the unpaired common candidate`() {
        val commonSource = """
            public common class CandidateBox<T> {
                public common init()
                public common func f(value: T): Unit { }
                public common func f(value: CandidateMarker): Unit { }
            }

            public class CandidateMarker { }
        """.trimIndent()
        val cjo = compileCommon(commonSource)
        val specificSource = """
            public specific class CandidateBox<U> {
                public specific init() { }
                public specific func f(value: U): Unit { }
            }

            public func instantiateString(value: CandidateBox<CandidateMarker>): Unit { }
            public func instantiateInt64(value: CandidateBox<Int64>): Unit { }
        """.trimIndent()
        val result = compileSpecific(specificSource, cjo)
        val ambiguities = result.diagnostics.filter {
            it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
        }

        assertEquals(1, ambiguities.size, "only CandidateBox<CandidateMarker> instantiates the overload collision")
        val notes = ambiguities.single().relatedInformation
        assertEquals(listOf("found candidate", "found candidate"), notes.map { it.message })
        val commonCandidate = notes.single { it.sourceLocation != null }
        assertEquals("common.cj", Path.of(commonCandidate.sourceLocation!!.filePath).fileName.toString())
        assertEquals(6, commonCandidate.sourceLocation!!.line, "the matched common f(T) is filtered from the candidates")
        val specificCandidate = notes.single { it.sourceLocation == null }
        assertNotNull(specificCandidate.element)
        assertEquals(
            "package cjmp_p\n\n".length + specificSource.indexOf("specific func f") + "specific func ".length,
            specificCandidate.element!!.startOffset,
            "the specific generic member is the second reported candidate",
        )
        assertTrue(ambiguities.single().renderMessage().contains("note: found candidate"))
    }

    @Test
    fun `generic instantiation retains unmatched common and specific same name candidates`() {
        val commonSource = """
            public common class UnmatchedCandidateBox<T> {
                public common init()
                public func f(value: T): Unit { }
                public func f(value: Bool): Unit { }
            }

            public class UnmatchedCandidateMarker { }
        """.trimIndent()
        val cjo = compileCommon(commonSource)
        val specificSource = """
            public specific class UnmatchedCandidateBox<U> {
                public specific init() { }
                public specific func f(value: UnmatchedCandidateMarker): Unit { }
            }

            public func instantiateString(value: UnmatchedCandidateBox<UnmatchedCandidateMarker>): Unit { }
            public func instantiateInt64(value: UnmatchedCandidateBox<Int64>): Unit { }
        """.trimIndent()
        val result = compileSpecific(specificSource, cjo)
        val ambiguities = result.diagnostics.filter {
            it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
        }

        assertEquals(1, ambiguities.size, "only the marker instantiation has equal f signatures")
        val notes = ambiguities.single().relatedInformation
        assertEquals(listOf("found candidate", "found candidate"), notes.map { it.message })
        val commonCandidate = notes.single { it.sourceLocation != null }
        assertEquals(5, commonCandidate.sourceLocation!!.line, "the unpaired common f(T) remains a candidate")
        val specificCandidate = notes.single { it.sourceLocation == null }
        assertNotNull(specificCandidate.element)
        assertEquals(
            "package cjmp_p\n\n".length + specificSource.indexOf("specific func f") + "specific func ".length,
            specificCandidate.element!!.startOffset,
            "the unpaired specific overload remains a candidate",
        )
        assertTrue(ambiguities.single().renderMessage().contains("note: found candidate"))
    }

    @Test
    fun `generic extend overload diagnostics retain only the unpaired common candidate`() {
        val commonSource = """
            public common class ExtendCandidateBox<T> {
                public common init()
            }

            common extend<T> ExtendCandidateBox<T> {
                public common func g(value: T): Unit { }
                public common func g(value: ExtendCandidateMarker): Unit { }
            }

            public class ExtendCandidateMarker { }
        """.trimIndent()
        val cjo = compileCommon(commonSource)
        val specificSource = """
            public specific class ExtendCandidateBox<U> {
                public specific init() { }
            }

            specific extend<V> ExtendCandidateBox<V> {
                public specific func g(value: V): Unit { }
            }

            public func instantiateString(value: ExtendCandidateBox<ExtendCandidateMarker>): Unit { }
            public func instantiateInt64(value: ExtendCandidateBox<Int64>): Unit { }
        """.trimIndent()
        val result = compileSpecific(specificSource, cjo)
        val ambiguities = result.diagnostics.filter {
            it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
        }

        assertEquals(1, ambiguities.size, "only ExtendCandidateBox<ExtendCandidateMarker> instantiates the overload collision")
        val notes = ambiguities.single().relatedInformation
        assertEquals(listOf("found candidate", "found candidate"), notes.map { it.message })
        val commonCandidate = notes.single { it.sourceLocation != null }
        assertEquals(9, commonCandidate.sourceLocation!!.line, "the common g(T) counterpart is filtered")
        val specificCandidate = notes.single { it.sourceLocation == null }
        assertNotNull(specificCandidate.element)
        assertEquals(
            "package cjmp_p\n\n".length + specificSource.indexOf("specific func g") + "specific func ".length,
            specificCandidate.element!!.startOffset,
            "the specific generic extend member is the second reported candidate",
        )
        assertTrue(ambiguities.single().renderMessage().contains("note: found candidate"))
    }

    @Test
    fun `generic extend instantiation retains unmatched common and specific candidates`() {
        val commonSource = """
            public common class UnmatchedExtendBox<T> {
                public common init()
            }

            common extend<T> UnmatchedExtendBox<T> {
                public func g(value: T): Unit { }
                public func g(value: Bool): Unit { }
            }

            public class UnmatchedExtendMarker { }
        """.trimIndent()
        val cjo = compileCommon(commonSource)
        val specificSource = """
            public specific class UnmatchedExtendBox<U> {
                public specific init() { }
            }

            specific extend<V> UnmatchedExtendBox<V> {
                public specific func g(value: UnmatchedExtendMarker): Unit { }
            }

            public func instantiateMarker(value: UnmatchedExtendBox<UnmatchedExtendMarker>): Unit { }
            public func instantiateInt64(value: UnmatchedExtendBox<Int64>): Unit { }
        """.trimIndent()
        val result = compileSpecific(specificSource, cjo)
        val ambiguities = result.diagnostics.filter {
            it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
        }

        assertEquals(1, ambiguities.size, "only the marker instantiation has equal g signatures")
        val notes = ambiguities.single().relatedInformation
        assertEquals(listOf("found candidate", "found candidate"), notes.map { it.message })
        val commonCandidate = notes.single { it.sourceLocation != null }
        assertEquals(8, commonCandidate.sourceLocation!!.line, "the unpaired common g(T) remains a candidate")
        val specificCandidate = notes.single { it.sourceLocation == null }
        assertNotNull(specificCandidate.element)
        assertEquals(
            "package cjmp_p\n\n".length + specificSource.indexOf("specific func g") + "specific func ".length,
            specificCandidate.element!!.startOffset,
            "the unpaired specific extend member remains a candidate",
        )
        assertTrue(ambiguities.single().renderMessage().contains("note: found candidate"))
    }

    @Test
    fun `generic instantiation filters a matched common extend owner and retains an unmatched peer owner`() {
        val commonSource = """
            public common class MultiOwnerExtendBox<T> {
                public common init()
            }

            common extend<T> MultiOwnerExtendBox<T> {
                public common func g(value: T): Unit { }
            }

            common extend<T> MultiOwnerExtendBox<T> {
                public func g(value: MultiOwnerExtendMarker): Unit { }
            }

            public class MultiOwnerExtendMarker { }
        """.trimIndent()
        val cjo = compileCommon(commonSource)
        val specificSource = """
            public specific class MultiOwnerExtendBox<U> {
                public specific init() { }
            }

            specific extend<V> MultiOwnerExtendBox<V> {
                public specific func g(value: V): Unit { }
            }

            public func instantiateMarker(value: MultiOwnerExtendBox<MultiOwnerExtendMarker>): Unit { }
            public func instantiateInt64(value: MultiOwnerExtendBox<Int64>): Unit { }
        """.trimIndent()
        val result = compileSpecific(specificSource, cjo)
        assertEquals(
            listOf("GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"),
            result.diagnostics.map { it.factoryName.removePrefix("CFIR_") },
            "the paired common owner does not produce a declaration conflict or an extra candidate group",
        )
        val ambiguities = result.diagnostics.filter {
            it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
        }

        assertEquals(1, ambiguities.size, "only MultiOwnerExtendBox<MultiOwnerExtendMarker> has an unpaired collision")
        val notes = ambiguities.single().relatedInformation
        assertEquals(listOf("found candidate", "found candidate"), notes.map { it.message })
        val commonCandidate = notes.single { it.sourceLocation != null }
        assertEquals("common.cj", Path.of(commonCandidate.sourceLocation!!.filePath).fileName.toString())
        assertEquals(12, commonCandidate.sourceLocation!!.line, "the unmatched second common extend remains a candidate")
        val specificCandidate = notes.single { it.sourceLocation == null }
        assertNotNull(specificCandidate.element)
        assertEquals(
            "package cjmp_p\n\n".length + specificSource.indexOf("specific func g") + "specific func ".length,
            specificCandidate.element!!.startOffset,
            "the specific generic extend member is the second reported candidate",
        )
        assertTrue(ambiguities.single().renderMessage().contains("note: found candidate"))
    }

    @Test
    fun `generic instantiation retains common extend peer outside its paired specific owner group`() {
        val commonSource = """
            public common class AsymmetricExtendOwnerBox<T> {
                public common init()
            }

            public class AsymmetricExtendOwnerWrapper<T> { }

            common extend<T> AsymmetricExtendOwnerBox<AsymmetricExtendOwnerWrapper<T>> {
                public common func g(value: T): Unit { }
            }

            common extend<U> AsymmetricExtendOwnerBox<U> {
                public func g(value: AsymmetricExtendOwnerMarker): Unit { }
                public common func h(value: U): Unit { }
            }

            public class AsymmetricExtendOwnerMarker { }
        """.trimIndent()
        val cjo = compileCommon(commonSource)
        val specificSource = """
            public specific class AsymmetricExtendOwnerBox<V> {
                public specific init() { }
            }

            specific extend<V> AsymmetricExtendOwnerBox<AsymmetricExtendOwnerWrapper<V>> {
                public specific func g(value: V): Unit { }
            }

            specific extend<W> AsymmetricExtendOwnerBox<W> {
                public specific func h(value: W): Unit { }
            }

            public func instantiateMarker(value: AsymmetricExtendOwnerBox<AsymmetricExtendOwnerWrapper<AsymmetricExtendOwnerMarker>>): Unit { }
            public func instantiateInt64(value: AsymmetricExtendOwnerBox<AsymmetricExtendOwnerWrapper<Int64>>): Unit { }
        """.trimIndent()
        val result = compileSpecific(specificSource, cjo)

        assertEquals(
            listOf("GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"),
            result.diagnostics.map { it.factoryName.removePrefix("CFIR_") },
            "only the nested marker instantiation collides with the unmatched common peer",
        )
        val notes = result.diagnostics.single().relatedInformation
        assertEquals(listOf("found candidate", "found candidate"), notes.map { it.message })
        val commonCandidate = notes.single { it.sourceLocation != null }
        assertEquals("common.cj", Path.of(commonCandidate.sourceLocation!!.filePath).fileName.toString())
        assertEquals(14, commonCandidate.sourceLocation!!.line, "the wide common peer remains in the narrow owner's group")
        val specificCandidate = notes.single { it.sourceLocation == null }
        assertNotNull(specificCandidate.element)
        assertEquals(
            "package cjmp_p\n\n".length + specificSource.indexOf("specific func g") + "specific func ".length,
            specificCandidate.element!!.startOffset,
            "the narrow specific owner contributes its own generic candidate",
        )
    }

    @Test
    fun `generic instantiation collapses inherited matched common and specific candidates`() {
        val commonSource = """
            public common interface Parent<T> {
                public common func f(value: T): Unit { }
                public common func f(value: ParentMarker): Unit { }
            }

            public common class Child<T> <: Parent<T> {
                public common init()
            }

            public class ParentMarker { }
        """.trimIndent()
        val cjo = compileCommon(commonSource)
        val specificSource = """
            public specific interface Parent<U> {
                public specific func f(value: U): Unit { }
            }

            public specific class Child<V> <: Parent<V> {
                public specific init() { }
            }

            public func instantiateMarker(value: Child<ParentMarker>): Unit { }
            public func instantiate(value: Child<Int64>): Unit { }
        """.trimIndent()
        val result = compileSpecific(specificSource, cjo)
        val ambiguities = result.diagnostics.filter {
            it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
        }
        assertEquals(1, ambiguities.size, "only Child<ParentMarker> keeps the unpaired common overload")
        val notes = ambiguities.single().relatedInformation
        assertEquals(listOf("found candidate", "found candidate"), notes.map { it.message })
        val commonCandidate = notes.single { it.sourceLocation != null }
        assertEquals(5, commonCandidate.sourceLocation!!.line, "the unpaired Parent.f(ParentMarker) remains")
        val specificCandidate = notes.single { it.sourceLocation == null }
        assertNotNull(specificCandidate.element)
        assertEquals(
            "package cjmp_p\n\n".length + specificSource.indexOf("specific func f") + "specific func ".length,
            specificCandidate.element!!.startOffset,
            "the inherited specific Parent.f(U) remains while paired common Parent.f(T) is filtered",
        )
    }

    @Test
    fun `generic instantiation filters matched common constructor after substitution`() {
        val commonSource = """
            public common class ConstructorBox<T> {
                public common init(value: T) { }
                public common init(value: ConstructorMarker) { }
            }

            public class ConstructorMarker { }
        """.trimIndent()
        val cjo = compileCommon(commonSource)
        val specificSource = """
            public specific class ConstructorBox<U> {
                public specific init(value: U) { }
            }

            public func instantiateMarker(value: ConstructorBox<ConstructorMarker>): Unit { }
            public func instantiateInt64(value: ConstructorBox<Int64>): Unit { }
        """.trimIndent()
        val result = compileSpecific(specificSource, cjo)
        val ambiguities = result.diagnostics.filter {
            it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
        }

        assertEquals(1, ambiguities.size, "only ConstructorBox<ConstructorMarker> instantiates the collision")
        val notes = ambiguities.single().relatedInformation
        assertEquals(listOf("found candidate", "found candidate"), notes.map { it.message })
        val commonCandidate = notes.single { it.sourceLocation != null }
        assertEquals("common.cj", Path.of(commonCandidate.sourceLocation!!.filePath).fileName.toString())
        assertEquals(5, commonCandidate.sourceLocation!!.line, "the unpaired common constructor remains")
        val specificCandidate = notes.single { it.sourceLocation == null }
        assertNotNull(specificCandidate.element)
        assertEquals(
            "package cjmp_p\n\n".length + specificSource.indexOf("specific init") + "specific ".length,
            specificCandidate.element!!.startOffset,
            "the specific constructor remains while its matched generic common constructor is filtered",
        )
    }

    @Test
    fun `unpaired subtype observes common inherited default after cjo loading`() {
        val cjo = compileCommon(
            """
            public common interface I<T> {
                public common func f(value: T): Unit { }
            }
            """.trimIndent(),
        )
        val specificSource = """
            public specific interface I<U> { }

            public class D<V> <: I<V> {
                public init() { }
                public func f(value: Int64): Unit { }
            }

            public func trigger(value: D<Int64>): Unit { }
        """.trimIndent()
        val result = compileSpecific(
            specificSource,
            cjo,
        )

        val ambiguities = result.diagnostics.filter {
            it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
        }
        assertEquals(
            1,
            ambiguities.size,
            "the unpaired D<Int64> sees the matched common interface default through its CJO supertype: " +
                    "diagnostics=${result.diagnostics.map { it.factoryName to it.renderMessage() }}, " +
                    "messages=${result.messages}, nominalMappings=${result.cjmpNominalMappings}",
        )
        val ambiguity = ambiguities.single()
        assertEquals(
            listOf("found candidate", "found candidate"),
            ambiguity.relatedInformation.map { it.message },
            "both the common inherited default and the specific direct member remain candidates",
        )
        val commonCandidate = ambiguity.relatedInformation.single { it.sourceLocation != null }
        assertTrue(
            commonCandidate.sourceLocation!!.filePath.endsWith("common.cj"),
            "the inherited default note points to its CJO source",
        )
        assertEquals(4, commonCandidate.sourceLocation!!.line)
        assertEquals(24, commonCandidate.sourceLocation!!.column, "the CJO note points to the common function name")
        val specificCandidate = ambiguity.relatedInformation.single { it.sourceLocation == null }
        assertNotNull(specificCandidate.element, "the direct subtype candidate retains a PSI source range")
        assertEquals(
            "package cjmp_p\n\n".length + specificSource.indexOf("func f(value: Int64)") + "func ".length,
            specificCandidate.element!!.startOffset,
            "the direct subtype candidate points to its function name",
        )
        assertTrue(ambiguity.renderMessage().contains("note: found candidate"))
    }

    @Test
    fun `deserialized generic class constructors participate in instantiated member checking`() {
        val ownerLibrary = """
            public class ConstructorBox<T> {
                public init(value: T) { }
                public init(value: Int64) { }
            }
        """.trimIndent()
        val consumerSource = """
            import constructorlib.*

            func trigger(box: ConstructorBox<Int64>): Unit { }
        """.trimIndent()
        val libraryCjo = compileNormalCjoLibrary(ownerLibrary, "constructorlib")
        val consumer = compileClasspathSource(consumerSource, "constructorclient", libraryCjo)

        val ambiguities = consumer.diagnostics.filter {
            it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
        }
        assertEquals(
            1,
            ambiguities.size,
            "the CJO constructor signatures collide at ConstructorBox<Int64>: " +
                consumer.diagnostics.map { it.factoryName to it.renderMessage() },
        )
        val ambiguity = ambiguities.single()
        assertEquals(
            listOf("found candidate", "found candidate"),
            ambiguity.relatedInformation.map { it.message },
            "both instantiated constructors remain visible as CJO candidate notes",
        )
        val candidateSourceLocations = ambiguity.relatedInformation.map { related ->
            val location = related.sourceLocation
            assertNotNull(location, "each deserialized constructor note retains its CJO source position")
            val sourceLocation = location!!
            assertTrue(
                sourceLocation.filePath.endsWith("library.cj"),
                "constructor note points into the CJO library source: $sourceLocation",
            )
            sourceLocation.line to sourceLocation.column
        }
        assertEquals(
            setOf(4 to 12, 5 to 12),
            candidateSourceLocations.toSet(),
            "constructor notes point to the init identifiers in library.cj",
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
        assertTrue("Bound" in upperBoundNames, "ordinary CJO retains the actual interface bound: $upperBoundNames")
        assertEquals(
            1,
            boundedConstraint.uppersLength,
            "the CJO contains only the explicit where-bound: $upperBoundNames",
        )
        assertFalse(
            loadedBoundedPeer!!.typeParameterUpperBounds.flatten().any { bound ->
                bound.classIdOrPrimitiveClassId == StdlibClassIds.Any
            },
            "a CJO type parameter with an explicit upper bound does not regain a redundant implicit Any",
        )
        val loadedUnboundedPeer = unsatisfiedConsumer.deserializedExtendFacts.singleOrNull { fact ->
            fact.packageName == "ownermatrixlib" && fact.targetType.contains("ChainOnlyBox")
        }
        assertNotNull(loadedUnboundedPeer, "the consumer recovers the unbounded generic CJO extend")
        assertTrue(
            loadedUnboundedPeer!!.typeParameterUpperBounds.flatten().any { bound ->
                bound.classIdOrPrimitiveClassId == StdlibClassIds.Any
            },
            "an omitted CJO upper-bound list restores CFIR's default Any bound",
        )
        val satisfiedConsumer = compileClasspathSource(consumerSource, "ownermatrixclient", satisfiedLibrary)
        assertTrue(
            satisfiedConsumer.diagnostics.count {
                it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
            } == 1,
            "the satisfied generic bound makes the inherited default and private peer ambiguous after CJO deserialization: " +
                "${satisfiedConsumer.diagnostics.map { it.factoryName to it.renderMessage() }}",
        )
        val ambiguity = satisfiedConsumer.diagnostics.single {
            it.factoryName.removePrefix("CFIR_") == "GENERIC_INSTANTIATION_CAUSES_AMBIGUOUS_FUNCTIONS"
        }
        assertEquals(
            listOf("found candidate", "found candidate"),
            ambiguity.relatedInformation.map { it.message },
            "the diagnostic retains both effective candidates as CJO notes",
        )
        val candidateSourceLocations = ambiguity.relatedInformation.map { related ->
            val location = related.sourceLocation
            assertNotNull(location, "each deserialized candidate note retains its CJO source position")
            val sourceLocation = location!!
            assertTrue(
                sourceLocation.filePath.endsWith("library.cj"),
                "candidate note points into the CJO library source: $sourceLocation",
            )
            sourceLocation.line to sourceLocation.column
        }
        assertEquals(
            setOf(15 to 17, 19 to 18),
            candidateSourceLocations.toSet(),
            "candidate notes point to the function identifiers serialized in CJO",
        )
        assertTrue(ambiguity.renderMessage().contains("note: found candidate"))
    }

    @Test
    fun `deserialized common supertypes compare mapped types independently of interface order`() {
        val common = """
            public interface I {}
            public interface Other {}
            public interface Generic<T> {}
            public open class BaseA { public init() {} }
            public open class BaseB { public init() {} }
            public common class DifferentInterface <: I { public common init() }
            public common class DifferentArgument <: Generic<Int64> { public common init() }
            public common class RenamedParameter<T> <: Generic<T> { public common init() }
            public common class ReorderedInterfaces <: I & Other { public common init() }
            public common class DifferentSuperclass <: BaseA { public common init() }
        """.trimIndent()
        val specific = """
            public specific class DifferentInterface <: Other { public specific init() {} }
            public specific class DifferentArgument <: Generic<Bool> { public specific init() {} }
            public specific class RenamedParameter<U> <: Generic<U> { public specific init() {} }
            public specific class ReorderedInterfaces <: Other & I { public specific init() {} }
            public specific class DifferentSuperclass <: BaseB { public specific init() {} }
        """.trimIndent()
        for (useLightTree in listOf(false, true)) {
            val cjo = compileCommon(common, outputDirectoryName = "supertypes-$useLightTree", useLightTree = useLightTree)
            val result = compileSpecific(specific, cjo, useLightTree = useLightTree)
            assertEquals(
                List(3) { "CFIR_SPECIFIC_HAS_DIFFERENT_SUPER_TYPE" },
                result.diagnostics.filter { it.severity.isError }.map { it.factoryName },
                "CJO types retain generic arguments and accept parameter renaming and interface permutations; LightTree=$useLightTree",
            )
            assertFalse(result.completed)
        }
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
        val diagnostic = result.diagnostics.single()
        assertEquals("CFIR_NOT_MATCHED", diagnostic.factoryName)
        assertTrue(diagnostic.severity.isError)
        assertEquals("'common' function 'b' can not find 'specific' match", diagnostic.renderMessage())
        val binarySource = assertInstanceOf(
            CfirCjoDeclarationSourceElement::class.java,
            (diagnostic as CjDiagnosticWithSource).element,
        )
        assertTrue(binarySource.sourceLocation?.filePath?.endsWith("common.cj") == true)
        assertEquals(4, binarySource.sourceLocation?.line)
        assertEquals(1, binarySource.sourceLocation?.column)

        // cjc 1.1.3 实测：`'common' function 'b' can not find 'specific' match ==> common.cj:4:1`。
        val index = result.messages.indexOfFirst { it.second == diagnostic.renderMessage() }
        assertTrue(index >= 0, "the structured common-side diagnostic is rendered once for CLI: ${result.messages}")
        assertEquals(1, result.messages.count { it.second == diagnostic.renderMessage() })
        assertEquals(CompilerMessageSeverity.ERROR, result.messages[index].first)
        val location = result.locations[index]
        assertNotNull(location, "diagnostic carries the common source location")
        location!!
        assertTrue(location.path.endsWith("common.cj"), "anchored in the common source file: ${location.path}")
        assertEquals(4, location.line)
        assertEquals(1, location.column)
    }

    @Test
    fun `multiple implementations of deserialized common declaration use structured diagnostic`() {
        val cjo = compileCommon("public common func f(): Int64")
        val result = compileSpecific(
            """
            public specific func f(): Int64 { 1 }
            public specific func f(): Int64 { 2 }
            public specific func f(): Int64 { 3 }
            """.trimIndent(),
            cjo,
        )

        val diagnostic = result.diagnostics.single { it.factoryName == "CFIR_MULTIPLE_COMMON_IMPLEMENTATIONS" }
        assertTrue(diagnostic.severity.isError)
        assertEquals("'common' function has several specific implementations", diagnostic.renderMessage())
        val binarySource = assertInstanceOf(
            CfirCjoDeclarationSourceElement::class.java,
            (diagnostic as CjDiagnosticWithSource).element,
        )
        val sourceLocation = binarySource.sourceLocation
        assertNotNull(sourceLocation)
        assertTrue(sourceLocation!!.filePath.endsWith("common.cj"))
        assertEquals(3, sourceLocation.line)
        assertEquals(1, sourceLocation.column)

        val cliDiagnostic = result.messages.single { it.second == diagnostic.renderMessage() }
        assertEquals(CompilerMessageSeverity.ERROR, cliDiagnostic.first)
        val cliLocation = result.locations[result.messages.indexOf(cliDiagnostic)]
        assertNotNull(cliLocation)
        assertTrue(cliLocation!!.path.endsWith("common.cj"))
        assertEquals(3, cliLocation.line)
        assertEquals(1, cliLocation.column)
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
            result.diagnostics.any {
                it.factoryName == "CFIR_CJMP_COMMON_CJO_PACKAGE_MISMATCH" && it.severity.isError
            },
            "the shared CFIR checker reports the wrong-package load fact: ${result.diagnostics}",
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
            languageVersion = "1.3.0-alpha.05",
        )

        assertFalse(result.completed, "an unsupported common cjo version aborts the frontend pipeline: ${result.messages}")
        assertTrue(
            result.diagnostics.any {
                it.factoryName == "CFIR_CJMP_COMMON_CJO_FORMAT_VERSION" && it.severity.isError
            },
            "the shared CFIR checker reports the cjo-version load fact: ${result.diagnostics}",
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
            languageVersion = "1.2.0-alpha.20",
        )

        assertFalse(result.completed, "a feature-set mismatch aborts the frontend pipeline: ${result.messages}")
        assertTrue(
            result.diagnostics.any {
                it.factoryName == "CFIR_CJMP_COMMON_CJO_FEATURES_NOT_SUBSET" && it.severity.isError
            },
            "the shared CFIR checker reports the feature-subset load fact: ${result.diagnostics}",
        )
    }

    @Test
    fun `specific feature set is collected from raw CFIR in both frontend modes`() {
        val commonCjo = compileCommon(commonSource, packageFeatures = setOf("sample.common"))
        for (lightTree in listOf(false, true)) {
            val specificSource = writeSpecific(
                "public specific func platform(): Int64 { 0 }",
                features = setOf("sample.common"),
            )
            val result = compile(specificSource) {
                useLightTree = lightTree
                assertTrue(configureLanguageVersionSettings("1.2.0-alpha.20"))
                configureCjmpCommonPartInputs(listOf(commonCjo.toString()), emptyList())
            }

            assertTrue(
                result.completed,
                "raw CFIR features must reach the common-CJO subset gate in LightTree=$lightTree: " +
                    "messages=${result.messages}, diagnostics=${result.diagnostics}",
            )
            assertFalse(
                result.diagnostics.any { it.factoryName == "CFIR_CJMP_COMMON_CJO_FEATURES_NOT_SUBSET" },
                "the parsed specific feature must satisfy the common feature set in LightTree=$lightTree",
            )
        }
    }

    @Test
    fun `specific features stay scoped to their source package`() {
        val alphaCommonCjo = compileCommon(
            content = "public common func alpha(): Int64",
            packageName = "feature_alpha",
            outputDirectoryName = "feature-alpha-common-output",
        )
        val betaCommonCjo = compileCommon(
            content = "public common func beta(): Int64",
            packageName = "feature_beta",
            packageFeatures = setOf("feature.required_by_beta"),
            outputDirectoryName = "feature-beta-common-output",
        )
        val sourceRoot = Files.createDirectories(tempDir.resolve("specific-package-features"))
        val alphaSource = sourceRoot.resolve("alpha.cj")
        Files.writeString(
            alphaSource,
            "features { feature.required_by_beta }\n\n" +
                "package feature_alpha\n\n" +
                "public specific func alpha(): Int64 { 0 }\n",
        )
        val betaSource = sourceRoot.resolve("beta.cj")
        Files.writeString(
            betaSource,
            "package feature_beta\n\n" +
                "public specific func beta(): Int64 { 0 }\n",
        )

        for (lightTree in listOf(false, true)) {
            val result = compile(alphaSource) {
                useLightTree = lightTree
                addCangJieSourceRoot(betaSource.toString())
                assertTrue(configureLanguageVersionSettings("1.2.0-alpha.20"))
                configureCjmpCommonPartInputs(
                    listOf(alphaCommonCjo.toString(), betaCommonCjo.toString()),
                    emptyList(),
                )
            }

            val featureDiagnostics = result.diagnostics.filter {
                it.factoryName == "CFIR_CJMP_COMMON_CJO_FEATURES_NOT_SUBSET"
            }
            assertEquals(
                1,
                featureDiagnostics.size,
                "only feature_beta lacks its own required feature in LightTree=$lightTree: " +
                    "messages=${result.messages}, diagnostics=${result.diagnostics}",
            )
            assertFalse(
                result.completed,
                "the package-local feature mismatch must reject the frontend pipeline in LightTree=$lightTree",
            )
        }
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
            languageVersion = "1.2.0-alpha.20",
        )

        assertFalse(result.completed, "a debug-option mismatch aborts the frontend pipeline: ${result.messages}")
        assertTrue(
            result.diagnostics.any {
                it.factoryName == "CFIR_CJMP_COMMON_CJO_DEBUG_MISMATCH" && it.severity.isError
            },
            "the shared CFIR checker reports the options mismatch load fact: ${result.diagnostics}",
        )
    }

    @Test
    fun `alpha nineteen common cjo keeps the legacy features and options loading behavior`() {
        val cjo = compileCommon(
            commonSource,
            packageFeatures = setOf("sample.common"),
            moduleDebug = true,
        )
        val result = compileSpecific(
            content = "public specific func platform(): Int64 { 0 }",
            commonCjo = cjo,
            packageFeatures = setOf("sample.specific"),
            moduleDebug = false,
            languageVersion = "1.2.0-alpha.19",
        )

        assertTrue(result.completed, "alpha.19 predates common CJO feature/options validation: ${result.messages}")
        assertFalse(result.messages.any { it.first.isError }, "legacy loading has no gate errors: ${result.messages}")
        assertFalse(
            result.messages.any { it.second.contains("feature", ignoreCase = true) || it.second.contains("debug") },
            "alpha.19 does not expose alpha.20 common CJO gate diagnostics: ${result.messages}",
        )
    }

    @Test
    fun `ordinary classpath CJMP packages do not run common-part load gates`() {
        val commonLibraryCjo = compileCommon(
            content = "public common func commonLibraryValue(): Int64 { 1 }",
            packageName = "regular_cjmp_library",
            packageFeatures = setOf("library.only"),
            moduleDebug = true,
            moduleOptLevel = CfirCjmpOptimizationLevel.O2,
            outputDirectoryName = "regular-cjmp-library-output",
        )
        val commonLibraryHeader = CjoPackageHeader.fromPackage(
            Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(commonLibraryCjo))),
        )
        assertTrue(commonLibraryHeader.isCjmpCommonPart, "the common library carries CJMP metadata")
        assertEquals(true, commonLibraryHeader.options?.debug)
        assertEquals(2u.toUByte(), commonLibraryHeader.options?.optLevel)
        assertEquals(listOf(listOf("library", "only")), commonLibraryHeader.fileFeatures.values.flatten())

        val specificLibraryCommonCjo = compileCommon(
            content = "public common func specificLibraryValue(): Int64",
            packageName = "specific_cjmp_library",
            outputDirectoryName = "specific-cjmp-library-common-output",
        )
        val specificLibraryOutput = Files.createDirectories(tempDir.resolve("specific-cjmp-library-output"))
        val specificLibraryBuild = compileSpecific(
            content = "public specific func specificLibraryValue(): Int64 { 2 }",
            commonCjo = specificLibraryCommonCjo,
            packageName = "specific_cjmp_library",
            outputDirectory = specificLibraryOutput,
            languageVersion = "1.2.0-alpha.20",
        )
        assertTrue(specificLibraryBuild.completed, "specific library CJO is produced: ${specificLibraryBuild.messages}")
        val specificLibraryCjo = specificLibraryOutput.resolve("specific_cjmp_library.cjo")
        assertTrue(Files.isRegularFile(specificLibraryCjo), "specific library output exists")
        val specificLibraryHeader = CjoPackageHeader.fromPackage(
            Package.getRootAsPackage(ByteBuffer.wrap(Files.readAllBytes(specificLibraryCjo))),
        )
        assertTrue(specificLibraryHeader.isCjmpCommonPart, "SPECIFIC declaration content is not common-input identity")
        assertEquals(null, specificLibraryHeader.options, "the specific producer omits common compile options")

        val consumerCommonCjo = compileCommon(
            content = "public common func platform(): Int64",
            packageName = "cjmp_p",
            outputDirectoryName = "ordinary-library-consumer-common-output",
        )
        val consumer = compileSpecific(
            content = """
                import regular_cjmp_library.*
                import specific_cjmp_library.*

                public specific func platform(): Int64 {
                    commonLibraryValue() + specificLibraryValue()
                }
            """.trimIndent(),
            commonCjo = consumerCommonCjo,
            classpathRoots = listOf(commonLibraryCjo.parent, specificLibraryOutput),
            languageVersion = "1.2.0-alpha.20",
        )

        assertTrue(consumer.completed, "ordinary CJMP libraries load in a specific compilation: ${consumer.messages}")
        assertFalse(consumer.diagnostics.any { it.severity.isError }, "both imported library callables resolve")
        assertFalse(consumer.messages.any { it.first.isError }, "ordinary library metadata does not reject compilation")
        assertFalse(
            consumer.messages.any { (_, message) ->
                message.contains("feature", ignoreCase = true) || message.contains("options", ignoreCase = true)
            },
            "features and options gates only apply to the explicitly configured common-part CJO: ${consumer.messages}",
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
                        // CJO 的可见性是显式属性位：官方 writer 对 public 声明会写 PUBLIC，
                        // 反序列化端在缺失/冲突位时保留 UNKNOWN（不臆造 public），因此最小夹具也要带上。
                        attributes = cjmpAttributeWords(
                            Attribute.COMMON,
                            Attribute.FROM_COMMON_PART,
                            Attribute.PUBLIC,
                        ),
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
            languageVersion = "1.2.0-alpha.20",
        )

        assertTrue(result.completed, "missing options is a warning and does not reject the common part")
        assertEquals(
            listOf("CFIR_CJMP_COMMON_CJO_OPTIONS_MISSING"),
            result.diagnostics.map { it.factoryName },
            "the shared CFIR checker reports the missing-options warning",
        )
        assertFalse(result.diagnostics.single().severity.isError, "missing options is reported as a warning")
    }

    @Test
    fun `common part command line arguments require paired cjo and chir paths`() {
        val collector = RecordingMessageCollector()
        val configuration = CompilerConfiguration().apply {
            messageCollector = collector
        }
        val commonCjo = Files.createFile(tempDir.resolve("unpaired-common.cjo"))
        val arguments = CjmpArgumentProbe().apply {
            languageVersion = "1.2.0-alpha.20"
            cjmpCommonPart = arrayOf(commonCjo.toString())
            cjmpCommonPartChir = emptyArray()
        }

        val completed = CjmpArgumentProbePipeline().execute(arguments, configuration)

        assertFalse(completed)
        assertInstanceOf(CjmpCommonPartInputResolution.Invalid::class.java, configuration.cjmpCommonPartInputResolution)
        assertThrows(IllegalStateException::class.java) { configuration.requireCjmpCommonPartInputs() }
        assertTrue(
            collector.messages.any {
                it.first.isError && it.second ==
                        "common .chir files count should be equal to common .cjo files count"
            },
            "the compiler argument pipeline reports the paired-path validation error: ${collector.messages}",
        )
    }
}

private class CjoConstUnionTarget : Table() {
    override fun __reset(index: Int, buffer: ByteBuffer) {
        bb_pos = index
        bb = buffer
    }

    fun int64Value(): Long = PackageFormat.Int64Value().__assign(bb_pos, bb).val_
}

private fun VarInfo.int64Value(): Long {
    val target = CjoConstUnionTarget()
    checkNotNull(value(target)) { "VarInfo has no ConstValue payload" }
    return target.int64Value()
}
