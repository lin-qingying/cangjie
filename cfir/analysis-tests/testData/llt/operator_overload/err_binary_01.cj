// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

enum E {
    A
}

main() {
    let _: E = A(A) + A
}

/* SCAN
error: no matching function for operator '()' function call
  ==> err_binary_01.cj:14:16:
   | 
14 |     let _: E = A(A) + A
   |                ^ 
   | 

1 error generated, 1 error printed.
*/
