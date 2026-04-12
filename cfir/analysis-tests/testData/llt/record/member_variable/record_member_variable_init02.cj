// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct A {
    public static let a: Int64 = A.b
    public static let b: Int64 = A.a
}

/* SCAN
error: variable 'b' is used before initialization
  ==> record_member_variable_init02.cj:10:34:
   | 
10 |     public static let a: Int64 = A.b
   |                                  ^ 
   | 

error: global/static variable 'b' is used before initialization during initializing 'a'
  ==> record_member_variable_init02.cj:10:36:
   | 
10 |     public static let a: Int64 = A.b
   |                                    ^ 
   | 

error: 'main' is missing
 ==> record_member_variable_init02.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

3 errors generated, 3 errors printed.
*/
