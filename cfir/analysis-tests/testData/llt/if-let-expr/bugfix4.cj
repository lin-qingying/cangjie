// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
public func foo(): Bool {
    if (let a <- 0) {
    } else {
    }
}

main () {
    foo()
    0
}


/* SCAN
error: mismatched types
 ==> bugfix4.cj:3:21:
  | 
3 |       if (let a <- 0) {
  |  _____________________^
4 | |     } else {
  | |_____^ expected 'Bool', found 'Unit'
  | 

error: mismatched types
 ==> bugfix4.cj:4:12:
  | 
4 |       } else {
  |  ____________^
5 | |     }
  | |_____^ expected 'Bool', found 'Unit'
  | 

2 errors generated, 2 errors printed.
*/

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
