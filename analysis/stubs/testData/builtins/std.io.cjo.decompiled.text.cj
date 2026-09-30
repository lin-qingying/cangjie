// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.io

import std.core.*

@Annotation
let HIGH_1_UInt8: UInt8

class BufferedInputStream<T> <: std.core.Object & std.io.InputStream {
    var inputStream: T

    let inBuf: std.core.Array<UInt8>

    @Annotation
    var curPos: Int64

    @Annotation
    var availLen: Int64

    init(input: T) { /* compiled code */ }

    init(input: T, capacity: Int64) { /* compiled code */ }

    init(input: T, buffer: std.core.Array<UInt8>) { /* compiled code */ }

    func read(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func readByte(): std.core.Option<UInt8> { /* compiled code */ }

    func reset(input: T): Unit { /* compiled code */ }
}

interface InputStream {
    abstract open func read(buffer: std.core.Array<UInt8>): Int64
}

class ByteBuffer <: std.core.Object & std.io.IOStream & std.io.Seekable {
    var myData: std.core.Array<UInt8>

    var start: Int64

    var _length: Int64

    init() { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    init(source: std.core.Array<UInt8>) { /* compiled code */ }

    @Annotation
    prop capacity: Int64 {
        get() { /* compiled code */ }
    }

    func clone(): std.io.ByteBuffer { /* compiled code */ }

    func clear(): Unit { /* compiled code */ }

    func bytes(): std.core.Array<UInt8> { /* compiled code */ }

    func read(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func readByte(): std.core.Option<UInt8> { /* compiled code */ }

    func write(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func writeByte(v: UInt8): Unit { /* compiled code */ }

    func setLength(length: Int64): Unit { /* compiled code */ }

    func reserve(addition: Int64): Unit { /* compiled code */ }

    func seek(sp: std.io.SeekPosition): Int64 { /* compiled code */ }
}

class ContentFormatException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    override func getClassName(): std.core.String { /* compiled code */ }
}

@Annotation
enum SeekPosition {
    | Current(Int64)

    | Begin(Int64)

    | End(Int64)
}

interface Seekable {
    abstract open func seek(sp: std.io.SeekPosition): Int64

    @Annotation
    open prop position: Int64 {
        get() { /* compiled code */ }
    }

    @Annotation
    open prop remainLength: Int64 {
        get() { /* compiled code */ }
    }

    @Annotation
    open prop length: Int64 {
        get() { /* compiled code */ }
    }
}

extend std.io.BufferedInputStream<T> <: Resource {
    func close(): Unit { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }
}

extend std.io.BufferedInputStream<T> <: Seekable {
    func seek(sp: std.io.SeekPosition): Int64 { /* compiled code */ }

    @Annotation
    prop position: Int64 {
        get() { /* compiled code */ }
    }

    @Annotation
    prop remainLength: Int64 {
        get() { /* compiled code */ }
    }

    @Annotation
    prop length: Int64 {
        get() { /* compiled code */ }
    }
}

class BufferedOutputStream<T> <: std.core.Object & std.io.OutputStream {
    var outputStream: T

    var outBuf: std.core.Array<UInt8>

    var curPos: Int64

    init(output: T) { /* compiled code */ }

    init(output: T, buffer: std.core.Array<UInt8>) { /* compiled code */ }

    init(output: T, capacity: Int64) { /* compiled code */ }

    func write(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func writeByte(v: UInt8): Unit { /* compiled code */ }

    func flush(): Unit { /* compiled code */ }

    func reset(output: T): Unit { /* compiled code */ }
}

interface OutputStream {
    abstract open func write(buffer: std.core.Array<UInt8>): Unit

    open func flush(): Unit { /* compiled code */ }
}

extend std.io.BufferedOutputStream<T> <: Resource {
    func close(): Unit { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }
}

extend std.io.BufferedOutputStream<T> <: Seekable {
    func seek(sp: std.io.SeekPosition): Int64 { /* compiled code */ }

    @Annotation
    prop position: Int64 {
        get() { /* compiled code */ }
    }

    @Annotation
    prop remainLength: Int64 {
        get() { /* compiled code */ }
    }

    @Annotation
    prop length: Int64 {
        get() { /* compiled code */ }
    }
}

interface IOStream <: std.io.InputStream & std.io.OutputStream {
}

class ChainedInputStream<T> <: std.core.Object & std.io.InputStream {
    let inputArr: std.core.Array<T>

    var curIndex: Int64

    init(input: std.core.Array<T>) { /* compiled code */ }

    func read(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }
}

open class IOException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    open override func getClassName(): std.core.String { /* compiled code */ }
}

func readString<T>(from: T): std.core.String { /* compiled code */ }

func readStringUnchecked<T>(from: T): std.core.String { /* compiled code */ }

func readToEnd<T>(from: T): std.core.Array<UInt8> { /* compiled code */ }

func copy(from: std.io.InputStream, to: std.io.OutputStream): Int64 { /* compiled code */ }

class MultiOutputStream<T> <: std.core.Object & std.io.OutputStream {
    var outputArr: std.core.Array<T>

    init(output: std.core.Array<T>) { /* compiled code */ }

    func write(buffer: std.core.Array<UInt8>): Unit { /* compiled code */ }

    func flush(): Unit { /* compiled code */ }
}

class StringReader<T> {
    let inputBIS: std.io.BufferedInputStream<T>

    init(input: T) { /* compiled code */ }

    @Frozen
    func read(): std.core.Option<Rune> { /* compiled code */ }

    func runes(): std.core.Iterator<Rune> { /* compiled code */ }

    func readlnFromBuf(bas: std.io.ByteBuffer, haveCR_: Bool): (Bool, Bool) { /* compiled code */ }

    @Frozen
    func readln(): std.core.Option<std.core.String> { /* compiled code */ }

    func lines(): std.core.Iterator<std.core.String> { /* compiled code */ }

    @Frozen
    func readToEnd(): std.core.String { /* compiled code */ }

    func readUntil(v: Rune): std.core.Option<std.core.String> { /* compiled code */ }

    func readUntil(predicate: (Rune) -> Bool): std.core.Option<std.core.String> { /* compiled code */ }

    func isFinished(): Bool { /* compiled code */ }

    func updateInBufInfo(readNum: Int64): Unit { /* compiled code */ }

    func readCharFromInbuf(): (Rune, Int64) { /* compiled code */ }
}

extend std.io.StringReader<T> <: Resource {
    func close(): Unit { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }
}

extend std.io.StringReader<T> <: Seekable {
    func seek(sp: std.io.SeekPosition): Int64 { /* compiled code */ }

    @Annotation
    prop position: Int64 {
        get() { /* compiled code */ }
    }
}

class StringWriter<T> {
    var outputBOS: std.io.BufferedOutputStream<T>

    init(output: T) { /* compiled code */ }

    func flush(): Unit { /* compiled code */ }

    func write(v: std.core.String): Unit { /* compiled code */ }

    func write<T>(v: T): Unit { /* compiled code */ }

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

    func writeln(): Unit { /* compiled code */ }

    func writeln(v: std.core.String): Unit { /* compiled code */ }

    func writeln<T>(v: T): Unit { /* compiled code */ }

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

extend std.io.StringWriter<T> <: Resource {
    func close(): Unit { /* compiled code */ }

    func isClosed(): Bool { /* compiled code */ }
}

extend std.io.StringWriter<T> <: Seekable {
    func seek(sp: std.io.SeekPosition): Int64 { /* compiled code */ }
}
