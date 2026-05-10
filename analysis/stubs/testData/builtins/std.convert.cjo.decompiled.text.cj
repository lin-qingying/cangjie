// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.convert

import std.core.*

func parseFloat(data: std.core.String): T

interface Formattable {
    func format(fmt: std.core.String): std.core.String
}

enum Flags {
    | NoFlag

    | Minus

    | Plus

    | Sharp

    | Zero

    operator func ==(right: std.convert.Flags): Bool

    operator func !=(right: std.convert.Flags): Bool
}

extend Extend <: Formattable {
    func format(fmt: std.core.String): std.core.String
}

extend Extend <: Formattable {
    func format(fmt: std.core.String): std.core.String
}

extend Extend <: Formattable {
    func format(fmt: std.core.String): std.core.String
}

extend Extend <: Formattable {
    func format(fmt: std.core.String): std.core.String
}

extend Extend <: Formattable {
    func format(fmt: std.core.String): std.core.String
}

extend Extend <: Formattable {
    func format(fmt: std.core.String): std.core.String
}

extend Extend <: Formattable {
    func format(fmt: std.core.String): std.core.String
}

extend Extend <: Formattable {
    func format(fmt: std.core.String): std.core.String
}

extend Extend <: Formattable {
    func format(fmt: std.core.String): std.core.String
}

extend Extend <: Formattable {
    func format(fmt: std.core.String): std.core.String
}

extend Extend <: Formattable {
    func format(fmt: std.core.String): std.core.String
}

extend Extend <: Formattable {
    func format(fmt: std.core.String): std.core.String
}

class Verify {
    var data: std.core.Array<Rune>

    var size: Int64

    var index: Int64

    init(fmt: std.core.String) { /* compiled code */ }

    func verifyIntFormatArg: Bool

    func verifyFloatFormatArg: Bool
}

func parseFormat(fmt: std.core.String): (std.convert.Flags, Int64, Int64, Rune)

func escapeControlCharacters(str: std.core.String): std.core.String

interface Parsable {
    func parse(value: std.core.String): T

    func tryParse(value: std.core.String): std.core.Option<T>
}

interface RadixConvertible {
    func parse(value: std.core.String, radix: Int64): T

    func tryParse(value: std.core.String, radix: Int64): std.core.Option<T>

    func toString(radix: Int64): std.core.String
}

extend Extend <: Parsable {
    func parse(data: std.core.String): Int8

    func tryParse(data: std.core.String): std.core.Option<Int8>
}

extend Extend <: RadixConvertible {
    func parse(value: std.core.String, radix: Int64): Int8

    func tryParse(value: std.core.String, radix: Int64): std.core.Option<Int8>

    func toString(radix: Int64): std.core.String
}

extend Extend <: Parsable {
    func parse(data: std.core.String): UInt8

    func tryParse(data: std.core.String): std.core.Option<UInt8>
}

extend Extend <: RadixConvertible {
    func parse(value: std.core.String, radix: Int64): UInt8

    func tryParse(value: std.core.String, radix: Int64): std.core.Option<UInt8>

    func toString(radix: Int64): std.core.String
}

extend Extend <: Parsable {
    func parse(data: std.core.String): Int16

    func tryParse(data: std.core.String): std.core.Option<Int16>
}

extend Extend <: RadixConvertible {
    func parse(value: std.core.String, radix: Int64): Int16

    func tryParse(value: std.core.String, radix: Int64): std.core.Option<Int16>

    func toString(radix: Int64): std.core.String
}

extend Extend <: Parsable {
    func parse(data: std.core.String): UInt16

    func tryParse(data: std.core.String): std.core.Option<UInt16>
}

extend Extend <: RadixConvertible {
    func parse(value: std.core.String, radix: Int64): UInt16

    func tryParse(value: std.core.String, radix: Int64): std.core.Option<UInt16>

    func toString(radix: Int64): std.core.String
}

extend Extend <: Parsable {
    func parse(data: std.core.String): Int32

    func tryParse(data: std.core.String): std.core.Option<Int32>
}

extend Extend <: RadixConvertible {
    func parse(value: std.core.String, radix: Int64): Int32

    func tryParse(value: std.core.String, radix: Int64): std.core.Option<Int32>

    func toString(radix: Int64): std.core.String
}

extend Extend <: Parsable {
    func parse(data: std.core.String): UInt32

    func tryParse(data: std.core.String): std.core.Option<UInt32>
}

extend Extend <: RadixConvertible {
    func parse(value: std.core.String, radix: Int64): UInt32

    func tryParse(value: std.core.String, radix: Int64): std.core.Option<UInt32>

    func toString(radix: Int64): std.core.String
}

extend Extend <: Parsable {
    func parse(data: std.core.String): Int64

    func tryParse(data: std.core.String): std.core.Option<Int64>
}

extend Extend <: RadixConvertible {
    func parse(value: std.core.String, radix: Int64): Int64

    func tryParse(value: std.core.String, radix: Int64): std.core.Option<Int64>

    func toString(radix: Int64): std.core.String
}

extend Extend <: Parsable {
    func parse(data: std.core.String): UInt64

    func tryParse(data: std.core.String): std.core.Option<UInt64>
}

extend Extend <: RadixConvertible {
    func parse(value: std.core.String, radix: Int64): UInt64

    func tryParse(value: std.core.String, radix: Int64): std.core.Option<UInt64>

    func toString(radix: Int64): std.core.String
}

interface FloatParsable {
    func fromFloat64(n: Float64): T
}

extend Extend <: Parsable {
    func parse(data: std.core.String): Float16

    func tryParse(data: std.core.String): std.core.Option<Float16>
}

extend Extend <: Parsable {
    func parse(data: std.core.String): Float32

    func tryParse(data: std.core.String): std.core.Option<Float32>
}

extend Extend <: Parsable {
    func parse(data: std.core.String): Float64

    func tryParse(data: std.core.String): std.core.Option<Float64>
}

extend Extend <: Parsable {
    func parse(data: std.core.String): Rune

    func tryParse(data: std.core.String): std.core.Option<Rune>
}

extend Extend <: Parsable {
    func parse(data: std.core.String): Bool

    func tryParse(data: std.core.String): std.core.Option<Bool>
}
