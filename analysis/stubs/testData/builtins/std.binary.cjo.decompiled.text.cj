// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.binary

import std.core.*

public interface BigEndianOrder<T> {
    public open func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    public static func readBigEndian(buffer: std.core.Array<UInt8>): T
}

public interface LittleEndianOrder<T> {
    public open func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64

    public static func readLittleEndian(buffer: std.core.Array<UInt8>): T
}

public interface SwapEndianOrder<T> {
    public open func swapBytes(): T
}

extend UInt64 <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    public func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): UInt64 { /* compiled code */ }

    public static func readLittleEndian(buffer: std.core.Array<UInt8>): UInt64 { /* compiled code */ }

    public func swapBytes(): UInt64 { /* compiled code */ }
}

extend Int64 <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    public func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func swapBytes(): Int64 { /* compiled code */ }
}

extend UInt32 <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    public func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): UInt32 { /* compiled code */ }

    public static func readLittleEndian(buffer: std.core.Array<UInt8>): UInt32 { /* compiled code */ }

    public func swapBytes(): UInt32 { /* compiled code */ }
}

extend Int32 <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    public func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): Int32 { /* compiled code */ }

    public static func readLittleEndian(buffer: std.core.Array<UInt8>): Int32 { /* compiled code */ }

    public func swapBytes(): Int32 { /* compiled code */ }
}

extend UInt16 <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    public func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): UInt16 { /* compiled code */ }

    public static func readLittleEndian(buffer: std.core.Array<UInt8>): UInt16 { /* compiled code */ }

    public func swapBytes(): UInt16 { /* compiled code */ }
}

extend Int16 <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    public func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): Int16 { /* compiled code */ }

    public static func readLittleEndian(buffer: std.core.Array<UInt8>): Int16 { /* compiled code */ }

    public func swapBytes(): Int16 { /* compiled code */ }
}

extend UInt8 <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    public func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): UInt8 { /* compiled code */ }

    public static func readLittleEndian(buffer: std.core.Array<UInt8>): UInt8 { /* compiled code */ }

    public func swapBytes(): UInt8 { /* compiled code */ }
}

extend Int8 <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    public func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): Int8 { /* compiled code */ }

    public static func readLittleEndian(buffer: std.core.Array<UInt8>): Int8 { /* compiled code */ }

    public func swapBytes(): Int8 { /* compiled code */ }
}

extend Float64 <: BigEndianOrder & LittleEndianOrder {
    public func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): Float64 { /* compiled code */ }

    public static func readLittleEndian(buffer: std.core.Array<UInt8>): Float64 { /* compiled code */ }
}

extend Float32 <: BigEndianOrder & LittleEndianOrder {
    public func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): Float32 { /* compiled code */ }

    public static func readLittleEndian(buffer: std.core.Array<UInt8>): Float32 { /* compiled code */ }
}

extend Float16 <: BigEndianOrder & LittleEndianOrder {
    public func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): Float16 { /* compiled code */ }

    public static func readLittleEndian(buffer: std.core.Array<UInt8>): Float16 { /* compiled code */ }
}

extend Bool <: BigEndianOrder & LittleEndianOrder {
    public func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    public static func readBigEndian(buffer: std.core.Array<UInt8>): Bool { /* compiled code */ }

    public static func readLittleEndian(buffer: std.core.Array<UInt8>): Bool { /* compiled code */ }
}

internal foreign func CJ_Bswap64(n: UInt64): UInt64

internal foreign func CJ_Bswap32(n: UInt32): UInt32

internal foreign func CJ_Bswap16(n: UInt16): UInt16
