package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiBasedTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.DirectivesContainer
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.StringDirective
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.test.services.assertions

/**
 * low-level symbol provider 的聚焦单元测试基类。
 *
 * 当前只覆盖 `hasPackage()`，后续如果仓颉侧暴露更多稳定查询入口，再沿着这里继续扩展。
 */
abstract class AbstractSymbolProviderTest : AbstractAnalysisApiBasedTest() {
    /**
     * symbol provider 测试指令。
     */
    private object Directives : SimpleDirectivesContainer() {
        /**
         * 对选定 symbol provider 调用 hasPackage() 的包名参数。
         */
        val HAS_PACKAGE by stringDirective(
            description = "对选定 symbol provider 调用 hasPackage()，值为包名；`<root>` 表示根包。",
            applicability = DirectiveApplicability.Any,
        )
    }

    /**
     * symbol provider 测试支持的额外指令。
     */
    override val additionalDirectives: List<DirectivesContainer>
        get() = super.additionalDirectives + Directives

    /**
     * 返回当前测试需要验证的 symbol provider。
     */
    protected abstract fun findTestSymbolProvider(mainModule: CjTestModule): CfirSymbolProvider

    /**
     * 根据 HAS_PACKAGE 指令调用目标 provider 并渲染结果。
     */
    override fun doTestByMainFile(mainFile: CjFile, mainModule: CjTestModule, testServices: TestServices) {
        val hasPackageTargets = getAllDirectivesWithFiles(Directives.HAS_PACKAGE, testServices)
            .map { parsePackageFqName(it.trim()) }

        val symbolProvider = findTestSymbolProvider(mainModule)
        val actual = buildString {
            for (packageFqName in hasPackageTargets) {
                appendLine("HAS_PACKAGE '$packageFqName':")
                appendLine("  ${symbolProvider.hasPackage(packageFqName)}")
                appendLine()
            }
        }

        testServices.assertions.assertEqualsToTestOutputFile(actual)
    }

    /**
     * 收集结构级和文件级的 [directive] 指令值。
     */
    private fun getAllDirectivesWithFiles(
        directive: StringDirective,
        testServices: TestServices,
    ): List<String> {
        val testModuleStructure = testServices.cjTestModuleStructure.testModuleStructure
        val structureDirectives = testModuleStructure.allDirectives[directive]
        val fileDirectives = testModuleStructure.modules.flatMap { testModule ->
            testModule.files.flatMap { it.directives[directive] }
        }
        return structureDirectives + fileDirectives
    }

    /**
     * 解析包名指令文本。
     */
    private fun parsePackageFqName(value: String): FqName {
        return if (value == "<root>") FqName.ROOT else FqName(value)
    }

    /**
     * 查找当前模块顶层和依赖中的指定类型 symbol provider。
     */
    internal inline fun <reified T> CaModule.findSymbolProvidersOfType(): List<T> {
        val resolutionFacade = getResolutionFacade(project)
        val useSiteSession = resolutionFacade.useSiteCfirSession

        val moduleSymbolProvider = useSiteSession.symbolProvider as? LLModuleWithDependenciesSymbolProvider
            ?: error("Expected `${LLModuleWithDependenciesSymbolProvider::class.simpleName}` as module-level symbol provider.")

        return buildList {
            addAll(moduleSymbolProvider.providers.filterIsInstance<T>())
            addAll(moduleSymbolProvider.dependencyProvider.providers.filterIsInstance<T>())
        }
    }

    /**
     * 返回当前模块自身和依赖的所有顶层 symbol provider。
     */
    internal fun CaModule.allTopLevelSymbolProviders(): List<CfirSymbolProvider> {
        val resolutionFacade = getResolutionFacade(project)
        val useSiteSession = resolutionFacade.useSiteCfirSession

        val moduleSymbolProvider = useSiteSession.symbolProvider as? LLModuleWithDependenciesSymbolProvider
            ?: error("Expected `${LLModuleWithDependenciesSymbolProvider::class.simpleName}` as module-level symbol provider.")

        return buildList {
            addAll(moduleSymbolProvider.providers)
            addAll(moduleSymbolProvider.dependencyProvider.providers)
        }
    }
}
