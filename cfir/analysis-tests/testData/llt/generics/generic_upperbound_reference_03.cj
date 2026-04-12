// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I<T> {
    func test(a: T): Int64 {
        return 0
    }
}

interface I2<T, V> <: I<T> & I<V> {}

class A <: I2<(Int64, Float64), Float64> {}

extend A <: I2<(Float64, Int64), Int64> {
    public func test(a: (Float64, Int64)): Int64 {
        return 1
    }
    public func test(a: Int64): Int64 {
        return 1
    }
}

extend A <: I2<A, I<Int64>> {
    public func test(a: A): Int64 {
        return 2
    }
    public func test(a: I<Int64>): Int64 {
        return 2
    }
}

class B <: I<Int64> {}

main() {
    var a = A()
    if (a.test(1.0) != 0) {
        return 1
    }
    var v = a.test(1)
    if (v != 1) {
        return 2
    }
    v = a.test((1, 1.0))
    if (v != 0 || a.test((1.0, 1)) != 1) {
        return 3
    }
    v = a.test(A())
    if (v != 2) {
        return 4
    }
    if (a.test(B()) != 2) {
        return 5
    }
    return 0
}
