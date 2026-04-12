// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

interface I {
    var a: Int64 = 1 
}

main(): Int64 {
    return 0
}

/* SCAN
error: unexpected variable declaration in interface body
  ==> interface_member_variable_check_01.cj:10:5:
   | 
9  | interface I {
   | ~~~~~~~~~ in interface
10 |     var a: Int64 = 1 
   |     ^^^ unexpected variable declaration
   | 

1 error generated, 1 error printed.
*/
