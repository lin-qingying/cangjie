// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct A {
    var x = 0
}

extend A {
    func f() {
        x += 1
    }
}

main() {}

/* SCAN
error: instance member variable 'x' cannot be modified in immutable function
  ==> mut_function_04.cj:15:9:
   | 
15 |         x += 1
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
