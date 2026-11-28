package org.cangnova.cangjie.cfir.serialization.cjo

import java.util.Collections

/**
 * 原生图表的发布事务。声明、表达式和类型各自拥有一基索引，但共用 reserve/fill 完成合同。
 *
 * 递归引用只取得已预留的身份；Invalid 类型和 FuncArg 的 InvalidExpr 是合法记录，不能用
 * 它们冒充尚未完成的槽。只有全部槽恰好填充一次后，表才封闭并交给格式写出器。
 */
internal class CjoGraphTable<T : Any>(private val domain: String) {
    private sealed interface Slot<out T> {
        data object Reserved : Slot<Nothing>
        class Filled<T>(val value: T) : Slot<T>
    }

    private val slots = ArrayList<Slot<T>>()
    private var finished = false

    val size: Int get() = slots.size

    fun reserve(): UInt {
        check(!finished) { "CJO $domain table is already published" }
        slots += Slot.Reserved
        return slots.size.toUInt()
    }

    fun fill(index: UInt, value: T) {
        check(!finished) { "CJO $domain table is already published" }
        val offset = index.toLong() - 1L
        check(offset >= 0L && offset < slots.size.toLong()) { "CJO $domain slot $index is not reserved" }
        check(slots[offset.toInt()] === Slot.Reserved) { "CJO $domain slot $index is already filled" }
        slots[offset.toInt()] = Slot.Filled(value)
    }

    fun finish(): List<T> {
        check(!finished) { "CJO $domain table is already published" }
        val result = slots.mapIndexed { offset, slot ->
            check(slot is Slot.Filled) { "CJO publication requires $domain slot ${offset + 1} to be filled" }
            slot.value
        }
        finished = true
        return Collections.unmodifiableList(result)
    }
}
