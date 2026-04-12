// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt 2>&1 | compare %f
main() {
    let a1 = 18
    let a2: Any = a1
    if (let b <- a2 as Int64) {
        println(b)
    }
}

/* SCAN
error: unclosed delimiter: '('
  ==> as.cj:11:20:
   | 
11 |     if (let b <- a2 as Int64) {
   |        ~           ^ expected ')' here
   |        |
   |        to match this opening '('
   | 

1 error generated, 1 error printed.
*/
