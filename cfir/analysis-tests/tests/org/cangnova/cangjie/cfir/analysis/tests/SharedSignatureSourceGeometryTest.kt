package org.cangnova.cangjie.cfir.analysis.tests

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnosticFactory0
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticBaseContext
import org.cangnova.cangjie.cfir.diagnostics.InternalDiagnosticFactoryMethod
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.diagnostics.SourceElementPositioningStrategies
import org.cangnova.cangjie.cfir.diagnostics.SourceElementPositioningStrategy
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * 共享来源定位策略的几何单测，不运行 resolve/checker，也不声明这些片段的类型或 body 语义。
 * 工厂只让真实 raw PSI/LT source 进入 markDiagnostic；body 语言语义由独立矩阵验收。
 * Oracle 是 Kotlin 已映射分支的固定字面量 offset，既不取 actual 范围，也不调用名称定位 helper。
 * extend 没有 Kotlin 名义 class 对应，两个共享策略都必须覆盖整个声明（包括前缀、约束与 body）。
 */
@OptIn(InternalDiagnosticFactoryMethod::class)
class SharedSignatureSourceGeometryTest : AbstractCfirAnalysisResolveTest() {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun ordinaryExtend() {
        assertSignatures(
            "// lead\n\nextend Int64 {}\n// tail\n",
            TextRange(9, 24),
            TextRange(9, 24),
        ) { file ->
            file.declarations.filterIsInstance<CfirExtend>().single().source
        }
    }

    @Test
    fun genericExtendWithBody() {
        assertSignatures(
            "// lead\n\nextend<T> Box<T> {\n    func f(): Unit {}\n}\n// tail\n",
            TextRange(9, 51),
            TextRange(9, 51),
        ) { file ->
            file.declarations.filterIsInstance<CfirExtend>().single().source
        }
    }

    @Test
    fun commonExtendWithComment() {
        assertSignatures(
            "// lead\n\ncommon /* role */ extend Int64 {}\n// tail\n",
            TextRange(9, 42),
            TextRange(9, 42),
            mode = CfirCjmpMode.COMMON,
        ) { file ->
            file.declarations.filterIsInstance<CfirExtend>().single().source
        }
    }

    @Test
    fun specificGenericExtendWithWhitespace() {
        assertSignatures(
            "// lead\n\nspecific\n/* role */ extend<T> Box<T> {\n}\n// tail\n",
            TextRange(9, 49),
            TextRange(9, 49),
            mode = CfirCjmpMode.SPECIFIC,
        ) { file ->
            file.declarations.filterIsInstance<CfirExtend>().single().source
        }
    }

    @Test
    fun commonGenericExtendWithConstraintAndBody() {
        assertSignatures(
            "// lead\n\ncommon /* role */ extend<T> Box<T>\nwhere T <: Object {\n    func f(): Unit {}\n}\n// tail\n",
            TextRange(9, 87),
            TextRange(9, 87),
            mode = CfirCjmpMode.COMMON,
        ) { file ->
            file.declarations.filterIsInstance<CfirExtend>().single().source
        }
    }

    @Test
    fun functionWithReturnType() {
        assertSignatures(
            "// lead\n\npublic func f(value: Int64): Unit {}\n",
            TextRange(9, 42),
            TextRange(16, 42),
        ) { file ->
            file.declarations.filterIsInstance<CfirNamedFunction>().single().source
        }
    }

    @Test
    fun functionWithParameterListEndpoint() {
        assertSignatures(
            "// lead\n\npublic func f(value: Int64) {}\n",
            TextRange(9, 36),
            TextRange(16, 36),
        ) { file ->
            file.declarations.filterIsInstance<CfirNamedFunction>().single().source
        }
    }

    @Test
    fun genericFunctionWithMultilineReturnType() {
        assertSignatures(
            "// lead\n\npublic /*mod*/ func map<T>(value: T):\n    Array<(T, ?T)>\n{}\n",
            TextRange(9, 65),
            TextRange(24, 65),
        ) { file ->
            file.declarations.filterIsInstance<CfirNamedFunction>().single().source
        }
    }

    @Test
    fun genericFunctionStopsBeforeConstraint() {
        assertSignatures(
            "// lead\n\npublic func f<T>(x: T): T where T <: Object {}\n",
            TextRange(9, 34),
            TextRange(16, 34),
        ) { file ->
            file.declarations.filterIsInstance<CfirNamedFunction>().single().source
        }
    }

    @Test
    fun commonFunctionWithComment() {
        assertSignatures(
            "// lead\n\npublic common /* role */ func f(): Unit\n",
            TextRange(9, 48),
            TextRange(34, 48),
            mode = CfirCjmpMode.COMMON,
        ) { file ->
            file.declarations.filterIsInstance<CfirNamedFunction>().single().source
        }
    }

    @Test
    fun specificFunctionWithComment() {
        assertSignatures(
            "// lead\n\npublic specific /* role */ func f(): Unit {}\n",
            TextRange(9, 50),
            TextRange(36, 50),
            mode = CfirCjmpMode.SPECIFIC,
        ) { file ->
            file.declarations.filterIsInstance<CfirNamedFunction>().single().source
        }
    }

    @Test
    fun propertyWithTypeEndpoint() {
        assertSignatures(
            "// lead\n\ninterface I {\n    public prop p: Int64\n}\n",
            TextRange(27, 47),
            TextRange(34, 47),
        ) { file ->
            file.declarations.filterIsInstance<CfirInterface>().single().declarations.filterIsInstance<CfirProperty>().single().source
        }
    }

    @Test
    fun propertyWithMultilineGenericType() {
        assertSignatures(
            "// lead\n\ninterface I {\n    public /*mod*/ prop p:\n        Array<(Int64, ?Int64)>\n}\n",
            TextRange(27, 80),
            TextRange(42, 80),
        ) { file ->
            file.declarations.filterIsInstance<CfirInterface>().single().declarations.filterIsInstance<CfirProperty>().single().source
        }
    }

    @Test
    fun commonPropertyWithComment() {
        assertSignatures(
            "// lead\n\ncommon interface I {\n    common /*mod*/ prop p: Array<Int64>\n}\n",
            TextRange(34, 69),
            TextRange(49, 69),
            mode = CfirCjmpMode.COMMON,
        ) { file ->
            file.declarations.filterIsInstance<CfirInterface>().single().declarations.filterIsInstance<CfirProperty>().single().source
        }
    }

    @Test
    fun getterWithComment() {
        assertSignatures(
            "// lead\n\nclass C {\n    prop p: Int64 {\n        get /*gap*/ () { 1 }\n    }\n}\n",
            TextRange(47, 61),
            TextRange(47, 61),
        ) { file ->
            file.declarations.filterIsInstance<CfirClass>().single().declarations.filterIsInstance<CfirProperty>().single().getter?.source
        }
    }

    @Test
    fun getterWithMultilineParameters() {
        assertSignatures(
            "// lead\n\nclass C {\n    prop p: Int64 {\n        get /*gap*/ (\n        ) { 1 }\n    }\n}\n",
            TextRange(47, 70),
            TextRange(47, 70),
        ) { file ->
            file.declarations.filterIsInstance<CfirClass>().single().declarations.filterIsInstance<CfirProperty>().single().getter?.source
        }
    }

    @Test
    fun setterWithParameterEndpoint() {
        assertSignatures(
            "// lead\n\nclass C {\n    public mut prop p: Int64 {\n        get() { 1 }\n        set(value) {}\n    }\n}\n",
            TextRange(78, 88),
            TextRange(78, 88),
        ) { file ->
            file.declarations.filterIsInstance<CfirClass>().single().declarations.filterIsInstance<CfirProperty>().single().setter?.source
        }
    }

    /**
     * 两入口分别与同一字面量 oracle 比较；source 必须真实且位于非零文件 offset。
     * 无诊断消息断言，也不借 factory 名称制造 compiler 错误的语义结论。
     */
    private fun assertSignatures(
        text: String,
        declarationRange: TextRange,
        callableRange: TextRange,
        mode: CfirCjmpMode? = null,
        select: (CfirFile) -> CjSourceElement?,
    ) {
        val psiSession = geometrySession(mode)
        val psi = createCjFile("signatureGeometryPsi", text)
        val psiRaw = PsiRawCfirBuilder(psiSession, annotationMetadata = CfirAnnotationMetadataBuilder())
            .buildPreMacroCfirFile(psi).cfirFile
        val lightSession = geometrySession(mode)
        val lightSource = CjInMemoryTextSourceFile("signatureGeometryLightTree.cj", null, text)
        val lightRaw = LightTree2Cfir(lightSession, lightSession.cangjieScopeProvider)
            .buildPreMacroCfirFile(text, lightSource, text.toSourceLinesMapping()).cfirFile
        val psiElement = checkNotNull(select(psiRaw))
        val lightElement = checkNotNull(select(lightRaw))
        assertTrue(psiElement is CjPsiSourceElement, "必须走真实 PSI 分支")
        assertTrue(lightElement is CjLightSourceElement, "必须走真实 LT 分支")

        for (source in listOf(psiElement, lightElement)) {
            assertEquals(CjRealSourceElementKind, source.kind)
            assertTrue(source.startOffset > 0, "前置文本必须保留真实文件 offset")
            assertMarkedRange(source, SourceElementPositioningStrategies.DECLARATION_SIGNATURE, declarationRange)
            assertMarkedRange(source, SourceElementPositioningStrategies.CALLABLE_DECLARATION_SIGNATURE_NO_MODIFIERS, callableRange)
        }
    }

    /** 工厂只创建来源载体；直接调用待测共享策略，不渲染或上报语义诊断。 */
    private fun assertMarkedRange(source: CjSourceElement, strategy: SourceElementPositioningStrategy, expected: TextRange) {
        val diagnostic = checkNotNull(geometryFactory.on(source, strategy, geometryContext))
        assertEquals(listOf(expected), strategy.markDiagnostic(diagnostic))
    }

    /** 单模块 raw 构造所需的文法配置，不存在跨 session 配对或 context 恢复。 */
    private fun geometrySession(mode: CfirCjmpMode?): CfirSession = createTestSession().also { session ->
        session.register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(geometrySettings))
        if (mode != null) {
            session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = mode))
        }
    }

    private val geometrySettings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
    private val geometryContext = object : DiagnosticBaseContext {
        override val languageVersionSettings = geometrySettings
    }

    /** 测试专属工厂；复用渲染器集合仅满足工厂构造协议，测试不会查找或调用 renderer。 */
    private val geometryFactory = CjDiagnosticFactory0(
        "SOURCE_SIGNATURE_GEOMETRY_TEST",
        Severity.ERROR,
        SourceElementPositioningStrategies.DECLARATION_SIGNATURE,
        PsiElement::class,
        CfirErrors.getRendererFactory(),
    )
}
