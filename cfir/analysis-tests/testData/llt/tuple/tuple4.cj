// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var (a, b, c) = (0, 1, 2)
    var d = (3, 4, (a, b, c)[3])
    var (one, two, three): (Int32, Int32, Int32) = (5, 6, 7, 8)
    return 0
}

/* SCAN
error: tuple index must be in bounds
  ==> tuple4.cj:11:30:
   | 
11 |     var d = (3, 4, (a, b, c)[3])
   |                              ^ 
   | 

error: mismatched types
  ==> tuple4.cj:12:52:
   | 
12 |     var (one, two, three): (Int32, Int32, Int32) = (5, 6, 7, 8)
   |                                                    ^^^^^^^^^^^^ expected 'Tuple<Int32, Int32, Int32>', found 'Tuple<Int64, Int64, Int64, Int64>'
   | 

2 errors generated, 2 errors printed.
*/
