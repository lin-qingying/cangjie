// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct A {
    public let b: Int64 = 1
    public static var a: () -> Int64 = {=> b}
}

/* SCAN
error: invalid use of non-static member 'b' in static lambda expression
  ==> record_member_variable_init04.cj:11:44:
   | 
11 |     public static var a: () -> Int64 = {=> b}
   |                                            ^ 
   | 

error: 'main' is missing
 ==> record_member_variable_init04.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
