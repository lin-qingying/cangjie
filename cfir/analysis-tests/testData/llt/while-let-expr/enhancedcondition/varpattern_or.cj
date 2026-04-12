// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt -o %output 2>&1 | compare %f

main() {
    let a = Some(15)
    while (let Some(b) <- a || true) {
        return 0
    }

    while (false || let Some(b) <- a) {
        return b - 1
    }

    return 2
}

/* SCAN
error: cannot introduce variables in conditions connected by '||'
  ==> varpattern_or.cj:11:21:
   | 
11 |     while (let Some(b) <- a || true) {
   |                     ^ 
   | 

error: cannot introduce variables in conditions connected by '||'
  ==> varpattern_or.cj:15:30:
   | 
15 |     while (false || let Some(b) <- a) {
   |                              ^ 
   | 

2 errors generated, 2 errors printed.
*/
