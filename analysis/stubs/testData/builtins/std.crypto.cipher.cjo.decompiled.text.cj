// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.crypto.cipher

import std.core.*

interface BlockCipher {
    @Annotation
    abstract open prop blockSize: Int64

    @Annotation
    abstract open prop algorithm: std.core.String

    open func encrypt(input: std.core.Array<UInt8>): std.core.Array<UInt8> { /* compiled code */ }

    open func decrypt(input: std.core.Array<UInt8>): std.core.Array<UInt8> { /* compiled code */ }

    abstract open func encrypt(input: std.core.Array<UInt8>, to: std.core.Array<UInt8>): Int64

    abstract open func decrypt(input: std.core.Array<UInt8>, to: std.core.Array<UInt8>): Int64
}
