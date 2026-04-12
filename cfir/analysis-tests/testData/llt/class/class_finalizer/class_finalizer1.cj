// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class A {
    ~init() {}
}

class B {
    ~init() {}
}

class C {
    ~init()() {}
}

class D {
    ~init() {}
    ~init() {}
}

open class E {
    ~init() {}
}

open class F {
    ~init() {}
    public open func f() {
        0
    }
}

open class Parent {
    public open func f() {
        0
    }
}

class Children {
    ~init() {}
}

main() {
    0
}

/* SCAN
error: finalizer is forbidden in class 'E' that is open
  ==> class_finalizer1.cj:27:5:
   | 
27 |     ~init() {}
   |     ^ 
   | 

error: finalizer is forbidden in class 'F' that is open
  ==> class_finalizer1.cj:31:5:
   | 
31 |     ~init() {}
   |     ^ 
   | 

error: function '~init' has overload conflicts
  ==> class_finalizer1.cj:23:5:
   | 
23 |     ~init() {}
   |     ^^^^^ 
   | 
note: conflict with the declaration
  ==> class_finalizer1.cj:22:5:
   | 
22 |     ~init() {}
   |     ^^^^^ 
   | 

error: finalizer cannot have more than one parameter list
  ==> class_finalizer1.cj:18:10:
   | 
18 |     ~init()() {}
   |          ^ 
   | 

4 errors generated, 4 errors printed.
*/
