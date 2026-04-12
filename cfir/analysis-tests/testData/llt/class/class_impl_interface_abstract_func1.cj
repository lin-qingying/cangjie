// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

interface I {
    func a(a: Int32): Int32
}

class A <: I {
    public func a(): Int32 {
        1
    }
}

/* SCAN
error: class 'A' missing abstract modifier, otherwise abstract function or property should be implemented
  ==> class_impl_interface_abstract_func1.cj:13:1:
   | 
13 | class A <: I {
   | ^ 
   | 
note: unimplemented interface function 'a'
  ==> class_impl_interface_abstract_func1.cj:10:5:
   | 
10 |     func a(a: Int32): Int32
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
