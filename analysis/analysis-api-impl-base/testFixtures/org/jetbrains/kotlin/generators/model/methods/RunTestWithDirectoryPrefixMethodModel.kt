package org.jetbrains.kotlin.generators.model.methods

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.utils.Printer

/**
 * JUnit5 目录模型中用于拼接目录前缀的运行包装方法。
 *
 * 生成的测试方法只传入文件名，该包装方法负责补齐目录路径再调用默认 `runTest`。
 */
class RunTestWithDirectoryPrefixMethodModel(
    /**
     * 当前目录模型对应的测试数据路径前缀。
     */
    val directory: String,
) : MethodModel<RunTestWithDirectoryPrefixMethodModel>() {
    /**
     * 生成该包装方法源码的生成器。
     */
    override val generator: MethodGenerator<RunTestWithDirectoryPrefixMethodModel> get() = Generator

    /**
     * 包装方法固定使用简短的 `run` 名称。
     */
    override val name get() = METHOD_NAME

    /**
     * 包装方法不绑定单个测试数据文件。
     */
    override val dataString: String? = null

    /**
     * 包装方法本身不是 JUnit 测试方法。
     */
    override val isTestMethod: Boolean get() = false

    /**
     * 包装方法不携带 JUnit5 标签。
     */
    override val tags: List<String> get() = emptyList()

    /**
     * JUnit5 目录前缀包装方法的 Java 源码生成器。
     */
    object Generator : MethodGenerator<RunTestWithDirectoryPrefixMethodModel>() {
        /**
         * 输出拼接目录前缀并调用默认运行入口的方法体。
         */
        override fun generateBody(method: RunTestWithDirectoryPrefixMethodModel, p: Printer) {
            p.println("""${DEFAULT_RUN_TEST_METHOD_NAME}("${method.directory}/" + fileName);""")
        }

        /**
         * 输出接收文件名的私有 Java 方法签名。
         */
        override fun generateSignature(method: RunTestWithDirectoryPrefixMethodModel, p: Printer) {
            p.print("private void $METHOD_NAME(String fileName)")
        }
    }

    /**
     * 包装方法名常量。
     */
    companion object {
        /**
         * 生成到 Java 测试类中的目录前缀包装方法名。
         */
        const val METHOD_NAME = "run"
    }
}
