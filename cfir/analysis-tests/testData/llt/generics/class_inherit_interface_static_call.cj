// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface Number<T> {}

class C <: Number<Int32> {}

class A<T1, T2> <: I<T1, T2> where T1 <: Number<T2> {
    static func foo() {
        1
    }
}

interface I<T1, T2> where T1 <: Number<T2> {}

main() {
    return A<C, Int32>.foo() - 1
}
