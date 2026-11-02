// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.convert

import std.core.*

internal func parseFloat<T>(data: std.core.String): T { /* compiled code */ }

public interface Formattable {
    public open func format(fmt: std.core.String): std.core.String
}

protected enum Flags {
    | NoFlag

    | Minus

    | Plus

    | Sharp

    | Zero

    public operator func ==(right: std.convert.Flags): Bool { /* compiled code */ }

    public operator func !=(right: std.convert.Flags): Bool { /* compiled code */ }
}

extend Int64 <: Formattable {
    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend Int32 <: Formattable {
    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend Int16 <: Formattable {
    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend Int8 <: Formattable {
    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend UInt64 <: Formattable {
    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend UInt32 <: Formattable {
    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend UInt16 <: Formattable {
    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend UInt8 <: Formattable {
    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend Float64 <: Formattable {
    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend Float32 <: Formattable {
    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend Float16 <: Formattable {
    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

extend Rune <: Formattable {
    public func format(fmt: std.core.String): std.core.String { /* compiled code */ }
}

protected class Verify {
    internal var data: std.core.Array<Rune>

    internal var size: Int64

    internal var index: Int64

    init(fmt: std.core.String) { /* compiled code */ }

    public func verifyIntFormatArg(): Bool { /* compiled code */ }

    public func verifyFloatFormatArg(): Bool { /* compiled code */ }
}

protected func parseFormat(fmt: std.core.String): (std.convert.Flags, Int64, Int64, Rune) { /* compiled code */ }

protected func escapeControlCharacters(str: std.core.String): std.core.String { /* compiled code */ }

public interface Parsable<T> {
    public static func parse(value: std.core.String): T

    public static func tryParse(value: std.core.String): std.core.Option<T>
}

public interface RadixConvertible<T> {
    public static func parse(value: std.core.String, radix!: Int64): T

    public static func tryParse(value: std.core.String, radix!: Int64): std.core.Option<T>

    public open func toString(radix!: Int64): std.core.String
}

extend Int8 <: Parsable {
    public static func parse(data: std.core.String): Int8 { /* compiled code */ }

    public static func tryParse(data: std.core.String): std.core.Option<Int8> { /* compiled code */ }
}

extend Int8 <: RadixConvertible {
    public static func parse(value: std.core.String, radix!: Int64): Int8 { /* compiled code */ }

    public static func tryParse(value: std.core.String, radix!: Int64): std.core.Option<Int8> { /* compiled code */ }

    public func toString(radix!: Int64): std.core.String { /* compiled code */ }
}

extend UInt8 <: Parsable {
    public static func parse(data: std.core.String): UInt8 { /* compiled code */ }

    public static func tryParse(data: std.core.String): std.core.Option<UInt8> { /* compiled code */ }
}

extend UInt8 <: RadixConvertible {
    public static func parse(value: std.core.String, radix!: Int64): UInt8 { /* compiled code */ }

    public static func tryParse(value: std.core.String, radix!: Int64): std.core.Option<UInt8> { /* compiled code */ }

    public func toString(radix!: Int64): std.core.String { /* compiled code */ }
}

extend Int16 <: Parsable {
    public static func parse(data: std.core.String): Int16 { /* compiled code */ }

    public static func tryParse(data: std.core.String): std.core.Option<Int16> { /* compiled code */ }
}

extend Int16 <: RadixConvertible {
    public static func parse(value: std.core.String, radix!: Int64): Int16 { /* compiled code */ }

    public static func tryParse(value: std.core.String, radix!: Int64): std.core.Option<Int16> { /* compiled code */ }

    public func toString(radix!: Int64): std.core.String { /* compiled code */ }
}

extend UInt16 <: Parsable {
    public static func parse(data: std.core.String): UInt16 { /* compiled code */ }

    public static func tryParse(data: std.core.String): std.core.Option<UInt16> { /* compiled code */ }
}

extend UInt16 <: RadixConvertible {
    public static func parse(value: std.core.String, radix!: Int64): UInt16 { /* compiled code */ }

    public static func tryParse(value: std.core.String, radix!: Int64): std.core.Option<UInt16> { /* compiled code */ }

    public func toString(radix!: Int64): std.core.String { /* compiled code */ }
}

extend Int32 <: Parsable {
    public static func parse(data: std.core.String): Int32 { /* compiled code */ }

    public static func tryParse(data: std.core.String): std.core.Option<Int32> { /* compiled code */ }
}

extend Int32 <: RadixConvertible {
    public static func parse(value: std.core.String, radix!: Int64): Int32 { /* compiled code */ }

    public static func tryParse(value: std.core.String, radix!: Int64): std.core.Option<Int32> { /* compiled code */ }

    public func toString(radix!: Int64): std.core.String { /* compiled code */ }
}

extend UInt32 <: Parsable {
    public static func parse(data: std.core.String): UInt32 { /* compiled code */ }

    public static func tryParse(data: std.core.String): std.core.Option<UInt32> { /* compiled code */ }
}

extend UInt32 <: RadixConvertible {
    public static func parse(value: std.core.String, radix!: Int64): UInt32 { /* compiled code */ }

    public static func tryParse(value: std.core.String, radix!: Int64): std.core.Option<UInt32> { /* compiled code */ }

    public func toString(radix!: Int64): std.core.String { /* compiled code */ }
}

extend Int64 <: Parsable {
    public static func parse(data: std.core.String): Int64 { /* compiled code */ }

    public static func tryParse(data: std.core.String): std.core.Option<Int64> { /* compiled code */ }
}

extend Int64 <: RadixConvertible {
    public static func parse(value: std.core.String, radix!: Int64): Int64 { /* compiled code */ }

    public static func tryParse(value: std.core.String, radix!: Int64): std.core.Option<Int64> { /* compiled code */ }

    public func toString(radix!: Int64): std.core.String { /* compiled code */ }
}

extend UInt64 <: Parsable {
    public static func parse(data: std.core.String): UInt64 { /* compiled code */ }

    public static func tryParse(data: std.core.String): std.core.Option<UInt64> { /* compiled code */ }
}

extend UInt64 <: RadixConvertible {
    public static func parse(value: std.core.String, radix!: Int64): UInt64 { /* compiled code */ }

    public static func tryParse(value: std.core.String, radix!: Int64): std.core.Option<UInt64> { /* compiled code */ }

    public func toString(radix!: Int64): std.core.String { /* compiled code */ }
}

internal interface FloatParsable<T> {
    public static func fromFloat64(n: Float64): T
}

extend Float16 <: Parsable {
    @Frozen
    public static func parse(data: std.core.String): Float16 { /* compiled code */ }

    public static func tryParse(data: std.core.String): std.core.Option<Float16> { /* compiled code */ }
}

extend Float32 <: Parsable {
    @Frozen
    public static func parse(data: std.core.String): Float32 { /* compiled code */ }

    public static func tryParse(data: std.core.String): std.core.Option<Float32> { /* compiled code */ }
}

extend Float64 <: Parsable {
    @Frozen
    public static func parse(data: std.core.String): Float64 { /* compiled code */ }

    public static func tryParse(data: std.core.String): std.core.Option<Float64> { /* compiled code */ }
}

extend Rune <: Parsable {
    public static func parse(data: std.core.String): Rune { /* compiled code */ }

    public static func tryParse(data: std.core.String): std.core.Option<Rune> { /* compiled code */ }
}

extend Bool <: Parsable {
    public static func parse(data: std.core.String): Bool { /* compiled code */ }

    public static func tryParse(data: std.core.String): std.core.Option<Bool> { /* compiled code */ }
}
