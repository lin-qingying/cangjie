

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.descriptors.SourceFile
import org.cangnova.cangjie.resolve.jvm.JvmClassName
import org.cangnova.cangjie.serialization.deserialization.IncompatibleVersionErrorData
import org.cangnova.cangjie.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import org.cangnova.cangjie.serialization.deserialization.descriptors.PreReleaseInfo

/**
 * Container source for deserialized declarations from ".kotlin_builtins" file.
 *
 * [facadeClassName] points to a facade class (e.g. "kotlin/LibraryCj") and
 * is used to differentiate between different builtins files.
 *
 * We have a dedicated container source for such declaration because we don't want
 * them to be identified as belonging to a facade class.
 *
 * For that specific reason, this class **DOES NOT** implement
 * [org.cangnova.cangjie.load.kotlin.FacadeClassSource],
 * because compiler backend might use instance checks to detect
 * regular facade files.
 *
 * See for KTIJ-27124 for an example of an issue in IR lowerings.
 */
internal class JvmStubDeserializedBuiltInsContainerSource(val facadeClassName: JvmClassName) : DeserializedContainerSource {
    override val incompatibility: IncompatibleVersionErrorData<*>?
        get() = null

    override val preReleaseInfo: PreReleaseInfo
        get() = PreReleaseInfo.DEFAULT_VISIBLE

    override val abiStability: DeserializedContainerAbiStability
        get() = DeserializedContainerAbiStability.STABLE

    override val presentableString: String
        get() = "Declarations from ${BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION} file '${facadeClassName}'"

    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
}
