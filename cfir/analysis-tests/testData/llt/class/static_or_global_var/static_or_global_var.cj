// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

var a = B.a
var b = A.b
var d = 1

class B {
    public static var a: Int64 = A.b
    public static var c = d
}

class A {
    public static var b: Int64 = B.a
}

/* SCAN
error: global/static variable 'a' is used before initialization during initializing 'a'
 ==> static_or_global_var.cj:9:11:
  | 
9 | var a = B.a
  |           ^ 
  | 

error: global/static variable 'b' is used before initialization during initializing 'b'
  ==> static_or_global_var.cj:10:11:
   | 
10 | var b = A.b
   |           ^ 
   | 

error: global/static variable 'b' is used before initialization during initializing 'a'
  ==> static_or_global_var.cj:14:36:
   | 
14 |     public static var a: Int64 = A.b
   |                                    ^ 
   | 

error: 'main' is missing
 ==> static_or_global_var.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

4 errors generated, 4 errors printed.
*/
