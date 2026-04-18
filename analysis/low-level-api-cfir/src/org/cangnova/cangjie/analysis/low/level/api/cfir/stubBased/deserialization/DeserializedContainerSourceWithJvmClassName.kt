

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.resolve.jvm.JvmClassName
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource

interface DeserializedContainerSourceWithJvmClassName : DeserializedContainerSource {
    val className: JvmClassName
}