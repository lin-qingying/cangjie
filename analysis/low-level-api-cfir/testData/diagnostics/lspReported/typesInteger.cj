/*
 * Copyright (C) 2024 Yesokim
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
protected package mariadb.protocol

import std.io.*

extend ByteBuffer {

    public func readInt1(): UInt8 {
        return UInt8.fromLittleEndian(readArray(1))
    }

    public func readInt2(): UInt16 {
        return UInt16.fromLittleEndian(readArray(2))
    }

    // public func readInt3(): UInt32 {
    //     return UInt32.fromLittleEndian(readArray(3))
    // }

    public func readInt4(): UInt32 {
        return UInt32.fromLittleEndian(readArray(4))
    }

    // public func readInt6(): UInt64 {
    //     return UInt64.fromLittleEndian(readArray(6))
    // }

    // public func readInt8(): UInt64 {
    //     return UInt64.fromLittleEndian(readArray(8))
    // }

    public func readLengthEncodedInteger(): ?UInt64 {
        match (readLengthEncodedIntegerRaw()) {
            case None => return None
            case Some(v) => UInt64.fromLittleEndian(v)
        }
    }

    public func readLengthEncodedIntegerRaw(): ?Array<Byte> {
        let flag = readInt1()
        match (flag) {
            case 0xFB => return None
            case 0xFC =>
                return readArray(2)
            case 0xFD =>
                return readArray(3)
            case 0xFE =>
                return readArray(8)
            case 0xFF => throw InvalidPacketException("This packet is Error packet")
            case _ => return [flag]
        }
    }

}

extend ByteBuffer {

    protected func writeInt1(value: UInt8): Unit {
        write(value)
    }

    protected func writeInt2(value: UInt16): Unit {
        write(value.toLittleEndian())
    }

    protected func writeInt3(value: UInt32): Unit {
        write(value.toLittleEndian().slice(0, 3))
    }

    protected func writeInt4(value: UInt32): Unit {
        write(value.toLittleEndian())
    }

    // protected func writeInt6(value: UInt64): Unit {
    //     write(value.toLittleEndian().slice(0, 6))
    // }

    protected func writeInt8(value: UInt64): Unit {
        write(value.toLittleEndian())
    }

    protected func writeLengthEncoded(value: UInt64): Unit {
        if (value < 0xFB) {
            writeInt1(UInt8(value))
        } else if (value < UInt64(1 << 16)) {
            writeInt1(0xFC)
            writeInt2(UInt16(value))
        } else if (value < UInt64(1 << 24)) {
            writeInt1(0xFD)
            writeInt3(UInt32(value))
        } else {
            writeInt1(0xFE)
            writeInt8(value)
        }
    }

}
