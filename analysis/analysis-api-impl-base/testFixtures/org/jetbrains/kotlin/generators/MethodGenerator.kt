package org.jetbrains.kotlin.generators

import org.jetbrains.kotlin.generators.model.MethodModel
import org.jetbrains.kotlin.utils.Printer

/**
 * 测试方法源码生成器的抽象基类。
 *
 * 每个 [MethodModel] 通过对应的生成器输出 Java 方法签名和方法体，从而让测试数据扫描、
 * 运行入口、覆盖检查等不同方法类型保持统一的模型接口。
 */
abstract class MethodGenerator<in T : MethodModel<in T>> {
    /**
     * 方法生成器共享的默认输出工具。
     */
    companion object {
        /**
         * 输出无参 public Java 测试方法签名。
         *
         * @param method 提供方法名的测试方法模型。
         * @param p 目标 Java 源码打印器。
         */
        fun generateDefaultSignature(method: MethodModel<*>, p: Printer) {
            p.print("public void ${method.name}()")
        }

        /**
         * 默认测试数据运行入口方法名。
         */
        const val DEFAULT_RUN_TEST_METHOD_NAME = "runTest"
    }

    /**
     * 输出该方法模型对应的 Java 方法签名。
     */
    abstract fun generateSignature(method: T, p: Printer)

    /**
     * 输出该方法模型对应的 Java 方法体。
     */
    abstract fun generateBody(method: T, p: Printer)
}
