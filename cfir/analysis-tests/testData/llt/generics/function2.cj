// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func Add<T>(a: T, b: T): T where U <: Num {
    return a + b
}

func g<T>() where T <: update {}

func g<T>() where T <: Int64 {}

/* SCAN
error: undeclared type name 'U'
 ==> function2.cj:9:34:
  | 
9 | func Add<T>(a: T, b: T): T where U <: Num {
  |                                  ^ 
  | 

error: undeclared type name 'Num'
 ==> function2.cj:9:39:
  | 
9 | func Add<T>(a: T, b: T): T where U <: Num {
  |                                       ^ 
  | 

error: undeclared type name 'update'
  ==> function2.cj:13:24:
   | 
13 | func g<T>() where T <: update {}
   |                        ^ 
   | 

error: the upper bound 'Int64' of generic parameter 'Generics-T' must be class or interface
  ==> function2.cj:15:8:
   | 
15 | func g<T>() where T <: Int64 {}
   |        ^ 
   | 

error: function 'g' has overload conflicts
  ==> function2.cj:15:6:
   | 
15 | func g<T>() where T <: Int64 {}
   |      ^ 
   | 
   # note: generic constraints are not involved in the overloading
note: conflict with the declaration
  ==> function2.cj:13:6:
   | 
13 | func g<T>() where T <: update {}
   |      ^ 
   | 

error: invalid binary operator '+' on type 'Generics-T' and 'Generics-T'
  ==> function2.cj:10:14:
   | 
10 |     return a + b
   |              ^ 
   | 
   # note: you may want to implement 'operator func +(right: Generics-T)' for type 'Generics-T'

6 errors generated, 6 errors printed.
*/
