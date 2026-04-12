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
    @!Hide
    func hoo() {}
    @!Hide[isChecked: false]
    func goo() {}
    @!Hide[isChecked: true]
    func koo() {}
}

public class A <: I {
    public func foo() {}
    public func hoo() {} // error
    public func goo() {} // error
    public func koo() {} // error
}

public class B <: I {
    @!Hide
    public func foo() {} // error
    @!Hide
    public func hoo() {}
    @!Hide
    public func goo() {}
    @!Hide
    public func koo() {} // error
}

public class C <: I {
    @!Hide[isChecked: false]
    public func foo() {} // error
    @!Hide[isChecked: false]
    public func hoo() {}
    @!Hide[isChecked: false]
    public func goo() {}
    @!Hide[isChecked: false]
    public func koo() {} // error
}

public class D <: I {
    @!Hide[isChecked: true]
    public func foo() {} // error
    @!Hide[isChecked: true]
    public func hoo() {} // error
    @!Hide[isChecked: true]
    public func goo() {} // error
    @!Hide[isChecked: true]
    public func koo() {}
}

/* SCAN
error: should be marked with '@!Hide' to be hidden
  ==> override_by_hide5.cj:16:5:
   | 
16 |     func foo() {}
   |     ^^^^^^^^^^^^^ 
   | 
note: the override function is marked with '@!Hide'
  ==> override_by_hide5.cj:34:5:
   | 
34 |     public func foo() {} // error
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 

error: the parameter 'isChecked' of '@!Hide' is false
  ==> override_by_hide5.cj:40:5:
   | 
40 |     public func koo() {} // error
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 
note: should be same with
  ==> override_by_hide5.cj:22:5:
   | 
22 |     func koo() {}
   |     ^^^^^^^^^^^^^ 
   | 

error: the parameter 'isChecked' of '@!Hide' is false
  ==> override_by_hide5.cj:51:5:
   | 
51 |     public func koo() {} // error
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 
note: should be same with
  ==> override_by_hide5.cj:22:5:
   | 
22 |     func koo() {}
   |     ^^^^^^^^^^^^^ 
   | 

error: the parameter 'isChecked' of '@!Hide' is true
  ==> override_by_hide5.cj:58:5:
   | 
58 |     public func hoo() {} // error
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 
note: should be same with
  ==> override_by_hide5.cj:18:5:
   | 
18 |     func hoo() {}
   |     ^^^^^^^^^^^^^ 
   | 

error: the parameter 'isChecked' of '@!Hide' is true
  ==> override_by_hide5.cj:60:5:
   | 
60 |     public func goo() {} // error
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 
note: should be same with
  ==> override_by_hide5.cj:20:5:
   | 
20 |     func goo() {}
   |     ^^^^^^^^^^^^^ 
   | 

5 errors generated, 5 errors printed.
*/
