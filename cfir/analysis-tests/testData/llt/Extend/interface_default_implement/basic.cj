// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

interface I {
    func f() {
        2
    }
}

struct R {}

extend R <: I {
    func g() {
        f() * 10
    }
}

class C {}

extend C <: I {
    func g() {
        f() * 10
    }
}

enum E {
    a
}

extend E <: I {
    func g() {
        f() * 10
    }
}

main(): Int64 {
    var r = R()
    if (r.f() + r.g() != 22) {
        return 1
    }
    var c = C()
    if (c.f() + c.g() != 22) {
        return 2
    }
    var e = E.a
    if (e.f() + e.g() != 22) {
        return 3
    }
    return 0
}
