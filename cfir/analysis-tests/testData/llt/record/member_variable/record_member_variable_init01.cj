// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct A {
    public static let a: Int64
}

/* SCAN
error: the static member variable 'a' is not initialized
  ==> record_member_variable_init01.cj:10:5:
   | 
10 |     public static let a: Int64
   |     ^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: you may add an initializer inline or define a 'static init' to perform initialization

error: 'main' is missing
 ==> record_member_variable_init01.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
