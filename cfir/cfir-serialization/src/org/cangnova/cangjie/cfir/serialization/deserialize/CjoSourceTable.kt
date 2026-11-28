package org.cangnova.cangjie.cfir.serialization.deserialize

import com.google.flatbuffers.Table
import java.nio.ByteBuffer

/**
 * 复用生成 accessor 的 vtable 读取，保存内联 F/P/union 宿主的真实地址。
 * 地址只在所属 loaded context 内解释，不反射 bb_pos、不把 payload 地址当成宿主身份。
 */
internal class CjoSourceTable : Table() {
    fun at(position: Int, buffer: ByteBuffer): CjoSourceTable = apply { __reset(position, buffer) }
    fun position(): Int = bb_pos
    fun tablePosition(field: Int, ordinal: Int? = null): Int {
        val offset = __offset(field)
        check(offset != 0) { "Missing owned SOURCE inline table field" }
        return if (ordinal == null) __indirect(offset + bb_pos) else __indirect(__vector(offset) + ordinal * Int.SIZE_BYTES)
    }
}

