// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A {
    private func foo(): Int64 {
        return 1
    }
}

extend A {
    public func foo(): Int64 {
        return 2
    }
}

main(): Int64 {
    let a = A()
    return a.foo() - 2
}
