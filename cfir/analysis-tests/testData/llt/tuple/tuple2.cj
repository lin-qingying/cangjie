// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %cmp_opt %typecheck %cmp_opt %f 2>&1 | compare %f

main() : Int64 {
    let (m) = (1)
    let n = match (m) {
        case (p) => p
    }
    var a: (Int32,Int32,Int32,Int32) = (1, 2, 3, 4)
    var b = (8, a[5], 9)
    return b[1]
}

/* SCAN
[33mwarning[0m: '--error-count-limit' is specified multiple times. The last one is chosen.
[33mwarning[0m: '--diagnostic-format' is specified multiple times. The last one is chosen.
error: 1-element tuple pattern is not allowed
  ==> tuple2.cj:10:9:
   | 
10 |     let (m) = (1)
   |         ^^^ 1-element tuple pattern is not allowed
   | 

error: 1-element tuple pattern is not allowed
  ==> tuple2.cj:12:14:
   | 
12 |         case (p) => p
   |              ^^^ 1-element tuple pattern is not allowed
   | 

2 errors generated, 2 errors printed.
*/
