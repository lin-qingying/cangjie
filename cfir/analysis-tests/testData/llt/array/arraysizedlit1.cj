// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

var a: Int64 = 5
var b: Int32 = 3
var c = Array<Int32>(a, b)
var d = Array<Int32>(b, a)
var e = Array<Int32>(a, [b])
var f: Array<Int64> = Array<Int32>(a, b)

/* SCAN
error: mismatched types
  ==> arraysizedlit1.cj:11:25:
   | 
11 | var c = Array<Int32>(a, b)
   |                         ^ expected '(Int64) -> Int32', found 'Int32'
   | 

error: mismatched types
  ==> arraysizedlit1.cj:12:22:
   | 
12 | var d = Array<Int32>(b, a)
   |                      ^ expected 'Int64', found 'Int32'
   | 

error: mismatched types
  ==> arraysizedlit1.cj:13:25:
   | 
13 | var e = Array<Int32>(a, [b])
   |                         ^^^ expected '(Int64) -> Int32', found 'Array'
   | 

error: mismatched types
  ==> arraysizedlit1.cj:14:39:
   | 
14 | var f: Array<Int64> = Array<Int32>(a, b)
   |                                       ^ expected '(Int64) -> Int32', found 'Int32'
   | 

4 errors generated, 4 errors printed.
*/
