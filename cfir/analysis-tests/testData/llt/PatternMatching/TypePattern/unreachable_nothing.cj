// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE)EXEC-PIPE: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// (CJNATIVE)EXEC: %run %run_opt %output %run_args

func f(x: Int64) {
    match (x) {
        case _: Nothing => -1 // unreachable
        case _: ToString => 0
    }
}

main() {
    f(return 0)
}

/* SCAN
warning: unreachable pattern
  ==> unreachable_nothing.cj:12:14:
   | 
12 |         case _: Nothing => -1 // unreachable
   |              ^^^^^^^^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused function:'f'
  ==> unreachable_nothing.cj:10:1:
   | 
10 |   func f(x: Int64) {
   |  _^
11 | |     match (x) {
   | | ...
15 | | }
   | |_^ unused function
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unreachable 'call'
  ==> unreachable_nothing.cj:18:5:
   | 
18 |     f(return 0)
   |     ^ ~~~~~~~~ any code following this expression is unreachable
   |     |
   |     unreachable 'call'
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

3 warnings generated, 3 warnings printed.
*/
