// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt 2>&1 | compare %f 

main() {
    let a = Some(1)
    if (let b <- a && let b <- Some(a)) {
        1
    } else {
        0
    }
}

/* SCAN
error: redefinition of declaration 'b'
  ==> samename_oneif.cj:11:27:
   | 
11 |     if (let b <- a && let b <- Some(a)) {
   |                           ^ 
   | 
note: 'b' is previously declared here
  ==> samename_oneif.cj:11:13:
   | 
11 |     if (let b <- a && let b <- Some(a)) {
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
