package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.junit.jupiter.api.AfterEach
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.impl.BaseDiagnosticsCollector
import org.cangnova.cangjie.cfir.pipeline.runCheckers
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.resolve.providers.macro.expandWithDefaultContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.recordExpandedRawFilesOnce
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.lang.declarations.CangJieDeclarationFileType
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.source.toSourceLinesMapping

/**
 * 声明文件（`.cj.d`）下 checker 的"实现完备性"豁免（4.4.5）。
 *
 * 端到端验证统一分派处的过滤：`if (checker.requiresImplementation && isFromDeclarationFile(context)) continue`。
 * 判定取自声明**所属文件**（`CjSourceKind`），与解析层同源——`.cj` 与 `.cj.d` 走同一 session，
 * 混合可见性场景由文件种类逐文件区分。
 *
 * 覆盖（A 表 11 项，逐项双向：`.cj` 产出诊断 + 同文本 `.cj.d` 无诊断，6.1 双向验证原则）：
 *  - `CfirConstVariableInitializerChecker`（EXPECT_CONST）
 *  - `CfirFileStaticGlobalInitializationChecker`（TYPE_UNINITIALIZED_STATIC_FIELD）
 *  - `CfirConstructorInitializationChecker`（CLASS_UNINITIALIZED_FIELD）
 *  - `CfirClassLikeInitializationChecker`（ILLEGAL_USAGE_OF_MEMBER）
 *  - `CfirFunctionInitializationChecker`（USED_BEFORE_INITIALIZATION）
 *  - `CfirNotImplementedOverrideChecker`（ABSTRACT_MEMBER_NOT_IMPLEMENTED）
 *  - `CfirCommonPackageMainChecker`（COMMON_PACKAGE_HAS_MAIN）
 *  - `CfirPropertySemanticsChecker`（PROPERTY_MUST_HAVE_ACCESSORS）
 *  - `CfirPropertyAccessorDeclarationChecker`（CANNOT_HAVE_PARAMETER）
 *  - `CfirOpenMemberChecker`（IGNORE_OPEN）
 *  - `CfirFinalizerDeclarationChecker`（FINALIZER_FORBIDDEN_IN_CLASS）
 *
 * 官方 CompileCjd 对所有 func/property body 检查提前返回；`.cj.d` 的无体成员不报缺体。
 * 该语义独立于 raw 是否推断 ABSTRACT，不能把 func checker 当成 raw R3 的探针。
 * raw R3/R4 的函数/属性状态差异仍由 DeclarationModeRawCfirStatusTest 独立验收。
 */
class CfirDeclarationModeCheckersTest : AbstractCfirAnalysisResolveTest() {

    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    // ==================== CfirNotImplementedOverrideChecker ====================

    @Test
    fun testSourceFileReportsUnimplementedInterfaceMember() {
        val cjFile = createCjFile("unimplementedImpl", UNIMPLEMENTED_INTERFACE_MEMBER)
        val diagnostics = collectDiagnosticNames(cjFile)
        assertTrue(
            "ABSTRACT_MEMBER_NOT_IMPLEMENTED" in diagnostics,
            "`.cj` 的 class 未实现接口成员必须报 ABSTRACT_MEMBER_NOT_IMPLEMENTED，实际：$diagnostics",
        )
    }

    @Test
    fun testDeclarationFileSuppressesUnimplementedInterfaceMember() {
        val cjFile = createDeclarationCjFile("unimplementedImpl", UNIMPLEMENTED_INTERFACE_MEMBER)
        val diagnostics = collectDiagnosticNames(cjFile)
        assertTrue(
            diagnostics.isEmpty(),
            "`.cj.d` 的同名文本不得报任何实现完备性诊断，实际：$diagnostics",
        )
    }

    // ==================== CfirConstVariableInitializerChecker ====================

    @Test
    fun testSourceFileConstVariableRejectsNonConstInitializer() {
        assertCheckerDualMode(
            fixtureName = "constVariableInitializer",
            source = """
                var x = 0
                const y = [0, 1, x]
                main() {}
            """.trimIndent(),
            expectedInCj = "EXPECT_CONST",
        )
    }

    // ==================== CfirFileStaticGlobalInitializationChecker ====================

    @Test
    fun testSourceFileReportsUninitializedStaticField() {
        assertCheckerDualMode(
            fixtureName = "fileStaticGlobalInitialization",
            source = """
                class A {
                    static var b: Int64
                }
                main(): Int64 {
                    0
                }
            """.trimIndent(),
            expectedInCj = "TYPE_UNINITIALIZED_STATIC_FIELD",
        )
    }

    // ==================== CfirConstructorInitializationChecker ====================

    @Test
    fun testSourceFileReportsConstructorLeavesFieldUninitialized() {
        assertCheckerDualMode(
            fixtureName = "constructorInitialization",
            source = """
                class A {
                    public init() {}
                    public var a: Int64
                }
                main(): Int64 {
                    0
                }
            """.trimIndent(),
            expectedInCj = "CLASS_UNINITIALIZED_FIELD",
        )
    }

    // ==================== CfirClassLikeInitializationChecker ====================

    @Test
    fun testSourceFileReportsMemberInitializerAccessingMember() {
        assertCheckerDualMode(
            fixtureName = "classLikeInitialization",
            source = """
                open class Base {
                    public func getNum1(): Int64 {
                        return 1
                    }
                }
                class Data <: Base {
                    public var res = getNum1()
                    public init() {}
                }
                main(): Int64 {
                    0
                }
            """.trimIndent(),
            expectedInCj = "ILLEGAL_USAGE_OF_MEMBER",
        )
    }

    // ==================== CfirFunctionInitializationChecker ====================

    @Test
    fun testSourceFileReportsUseBeforeInitialization() {
        assertCheckerDualMode(
            fixtureName = "functionInitialization",
            source = """
                main() {
                    var a: Int64
                    let b = a
                }
            """.trimIndent(),
            expectedInCj = "USED_BEFORE_INITIALIZATION",
        )
    }

    // ==================== CfirCommonPackageMainChecker ====================

    @Test
    fun testSourceFileReportsMainInCommonPackagePart() {
        // 官方 File::isCommon：common 编译中含 common 声明的文件即 common package part 文件，其中的 main 报错
        //（cjc 1.1.3 `--output-type=chir` 实测）。common 修饰符走正常源码装填，不再手工改写 status。
        assertCheckerDualMode(
            fixtureName = "commonPackageMain",
            source = """
                public common func f(): Int64

                main(): Int64 {
                    0
                }
            """.trimIndent(),
            expectedInCj = "COMMON_PACKAGE_HAS_MAIN",
            cjmpMode = CfirCjmpMode.COMMON,
        )
    }

    // ==================== CfirPropertySemanticsChecker ====================

    @Test
    fun testSourceFileReportsPropertyWithoutAccessors() {
        assertCheckerDualMode(
            fixtureName = "propertySemantics",
            source = """
                struct Base12 {
                    public var du: Int64 = 1
                    public prop a: Int64 {
                    }
                }
                main() {
                    0
                }
            """.trimIndent(),
            expectedInCj = "PROPERTY_MUST_HAVE_ACCESSORS",
        )
    }

    // ==================== CfirPropertyAccessorDeclarationChecker ====================

    @Test
    fun testSourceFileReportsGetterWithParameter() {
        assertCheckerDualMode(
            fixtureName = "propertyAccessorDeclaration",
            source = """
                struct A {
                    public mut prop a: Int64 {
                        get(b) {
                            1
                        }
                        set(v) {}
                    }
                }
                main(): Int64 {
                    0
                }
            """.trimIndent(),
            expectedInCj = "CANNOT_HAVE_PARAMETER",
        )
    }

    // ==================== CfirOpenMemberChecker ====================

    @Test
    fun testSourceFileReportsOpenMemberInNonOpenClass() {
        assertCheckerDualMode(
            fixtureName = "openMember",
            source = """
                class A {
                    public open func f(): Int32 {
                        return 0
                    }
                }
                main(): Int64 {
                    0
                }
            """.trimIndent(),
            expectedInCj = "IGNORE_OPEN",
        )
    }

    // ==================== CfirFinalizerDeclarationChecker ====================

    @Test
    fun testSourceFileReportsFinalizerInOpenClass() {
        assertCheckerDualMode(
            fixtureName = "finalizerDeclaration",
            source = """
                open class E {
                    ~init() {}
                }
                main(): Int64 {
                    0
                }
            """.trimIndent(),
            expectedInCj = "FINALIZER_FORBIDDEN_IN_CLASS",
        )
    }

    // ==================== CompileCjd：函数与属性 body 检查统一跳过 ====================

    @Test
    fun testDeclarationFileBodylessMembersProduceNoDiagnostics() {
        for (useLightTree in listOf(false, true)) {
            val cjFile = createDeclarationCjFile("bodylessMembers", BODYLESS_MEMBERS)
            val diagnostics = collectDiagnosticNames(cjFile, useLightTree = useLightTree)
            assertTrue(diagnostics.isEmpty(), "CompileCjd 应跳过所有 func/property body 检查，LT=$useLightTree：$diagnostics")
        }
    }

    /** 显式 ABSTRACT 不受 raw R3 抑制；声明文件仍须整体跳过，普通源文件分别报函数与属性缺体。 */
    @Test
    fun testDeclarationFileExplicitStaticAbstractFunctionAndPropertySkipBodyChecks() {
        val functionProbe = resolveTestDataPath(
            "cfir/analysis-tests/testData/diagnostics2/common-specific/commonSpecificStaticAbstractFunctionRequiresBody.cj",
        )
        val propertyProbe = resolveTestDataPath(
            "cfir/analysis-tests/testData/diagnostics2/common-specific/commonSpecificStaticAbstractPropertyRequiresBody.cj",
        )
        for (probe in listOf(functionProbe, propertyProbe)) {
            val source = loadFile(probe.path).replace(Regex("<![^>]*!>|<!>"), "")
            for (useLightTree in listOf(false, true)) {
                assertEquals(
                    listOf("MISSING_FUNC_BODY"),
                    collectDiagnosticNames(createCjFile(probe.nameWithoutExtension, source), CfirCjmpMode.COMMON, useLightTree),
                )
                assertEquals(
                    emptyList<String>(),
                    collectDiagnosticNames(createDeclarationCjFile(probe.nameWithoutExtension, source), CfirCjmpMode.COMMON, useLightTree),
                    "CompileCjd 与 raw ABSTRACT 状态无关，必须跳过缺体检查",
                )
            }
        }
    }

    // ==================== 基础设施 ====================

    private fun collectDiagnosticNames(
        cjFile: CjFile,
        cjmpMode: CfirCjmpMode? = null,
        useLightTree: Boolean = false,
    ): List<String> {
        val session = createTestSession()
        if (cjmpMode != null) {
            session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = cjmpMode))
        }
        // 测试需要看到真实异常，注册"原样重抛"的 handler，
        // 避免 CLI 包装器把无路径 PSI 文件上的异常吞成 "Sourceless CfirFile"。
        session.register(
            org.cangnova.cangjie.cfir.CfirExceptionHandler::class,
            object : org.cangnova.cangjie.cfir.CfirExceptionHandler() {
                override fun handleExceptionOnElementAnalysis(
                    element: org.cangnova.cangjie.cfir.CfirElement,
                    throwable: Throwable,
                ): Nothing = throw throwable

                override fun handleExceptionOnFileAnalysis(file: CfirFile, throwable: Throwable): Nothing =
                    throw throwable
            },
        )
        val cfirFile = if (useLightTree) buildLightTreeFile(cjFile, session) else cjFile.toCfirFile(session = session)

        // 对齐 analyse.kt 的 resolveAndCheckCfir 两段式：resolve 与 checkers 是两个独立步骤，
        // checker 诊断只在 runCheckers 阶段收集，resolve 阶段诊断走 session 注册的 reporter。
        val resolveCollector = CfirDiagnosticCollector()
        resolveToPhase(cfirFile, session, targetPhase, resolveCollector)

        val checkerCollector = RecordingDiagnosticsCollector()
        for (mppCheckerKind in MppCheckerKind.entries) {
            session.runCheckers(ScopeSession(), listOf(cfirFile), checkerCollector, mppCheckerKind)
        }

        return (resolveCollector.rawDiagnostics + checkerCollector.diagnostics)
            .map { it.factoryName.removePrefix("CFIR_") }
    }

    /** 与 PSI 共用正式 identity macro producer；来源种类来自真实 PSI 文件，不改写 raw 状态。 */
    private fun buildLightTreeFile(cjFile: CjFile, session: CfirSession): CfirFile {
        val text = cjFile.text
        val source = CjInMemoryTextSourceFile(cjFile.name, null, text, explicitKind = cjFile.sourceKind)
        val built = LightTree2Cfir(session, session.cangjieScopeProvider)
            .buildPreMacroCfirFile(text, source, text.toSourceLinesMapping())
        val pre = buildPreMacroRawFiles(session, listOf(built))
        val result = MacroConstructionService.Identity.expandWithDefaultContext(
            pre = pre,
            mode = MacroConstructionService.Mode.STRICT,
        ) as MacroConstructionResult.Success
        recordExpandedRawFilesOnce(session.cfirProvider as CfirProviderImpl, result.recordableFiles, result.registry)
        return built.cfirFile
    }

    /** checker 阶段原样保存全部报告，不能通过筛选或去重改变双向验证。 */
    private class RecordingDiagnosticsCollector : BaseDiagnosticsCollector() {
        private val storage = mutableListOf<CjDiagnostic>()

        override val diagnostics: List<CjDiagnostic>
            get() = storage

        override val diagnosticsByFilePath: Map<String?, List<CjDiagnostic>>
            get() = storage.groupBy { null }

        override val hasErrors: Boolean
            get() = storage.any { it.severity.isError }

        override val hasWarningsForWError: Boolean
            get() = false

        override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
            if (diagnostic != null) storage += diagnostic
        }
    }

    /**
     * 双向验证（6.1）：同一文本在 `.cj` 下必须产出 [expectedInCj] 诊断（checker 正常工作），
     * 在 `.cj.d` 下不得产出**任何**诊断（统一分派处整体跳过该 checker）。
     */
    private fun assertCheckerDualMode(
        fixtureName: String,
        source: String,
        expectedInCj: String,
        cjmpMode: CfirCjmpMode? = null,
    ) {
        val cjDiagnostics = collectDiagnosticNames(createCjFile(fixtureName, source), cjmpMode)
        assertTrue(
            expectedInCj in cjDiagnostics,
            "`.cj` 下 $fixtureName 必须由带 requiresImplementation 标记的 checker 产出 $expectedInCj，实际：$cjDiagnostics",
        )

        val cjdDiagnostics = collectDiagnosticNames(createDeclarationCjFile(fixtureName, source), cjmpMode)
        assertTrue(
            cjdDiagnostics.isEmpty(),
            "`.cj.d` 下同一文本不得产出任何诊断（requiresImplementation 豁免在统一分派处跳过该 checker），实际：$cjdDiagnostics",
        )
    }

    /** 以显式 FileType 创建 `.cj.d` PSI 文件：fileType 是 `CjFile.sourceKind` 的唯一真源。 */
    private fun createDeclarationCjFile(name: String, text: String): CjFile =
        psiFileFactory.createFileFromText("$name.cj.d", CangJieDeclarationFileType, text) as CjFile

    companion object {
        /** `.cj` 与 `.cj.d` 共用的输入：class 未实现接口的无体成员。 */
        private val UNIMPLEMENTED_INTERFACE_MEMBER = """
            interface A {
                func a(): Unit
            }
            class B <: A {
            }
        """.trimIndent()

        /** `.cj.d` 的合法无体成员全集（函数 / 属性 / 访问器 / 构造）。 */
        private val BODYLESS_MEMBERS = """
            class C {
                func f(): Int64
                prop p: Int64
                init()
            }
            interface I {
                func g(): Unit
                prop q: Int64
            }
        """.trimIndent()
    }
}
