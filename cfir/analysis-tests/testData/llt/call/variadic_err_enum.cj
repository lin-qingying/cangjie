// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

enum E {
    A(Array<Int64>)

    func f(x!: E = A(1, 2)) {}
}

main() {}

/* SCAN
error: extra argument given for parameter list '(Struct-Array<Int64>)' in call
  ==> variadic_err_enum.cj:12:21:
   | 
12 |     func f(x!: E = A(1, 2)) {}
   |                     ^^^^^^ expected 1 argument, found 2
   | 
note: found candidate
  ==> variadic_err_enum.cj:10:5:
   | 
10 |     A(Array<Int64>)
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
