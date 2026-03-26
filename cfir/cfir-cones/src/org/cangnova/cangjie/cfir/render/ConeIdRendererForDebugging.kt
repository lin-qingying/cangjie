package org.cangnova.cangjie.cfir.render

import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId

class ConeIdRendererForDebugging : ConeIdRenderer() {
    override fun renderClassId(classId: ClassId) {
        builder.append(classId.asString())
    }

    override fun renderCallableId(callableId: CallableId) {
        builder.append(callableId.callableName)
    }
}