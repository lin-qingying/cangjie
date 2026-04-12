// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC-1: %compiler %cmp_opt %f -o %output
// (not CJNATIVE) EXEC: %compiler %cmp_opt %f -o %output

public interface Addable<T> where T <: Addable<T> {
    operator func +(right: T): T
}

public interface Number<T> <: Addable<T> where T <: Number<T> {
    static prop zero: T
    operator func -(right: T): T
    operator func *(right: T): T
    operator func /(right: T): T
    operator func >(right: T): Bool
    operator func >=(right: T): Bool
    operator func <(right: T): Bool
    operator func <=(right: T): Bool
    operator func ==(right: T): Bool
    operator func !=(right: T): Bool
}

public interface Integer<T> <: Number<T> where T <: Integer<T> {
    operator func %(right: T): T
    operator func &(right: T): T
    operator func |(right: T): T
    operator func ^(right: T): T
    operator func >>(right: T): T
    operator func <<(right: T): T
    operator func !(): T
}

public interface Float<T> <: Number<T> where T <: Float<T> {}

extend Int8 <: Integer<Int8> {
    public static prop zero: Int8 {
        get() {
            0
        }
    }
}

extend Int16 <: Integer<Int16> {
    public static prop zero: Int16 {
        get() {
            0
        }
    }
}

extend Int32 <: Integer<Int32> {
    public static prop zero: Int32 {
        get() {
            0
        }
    }
}

extend Int64 <: Integer<Int64> {
    public static prop zero: Int64 {
        get() {
            0
        }
    }
}

extend UInt8 <: Integer<UInt8> {
    public static prop zero: UInt8 {
        get() {
            0
        }
    }
}

extend UInt16 <: Integer<UInt16> {
    public static prop zero: UInt16 {
        get() {
            0
        }
    }
}

extend UInt32 <: Integer<UInt32> {
    public static prop zero: UInt32 {
        get() {
            0
        }
    }
}

extend UInt64 <: Integer<UInt64> {
    public static prop zero: UInt64 {
        get() {
            0
        }
    }
}

extend Float16 <: Float<Float16> {
    public static prop zero: Float16 {
        get() {
            0.0
        }
    }
}

extend Float32 <: Float<Float32> {
    public static prop zero: Float32 {
        get() {
            0.0
        }
    }
}

extend Float64 <: Float<Float64> {
    public static prop zero: Float64 {
        get() {
            0.0
        }
    }
}

public interface Powerable<T, P> {
    operator func **(p: P): T
}

extend Int64 <: Powerable<Int64, UInt64> {}

extend Float64 <: Powerable<Float64, Int64> {}

extend Float64 <: Powerable<Float64, Float64> {}

func test<T, K>(a: T, b: K): T where T <: Powerable<T, K> {
    return a ** b
}

main() {
    let v1 = test<Int64, UInt64>(2, 3)
    let v2 = test<Float64, Float64>(2.0, 3.0)
    let v3 = test<Float64, Int64>(2.0, 3)
    if (v1 != 8 || v2 != v3) {
        return 1
    }
    return 0
}

/* SCAN-IN
error: static member cannot depend on generic parameter 'Generics-T'
 ==> extend_overload_builtin_op6.cj:8:23:
  |
8 |     static prop zero: T
  |                       ^
  |

1 error generated, 1 error printed.
 */
