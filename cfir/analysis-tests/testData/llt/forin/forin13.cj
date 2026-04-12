// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

enum E {
    A | B(Int64)
}

main() {
    let arr = [A, B(0)]
    for (E.A(x) in arr) {}
}

/* SCAN
error: enum pattern's parameters size is wrong
  ==> forin13.cj:15:10:
   | 
15 |     for (E.A(x) in arr) {}
   |          ^ 
   | 

error: the pattern in for-in expression must be irrefutable
  ==> forin13.cj:15:5:
   | 
15 |     for (E.A(x) in arr) {}
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
