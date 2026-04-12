// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

var f: () -> Int64 = {=> 1}

open class A {
    var x: Int64 = 123
    init() {
        f = {=> this.x}
    }
}

abstract class B {
    var x: Int64 = 123
    init() {
        f = {=> this.x}
    }
}

class C {
    var x: Int64 = 123
    init() {
        f = {=> this.x}
    }
}

main() {}

/* SCAN
error: 'this' is not allowed to be captured in constructor of inheritable class
  ==> super_this_12.cj:14:17:
   | 
14 |         f = {=> this.x}
   |                 ^ 
   | 

error: 'this' is not allowed to be captured in constructor of inheritable class
  ==> super_this_12.cj:21:17:
   | 
21 |         f = {=> this.x}
   |                 ^ 
   | 

2 errors generated, 2 errors printed.
*/
