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
    /**
     * JVM owner internal name 属性键。
     */
    const val OWNER: String = "jvm.owner"
    /**
     * JVM 成员名属性键。
     */
    const val NAME: String = "jvm.name"
    /**
     * JVM 调用类型属性键，例如 static、virtual、special。
     */
    const val INVOKE_KIND: String = "jvm.invokeKind"
    /**
     * JVM 方法或字段 descriptor 属性键。
     */
    const val DESCRIPTOR: String = "jvm.descriptor"
    /**
     * JVM 类型 internal name 或 descriptor 属性键。
     */
    const val TYPE: String = "jvm.type"

    /**
     * 读取必需的字符串 ABI 属性，缺失时报告带 CHIR 节点 ID 的异常。
     */
    fun requireString(
        attributes: Set<ChirAttribute>,
        key: String,
        nodeId: ChirSemanticId,
    ): String {
        return optionalString(attributes, key)
            ?: throw JvmCodegenException("missing required JVM ABI attribute '$key'", nodeId)
    }

    /**
     * 读取可选字符串 ABI 属性。
     */
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
