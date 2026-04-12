// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

struct B {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

func m() {
    return B()
}

func k() {
    return A()
}

main(): Int64 {
    k().a = 2
    k().b = 2
    m().a = 2
    m().b = 2

    return 0
}

class A {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

/* SCAN
error: cannot assign to immutable value
  ==> let_value_assign_2.cj:24:5:
   | 
24 |     k().b = 2
   |     ^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_value_assign_2.cj:33:16:
   | 
33 |     public let b: Int32 = 1
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> let_value_assign_2.cj:25:5:
   | 
25 |     m().a = 2
   |     ^^^^^^^^^ 
   | 
note: function call returns immutable value
  ==> let_value_assign_2.cj:25:5:
   | 
25 |     m().a = 2
   |     ^^^ 
   | 

error: cannot assign to immutable value
  ==> let_value_assign_2.cj:26:5:
   | 
26 |     m().b = 2
   |     ^^^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_value_assign_2.cj:11:16:
   | 
11 |     public let b: Int32 = 1
   |                ^ 
   | 

3 errors generated, 3 errors printed.
*/
