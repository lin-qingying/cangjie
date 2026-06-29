package org.cangnova.cangjie.analysis.api.impl.base.test.cases.components.analysisScopeProvider

import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.impl.base.test.AbstractAnalysisApiComponentTest
import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrint
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.services.expressionMarkerProvider
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * `analysisScopeProvider.canBeAnalysed` 的抽象测试。
 *
 * 这组测试固定观察：
 * 1. 指定 use-site module 的 analysis session；
 * 2. 某个明确 PSI 元素在该 session 下是否可分析；
 * 3. 结果只围绕公开 `canBeAnalysed()` 契约断言。
 */
abstract class AbstractCanBeAnalysedTest : AbstractAnalysisApiComponentTest() {
    /**
     * 当前可分析性测试额外注册的 use-site module 指令。
     *
     * 该指令允许同一份 testData 指定从哪个模块的 analysis session 调用 `canBeAnalysed()`。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    /**
     * 可分析性测试的专用指令集合。
     */
    object Directives : SimpleDirectivesContainer() {
        /**
         * 指定调用 `canBeAnalysed()` 时使用的 analysis session 所属模块。
         */
        val USE_SITE_MODULE by directive("指定从哪个测试模块的 analysis session 调用 `canBeAnalysed()`。")
    }

    /**
     * 执行 `canBeAnalysed()` 组件测试。
     *
     * 方法根据指令选择 use-site module，并验证目标文件或元素是否能被公开 Analysis API 分析。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val targetElement = testServices.expressionMarkerProvider.getBottommostElementOfTypeByDirective(
            file = mainFile,
            module = mainModule.testModule,
            defaultType = CjElement::class,
        ) as CjElement

        val useSiteModule = testServices.cjTestModuleStructure.mainModules
            .singleOrNull { module -> Directives.USE_SITE_MODULE in module.testModule.directives }
            ?: error("No use-site module specified. Please add `${Directives.USE_SITE_MODULE.name}` to one test module.")

        val actual = analyze(useSiteModule.caModule) {
            prettyPrint {
                appendLine("target: ${targetElement.text}")
                appendLine("canBeAnalysed: ${targetElement.canBeAnalysed()}")
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }
}
