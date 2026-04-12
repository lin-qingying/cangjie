// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    (Some(1 + true) ?? 2(false)) + true
    0
}

/* SCAN
error: invalid binary operator '+' on type 'Int64' and 'Bool'
  ==> coalescing_1.cj:10:13:
   | 
10 |     (Some(1 + true) ?? 2(false)) + true
   |             ^ 
   | 
   # note: you may want to implement 'operator func +(right: Bool)' for type 'Int64'

error: no matching function for operator '()' function call
  ==> coalescing_1.cj:10:24:
   | 
10 |     (Some(1 + true) ?? 2(false)) + true
   |                        ^ 
   | 

2 errors generated, 2 errors printed.
*/
