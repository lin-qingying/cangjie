// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

interface I1 {}

interface I2 {}

class A {
    func f<T>(a: T) where T <: I1 {}
    func f<T>(a: T) where T <: I2 {} // Error, not overloading
    func f<T>(a: T) {} // Error, not overloading
}

func f<T>(a: T) where T <: I1 {}

func f<T>(a: T) where T <: I2 {} // Error, not overloading

func f<T>(a: T) {} // Error, not overloading

/* SCAN
error: function 'f' has overload conflicts
  ==> redefinition2.cj:23:6:
   | 
23 | func f<T>(a: T) {} // Error, not overloading
   |      ^ 
   | 
   # note: generic constraints are not involved in the overloading
note: conflict with the declaration
  ==> redefinition2.cj:21:6:
   | 
21 | func f<T>(a: T) where T <: I2 {} // Error, not overloading
   |      ^ 
   | 
note: conflict with the declaration
  ==> redefinition2.cj:19:6:
   | 
19 | func f<T>(a: T) where T <: I1 {}
   |      ^ 
   | 

error: function 'f' has overload conflicts
  ==> redefinition2.cj:16:10:
   | 
16 |     func f<T>(a: T) {} // Error, not overloading
   |          ^ 
   | 
   # note: generic constraints are not involved in the overloading
note: conflict with the declaration
  ==> redefinition2.cj:15:10:
   | 
15 |     func f<T>(a: T) where T <: I2 {} // Error, not overloading
   |          ^ 
   | 
note: conflict with the declaration
  ==> redefinition2.cj:14:10:
   | 
14 |     func f<T>(a: T) where T <: I1 {}
   |          ^ 
   | 

2 errors generated, 2 errors printed.
*/
