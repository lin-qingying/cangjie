// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class A {}

class B <: A {}

class C {
    func foo(a: B) {
        1
    }
    func goo(a: A) {
        1
    }
}

extend C {
    func foo(a: A) {
        2
    }
    func goo(a: B) {
        2
    }
}

main() {
    let c = C()
    if (c.foo(A()) != 2) {
        return 1
    }
    if (c.goo(A()) != 1) {
        return 2
    }
    if (c.foo(B()) != 1) {
        return 3
    }
    if (c.goo(B()) != 2) {
        return 4
    }
    return 0
}
