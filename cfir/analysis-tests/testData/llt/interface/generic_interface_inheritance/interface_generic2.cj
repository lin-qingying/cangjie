// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I<T> {
    func foo(a: T): T
}

class C<T> <: I<T> & I<Int32> & I<Float64> {
    public func foo(a: Int32): Int32 {
        return a
    }
    public func foo(a: T): T {
        return a
    }
    public func foo(a: Float64): Float64 {
        return a
    }
}

main() {
    var c = C<Bool>()
    if (c.foo(1) != 1 || !c.foo(true)) {
        return 1
    }
    return 0
}
