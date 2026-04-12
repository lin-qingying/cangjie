// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

main() {
    let a = None<Int64>
    if (let Option<Int64>.None(v) <- a) {
        return 1
    }
    return 0
}

/* SCAN
error: enum pattern's parameters size is wrong
  ==> invalid_enum_pattern_002.cj:11:13:
   | 
11 |     if (let Option<Int64>.None(v) <- a) {
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
