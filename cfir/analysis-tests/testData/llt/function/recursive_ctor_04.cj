// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

open class B {
    var a: A = A()
}

class A <: B {}

main() {
    let _ = A()
}

/* SCAN
error: recursive constructor calling detected
  ==> recursive_ctor_04.cj:13:7:
   | 
13 | class A <: B {}
   |       ^ 
   | 
note: depends on
 ==> recursive_ctor_04.cj:9:12:
  | 
9 | open class B {
  |            ^ 
  | 
note: depends on
  ==> recursive_ctor_04.cj:10:16:
   | 
10 |     var a: A = A()
   |                ^ 
   | 
note: depends on
  ==> recursive_ctor_04.cj:13:7:
   | 
13 | class A <: B {}
   |       ^ 
   | 

1 error generated, 1 error printed.
*/
