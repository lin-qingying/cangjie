// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.crypto.cipher

import std.core.*

interface BlockCipher {
    prop blockSize: Int64 {
        get() { /* compiled code */ }
    }

    prop algorithm: std.core.String {
        get() { /* compiled code */ }
    }

    func encrypt(input: std.core.Array<UInt8>): std.core.Array<UInt8> { /* compiled code */ }

    func decrypt(input: std.core.Array<UInt8>): std.core.Array<UInt8> { /* compiled code */ }

    func encrypt(input: std.core.Array<UInt8>, to: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func decrypt(input: std.core.Array<UInt8>, to: std.core.Array<UInt8>): Int64 { /* compiled code */ }
}
