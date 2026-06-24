package org.cangnova.cangjie.cfir.render

import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId

/**
 * `renderForDebugging()` 使用的 ID 渲染策略。
 *
 * 该策略偏向短而稳定的调试文本：类使用 [ClassId.asString]，可调用只输出 callable name。
 */
class ConeIdRendererForDebugging : ConeIdRenderer() {
    /**
     * 输出 ClassId 的调试字符串。
     */
    override fun renderClassId(classId: ClassId) {
        builder.append(classId.asString())
    }

    /**
     * 输出可调用名称。
     */
    override fun renderCallableId(callableId: CallableId) {
        builder.append(callableId.callableName)
    }
}
