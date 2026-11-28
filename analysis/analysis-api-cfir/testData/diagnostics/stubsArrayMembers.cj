package stubs_array_members

// IDE（STUBS）链路的两个症状：库签名里的 `Array<Byte>` 与库 extend 提供的 `Array.size`。
// `Byte` 是 std.core 的 typealias；`.cjo` 用 TypeKind.Type 记录别名类型。
// 期望：两者都不产生诊断（官方 cjc 1.1.3 编译通过）。
import std.io.*

func arraySize(capacity: Int64): Int64 {
    let bytes = Array<Byte>(capacity, repeat: 0)
    return bytes.size
}

func bufferBytes(buffer: ByteBuffer, capacity: Int64): Int64 {
    let filled = Array<Byte>(capacity, repeat: 0)
    buffer.write(filled)
    let result: Array<Byte> = buffer.bytes()
    // 库返回类型 `Array<Byte>` 上继续取 `size`：别名未解析时这里会退化成
    // `'size' is not a member of 'Array'`。
    return result.size
}
