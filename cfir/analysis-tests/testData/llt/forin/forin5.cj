// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

main(): Int64 {
    for (i in 0..i : 1) {
        print("${i}")
    }
    return 0
}

/* SCAN
error: undeclared identifier 'i'
  ==> forin5.cj:10:18:
   | 
10 |     for (i in 0..i : 1) {
   |                  ^ 
   | 

1 error generated, 1 error printed.
*/
