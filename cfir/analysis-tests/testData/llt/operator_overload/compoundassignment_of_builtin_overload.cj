// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f

public interface INumber<T> {
    static prop size: Int
    static prop diff: Int
    operator func |(other: T): T
    static func coerce(x: UInt64): T
    static func lift(x: T): UInt64
}

extend UInt8 <: INumber<UInt8> {
    public static prop size: Int {
        get() {
            8
        }
    }
    public static prop diff: Int {
        get() {
            56
        }
    }
    public static func coerce(x: UInt64): UInt8 {
        UInt8(x)
    }
    public static func lift(x: UInt8): UInt64 {
        UInt64(x)
    }
}

extend UInt16 <: INumber<UInt16> {
    public static prop size: Int {
        get() {
            16
        }
    }
    public static prop diff: Int {
        get() {
            48
        }
    }
    public static func coerce(x: UInt64): UInt16 {
        UInt16(x)
    }
    public static func lift(x: UInt16): UInt64 {
        UInt64(x)
    }
}

extend UInt32 <: INumber<UInt32> {
    public static prop size: Int {
        get() {
            32
        }
    }
    public static prop diff: Int {
        get() {
            32
        }
    }
    public static func coerce(x: UInt64): UInt32 {
        UInt32(x)
    }
    public static func lift(x: UInt32): UInt64 {
        UInt64(x)
    }
}

extend UInt64 <: INumber<UInt64> {
    public static prop size: Int {
        get() {
            64
        }
    }
    public static prop diff: Int {
        get() {
            0
        }
    }
    public static func coerce(x: UInt64): UInt64 {
        x
    }
    public static func lift(x: UInt64): UInt64 {
        x
    }
}

public func getT<T, T1>(a: Array<T>, index: Int): ?T1 where T <: INumber<T>, T1 <: INumber<T1> {
    if (T1.size == T.size && a.size > index) {
        return T1.coerce(T.lift(a[index]))
    }
    if (T1.size > T.size) {
        let n = T1.size / T.size
        if (a.size > index + n) {
            let d = T1.size - T.size
            var x = T1.coerce(T.lift(a[index]) << d)
            for (i in 1..n) {
                x |= T1.coerce(T.lift(a[index + i]) << d - i * T.size)
            }
            return x
        }
    }
    return None
}

main() {
    let a: Array<Byte> = [1, 2, 3, 4, 5]
    getT<UInt8, UInt16>(a, 0)
    println("getT<UInt8, UInt16>(a, 0): ${getT<UInt8, UInt16>(a, 0)}")
    return 0
}
