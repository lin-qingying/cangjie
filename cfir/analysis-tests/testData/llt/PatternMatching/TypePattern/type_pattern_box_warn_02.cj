// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

interface I {}

struct A <: I {}

main() {
    let x = (1, (1, A()))
    match (x) {
        case _: (Int64, (Int64, I)) => 0
        case _ => 1
    }
}

/* SCAN
warning: unreachable pattern
  ==> type_pattern_box_warn_02.cj:16:14:
   | 
16 |         case _: (Int64, (Int64, I)) => 0
   |              ^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: the selector is of type 'Tuple<Int64, Tuple<Int64, Struct-A>>', which is not a subtype of 'Tuple<Int64, Tuple<Int64, Interface-I>>
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused function:'main'
  ==> type_pattern_box_warn_02.cj:13:1:
   | 
13 |   main() {
   |  _^
14 | |     let x = (1, (1, A()))
   | | ...
19 | | }
   | |_^ unused function
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

2 warnings generated, 2 warnings printed.
*/
