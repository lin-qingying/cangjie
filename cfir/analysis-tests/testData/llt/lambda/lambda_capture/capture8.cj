// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC: %run %run_opt %output %run_args

var a = 1

main(): Int64 {
    func g() {
        var x = {=> a}
    }
    let a = true
    return 0
}

/* SCAN
warning: the variable 'a' actually captures this decl capture8.cj:10:1, can not captures the decl capture8.cj:16:5
  ==> capture8.cj:14:21:
   | 
14 |         var x = {=> a}
   |                     ^ 
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff semantics`

warning: unused function:'g'
  ==> capture8.cj:13:5:
   | 
13 |       func g() {
   |  _____^
14 | |         var x = {=> a}
15 | |     }
   | |_____^ unused function
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused variable:'x'
  ==> capture8.cj:14:13:
   | 
14 |         var x = {=> a}
   |             ^ unused variable
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

warning: unused variable:'a'
  ==> capture8.cj:16:9:
   | 
16 |     let a = true
   |         ^ unused variable
   | 
   # note: this warning can be suppressed by setting the compiler option `-Woff unused`

4 warnings generated, 4 warnings printed.
*/
