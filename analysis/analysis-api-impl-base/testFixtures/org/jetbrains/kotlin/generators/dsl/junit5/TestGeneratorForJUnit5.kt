

package org.jetbrains.kotlin.generators.dsl.junit5

import org.jetbrains.kotlin.generators.AbstractTestGenerator
import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.model.AnnotationModel
import org.jetbrains.kotlin.generators.model.DelegatingTestClassModel
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.generators.model.TestClassModel
import org.jetbrains.kotlin.generators.model.TestEntityModel
import org.jetbrains.kotlin.generators.model.SimpleTestClassModel
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.getFilePath
import org.cangnova.cangjie.test.TestMetadata
import org.jetbrains.kotlin.utils.Printer
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.io.IOException

/**
 * JUnit5 版本的测试源码生成器。
 *
 * 该生成器输出标准 JUnit5 Java 测试套件，使用 `@Nested` 表示目录层级，
 * 使用 `@Tag` 保留测试数据目录中的标签信息。
 */
object TestGeneratorForJUnit5 : AbstractTestGenerator() {
    /**
     * 根据测试实体的数据路径输出 `@TestMetadata` 注解。
     */
    private fun Printer.generateMetadata(testDataSource: TestEntityModel) {
        val dataString = testDataSource.dataString
        if (dataString != null) {
            println("@TestMetadata(\"", dataString, "\")")
        }
    }

    /**
     * 输出 JUnit5 `@Test` 注解。
     */
    private fun Printer.generateTestAnnotation() {
        println("@Test")
    }

    /**
     * 在内部测试类上输出 JUnit5 `@Nested` 注解。
     */
    private fun Printer.generateNestedAnnotation(isNested: Boolean) {
        if (isNested) {
            println("@Nested")
        }
    }

    /**
     * 根据测试类模型输出 `@TestDataPath` 注解。
     */
    private fun Printer.generateTestDataPath(testClassModel: TestClassModel) {
        val dataPathRoot = testClassModel.dataPathRoot
        if (dataPathRoot != null) {
            println("@TestDataPath(\"", dataPathRoot, "\")")
        }
    }

    /**
     * 输出测试类模型携带的自定义类级注解。
     */
    private fun Printer.generateParameterAnnotations(testClassModel: TestClassModel) {
        for (annotationModel in testClassModel.annotations) {
            annotationModel.generate(this)
            println()
        }
    }

    /**
     * 输出测试实体携带的 JUnit5 标签注解。
     */
    private fun Printer.generateTags(testEntityModel: TestEntityModel) {
        for (tag in testEntityModel.tags) {
            println("@Tag(\"$tag\")")
        }
    }

    /**
     * 输出生成测试类上的全量警告抑制注解。
     */
    private fun Printer.generateSuppressAllWarnings() {
        println("@SuppressWarnings(\"all\")")
    }

    /**
     * 根据 DSL 测试类模型生成并保存 JUnit5 Java 测试源码。
     */
    override fun generateAndSave(
        testClass: TestGroup.TestClass,
        dryRun: Boolean,
        allowGenerationOnTeamCity: Boolean,
        mainClassName: String?,
    ): GenerationResult {
        val generatorInstance = TestGeneratorInstance(
            testClass.baseDir,
            testClass.suiteTestClassName,
            testClass.baseTestClassName,
            testClass.testModels,
            mainClassName,
        )
        return generatorInstance.generateAndSave(dryRun, allowGenerationOnTeamCity)
    }

    /**
     * 单个 JUnit5 测试套件文件的生成实例。
     *
     * @param baseDir 生成源码根目录。
     * @param suiteTestClassFqName 生成套件类全限定名或类名。
     * @param baseTestClassFqName 需要继承的抽象测试基类全限定名或类名。
     * @property testClassModels 需要写入该套件文件的测试类模型集合。
     * @property mainClassName 生成文件头中记录的生成入口类名。
     */
    private class TestGeneratorInstance(
        baseDir: String,
        suiteTestClassFqName: String,
        baseTestClassFqName: String,
        /**
         * 需要写入该套件文件的测试类模型集合。
         */
        private val testClassModels: Collection<TestClassModel>,
        /**
         * 生成文件头中记录的生成入口类名。
         */
        private val mainClassName: String?
    ) {
        /**
         * 抽象测试基类所在包名。
         */
        private val baseTestClassPackage: String = baseTestClassFqName.substringBeforeLast('.', "")

        /**
         * 抽象测试基类短类名。
         */
        private val baseTestClassName: String = baseTestClassFqName.substringAfterLast('.', baseTestClassFqName)

        /**
         * 生成套件类所在包名。
         */
        private val suiteClassPackage: String = suiteTestClassFqName.substringBeforeLast('.', baseTestClassPackage)

        /**
         * 生成套件类短类名。
         */
        private val suiteClassName: String = suiteTestClassFqName.substringAfterLast('.', suiteTestClassFqName)

        /**
         * 生成 Java 测试源码文件的目标路径。
         */
        private val testSourceFilePath: String =
            baseDir + "/" + this.suiteClassPackage.replace(".", "/") + "/" + this.suiteClassName + ".java"

        /**
         * 生成 JUnit5 测试源码并根据模式写入或比较目标文件。
         */
        @Throws(IOException::class)
        fun generateAndSave(dryRun: Boolean, allowGenerationOnTeamCity: Boolean): GenerationResult {
            val generatedCode = generate()

            val testSourceFile = File(testSourceFilePath)
            val changed = if (!dryRun) {
                GeneratorsFileUtil.writeFileIfContentChanged(
                    testSourceFile,
                    generatedCode,
                    logNotChanged = false,
                    forbidGenerationOnTeamcity = !allowGenerationOnTeamCity
                )
            } else {
                GeneratorsFileUtil.isFileContentChangedIgnoringLineSeparators(testSourceFile, generatedCode)
            }
            return GenerationResult(changed, testSourceFilePath)
        }

        /**
         * 构造完整的 JUnit5 Java 测试源码文本。
         */
        private fun generate(): String {
            val out = StringBuilder()
            val p = Printer(out, indentUnit = Printer.TWO_SPACE_INDENT)

            val copyright = File("license/COPYRIGHT_HEADER.txt").takeIf { it.exists() }?.readText() ?: ""
            p.println(copyright)
            p.println()
            p.println("package $suiteClassPackage;")
            p.println()
            p.println("import com.intellij.testFramework.TestDataPath;")
            p.println("import org.cangnova.cangjie.test.util.CjTestUtil;")

            for (clazz in testClassModels.flatMapTo(mutableSetOf()) { classModel -> classModel.imports }) {
                p.println("import ${clazz.canonicalName};")
            }

            if (suiteClassPackage != baseTestClassPackage) {
                p.println("import $baseTestClassPackage.$baseTestClassName;")
            }

            p.println("import ${TestMetadata::class.java.canonicalName};")

            if (testClassModels.requiresNestedAnnotation()) {
                p.println("import ${Nested::class.java.canonicalName};")
            }

            p.println("import ${Test::class.java.canonicalName};")
            if (testClassModels.any { it.containsTags() }) {
                p.println("import ${Tag::class.java.canonicalName};")
            }
            p.println()
            p.println("import java.io.File;")
            p.println("import java.util.regex.Pattern;")
            p.println()
            p.println("/** This class is generated by {@link ", mainClassName, "}. DO NOT MODIFY MANUALLY */")

            p.generateSuppressAllWarnings()

            val model: TestClassModel
            if (testClassModels.size == 1) {
                model = object : DelegatingTestClassModel(testClassModels.single()) {
                    override val name: String
                        get() = suiteClassName
                }
            } else {
                val hasModelNameClashes = testClassModels.mapTo(mutableSetOf()) { it.name }.size < testClassModels.size
                val models = if (hasModelNameClashes) testClassModels.map { it.unfold() } else testClassModels
                model = object : TestClassModel() {
                    override val innerTestClasses: Collection<TestClassModel> = models

                    override val methods: Collection<MethodModel<*>>
                        get() = emptyList()

                    override val isEmpty: Boolean
                        get() = false

                    override val name: String
                        get() = suiteClassName

                    override val dataString: String?
                        get() = null

                    override val dataPathRoot: String?
                        get() = null

                    override val annotations: Collection<AnnotationModel>
                        get() = testClassModels.flatMap { it.annotations }.distinct()

                    override val tags: List<String>
                        get() = testClassModels.flatMap { it.tags }.distinct()
                }
            }

            generateTestClass(p, model, isNested = false)
            return out.toString()
        }

        /**
         * 当多个模型生成同名内部类时，把目录层级展开为嵌套测试类以消除命名冲突。
         */
        private fun TestClassModel.unfold(): TestClassModel {
            if (this !is SimpleTestClassModel) return this
            var result = this
            var rootFile = rootFile
            val testDataRoot = testDataRoot
            while (rootFile.parentFile != testDataRoot) {
                rootFile = rootFile.parentFile
                val fileForModel = rootFile
                result = object : TestClassModel() {
                    override val innerTestClasses: Collection<TestClassModel> = listOf(result)

                    override val methods: Collection<MethodModel<*>>
                        get() = emptyList()

                    override val isEmpty: Boolean
                        get() = false

                    override val name: String
                        get() = TestGeneratorUtil.fileNameToJavaIdentifier(fileForModel)

                    override val dataString: String
                        get() = fileForModel.getFilePath()

                    override val dataPathRoot: String?
                        get() = null

                    override val annotations: Collection<AnnotationModel>
                        get() = emptyList()

                    override val tags: List<String>
                        get() = emptyList()
                }
            }
            return result
        }

        /**
         * 递归输出一个 JUnit5 测试类及其内部测试类。
         *
         * @param isNested 当前类是否作为 JUnit5 `@Nested` 内部类输出。
         */
        private fun generateTestClass(
            p: Printer,
            testClassModel: TestClassModel,
            isNested: Boolean,
        ) {
            p.generateNestedAnnotation(isNested)
            p.generateTags(testClassModel)
            p.generateMetadata(testClassModel)
            p.generateTestDataPath(testClassModel)
            p.generateParameterAnnotations(testClassModel)

            val extendsClause = if (!isNested) " extends $baseTestClassName" else ""

            p.println("public class ${testClassModel.name}$extendsClause {")
            p.pushIndent()

            val testMethods = testClassModel.methods
            val innerTestClasses = testClassModel.innerTestClasses

            var first = true

            for (methodModel in testMethods) {
                if (first) {
                    first = false
                } else {
                    p.println()
                }

                generateTestMethod(p, methodModel)
            }

            for (innerTestClass in innerTestClasses) {
                if (!innerTestClass.isEmpty) {
                    if (first) {
                        first = false
                    } else {
                        p.println()
                    }

                    generateTestClass(p, innerTestClass, true)
                }
            }

            p.popIndent()
            p.println("}")
        }

        /**
         * 输出单个 JUnit5 测试方法的注解、签名和方法体。
         */
        private fun generateTestMethod(p: Printer, methodModel: MethodModel<*>) {
            if (methodModel.isTestMethod) {
                p.generateTestAnnotation()
                p.generateTags(methodModel)
                p.generateMetadata(methodModel)
            }
            methodModel.generateSignature(p)
            p.printWithNoIndent(" {")
            p.println()

            p.pushIndent()

            methodModel.generateBody(p)

            p.popIndent()
            p.println("}")
        }
    }

    /**
     * 判断模型集合是否需要导入并使用 JUnit5 `@Nested`。
     */
    private fun Collection<TestClassModel>.requiresNestedAnnotation(): Boolean {
        return size > 1 || singleOrNull()?.requiresNestedAnnotation() == true
    }

    /**
     * 判断单个测试类模型是否包含内部测试类。
     */
    private fun TestClassModel.requiresNestedAnnotation(): Boolean = innerTestClasses.isNotEmpty()

    /**
     * 判断测试实体及其子实体是否包含任何 JUnit5 标签。
     */
    private fun TestEntityModel.containsTags(): Boolean {
        if (this.tags.isNotEmpty()) return true
        if (this is TestClassModel) {
            if (innerTestClasses.any { it.containsTags() }) return true
            if (methods.any { it.containsTags() }) return true
        }
        return false
    }
}
