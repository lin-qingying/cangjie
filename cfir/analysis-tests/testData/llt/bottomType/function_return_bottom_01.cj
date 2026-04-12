// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// (CJNATIVE) EXEC: %run %run_opt %output %run_args

main(): Int64 {
    case01()
    return 0
}

func case01() {
    while (return ()) {
        print("yes")
    }
}

/* SCAN
warning: unreachable expression
  ==> function_return_bottom_01.cj:16:5:
   | 
16 |       while (return ()) {
   |       ^      ~~~~~~~~~ any code following this expression is unreachable
   |  _____|
17 | |         print("yes")
18 | |     }
   | |_____^ unreachable expression
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

1 warning generated, 1 warning printed.
*/
