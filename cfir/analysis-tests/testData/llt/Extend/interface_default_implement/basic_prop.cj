// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

var g = 0

interface I {
    mut prop p: Int64 {
        get() {
            return 2
        }
        set(v) {
            g++
        } // side effect.
    }
}

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

extend Int32 <: I {
    func f() {
        p = 0
        p
    }
}

main(): Int64 {
    var r = R()
    if (r.f() != 2 || g != 1 || r.p != 2) {
        return 1
    }
    var c = C()
    if (c.f() != 2 || g != 2 || c.p != 2) {
        return 2
    }
    var e = E.a
    if (e.f() != 2 || g != 3 || e.p != 2) {
        return 3
    }
    if (1.f() != 2 || g != 4 || 1.p != 2) {
        return 4
    }
    return 0
}

/* SCAN
error: interface 'I' is not able to be extended
  ==> basic_prop.cj:44:1:
   | 
44 |   extend E <: I {
   |  _^
45 | |     func f() {
   | | ...
49 | | }
   | |_^
   | 
   # note: interface 'I' contains mutable member 'p'

error: interface 'I' is not able to be extended
  ==> basic_prop.cj:51:1:
   | 
51 |   extend Int32 <: I {
   |  _^
52 | |     func f() {
   | | ...
56 | | }
   | |_^
   | 
   # note: interface 'I' contains mutable member 'p'

2 errors generated, 2 errors printed.
*/
