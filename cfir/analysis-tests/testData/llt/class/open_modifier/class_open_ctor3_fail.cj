// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

interface I {
    func coo() {}
}

open class A <: I {
    protected func foo() {}
}

extend A {
    func goo() {}
}

open class B <: A {
    public init() {
        { // lambda
            => this
        }
        let v = this
        test(this)
    }
}

func test(v: B) {
    1
}

main(): Int64 {
    0
}

/* SCAN
error: 'this' cannot be used as an expression in the constructor of open class
  ==> class_open_ctor3_fail.cj:24:16:
   | 
24 |             => this
   |                ^ 
   | 

error: 'this' cannot be used as an expression in the constructor of open class
  ==> class_open_ctor3_fail.cj:26:17:
   | 
26 |         let v = this
   |                 ^ 
   | 

error: 'this' cannot be used as an expression in the constructor of open class
  ==> class_open_ctor3_fail.cj:27:14:
   | 
27 |         test(this)
   |              ^ 
   | 

3 errors generated, 3 errors printed.
*/
