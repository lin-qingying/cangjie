// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

internal import std.collection.*

main(): Int64 {
    let b = ArrayList<Int32>()
    let a: Int64 = std
    return 0
}

/* SCAN
error: undeclared identifier 'std'
  ==> assign_005.cj:13:20:
   | 
13 |     let a: Int64 = std
   |                    ^ 
   | 

1 error generated, 1 error printed.
*/
