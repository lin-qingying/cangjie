// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg/pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt %n.cj pkg.%middle -o %n.%suffix 2>&1 | compare %f

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

extend E <: I {
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

/* SCAN
error: interface 'I' is not able to be extended
  ==> main.cj:35:1:
   | 
35 |   extend E <: I {
   |  _^
36 | |     func f() {
   | | ...
40 | | }
   | |_^
   | 
   # note: interface 'I' contains mutable member 'p'

1 error generated, 1 error printed.
*/
