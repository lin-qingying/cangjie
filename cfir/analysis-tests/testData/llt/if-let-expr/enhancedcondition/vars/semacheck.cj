// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt 2>&1 | compare %f

main() {
    if (let a1 | a2 | a3 <- 1 && let a4 | a5 | a6 <- 1 || let a7 | a8 | a9 <- 1) {
        return 0
    }
    while (let a1 | a2 | a3 <- 1 && let a4 | a5 | a6 <- 1 || let a7 | a8 | a9 <- 1) {
        return 0
    }
    return 1
}

/* SCAN
error: cannot introduce variables in conditions connected by '||'
  ==> semacheck.cj:10:13:
   | 
10 |     if (let a1 | a2 | a3 <- 1 && let a4 | a5 | a6 <- 1 || let a7 | a8 | a9 <- 1) {
   |             ^^   ~~   ~~             ~~   ~~   ~~             ~~   ~~   ~~ 
   | 

error: cannot introduce variables in conditions connected by '||'
  ==> semacheck.cj:13:16:
   | 
13 |     while (let a1 | a2 | a3 <- 1 && let a4 | a5 | a6 <- 1 || let a7 | a8 | a9 <- 1) {
   |                ^^   ~~   ~~             ~~   ~~   ~~             ~~   ~~   ~~ 
   | 

2 errors generated, 2 errors printed.
*/
