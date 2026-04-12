// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC: %run %run_opt %output %run_args

main() {
    let any: Any = 0
    let x = any as Any
    match (x) {
        case Some(_) => 0
        case None => 1
    }
}

/* SCAN
warning: unreachable pattern
  ==> as_expr_00.cj:15:14:
   | 
15 |         case None => 1
   |              ^^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

1 warning generated, 1 warning printed.
*/
