package org.jetbrains.kotlin.generators.model.methods

import org.jetbrains.kotlin.generators.MethodGenerator
import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.utils.Printer

/**
 * 旧版 JUnit4 测试类中的 `runTest` 包装方法模型。
 *
 * 该方法把生成出的测试数据路径转交给 KotlinTestUtils，并在需要时传入目标后端。
 */
class RunTestMethodModel(
    /**
     * 传给测试运行器的目标后端；为 null 时不生成后端参数。
     */
    val targetBackend: TargetBackend?,
    /**
     * 真实测试实现方法名，生成方法体会以函数引用形式传入。
     */
    val testMethodName: String,
    /**
     * KotlinTestUtils 上实际调用的运行器方法名。
     */
    val testRunnerMethodName: String,
) : MethodModel<RunTestMethodModel>() {
    /**
     * 生成该包装方法签名和方法体的生成器。
     */
    override val generator: MethodGenerator<RunTestMethodModel> get() = Generator

    /**
     * 包装方法固定使用默认运行入口名称。
     */
    override val name = MethodGenerator.DEFAULT_RUN_TEST_METHOD_NAME

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
     * 在生成带目标后端参数的方法体时引入 [TargetBackend] 导入。
     */
    override fun imports(): Collection<Class<*>> {
        return super.imports() + if (isWithTargetBackend()) setOf(TargetBackend::class.java) else emptySet()
    }

    /**
     * 判断生成的运行调用是否需要显式传递目标后端或自定义运行器方法名。
     */
    fun isWithTargetBackend(): Boolean {
        return !(targetBackend == null && testRunnerMethodName == MethodGenerator.DEFAULT_RUN_TEST_METHOD_NAME)
    }

    /**
     * 旧版 JUnit4 `runTest` 包装方法的 Java 源码生成器。
     */
    object Generator : MethodGenerator<RunTestMethodModel>() {
        /**
         * 输出调用 KotlinTestUtils 的方法体。
         */
        override fun generateBody(method: RunTestMethodModel, p: Printer) {
            with(method) {
                val modifiedTestMethodName = "this::$testMethodName"
                if (!isWithTargetBackend()) {
                    p.println("KotlinTestUtils.$testRunnerMethodName($modifiedTestMethodName, this, testDataFilePath);")
                } else {
                    val className = TargetBackend::class.java.simpleName
                    p.println("KotlinTestUtils.$testRunnerMethodName($modifiedTestMethodName, $className.$targetBackend, testDataFilePath);")
                }
            }
        }

        /**
         * 输出接收测试数据路径的私有 Java 方法签名。
         */
        override fun generateSignature(method: RunTestMethodModel, p: Printer) {
            p.print("private void ${method.name}(String testDataFilePath)")
        }
    }
}
