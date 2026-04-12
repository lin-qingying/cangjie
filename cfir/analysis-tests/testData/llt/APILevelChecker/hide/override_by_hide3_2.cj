// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: hide.cj hide_use1.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt hide.cj -o hide.%middle
// ERRCHECK: %compiler %cmp_opt %compile_lib_opt hide.%middle %n.cj -o %n.%suffix -Woff all

package ohos.dep

import ohos.labels.*

public interface I {
    func foo() {}
    func goo() {}
    func koo() {}
}

@!Hide[isChecked: false]
public class B <: I {
    public func foo() {}
    public func goo() {}
    public func koo() {}
}

/* SCAN
error: should be marked with '@!Hide' to be hidden
  ==> override_by_hide3_2.cj:16:5:
   | 
16 |     func foo() {}
   |     ^^^^^^^^^^^^^ 
   | 
note: the override function is marked with '@!Hide'
  ==> override_by_hide3_2.cj:23:5:
   | 
23 |     public func foo() {}
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 

error: should be marked with '@!Hide' to be hidden
  ==> override_by_hide3_2.cj:17:5:
   | 
17 |     func goo() {}
   |     ^^^^^^^^^^^^^ 
   | 
note: the override function is marked with '@!Hide'
  ==> override_by_hide3_2.cj:24:5:
   | 
24 |     public func goo() {}
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 

error: should be marked with '@!Hide' to be hidden
  ==> override_by_hide3_2.cj:18:5:
   | 
18 |     func koo() {}
   |     ^^^^^^^^^^^^^ 
   | 
note: the override function is marked with '@!Hide'
  ==> override_by_hide3_2.cj:25:5:
   | 
25 |     public func koo() {}
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 

3 errors generated, 3 errors printed.
*/
