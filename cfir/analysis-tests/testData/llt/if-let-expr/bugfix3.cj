// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
public func foo(): Bool {
    if (let a <- 0) {
    }
}

main () {
    foo()
    0
}


/* SCAN
error: mismatched types
 ==> bugfix3.cj:3:5:
  | 
3 |       if (let a <- 0) {
  |  _____^
4 | |     }
  | |_____^ expected 'Bool', found 'Unit'
  | 
  # note: the type of an 'if' expression without an 'else' branch is always 'Unit'

1 error generated, 1 error printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
