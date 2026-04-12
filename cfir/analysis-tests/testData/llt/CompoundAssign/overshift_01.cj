// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main(): Int64 {
    var a: Int8 = 1
    a <<= 9
    a >>= 9
    return 1
}

/* SCAN
error: shift count overflow
  ==> overshift_01.cj:11:11:
   | 
11 |     a <<= 9
   |           ^ 
   | 

error: shift count overflow
  ==> overshift_01.cj:12:11:
   | 
12 |     a >>= 9
   |           ^ 
   | 

2 errors generated, 2 errors printed.
*/
