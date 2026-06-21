package org.cangnova.cangjie.cfir.session.services

import org.cangnova.cangjie.name.ClassId

/**
 * extend 目标类型的索引键。
 *
 * 官方前端的 extend map 同时覆盖 nominal declaration 与 built-in type。
 * CFIR 不能只用 ClassId 作为键，否则 `CPointer<T>` / `CString` 这类
 * non-primitive built-in type 会在合法性、成员查询和接口冲突检查中丢失。
 */
sealed interface CfirExtendTargetKey {
    val classIdOrNull: ClassId?

    data class ClassLike(val classId: ClassId) : CfirExtendTargetKey {
        override val classIdOrNull: ClassId get() = classId
        override fun toString(): String = classId.asString()
    }

    data object CPointer : CfirExtendTargetKey {
        override val classIdOrNull: ClassId? get() = null
        override fun toString(): String = "CPointer"
    }

    data object CString : CfirExtendTargetKey {
        override val classIdOrNull: ClassId? get() = null
        override fun toString(): String = "CString"
    }
}
