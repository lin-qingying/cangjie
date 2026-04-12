// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

interface I {}

struct A <: I {}

func f(x: (Int64, I)): A {
    A()
}

main() {
    let x = (1, f)
    match (x) {
        case _: (Int64, ((Int64, A)) -> I) => 0
        case _ => 1
    }
}

/* SCAN
warning: unreachable pattern
  ==> type_pattern_box_warn_03.cj:20:14:
   | 
20 |         case _: (Int64, ((Int64, A)) -> I) => 0
   |              ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ 
   | 
   # note: the selector is of type 'Tuple<Int64, (Tuple<Int64, Interface-I>) -> Struct-A>', which is not a subtype of 'Tuple<Int64, (Tuple<Int64, Struct-A>) -> Interface-I>
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused variable:'x'
  ==> type_pattern_box_warn_03.cj:13:8:
   | 
13 | func f(x: (Int64, I)): A {
   |        ^ unused variable
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused function:'main'
  ==> type_pattern_box_warn_03.cj:17:1:
   | 
17 |   main() {
   |  _^
18 | |     let x = (1, f)
   | | ...
23 | | }
   | |_^ unused function
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

3 warnings generated, 3 warnings printed.
*/
