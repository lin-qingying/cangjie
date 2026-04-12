// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I1 {}

interface I2 <: I1 {}

open class C1 <: I2 {
    static var i = 1
}

class C2 <: C1 {}

class A<T, R> {}

extend<T, R> A<T, R> where T <: C1, R <: I2 {
    func f() {
        return T.i
    }
}

extend<Q, W> A<Q, W> where Q <: C2, W <: C1 {
    func ff() {
        return f()
    }
}

main() {
    return A<C2, C1>().f() - 1
}
