// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: p1.cj, p2.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt p1.cj -o p1.%middle
// EXEC: %compiler %cmp_opt %compile_lib_opt p2.cj -o p2.%middle
// ERRCHECK: %compiler %cmp_opt p1.%middle p2.%middle %f -o %output

import p2.B

class C <: B {
    func foo2() {
        foo()
    }
    func foo() {
        2
    }
}

main() {
    0
}

/* SCAN
error: ambiguous match for function call 'foo'
  ==> main_err.cj:16:9:
   | 
16 |         foo()
   |         ^ 
   | 
note: found extended function member of 'Class-A'
  ==> (package p1)p1.cj:12:20:
note: found candidate
  ==> main_err.cj:18:10:
   | 
18 |     func foo() {
   |          ^ 
   | 

error: cannot override function 'foo' in extend of supertype
  ==> main_err.cj:18:10:
   | 
18 |     func foo() {
   |          ^^^ 
   | 
note: overridden definition of 'foo' is here
  ==> (package p1)p1.cj:12:20:

2 errors generated, 2 errors printed.
*/
