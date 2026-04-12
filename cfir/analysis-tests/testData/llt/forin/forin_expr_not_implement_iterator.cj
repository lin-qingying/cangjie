// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

internal import std.collection.*

main() {
    for (num in 10) {
        println(num)
    }
    return 0
}

/* SCAN
error: the type Int64 of expression in for-in expression does not implement Iterator
  ==> forin_expr_not_implement_iterator.cj:12:17:
   | 
12 |     for (num in 10) {
   |                 ^ 
   | 

1 error generated, 1 error printed.
*/
