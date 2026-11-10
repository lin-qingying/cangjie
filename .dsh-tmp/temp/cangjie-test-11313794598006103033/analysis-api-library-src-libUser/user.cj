






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

