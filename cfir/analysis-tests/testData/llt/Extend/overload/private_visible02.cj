// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

// case 1
class A {}

extend A {
    private func h() {}
}

extend A {
    private func h() {} // OK
}
// case 2
class B {
    private func h() {}
}

extend B {
    private func h() {} // OK
}

// case 3
class C {
    private func f() {}
    private func f() {} // error
}

// case 4
class D {
    func f() {}
}

extend D {
    private func f() {} // error
}
// case 5
class E {
    private func h() {}
}

extend E {
    func h() {} // ok
}

// case 6
class F {}

extend F {
    private func f() {} //error
}

extend F {
    func f() {} // ok
}

// case 4
class I {}

extend I {
    func f() {} // ok
}

extend I {
    private func f() {} // error
}

main() {
    0
}

/* SCAN
error: function 'f' has overload conflicts
  ==> private_visible02.cj:31:18:
   | 
31 |     private func f() {} // error
   |                  ^ 
   | 
note: conflict with the declaration
  ==> private_visible02.cj:30:18:
   | 
30 |     private func f() {}
   |                  ^ 
   | 

error: extend member 'f' is not allowed to shadow members of 'Class-D'
  ==> private_visible02.cj:40:18:
   | 
40 |     private func f() {} // error
   |                  ^ 
   | 
note: shadowed definition of 'f' is here
  ==> private_visible02.cj:36:10:
   | 
36 |     func f() {}
   |          ^ 
   | 

error: extend member 'f' is not allowed to shadow members of 'extend F'
  ==> private_visible02.cj:55:18:
   | 
55 |     private func f() {} //error
   |                  ^ 
   | 

error: extend member 'f' is not allowed to shadow members of 'extend I'
  ==> private_visible02.cj:70:18:
   | 
70 |     private func f() {} // error
   |                  ^ 
   | 

4 errors generated, 4 errors printed.
*/
