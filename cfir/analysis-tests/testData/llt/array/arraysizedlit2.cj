// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var i: Int64 = 1

    var a: Array<Array<Int64>> = Array<Array<Int64>>(i, Array<Int64>(i, i))

    return 0
}

/* SCAN
error: mismatched types
  ==> arraysizedlit2.cj:12:73:
   | 
12 |     var a: Array<Array<Int64>> = Array<Array<Int64>>(i, Array<Int64>(i, i))
   |                                                                         ^ expected '(Int64) -> Int64', found 'Int64'
   | 

1 error generated, 1 error printed.
*/
