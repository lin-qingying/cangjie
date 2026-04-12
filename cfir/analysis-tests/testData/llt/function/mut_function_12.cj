// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

struct A {
    mut func f() {}
}

class B {
    var a = A()

    mut prop x: A {
        get() {
            a
        }
        set(_) {}
    }

    func f() {
        a
    }
}

main() {
    B().x.f()
}

/* SCAN
error: cannot use mutable function on immutable value
  ==> mut_function_12.cj:29:5:
   | 
29 |     B().x.f()
   |     ^^^^^ ~ is a mutable function
   |     |
   |     is immutable
   | 
note: property getter returns immutable value
  ==> mut_function_12.cj:17:9:
   | 
17 |         get() {
   |         ^^^ 
   | 

1 error generated, 1 error printed.
*/
