// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum TimeUnit1 {
    Hour
    | Year
}

enum TimeUnit2 {
    Month
}

func run(): Int64 {
    var time = TimeUnit2.Month
    var res: Int32 = match (time) {
        case Month(Hour) => 2020
    }
    return 0
}

/* SCAN
error: enum pattern's parameters size is wrong
  ==> enum3.cj:21:14:
   | 
21 |         case Month(Hour) => 2020
   |              ^ 
   | 

1 error generated, 1 error printed.
*/
