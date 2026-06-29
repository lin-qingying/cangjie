package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.serialization.deserialization.IncompatibleVersionErrorData
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import org.cangnova.cangjie.serialization.deserialization.descriptors.PreReleaseInfo

/**
 * builtins package 容器来源。
 */
internal class BuiltinsDeserializedContainerSource(
    /**
     * builtins package 全限定名。
     */
    val packageFqName: FqName,

    /**
     * builtins facade 全限定名。
     */
    val facadeFqName: FqName,
    binaryFilePath: String?,
) : DeserializedContainerSource(
    presentableString = "builtins package facade ${facadeFqName.asString()}",
    binaryFilePath = binaryFilePath,
    stableIdentity = listOf(packageFqName, facadeFqName, binaryFilePath),
) {

    override val incompatibility: IncompatibleVersionErrorData<*>?
        get() = null

    override val preReleaseInfo: PreReleaseInfo
        get() = PreReleaseInfo.DEFAULT_VISIBLE

    override val abiStability: DeserializedContainerAbiStability
        get() = DeserializedContainerAbiStability.STABLE

    override val presentableString: String
        get() = "builtins package facade ${facadeFqName.asString()}"
}
