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

public class A {}

@!Hide[isChecked: true]
extend A {
    @!Hide
    public func foo() {} // error
    @!Hide[isChecked: false]
    public func goo() {} // error
    @!Hide[isChecked: true]
    public func hoo() {}
    public func ioo() {}
}

/* SCAN
error: the parameter 'isChecked' of '@!Hide' is false
  ==> hide_in_extend6.cj:20:5:
   | 
20 |     public func foo() {} // error
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 
note: should be same with
  ==> hide_in_extend6.cj:18:1:
   | 
18 |   extend A {
   |  _^
19 | |     @!Hide
   | | ...
26 | | }
   | |_^
   | 

error: the parameter 'isChecked' of '@!Hide' is false
  ==> hide_in_extend6.cj:22:5:
   | 
22 |     public func goo() {} // error
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 
note: should be same with
  ==> hide_in_extend6.cj:18:1:
   | 
18 |   extend A {
   |  _^
19 | |     @!Hide
   | | ...
26 | | }
   | |_^
   | 

2 errors generated, 2 errors printed.
*/
