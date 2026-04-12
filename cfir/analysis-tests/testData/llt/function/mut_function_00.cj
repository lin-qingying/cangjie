// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

interface I {
    mut func f(): Unit
}

struct Foo <: I {
    var i = 0
    public mut func f(): Unit {
        i += 1
    }
}

class Bar <: I {
    var i = 0
    public func f(): Unit {
        i += 1
    }
}

func g1<T>(v: T): Unit where T <: I {
    v.f()
}

func g2<T>(v: T): Unit where T <: Bar & I {
    v.f()
}

main() {}

/* SCAN
error: cannot use mutable function on immutable value
  ==> mut_function_00.cj:28:5:
   | 
28 |     v.f()
   |     ^ ~ is a mutable function
   |     |
   |     is immutable
   | 
note: 'v' is a variable declared with 'let'
  ==> mut_function_00.cj:27:12:
   | 
27 | func g1<T>(v: T): Unit where T <: I {
   |            ^ 
   | 

1 error generated, 1 error printed.
*/
