package org.jetbrains.kotlin.generators.model.methods

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.generators.model.TestInfraRevision
import org.jetbrains.kotlin.generators.util.TestGeneratorUtil
import org.jetbrains.kotlin.generators.util.getFilePath
import org.jetbrains.kotlin.utils.Printer
import java.io.File
import java.util.regex.Pattern

/**
 * 单个测试数据文件或目录对应的测试方法模型。
 *
 * 该模型根据文件名正则生成 Java 测试方法名，并根据测试基础设施版本输出对应的运行调用。
 */
class SimpleTestMethodModel(
    /**
     * 当前测试方法所属的生成基础设施版本。
     */
    private val testInfraRevision: TestInfraRevision,
    /**
     * 计算测试数据相对路径和方法名时使用的根目录。
     */
    private val rootDir: File,
    /**
     * 当前测试方法覆盖的测试数据文件或目录。
     */
    val file: File,
    /**
     * 用于匹配文件名并提取方法名主体的正则。
     */
    private val filenamePattern: Pattern,
    /**
     * 写入 JUnit5 `@Tag` 的标签集合。
     */
    override val tags: List<String>,
) : MethodModel<SimpleTestMethodModel>() {
    /**
     * 生成该测试方法源码的生成器。
     */
    override val generator: MethodGenerator<SimpleTestMethodModel> get() = Generator

    /**
     * 写入 `@TestMetadata` 的测试数据相对路径。
     */
    override val dataString: String
        get() {
            val path = FileUtil.getRelativePath(rootDir, file)!!
            return File(path).getFilePath()
        }

    /**
     * 根据测试数据文件名和相对目录计算 Java 测试方法名。
     */
    override val name: String
        get() {
            val matcher = filenamePattern.matcher(file.name)
            val found = matcher.find()
            assert(found) { file.name + " isn't matched by regex " + filenamePattern.pattern() }
            assert(matcher.groupCount() >= 1) { filenamePattern.pattern() }
            val extractedName = try {
                matcher.group(1) ?: error("extractedName should not be null: " + filenamePattern.pattern())
            } catch (e: Throwable) {
                throw IllegalStateException("Error generating test ${file.name}", e)
            }
            val unescapedName = if (rootDir == file.parentFile) {
                extractedName
            } else {
                val relativePath = FileUtil.getRelativePath(rootDir, file.parentFile)
                relativePath + "-" + extractedName.replaceFirstChar(Char::uppercaseChar)
            }
            val nameSuffix = TestGeneratorUtil.escapeForJavaIdentifier(unescapedName).replaceFirstChar(Char::uppercaseChar)
            return "test$nameSuffix"
        }

    /**
     * 单文件测试方法的 Java 源码生成器。
     */
    private object Generator : MethodGenerator<SimpleTestMethodModel>() {
        /**
         * 输出默认 public 无参测试方法签名。
         */
        override fun generateSignature(method: SimpleTestMethodModel, p: Printer) {
            generateDefaultSignature(method, p)
        }

        /**
         * 输出调用运行入口的测试方法体。
         */
        override fun generateBody(method: SimpleTestMethodModel, p: Printer) {
            val file = method.file
            when (method.testInfraRevision) {
                TestInfraRevision.StandardJUnit5 if file.isFile -> {
                    p.println(RunTestWithDirectoryPrefixMethodModel.METHOD_NAME, "(\"", file.name, "\");")
                }
                else -> {
                    val filePath = file.getFilePath() + if (file.isDirectory) "/" else ""
                    p.println(DEFAULT_RUN_TEST_METHOD_NAME, "(\"", filePath, "\");")
                }
            }
        }
    }
}
