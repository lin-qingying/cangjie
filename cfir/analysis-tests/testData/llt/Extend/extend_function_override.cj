// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

// extend member functions cannot be shadowed by it sub classes
open class A {}

extend A {
    func foo() {}
}

class B <: A {
    func foo() {}
}

/* SCAN
error: cannot override function 'foo' in extend of supertype
  ==> extend_function_override.cj:17:10:
   | 
17 |     func foo() {}
   |          ^^^ 
   | 
note: overridden definition of 'foo' is here
  ==> extend_function_override.cj:13:10:
   | 
13 |     func foo() {}
   |          ^^^ 
   | 

error: 'main' is missing
 ==> extend_function_override.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
