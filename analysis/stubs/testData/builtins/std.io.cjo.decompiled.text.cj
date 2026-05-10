// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.io

import std.core.*

let HIGH_1_UInt8: UInt8

class BufferedInputStream <: std.core.Object & std.io.InputStream {
    var inputStream: T

    let inBuf: std.core.Array<UInt8>

    var curPos: Int64

    var availLen: Int64

    init(input: T) { /* compiled code */ }

    init(input: T, capacity: Int64) { /* compiled code */ }

    init(input: T, buffer: std.core.Array<UInt8>) { /* compiled code */ }

    func read(buffer: std.core.Array<UInt8>): Int64

    func readByte: std.core.Option<UInt8>

    func reset(input: T): Unit
}

interface InputStream {
    func read(buffer: std.core.Array<UInt8>): Int64
}

class ByteBuffer <: std.core.Object & std.io.IOStream & std.io.Seekable {
    var myData: std.core.Array<UInt8>

    var start: Int64

    var _length: Int64

    init() { /* compiled code */ }

    init(capacity: Int64) { /* compiled code */ }

    init(source: std.core.Array<UInt8>) { /* compiled code */ }

    prop capacity: Int64

    func clone: std.io.ByteBuffer

    func clear: Unit

    func bytes: std.core.Array<UInt8>

    func read(buffer: std.core.Array<UInt8>): Int64

    func readByte: std.core.Option<UInt8>

    func write(buffer: std.core.Array<UInt8>): Unit

    func writeByte(v: UInt8): Unit

    func setLength(length: Int64): Unit

    func reserve(addition: Int64): Unit

    func seek(sp: std.io.SeekPosition): Int64
}

class ContentFormatException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

enum SeekPosition {
    | Current(Int64)

    | Begin(Int64)

    | End(Int64)
}

interface Seekable {
    func seek(sp: std.io.SeekPosition): Int64

    prop position: Int64

    prop remainLength: Int64

    prop length: Int64
}

extend BufferedInputStream <: Resource {
    func close: Unit

    func isClosed: Bool
}

extend BufferedInputStream <: Seekable {
    func seek(sp: std.io.SeekPosition): Int64

    prop position: Int64

    prop remainLength: Int64

    prop length: Int64
}

class BufferedOutputStream <: std.core.Object & std.io.OutputStream {
    var outputStream: T

    var outBuf: std.core.Array<UInt8>

    var curPos: Int64

    init(output: T) { /* compiled code */ }

    init(output: T, buffer: std.core.Array<UInt8>) { /* compiled code */ }

    init(output: T, capacity: Int64) { /* compiled code */ }

    func write(buffer: std.core.Array<UInt8>): Unit

    func writeByte(v: UInt8): Unit

    func flush: Unit

    func reset(output: T): Unit
}

interface OutputStream {
    func write(buffer: std.core.Array<UInt8>): Unit

    func flush: Unit
}

extend BufferedOutputStream <: Resource {
    func close: Unit

    func isClosed: Bool
}

extend BufferedOutputStream <: Seekable {
    func seek(sp: std.io.SeekPosition): Int64

    prop position: Int64

    prop remainLength: Int64

    prop length: Int64
}

interface IOStream <: std.io.InputStream & std.io.OutputStream {
}

class ChainedInputStream <: std.core.Object & std.io.InputStream {
    let inputArr: std.core.Array<T>

    var curIndex: Int64

    init(input: std.core.Array<T>) { /* compiled code */ }

    func read(buffer: std.core.Array<UInt8>): Int64
}

class IOException <: std.core.Exception {
    init() { /* compiled code */ }

    init(message: std.core.String) { /* compiled code */ }

    func getClassName: std.core.String
}

func readString(from: T): std.core.String

func readStringUnchecked(from: T): std.core.String

func readToEnd(from: T): std.core.Array<UInt8>

func copy(from: std.io.InputStream, to: std.io.OutputStream): Int64

class MultiOutputStream <: std.core.Object & std.io.OutputStream {
    var outputArr: std.core.Array<T>

    init(output: std.core.Array<T>) { /* compiled code */ }

    func write(buffer: std.core.Array<UInt8>): Unit

    func flush: Unit
}

class StringReader {
    let inputBIS: std.io.BufferedInputStream<T>

    init(input: T) { /* compiled code */ }

    func read: std.core.Option<Rune>

    func runes: std.core.Iterator<Rune>

    func readlnFromBuf(bas: std.io.ByteBuffer, haveCR_: Bool): (Bool, Bool)

    func readln: std.core.Option<std.core.String>

    func lines: std.core.Iterator<std.core.String>

    func readToEnd: std.core.String

    func readUntil(v: Rune): std.core.Option<std.core.String>

    func readUntil(predicate: (Rune) -> Bool): std.core.Option<std.core.String>

    func isFinished: Bool

    func updateInBufInfo(readNum: Int64): Unit

    func readCharFromInbuf: (Rune, Int64)
}

extend StringReader <: Resource {
    func close: Unit

    func isClosed: Bool
}

extend StringReader <: Seekable {
    func seek(sp: std.io.SeekPosition): Int64

    prop position: Int64
}

class StringWriter {
    var outputBOS: std.io.BufferedOutputStream<T>

    init(output: T) { /* compiled code */ }

    func flush: Unit

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

    func writeln: Unit

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

extend StringWriter <: Resource {
    func close: Unit

    func isClosed: Bool
}

extend StringWriter <: Seekable {
    func seek(sp: std.io.SeekPosition): Int64
}
