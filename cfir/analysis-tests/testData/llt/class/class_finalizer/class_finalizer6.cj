// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f



class C {}

extend C {
    ~init(){}
}

class D {
    ~init(){}
}

class B {
 ~ 
 init(){}
}

main() {
    D().~init()
    0
}

/* SCAN
error: unexpected finalizer in extend body
  ==> class_finalizer6.cj:14:5:
   | 
13 | extend C {
   | ~~~~~~ in extend
14 |     ~init(){}
   |     ^^^^^ unexpected finalizer
   | 

error: expected declaration, found '~'
  ==> class_finalizer6.cj:22:2:
   | 
22 |  ~ 
   |  ^ expected declaration here
   | 

error: expected a member name after '.' in qualified name, found '~'
  ==> class_finalizer6.cj:27:9:
   | 
27 |     D().~init()
   |        ~^ expected a member name here
   |        |
   |        after '.' in qualified name
   | 

3 errors generated, 3 errors printed.
*/
