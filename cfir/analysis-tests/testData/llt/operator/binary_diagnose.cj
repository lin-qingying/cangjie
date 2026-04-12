// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    let a: Float32 = 10.0
    let b: Bool = true

    if (a == b) {
        return 1
    }
    return 0
}

/* SCAN
error: invalid binary operator '==' on type 'Float32' and 'Bool'
  ==> binary_diagnose.cj:13:11:
   | 
13 |     if (a == b) {
   |           ^^ 
   | 
   # note: you may want to implement 'operator func ==(right: Bool)' for type 'Float32'

1 error generated, 1 error printed.
*/
