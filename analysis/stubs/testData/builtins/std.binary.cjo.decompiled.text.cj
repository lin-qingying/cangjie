// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.binary

import std.core.*

interface BigEndianOrder {
    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): T
}

interface LittleEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64

    func readLittleEndian(buffer: std.core.Array<UInt8>): T
}

interface SwapEndianOrder {
    func swapBytes: T
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): UInt64

    func readLittleEndian(buffer: std.core.Array<UInt8>): UInt64

    func swapBytes: UInt64
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readLittleEndian(buffer: std.core.Array<UInt8>): Int64

    func swapBytes: Int64
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): UInt32

    func readLittleEndian(buffer: std.core.Array<UInt8>): UInt32

    func swapBytes: UInt32
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): Int32

    func readLittleEndian(buffer: std.core.Array<UInt8>): Int32

    func swapBytes: Int32
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): UInt16

    func readLittleEndian(buffer: std.core.Array<UInt8>): UInt16

    func swapBytes: UInt16
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): Int16

    func readLittleEndian(buffer: std.core.Array<UInt8>): Int16

    func swapBytes: Int16
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): UInt8

    func readLittleEndian(buffer: std.core.Array<UInt8>): UInt8

    func swapBytes: UInt8
}

extend Extend <: BigEndianOrder & LittleEndianOrder & SwapEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): Int8

    func readLittleEndian(buffer: std.core.Array<UInt8>): Int8

    func swapBytes: Int8
}

extend Extend <: BigEndianOrder & LittleEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): Float64

    func readLittleEndian(buffer: std.core.Array<UInt8>): Float64
}

extend Extend <: BigEndianOrder & LittleEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): Float32

    func readLittleEndian(buffer: std.core.Array<UInt8>): Float32
}

extend Extend <: BigEndianOrder & LittleEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): Float16

    func readLittleEndian(buffer: std.core.Array<UInt8>): Float16
}

extend Extend <: BigEndianOrder & LittleEndianOrder {
    func writeLittleEndian(buffer: std.core.Array<UInt8>): Int64

    func writeBigEndian(buffer: std.core.Array<UInt8>): Int64

    func readBigEndian(buffer: std.core.Array<UInt8>): Bool

    func readLittleEndian(buffer: std.core.Array<UInt8>): Bool
}

func CJ_Bswap64(n: UInt64): UInt64

func CJ_Bswap32(n: UInt32): UInt32

func CJ_Bswap16(n: UInt16): UInt16
