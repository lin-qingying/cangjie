// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    match (0) {
        case x =>
            let x = 1
            println(x)
    }
}

/* SCAN
error: redefinition of declaration 'x'
  ==> err_scope_00.cj:12:17:
   | 
12 |             let x = 1
   |                 ^ 
   | 
note: 'x' is previously declared here
  ==> err_scope_00.cj:11:14:
   | 
11 |         case x =>
   |              ^ 
   | 

1 error generated, 1 error printed.
*/
