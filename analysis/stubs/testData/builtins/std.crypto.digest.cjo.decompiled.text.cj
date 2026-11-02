// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.crypto.digest

import std.io.InputStream
import std.io.OutputStream
import std.core.*

public interface Digest {
    public open prop size: Int64

    public open prop blockSize: Int64

    public open prop algorithm: std.core.String

    public open func write(buffer: std.core.Array<UInt8>): Unit

    public open func finish(to!: std.core.Array<UInt8>): Unit

    public open func finish(): std.core.Array<UInt8> { /* compiled code */ }

    public open func reset(): Unit
}

@Deprecated[message: "Use global function `public func digest<T>(algorithm: T, input: InputStream): Array<Byte> where T <: Digest` instead."]
public func digest<T>(algorithm: T, data: std.core.String): std.core.Array<UInt8> { /* compiled code */ }

public func digest<T>(algorithm: T, input: std.io.InputStream): std.core.Array<UInt8> { /* compiled code */ }

public func digest<T>(algorithm: T, data: std.core.Array<UInt8>): std.core.Array<UInt8> { /* compiled code */ }
