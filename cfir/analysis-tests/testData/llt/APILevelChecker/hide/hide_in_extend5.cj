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

@!Hide[isChecked: false]
extend A {
    @!Hide
    public func foo() {}
    @!Hide[isChecked: false]
    public func goo() {}
    @!Hide[isChecked: true] // error
    public func hoo() {}
    public func ioo() {}
}

/* SCAN
error: the parameter 'isChecked' of '@!Hide' is true
  ==> hide_in_extend5.cj:24:5:
   | 
24 |     public func hoo() {}
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 
note: should be same with
  ==> hide_in_extend5.cj:18:1:
   | 
18 |   extend A {
   |  _^
19 | |     @!Hide
   | | ...
26 | | }
   | |_^
   | 

1 error generated, 1 error printed.
*/
