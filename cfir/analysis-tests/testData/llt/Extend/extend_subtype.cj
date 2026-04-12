// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I {
    func foo(): Int64
}

class A {}

extend A <: I {
    public func foo(): Int64 {
        return 0
    }
}

main() {
    let a: I = A()
    return a.foo()
}
