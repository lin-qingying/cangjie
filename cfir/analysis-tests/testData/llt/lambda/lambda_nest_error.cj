// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo(): Unit {}

main(): Unit {
    let a: Int64 = UInt16({a: Int64 => foo(1, 2)}(1)) & 1
}

/* SCAN
error: extra arguments given for parameter list '()' in call
  ==> lambda_nest_error.cj:12:43:
   | 
12 |     let a: Int64 = UInt16({a: Int64 => foo(1, 2)}(1)) & 1
   |                                           ^^^^^^ expected 0 arguments, found 2
   | 
note: found candidate
 ==> lambda_nest_error.cj:9:6:
  | 
9 | func foo(): Unit {}
  |      ^^^ 
  | 

1 error generated, 1 error printed.
*/
