// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg1 pkg2
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg1/pkg1.cj -o pkg1.o
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg2/pkg2.cj -o pkg2.o
// EXEC: %compiler %cmp_opt pkg1.o pkg2.o %n.cj -o %n.%suffix 2>&1 | compare %f

import pkg1.*
import pkg2.*

interface I1 {
    func foo(): Int64
}

extend<T> A<T> <: I1 {
    public func foo() {
        1
    }
}

main() {
    return A<Int64>().foo() - 1
}

/* SCAN
error: ambiguous match for function call 'foo'
  ==> main.cj:26:23:
   | 
26 |     return A<Int64>().foo() - 1
   |                       ^ 
   | 
note: found extended function member of 'Class-A<Generics-T>'
  ==> (package pkg2)pkg2.cj:16:17:
note: found candidate
  ==> main.cj:20:17:
   | 
20 |     public func foo() {
   |                 ^ 
   | 

error: extend member 'foo' is not allowed to shadow members of 'extend A'
  ==> main.cj:20:17:
   | 
20 |     public func foo() {
   |                 ^^^ 
   | 

2 errors generated, 2 errors printed.
*/
