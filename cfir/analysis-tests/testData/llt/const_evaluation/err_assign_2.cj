// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct S {
    static var v = 0
    const init() {
        v = 0
    }
}

main() {
    const s = S()
    S.v
}

/* SCAN
error: expected 'const' expression
  ==> err_assign_2.cj:12:9:
   | 
12 |         v = 0
   |         ^^^^^ 
   | 

1 error generated, 1 error printed.
*/
