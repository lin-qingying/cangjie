// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func test(a: (Int64) -> Int64): (Int64) -> Int64 {
    return a
}

main() {
    let x = test {i: Int8 => test {i => i}}
    return 0
}

/* SCAN
error: mismatched types
  ==> trailing_closure15.cj:14:18:
   | 
14 |     let x = test {i: Int8 => test {i => i}}
   |                  ^^^^^^^^^^^^^^^^^^^^^^^^^^ expected '(Int64) -> Int64', found '(Int8) -> (Int64) -> Int64'
   | 

1 error generated, 1 error printed.
*/
