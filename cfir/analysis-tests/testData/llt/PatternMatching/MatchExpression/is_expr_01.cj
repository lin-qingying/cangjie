// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    println(0 is Any)
    println((1 / 0) is Any)
}

/* SCAN
error: divide by zero
  ==> is_expr_01.cj:11:14:
   | 
11 |     println((1 / 0) is Any)
   |              ^^^^^ attempt to divide by zero
   | 

1 error generated, 1 error printed.
*/
