// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct S0 {
    var t0 = 0
}

struct S1 {
    mut prop t1: S0 {
        get() {
            S0()
        }
        set(value) {}
    }

    mut func f() {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>t1.t0<!> += 1
    }
}

struct S2 {
    var t2 = S1()
}

class C {
    var s2 = S2()
}

main() {
    let a = [S2()]
    a[0].t2.f()
    let c = C()
    c.s2.t2.f()
}
