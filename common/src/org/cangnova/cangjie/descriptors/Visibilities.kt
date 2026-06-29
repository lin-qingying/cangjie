package org.cangnova.cangjie.descriptors

/**
 * 仓颉声明可见性单例集合。
 */
object Visibilities {
    /**
     * private 可见性，仅在声明作用域内可见。
     */
    object Private : Visibility("private", isPublicAPI = false) {
        /**
         * private 成员在 import 检查中需要额外校验。
         */
        override fun mustCheckInImports(): Boolean = true
    }

    /**
     * private-to-this 可见性，限制在当前接收者实例上可见。
     */
    object PrivateToThis : Visibility("private_to_this", isPublicAPI = false) {
        /**
         * 内部调试展示名。
         */
        override val internalDisplayName: String
            get() = "private/*private to this*/"

        /**
         * private-to-this 成员在 import 检查中需要额外校验。
         */
        override fun mustCheckInImports(): Boolean = true
    }

    /**
     * protected 可见性，对继承层级可见。
     */
    object Protected : Visibility("protected", isPublicAPI = true) {
        /**
         * protected 成员在 import 检查中需要额外校验。
         */
        override fun mustCheckInImports(): Boolean = true
    }

    /**
     * internal 可见性，对当前模块可见。
     */
    object Internal : Visibility("internal", isPublicAPI = false) {
        /**
         * internal 成员在 import 检查中需要额外校验。
         */
        override fun mustCheckInImports(): Boolean = true
    }

    /**
     * public 可见性，对外公开。
     */
    object Public : Visibility("public", isPublicAPI = true) {
        /**
         * public 成员不需要额外 import 可见性检查。
         */
        override fun mustCheckInImports(): Boolean = false
    }

    /**
     * local 可见性，用于局部声明。
     */
    object Local : Visibility("local", isPublicAPI = false) {
        /**
         * local 声明在 import 检查中按不可公开路径处理。
         */
        override fun mustCheckInImports(): Boolean = true
    }

    /**
     * 继承而来的占位可见性，不应直接参与 import 检查。
     */
    object Inherited : Visibility("inherited", isPublicAPI = false) {
        /**
         * inherited 只是占位值，调用该方法表示使用错误。
         */
        override fun mustCheckInImports(): Boolean {
            throw IllegalStateException("This method shouldn't be invoked for INHERITED visibility")
        }
    }

    /**
     * 由父类型 private 成员造成的不可见占位可见性。
     */
    object InvisibleFake : Visibility("invisible_fake", isPublicAPI = false) {
        /**
         * invisible fake 成员在 import 检查中需要额外校验。
         */
        override fun mustCheckInImports(): Boolean = true

        /**
         * 面向用户展示的不可见原因。
         */
        override val externalDisplayName: String
            get() = "invisible (private in a supertype)"
    }

    /**
     * 未知可见性占位，不应直接参与 import 检查。
     */
    object Unknown : Visibility("unknown", isPublicAPI = false) {
        /**
         * unknown 只是错误恢复占位值，调用该方法表示使用错误。
         */
        override fun mustCheckInImports(): Boolean {
            throw IllegalStateException("This method shouldn't be invoked for UNKNOWN visibility")
        }
    }

    /**
     * 可比较可见性的局部排序表。
     */
    private val ORDERED_VISIBILITIES: Map<Visibility, Int> = buildMap {
        put(PrivateToThis, 0)
        put(Private, 0)
        put(Internal, 1)
        put(Protected, 1)
        put(Public, 2)
    }

    /**
     * 比较两个可见性的开放程度；无法确定顺序时返回 null。
     */
    fun compare(first: Visibility, second: Visibility): Int? {
        val result = first.compareTo(second)
        if (result != null) return result
        val oppositeResult = second.compareTo(first)
        return if (oppositeResult != null) -oppositeResult else null
    }

    /**
     * 使用本文件维护的局部排序比较两个可见性。
     */
    internal fun compareLocal(first: Visibility, second: Visibility): Int? {
        if (first === second) return 0
        val firstIndex = ORDERED_VISIBILITIES[first]
        val secondIndex = ORDERED_VISIBILITIES[second]
        return if (firstIndex == null || secondIndex == null || firstIndex == secondIndex) {
            null
        } else firstIndex - secondIndex
    }

    /**
     * 判断可见性是否属于 private 家族。
     */
    fun isPrivate(visibility: Visibility): Boolean =
        visibility === Private || visibility === PrivateToThis

    /**
     * 声明未显式指定时采用的默认可见性。
     */
    val DEFAULT_VISIBILITY: Visibility = Public
}
