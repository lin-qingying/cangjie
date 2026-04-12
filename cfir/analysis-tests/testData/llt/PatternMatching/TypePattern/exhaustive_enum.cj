// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC-1: %run %run_opt %output %run_args

enum E {
    | A(Bool, Int64)
    | B(Rune)
    | C
}

func f(x: E) {
    match (x) {
        case A(_: Int64, _: Int64) => -1 // unreachable
        case A(_: Bool, _: Int64) => 0
        case B(_: ToString) => 1
        case C => 2
        case _ => -1 // unreachable
    }
}

main() {
    f(B(r'😂'))
}

/* SCAN
warning: unreachable pattern
  ==> exhaustive_enum.cj:18:14:
   | 
18 |         case A(_: Int64, _: Int64) => -1 // unreachable
   |              ^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unreachable pattern
  ==> exhaustive_enum.cj:22:14:
   | 
22 |         case _ => -1 // unreachable
   |              ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 warnings generated, 2 warnings printed.
*/
