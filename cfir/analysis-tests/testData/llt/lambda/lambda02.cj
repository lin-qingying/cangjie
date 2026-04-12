// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

let i: Int32 = 10
var lmda1 = {
    x: Int32 =>
    var test: Int32 = 10
    test + x + i
}

main(): Int64 {
    var res1: Int64 = lmda1(3)
    if (res1 != 30) {
        return 1
    }
    return 0
}

/* SCAN
error: mismatched types
  ==> lambda02.cj:17:23:
   | 
17 |     var res1: Int64 = lmda1(3)
   |                       ^^^^^^^^ expected 'Int64', found 'Int32'
   | 

1 error generated, 1 error printed.
*/
