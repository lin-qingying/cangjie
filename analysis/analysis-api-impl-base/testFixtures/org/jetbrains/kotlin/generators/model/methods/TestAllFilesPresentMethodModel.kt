package org.jetbrains.kotlin.generators.model.methods

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.generators.model.SimpleTestClassModel
import org.jetbrains.kotlin.generators.util.getFilePath
import org.jetbrains.kotlin.utils.Printer
import java.util.regex.Pattern

/**
 * 生成 `testAllFilesPresent` 覆盖检查方法的模型。
 *
 * 该方法确保测试数据目录下所有匹配文件都被生成的测试方法覆盖，避免新增测试数据后漏跑。
 */
class TestAllFilesPresentMethodModel(val classModel: SimpleTestClassModel) : MethodModel<TestAllFilesPresentMethodModel>() {
    /**
     * 生成覆盖检查方法源码的生成器。
     */
    override val generator: MethodGenerator<TestAllFilesPresentMethodModel> get() = Generator

    /**
     * 根据测试类名生成唯一的覆盖检查方法名。
     */
    override val name: String get() = "testAllFilesPresentIn${classModel.testClassName}"

    /**
     * 覆盖检查方法不绑定单个测试数据路径。
     */
    override val dataString: String? get() = null

    /**
     * 覆盖检查方法不携带 JUnit5 标签。
     */
    override val tags: List<String> get() = emptyList()

    /**
     * `testAllFilesPresent` 方法的 Java 源码生成器。
     */
    private object Generator : MethodGenerator<TestAllFilesPresentMethodModel>() {
        /**
         * 输出默认 public 无参测试方法签名。
         */
        override fun generateSignature(method: TestAllFilesPresentMethodModel, p: Printer) {
            generateDefaultSignature(method, p)
        }

        /**
         * 输出调用 CjTestUtil 覆盖检查工具的方法体。
         */
        override fun generateBody(method: TestAllFilesPresentMethodModel, p: Printer) {
            with(method) {
                val exclude = StringBuilder()
                for (dir in classModel.allExcludedDirs) {
                    exclude.append(", \"")
                    exclude.append(StringUtil.escapeStringCharacters(dir))
                    exclude.append("\"")
                }
                val excludePattern = classModel.excludePattern
                p.print(
                    "CjTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File(\"",
                    classModel.rootFile.getFilePath(),
                    "\"), Pattern.compile(\"",
                    StringUtil.escapeStringCharacters(classModel.filenamePattern.pattern()),
                    "\"), ",
                )
                p.printExcludePattern(excludePattern)
                p.printlnWithNoIndent(", ", classModel.recursive, exclude, ");")
            }
        }

        /**
         * 输出可空排除正则的 Java 表达式。
         */
        private fun Printer.printExcludePattern(excludePattern: Pattern?) {
            if (excludePattern != null) {
                printWithNoIndent("Pattern.compile(\"", StringUtil.escapeStringCharacters(excludePattern.pattern()), "\")")
            } else {
                printWithNoIndent("null")
            }
        }
    }
}
