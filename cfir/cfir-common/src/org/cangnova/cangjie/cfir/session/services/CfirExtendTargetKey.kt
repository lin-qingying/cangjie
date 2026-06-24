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
    /**
     * nominal declaration 目标对应的 [ClassId]。
     *
     * 对内建目标类型返回 `null`，调用方必须使用完整的 [CfirExtendTargetKey] 区分目标。
     */
    val classIdOrNull: ClassId?

    /**
     * 以普通类、接口、结构体、枚举或类型别名声明为目标的 extend 键。
     *
     * @property classId 目标声明的类标识。
     */
    data class ClassLike(val classId: ClassId) : CfirExtendTargetKey {
        /**
         * 返回 nominal declaration 的 [ClassId]。
         */
        override val classIdOrNull: ClassId get() = classId

        /**
         * 使用类标识作为稳定调试文本。
         */
        override fun toString(): String = classId.asString()
    }

    /**
     * `CPointer<T>` 内建目标类型的 extend 键。
     */
    data object CPointer : CfirExtendTargetKey {
        /**
         * `CPointer<T>` 没有 nominal [ClassId]。
         */
        override val classIdOrNull: ClassId? get() = null

        /**
         * 返回用于索引排序和调试输出的稳定名称。
         */
        override fun toString(): String = "CPointer"
    }

    /**
     * `CString` 内建目标类型的 extend 键。
     */
    data object CString : CfirExtendTargetKey {
        /**
         * `CString` 没有 nominal [ClassId]。
         */
        override val classIdOrNull: ClassId? get() = null

        /**
         * 返回用于索引排序和调试输出的稳定名称。
         */
        override fun toString(): String = "CString"
    }
}
