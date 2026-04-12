// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f


struct A {
    ~init(){} 
}

interface I {
    ~init(){}  
}

enum E{
    Date
    ~init(){} 
}

main() {
    0
}

/* SCAN
error: unexpected finalizer in struct body
  ==> class_finalizer4.cj:11:5:
   | 
10 | struct A {
   | ~~~~~~ in struct
11 |     ~init(){} 
   |     ^^^^^ unexpected finalizer
   | 

error: unexpected finalizer in interface body
  ==> class_finalizer4.cj:15:5:
   | 
14 | interface I {
   | ~~~~~~~~~ in interface
15 |     ~init(){}  
   |     ^^^^^ unexpected finalizer
   | 

error: unexpected finalizer in enum body
  ==> class_finalizer4.cj:20:5:
   | 
18 | enum E{
   | ~~~~ in enum
19 |     Date
20 |     ~init(){} 
   |     ^^^^^ unexpected finalizer
   | 

3 errors generated, 3 errors printed.
*/
