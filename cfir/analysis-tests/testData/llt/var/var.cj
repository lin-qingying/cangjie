// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

func f() {
    var a = a
    var b: (Unit) -> Int32 = {=> 1}
    var c: (Unit) -> Int32 = 1
}

/* SCAN
error: undeclared identifier 'a'
  ==> var.cj:10:13:
   | 
10 |     var a = a
   |             ^ 
   | 

error: mismatched types
  ==> var.cj:11:30:
   | 
11 |     var b: (Unit) -> Int32 = {=> 1}
   |                              ^^^^^^ expected '(Unit) -> Int32', found '() -> Int64'
   | 

error: cannot convert an integer literal to type '(Unit) -> Int32'
  ==> var.cj:12:30:
   | 
12 |     var c: (Unit) -> Int32 = 1
   |                              ^ 
   | 

3 errors generated, 3 errors printed.
*/
