// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj call_hide_extend_dep.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.cj -o hide.%middle
// ERRCHECK: %compiler %cmp_opt %compile_lib_opt hide.%middle %n.cj -o %n.%suffix -Woff all

package ohos.dep

import ohos.labels.*

public class A {
    public A(@!Hide[isChecked: true] public var a!: Int64 = 1/* ok */, @!Hide[isChecked: true] b!: Int64 = 2/* error */) {}

    public init(@!Hide[isChecked: true] b!: String) { a = 2 }

    public func foo(@!Hide[isChecked: true] c: Int64) {}
}

/* SCAN
error: function parameter cannot be annotated with '@!Hide'
  ==> call_hide_param_dep_err.cj:18:41:
   | 
18 |     public init(@!Hide[isChecked: true] b!: String) { a = 2 }
   |                                         ^^^^^^^^^^ 
   | 

error: function parameter cannot be annotated with '@!Hide'
  ==> call_hide_param_dep_err.cj:20:45:
   | 
20 |     public func foo(@!Hide[isChecked: true] c: Int64) {}
   |                                             ^^^^^^^^ 
   | 

error: function parameter cannot be annotated with '@!Hide'
  ==> call_hide_param_dep_err.cj:16:96:
   | 
16 |     public A(@!Hide[isChecked: true] public var a!: Int64 = 1/* ok */, @!Hide[isChecked: true] b!: Int64 = 2/* error */) {}
   |                                                                                                ^^^^^^^^^^^^^ 
   | 

3 errors generated, 3 errors printed.
*/
