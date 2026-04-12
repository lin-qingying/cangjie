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
            =>
            foo()
            goo()
            let f1 = foo
            goo
            this.coo
        }
    }
}

main(): Int64 {
    0
}

/* SCAN
error: instance member function 'foo' cannot be accessed in the constructor of open class 'B'
  ==> class_open_ctor2_fail.cj:25:13:
   | 
25 |             foo()
   |             ^ 
   | 

error: instance member function 'goo' cannot be accessed in the constructor of open class 'B'
  ==> class_open_ctor2_fail.cj:26:13:
   | 
26 |             goo()
   |             ^ 
   | 

error: instance member function 'foo' cannot be accessed in the constructor of open class 'B'
  ==> class_open_ctor2_fail.cj:27:22:
   | 
27 |             let f1 = foo
   |                      ^ 
   | 

error: instance member function 'goo' cannot be accessed in the constructor of open class 'B'
  ==> class_open_ctor2_fail.cj:28:13:
   | 
28 |             goo
   |             ^ 
   | 

error: instance member function 'coo' cannot be accessed in the constructor of open class 'B'
  ==> class_open_ctor2_fail.cj:29:18:
   | 
29 |             this.coo
   |                  ^ 
   | 

5 errors generated, 5 errors printed.
*/
