// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum D {
    d(Int64) | d
}

enum E {
    e(D) | e
}

main(): Int64 {
    var a: E = e(d(1))
    match (a) {
        case e(d) => 1
        case e => 2
    }
}

/* SCAN
error: non-exhaustive patterns
  ==> enum28.cj:19:12:
   | 
19 |     match (a) {
   |            ^ the selector is of type 'Enum-E'
   | 
   # note: e(d(_)) is not covered

1 error generated, 1 error printed.
*/
