// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct S0 {
    var x0 = 0
}

struct S1 {
    var x1 = S0()

    func f() {
        x1.x0 += 1
    }
}

main() {}

/* SCAN
error: instance member variable 'x1' cannot be modified in immutable function
  ==> mut_function_05.cj:17:9:
   | 
17 |         x1.x0 += 1
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
