// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: file2.cj
// ERRCHECK: %compiler %cmp_opt %f file2.cj -o %output

main() {
    A.a++
    A.b += 1
}

/* SCAN
error: cannot assign to immutable value
  ==> file1.cj:11:5:
   | 
11 |     A.a++
   |     ^^^^^ 
   | 
note: variable 'a' is immutable
 ==> file2.cj:8:16:
  | 
8 |     static let a = 1
  |                ^ 
  | 

error: cannot assign to immutable value
  ==> file1.cj:12:5:
   | 
12 |     A.b += 1
   |     ^^^^^^^^ 
   | 
note: variable 'b' is immutable
 ==> file2.cj:9:18:
  | 
9 |     static const b = 1
  |                  ^ 
  | 

2 errors generated, 2 errors printed.
*/
