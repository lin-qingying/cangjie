// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A {
    operator func [](arg1: Int64, arg2: String, x!: Int64): Int64 {
        return 1
    }
}

/* SCAN
error: overloaded operator '[]' can only have one named parameter 'value'
  ==> index_set_overload_n.cj:10:49:
   | 
10 |     operator func [](arg1: Int64, arg2: String, x!: Int64): Int64 {
   |                                                 ^ 
   | 

error: 'main' is missing
 ==> index_set_overload_n.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

2 errors generated, 2 errors printed.
*/
