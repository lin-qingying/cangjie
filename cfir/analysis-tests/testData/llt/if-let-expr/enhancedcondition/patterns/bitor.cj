// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt -o %output -Woff all 2>&1 | compare %f

func foo(a: ?Int64, b: ?Int64) {
    if (let Some(_) <- a | let None <- b) {
        1
    }
    if (let Some(_) <- b | Go(8)) {
        2
    }
    if (let Some(_) | None <- a) {
        3
    } else {
        8
    } 
}

func Go(a: Int64) {
    a > 3
}

main() {
    println(foo(Some(15), Some(3)))
    println(foo(Some(8), None))
}

/* SCAN
error: unclosed delimiter: '('
  ==> bitor.cj:10:25:
   | 
10 |     if (let Some(_) <- a | let None <- b) {
   |        ~                ^ expected ')' here
   |        |
   |        to match this opening '('
   | 

error: unclosed delimiter: '('
  ==> bitor.cj:13:25:
   | 
13 |     if (let Some(_) <- b | Go(8)) {
   |        ~                ^ expected ')' here
   |        |
   |        to match this opening '('
   | 

2 errors generated, 2 errors printed.
*/
