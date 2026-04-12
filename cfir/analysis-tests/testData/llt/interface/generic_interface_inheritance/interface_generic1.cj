// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I<T> {
    func foo(a: T): Int32
}

interface II<T> <: I<T> & I<Int32> & I<Float64> {
    func foo(a: Int32): Int32
    func foo(a: T): Int32
    func foo(a: Float64): Int32
}

main() {
    return 0
}
