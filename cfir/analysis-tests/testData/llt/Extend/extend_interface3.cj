// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

// extend decl must implement abstract functions of its super interfaces
class A {}

interface I {
    func foo(): Unit
}

extend A <: I {
    // must implement foo() here
}

/* SCAN
error: implementation of function or property is needed in 'extend A'
  ==> extend_interface3.cj:16:1:
   | 
16 | extend A <: I {
   | ^ 
   | 
note: unimplemented interface function 'foo'
  ==> extend_interface3.cj:13:5:
   | 
13 |     func foo(): Unit
   |     ^ 
   | 

error: 'main' is missing
 ==> extend_interface3.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
