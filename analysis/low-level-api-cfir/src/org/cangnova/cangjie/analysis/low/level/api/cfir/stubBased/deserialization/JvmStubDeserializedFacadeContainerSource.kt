

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.descriptors.SourceFile
import org.cangnova.cangjie.load.kotlin.FacadeClassSource
import org.cangnova.cangjie.resolve.jvm.JvmClassName
import org.cangnova.cangjie.serialization.deserialization.IncompatibleVersionErrorData
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.cangnova.cangjie.serialization.deserialization.descriptors.PreReleaseInfo

internal class JvmStubDeserializedFacadeContainerSource(
    override val className: JvmClassName,
    override val jvmClassName: JvmClassName?,
    override val facadeClassName: JvmClassName?
) : DeserializedContainerSourceWithJvmClassName, FacadeClassSource {
    override val incompatibility: IncompatibleVersionErrorData<*>?
        get() = null

    override val preReleaseInfo: PreReleaseInfo
        get() = PreReleaseInfo.DEFAULT_VISIBLE

    override val abiStability: DeserializedContainerAbiStability
        get() = DeserializedContainerAbiStability.STABLE

    override val presentableString: String
        get() = className.internalName

    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
}