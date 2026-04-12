// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

package p

enum E {
    | f(Int64)
    | f(Int64, Int64, Int64)
}

func f(a: Int64) {}

func f(a: Int64, b: Int64) {}

func g() {
    f(1)
    f(1) // ok
    E.f(1) // ok
    f(1, 2) // ok, find f(a: Int64, b: Int64)
    f(1, 2, 3) // ok, find E.f(Int64, Int64, Int64)
}

var A = 0 // conflict with A
var F: (Int64) -> Int64 = {i => i} // conflict with F

enum E2 {
    | A // conflict with A
    | F(Int64) // conflict with F
}

enum R {
    | C(Int64) // conflict with C
}

enum C { // conflict with C
    | B
}

func g2() {
    let a = A
    let b = E2.A // ok
    let c = A // ok
    F(1)
    E2.F(1) // ok
    F(1) // ok
    let y: C // ok
}

/* SCAN
error: extra argument given for parameter list '(Int64, Int64)' in call
  ==> enum16_2.cj:25:6:
   | 
25 |     f(1, 2, 3) // ok, find E.f(Int64, Int64, Int64)
   |      ^^^^^^^^^ expected 2 arguments, found 3
   | 
note: found candidate
  ==> enum16_2.cj:18:6:
   | 
18 | func f(a: Int64, b: Int64) {}
   |      ^ 
   | 

1 error generated, 1 error printed.
*/
