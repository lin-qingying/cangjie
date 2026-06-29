package org.jetbrains.kotlin.generators.model

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.generators.model.methods.RunTestMethodModel
import org.jetbrains.kotlin.generators.model.methods.RunTestWithDirectoryPrefixMethodModel
import org.jetbrains.kotlin.generators.model.methods.SimpleTestMethodModel
import org.jetbrains.kotlin.generators.model.methods.TestAllFilesPresentMethodModel
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil.fileNameToJavaIdentifier
import org.jetbrains.kotlin.generators.util.extractTagsFromDirectory
import org.jetbrains.kotlin.generators.util.extractTagsFromTestFile
import org.jetbrains.kotlin.generators.util.getFilePath
import org.jetbrains.kotlin.test.TargetBackend
import java.io.File
import java.util.regex.Pattern

/**
 * 基于测试数据文件或目录生成测试类的模型。
 *
 * 该模型负责扫描测试数据目录、派生内部测试类、生成单文件测试方法以及
 * `testAllFilesPresent` 覆盖检查方法，是测试生成 DSL 中最常用的类模型实现。
 */
class SimpleTestClassModel(
    /**
     * 当前测试类使用的生成基础设施版本。
     */
    val testInfraRevision: TestInfraRevision,
    /**
     * 整个测试数据树的根目录。
     */
    val testDataRoot: File,
    /**
     * 当前模型负责扫描的文件或目录。
     */
    val rootFile: File,
    /**
     * 是否递归扫描子目录并生成内部测试类。
     */
    val recursive: Boolean,
    /**
     * 子目录包含更深层子目录时，是否排除该父目录自身的测试方法生成。
     */
    private val excludeParentDirs: Boolean,
    /**
     * 用于筛选测试数据文件并提取测试方法名主体的文件名正则。
     */
    val filenamePattern: Pattern,
    /**
     * 需要从测试方法生成中排除的文件名正则。
     */
    val excludePattern: Pattern?,
    /**
     * 旧版 JUnit4 运行测试数据时调用的实际测试方法名。
     */
    private val doTestMethodName: String,
    /**
     * 当前模型生成的 Java 测试类名。
     */
    val testClassName: String,
    /**
     * 旧版 JUnit4 生成器传递给运行入口的目标后端。
     */
    val targetBackend: TargetBackend?,
    excludeDirs: Collection<String>,
    excludeDirsRecursively: Collection<String>,
    /**
     * 生成的 `runTest` 包装方法调用的运行器方法名。
     */
    private val testRunnerMethodName: String,
    /**
     * 附加到生成测试类上的注解集合。
     */
    override val annotations: Collection<AnnotationModel>,
    /**
     * 附加到生成测试类上的 JUnit5 标签集合。
     */
    override val tags: List<String>,
    /**
     * 需要追加到该测试类中的额外方法模型。
     */
    private val additionalMethods: Collection<MethodModel<*>>,
    /**
     * 是否跳过 `testAllFilesPresent` 覆盖检查方法。
     */
    val skipTestAllFilesCheck: Boolean,
) : TestClassModel() {
    /**
     * 生成源码中使用的类名。
     */
    override val name: String
        get() = testClassName

    /**
     * 需要在当前层级或递归层级排除的目录名集合。
     */
    val allExcludedDirs: Set<String> = (excludeDirs + excludeDirsRecursively).toSet()

    /**
     * 当前目录下按名称排序的嵌套测试类模型。
     */
    override val innerTestClasses: Collection<TestClassModel> by lazy {
        if (!rootFile.isDirectory || !recursive) {
            return@lazy emptyList()
        }
        rootFile.listFiles().orEmpty().mapNotNull l@{ file ->
            if (!file.isDirectory) return@l null
            if (!dirHasFilesInside(file)) return@l null
            if (allExcludedDirs.contains(file.name)) return@l null

            SimpleTestClassModel(
                testInfraRevision,
                testDataRoot,
                rootFile = file,
                recursive = true,
                excludeParentDirs,
                filenamePattern,
                excludePattern,
                doTestMethodName,
                testClassName = fileNameToJavaIdentifier(file),
                targetBackend,
                excludesStripOneDirectory(excludeDirs, file.name),
                excludeDirsRecursively,
                testRunnerMethodName,
                annotations,
                extractTagsFromDirectory(file),
                additionalMethods.filter { it.shouldBeGeneratedForInnerTestClass },
                skipTestAllFilesCheck,
            )
        }.sortedWith(BY_NAME)
    }

    /**
     * 将面向父目录书写的排除目录路径剥离一层，使子模型继续使用相对路径。
     */
    private fun excludesStripOneDirectory(excludeDirs: Collection<String>, directoryName: String): Collection<String> {
        if (excludeDirs.isEmpty()) return excludeDirs
        val result: MutableSet<String> = LinkedHashSet()
        for (excludeDir in excludeDirs) {
            val firstSlash = excludeDir.indexOf('/')
            if (firstSlash >= 0 && excludeDir.substring(0, firstSlash) == directoryName) {
                result.add(excludeDir.substring(firstSlash + 1))
            }
        }
        return result
    }

    /**
     * 当前模型直接生成的运行入口、覆盖检查和单文件测试方法。
     */
    override val methods: Collection<MethodModel<*>> by lazy {
        if (!rootFile.isDirectory) {
            val methodModel = SimpleTestMethodModel(
                testInfraRevision,
                rootDir = rootFile,
                file = rootFile,
                filenamePattern,
                extractTagsFromTestFile(rootFile),
            )
            return@lazy listOf(methodModel)
        }

        buildList {
            when (testInfraRevision) {
                TestInfraRevision.LegacyJUnit4 -> add(RunTestMethodModel(targetBackend, doTestMethodName, testRunnerMethodName))
                TestInfraRevision.StandardJUnit5 -> add(RunTestWithDirectoryPrefixMethodModel(rootFile.getFilePath()))
            }
            if (!skipTestAllFilesCheck) {
                add(TestAllFilesPresentMethodModel(this@SimpleTestClassModel))
            }
            addAll(additionalMethods)
            rootFile.listFiles().orEmpty().mapNotNullTo(this) l@{ file ->
                val fileName = file.name
                if (!filenamePattern.matcher(fileName).matches()) return@l null
                if (excludePattern != null && excludePattern.matcher(fileName).matches()) return@l null
                if (file.isDirectory && (fileName in allExcludedDirs)) return@l null
                if (file.isDirectory && excludeParentDirs && dirHasSubDirs(file)) return@l null

                if (file.isDirectory && !dirHasFilesInside(file)) {
                    error(
                        "testData directory $file is empty. " +
                            "This might be due to git branch switching removed the contents but left directory intact. " +
                            "Consider removing empty directory or revert removing of its' contents.",
                    )
                }
                SimpleTestMethodModel(
                    testInfraRevision,
                    rootFile,
                    file,
                    filenamePattern,
                    extractTagsFromTestFile(file),
                )
            }
        }.sortedWith(BY_NAME)
    }

    /**
     * 判断当前模型是否只包含运行包装方法且没有有效内部测试类。
     */
    override val isEmpty: Boolean
        get() {
            val noTestMethods = methods.size == 1
            return noTestMethods && innerTestClasses.isEmpty()
        }

    /**
     * 当前模型写入 `@TestMetadata` 的测试数据路径。
     */
    override val dataString: String
        get() = rootFile.getFilePath()

    /**
     * 生成的 `@TestDataPath` 使用项目根目录作为路径基准。
     */
    override val dataPathRoot: String
        get() = "\$PROJECT_ROOT"

    /**
     * 目录扫描和排序相关的共享工具。
     */
    companion object {
        /**
         * 按生成名称稳定排序测试类和测试方法。
         */
        private val BY_NAME = Comparator.comparing(TestEntityModel::name)

        /**
         * 判断目录树中是否存在实际测试数据文件。
         */
        private fun dirHasFilesInside(dir: File): Boolean {
            return !FileUtil.processFilesRecursively(dir) { obj: File -> obj.isDirectory }
        }

        /**
         * 判断目录第一层是否包含子目录。
         */
        private fun dirHasSubDirs(dir: File): Boolean {
            val listFiles = dir.listFiles() ?: return false
            for (file in listFiles) {
                if (file.isDirectory) {
                    return true
                }
            }
            return false
        }
    }
}
