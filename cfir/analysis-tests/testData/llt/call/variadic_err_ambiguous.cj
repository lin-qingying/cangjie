// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f() {
    0
}

func f(x!: Int64 = 0) {
    x
}

func f(x: Array<Int64>) {
    x.size
}

func g(x: Int64) {
    x
}

func g(x: Int64, y!: Int64 = 0) {
    x + y
}

func g(x: Array<Int64>) {
    x.size
}

main() {
    f() + g(1)
}

/* SCAN
error: ambiguous match for function call 'f'
  ==> variadic_err_ambiguous.cj:34:5:
   | 
34 |     f() + g(1)
   |     ^ 
   | 
note: found candidate
 ==> variadic_err_ambiguous.cj:9:6:
  | 
9 | func f() {
  |      ^ 
  | 
note: found candidate
  ==> variadic_err_ambiguous.cj:13:6:
   | 
13 | func f(x!: Int64 = 0) {
   |      ^ 
   | 

error: ambiguous match for function call 'g'
  ==> variadic_err_ambiguous.cj:34:11:
   | 
34 |     f() + g(1)
   |           ^ 
   | 
note: found candidate
  ==> variadic_err_ambiguous.cj:21:6:
   | 
21 | func g(x: Int64) {
   |      ^ 
   | 
note: found candidate
  ==> variadic_err_ambiguous.cj:25:6:
   | 
25 | func g(x: Int64, y!: Int64 = 0) {
   |      ^ 
   | 

2 errors generated, 2 errors printed.
*/
