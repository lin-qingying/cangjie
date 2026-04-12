// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct S {
    var x = 0

    init() {
        func g() {
            x += 1
        }
        g()
    }

    mut func f() {
        func g() {
            x += 1
        }
        g()
        x += 1
    }
}

main() {
    var x = S()
    x.f()
    x.x
}

/* SCAN
error: 'this' is not allowed to be captured in constructor of struct
  ==> mut_function_03.cj:14:13:
   | 
14 |             x += 1
   |             ^ 
   | 

error: 'x' cannot be captured in the mutable function 'f'
  ==> mut_function_03.cj:21:13:
   | 
21 |             x += 1
   |             ^ 
   | 

2 errors generated, 2 errors printed.
*/
