package org.cangnova.cangjie.utils.concurrent.block

/**
 * 可锁定且可清除的延迟初始化值。
 *
 * 对位 Kotlin `compiler/util` 中 `org.jetbrains.kotlin.utils.concurrent.block.LockedClearableLazyValue`。
 */
class LockedClearableLazyValue<out T : Any>(
    val lock: Any,
    val init: () -> T,
) {
    @Volatile
    private var value: T? = null

    fun get(): T {
        val currentValue = value
        if (currentValue != null) {
            return currentValue
        }

        return synchronized(lock) {
            val synchronizedValue = value
            if (synchronizedValue != null) {
                synchronizedValue
            } else {
                val computedValue = init()
                value = computedValue
                computedValue
            }
        }
    }

    fun drop() {
        synchronized(lock) {
            value = null
        }
    }
}
