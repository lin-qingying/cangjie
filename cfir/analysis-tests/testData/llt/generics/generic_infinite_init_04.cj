// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

// X -> X, Y -> E<X>
// X -> Y, Y -> E<X>  cause infinite substitution
// X -> E<X>, Y -> E<X>  cause infinite substitution

enum E<X> {
    e(X)
}

struct A<X, Y> {
    public operator func [](b: A<X, E<X>>): Bool {
        true
    }
    public operator func +(b: A<Y, E<X>>): Bool {
        true
    }
}

main(): Int64 {
    var a: A<Int32, Int32>
    return 0
}

/* SCAN
error: generic infinite instantiation
  ==> generic_infinite_init_04.cj:21:31:
   | 
21 |     public operator func +(b: A<Y, E<X>>): Bool {
   |                               ^ 
   | 

1 error generated, 1 error printed.
*/
