// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo(): Int32 {
    var a: Int32 = 1
    var m: Int32 = 4
    var b = match (m) {
        case 1 => 1
        case 2 => 2
        case 3 => 3
    }
    return b
}

/* SCAN
error: non-exhaustive patterns
  ==> matchcase3.cj:12:20:
   | 
12 |     var b = match (m) {
   |                    ^ the selector is of type 'Int32'
   | 
   # note: _ is not covered

1 error generated, 1 error printed.
*/
