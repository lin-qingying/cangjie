// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

struct B {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

main(): Int64 {
    A().a = 2
    A().b = 2
    B().a = 2
    B().b = 2
    return 0
}

class A {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

/* SCAN
error: cannot assign to immutable value
  ==> let_in_init8.cj:16:5:
   | 
16 |     A().b = 2
   |     ^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_in_init8.cj:24:16:
   | 
24 |     public let b: Int32 = 1
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init8.cj:17:5:
   | 
17 |     B().a = 2
   |     ^^^^^^^^^ 
   | 
note: function call returns immutable value
  ==> let_in_init8.cj:17:5:
   | 
17 |     B().a = 2
   |     ^^^ 
   | 

error: cannot assign to immutable value
  ==> let_in_init8.cj:18:5:
   | 
18 |     B().b = 2
   |     ^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_in_init8.cj:11:16:
   | 
11 |     public let b: Int32 = 1
   |                ^ 
   | 

3 errors generated, 3 errors printed.
*/
