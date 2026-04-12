// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
public func foo(): Bool {
    while (let a <- 0) {
    }
}

main() {
    foo()
    0
}

/* SCAN
error: mismatched types
  ==> while_let8.cj:9:5:
   | 
9  |       while (let a <- 0) {
   |  _____^
10 | |     }
   | |_____^ expected 'Bool', found 'Unit'
   | 

1 error generated, 1 error printed.
*/
