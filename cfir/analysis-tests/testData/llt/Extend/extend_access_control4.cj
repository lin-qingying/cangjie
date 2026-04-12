// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class A {
    protected var v2 = 1
    protected func g(): Int64 {
        return 10
    }
}

extend A {
    public func foo(): Int64 {
        return v2 + g()
    }
}

class B <: A {}

extend B {
    public func goo(): Int64 {
        return v2 + g()
    }
}

main(): Int64 {
    let a = A()
    let b = B()
    return a.foo() + b.goo() - 22
}
