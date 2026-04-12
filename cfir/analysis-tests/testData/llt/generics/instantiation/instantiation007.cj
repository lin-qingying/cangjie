// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface Number<T> {
    operator func +(that: T): T
}

class A<T1, T2, T3> where T1 <: Number<T2>, T2 <: Number<T3> {}

interface I<T1, T2, T3> where T1 <: Number<T2>, T2 <: Number<T3> {}

extend Int64 <: Number<Int32> {
    public operator func +(that: Int32): Int32 {
        Int32(this) + that
    }
}

extend Int64 <: Number<Int16> {
    public operator func +(that: Int16): Int16 {
        Int16(this) + that
    }
}

extend Int16 <: Number<Int32> {
    public operator func +(that: Int32): Int32 {
        Int32(this) + that
    }
}

extend Int16 <: Number<Int64> {
    public operator func +(that: Int64): Int64 {
        Int64(this) + that
    }
}

extend Int32 <: Number<Int16> {
    public operator func +(that: Int16): Int16 {
        Int16(this) + that
    }
}

extend Int32 <: Number<Int64> {
    public operator func +(that: Int64): Int64 {
        Int64(this) + that
    }
}

extend<T1, T3, T2> A<T1, T3, T2> <: I<T1, T3, T2> where T1 <: Number<T2>, T2 <: Number<T3> {
    func foo(a: T1, b: T3, c: T2) {
        let v = a + c + b
        return a + b + c + v
    }
}

main() {
    return A<Int16, Int32, Int64>().foo(1, 2, 3) - 12
}
