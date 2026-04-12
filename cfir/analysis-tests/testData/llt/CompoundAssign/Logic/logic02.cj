// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    var a = 2
    var b = 15
    a &&= b
    a ||= b
    return 0
}

/* SCAN
error: type incompatible in this compound assignment expression
  ==> logic02.cj:12:5:
   | 
12 |     a &&= b
   |     ^ 
   | 

error: type incompatible in this compound assignment expression
  ==> logic02.cj:13:5:
   | 
13 |     a ||= b
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
