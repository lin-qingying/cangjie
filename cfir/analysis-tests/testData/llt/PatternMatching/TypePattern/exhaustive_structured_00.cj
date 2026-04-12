// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC: %run %run_opt %output %run_args

func f<T>(x: Option<T>) {
    match (x) {
        case Some(_) => 0
        case None => 1
        case _: ToString => 2 // unreachable!
    }
}

main() {
    f(Some(0))
}

/* SCAN
warning: unreachable pattern
  ==> exhaustive_structured_00.cj:14:14:
   | 
14 |         case _: ToString => 2 // unreachable!
   |              ^^^^^^^^^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

1 warning generated, 1 warning printed.
*/
