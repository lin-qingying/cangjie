// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

abstract class A {
    var b: B
    init() {
        b = B()
        b.bind(this)
    }
}

class B {
    var a: Option<A> = Option<A>.None
    public func bind(a: A) {
        this.a = a
    }
}

main(): Int64 {
    0
}

/* SCAN
error: 'this' cannot be used as an expression in the constructor of abstract class
  ==> class_open_ctor4_fail.cj:13:16:
   | 
13 |         b.bind(this)
   |                ^ 
   | 

1 error generated, 1 error printed.
*/
