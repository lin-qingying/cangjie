package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.name.FqName

/**
 * 声明可见性的抽象基类。
 */
abstract class Visibility protected constructor(
    /**
     * 可见性的稳定内部名称。
     */
    val name: String,
    /**
     * 该可见性是否表示公开 API。
     */
    val isPublicAPI: Boolean,
) {
    /**
     * 编译器内部展示名。
     */
    open val internalDisplayName: String
        get() = name

    /**
     * 面向用户的展示名。
     */
    open val externalDisplayName: String
        get() = internalDisplayName

    /**
     * import 可见性检查是否需要特别处理该可见性。
     */
    abstract fun mustCheckInImports(): Boolean

    /**
     * 比较当前可见性与另一个可见性的开放程度。
     */
    open fun compareTo(visibility: Visibility): Int? =
        Visibilities.compareLocal(this, visibility)

    /**
     * 返回内部展示名。
     */
    final override fun toString(): String = internalDisplayName

    /**
     * 返回语义等价的规范化可见性。
     */
    open fun normalize(): Visibility = this

    /**
     * 判断从指定包访问声明所在包是否可见。
     */
    open fun visibleFromPackage(fromPackage: FqName, myPackage: FqName): Boolean = true
}
