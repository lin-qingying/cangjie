// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface Eq<T> {
    func add(a: T): Unit
}

class C {}

extend C <: Eq<C> {
    public func add(a: C): Unit {
    }
}

extend Int32 <: Eq<Int32> {
    public func add(a: Int32): Unit {
        print("1234")
    }
}

main() {
    1.add(1)
    0
}
