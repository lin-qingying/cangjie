// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

var a: Array<Int64> = [1, 5]
var b: Array<Array<Int64>> = [a, [1]]
var c: Int32 = a[0]
var d: Int64 = b[1]

/* SCAN
error: mismatched types
  ==> array_access1.cj:11:16:
   | 
11 | var c: Int32 = a[0]
   |                ^^^^ expected 'Int32', found 'Int64'
   | 

error: mismatched types
  ==> array_access1.cj:12:16:
   | 
12 | var d: Int64 = b[1]
   |                ^^^^ expected 'Int64', found 'Struct-Array<Int64>'
   | 

2 errors generated, 2 errors printed.
*/
