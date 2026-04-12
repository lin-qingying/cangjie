// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    init() {
        A(true)
    }
    init(_: Bool) {
        A()
    }
}

main() {}

/* SCAN
error: recursive constructor calling detected
  ==> recursive_ctor_01.cj:10:5:
   | 
10 |     init() {
   |     ^^^^ 
   | 
note: depends on
  ==> recursive_ctor_01.cj:11:9:
   | 
11 |         A(true)
   |         ^ 
   | 
note: depends on
  ==> recursive_ctor_01.cj:13:5:
   | 
13 |     init(_: Bool) {
   |     ^^^^ 
   | 
note: depends on
  ==> recursive_ctor_01.cj:14:9:
   | 
14 |         A()
   |         ^ 
   | 
note: depends on
  ==> recursive_ctor_01.cj:10:5:
   | 
10 |     init() {
   |     ^^^^ 
   | 

1 error generated, 1 error printed.
*/
