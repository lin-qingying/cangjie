// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt 2>&1 | compare %f 

main() {
    let a = Some(1)
    if (let Some(b) <- a) {
        let b = b
    } else {
        0
    }
}

/* SCAN
error: redefinition of declaration 'b'
  ==> samename_conditionandifbody.cj:12:13:
   | 
12 |         let b = b
   |             ^ 
   | 
note: 'b' is previously declared here
  ==> samename_conditionandifbody.cj:11:18:
   | 
11 |     if (let Some(b) <- a) {
   |                  ^ 
   | 

error: undeclared identifier 'b'
  ==> samename_conditionandifbody.cj:12:17:
   | 
12 |         let b = b
   |                 ^ 
   | 

2 errors generated, 2 errors printed.
*/
