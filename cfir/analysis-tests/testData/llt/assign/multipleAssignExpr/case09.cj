// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f

main() {
    var a: Array<Int64> = [1, 2]
    (a[0], a[1], a[2]) = (100, 200, 300)
}

/* SCAN
error: array index is out of bounds
  ==> case09.cj:11:18:
   | 
11 |     (a[0], a[1], a[2]) = (100, 200, 300)
   |                  ^^^^ array index 2 is past the end of array (which contains 2 elements)
   | 

1 error generated, 1 error printed.
*/
