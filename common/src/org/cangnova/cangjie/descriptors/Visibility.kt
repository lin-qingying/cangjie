package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.name.FqName

abstract class Visibility protected constructor(
    val name: String,
    val isPublicAPI: Boolean,
) {
    open val internalDisplayName: String
        get() = name

    open val externalDisplayName: String
        get() = internalDisplayName

    abstract fun mustCheckInImports(): Boolean

    open fun compareTo(visibility: Visibility): Int? =
        Visibilities.compareLocal(this, visibility)

    final override fun toString(): String = internalDisplayName

    open fun normalize(): Visibility = this

    open fun visibleFromPackage(fromPackage: FqName, myPackage: FqName): Boolean = true
}
