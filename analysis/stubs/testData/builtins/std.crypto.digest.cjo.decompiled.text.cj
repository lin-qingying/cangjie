// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.crypto.digest

import std.io.InputStream
import std.io.OutputStream
import std.core.*

interface Digest {
    abstract prop size: Int64

    abstract prop blockSize: Int64

    abstract prop algorithm: std.core.String

    abstract func write(buffer: std.core.Array<UInt8>): Unit

    abstract func finish(to: std.core.Array<UInt8>): Unit

    func finish(): std.core.Array<UInt8> { /* compiled code */ }

    abstract func reset(): Unit
}

func digest(algorithm: T, data: std.core.String): std.core.Array<UInt8> { /* compiled code */ }

func digest(algorithm: T, input: std.io.InputStream): std.core.Array<UInt8> { /* compiled code */ }

func digest(algorithm: T, data: std.core.Array<UInt8>): std.core.Array<UInt8> { /* compiled code */ }
