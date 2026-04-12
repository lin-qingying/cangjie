// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./testa.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt testa.cj -o a.%middle
// EXEC-PIPE-1: %compiler %cmp_opt a.%middle %f -o %output 2>&1 | compare %f

package a.c

import a.b.*

interface I {
    func foo(): Unit
}

extend A <: I {}

extend A {
    public func foo(): Unit {}
}

main() {}

/* SCAN
error: exported extension cannot indirectly export the functions 'foo' of the non-exported extension
  ==> testb.cj:19:1:
   | 
19 | extend A <: I {}
   | ^^^^^^^^^^^^^^^^ 
   | 
note: following extension cannot be exported
  ==> testb.cj:21:1:
   | 
21 |   extend A {
   |  _^
22 | |     public func foo(): Unit {}
23 | | }
   | |_^
   | 

1 error generated, 1 error printed.
*/
