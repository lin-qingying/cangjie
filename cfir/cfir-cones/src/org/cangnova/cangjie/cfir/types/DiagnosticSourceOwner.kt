package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.source.AbstractCjSourceElement

/** 诊断载荷独立拥有的源槽位；不依靠CFIR主source或字符串消息恢复位置。 */
interface DiagnosticSourceOwner {
    val diagnosticSourceSlots: List<DiagnosticSourceSlot>
}

/** 已准备的诊断来源写入，与IR、metadata及缓存代次在同一写事务提交。 */
interface DiagnosticSourceUpdate {
    fun validate()
    fun commit()
}

/** 槽位封装owner的实际存储，准备阶段只读取，提交前按原对象身份复验。 */
class DiagnosticSourceSlot(
    private val read: () -> AbstractCjSourceElement?,
    private val write: (AbstractCjSourceElement) -> Unit,
) {
    fun prepare(mapper: (AbstractCjSourceElement) -> AbstractCjSourceElement): DiagnosticSourceUpdate {
        val before = read()
        val after = before?.let(mapper)
        return object : DiagnosticSourceUpdate {
            private var committed = false

            override fun validate() {
                check(!committed && read() === before) { "Diagnostic source changed before generation adoption." }
            }

            override fun commit() {
                validate()
                if (after != null && after !== before) write(after)
                committed = true
            }
        }
    }
}
