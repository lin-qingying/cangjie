// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main() {
    let a: VArray<Int64, $-1> = [1, 2, 3] //error
    0
}

/* SCAN
error: expected an integer literal than or equal to 0 after '$' to specificate the size of 'VArray' type
  ==> varray03.cj:10:26:
   | 
10 |     let a: VArray<Int64, $-1> = [1, 2, 3] //error
   |                          ^ 
   | 

1 error generated, 1 error printed.
*/
