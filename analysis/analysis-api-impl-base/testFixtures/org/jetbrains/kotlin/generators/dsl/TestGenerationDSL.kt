package org.jetbrains.kotlin.generators.dsl

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.AnnotationModel
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.generators.model.SimpleTestClassModel
import org.jetbrains.kotlin.generators.model.TestClassModel
import org.jetbrains.kotlin.generators.model.TestInfraRevision
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.extractTagsFromDirectory
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.regex.Pattern

/**
 * 并行遍历测试套件中的所有测试类定义。
 *
 * 遍历前按测试方法数量倒序排序，让较大的测试类优先进入并行队列，减少整体生成尾部等待时间。
 */
fun TestGroupSuite.forEachTestClassParallel(f: (TestGroup.TestClass) -> Unit) {
    testGroups
        .parallelStream()
        .flatMap { it.testClasses.stream() }
        .sorted(compareByDescending { it.testModels.sumOf { it.methods.size } })
        .forEach(f)
}

/**
 * 一次测试生成任务的顶层 DSL 容器。
 *
 * @property testInfraRevision 当前套件使用的测试基础设施版本。
 * @property defaultSkipTestAllFilesCheck 套件内测试类默认是否跳过测试数据覆盖检查方法。
 */
class TestGroupSuite(
    /**
     * 当前套件使用的测试基础设施版本。
     */
    val testInfraRevision: TestInfraRevision,
    /**
     * 套件内测试类默认是否跳过测试数据覆盖检查方法。
     */
    val defaultSkipTestAllFilesCheck: Boolean,
) {
    /**
     * 该套件下注册的测试组列表。
     */
    val testGroups: MutableList<TestGroup> = mutableListOf()

    /**
     * 注册一个测试组。
     *
     * @param testsRoot 生成测试源码的根目录。
     * @param testDataRoot 测试数据根目录。
     * @param testRunnerMethodName 生成 `runTest` 包装方法时调用的运行器方法名。
     * @param init 测试组内部 DSL 配置。
     */
    fun testGroup(
        testsRoot: String,
        testDataRoot: String,
        testRunnerMethodName: String = MethodGenerator.DEFAULT_RUN_TEST_METHOD_NAME,
        init: TestGroup.() -> Unit,
    ) {
        testGroups += TestGroup(
            testsRoot,
            testDataRoot,
            testRunnerMethodName,
            testInfraRevision,
            defaultSkipTestAllFilesCheck,
        ).apply(init)
    }
}

/**
 * 一组共享源码根、测试数据根和运行入口的测试类定义。
 *
 * @property testsRoot 生成测试源码的根目录。
 * @property testDataRoot 测试数据根目录。
 * @property testRunnerMethodName 生成运行包装方法时调用的测试运行器方法名。
 * @property testInfraRevision 当前测试组使用的测试基础设施版本。
 * @property defaultSkipTestAllFilesCheck 当前测试组默认是否跳过测试数据覆盖检查。
 */
class TestGroup(
    /**
     * 生成测试源码的根目录。
     */
    private val testsRoot: String,
    /**
     * 测试数据根目录。
     */
    val testDataRoot: String,
    /**
     * 生成运行包装方法时调用的测试运行器方法名。
     */
    val testRunnerMethodName: String,
    /**
     * 当前测试组使用的测试基础设施版本。
     */
    val testInfraRevision: TestInfraRevision,
    /**
     * 当前测试组默认是否跳过测试数据覆盖检查。
     */
    val defaultSkipTestAllFilesCheck: Boolean,
) {
    /**
     * 当前测试组下注册的测试类定义。
     */
    val testClasses: MutableList<TestClass> = mutableListOf()

    /**
     * 通过泛型基类注册一个测试类定义。
     *
     * @param suiteTestClassName 生成测试套件类名。
     * @param annotations 附加到生成类上的注解模型。
     * @param init 测试类内部 DSL 配置。
     */
    inline fun <reified T> testClass(
        suiteTestClassName: String = getDefaultSuiteTestClassName(T::class.java.simpleName),
        annotations: List<AnnotationModel> = emptyList(),
        noinline init: TestClass.() -> Unit,
    ) {
        val testKClass = T::class.java
        testClass(testKClass, testKClass.name, suiteTestClassName, annotations, init)
    }

    /**
     * 通过显式基类注册一个测试类定义。
     *
     * @param testKClass 测试基类的 Class 对象。
     * @param baseTestClassName 生成类继承的基类全限定名或类名。
     * @param suiteTestClassName 生成测试套件类名。
     * @param annotations 附加到生成类上的注解模型。
     * @param init 测试类内部 DSL 配置。
     */
    fun testClass(
        testKClass: Class<*>,
        baseTestClassName: String = testKClass.name,
        suiteTestClassName: String = getDefaultSuiteTestClassName(baseTestClassName.substringAfterLast('.')),
        annotations: List<AnnotationModel> = emptyList(),
        init: TestClass.() -> Unit,
    ) {
        testClasses += TestClass(testKClass, baseTestClassName, suiteTestClassName, annotations).apply(init)
    }

    /**
     * 单个生成测试类的 DSL 定义。
     *
     * @property testKClass 测试基类的 Class 对象。
     * @property baseTestClassName 生成类继承的基类名称。
     * @property suiteTestClassName 生成测试套件类名。
     * @property annotations 附加到该生成类上的注解模型。
     */
    inner class TestClass(
        /**
         * 测试基类的 Class 对象。
         */
        val testKClass: Class<*>,
        /**
         * 生成类继承的基类名称。
         */
        val baseTestClassName: String,
        /**
         * 生成测试套件类名。
         */
        val suiteTestClassName: String,
        /**
         * 附加到该生成类上的注解模型。
         */
        val annotations: List<AnnotationModel>,
    ) {
        /**
         * 该测试类可见的测试数据根目录。
         */
        val testDataRoot: String
            get() = this@TestGroup.testDataRoot

        /**
         * 该测试类对应的生成源码根目录。
         */
        val baseDir: String
            get() = this@TestGroup.testsRoot

        /**
         * 目录或文件扫描生成出的测试类模型集合。
         */
        val testModels = ArrayList<TestClassModel>()

        /**
         * 需要附加到每个生成测试类中的方法模型集合。
         */
        val methodModels = mutableListOf<MethodModel<*>>()

        /**
         * 向当前测试类追加一个自定义方法模型。
         */
        fun method(method: MethodModel<*>) {
            methodModels += method
        }

        /**
         * 为基于目录命名的测试数据注册一个测试类模型。
         *
         * @param relativePath 测试数据根目录下的相对父路径。
         * @param testDirectoryName 当前测试目录名，同时参与生成类名。
         * @param extension 测试数据文件扩展名；为 null 时匹配无扩展名文件。
         * @param excludeParentDirs 是否排除包含子目录的父目录自身。
         * @param recursive 是否递归扫描子目录。
         * @param targetBackend 旧版 JUnit4 运行入口使用的目标后端。
         * @param excludedPattern 排除测试数据文件的正则字符串。
         */
        fun modelForDirectoryBasedTest(
            relativePath: String,
            testDirectoryName: String,
            extension: String? = "kt",
            excludeParentDirs: Boolean = false,
            recursive: Boolean = true,
            targetBackend: TargetBackend? = null,
            excludedPattern: String? = null,
        ) {
            model(
                "${relativePath}/${testDirectoryName}",
                extension = extension,
                recursive = recursive,
                excludeParentDirs = excludeParentDirs,
                targetBackend = targetBackend,
                excludedPattern = excludedPattern,
                testClassName = testDirectoryName.replaceFirstChar { it.uppercaseChar() } + testKClass.simpleName,
            )
        }

        /**
         * 注册一个通用测试数据扫描模型。
         *
         * @param relativeRootPath 测试数据根目录下当前模型的相对路径。
         * @param recursive 是否递归生成内部测试类。
         * @param excludeParentDirs 是否排除含子目录的父目录自身。
         * @param extension 测试数据文件扩展名；为 null 时匹配无扩展名文件。
         * @param pattern 文件名匹配正则，第一捕获组用于生成测试方法名。
         * @param excludedPattern 排除文件名正则。
         * @param testMethod 真实测试实现方法名。
         * @param testClassName 生成测试类名；为 null 时从目录或文件名推导。
         * @param targetBackend 旧版 JUnit4 运行入口使用的目标后端。
         * @param excludeDirs 当前层级需要排除的相对目录集合。
         * @param excludeDirsRecursively 所有递归层级都需要排除的目录集合。
         * @param skipTestAllFilesCheck 是否跳过测试数据覆盖检查方法。
         */
        fun model(
            relativeRootPath: String = "",
            recursive: Boolean = true,
            excludeParentDirs: Boolean = false,
            extension: String? = "kt",
            pattern: String = if (extension == null) """^([^.]+)$""" else """^(.+)\.$extension$""",
            excludedPattern: String? = null,
            testMethod: String = "doTest",
            testClassName: String? = null,
            targetBackend: TargetBackend? = null,
            excludeDirs: List<String> = listOf(),
            excludeDirsRecursively: List<String> = listOf(),
            skipTestAllFilesCheck: Boolean = defaultSkipTestAllFilesCheck,
        ) {
            val rootFile = File("$testDataRoot/$relativeRootPath")
            val compiledPattern = Pattern.compile(pattern)
            val compiledExcludedPattern = excludedPattern?.let { Pattern.compile(it) }
            val className = testClassName ?: TestGeneratorUtil.fileNameToJavaIdentifier(rootFile)
            require(targetBackend != TargetBackend.ANY) { "TargetBackend.ANY is not allowed, please specify target backend explicitly" }
            if (testInfraRevision == TestInfraRevision.StandardJUnit5) {
                require(targetBackend == null) { "TargetBackend shouldn't be defined for JUnit5" }
            }
            testModels.add(
                SimpleTestClassModel(
                    testInfraRevision,
                    File(testDataRoot),
                    rootFile,
                    recursive,
                    excludeParentDirs,
                    compiledPattern,
                    compiledExcludedPattern,
                    testMethod,
                    className,
                    targetBackend,
                    excludeDirs,
                    excludeDirsRecursively,
                    testRunnerMethodName,
                    annotations,
                    extractTagsFromDirectory(rootFile),
                    methodModels,
                    skipTestAllFilesCheck,
                ),
            )
        }
    }
}

/**
 * 根据抽象测试基类名推导生成测试套件类名。
 */
fun getDefaultSuiteTestClassName(baseTestClassName: String): String {
    require(baseTestClassName.startsWith("Abstract")) { "Doesn't start with \"Abstract\": $baseTestClassName" }
    return baseTestClassName.substringAfter("Abstract") + "Generated"
}
