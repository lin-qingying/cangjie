// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main() {
    let a1_1 = VArray<Int64, $3>(repeat: 1, {i => i}) // x2 error
    let a1_2 = VArray<Int64, $3>() // x2 error
    let a1_3: VArray<Int64, $3> = VArray<Int64, $3>() // error
    let a1_4 = VArray<C, $3>(repeat: 0) // error
    0
}

/* SCAN
error: undeclared type name 'C'
  ==> varray10.cj:13:23:
   | 
13 |     let a1_4 = VArray<C, $3>(repeat: 0) // error
   |                       ^ 
   | 

error: 'VArray' constructor accepts only one argument
  ==> varray10.cj:10:33:
   | 
10 |     let a1_1 = VArray<Int64, $3>(repeat: 1, {i => i}) // x2 error
   |                                 ^^^^^^^^^^^^^^^^^^^^^ 
   | 

error: 'VArray' constructor accepts only one argument
  ==> varray10.cj:11:33:
   | 
11 |     let a1_2 = VArray<Int64, $3>() // x2 error
   |                                 ^^ 
   | 

error: 'VArray' constructor accepts only one argument
  ==> varray10.cj:12:52:
   | 
12 |     let a1_3: VArray<Int64, $3> = VArray<Int64, $3>() // error
   |                                                    ^^ 
   | 

4 errors generated, 4 errors printed.
*/
