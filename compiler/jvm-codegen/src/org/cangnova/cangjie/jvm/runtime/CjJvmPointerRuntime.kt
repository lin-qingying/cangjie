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
    /**
     * 下一个可分配的进程内虚拟地址。
     */
    private val nextAddress = AtomicLong(1L)
    /**
     * 虚拟地址到 ByteBuffer pointer carrier 的映射。
     */
    private val pointersByAddress = ConcurrentHashMap<Long, ByteBuffer>()
    /**
     * ByteBuffer 实例到虚拟地址的 identity 映射。
     */
    private val addressesByPointer = IdentityHashMap<ByteBuffer, Long>()

    /**
     * 将 ByteBuffer pointer carrier 转换为稳定的进程内虚拟地址。
     */
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

    /**
     * 将先前注册过的虚拟地址还原为 ByteBuffer pointer carrier。
     */
    @JvmStatic
    fun fromAddress(address: Long): ByteBuffer {
        return pointersByAddress[address]
            ?: throw IllegalArgumentException("unknown CHIR JVM pointer address: $address")
    }
}
