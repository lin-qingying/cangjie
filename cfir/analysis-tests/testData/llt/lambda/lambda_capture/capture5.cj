// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

main(): Int64 {
    return 0
}

var a = 0
var f1: Unit = if (a > 0) {
    var a: Int32 = 1
    let b: Int32 = 2
    var x1 = {=> a}
    var x2 = {=> b}
    var x3 = {=> a}()
    var x4 = {=> b}()
}

func foo(x: () -> Int32) {
    return x
}

func goo(x: Int32) {
    return x
}

/* SCAN
error: lambda capturing mutable variables needs to be called directly
  ==> capture5.cj:17:14:
   | 
17 |     var x1 = {=> a}
   |              ^^^^^^ 
   | 
note: 'a' is mutable
  ==> capture5.cj:17:18:
   | 
17 |     var x1 = {=> a}
   |                  ^ 
   | 

1 error generated, 1 error printed.
*/
