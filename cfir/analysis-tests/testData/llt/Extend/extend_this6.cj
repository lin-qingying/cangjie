// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class A {
    func foo() {
        this.bar() // error
        bar() // error
    }
}

extend A {
    func bar() {}
}

struct S {
    func foo() {
        this.bar() // error
        bar() // error
    }
}

extend S {
    func bar() {}
}

enum E {
    EE

    func foo() {
        this.bar() // error
        bar() // error
    }
}

extend E {
    func bar() {}
}

main() {}

/* SCAN
error: 'bar' is not a member of class 'A'
  ==> extend_this6.cj:11:14:
   | 
11 |         this.bar() // error
   |              ^^^ 
   | 

error: undeclared identifier 'bar'
  ==> extend_this6.cj:12:9:
   | 
12 |         bar() // error
   |         ^ 
   | 

error: 'bar' is not a member of struct 'S'
  ==> extend_this6.cj:22:14:
   | 
22 |         this.bar() // error
   |              ^^^ 
   | 

error: undeclared identifier 'bar'
  ==> extend_this6.cj:23:9:
   | 
23 |         bar() // error
   |         ^ 
   | 

error: 'bar' is not a member of enum 'E'
  ==> extend_this6.cj:35:14:
   | 
35 |         this.bar() // error
   |              ^^^ 
   | 

error: undeclared identifier 'bar'
  ==> extend_this6.cj:36:9:
   | 
36 |         bar() // error
   |         ^ 
   | 

6 errors generated, 6 errors printed.
*/
