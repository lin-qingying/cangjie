// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class Father {}

class Child <: Father {}

interface I1 {
    func f(a: Int32): Father
}

interface I2 {
    func f(a: Int32): Child
}

class C <: I1 & I2 {
    public func f(a: Int32): Father {
        Child()
    }
}

/* SCAN
error: return type of 'f' is not identical or not a subtype of the overridden/redefined/implement function
  ==> class_impl_interface_abstract_func8.cj:22:17:
   | 
22 |     public func f(a: Int32): Father {
   |                 ^ 
   | 
note: 'Class-Father' is not a subtype of 'Class-Child'
  ==> class_impl_interface_abstract_func8.cj:18:10:
   | 
18 |     func f(a: Int32): Child
   |          ^ 
   | 

1 error generated, 1 error printed.
*/
