// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I {
    func f(): Int64
}

open class C1 {}

class C2 <: C1 {}

class A<T> {}

extend<T> A<T> where T <: C1 {
    public func f() {
        1
    }
}

extend<T> A<T> <: I where T <: C2 {}

main() {
    return A<C1>().f() + A<C2>().f() - 2
}
