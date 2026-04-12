// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    (Some(1(true)) ?? false) + true
    (Some(true) ?? 1) * false

    let err = 1(true)
    err ?? err
    0
}

/* SCAN
error: no matching function for operator '()' function call
  ==> coalescing_0.cj:10:11:
   | 
10 |     (Some(1(true)) ?? false) + true
   |           ^ 
   | 

error: cannot convert an integer literal to type 'Bool'
  ==> coalescing_0.cj:11:20:
   | 
11 |     (Some(true) ?? 1) * false
   |                    ^ 
   | 

error: no matching function for operator '()' function call
  ==> coalescing_0.cj:13:15:
   | 
13 |     let err = 1(true)
   |               ^ 
   | 

3 errors generated, 3 errors printed.
*/
