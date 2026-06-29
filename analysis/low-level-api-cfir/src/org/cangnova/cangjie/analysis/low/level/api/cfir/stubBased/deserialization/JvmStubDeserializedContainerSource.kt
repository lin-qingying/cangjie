package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.serialization.deserialization.IncompatibleVersionErrorData
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import org.cangnova.cangjie.serialization.deserialization.descriptors.PreReleaseInfo

/**
 * 类声明的二进制容器来源。
 *
 * 这里表达的是“声明归属于哪个 class 容器”，
 * 不再借用 JVM class name 兼容壳。
 */
internal class ClassDeserializedContainerSource(
    /**
     * 当前 class container source 对应的 class id。
     */
    val classId: ClassId,
) : DeserializedContainerSource(
    presentableString = "class ${classId.asString()}",
    binaryFilePath = null,
    stableIdentity = classId,
) {

    override val incompatibility: IncompatibleVersionErrorData<*>?
        get() = null

    override val preReleaseInfo: PreReleaseInfo
        get() = PreReleaseInfo.DEFAULT_VISIBLE

    override val abiStability: DeserializedContainerAbiStability
        get() = DeserializedContainerAbiStability.STABLE

    override val presentableString: String
        get() = "class ${classId.asString()}"
}
