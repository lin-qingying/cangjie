// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I {
    func foo(): Unit
}

open class A {
    protected func foo(): Unit {}
}

class B <: A {}

extend B <: I {}

/* SCAN
error: a deriving member must be at least as visible as its base member
  ==> extend_supertype7.cj:19:8:
   | 
19 | extend B <: I {}
   |        ^ 'foo' is inherited
   | 
note: the deriving 'foo' is defined here
  ==> extend_supertype7.cj:14:20:
   | 
14 |     protected func foo(): Unit {}
   |                    ^^^ the visibility of the deriving 'foo' is 'protected'
   | 
note: the visibility of the base 'foo' is 'public'
  ==> extend_supertype7.cj:10:10:
   | 
10 |     func foo(): Unit
   |          ^^^ the base 'foo' is in 'interface'
   | 

error: 'main' is missing
 ==> extend_supertype7.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
