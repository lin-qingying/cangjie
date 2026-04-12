// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt 2>&1 | compare %f

import std.collection.*

let array = [1, 2, 3]
let list = ArrayList<Int64>()

main() {
    test(100)
    println(array)
    println(list)
}

const func test(a: Int64) {
    array[0]++
    array[1] = a
    list.add(a)
}

/* SCAN
error: expected 'const' expression
  ==> array_inc.cj:21:5:
   | 
21 |     array[0]++
   |     ^^^^^ 
   | 

error: expected 'const' expression
  ==> array_inc.cj:22:5:
   | 
22 |     array[1] = a
   |     ^^^^^ 
   | 

error: expected 'const' expression
  ==> array_inc.cj:23:5:
   | 
23 |     list.add(a)
   |     ^^^^ 
   | 

3 errors generated, 3 errors printed.
*/
