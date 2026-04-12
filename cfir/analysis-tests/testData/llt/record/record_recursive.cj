// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

struct A {
    public let x: A
}

struct B {
    public let x: C
    public let y: Int32
}

struct C {
    public let x: D
}

struct D {
    public let x: B
}

/* SCAN
error: value type recursive detected: 'A->A'
 ==> record_recursive.cj:9:1:
  | 
9 | struct A {
  | ^ 
  | 

error: value type recursive detected: 'B->C->D->B'
  ==> record_recursive.cj:13:1:
   | 
13 | struct B {
   | ^ 
   | 

error: the uninitialized member variable 'x' is not initialized in the constructor of class or struct
  ==> record_recursive.cj:10:5:
   | 
10 |     public let x: A
   |     ^ 
   | 

error: the uninitialized member variable 'x' is not initialized in the constructor of class or struct
  ==> record_recursive.cj:14:5:
   | 
14 |     public let x: C
   |     ^ 
   | 

error: the uninitialized member variable 'y' is not initialized in the constructor of class or struct
  ==> record_recursive.cj:15:5:
   | 
15 |     public let y: Int32
   |     ^ 
   | 

error: the uninitialized member variable 'x' is not initialized in the constructor of class or struct
  ==> record_recursive.cj:19:5:
   | 
19 |     public let x: D
   |     ^ 
   | 

error: the uninitialized member variable 'x' is not initialized in the constructor of class or struct
  ==> record_recursive.cj:23:5:
   | 
23 |     public let x: B
   |     ^ 
   | 

7 errors generated, 7 errors printed.
*/
