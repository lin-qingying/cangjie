// MODULE: libAlias
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: alias.cj
package sample.alias

public type MyByte = UInt8

// MODULE: libUser(libAlias)
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: user.cj
package sample.user

import sample.alias.*

// 跨包场景：本包签名引用**另一个库包**里的 type 别名（真实 IDE 里就是 std.io 引用 std.core.Byte）。
public func bytesOf(text: String): Array<MyByte> {
    return Array<MyByte>(text.size, repeat: 0u8)
}

public func pairOf(value: Int64): (MyByte, MyByte) {
    return (0u8, 0u8)
}

// MODULE: main(libUser, libAlias)
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
package sample.main

import sample.user.*

// 库 A 的签名使用了库 B 的别名：STUBS 链路必须把它解析成别名并展开，
// 否则会报 RETURN_TYPE_MISMATCH(Array<UInt8> vs Array<MyByte>) 与 NOT_MEMBER_OF('size')。
func takeBytes(): Array<UInt8> {
    let bytes = bytesOf("hi")
    let length = bytes.size
    if (length == 0) {
        return []
    }
    return bytes
}

func takePair(): (UInt8, UInt8) {
    return pairOf(1)
}
