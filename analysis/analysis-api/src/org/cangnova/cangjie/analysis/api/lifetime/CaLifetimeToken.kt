package org.cangnova.cangjie.analysis.api.lifetime

abstract class CaLifetimeToken {
    abstract fun isValid(): Boolean

    abstract fun getInvalidationReason(): String

    abstract fun isAccessible(): Boolean

    abstract fun getInaccessibilityReason(): String
}

fun CaLifetimeToken.assertIsValid() {
    if (!isValid()) {
        throw CaInvalidLifetimeOwnerAccessException(getInvalidationReason())
    }
}

fun CaLifetimeToken.assertIsAccessible() {
    if (!isAccessible()) {
        throw CaInaccessibleLifetimeOwnerAccessException(getInaccessibilityReason())
    }
}

fun CaLifetimeToken.assertIsValidAndAccessible() {
    assertIsValid()
    assertIsAccessible()
}
