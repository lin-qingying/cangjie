// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC: %run %run_opt %output %run_args

abstract sealed class Color {}

class Red <: Color {}

class Green <: Color {}

open class Blue <: Color {}

class DarkBlue <: Blue {}

func f(c: Color) {
    match (c) {
        case _: Int64 => -1 // unreachable
        case _: Red => 0
        case _: Green => 1
        case _: Blue => 2
        case _: DarkBlue => -1 // unreachable
        case _ => -1 // unreachable
    }
}

main() {
    f(Red())
}

/* SCAN
warning: unreachable pattern
  ==> sealed_open_unreachable.cj:22:14:
   | 
22 |         case _: Int64 => -1 // unreachable
   |              ^^^^^^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unreachable pattern
  ==> sealed_open_unreachable.cj:26:14:
   | 
26 |         case _: DarkBlue => -1 // unreachable
   |              ^^^^^^^^^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 warnings generated, 2 warnings printed.
*/
