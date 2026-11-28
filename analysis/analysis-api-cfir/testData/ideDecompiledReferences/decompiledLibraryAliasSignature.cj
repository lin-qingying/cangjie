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

// 库签名里的别名类型必须按展开类型参与比较：否则会报
// RETURN_TYPE_MISMATCH(expected Array<UInt8>, actual Array<MyByte>)。
func take(): Array<UInt8> {
    let bytes = bytesOf("hi")
    return bytes
}
