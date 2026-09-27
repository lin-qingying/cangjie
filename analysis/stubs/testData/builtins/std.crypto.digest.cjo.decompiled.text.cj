// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.crypto.digest

import std.io.InputStream
import std.io.OutputStream
import std.core.*

interface Digest {
    @Annotation
    abstract open prop size: Int64

    @Annotation
    abstract open prop blockSize: Int64

    @Annotation
    abstract open prop algorithm: std.core.String

    abstract open func write(buffer: std.core.Array<UInt8>): Unit

    abstract open func finish(to: std.core.Array<UInt8>): Unit

    open func finish(): std.core.Array<UInt8> { /* compiled code */ }

    abstract open func reset(): Unit
}

@Deprecated[message: "Use global function `public func digest<T>(algorithm: T, input: InputStream): Array<Byte> where T <: Digest` instead."]
func digest<T>(algorithm: T, data: std.core.String): std.core.Array<UInt8> { /* compiled code */ }

func digest<T>(algorithm: T, input: std.io.InputStream): std.core.Array<UInt8> { /* compiled code */ }

func digest<T>(algorithm: T, data: std.core.Array<UInt8>): std.core.Array<UInt8> { /* compiled code */ }
