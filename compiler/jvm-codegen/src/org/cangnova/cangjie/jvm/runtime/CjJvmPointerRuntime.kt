package org.cangnova.cangjie.jvm.runtime

import java.nio.ByteBuffer
import java.util.IdentityHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * JVM 后端的 CHIR C pointer carrier 注册表。
 *
 * CHIR 仍以 `CPointer<T>` 表达地址语义；JVM 侧使用 `ByteBuffer` 保存可读写内存，并通过注册表
 * 支持 `ptrtoint` / `inttoptr` 在同一 JVM 进程内做可逆转换。
 */
object CjJvmPointerRuntime {
    private val nextAddress = AtomicLong(1L)
    private val pointersByAddress = ConcurrentHashMap<Long, ByteBuffer>()
    private val addressesByPointer = IdentityHashMap<ByteBuffer, Long>()

    @JvmStatic
    fun toAddress(pointer: ByteBuffer): Long {
        synchronized(addressesByPointer) {
            addressesByPointer[pointer]?.let { return it }
            val address = nextAddress.getAndIncrement()
            addressesByPointer[pointer] = address
            pointersByAddress[address] = pointer
            return address
        }
    }

    @JvmStatic
    fun fromAddress(address: Long): ByteBuffer {
        return pointersByAddress[address]
            ?: throw IllegalArgumentException("unknown CHIR JVM pointer address: $address")
    }
}
