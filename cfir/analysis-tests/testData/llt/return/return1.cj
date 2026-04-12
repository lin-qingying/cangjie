// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func f(): Float64 {
    var a: Int64 = 1
    a
}

func f(): Int64 {
    while (true) {}
}

func foo() {
    return true
}

func bar() {
    return 1
}

/* SCAN
error: function 'f' has overload conflicts
  ==> return1.cj:14:6:
   | 
14 | func f(): Int64 {
   |      ^ 
   | 
note: conflict with the declaration
 ==> return1.cj:9:6:
  | 
9 | func f(): Float64 {
  |      ^ 
  | 

error: mismatched types
  ==> return1.cj:11:5:
   | 
11 |     a
   |     ^ expected 'Float64', found 'Int64'
   | 

error: mismatched types
  ==> return1.cj:15:5:
   | 
15 |     while (true) {}
   |     ^^^^^^^^^^^^^^^ expected 'Int64', found 'Unit'
   | 

3 errors generated, 3 errors printed.
*/
