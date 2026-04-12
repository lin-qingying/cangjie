// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

let x = 0
const y = x

main() {
    y
}

/* SCAN
error: expected 'const' expression guaranteed to be evaluated at compile time
  ==> err_let.cj:10:11:
   | 
10 | const y = x
   |           ^ 
   | 

1 error generated, 1 error printed.
*/
