// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
public func foo() {
    while (let a <- 0) {
        let a = 1
    }
}

main() {
    foo()
    0
}

/* SCAN
error: redefinition of declaration 'a'
  ==> while_let9.cj:10:13:
   | 
10 |         let a = 1
   |             ^ 
   | 
note: 'a' is previously declared here
 ==> while_let9.cj:9:16:
  | 
9 |     while (let a <- 0) {
  |                ^ 
  | 

1 error generated, 1 error printed.
*/
