// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.binary

import std.core.*

interface BigEndianOrder {
    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): T { /* compiled code */ }
}

interface LittleEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readLittleEndian(buffer: std.core.Array<UInt8>): T { /* compiled code */ }
}

interface SwapEndianOrder {
    func swapBytes(): T { /* compiled code */ }
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): UInt64 { /* compiled code */ }

    static func readLittleEndian(buffer: std.core.Array<UInt8>): UInt64 { /* compiled code */ }

    func swapBytes(): UInt64 { /* compiled code */ }
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func swapBytes(): Int64 { /* compiled code */ }
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): UInt32 { /* compiled code */ }

    static func readLittleEndian(buffer: std.core.Array<UInt8>): UInt32 { /* compiled code */ }

    func swapBytes(): UInt32 { /* compiled code */ }
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): Int32 { /* compiled code */ }

    static func readLittleEndian(buffer: std.core.Array<UInt8>): Int32 { /* compiled code */ }

    func swapBytes(): Int32 { /* compiled code */ }
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): UInt16 { /* compiled code */ }

    static func readLittleEndian(buffer: std.core.Array<UInt8>): UInt16 { /* compiled code */ }

    func swapBytes(): UInt16 { /* compiled code */ }
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): Int16 { /* compiled code */ }

    static func readLittleEndian(buffer: std.core.Array<UInt8>): Int16 { /* compiled code */ }

    func swapBytes(): Int16 { /* compiled code */ }
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): UInt8 { /* compiled code */ }

    static func readLittleEndian(buffer: std.core.Array<UInt8>): UInt8 { /* compiled code */ }

    func swapBytes(): UInt8 { /* compiled code */ }
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): Int8 { /* compiled code */ }

    static func readLittleEndian(buffer: std.core.Array<UInt8>): Int8 { /* compiled code */ }

    func swapBytes(): Int8 { /* compiled code */ }
}

extend Extend <: BigEndianOrder & LittleEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): Float64 { /* compiled code */ }

    static func readLittleEndian(buffer: std.core.Array<UInt8>): Float64 { /* compiled code */ }
}

extend Extend <: BigEndianOrder & LittleEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): Float32 { /* compiled code */ }

    static func readLittleEndian(buffer: std.core.Array<UInt8>): Float32 { /* compiled code */ }
}

extend Extend <: BigEndianOrder & LittleEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): Float16 { /* compiled code */ }

    static func readLittleEndian(buffer: std.core.Array<UInt8>): Float16 { /* compiled code */ }
}

extend Extend <: BigEndianOrder & LittleEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64 { /* compiled code */ }

    static func readBigEndian(buffer: std.core.Array<UInt8>): Bool { /* compiled code */ }

    static func readLittleEndian(buffer: std.core.Array<UInt8>): Bool { /* compiled code */ }
}

foreign func CJ_Bswap64(n: UInt64): UInt64

foreign func CJ_Bswap32(n: UInt32): UInt32

foreign func CJ_Bswap16(n: UInt16): UInt16
