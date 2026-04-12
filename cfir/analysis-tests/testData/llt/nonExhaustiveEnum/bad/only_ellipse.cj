// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output
enum A {
    | ...
}

enum B {
    ...
}
main() {
}

/* SCAN
error: expected an enum name at beginning of enum body, found '...'
 ==> only_ellipse.cj:9:7:
  | 
9 |     | ...
  |       ^^^ expected an enum constructor name here
  | 
  # note: ellipse cannot be the only constructor of an enum decl

error: expected an enum name at beginning of enum body, found '...'
  ==> only_ellipse.cj:13:5:
   | 
13 |     ...
   |     ^^^ expected an enum constructor name here
   | 
   # note: ellipse cannot be the only constructor of an enum decl

2 errors generated, 2 errors printed.
*/
