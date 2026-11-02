// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.crypto.cipher

import std.core.*

public interface BlockCipher {
    public open prop blockSize: Int64

    public open prop algorithm: std.core.String

    public open func encrypt(input: std.core.Array<UInt8>): std.core.Array<UInt8> { /* compiled code */ }

    public open func decrypt(input: std.core.Array<UInt8>): std.core.Array<UInt8> { /* compiled code */ }

    public open func encrypt(input: std.core.Array<UInt8>, to!: std.core.Array<UInt8>): Int64

    public open func decrypt(input: std.core.Array<UInt8>, to!: std.core.Array<UInt8>): Int64
}
