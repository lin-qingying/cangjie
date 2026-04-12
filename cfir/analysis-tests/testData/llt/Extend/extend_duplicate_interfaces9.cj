// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix  2>&1 | compare %f

extend Int64 <: ToString {}

main() {
    return 0
}

/* SCAN
error: interface 'Interface-ToString' has been implemented by 'Int64', please remove it
 ==> extend_duplicate_interfaces9.cj:9:17:
  | 
9 | extend Int64 <: ToString {}
  |                 ^^^^^^^^ 
  | 

1 error generated, 1 error printed.
*/
