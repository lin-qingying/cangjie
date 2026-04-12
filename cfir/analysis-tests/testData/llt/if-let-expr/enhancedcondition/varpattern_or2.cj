// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt -o %output 2>&1 | compare %f

main() {
    let a = Some(15)
    if (false || let Some(b) <- a || let Some(c) <- a) {
        return b - 1
    }

    return 2
}

enum A {
    B | C(Int32) | D(Int64)
}

func foo(a: A) {
    if (let B | C(v) | D(d) <- a || false) {
        1
    }
}

/* SCAN
error: cannot introduce variables in conditions connected by '||'
  ==> varpattern_or2.cj:11:27:
   | 
11 |     if (false || let Some(b) <- a || let Some(c) <- a) {
   |                           ^                   ~ 
   | 

error: cannot introduce variables in conditions connected by '||'
  ==> varpattern_or2.cj:23:19:
   | 
23 |     if (let B | C(v) | D(d) <- a || false) {
   |                   ^      ~ 
   | 

2 errors generated, 2 errors printed.
*/
