// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    var a = [-1, 1, 2]
    a = 1000
    return 0
}

var a = [-1, 1, 2]
var b = 1000

func foo() {
    var a = [-1, 1, 2]
    a = 1000
}

/* SCAN
error: cannot convert an integer literal to type 'Struct-Array<Int64>'
  ==> assign_004.cj:11:9:
   | 
11 |     a = 1000
   |         ^ 
   | 

error: cannot convert an integer literal to type 'Struct-Array<Int64>'
  ==> assign_004.cj:20:9:
   | 
20 |     a = 1000
   |         ^ 
   | 

2 errors generated, 2 errors printed.
*/
