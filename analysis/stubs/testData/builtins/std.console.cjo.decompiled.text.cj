// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.console

import std.core.*
import std.io.InputStream
import std.sync.*
import std.io.OutputStream

@Deprecated[message: "Use related global functions in the std.env instead."]
public class Console {
    public static prop stdIn: std.console.ConsoleReader {
        get() { /* compiled code */ }
    }

    public static prop stdOut: std.console.ConsoleWriter {
        get() { /* compiled code */ }
    }

    public static prop stdErr: std.console.ConsoleWriter {
        get() { /* compiled code */ }
    }
}

@Deprecated[message: "This class migrate to the std.env."]
public class ConsoleReader <: std.core.Object & std.io.InputStream {
    private var buffer: std.core.Array<UInt8>

    private var endIndex: Int64

    private var startIndex: Int64

    public func read(): std.core.Option<Rune> { /* compiled code */ }

    public func readln(): std.core.Option<std.core.String> { /* compiled code */ }

    public func readToEnd(): std.core.Option<std.core.String> { /* compiled code */ }

    public func readUntil(ch: Rune): std.core.Option<std.core.String> { /* compiled code */ }

    public func readUntil(predicate: (Rune) -> Bool): std.core.Option<std.core.String> { /* compiled code */ }

    public func read(arr: std.core.Array<UInt8>): Int64 { /* compiled code */ }
}

@Deprecated[message: "This class migrate to the std.env."]
public class ConsoleWriter <: std.core.Object & std.io.OutputStream {
    private let mutex: std.sync.ReentrantMutex

    private let bufferSize: Int64

    private let buffer_: std.core.Array<UInt8>

    private let fd_: Int32

    public func flush(): Unit { /* compiled code */ }

    public func write(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public func writeln(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public func write(v: std.core.String): Unit { /* compiled code */ }

    public func write<T>(v: T): Unit { /* compiled code */ }

    public func write(v: Bool): Unit { /* compiled code */ }

    public func write(v: Int8): Unit { /* compiled code */ }

    public func write(v: Int16): Unit { /* compiled code */ }

    public func write(v: Int32): Unit { /* compiled code */ }

    public func write(v: Int64): Unit { /* compiled code */ }

    public func write(v: UInt8): Unit { /* compiled code */ }

    public func write(v: UInt16): Unit { /* compiled code */ }

    public func write(v: UInt32): Unit { /* compiled code */ }

    public func write(v: UInt64): Unit { /* compiled code */ }

    public func write(v: Float16): Unit { /* compiled code */ }

    public func write(v: Float32): Unit { /* compiled code */ }

    public func write(v: Float64): Unit { /* compiled code */ }

    public func write(v: Rune): Unit { /* compiled code */ }

    public func writeln(v: std.core.String): Unit { /* compiled code */ }

    public func writeln<T>(v: T): Unit { /* compiled code */ }

    public func writeln(v: Bool): Unit { /* compiled code */ }

    public func writeln(v: Int8): Unit { /* compiled code */ }

    public func writeln(v: Int16): Unit { /* compiled code */ }

    public func writeln(v: Int32): Unit { /* compiled code */ }

    public func writeln(v: Int64): Unit { /* compiled code */ }

    public func writeln(v: UInt8): Unit { /* compiled code */ }

    public func writeln(v: UInt16): Unit { /* compiled code */ }

    public func writeln(v: UInt32): Unit { /* compiled code */ }

    public func writeln(v: UInt64): Unit { /* compiled code */ }

    public func writeln(v: Float16): Unit { /* compiled code */ }

    public func writeln(v: Float32): Unit { /* compiled code */ }

    public func writeln(v: Float64): Unit { /* compiled code */ }

    public func writeln(v: Rune): Unit { /* compiled code */ }
}

internal struct StructureString {
    internal let buffer: std.core.CPointer<UInt8>

    internal let length: UIntNative

    init() { /* compiled code */ }
}
