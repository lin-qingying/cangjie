// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC: %run %run_opt %output %run_args

func f(x: Int64) {
    match (x) {
        case _: Bool => -1 // unreachable
        case _ => 0
        case _: ToString => -1 // unreachable
    }
}

main() {
    f(0)
}

/* SCAN
warning: unreachable pattern
  ==> unreachable_simple.cj:12:14:
   | 
12 |         case _: Bool => -1 // unreachable
   |              ^^^^^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unreachable pattern
  ==> unreachable_simple.cj:14:14:
   | 
14 |         case _: ToString => -1 // unreachable
   |              ^^^^^^^^^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 warnings generated, 2 warnings printed.
*/
