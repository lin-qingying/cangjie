// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

open class A {
    let b = B()
}

class B <: A {
    B() {
        super() // the super should be in the dependency path
    }
}

main() {
    let _ = A()
}

/* SCAN
error: recursive constructor calling detected
  ==> recursive_ctor_06.cj:14:5:
   | 
14 |     B() {
   |     ^ 
   | 
note: depends on
  ==> recursive_ctor_06.cj:15:9:
   | 
15 |         super() // the super should be in the dependency path
   |         ^^^^^ 
   | 
note: depends on
 ==> recursive_ctor_06.cj:9:12:
  | 
9 | open class A {
  |            ^ 
  | 
note: depends on
  ==> recursive_ctor_06.cj:10:13:
   | 
10 |     let b = B()
   |             ^ 
   | 
note: depends on
  ==> recursive_ctor_06.cj:14:5:
   | 
14 |     B() {
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
