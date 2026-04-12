// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f1() {
    var a: Int64
    var b: Int64
    if ((a, b) = (1, 2, 3)) {
    }
    b - a - a
}

func f2() {
    var a: Int64
    var b: Int64
    if ((a, b) = (1, 2, 3)) {
        a = 1
    }
    b - a - a
}

func f3() {
    var a: Int64
    var b: Int64
    if ((a, b) = (1, 2, 3)) {
        b = 1
    }
    b - a - a
}

main() {
    f1()
    f2()
    f3()
}

/* SCAN
error: mismatched types
  ==> variable_use_before_init_03.cj:12:18:
   | 
12 |     if ((a, b) = (1, 2, 3)) {
   |         ~~~~~~   ^^^^^^^^^ the expression has type 'Tuple<Int64, Int64, Int64>'
   |         |
   |         the tuple has 2-elements, found one with 3-elements
   | 

error: mismatched types
  ==> variable_use_before_init_03.cj:20:18:
   | 
20 |     if ((a, b) = (1, 2, 3)) {
   |         ~~~~~~   ^^^^^^^^^ the expression has type 'Tuple<Int64, Int64, Int64>'
   |         |
   |         the tuple has 2-elements, found one with 3-elements
   | 

error: mismatched types
  ==> variable_use_before_init_03.cj:29:18:
   | 
29 |     if ((a, b) = (1, 2, 3)) {
   |         ~~~~~~   ^^^^^^^^^ the expression has type 'Tuple<Int64, Int64, Int64>'
   |         |
   |         the tuple has 2-elements, found one with 3-elements
   | 

3 errors generated, 3 errors printed.
*/
