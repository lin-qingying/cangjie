// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

let x = 4

private func f() {
    func f1() {
        x
    }
    let a = {
        => x
    }
    let x = 2
}

main() {
    0
}

/* SCAN
warning: the variable 'x' actually captures this decl capture_warning.cj:9:1, can not captures the decl capture_warning.cj:18:5
  ==> capture_warning.cj:13:9:
   | 
13 |         x
   |         ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff semantics`

warning: the variable 'x' actually captures this decl capture_warning.cj:9:1, can not captures the decl capture_warning.cj:18:5
  ==> capture_warning.cj:16:12:
   | 
16 |         => x
   |            ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff semantics`

warning: unused function:'f'
  ==> capture_warning.cj:11:1:
   | 
11 |   private func f() {
   |  _^
12 | |     func f1() {
   | | ...
19 | | }
   | |_^ unused function
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused function:'f1'
  ==> capture_warning.cj:12:5:
   | 
12 |       func f1() {
   |  _____^
13 | |         x
14 | |     }
   | |_____^ unused function
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused variable:'a'
  ==> capture_warning.cj:15:9:
   | 
15 |     let a = {
   |         ^ unused variable
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused variable:'x'
  ==> capture_warning.cj:18:9:
   | 
18 |     let x = 2
   |         ^ unused variable
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused function:'main'
  ==> capture_warning.cj:21:1:
   | 
21 |   main() {
   |  _^
22 | |     0
23 | | }
   | |_^ unused function
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

7 warnings generated, 7 warnings printed.
*/
