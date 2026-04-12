// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A { }

extend A {
    var x: Int64 = 1  
    var y: Int64 = 2  
}

/* SCAN
error: unexpected variable declaration in extend body
  ==> extend_member_illegal_decl.cj:12:5:
   | 
11 | extend A {
   | ~~~~~~ in extend
12 |     var x: Int64 = 1  
   |     ^^^ unexpected variable declaration
   | 

error: unexpected variable declaration in extend body
  ==> extend_member_illegal_decl.cj:13:5:
   | 
11 | extend A {
   | ~~~~~~ in extend
12 |     var x: Int64 = 1  
13 |     var y: Int64 = 2  
   |     ^^^ unexpected variable declaration
   | 

2 errors generated, 2 errors printed.
*/
