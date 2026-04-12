// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class B {
    init() {
        A()
    }
}

class A {
    init() {
        B()
    }
}

main() {
    A()
    0
}

/* SCAN
error: recursive constructor calling detected
  ==> recursive_ctor_08.cj:16:5:
   | 
16 |     init() {
   |     ^^^^ 
   | 
note: depends on
  ==> recursive_ctor_08.cj:17:9:
   | 
17 |         B()
   |         ^ 
   | 
note: depends on
  ==> recursive_ctor_08.cj:10:5:
   | 
10 |     init() {
   |     ^^^^ 
   | 
note: depends on
  ==> recursive_ctor_08.cj:11:9:
   | 
11 |         A()
   |         ^ 
   | 
note: depends on
  ==> recursive_ctor_08.cj:16:5:
   | 
16 |     init() {
   |     ^^^^ 
   | 

1 error generated, 1 error printed.
*/
