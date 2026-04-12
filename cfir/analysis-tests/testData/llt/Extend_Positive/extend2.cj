// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I {
    func add1(): Unit
}

interface I2 {
    func add2(): Unit
}

struct A {}

extend A <: I & I2 {
    public func add1() {
        print("1")
    }
    public func add2() {
        print("2")
    }
    public func add3() {
        print("3")
    }
}

main() {
    let a: A = A()
    a.add1()
    a.add2()
    a.add3()
    0
}
