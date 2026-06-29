package org.cangnova.cangjie.test.util

import org.cangnova.cangjie.test.model.TestModule

/**
 * 表示 `MultiModuleInfoDumper`，承载测试工具中的配置数据、测试产物或处理步骤。
 */
class MultiModuleInfoDumper(
    /**
     * 保存 `moduleHeaderTemplate`，供测试工具在测试执行期间读取或传递。
     */
    private val moduleHeaderTemplate: String? = "Module: %s",
) {
    /**
     * 保存 `builderByModule`，供测试工具在测试执行期间读取或传递。
     */
    private val builderByModule = LinkedHashMap<String, StringBuilder>()

    /**
     * 执行 `builderForModule` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    fun builderForModule(module: TestModule): StringBuilder = builderForModule(module.name)

    /**
     * 执行 `builderForModule` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    fun builderForModule(moduleName: String): StringBuilder {
        return builderByModule.getOrPut(moduleName, ::StringBuilder)
    }

    /**
     * 执行 `generateResultingDump` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    fun generateResultingDump(): String {
        builderByModule.values.singleOrNull()?.let {
            it.addNewLineIfNeeded()
            return it.toString()
        }

        return buildString {
            for ((moduleName, builder) in builderByModule) {
                moduleHeaderTemplate?.let { appendLine(String.format(it, moduleName)) }
                append(builder)
            }
            addNewLineIfNeeded()
        }
    }

    /**
     * 执行 `isEmpty` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    fun isEmpty(): Boolean = builderByModule.isEmpty()

    /**
     * 提供 `addNewLineIfNeeded` 对应的测试工具流程，维持测试框架的阶段契约。
     */
    private fun StringBuilder.addNewLineIfNeeded() {
        if (isEmpty()) return
        if (last() != '\n') {
            appendLine()
        }
    }
}
