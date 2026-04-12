// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

// test type recursive.
struct S1 {
    var a: S2 = S2()
}

struct S2 {
    var a: VArray<S1, $1> = [S1()]
}

main() {
    var a: S2 = S2()
    0
}

/* SCAN
error: recursive constructor calling detected
  ==> varray14.cj:15:30:
   | 
15 |     var a: VArray<S1, $1> = [S1()]
   |                              ^^ 
   | 
note: depends on
  ==> varray14.cj:10:8:
   | 
10 | struct S1 {
   |        ^^ 
   | 
note: depends on
  ==> varray14.cj:11:17:
   | 
11 |     var a: S2 = S2()
   |                 ^^ 
   | 
note: depends on
  ==> varray14.cj:14:8:
   | 
14 | struct S2 {
   |        ^^ 
   | 
note: depends on
  ==> varray14.cj:15:30:
   | 
15 |     var a: VArray<S1, $1> = [S1()]
   |                              ^^ 
   | 

error: value type recursive detected: 'S1->S2->S1'
  ==> varray14.cj:10:1:
   | 
10 | struct S1 {
   | ^ 
   | 

2 errors generated, 2 errors printed.
*/
