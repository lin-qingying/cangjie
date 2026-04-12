// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC-1: %run %run_opt %output %run_args

let a: (Int32, Unit) = (0, ())
var b: ((Int8, UInt32), Int64, Unit, Int32) = ((0, 0), 0, (), 0)

main() {
    match ((b, a, 4, 8)) {
        case (_, _, _: Int32, _) => 0
        case _ => 1
        case (((0, 1), 2, (), 3), _, 4, 5) => 2
    }
}

/* SCAN
warning: unreachable pattern
  ==> tuple4.cj:15:14:
   | 
15 |         case (_, _, _: Int32, _) => 0
   |              ^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unreachable pattern
  ==> tuple4.cj:17:14:
   | 
17 |         case (((0, 1), 2, (), 3), _, 4, 5) => 2
   |              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 warnings generated, 2 warnings printed.
*/
