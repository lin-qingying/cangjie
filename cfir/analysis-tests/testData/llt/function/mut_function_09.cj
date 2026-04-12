// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

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
        t1.t0 += 1
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

/* SCAN
error: cannot use mutable function on immutable value
  ==> mut_function_09.cj:36:5:
   | 
36 |     a[0].t2.f()
   |     ^^^^    ~ is a mutable function
   |     |
   |     is immutable
   | 

error: cannot assign to immutable value
  ==> mut_function_09.cj:22:9:
   | 
22 |         t1.t0 += 1
   |         ^^^^^^^^^^ 
   | 
note: property 't1' is immutable
  ==> mut_function_09.cj:14:14:
   | 
14 |     mut prop t1: S0 {
   |              ^^ 
   | 

2 errors generated, 2 errors printed.
*/
