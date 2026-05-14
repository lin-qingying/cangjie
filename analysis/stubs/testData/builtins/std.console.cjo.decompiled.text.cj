// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.console

import std.core.*
import std.io.InputStream
import std.sync.*
import std.io.OutputStream

class Console {
    static prop stdIn: std.console.ConsoleReader {
        get() { /* compiled code */ }
    }

    static prop stdOut: std.console.ConsoleWriter {
        get() { /* compiled code */ }
    }

    static prop stdErr: std.console.ConsoleWriter {
        get() { /* compiled code */ }
    }
}

class ConsoleReader <: std.core.Object & std.io.InputStream {
    var buffer: std.core.Array<UInt8>

    var endIndex: Int64

    var startIndex: Int64

    func read(): std.core.Option<Rune> { /* compiled code */ }

    func readln(): std.core.Option<std.core.String> { /* compiled code */ }

    func readToEnd(): std.core.Option<std.core.String> { /* compiled code */ }

    func readUntil(ch: Rune): std.core.Option<std.core.String> { /* compiled code */ }

    func readUntil(predicate: (Rune) -> Bool): std.core.Option<std.core.String> { /* compiled code */ }

    func read(arr: std.core.Array<UInt8>): Int64 { /* compiled code */ }
}

class ConsoleWriter <: std.core.Object & std.io.OutputStream {
    let mutex: std.sync.ReentrantMutex

    let bufferSize: Int64

    let buffer_: std.core.Array<UInt8>

    let fd_: Int32

    func flush(): Unit { /* compiled code */ }

    func write(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func writeln(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func write(v: std.core.String): Unit { /* compiled code */ }

    func write(v: T): Unit { /* compiled code */ }

    func write(v: Bool): Unit { /* compiled code */ }

    func write(v: Int8): Unit { /* compiled code */ }

    func write(v: Int16): Unit { /* compiled code */ }

    func write(v: Int32): Unit { /* compiled code */ }

    func write(v: Int64): Unit { /* compiled code */ }

    func write(v: UInt8): Unit { /* compiled code */ }

    func write(v: UInt16): Unit { /* compiled code */ }

    func write(v: UInt32): Unit { /* compiled code */ }

    func write(v: UInt64): Unit { /* compiled code */ }

    func write(v: Float16): Unit { /* compiled code */ }

    func write(v: Float32): Unit { /* compiled code */ }

    func write(v: Float64): Unit { /* compiled code */ }

    func write(v: Rune): Unit { /* compiled code */ }

    func writeln(v: std.core.String): Unit { /* compiled code */ }

    func writeln(v: T): Unit { /* compiled code */ }

    func writeln(v: Bool): Unit { /* compiled code */ }

    func writeln(v: Int8): Unit { /* compiled code */ }

    func writeln(v: Int16): Unit { /* compiled code */ }

    func writeln(v: Int32): Unit { /* compiled code */ }

    func writeln(v: Int64): Unit { /* compiled code */ }

    func writeln(v: UInt8): Unit { /* compiled code */ }

    func writeln(v: UInt16): Unit { /* compiled code */ }

    func writeln(v: UInt32): Unit { /* compiled code */ }

    func writeln(v: UInt64): Unit { /* compiled code */ }

    func writeln(v: Float16): Unit { /* compiled code */ }

    func writeln(v: Float32): Unit { /* compiled code */ }

    func writeln(v: Float64): Unit { /* compiled code */ }

    func writeln(v: Rune): Unit { /* compiled code */ }
}

struct StructureString {
    let buffer: CPointer<UInt8>

    let length: UIntNative

    init() { /* compiled code */ }
}
