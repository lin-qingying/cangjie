// MODULE: lib
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: lib.cj
package sample.lib

public type MyByte = UInt8

public func bytesOf(text: String): Array<MyByte> {
    let result = Array<MyByte>(text.size, repeat: 0u8)
    return result
}

// MODULE: main(lib)
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
package sample.main

import sample.lib.*

// 库签名里的别名类型 + Array 的 extend 成员（size/isEmpty 来自 std.core 的 extend Array<T>）：
// 两者都不应产生诊断。
func take(): Array<UInt8> {
    let bytes = bytesOf("hi")
    let length = bytes.size
    let empty = bytes.isEmpty()
    if (length == 0 || empty) {
        return []
    }
    return bytes
}
