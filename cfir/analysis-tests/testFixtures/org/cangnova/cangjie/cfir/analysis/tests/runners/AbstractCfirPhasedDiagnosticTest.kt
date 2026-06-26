package org.cangnova.cangjie.cfir.analysis.tests.runners

import org.cangnova.cangjie.cfir.analysis.tests.services.CfirInlineDiagnosticsChecker
import org.cangnova.cangjie.cfir.analysis.tests.services.MacroConstructionEnvironmentConfigurator
import org.cangnova.cangjie.cfir.analysis.tests.services.StructuredInlineDiagnosticsAssertionError
import org.cangnova.cangjie.test.CfirParser
import org.cangnova.cangjie.test.TargetBackend
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.config.TestPhaseDirectives
import org.cangnova.cangjie.test.config.TestPhase
import org.cangnova.cangjie.test.config.configurePhasedDiagnosticTest
import org.cangnova.cangjie.test.directives.LanguageSettingsDirectives
import org.cangnova.cangjie.test.directives.MacroConstructionDirectives
import org.cangnova.cangjie.test.frontend.CfirDefaultFacade
import org.cangnova.cangjie.test.runners.AbstractCangjieCompilerWithTargetBackendTest
import org.cangnova.cangjie.test.services.impl.JUnit5Assertions

/**
 * CFIR phased diagnostics test base.
 *
 * Aligned with Kotlin K2 `AbstractFirPhasedDiagnosticTest`:
 * - declares phased defaults
 * - binds parser mode
 * - wires CFIR frontend phase facade
 */
abstract class AbstractCfirPhasedDiagnosticTest(
    /**
     * 当前 phased diagnostic 测试使用的 CFIR parser。
     */
    val parser: CfirParser,
) : AbstractCangjieCompilerWithTargetBackendTest(TargetBackend.ANY) {

    /**
     * 配置 phased diagnostic 测试的统一前端、指令和宏构造环境。
     */
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        assertions = JUnit5Assertions
        useDirectives(LanguageSettingsDirectives, TestPhaseDirectives, MacroConstructionDirectives)
        useConfigurators(::MacroConstructionEnvironmentConfigurator)

        defaultDirectives {
            TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE with TestPhase.BACKEND
        }

        configurePhasedDiagnosticTest(
            parser = parser,
            frontendFacade = ::CfirDefaultFacade,
        )
    }
}

/**
 * LightTree parser 的普通 phased diagnostic 测试基类。
 */
open class AbstractPhasedDiagnosticLightTreeTest : AbstractCfirPhasedDiagnosticTest(CfirParser.LightTree)

/**
 * PSI parser 的普通 phased diagnostic 测试基类。
 */
open class AbstractPhasedDiagnosticPsiTest : AbstractCfirPhasedDiagnosticTest(CfirParser.Psi)

/**
 * 使用结构化内联诊断比对器的 phased diagnostic 测试基类。
 *
 * 该基类关闭默认 meta-info handler，改由 [CfirInlineDiagnosticsChecker]
 * 在 after-analysis 阶段输出结构化 mismatch。
 */
abstract class AbstractCfirStructuredPhasedDiagnosticTest(
    /**
     * 结构化诊断测试使用的 parser。
     */
    private val structuredParser: CfirParser,
) : AbstractCangjieCompilerWithTargetBackendTest(TargetBackend.ANY) {

    /**
     * 配置结构化诊断测试的前端 facade、指令与 after-analysis checker。
     */
    override fun configure(builder: TestConfigurationBuilder) = with(builder) {
        useDirectives(LanguageSettingsDirectives, TestPhaseDirectives, MacroConstructionDirectives)
        useConfigurators(::MacroConstructionEnvironmentConfigurator)

        defaultDirectives {
            TestPhaseDirectives.LATEST_PHASE_IN_PIPELINE with TestPhase.BACKEND
        }

        configurePhasedDiagnosticTest(
            parser = structuredParser,
            frontendFacade = ::CfirDefaultFacade,
            enableMetaInfoHandler = false,
        )

        useAfterAnalysisCheckers(::CfirInlineDiagnosticsChecker)
    }

    /**
     * 运行测试并优先抛出结构化内联诊断断言。
     *
     * 测试框架可能会把 after-analysis 失败包装在外层 AssertionError 中，
     * 这里展开 cause 链以保留诊断 diff 的完整消息。
     */
    override fun runTest(filePath: String) {
        try {
            super.runTest(filePath)
        } catch (error: AssertionError) {
            val structured = generateSequence(error as Throwable?) { it.cause }
                .filterIsInstance<StructuredInlineDiagnosticsAssertionError>()
                .firstOrNull()
            if (structured != null) {
                throw structured
            }
            throw error
        }
    }
}

/**
 * LightTree parser 的结构化 phased diagnostic 测试基类。
 */
open class AbstractStructuredPhasedDiagnosticLightTreeTest :
    AbstractCfirStructuredPhasedDiagnosticTest(CfirParser.LightTree)

/**
 * PSI parser 的结构化 phased diagnostic 测试基类。
 */
open class AbstractStructuredPhasedDiagnosticPsiTest :
    AbstractCfirStructuredPhasedDiagnosticTest(CfirParser.Psi)
