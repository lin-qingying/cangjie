package org.cangnova.cangjie.test.util

import org.cangnova.cangjie.test.model.TestModule

class MultiModuleInfoDumper(
    private val moduleHeaderTemplate: String? = "Module: %s",
) {
    private val builderByModule = LinkedHashMap<String, StringBuilder>()

    fun builderForModule(module: TestModule): StringBuilder = builderForModule(module.name)

    fun builderForModule(moduleName: String): StringBuilder {
        return builderByModule.getOrPut(moduleName, ::StringBuilder)
    }

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

    fun isEmpty(): Boolean = builderByModule.isEmpty()

    private fun StringBuilder.addNewLineIfNeeded() {
        if (isEmpty()) return
        if (last() != '\n') {
            appendLine()
        }
    }
}
