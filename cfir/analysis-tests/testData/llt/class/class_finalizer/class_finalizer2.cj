// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class A {
    public var x = 1
    ~init() {
        f(this)
        let a = {=> this.x}
        {=> x}
        this
        func g() {
            x
        }
    }
}

func f(a: A) {
    return a
}

main() {
    0
}

/* SCAN
error: 'this' cannot be used as an expression in the finalizer
  ==> class_finalizer2.cj:12:11:
   | 
12 |         f(this)
   |           ^ 
   | 

error: 'this' cannot be used as an expression in the finalizer
  ==> class_finalizer2.cj:15:9:
   | 
15 |         this
   |         ^ 
   | 

2 errors generated, 2 errors printed.
*/
