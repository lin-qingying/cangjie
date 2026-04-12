// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main() {
    let a: VArray<Int64, $3> = [1, 2, "3"] //error
    let a1: VArray<Int64, $1> = [1, 2, 3] //error
    0
}

/* SCAN
error: mismatched types
  ==> varray04.cj:10:39:
   | 
10 |     let a: VArray<Int64, $3> = [1, 2, "3"] //error
   |                                       ^^^ expected 'Int64', found 'Struct-String'
   | 

error: mismatch 'VArray' type's size
  ==> varray04.cj:11:33:
   | 
11 |     let a1: VArray<Int64, $1> = [1, 2, 3] //error
   |                                 ^^^^^^^^^ expected size is 1, found 3
   | 

2 errors generated, 2 errors printed.
*/
