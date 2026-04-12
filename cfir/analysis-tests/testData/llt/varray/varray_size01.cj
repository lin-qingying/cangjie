// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

main() {
    let a: VArray<Int64, $5> = [1, 2, 3, 4, 5]

    let s: Int32 = a.size // error
    a.size = 1 // error
    a.size = s // error
    a.sss // error
    0
}

/* SCAN
error: mismatched types
  ==> varray_size01.cj:12:20:
   | 
12 |     let s: Int32 = a.size // error
   |                    ^^^^^^ expected 'Int32', found 'Int64'
   | 

error: cannot assign to immutable value
  ==> varray_size01.cj:13:5:
   | 
13 |     a.size = 1 // error
   |     ^^^^^^ 
   | 

error: cannot assign to immutable value
  ==> varray_size01.cj:14:5:
   | 
14 |     a.size = s // error
   |     ^^^^^^ 
   | 

error: undeclared identifier 'sss'
  ==> varray_size01.cj:15:7:
   | 
15 |     a.sss // error
   |       ^^^ 
   | 

4 errors generated, 4 errors printed.
*/
