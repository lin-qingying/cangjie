package org.cangnova.cangjie.analysis.api.impl.base.test.dsl

import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.defaultExtension
import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.dsl.getDefaultSuiteTestClassName
import kotlin.reflect.KClass

class AnalysisApiTestGroup(
    private val generator: AnalysisApiTestGenerator,
    private val groupFilter: TestFilter,
    private val directory: String?,
) {
    fun group(directory: String? = null, filter: TestFilter = { true }, init: AnalysisApiTestGroup.() -> Unit) {
        AnalysisApiTestGroup(
            generator,
            groupFilter and filter,
            listOfNotNull(this.directory, directory).joinToString(separator = "/").ifEmpty { null },
        ).init()
    }

    inline fun <reified T : Any> test(
        noinline filter: TestFilter = { true },
        noinline init: TestGroup.TestClass.(data: AnalysisApiTestConfiguratorFactoryData) -> Unit,
    ) {
        test(T::class, filter, init)
    }

    fun test(
        testClass: KClass<*>,
        filter: TestFilter = { true },
        init: TestGroup.TestClass.(data: AnalysisApiTestConfiguratorFactoryData) -> Unit,
    ) {
        with(generator.suite) {
            val fullTestPath = "analysis/analysis-api/testData" + directory?.let { "/$it" }.orEmpty()

            allPossibleFactoryDataList.filter(groupFilter).filter(filter)
                .groupBy { it.testPath }
                .forEach { (testRoot, datas) ->
                    if (testRoot == null) return@forEach
                    testGroup(testRoot, fullTestPath) {
                        datas.forEach { data ->
                            analysisApiTestClass(data, testClass, init)
                        }
                    }
                }
        }
    }

    private fun TestGroup.analysisApiTestClass(
        data: AnalysisApiTestConfiguratorFactoryData,
        testClass: KClass<*>,
        init: TestGroup.TestClass.(data: AnalysisApiTestConfiguratorFactoryData) -> Unit,
    ) {
        val factory = findMatchingFactory(data) ?: return
        val fullPackage = getPackageName(data)
        val suiteTestClassName = buildString {
            append(fullPackage)
            append(getTestNameSuffix(data))
            append(getDefaultSuiteTestClassName(testClass.simpleName ?: error("Anonymous test class: $testClass")))
        }

        testClass(
            testClass.java,
            suiteTestClassName = suiteTestClassName,
        ) {
            method(FrontendConfiguratorTestModel(factory::class, data))
            init(data)
        }
    }

    private fun findMatchingFactory(data: AnalysisApiTestConfiguratorFactoryData) =
        generator.configuratorFactories.singleOrNull { it.supportMode(data) }

    private fun getTestNameSuffix(data: AnalysisApiTestConfiguratorFactoryData): String {
        return buildString {
            append(data.frontend.suffix)
            append(data.analysisApiMode.suffix)
            append(data.analysisSessionMode.suffix)
            append(data.moduleKind.suffix)
            append("Module")
        }
    }

    private fun getPackageName(data: AnalysisApiTestConfiguratorFactoryData): String {
        return buildString {
            append("org.cangnova.cangjie.analysis.api.")
            append(data.frontend.suffix.lowercase())
            append(".test.cases.generated.")
        }
    }

    private val AnalysisApiTestConfiguratorFactoryData.testPath: String?
        get() = when (frontend) {
            FrontendKind.Cfir -> "analysis/analysis-api-cfir/tests-gen"
        }

    private companion object {
        private val allPossibleFactoryDataList: List<AnalysisApiTestConfiguratorFactoryData> = buildList {
            FrontendKind.entries.forEach { frontend ->
                TestModuleKind.entries.forEach { moduleKind ->
                    AnalysisSessionMode.entries.forEach { analysisSessionMode ->
                        AnalysisApiMode.entries.forEach { analysisApiMode ->
                            add(AnalysisApiTestConfiguratorFactoryData(frontend, moduleKind, analysisSessionMode, analysisApiMode))
                        }
                    }
                }
            }
        }
    }
}

internal fun TestGroup.TestClass.model(
    data: AnalysisApiTestConfiguratorFactoryData,
    relativeRootPath: String,
    excludedPattern: String? = null,
    pattern: String = "^.+\\.${data.defaultExtension()}$",
) {
    model(
        relativeRootPath = relativeRootPath,
        extension = data.defaultExtension(),
        pattern = pattern,
        excludedPattern = excludedPattern,
    )
}
