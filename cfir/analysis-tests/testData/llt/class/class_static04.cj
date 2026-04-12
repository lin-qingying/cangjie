// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    private static func f() {
        0
    }
    static func f(_: Int64) {
        1
    }
}

main() {
    let f: () -> Int64 = A.f
    f()
}

/* SCAN
error: can not access function 'f'
  ==> class_static04.cj:19:26:
   | 
19 |     let f: () -> Int64 = A.f
   |                          ^ 
   | 

1 error generated, 1 error printed.
*/
