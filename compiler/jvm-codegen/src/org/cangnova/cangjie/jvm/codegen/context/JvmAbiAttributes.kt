package org.cangnova.cangjie.jvm.codegen.context

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.attribute.ChirStringAttribute
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.jvm.codegen.diagnostics.JvmCodegenException

/**
 * CHIR 到 JVM 的显式 ABI 属性。
 *
 * 后端只读取这些结构化属性，不从 imported symbol 或表达式展示名里猜 owner、descriptor 或调用类型。
 */
object JvmAbiAttributes {
    const val OWNER: String = "jvm.owner"
    const val NAME: String = "jvm.name"
    const val INVOKE_KIND: String = "jvm.invokeKind"
    const val DESCRIPTOR: String = "jvm.descriptor"
    const val TYPE: String = "jvm.type"

    fun requireString(
        attributes: Set<ChirAttribute>,
        key: String,
        nodeId: ChirSemanticId,
    ): String {
        return optionalString(attributes, key)
            ?: throw JvmCodegenException("missing required JVM ABI attribute '$key'", nodeId)
    }

    fun optionalString(
        attributes: Set<ChirAttribute>,
        key: String,
    ): String? {
        return attributes
            .filterIsInstance<ChirStringAttribute>()
            .singleOrNull { it.key == key }
            ?.value
    }
}
