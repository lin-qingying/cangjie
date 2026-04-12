// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

func f() {
    E.A = E.A
    A = A
}

enum E {
    A
}

main() {}

/* SCAN
error: cannot assign to immutable value
  ==> assign_008.cj:10:5:
   | 
10 |     E.A = E.A
   |     ^^^^^^^^^ 
   | 
note: variable 'A' is immutable
  ==> assign_008.cj:15:5:
   | 
15 |     A
   |     ^ 
   | 

error: cannot assign to immutable value
  ==> assign_008.cj:11:5:
   | 
11 |     A = A
   |     ^^^^^ 
   | 
note: variable 'A' is immutable
  ==> assign_008.cj:15:5:
   | 
15 |     A
   |     ^ 
   | 

2 errors generated, 2 errors printed.
*/
