package org.cangnova.cangjie.utils.concurrent.block

/**
 * 可锁定且可清除的延迟初始化值。
 *
 * 对位 Kotlin `compiler/util` 中 `org.jetbrains.kotlin.utils.concurrent.block.LockedClearableLazyValue`。
 */
class LockedClearableLazyValue<out T : Any>(
    /**
     * 保护懒值初始化和清理的同步锁对象。
     */
    val lock: Any,
    /**
     * 首次读取时用于计算值的初始化函数。
     */
    val init: () -> T,
) {
    /**
     * 已计算的缓存值；为 null 表示尚未初始化或已经被 [drop] 清理。
     */
    @Volatile
    private var value: T? = null

    /**
     * 返回当前懒值，必要时在 [lock] 保护下完成初始化。
     */
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

    /**
     * 清除已缓存的值，使下一次 [get] 重新计算。
     */
    fun drop() {
        synchronized(lock) {
            value = null
        }
    }
}
