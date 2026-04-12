// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC: %run %run_opt %output %run_args

interface I {}

class A <: I {}

func f(x: I) {}

main() {
    match (f) {
        case _: (A) -> Unit => 0
        case _ => 1
    }
}

/* SCAN
warning: unreachable pattern
  ==> type_pattern_box_warn_04.cj:19:14:
   | 
19 |         case _ => 1
   |              ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused variable:'x'
  ==> type_pattern_box_warn_04.cj:14:8:
   | 
14 | func f(x: I) {}
   |        ^ unused variable
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 warnings generated, 2 warnings printed.
*/
