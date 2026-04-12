// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface Eq<T> {
    func equals(right: T): Bool
}

struct A <: Eq<A> {
    public func equals(right: A): Bool {
        return true
    }
}

func equal<T>(left: T, right: T) where T <: Eq<T> {
    return left.equals(right)
}

main(): Int64 {
    let a = A()
    let b = a
    if (equal<A>(a, b)) {
        return 0
    }
    return 1
}
