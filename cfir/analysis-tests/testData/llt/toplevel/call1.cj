// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo(): Int32 {
    return 1
}

var a: Int32 = foo(1)

/* SCAN
error: extra argument given for parameter list '()' in call
  ==> call1.cj:13:19:
   | 
13 | var a: Int32 = foo(1)
   |                   ^^^ expected 0 arguments, found 1
   | 
note: found candidate
 ==> call1.cj:9:6:
  | 
9 | func foo(): Int32 {
  |      ^^^ 
  | 

1 error generated, 1 error printed.
*/
