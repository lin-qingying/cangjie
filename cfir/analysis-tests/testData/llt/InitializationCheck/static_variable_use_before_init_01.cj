// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class Foo <: Bar {
    static let y: Int64
    static init() { // called first
        z
        y = x // error: not yet initialized variable
    }
    static let z = 1
}

open class Bar {
    static let x: Int64
    static init() { // called second
        x = 2
    }
}

var b = Bar.x

main() {
    println(Foo.y)
    println(Bar.x)
}

/* SCAN
error: variable 'x' is used before initialization
  ==> static_variable_use_before_init_01.cj:13:13:
   | 
13 |         y = x // error: not yet initialized variable
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
