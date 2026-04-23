// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct S0 {
    var t0 = 0
}

struct S1 {
    var t1 = S0()

    mut func f() {
        t1.t0 += 1
    }
}

struct S2 {
    var t2 = S1()
}

func foo(f: () -> S2) {
    f()
}

main() {
    let s2 = [S2()]
    foo {=> S2()}.t2.<!IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION!>f<!>()
    println(s2[0].t2.t1.t0)
}
