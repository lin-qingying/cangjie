// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

main(): Int64 {
    var tuple = (1, 2, 3)
    tuple[0]++
    return tuple[0]
}

/* SCAN
error: 'tuple element' can not be assigned
  ==> incordec1.cj:11:5:
   | 
11 |     tuple[0]++
   |     ^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
