// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt -o %output 2>&1 | compare %f

main() {
    let a = true
    let b = false
    let d = Some(3)
    while (a || b && let Some(c) <- d) {
        0
    }
    while ((let Some(c) <- d) && a || b) {
        0
    }
}

/* SCAN
error: cannot introduce variables in conditions connected by '||'
  ==> varpattern_orinand.cj:13:31:
   | 
13 |     while (a || b && let Some(c) <- d) {
   |                               ^ 
   | 

error: cannot introduce variables in conditions connected by '||'
  ==> varpattern_orinand.cj:16:22:
   | 
16 |     while ((let Some(c) <- d) && a || b) {
   |                      ^ 
   | 

2 errors generated, 2 errors printed.
*/
