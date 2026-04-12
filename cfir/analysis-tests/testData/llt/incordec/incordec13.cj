// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main(): Int64 {
    var list0: Array<Array<Int64>> = [[1, 2, 3, 4], [5, 6, 7]]
    list0[0]++
    list0[0]--
    print("list0[0]: " + list0[0][0].toString())
    if (list0[0][0] == 1) {
        return 0
    }
    return 1
}

/* SCAN
error: mismatched types
  ==> incordec13.cj:11:5:
   | 
11 |     list0[0]++
   |     ^^^^^^^^ expected 'integer type', found 'Struct-Array<Int64>'
   | 
   # note: the base of increment or decrement expressions should be of integer type

error: mismatched types
  ==> incordec13.cj:12:5:
   | 
12 |     list0[0]--
   |     ^^^^^^^^ expected 'integer type', found 'Struct-Array<Int64>'
   | 
   # note: the base of increment or decrement expressions should be of integer type

2 errors generated, 2 errors printed.
*/
