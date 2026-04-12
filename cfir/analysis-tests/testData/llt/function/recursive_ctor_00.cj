// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    let a = A()
}

main() {}

/* SCAN
error: recursive constructor calling detected
  ==> recursive_ctor_00.cj:10:13:
   | 
10 |     let a = A()
   |             ^ 
   | 
note: depends on
 ==> recursive_ctor_00.cj:9:7:
  | 
9 | class A {
  |       ^ 
  | 
note: depends on
  ==> recursive_ctor_00.cj:10:13:
   | 
10 |     let a = A()
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
