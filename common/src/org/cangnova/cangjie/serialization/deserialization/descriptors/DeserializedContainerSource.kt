package org.cangnova.cangjie.serialization.deserialization.descriptors

import org.cangnova.cangjie.serialization.deserialization.IncompatibleVersionErrorData
import org.cangnova.cangjie.source.CjBinarySourceElement

/**
 * 反序列化容器来源统一进入仓颉 source 体系。
 *
 * 它表示“声明来自哪个二进制语义容器”，而不是声明本身的真实 PSI source。
 */
abstract class DeserializedContainerSource(
    presentableString: String,
    binaryFilePath: String?,
    stableIdentity: Any,
) : CjBinarySourceElement(
    debugText = presentableString,
    binaryFilePath = binaryFilePath,
    stableIdentity = stableIdentity,
) {
    /**
     * 当该 container 来自不兼容的二进制版本时非空。
     */
    abstract val incompatibility: IncompatibleVersionErrorData<*>?

    abstract val preReleaseInfo: PreReleaseInfo

    /**
     * 表示该 container 的 ABI 稳定性。
     */
    abstract val abiStability: DeserializedContainerAbiStability

    /**
     * 仅用于错误消息展示。
     */
    abstract val presentableString: String
}

enum class DeserializedContainerAbiStability {
    STABLE,
    UNSTABLE,
}

/**
 * 对齐 Kotlin `PreReleaseInfo`。
 */
data class PreReleaseInfo(
    val isInvisible: Boolean,
    val poisoningFeatures: List<String> = emptyList(),
) {
    companion object {
        val DEFAULT_VISIBLE = PreReleaseInfo(isInvisible = false)
    }
}
