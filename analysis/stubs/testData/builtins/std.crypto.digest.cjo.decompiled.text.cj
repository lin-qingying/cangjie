// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.crypto.digest

import std.io.InputStream
import std.io.OutputStream
import std.core.*

interface Digest {
    prop size: Int64

    prop blockSize: Int64

    prop algorithm: std.core.String

    func write(buffer: std.core.Array<UInt8>): Unit

    func finish(to: std.core.Array<UInt8>): Unit

    func finish: std.core.Array<UInt8>

    func reset: Unit
}

func digest(algorithm: T, data: std.core.String): std.core.Array<UInt8>

func digest(algorithm: T, input: std.io.InputStream): std.core.Array<UInt8>

func digest(algorithm: T, data: std.core.Array<UInt8>): std.core.Array<UInt8>
