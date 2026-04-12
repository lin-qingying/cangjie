// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct S0 {
    var t0 = 0
    var t1 = {=> t0 += 1}
}

main() {
    let s0 = S0()
    s0.t0
}

/* SCAN
error: instance member variable 't0' cannot be modified in immutable function
  ==> mut_function_06.cj:11:18:
   | 
11 |     var t1 = {=> t0 += 1}
   |                  ^ 
   | 

1 error generated, 1 error printed.
*/
