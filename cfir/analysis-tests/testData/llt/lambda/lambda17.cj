// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

var a1: (Int32, Int32) -> Int32 = {a, b => a + b}
var a2: (Int32, Int32) -> Int32 = {a: Int16, b => a + b}

main(): Int64 {
    if (a1(1, 1) != 2) {
        return 1
    }
    if (a2(1, 1) != 2) {
        return 1
    }
    return 0
}

/* SCAN
error: mismatched types
  ==> lambda17.cj:10:36:
   | 
10 | var a2: (Int32, Int32) -> Int32 = {a: Int16, b => a + b}
   |                                    ^^^^^^^^ expected 'Int32', found 'Int16'
   | 

1 error generated, 1 error printed.
*/
