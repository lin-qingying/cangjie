// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main(): Int64 {
    if (case01() != 4) {
        return 1
    }
    return 0
}

func case01() {
    let intList: Array<Int32> = [0, 1, 2, 3, 4]
    let a: Int32 = 0
    for (item in intList where item > 3) {
        a = item
        item++
    }
    a
}

/* SCAN
error: cannot assign to immutable value
  ==> forin4.cj:20:9:
   | 
20 |         a = item
   |         ^^^^^^^^ 
   | 
note: variable 'a' is immutable
  ==> forin4.cj:18:9:
   | 
18 |     let a: Int32 = 0
   |         ^ 
   | 

error: cannot assign to immutable value
  ==> forin4.cj:21:9:
   | 
21 |         item++
   |         ^^^^^^ 
   | 
note: variable 'item' is immutable
  ==> forin4.cj:19:10:
   | 
19 |     for (item in intList where item > 3) {
   |          ^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
