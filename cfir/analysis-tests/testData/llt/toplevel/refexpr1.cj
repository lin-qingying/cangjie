// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

var a: Int64 = 5
var b = a
var c: Int64 = d
var e = while (b) {
    var c: Int32 = 1
    var f: Int64 = c
}

func F(a: Int32) {
    var g: Int64 = a
}

/* SCAN
error: undeclared identifier 'd'
  ==> refexpr1.cj:11:16:
   | 
11 | var c: Int64 = d
   |                ^ 
   | 

error: mismatched types
  ==> refexpr1.cj:12:16:
   | 
12 | var e = while (b) {
   |                ^ expected 'Bool', found 'Int64'
   | 

error: mismatched types
  ==> refexpr1.cj:14:20:
   | 
14 |     var f: Int64 = c
   |                    ^ expected 'Int64', found 'Int32'
   | 

error: mismatched types
  ==> refexpr1.cj:18:20:
   | 
18 |     var g: Int64 = a
   |                    ^ expected 'Int64', found 'Int32'
   | 

4 errors generated, 4 errors printed.
*/
