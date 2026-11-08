package test

// 库签名里的 `Array<Byte>` 元素是 std.core 的 typealias `Byte`；把它作为运算符操作数时，
// 必须按 use-site 展开成 UInt8 才能命中内建运算符表（否则会报找不到运算符）。
import std.io.*

func sumBytes(buffer: ByteBuffer): Byte {
    let bytes = buffer.bytes()
    return bytes[0] + bytes[1]
}
