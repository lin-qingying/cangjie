// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A<T> {}

interface EQ {}

class S <: EQ {}

extend<T> A<T> where T <: EQ {
    public func add(a: T) {}
}

main() {
    let a: A<S> = A<S>()
    let s: S = S()
    a.add(s)
    0
}
