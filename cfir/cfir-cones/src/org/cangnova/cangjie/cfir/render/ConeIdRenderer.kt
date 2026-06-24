package org.cangnova.cangjie.cfir.render

import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId

/**
 * Cone 渲染流程中负责输出声明 ID 的策略基类。
 *
 * 类型渲染器只关心“在当前位置输出 class/callable 标识”，具体是短名、
 * 全限定名还是调试名由该策略决定。
 */
abstract class ConeIdRenderer {

    /**
     * 当前渲染输出目标。
     */
    lateinit var builder: StringBuilder

    /**
     * 渲染类或类成员声明的 [ClassId]。
     */
    abstract fun renderClassId(classId: ClassId)

    /**
     * 渲染可调用声明的 [CallableId]。
     */
    abstract fun renderCallableId(callableId: CallableId)
}

/**
 * 仅渲染短名（不含包名）的 ID 渲染器。
 *
 * 用于常规可读输出，避免日志过长。
 */
class ConeShortIdRenderer : ConeIdRenderer() {
    /**
     * 只输出类短名。
     */
    override fun renderClassId(classId: ClassId) {
        builder.append(classId.shortClassName.asString())
    }

    /**
     * 只输出可调用短名。
     */
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
    /**
     * 输出类的全限定名。
     */
    override fun renderClassId(classId: ClassId) {
        builder.append(classId.asFqNameString())
    }

    /**
     * 输出可调用声明的调试全限定名。
     */
    override fun renderCallableId(callableId: CallableId) {
        builder.append(callableId.asFqNameForDebugInfo().asString())
    }
}
