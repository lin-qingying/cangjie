package org.cangnova.cangjie.analysis.api.impl.base.test.dsl

import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.defaultExtension
import org.cangnova.cangjie.utils.capitalizeAsciiOnly
import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.dsl.TestGroupSuite
import org.jetbrains.kotlin.generators.dsl.getDefaultSuiteTestClassName

/**
 * Analysis API generated tests 的分组 DSL。
 *
 * 分组对象保存当前目录前缀和配置过滤器，并在声明测试类时自动展开所有可用的
 * frontend、module kind、analysis session mode 和 analysis API mode 组合。
 */
class AnalysisApiTestGroup(
    /**
     * 当前分组所属的顶层生成器。
     *
     * 通过该生成器访问共享 suite 和可用 configurator factory 列表。
     */
    private val generator: AnalysisApiTestGenerator,
    /**
     * 当前分组继承下来的配置组合过滤器。
     *
     * 子分组和具体测试会继续与该过滤器做逻辑与，逐层收窄生成范围。
     */
    private val groupFilter: TestFilter,
    /**
     * 当前分组对应的 testData 相对目录。
     *
     * 该路径会拼接到 `analysis/analysis-api/testData` 之后，用作 generated test 的数据根。
     */
    private val directory: String?,
) {
    /**
     * 创建子分组并继承当前过滤条件。
     *
     * 子分组目录会追加到当前目录之后，过滤器会与父分组过滤器组合，保证嵌套 DSL 的作用域稳定。
     */
    fun group(directory: String? = null, filter: TestFilter = { true }, init: AnalysisApiTestGroup.() -> Unit) {
        AnalysisApiTestGroup(
            generator,
            groupFilter and filter,
            listOfNotNull(this.directory, directory).joinToString(separator = "/")
        ).init()
    }

    /**
     * 直接访问底层 `TestGroupSuite` 进行 suite 级测试声明。
     *
     * 该入口保留给少量不能按 Analysis API 配置矩阵展开的生成逻辑。
     */
    fun suiteBasedTests(init: TestGroupSuite.() -> Unit) {
        generator.suite.init()
    }

    /**
     * 声明一个基于 reified 抽象测试类的 generated test。
     *
     * 该重载把 Kotlin 类型参数转换为 Java class，再交给非 inline 版本统一展开配置组合。
     */
    inline fun <reified T : Any> test(
        noinline filter: TestFilter = { true },
        noinline init: TestGroup.TestClass.(data: AnalysisApiTestConfiguratorFactoryData) -> Unit,
    ) {
        test(T::class.java, filter, init)
    }

    /**
     * 声明一个 Analysis API generated test class。
     *
     * 方法会遍历当前过滤条件允许的配置组合，按目标 tests-gen 根目录分组，并为每个组合生成带
     * `getConfigurator()` 的测试类。
     */
    fun test(
        testClass: Class<*>,
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

    /**
     * 在具体 tests-gen 目录下生成单个配置组合对应的测试类。
     *
     * 该函数负责选择匹配的 configurator factory、计算包名与 suite class 名称，并向测试类注入
     * `FrontendConfiguratorTestModel`。
     */
    private fun TestGroup.analysisApiTestClass(
        data: AnalysisApiTestConfiguratorFactoryData,
        testClass: Class<*>,
        init: TestGroup.TestClass.(data: AnalysisApiTestConfiguratorFactoryData) -> Unit
    ) {
        val factory = findMatchingFactory(data) ?: return

        val fullPackage = getPackageName(data, testClass)

        val suiteTestClassName = buildString {
            append(fullPackage)
            append(getTestNameSuffix(data))
            append(getDefaultSuiteTestClassName(testClass.simpleName))
        }

        getDefaultSuiteTestClassName(testClass.simpleName)

        testClass(
            testClass,
            suiteTestClassName = suiteTestClassName,
        ) {
            method(FrontendConfiguratorTestModel(factory::class, data))
            init(data)
        }
    }

    /**
     * 为指定配置组合查找唯一可用的 configurator factory。
     *
     * 若多个 factory 同时声称支持同一组合，直接失败以暴露生成配置冲突；若没有支持者则跳过该组合。
     */
    private fun findMatchingFactory(data: AnalysisApiTestConfiguratorFactoryData): AnalysisApiTestConfiguratorFactory? {
        val supportedFactories = generator.configuratorFactories.filter { it.supportMode(data) }
        check(supportedFactories.size <= 1) {
            buildString {
                append("For $data")
                append(" expected no more than 1 supported ")
                append(AnalysisApiTestConfiguratorFactory::class.simpleName)
                append(" but ${supportedFactories.size} found ")
                append(supportedFactories.joinToString(prefix = "[", postfix = "]") { it::class.simpleName!! })
            }

        }
        return supportedFactories.singleOrNull()
    }
}

/**
 * 为当前 generated test class 声明按配置组合推导出的 test model。
 *
 * 扩展函数统一填入前端默认文件扩展名，并保留排除目录与排除文件模式入口，使各个测试族只需要声明
 * 自己的 testData 相对根目录。
 */
internal fun TestGroup.TestClass.model(
    data: AnalysisApiTestConfiguratorFactoryData,
    relativeRootPath: String,
    excludeDirsRecursively: List<String> = listOf(),
    excludedPattern: String? = null,
) {
    model(
        relativeRootPath = relativeRootPath,
        extension = data.defaultExtension(),
        excludeDirsRecursively = excludeDirsRecursively,
        excludedPattern = excludedPattern,
    )
}

/**
 * 根据 Analysis API 配置组合生成测试类名称后缀。
 *
 * 后缀包含 frontend、API mode、session mode 和 module kind，保证同一个抽象测试在不同配置下生成的类名可区分。
 */
private fun getTestNameSuffix(data: AnalysisApiTestConfiguratorFactoryData): String {
    return buildString {
        append(data.frontend.suffix.capitalizeAsciiOnly())
        append(data.analysisApiMode.suffix.capitalizeAsciiOnly())
        append(data.analysisSessionMode.suffix.capitalizeAsciiOnly()); append("Analysis")
        append(data.moduleKind.suffix.capitalizeAsciiOnly()); append("Module")
    }
}

/**
 * 根据配置组合和抽象测试类推导 generated test 的包名。
 *
 * 包名保留原抽象测试在 `test.cases` 之后的目录结构，并按 standalone/IDE 与 frontend 维度分流到对应命名空间。
 */
private fun getPackageName(data: AnalysisApiTestConfiguratorFactoryData, testClass: Class<*>): String {
    val basePrefix = buildString {
        append("org.cangnova.cangjie.analysis.api.")
        if (data.analysisApiMode == AnalysisApiMode.Standalone) {
            append("standalone.")
        }
        append(data.frontend.suffix.lowercase())
        append(".test.cases.generated")
    }
    val packagePrefix = "cases." + testClass.name
        .substringAfter("test.cases.")
        .substringBeforeLast('.', "")

    return if (packagePrefix.isEmpty()) "$basePrefix." else "$basePrefix.$packagePrefix."
}

/**
 * 当前配置组合对应的 tests-gen 输出根目录。
 *
 * IDE CFIR 与 standalone CFIR 分别生成到不同模块；不支持的组合返回 `null` 并在生成时被跳过。
 */
private val AnalysisApiTestConfiguratorFactoryData.testPath: String?
    get() = when (frontend) {
        FrontendKind.Cfir if analysisApiMode == AnalysisApiMode.Ide -> "analysis/analysis-api-cfir/tests-gen"
        FrontendKind.Cfir if analysisApiMode == AnalysisApiMode.Standalone -> "analysis/analysis-api-standalone/tests-gen"
        else -> null
    }

/**
 * Analysis API 测试生成器会尝试展开的全部配置组合。
 *
 * 后续分组过滤器和 configurator factory 支持性检查会共同决定哪些组合真正生成测试类。
 */
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
