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

    /**
     * 预发布二进制元信息，决定容器是否对当前编译流程可见。
     */
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

/**
 * 反序列化容器的 ABI 稳定性分类。
 */
enum class DeserializedContainerAbiStability {
    /**
     * ABI 已稳定，可作为普通二进制输入参与解析。
     */
    STABLE,
    /**
     * ABI 尚不稳定，需要在调用方按策略限制可见性或报告诊断。
     */
    UNSTABLE,
}

/**
 * 对齐 Kotlin `PreReleaseInfo`。
 */
data class PreReleaseInfo(
    /**
     * 当前预发布容器是否应对调用方隐藏。
     */
    val isInvisible: Boolean,
    /**
     * 导致预发布容器具有污染性的语言特性列表。
     */
    val poisoningFeatures: List<String> = emptyList(),
) {
    companion object {
        /**
         * 默认的可见预发布信息，表示没有额外隐藏或污染特性。
         */
        val DEFAULT_VISIBLE = PreReleaseInfo(isInvisible = false)
    }
}
