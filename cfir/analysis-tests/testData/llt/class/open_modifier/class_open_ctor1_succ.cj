// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I {
    func coo() {
        1
    }
}

open class A <: I {
    protected func foo() {
        2
    }
}

extend A {
    func goo() {
        3
    }
}

class B <: A {
    var a = 0
    public init() {
        let v1 = foo() + goo() + coo()
        let f1 = this.coo
        let f2 = this.foo
        let v2 = f1() + f2()
        a = v1 + v2
    }
}

main(): Int64 {
    let b = B()
    if (b.a != 9) {
        return 1
    }
    return 0
}
