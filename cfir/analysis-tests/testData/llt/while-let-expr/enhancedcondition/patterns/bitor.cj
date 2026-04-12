// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt -o %output 2>&1 | compare %f

func foo(a: ?Int64, b: ?Int64) {
    while (let Some(_) <- a | let None <- b) {
        return 1
    }
    8
}

func Go(a: Int64) {
    a > 3
}

main() {
    println(foo(Some(15), Some(3))) // -1
    println(foo(Some(8), None)) // 8
}

/* SCAN
error: unclosed delimiter: '('
  ==> bitor.cj:10:28:
   | 
10 |     while (let Some(_) <- a | let None <- b) {
   |           ~                ^ expected ')' here
   |           |
   |           to match this opening '('
   | 

1 error generated, 1 error printed.
*/
