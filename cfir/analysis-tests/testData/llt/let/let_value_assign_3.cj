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

main(): Int64 {
    A().a++
    A().b++

    B().a++
    B().b++

    return 0
}

class A {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

/* SCAN
error: cannot assign to immutable value
  ==> let_value_assign_3.cj:16:5:
   | 
16 |     A().b++
   |     ^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_value_assign_3.cj:26:16:
   | 
26 |     public let b: Int32 = 1
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> let_value_assign_3.cj:18:5:
   | 
18 |     B().a++
   |     ^^^^^^^ 
   | 
note: function call returns immutable value
  ==> let_value_assign_3.cj:18:5:
   | 
18 |     B().a++
   |     ^^^ 
   | 

error: cannot assign to immutable value
  ==> let_value_assign_3.cj:19:5:
   | 
19 |     B().b++
   |     ^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> let_value_assign_3.cj:11:16:
   | 
11 |     public let b: Int32 = 1
   |                ^ 
   | 

3 errors generated, 3 errors printed.
*/
