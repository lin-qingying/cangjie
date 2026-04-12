// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let aa: Option<Bool> = None
    if (let Some(xx) <- aa) {
        println("11")
    } else if (!xx) {
        println("22")
    } else {
        println("33")
    }
}

/* SCAN
error: undeclared identifier 'xx'
  ==> scope.cj:13:17:
   | 
13 |     } else if (!xx) {
   |                 ^ 
   | 

1 error generated, 1 error printed.
*/
