// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct A {
    var x = 0
}

struct B {
    var a: A
    init() {
        a.x = 1
        a = A()
    }
}

main() {
    println(B().a.x)
}

/* SCAN
error: variable 'a' is used before initialization
  ==> variable_use_before_init_11.cj:16:9:
   | 
16 |         a.x = 1
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
