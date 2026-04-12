// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo(): Int32 {
    let x: Int64 = 1
    let flag: Bool = true
    var b: Int32 = match (x) {
        case _ where flag => 1
        case _ where false => 2
    }
    return b
}

/* SCAN
error: non-exhaustive patterns
  ==> matchcase8.cj:12:27:
   | 
12 |     var b: Int32 = match (x) {
   |                           ^ the selector is of type 'Int64'
   | 
   # note: _ is not covered

1 error generated, 1 error printed.
*/
