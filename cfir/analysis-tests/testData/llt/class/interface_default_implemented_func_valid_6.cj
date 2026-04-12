// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// (CJNATIVE)EXEC: %run %run_opt %n.%suffix
// (not CJNATIVE)EXEC-1: %run %run_opt %n.%suffix

interface I {
    static func foo(x: Int32): Int32 {
        x
    }
    func coo() {
        foo(2)
    }
}

open class A <: I {
    public static redef func foo(x: Int32): Int32 {
        return 111
    }
}

open class B <: A {
    public static redef func foo(x: Int32): Int32 {
        return 123
    }
}

class C <: B & I {
    public static redef func foo(x: Int32): Int32 {
        return 234
    }
}

main() {
    var a = A()
    var b = B()
    var c = C()
    if (a.coo() != 111 || b.coo() != 123 || c.coo() != 234) {
        return 1
    }
    return 0
}
