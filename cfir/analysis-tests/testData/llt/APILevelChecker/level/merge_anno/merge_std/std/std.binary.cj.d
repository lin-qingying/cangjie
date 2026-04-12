// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package std.binary


/**
* big-endian order interface
*
*/
@!APILevel[since: "12", atomicservice : true]
public interface BigEndianOrder<T> {
}

/**
* little-endian order interface
*
*/
@!APILevel[since: "12", atomicservice : true]
public interface LittleEndianOrder<T> {
}

/**
* @brief swap endian order interface
*
*/
@!APILevel[since: "12", atomicservice : true]
public interface SwapEndianOrder<T> {
}

extend UInt64 <: BigEndianOrder<UInt64> & LittleEndianOrder<UInt64> & SwapEndianOrder<UInt64> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeLittleEndian(buffer: Array<UInt8>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeBigEndian(buffer: Array<UInt8>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func readBigEndian(buffer: Array<UInt8>): UInt64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func readLittleEndian(buffer: Array<UInt8>): UInt64
    
    @!APILevel[since: "12", atomicservice : true]
    public func swapBytes(): UInt64
}

extend Int64 <: BigEndianOrder<Int64> & LittleEndianOrder<Int64> & SwapEndianOrder<Int64> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeLittleEndian(buffer: Array<Byte>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeBigEndian(buffer: Array<Byte>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func readBigEndian(buffer: Array<UInt8>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func readLittleEndian(buffer: Array<UInt8>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func swapBytes(): Int64
}

extend UInt32 <: BigEndianOrder<UInt32> & LittleEndianOrder<UInt32> & SwapEndianOrder<UInt32> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeLittleEndian(buffer: Array<Byte>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeBigEndian(buffer: Array<Byte>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func readBigEndian(buffer: Array<UInt8>): UInt32
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func readLittleEndian(buffer: Array<UInt8>): UInt32
    
    @!APILevel[since: "12", atomicservice : true]
    public func swapBytes(): UInt32
}

extend Int32 <: BigEndianOrder<Int32> & LittleEndianOrder<Int32> & SwapEndianOrder<Int32> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeLittleEndian(buffer: Array<Byte>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeBigEndian(buffer: Array<Byte>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func readBigEndian(buffer: Array<UInt8>): Int32
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func readLittleEndian(buffer: Array<UInt8>): Int32
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func swapBytes(): Int32
}

extend UInt16 <: BigEndianOrder<UInt16> & LittleEndianOrder<UInt16> & SwapEndianOrder<UInt16> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeLittleEndian(buffer: Array<Byte>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeBigEndian(buffer: Array<Byte>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func readBigEndian(buffer: Array<UInt8>): UInt16
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func readLittleEndian(buffer: Array<UInt8>): UInt16
    
    @!APILevel[since: "12", atomicservice : true]
    public func swapBytes(): UInt16
}

extend Int16 <: BigEndianOrder<Int16> & LittleEndianOrder<Int16> & SwapEndianOrder<Int16> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeLittleEndian(buffer: Array<Byte>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeBigEndian(buffer: Array<Byte>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func readBigEndian(buffer: Array<UInt8>): Int16
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func readLittleEndian(buffer: Array<UInt8>): Int16
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func swapBytes(): Int16
}

extend UInt8 <: BigEndianOrder<UInt8> & LittleEndianOrder<UInt8> & SwapEndianOrder<UInt8> {
    @!APILevel[since: "12", atomicservice : true]
    public func writeLittleEndian(buffer: Array<Byte>): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public func writeBigEndian(buffer: Array<Byte>): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public static func readBigEndian(buffer: Array<UInt8>): UInt8
    
    @!APILevel[since: "12", atomicservice : true]
    public static func readLittleEndian(buffer: Array<UInt8>): UInt8
    
    @!APILevel[since: "12", atomicservice : true]
    public func swapBytes(): UInt8
}

extend Int8 <: BigEndianOrder<Int8> & LittleEndianOrder<Int8> & SwapEndianOrder<Int8> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeLittleEndian(buffer: Array<Byte>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeBigEndian(buffer: Array<Byte>): Int64
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func readBigEndian(buffer: Array<UInt8>): Int8
    
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public static func readLittleEndian(buffer: Array<UInt8>): Int8
    
    @!APILevel[since: "12", atomicservice : true]
    public func swapBytes(): Int8
}

extend Float64 <: BigEndianOrder<Float64> & LittleEndianOrder<Float64> {
    @!APILevel[since: "12", atomicservice : true]
    public func writeLittleEndian(buffer: Array<Byte>): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public func writeBigEndian(buffer: Array<Byte>): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public static func readBigEndian(buffer: Array<UInt8>): Float64
    
    @!APILevel[since: "12", atomicservice : true]
    public static func readLittleEndian(buffer: Array<UInt8>): Float64
}

extend Float32 <: BigEndianOrder<Float32> & LittleEndianOrder<Float32> {
    @!APILevel[since: "12", atomicservice : true]
    public func writeLittleEndian(buffer: Array<Byte>): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public func writeBigEndian(buffer: Array<Byte>): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public static func readBigEndian(buffer: Array<UInt8>): Float32
    
    @!APILevel[since: "12", atomicservice : true]
    public static func readLittleEndian(buffer: Array<UInt8>): Float32
}

extend Float16 <: BigEndianOrder<Float16> & LittleEndianOrder<Float16> {
    @OverflowWrapping
    @!APILevel[since: "12", atomicservice : true]
    public func writeLittleEndian(buffer: Array<Byte>): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public func writeBigEndian(buffer: Array<Byte>): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public static func readBigEndian(buffer: Array<UInt8>): Float16
    
    @!APILevel[since: "12", atomicservice : true]
    public static func readLittleEndian(buffer: Array<UInt8>): Float16
}

extend Bool <: BigEndianOrder<Bool> & LittleEndianOrder<Bool> {
    @!APILevel[since: "12", atomicservice : true]
    public func writeLittleEndian(buffer: Array<Byte>): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public func writeBigEndian(buffer: Array<Byte>): Int64
    
    @!APILevel[since: "12", atomicservice : true]
    public static func readBigEndian(buffer: Array<UInt8>): Bool
    
    @!APILevel[since: "12", atomicservice : true]
    public static func readLittleEndian(buffer: Array<UInt8>): Bool
}

