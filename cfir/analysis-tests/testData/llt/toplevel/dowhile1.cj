// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

var i = 0
var b: Int32 = do {
    i++
} while (5)

/* SCAN
error: mismatched types
  ==> dowhile1.cj:10:16:
   | 
10 |   var b: Int32 = do {
   |  ________________^
11 | |     i++
12 | | } while (5)
   | |___________^ expected 'Int32', found 'Unit'
   | 

error: cannot convert an integer literal to type 'Bool'
  ==> dowhile1.cj:12:10:
   | 
12 | } while (5)
   |          ^ 
   | 

2 errors generated, 2 errors printed.
*/
