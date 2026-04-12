// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f

func test() {
    var tuple = (1, 2, 3)
    (tuple[0], _, tuple[2]) = (4, 5, 6)
}

main() {
}

/* SCAN
error: 'tuple element' can not be assigned
  ==> case02.cj:11:6:
   | 
11 |     (tuple[0], _, tuple[2]) = (4, 5, 6)
   |      ^^^^^^^^ 
   | 

error: 'tuple element' can not be assigned
  ==> case02.cj:11:19:
   | 
11 |     (tuple[0], _, tuple[2]) = (4, 5, 6)
   |                   ^^^^^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
