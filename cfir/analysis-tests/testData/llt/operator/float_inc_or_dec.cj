// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main() {
    var a: Float64 = 0.0
    a++
    a--
}

/* SCAN
error: mismatched types
  ==> float_inc_or_dec.cj:11:5:
   | 
11 |     a++
   |     ^ expected 'integer type', found 'Float64'
   | 
   # note: the base of increment or decrement expressions should be of integer type

error: mismatched types
  ==> float_inc_or_dec.cj:12:5:
   | 
12 |     a--
   |     ^ expected 'integer type', found 'Float64'
   | 
   # note: the base of increment or decrement expressions should be of integer type

2 errors generated, 2 errors printed.
*/
