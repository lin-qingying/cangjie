// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

interface I {}

struct A <: I {}

func f(x: I) {}

main() {
    match (f) {
        case _: (A) -> Unit => 0
        case _ => 1
    }
}

/* SCAN
warning: unreachable pattern
  ==> type_pattern_box_warn_01.cj:17:14:
   | 
17 |         case _: (A) -> Unit => 0
   |              ^^^^^^^^^^^^^^ 
   | 
   # note: the selector is of type '(Interface-I) -> Unit', which is not a subtype of '(Struct-A) -> Unit
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused variable:'x'
  ==> type_pattern_box_warn_01.cj:13:8:
   | 
13 | func f(x: I) {}
   |        ^ unused variable
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused function:'main'
  ==> type_pattern_box_warn_01.cj:15:1:
   | 
15 |   main() {
   |  _^
16 | |     match (f) {
   | | ...
20 | | }
   | |_^ unused function
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

3 warnings generated, 3 warnings printed.
*/
