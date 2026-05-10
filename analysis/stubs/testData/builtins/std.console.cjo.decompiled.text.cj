// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.console

import std.core.*
import std.io.InputStream
import std.sync.*
import std.io.OutputStream

class Console {
    prop stdIn: std.console.ConsoleReader

    prop stdOut: std.console.ConsoleWriter

    prop stdErr: std.console.ConsoleWriter
}

class ConsoleReader <: std.core.Object & std.io.InputStream {
    var buffer: std.core.Array<UInt8>

    var endIndex: Int64

    var startIndex: Int64

    func read: std.core.Option<Rune>

    func readln: std.core.Option<std.core.String>

    func readToEnd: std.core.Option<std.core.String>

    func readUntil(ch: Rune): std.core.Option<std.core.String>

    func readUntil(predicate: (Rune) -> Bool): std.core.Option<std.core.String>

    func read(arr: std.core.Array<UInt8>): Int64
}

class ConsoleWriter <: std.core.Object & std.io.OutputStream {
    let mutex: std.sync.ReentrantMutex

    let bufferSize: Int64

    let buffer_: std.core.Array<UInt8>

    let fd_: Int32

    func flush: Unit

    func write(buffer: std.core.Array<UInt8>): Unit

    func writeln(buffer: std.core.Array<UInt8>): Unit

    func write(v: std.core.String): Unit

    func write(v: T): Unit

    func write(v: Bool): Unit

    func write(v: Int8): Unit

    func write(v: Int16): Unit

    func write(v: Int32): Unit

    func write(v: Int64): Unit

    func write(v: UInt8): Unit

    func write(v: UInt16): Unit

    func write(v: UInt32): Unit

    func write(v: UInt64): Unit

    func write(v: Float16): Unit

    func write(v: Float32): Unit

    func write(v: Float64): Unit

    func write(v: Rune): Unit

    func writeln(v: std.core.String): Unit

    func writeln(v: T): Unit

    func writeln(v: Bool): Unit

    func writeln(v: Int8): Unit

    func writeln(v: Int16): Unit

    func writeln(v: Int32): Unit

    func writeln(v: Int64): Unit

    func writeln(v: UInt8): Unit

    func writeln(v: UInt16): Unit

    func writeln(v: UInt32): Unit

    func writeln(v: UInt64): Unit

    func writeln(v: Float16): Unit

    func writeln(v: Float32): Unit

    func writeln(v: Float64): Unit

    func writeln(v: Rune): Unit
}

struct StructureString {
    let buffer: CPointer<UInt8>

    let length: UIntNative

    init() { /* compiled code */ }
}
