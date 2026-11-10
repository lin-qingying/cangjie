// MODULE: lib
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: lib.cj
package sample.lib

public type MyByte = UInt8

public func bytesOf(text: String): Array<MyByte> {
    let result = Array<MyByte>(text.size, repeat: 0u8)
    return result
}

