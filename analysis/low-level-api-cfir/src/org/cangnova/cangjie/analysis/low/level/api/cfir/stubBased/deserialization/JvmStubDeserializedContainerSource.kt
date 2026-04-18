

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.descriptors.SourceFile
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.resolve.jvm.JvmClassName
import org.cangnova.cangjie.serialization.deserialization.IncompatibleVersionErrorData
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.cangnova.cangjie.serialization.deserialization.descriptors.PreReleaseInfo

//required for LLDependenciesSymbolProvider#jvmClassName, to resolve ambiguities
//todo check if moving builtins to stubs would solve the issue
internal class JvmStubDeserializedContainerSource(classId: ClassId) : DeserializedContainerSourceWithJvmClassName {
    override val className = JvmClassName.byClassId(classId)

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