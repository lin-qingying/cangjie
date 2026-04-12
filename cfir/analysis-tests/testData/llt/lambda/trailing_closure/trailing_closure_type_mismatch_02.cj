// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

func test(a: Any) {}

main(): Int64 {
    test {=> 1}
    return 0
}

/* SCAN
error: trailing lambda cannot be used for non-function type
  ==> trailing_closure_type_mismatch_02.cj:12:10:
   | 
12 |     test {=> 1}
   |          ^^^^^^ declaration type of parameter: 'Interface-Any'
   | 
note: found candidate
 ==> trailing_closure_type_mismatch_02.cj:9:6:
  | 
9 | func test(a: Any) {}
  |      ^^^^ 
  | 

1 error generated, 1 error printed.
*/
