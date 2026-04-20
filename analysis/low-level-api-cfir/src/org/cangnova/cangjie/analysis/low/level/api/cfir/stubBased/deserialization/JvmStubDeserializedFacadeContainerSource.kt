package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.serialization.deserialization.IncompatibleVersionErrorData
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import org.cangnova.cangjie.serialization.deserialization.descriptors.PreReleaseInfo

/**
 * package 顶层 callable 的反序列化容器来源。
 *
 * 语义以仓颉 package facade / multifile facade 为准，
 * 不再暴露 JVM internal name。
 */
internal class PackageFacadeDeserializedContainerSource(
    val packageFqName: FqName,
    val facadeFqName: FqName,
    val partSimpleName: String?,
    val partSimpleNames: List<String>,
    val isMultifile: Boolean,
    binaryFilePath: String?,
) : DeserializedContainerSource(
    presentableString = if (isMultifile) {
        "package multifile facade ${facadeFqName.asString()} [parts: ${partSimpleNames.joinToString()}]"
    } else {
        "package facade ${facadeFqName.asString()}"
    },
    binaryFilePath = binaryFilePath,
    stableIdentity = listOf(packageFqName, facadeFqName, partSimpleName, partSimpleNames, isMultifile, binaryFilePath),
) {

    override val incompatibility: IncompatibleVersionErrorData<*>?
        get() = null

    override val preReleaseInfo: PreReleaseInfo
        get() = PreReleaseInfo.DEFAULT_VISIBLE

    override val abiStability: DeserializedContainerAbiStability
        get() = DeserializedContainerAbiStability.STABLE

    override val presentableString: String
        get() = if (isMultifile) {
            "package multifile facade ${facadeFqName.asString()} [parts: ${partSimpleNames.joinToString()}]"
        } else {
            "package facade ${facadeFqName.asString()}"
        }
}
