// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo() {
    let temp = 2
    return temp % match (foo) {
        case 2 => 0
        case _ => 1
    }
}

func foo1() {
    let temp = 2
    return foo % match (temp) {
        case 1 => 0
        case _ => 1
    }
}

main(): Int64 {
    return foo() + foo1()
}

/* SCAN
error: unable to infer return type, please add type annotation
 ==> infer_return_fail.cj:9:6:
  | 
9 | func foo() {
  |      ^^^ 
  | 
note: with recursive usage from
  ==> infer_return_fail.cj:11:26:
   | 
11 |     return temp % match (foo) {
   |                          ^^^ 
   | 

1 error generated, 1 error printed.
*/
