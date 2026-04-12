// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

let glo = 123

func a() {
    let d = b
    let glu = glo

    let b = "hello"
}

/* SCAN
error: undeclared identifier 'b'
  ==> vardecl.cj:12:13:
   | 
12 |     let d = b
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
