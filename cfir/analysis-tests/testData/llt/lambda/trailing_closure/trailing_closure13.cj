// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    var x = {t: Int64 => 1}
    x {}
    x {}
}

/* SCAN
error: mismatched types
  ==> trailing_closure13.cj:11:7:
   | 
11 |     x {}
   |       ^^ expected 'Int64', found '() -> Unit'
   | 

error: mismatched types
  ==> trailing_closure13.cj:12:7:
   | 
12 |     x {}
   |       ^^ expected 'Int64', found '() -> Unit'
   | 

2 errors generated, 2 errors printed.
*/
