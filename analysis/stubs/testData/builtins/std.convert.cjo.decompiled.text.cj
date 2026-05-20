// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.convert

import std.core.*

func parseFloat(data: std.core.String): T { /* compiled code */ }

interface Formattable {
    abstract func format(fmt: std.core.String): std.core.String
}

enum Flags {
    | NoFlag

    | Minus

    | Plus

    | Sharp

    | Zero

    operator func ==(right: std.convert.Flags): Bool { /* compiled code */ }

    operator func !=(right: std.convert.Flags): Bool { /* compiled code */ }
}

extend `Unit` <: Formattable {
    func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend `Unit` <: Formattable {
    func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend `Unit` <: Formattable {
    func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend `Unit` <: Formattable {
    func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend `Unit` <: Formattable {
    func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend `Unit` <: Formattable {
    func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend `Unit` <: Formattable {
    func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend `Unit` <: Formattable {
    func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend `Unit` <: Formattable {
    func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend `Unit` <: Formattable {
    func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend `Unit` <: Formattable {
    func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend `Unit` <: Formattable {
    func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

class Verify {
    var data: std.core.Array<Rune>

    var size: Int64

    var index: Int64

    init(fmt: std.core.String) { /* compiled code */ }

    func verifyIntFormatArg(): Bool { /* compiled code */ }

    func verifyFloatFormatArg(): Bool { /* compiled code */ }
}

func parseFormat(fmt: std.core.String): (std.convert.Flags, Int64, Int64, Rune) { /* compiled code */ }

func escapeControlCharacters(str: std.core.String): std.core.String { /* compiled code */ }

interface Parsable {
    abstract static func parse(value: std.core.String): T

    abstract static func tryParse(value: std.core.String): std.core.Option<T>
}

interface RadixConvertible {
    abstract static func parse(value: std.core.String, radix: Int64): T

    abstract static func tryParse(value: std.core.String, radix: Int64): std.core.Option<T>

    abstract func toString(radix: Int64): std.core.String
}

extend `Unit` <: Parsable {
    static func parse(data: std.core.String): Int8 { /* compiled code */ }

    static func tryParse(data: std.core.String): std.core.Option<Int8> { /* compiled code */ }
}

extend `Unit` <: RadixConvertible {
    static func parse(value: std.core.String, radix: Int64): Int8 { /* compiled code */ }

    static func tryParse(value: std.core.String, radix: Int64): std.core.Option<Int8> { /* compiled code */ }

    func toString(radix: Int64): std.core.String { /* compiled code */ }
}

extend `Unit` <: Parsable {
    static func parse(data: std.core.String): UInt8 { /* compiled code */ }

    static func tryParse(data: std.core.String): std.core.Option<UInt8> { /* compiled code */ }
}

extend `Unit` <: RadixConvertible {
    static func parse(value: std.core.String, radix: Int64): UInt8 { /* compiled code */ }

    static func tryParse(value: std.core.String, radix: Int64): std.core.Option<UInt8> { /* compiled code */ }

    func toString(radix: Int64): std.core.String { /* compiled code */ }
}

extend `Unit` <: Parsable {
    static func parse(data: std.core.String): Int16 { /* compiled code */ }

    static func tryParse(data: std.core.String): std.core.Option<Int16> { /* compiled code */ }
}

extend `Unit` <: RadixConvertible {
    static func parse(value: std.core.String, radix: Int64): Int16 { /* compiled code */ }

    static func tryParse(value: std.core.String, radix: Int64): std.core.Option<Int16> { /* compiled code */ }

    func toString(radix: Int64): std.core.String { /* compiled code */ }
}

extend `Unit` <: Parsable {
    static func parse(data: std.core.String): UInt16 { /* compiled code */ }

    static func tryParse(data: std.core.String): std.core.Option<UInt16> { /* compiled code */ }
}

extend `Unit` <: RadixConvertible {
    static func parse(value: std.core.String, radix: Int64): UInt16 { /* compiled code */ }

    static func tryParse(value: std.core.String, radix: Int64): std.core.Option<UInt16> { /* compiled code */ }

    func toString(radix: Int64): std.core.String { /* compiled code */ }
}

extend `Unit` <: Parsable {
    static func parse(data: std.core.String): Int32 { /* compiled code */ }

    static func tryParse(data: std.core.String): std.core.Option<Int32> { /* compiled code */ }
}

extend `Unit` <: RadixConvertible {
    static func parse(value: std.core.String, radix: Int64): Int32 { /* compiled code */ }

    static func tryParse(value: std.core.String, radix: Int64): std.core.Option<Int32> { /* compiled code */ }

    func toString(radix: Int64): std.core.String { /* compiled code */ }
}

extend `Unit` <: Parsable {
    static func parse(data: std.core.String): UInt32 { /* compiled code */ }

    static func tryParse(data: std.core.String): std.core.Option<UInt32> { /* compiled code */ }
}

extend `Unit` <: RadixConvertible {
    static func parse(value: std.core.String, radix: Int64): UInt32 { /* compiled code */ }

    static func tryParse(value: std.core.String, radix: Int64): std.core.Option<UInt32> { /* compiled code */ }

    func toString(radix: Int64): std.core.String { /* compiled code */ }
}

extend `Unit` <: Parsable {
    static func parse(data: std.core.String): Int64 { /* compiled code */ }

    static func tryParse(data: std.core.String): std.core.Option<Int64> { /* compiled code */ }
}

extend `Unit` <: RadixConvertible {
    static func parse(value: std.core.String, radix: Int64): Int64 { /* compiled code */ }

    static func tryParse(value: std.core.String, radix: Int64): std.core.Option<Int64> { /* compiled code */ }

    func toString(radix: Int64): std.core.String { /* compiled code */ }
}

extend `Unit` <: Parsable {
    static func parse(data: std.core.String): UInt64 { /* compiled code */ }

    static func tryParse(data: std.core.String): std.core.Option<UInt64> { /* compiled code */ }
}

extend `Unit` <: RadixConvertible {
    static func parse(value: std.core.String, radix: Int64): UInt64 { /* compiled code */ }

    static func tryParse(value: std.core.String, radix: Int64): std.core.Option<UInt64> { /* compiled code */ }

    func toString(radix: Int64): std.core.String { /* compiled code */ }
}

interface FloatParsable {
    abstract static func fromFloat64(n: Float64): T
}

extend `Unit` <: Parsable {
    static func parse(data: std.core.String): Float16 { /* compiled code */ }

    static func tryParse(data: std.core.String): std.core.Option<Float16> { /* compiled code */ }
}

extend `Unit` <: Parsable {
    static func parse(data: std.core.String): Float32 { /* compiled code */ }

    static func tryParse(data: std.core.String): std.core.Option<Float32> { /* compiled code */ }
}

extend `Unit` <: Parsable {
    static func parse(data: std.core.String): Float64 { /* compiled code */ }

    static func tryParse(data: std.core.String): std.core.Option<Float64> { /* compiled code */ }
}

extend `Unit` <: Parsable {
    static func parse(data: std.core.String): Rune { /* compiled code */ }

    static func tryParse(data: std.core.String): std.core.Option<Rune> { /* compiled code */ }
}

extend `Unit` <: Parsable {
    static func parse(data: std.core.String): Bool { /* compiled code */ }

    static func tryParse(data: std.core.String): std.core.Option<Bool> { /* compiled code */ }
}
