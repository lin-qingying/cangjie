// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface Eq<T> {
    func equals(other: T): Bool
}

class A {
    public func equals(other: A): Bool {
        return true;
    }
}

extend A <: Eq<A> {}

main(): Int64 {
    let a: Eq<A> = A();
    if (!a.equals(A())) {
        return 1;
    }
    return 0;
}
