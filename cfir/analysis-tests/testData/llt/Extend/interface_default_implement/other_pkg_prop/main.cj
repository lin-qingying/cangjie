// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
internal import pkg.*

struct R {}

extend R <: I {
    mut func f() {
        p = 0
        p
    }
}

class C {}

extend C <: I {
    func f() {
        p = 0
        p
    }
}

enum E {
    a
}

<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend E <: I {<!>
    func f() {
        p = 0
        p
    }
}

main(): Int64 {
    var r = R()
    if (r.f() != 1 || r.p != 1) {
        return 1
    }
    var c = C()
    if (c.f() != 2 || c.p != 2) {
        return 2
    }
    var e = E.a
    if (e.f() != 3 || e.p != 3) {
        return 3
    }
    return 0
}
