// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt -o %output 2>&1 | compare %f
enum My {
    A |B(Int64) | C(Int64)
}

main() {
    let a = A 

    if (let B(value) <- a where value > 0) {
        value + 1
    } else { -1 }
}

/* SCAN
error: expected '&&', '||', or ')', got 'where'
  ==> ifletwhere.cj:15:27:
   | 
15 |     if (let B(value) <- a where value > 0) {
   |                           ^^^^^ did you mean to write '&&'
   | 

1 error generated, 1 error printed.
*/
