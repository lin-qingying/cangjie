// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

class A {
    private static let a: Int64
    private operator func +(b: A, c: A) {}
}

main() {
    A.a
    A() + A()
    return 0
}

/* SCAN
error: invalid number of parameters for operator '+'
  ==> class_access_control.cj:11:5:
   | 
11 |     private operator func +(b: A, c: A) {}
   |     ^ 
   | 

error: invalid binary operator '+' on type 'Class-A' and 'Class-A'
  ==> class_access_control.cj:16:9:
   | 
16 |     A() + A()
   |         ^ 
   | 
   # note: you may want to implement 'operator func +(right: Class-A)' for type 'Class-A'

error: can not access field 'a'
  ==> class_access_control.cj:15:5:
   | 
15 |     A.a
   |     ^ 
   | 

error: the static member variable 'a' is not initialized
  ==> class_access_control.cj:10:5:
   | 
10 |     private static let a: Int64
   |     ^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: you may add an initializer inline or define a 'static init' to perform initialization

4 errors generated, 4 errors printed.
*/
