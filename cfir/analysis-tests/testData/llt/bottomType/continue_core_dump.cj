// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// (CJNATIVE) EXEC: %run %run_opt %output %run_args

func function20() {
    while (false) {
        1 == continue
    };

    true;
}

main(): Int64 {
    function20()
    return 0;
}

/* SCAN
warning: unreachable block in 'while' expression
  ==> continue_core_dump.cj:11:19:
   | 
11 |       while (false) {
   |  ___________________^
12 | |         1 == continue
13 | |     };
   | |_____^ unreachable block in 'while' expression
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unreachable 'operator'
  ==> continue_core_dump.cj:12:11:
   | 
12 |         1 == continue
   |           ^^ ~~~~~~~~ any code following this expression is unreachable
   |           |
   |           unreachable 'operator'
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 warnings generated, 2 warnings printed.
*/
