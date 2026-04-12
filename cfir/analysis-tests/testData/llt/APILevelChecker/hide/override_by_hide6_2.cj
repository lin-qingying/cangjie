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

public interface I1 {
    func foo() {}
}

@!Hide
public class A1 <: I1 {
    public func foo() {}
}

@!Hide
public interface I2 {
    func foo() {}
}

public class A2 <: I2 {
    public func foo() {}
}

@!Hide[isChecked: false]
public interface I3 {
    func foo() {}
}

@!Hide[isChecked: true]
public class A3 <: I3 {
    public func foo() {}
}

@!Hide[isChecked: true]
public interface I4 {
    func foo() {}
}

@!Hide[isChecked: false]
public class A4 <: I4 {
    public func foo() {}
}

/* SCAN
error: should be marked with '@!Hide' to be hidden
  ==> override_by_hide6_2.cj:16:5:
   | 
16 |     func foo() {}
   |     ^^^^^^^^^^^^^ 
   | 
note: the override function is marked with '@!Hide'
  ==> override_by_hide6_2.cj:21:5:
   | 
21 |     public func foo() {}
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 

error: the parameter 'isChecked' of '@!Hide' is true
  ==> override_by_hide6_2.cj:40:5:
   | 
40 |     public func foo() {}
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 
note: should be same with
  ==> override_by_hide6_2.cj:35:5:
   | 
35 |     func foo() {}
   |     ^^^^^^^^^^^^^ 
   | 

error: the parameter 'isChecked' of '@!Hide' is false
  ==> override_by_hide6_2.cj:50:5:
   | 
50 |     public func foo() {}
   |     ^^^^^^^^^^^^^^^^^^^^ 
   | 
note: should be same with
  ==> override_by_hide6_2.cj:45:5:
   | 
45 |     func foo() {}
   |     ^^^^^^^^^^^^^ 
   | 

3 errors generated, 3 errors printed.
*/
