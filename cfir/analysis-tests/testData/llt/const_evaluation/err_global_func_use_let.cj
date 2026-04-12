// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

let x = 0

const func f() {
    x + 1
}

main() {
    f()
}

/* SCAN
error: expected 'const' expression
  ==> err_global_func_use_let.cj:12:5:
   | 
12 |     x + 1
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
