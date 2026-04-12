// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    var (a, b): (Int64, Int64)
    println(a)
    println(b)
}

/* SCAN
error: variable 'a' is used before initialization
  ==> variable_use_before_init_14.cj:11:13:
   | 
11 |     println(a)
   |             ^ 
   | 

error: variable 'b' is used before initialization
  ==> variable_use_before_init_14.cj:12:13:
   | 
12 |     println(b)
   |             ^ 
   | 

2 errors generated, 2 errors printed.
*/
