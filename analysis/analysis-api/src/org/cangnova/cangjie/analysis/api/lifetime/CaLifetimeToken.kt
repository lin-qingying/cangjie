package org.cangnova.cangjie.analysis.api.lifetime


public interface CaSessionComponent : CaLifetimeOwner

/**
 * 生命周期令牌（对齐 Kotlin 的 KaLifetimeToken）。
 *
 * 管理分析对象的有效性和可访问性。
 * 令牌一旦失效（代码修改/项目结构变化），不可恢复。
 */
abstract class CaLifetimeToken {
    /** 底层信息是否仍然有效 */
    abstract fun isValid(): Boolean

    /** 失效原因（仅当 isValid() == false 时调用） */
    abstract fun getInvalidationReason(): String

    /** 当前是否可访问（平台特定：IntelliJ 要求读操作中访问） */
    abstract fun isAccessible(): Boolean

    /** 不可访问的原因（仅当 isAccessible() == false 时调用） */
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

abstract class CaIllegalLifetimeOwnerAccessException : IllegalStateException()

class CaInvalidLifetimeOwnerAccessException(override val message: String) : CaIllegalLifetimeOwnerAccessException()

class CaInaccessibleLifetimeOwnerAccessException(override val message: String) : CaIllegalLifetimeOwnerAccessException()
