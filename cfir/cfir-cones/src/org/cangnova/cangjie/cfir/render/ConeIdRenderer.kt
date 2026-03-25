package org.cangnova.cangjie.cfir.render

import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId

abstract class ConeIdRenderer {

    lateinit var builder: StringBuilder

    abstract fun renderClassId(classId: ClassId)
    abstract fun renderCallableId(callableId: CallableId)
}

/**
 * 仅渲染短名（不含包名）的 ID 渲染器。
 *
 * 用于常规可读输出，避免日志过长。
 */
class ConeShortIdRenderer : ConeIdRenderer() {
    override fun renderClassId(classId: ClassId) {
        builder.append(classId.shortClassName.asString())
    }

    override fun renderCallableId(callableId: CallableId) {
        builder.append(callableId.callableName.asString())
    }
}

/**
 * 渲染全限定名的 ID 渲染器。
 *
 * 用于调试信息，确保跨模块定位时不丢失语义上下文。
 */
class ConeFullyQualifiedIdRenderer : ConeIdRenderer() {
    override fun renderClassId(classId: ClassId) {
        builder.append(classId.asFqNameString())
    }

    override fun renderCallableId(callableId: CallableId) {
        builder.append(callableId.asFqNameForDebugInfo().asString())
    }
}
