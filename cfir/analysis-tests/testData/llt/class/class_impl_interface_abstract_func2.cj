// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

interface I1 {
    func a(a: Int32): Int32
}

interface I2 <: I1 {}

interface I3 <: I2 {}

class A <: I2 {
    public func a(): Int32 {
        1
    }
}

/* SCAN
error: class 'A' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> class_impl_interface_abstract_func2.cj:17:1:
   | 
17 | class A <: I2 {
   | ^ 
   | 
note: unimplemented interface function 'a'
  ==> class_impl_interface_abstract_func2.cj:10:5:
   | 
10 |     func a(a: Int32): Int32
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
