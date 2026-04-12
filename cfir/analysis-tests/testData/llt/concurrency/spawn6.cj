// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %n.%suffix

main(): Int64 {
    let a: Int64 = spawn {}
    let b: Int64 = spwan {}
    return 0
}

/* SCAN
error: mismatched types
  ==> spawn6.cj:10:20:
   | 
10 |     let a: Int64 = spawn {}
   |                    ^^^^^^^^ expected 'Int64', found 'Class-Future<Unit>'
   | 

error: undeclared identifier 'spwan'
  ==> spawn6.cj:11:20:
   | 
11 |     let b: Int64 = spwan {}
   |                    ^ 
   | 

2 errors generated, 2 errors printed.
*/
