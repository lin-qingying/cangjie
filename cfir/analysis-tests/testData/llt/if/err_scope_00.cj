// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    if (let Some(x) <- Some(1)) {
        let x = 0
        println(x)
    }
}

/* SCAN
error: redefinition of declaration 'x'
  ==> err_scope_00.cj:11:13:
   | 
11 |         let x = 0
   |             ^ 
   | 
note: 'x' is previously declared here
  ==> err_scope_00.cj:10:18:
   | 
10 |     if (let Some(x) <- Some(1)) {
   |                  ^ 
   | 

1 error generated, 1 error printed.
*/
