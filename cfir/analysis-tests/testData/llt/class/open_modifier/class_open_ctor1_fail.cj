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
        foo()
        goo()
        let f1 = foo
        goo
        this.coo
    }
}

main(): Int64 {
    0
}

/* SCAN
error: instance member function 'foo' cannot be accessed in the constructor of open class 'B'
  ==> class_open_ctor1_fail.cj:23:9:
   | 
23 |         foo()
   |         ^ 
   | 

error: instance member function 'goo' cannot be accessed in the constructor of open class 'B'
  ==> class_open_ctor1_fail.cj:24:9:
   | 
24 |         goo()
   |         ^ 
   | 

error: instance member function 'foo' cannot be accessed in the constructor of open class 'B'
  ==> class_open_ctor1_fail.cj:25:18:
   | 
25 |         let f1 = foo
   |                  ^ 
   | 

error: instance member function 'goo' cannot be accessed in the constructor of open class 'B'
  ==> class_open_ctor1_fail.cj:26:9:
   | 
26 |         goo
   |         ^ 
   | 

error: instance member function 'coo' cannot be accessed in the constructor of open class 'B'
  ==> class_open_ctor1_fail.cj:27:14:
   | 
27 |         this.coo
   |              ^ 
   | 

5 errors generated, 5 errors printed.
*/
