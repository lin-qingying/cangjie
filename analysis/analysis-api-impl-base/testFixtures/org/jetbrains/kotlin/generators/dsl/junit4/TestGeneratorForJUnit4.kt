

package org.jetbrains.kotlin.generators.dsl.junit4

import org.jetbrains.kotlin.generators.AbstractTestGenerator
import org.jetbrains.kotlin.generators.dsl.TestGroup
import org.jetbrains.kotlin.generators.model.AnnotationModel
import org.jetbrains.kotlin.generators.model.DelegatingTestClassModel
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.generators.model.TestClassModel
import org.jetbrains.kotlin.generators.model.TestEntityModel
import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.cangnova.cangjie.test.TestMetadata
import org.jetbrains.kotlin.utils.Printer
import org.junit.runner.RunWith
import java.io.File
import java.io.IOException

/**
 * JUnit4 版本的测试源码生成器。
 *
 * 该生成器输出继承抽象测试基类的 Java 测试套件，并使用 JUnit3 runner 兼容内部类测试结构。
 */
object TestGeneratorForJUnit4 : AbstractTestGenerator() {
    /**
     * 根据 DSL 测试类模型生成并保存 JUnit4 Java 测试源码。
     */
    override fun generateAndSave(
        testClass: TestGroup.TestClass,
        dryRun: Boolean,
        allowGenerationOnTeamCity: Boolean,
        mainClassName: String?,
    ): GenerationResult {
        val generatorInstance = TestGeneratorForJUnit4Instance(
            testClass.baseDir,
            testClass.suiteTestClassName,
            testClass.baseTestClassName,
            testClass.testModels,
            mainClassName,
        )
        return generatorInstance.generateAndSave(dryRun, allowGenerationOnTeamCity)
    }
}

/**
 * 单个 JUnit4 测试套件文件的生成实例。
 *
 * @param baseDir 生成源码根目录。
 * @param suiteTestClassFqName 生成套件类全限定名或类名。
 * @param baseTestClassFqName 需要继承的抽象测试基类全限定名或类名。
 * @property testClassModels 需要写入该套件文件的测试类模型集合。
 * @property mainClassName 生成文件头中记录的生成入口类名。
 */
private class TestGeneratorForJUnit4Instance(
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
     * JUnit4 源码生成中复用的注解和 runner 输出工具。
     */
    companion object {
        /**
         * 支持内部测试类的 JUnit3 runner 类型。
         */
        private val JUNIT3_RUNNER = Class.forName("org.cangnova.cangjie.test.JUnit3RunnerWithInners")

        /**
         * 根据测试实体的数据路径输出 `@TestMetadata` 注解。
         */
        private fun generateMetadata(p: Printer, testDataSource: TestEntityModel) {
            val dataString = testDataSource.dataString
            if (dataString != null) {
                p.println("@TestMetadata(\"", dataString, "\")")
            }
        }

        /**
         * 根据测试类模型输出 `@TestDataPath` 注解。
         */
        private fun generateTestDataPath(p: Printer, testClassModel: TestClassModel) {
            val dataPathRoot = testClassModel.dataPathRoot
            if (dataPathRoot != null) {
                p.println("@TestDataPath(\"", dataPathRoot, "\")")
            }
        }

        /**
         * 输出测试类模型携带的自定义类级注解。
         */
        private fun generateParameterAnnotations(p: Printer, testClassModel: TestClassModel) {
            for (annotationModel in testClassModel.annotations) {
                annotationModel.generate(p)
                p.println()
            }
        }

        /**
         * 输出生成测试类上的全量警告抑制注解。
         */
        private fun generateSuppressAllWarnings(p: Printer) {
            p.println("@SuppressWarnings(\"all\")")
        }
    }

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
    private val testSourceFilePath: String = baseDir + "/" + this.suiteClassPackage.replace(".", "/") + "/" + this.suiteClassName + ".java"

    /**
     * 生成 JUnit4 测试源码并根据模式写入或比较目标文件。
     */
    @Throws(IOException::class)
    fun generateAndSave(dryRun: Boolean, allowGenerationOnTeamCity: Boolean): AbstractTestGenerator.GenerationResult {
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
        return AbstractTestGenerator.GenerationResult(changed, testSourceFilePath)
    }

    /**
     * 构造完整的 JUnit4 Java 测试源码文本。
     */
    private fun generate(): String {
        val out = StringBuilder()
        val p = Printer(out, indentUnit = Printer.TWO_SPACE_INDENT)

        val copyright = File("license/COPYRIGHT_HEADER.txt").readText()
        p.println(copyright)
        p.println()
        p.println("package ", suiteClassPackage, ";")
        p.println()
        p.println("import com.intellij.testFramework.TestDataPath;")
        p.println("import ", JUNIT3_RUNNER.canonicalName, ";")
        p.println("import org.jetbrains.kotlin.test.KotlinTestUtils;")
        p.println("import org.cangnova.cangjie.test.util.CjTestUtil;")

        for (clazz in testClassModels.flatMapTo(mutableSetOf()) { classModel -> classModel.imports }) {
            p.println("import ${clazz.canonicalName};")
        }

        if (suiteClassPackage != baseTestClassPackage) {
            p.println("import $baseTestClassPackage.$baseTestClassName;")
        }

        p.println("import " + TestMetadata::class.java.canonicalName + ";")
        p.println("import " + RunWith::class.java.canonicalName + ";")
        p.println()
        p.println("import java.io.File;")
        p.println("import java.util.regex.Pattern;")
        p.println()
        p.println("/** This class is generated by {@link ", mainClassName, "}. DO NOT MODIFY MANUALLY */")

        generateSuppressAllWarnings(p)

        val model: TestClassModel
        if (testClassModels.size == 1) {
            model = object : DelegatingTestClassModel(testClassModels.single()) {
                override val name: String
                    get() = suiteClassName
            }
        } else {
            model = object : TestClassModel() {
                override val innerTestClasses: Collection<TestClassModel>
                    get() = testClassModels

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
                    get() = emptyList()

                override val tags: List<String>
                    get() = emptyList()
            }
        }

        generateTestClass(p, model, false)
        return out.toString()
    }

    /**
     * 递归输出一个 JUnit4 测试类及其内部测试类。
     *
     * @param isStatic 当前类是否作为内部静态类输出。
     */
    private fun generateTestClass(p: Printer, testClassModel: TestClassModel, isStatic: Boolean) {
        val staticModifier = if (isStatic) "static " else ""

        generateMetadata(p, testClassModel)
        generateTestDataPath(p, testClassModel)
        generateParameterAnnotations(p, testClassModel)

        p.println("@RunWith(${JUNIT3_RUNNER.simpleName}.class)")

        p.println("public " + staticModifier + "class ", testClassModel.name, " extends ", baseTestClassName, " {")
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
     * 输出单个 JUnit4 测试方法的元数据、签名和方法体。
     */
    private fun generateTestMethod(p: Printer, methodModel: MethodModel<*>) {
        generateMetadata(p, methodModel)
        methodModel.generateSignature(p)
        p.printWithNoIndent(" {")
        p.println()

        p.pushIndent()

        methodModel.generateBody(p)

        p.popIndent()
        p.println("}")
    }
}
