// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.io

import std.core.*

let HIGH_1_UInt8: UInt8 = COMPILED_CODE /* compiled code */

public class BufferedInputStream<T> <: std.core.Object & std.io.InputStream {
    internal var inputStream: T

    internal let inBuf: std.core.Array<UInt8>

    internal var curPos: Int64 = COMPILED_CODE /* compiled code */

    internal var availLen: Int64 = COMPILED_CODE /* compiled code */

    init(input: T) { /* compiled code */ }

    init(input: T, capacity: Int64) { /* compiled code */ }

    init(input: T, buffer: std.core.Array<UInt8>) { /* compiled code */ }

    public func read(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func readByte(): std.core.Option<UInt8> { /* compiled code */ }

    public func reset(input: T): Unit { /* compiled code */ }
}

public interface InputStream {
    public open func read(buffer: std.core.Array<UInt8>): Int64
}

public class ByteBuffer <: std.core.Object & std.io.IOStream & std.io.Seekable {
    internal var myData: std.core.Array<UInt8>

    internal var start: Int64

    internal var _length: Int64

    init() { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    init(source: std.core.Array<UInt8>) { /* compiled code */ }

    public prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    public func clone(): std.io.ByteBuffer { /* compiled code */ }

    public func clear(): Unit { /* compiled code */ }

    public func bytes(): std.core.Array<UInt8> { /* compiled code */ }

    public func read(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func readByte(): std.core.Option<UInt8> { /* compiled code */ }

    public func write(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public func writeByte(v: UInt8): Unit { /* compiled code */ }

    public func setLength(length: Int64): Unit { /* compiled code */ }

    public func reserve(addition: Int64): Unit { /* compiled code */ }

    public func seek(sp: std.io.SeekPosition): Int64 { /* compiled code */ }
}

public class ContentFormatException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected override func getClassName(): std.core.String { /* compiled code */ }
}

public enum SeekPosition {
    | Current(Int64)

    | Begin(Int64)

    | End(Int64)
}

public interface Seekable {
    public open func seek(sp: std.io.SeekPosition): Int64

    public open prop position: Int64 {
        get() { /* compiled code */ }
    }

    public open prop remainLength: Int64 {
        get() { /* compiled code */ }
    }

    public open prop length: Int64 {
        get() { /* compiled code */ }
    }
}

extend std.io.BufferedInputStream<T> <: Resource {
    public func close(): Unit { /* compiled code */ }

    public func isClosed(): Bool { /* compiled code */ }
}

extend std.io.BufferedInputStream<T> <: Seekable {
    public func seek(sp: std.io.SeekPosition): Int64 { /* compiled code */ }

    public prop position: Int64 {
        get() { /* compiled code */ }
    }

    public prop remainLength: Int64 {
        get() { /* compiled code */ }
    }

    public prop length: Int64 {
        get() { /* compiled code */ }
    }
}

public class BufferedOutputStream<T> <: std.core.Object & std.io.OutputStream {
    internal var outputStream: T

    internal var outBuf: std.core.Array<UInt8>

    internal var curPos: Int64

    init(output: T) { /* compiled code */ }

    init(output: T, buffer: std.core.Array<UInt8>) { /* compiled code */ }

    init(output: T, capacity: Int64) { /* compiled code */ }

    public func write(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public func writeByte(v: UInt8): Unit { /* compiled code */ }

    public func flush(): Unit { /* compiled code */ }

    public func reset(output: T): Unit { /* compiled code */ }
}

public interface OutputStream {
    public open func write(buffer: std.core.Array<UInt8>): Unit

    public open func flush(): Unit { /* compiled code */ }
}

extend std.io.BufferedOutputStream<T> <: Resource {
    public func close(): Unit { /* compiled code */ }

    public func isClosed(): Bool { /* compiled code */ }
}

extend std.io.BufferedOutputStream<T> <: Seekable {
    public func seek(sp: std.io.SeekPosition): Int64 { /* compiled code */ }

    public prop position: Int64 {
        get() { /* compiled code */ }
    }

    public prop remainLength: Int64 {
        get() { /* compiled code */ }
    }

    public prop length: Int64 {
        get() { /* compiled code */ }
    }
}

public interface IOStream <: std.io.InputStream & std.io.OutputStream {
}

public class ChainedInputStream<T> <: std.core.Object & std.io.InputStream {
    private let inputArr: std.core.Array<T>

    private var curIndex: Int64

    init(input: std.core.Array<T>) { /* compiled code */ }

    public func read(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }
}

public open class IOException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    protected open override func getClassName(): std.core.String { /* compiled code */ }
}

public func readString<T>(from: T): std.core.String { /* compiled code */ }

public func readStringUnchecked<T>(from: T): std.core.String { /* compiled code */ }

public func readToEnd<T>(from: T): std.core.Array<UInt8> { /* compiled code */ }

public func copy(from: std.io.InputStream, to!: std.io.OutputStream): Int64 { /* compiled code */ }

public class MultiOutputStream<T> <: std.core.Object & std.io.OutputStream {
    private var outputArr: std.core.Array<T>

    init(output: std.core.Array<T>) { /* compiled code */ }

    public func write(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    public func flush(): Unit { /* compiled code */ }
}

public class StringReader<T> {
    internal let inputBIS: std.io.BufferedInputStream<T>

    init(input: T) { /* compiled code */ }

    @Frozen
    public func read(): std.core.Option<Rune> { /* compiled code */ }

    public func runes(): std.core.Iterator<Rune> { /* compiled code */ }

    private func readlnFromBuf(bas: std.io.ByteBuffer, haveCR_: Bool): (Bool, Bool) { /* compiled code */ }

    @Frozen
    public func readln(): std.core.Option<std.core.String> { /* compiled code */ }

    public func lines(): std.core.Iterator<std.core.String> { /* compiled code */ }

    @Frozen
    public func readToEnd(): std.core.String { /* compiled code */ }

    public func readUntil(v: Rune): std.core.Option<std.core.String> { /* compiled code */ }

    public func readUntil(predicate: (Rune) -> Bool): std.core.Option<std.core.String> { /* compiled code */ }

    private func isFinished(): Bool { /* compiled code */ }

    private func updateInBufInfo(readNum: Int64): Unit { /* compiled code */ }

    private func readCharFromInbuf(): (Rune, Int64) { /* compiled code */ }
}

extend std.io.StringReader<T> <: Resource {
    public func close(): Unit { /* compiled code */ }

    public func isClosed(): Bool { /* compiled code */ }
}

extend std.io.StringReader<T> <: Seekable {
    public func seek(sp: std.io.SeekPosition): Int64 { /* compiled code */ }

    public prop position: Int64 {
        get() { /* compiled code */ }
    }
}

public class StringWriter<T> {
    internal var outputBOS: std.io.BufferedOutputStream<T>

    init(output: T) { /* compiled code */ }

    public func flush(): Unit { /* compiled code */ }

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

    public func writeln(): Unit { /* compiled code */ }

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

extend std.io.StringWriter<T> <: Resource {
    public func close(): Unit { /* compiled code */ }

    public func isClosed(): Bool { /* compiled code */ }
}

extend std.io.StringWriter<T> <: Seekable {
    public func seek(sp: std.io.SeekPosition): Int64 { /* compiled code */ }
}
