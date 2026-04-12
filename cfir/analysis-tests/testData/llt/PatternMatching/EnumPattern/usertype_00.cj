// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

package myenum

enum E {
    | A
    | B
}

main() {
    let x = A
    match (x) {
        case E.A => 0
        case A => 0
        case E.A => 0
        case B => 1
        case E.B => 1
        case E.B => 1
    }
}

/* SCAN
warning: unreachable pattern
  ==> usertype_00.cj:20:14:
   | 
20 |         case A => 0
   |              ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unreachable pattern
  ==> usertype_00.cj:21:14:
   | 
21 |         case E.A => 0
   |              ^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unreachable pattern
  ==> usertype_00.cj:23:14:
   | 
23 |         case E.B => 1
   |              ^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unreachable pattern
  ==> usertype_00.cj:24:14:
   | 
24 |         case E.B => 1
   |              ^^^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused function:'main'
  ==> usertype_00.cj:16:1:
   | 
16 |   main() {
   |  _^
17 | |     let x = A
   | | ...
26 | | }
   | |_^ unused function
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

5 warnings generated, 5 warnings printed.
*/
