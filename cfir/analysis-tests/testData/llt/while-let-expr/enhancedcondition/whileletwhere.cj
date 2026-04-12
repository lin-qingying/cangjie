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

    while (let B(value) <- a where value > 0) {
        return value + 1
    }
    return -1
}

/* SCAN
error: expected '&&', '||', or ')', got 'where'
  ==> whileletwhere.cj:15:30:
   | 
15 |     while (let B(value) <- a where value > 0) {
   |                              ^^^^^ did you mean to write '&&'
   | 

1 error generated, 1 error printed.
*/
