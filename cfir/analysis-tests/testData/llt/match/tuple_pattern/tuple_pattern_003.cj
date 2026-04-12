// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    let scoreTuple = ("Allen", 90)
    var scoreResult: String = match (scoreTuple) {
        case ("Bob", 90) => "Bob got 90"
        case (_, _, _) => "someone got 100"
    }
    0
}

/* SCAN
error: tuple pattern with correct size expected
  ==> tuple_pattern_003.cj:13:14:
   | 
13 |         case (_, _, _) => "someone got 100"
   |              ^ 
   | 

1 error generated, 1 error printed.
*/
