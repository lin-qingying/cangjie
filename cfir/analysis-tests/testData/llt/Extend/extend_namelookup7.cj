// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class A {
    public A(var a!: Int64 = 5) {}
}

extend A {
    public func me() {
        return ppp()
    }
    public func ppp() {
        return this.a
    }
}

main(): Int64 {
    return A().me() - 5
}
