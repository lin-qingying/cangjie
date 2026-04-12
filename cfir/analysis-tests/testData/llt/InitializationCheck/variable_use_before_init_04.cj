// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f() {
    1 < 2
}

main() {
    var a: Int64
    var b: Int64
    if (f()) {
    } else if ((a, b) = (1, 2, 3)) {
    }
    b - a - a
}

/* SCAN
error: mismatched types
  ==> variable_use_before_init_04.cj:17:25:
   | 
17 |     } else if ((a, b) = (1, 2, 3)) {
   |                ~~~~~~   ^^^^^^^^^ the expression has type 'Tuple<Int64, Int64, Int64>'
   |                |
   |                the tuple has 2-elements, found one with 3-elements
   | 

error: variable 'b' is used before initialization
  ==> variable_use_before_init_04.cj:19:5:
   | 
19 |     b - a - a
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
