// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

struct A {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

main(): Int64 {
    var x = A()
    x.a = 2
    x.b = 2

    let y = A()
    y.a = 2
    y.b = 2

    var m = B()
    m.a = 2
    m.b = 2

    let n = B()
    n.a = 2
    n.b = 2

    return 0
}

struct B {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

/* SCAN
error: cannot assign to immutable value
  ==> assign_const_instance_var.cj:17:5:
   | 
17 |     x.b = 2
   |     ^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> assign_const_instance_var.cj:11:16:
   | 
11 |     public let b: Int32 = 1
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> assign_const_instance_var.cj:20:5:
   | 
20 |     y.a = 2
   |     ^^^^^^^ 
   | 
note: variable 'y' is immutable
  ==> assign_const_instance_var.cj:19:9:
   | 
19 |     let y = A()
   |         ^ 
   | 

error: cannot assign to immutable value
  ==> assign_const_instance_var.cj:21:5:
   | 
21 |     y.b = 2
   |     ^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> assign_const_instance_var.cj:11:16:
   | 
11 |     public let b: Int32 = 1
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> assign_const_instance_var.cj:25:5:
   | 
25 |     m.b = 2
   |     ^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> assign_const_instance_var.cj:36:16:
   | 
36 |     public let b: Int32 = 1
   |                ^ 
   | 

error: cannot assign to immutable value
  ==> assign_const_instance_var.cj:28:5:
   | 
28 |     n.a = 2
   |     ^^^^^^^ 
   | 
note: variable 'n' is immutable
  ==> assign_const_instance_var.cj:27:9:
   | 
27 |     let n = B()
   |         ^ 
   | 

error: cannot assign to immutable value
  ==> assign_const_instance_var.cj:29:5:
   | 
29 |     n.b = 2
   |     ^^^^^^^ 
   | 
note: variable 'b' is immutable
  ==> assign_const_instance_var.cj:36:16:
   | 
36 |     public let b: Int32 = 1
   |                ^ 
   | 

6 errors generated, 6 errors printed.
*/
