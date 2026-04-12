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
    var t1 = S0()

    mut func f() {
        t1.t0 += 1
    }
}

struct S2 {
    var t2 = S1()
}

main() {
    let s2 = S2()
    s2.t2.f()
    println(s2.t2.t1.t0)
}

/* SCAN
error: cannot use mutable function on immutable value
  ==> mut_function_07.cj:27:5:
   | 
27 |     s2.t2.f()
   |     ^^    ~ is a mutable function
   |     |
   |     is immutable
   | 
note: 's2' is a variable declared with 'let'
  ==> mut_function_07.cj:26:9:
   | 
26 |     let s2 = S2()
   |         ^^ 
   | 

1 error generated, 1 error printed.
*/
