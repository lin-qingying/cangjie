// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

var a: Int64 = 5
var b: Int32 = 3
var c = [a, 5]
var d: Int32 = c[1]
var f: Array<Int64> = [[]]
var g: Array<Int64> = [3.1]
var h: Array<Array<Int64>> = [c, [b]]
let i = [[10, 10, 10], [20, 20, r'a']]
var j = []
var k: Int64 = [].size

/* SCAN
error: mismatched types
  ==> arraylit1.cj:12:16:
   | 
12 | var d: Int32 = c[1]
   |                ^^^^ expected 'Int32', found 'Int64'
   | 

error: mismatched types
  ==> arraylit1.cj:13:24:
   | 
13 | var f: Array<Int64> = [[]]
   |                        ^^ expected 'Int64', found 'Array'
   | 

error: cannot convert a floating-point literal to type 'Int64'
  ==> arraylit1.cj:14:24:
   | 
14 | var g: Array<Int64> = [3.1]
   |                        ^ 
   | 

error: mismatched types
  ==> arraylit1.cj:15:35:
   | 
15 | var h: Array<Array<Int64>> = [c, [b]]
   |                                   ^ expected 'Int64', found 'Int32'
   | 

error: inconsistent element type for array literal
  ==> arraylit1.cj:16:24:
   | 
16 | let i = [[10, 10, 10], [20, 20, r'a']]
   |                        ^ 
   | 
   # note: Traces:
The types 'Int64' and 'Rune' do not have the smallest common supertype


error: array literal type cannot be inferred
  ==> arraylit1.cj:17:9:
   | 
17 | var j = []
   |         ^ 
   | 

error: array literal type cannot be inferred
  ==> arraylit1.cj:18:16:
   | 
18 | var k: Int64 = [].size
   |                ^ 
   | 

7 errors generated, 7 errors printed.
*/
