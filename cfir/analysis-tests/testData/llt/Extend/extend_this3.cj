// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A {
    public let x: Int64 = 1
}

extend A {
    public func foo1(): A {
        return this
    }
    public func foo2(): Int64 {
        return foo1().x - 1
    }
}

main() {
    return A().foo2()
}
