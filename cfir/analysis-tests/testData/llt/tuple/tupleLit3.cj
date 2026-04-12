// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f


main(): Int64 {
    let a : Int32 = 1
    var MyTuple = (
    try
    )
    return 0
}

/* SCAN
error: expected '{' or '(', found ')'
  ==> tupleLit3.cj:14:5:
   | 
14 |     )
   |     ^ expected '{' or '(' here
   | 
   # note: try expression must have block or resources

1 error generated, 1 error printed.
*/
