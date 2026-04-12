// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func composition2<T1, T2, T3>(f: (T1) -> T2, g: (T2) -> T3): (T1) -> T3 {
    return {x: T1 => g(f(x))}
}

class A {
    public func foo(input: Int64): Int32 {
        Int32(input)
    }
    public func foo(input: Int32): Int16 {
        Int16(input)
    }
}

main(): Int64 {
    var a = A()
    composition2(a.foo, a.foo)
    return 0
}
