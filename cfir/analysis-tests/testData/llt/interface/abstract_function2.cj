// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

interface I {
    static func f2(a!: Int64, b!: Int64): Int64
}

class A <: I {}

interface I2 <: I {}

class B {
    static func a(): Unit
}

/* SCAN
error: function 'a' can not be abstract
  ==> abstract_function2.cj:18:5:
   | 
18 |     static func a(): Unit
   |     ^ 
   | 

error: class 'A' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> abstract_function2.cj:13:1:
   | 
13 | class A <: I {}
   | ^ 
   | 
note: unimplemented interface function 'f2'
  ==> abstract_function2.cj:10:5:
   | 
10 |     static func f2(a!: Int64, b!: Int64): Int64
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
